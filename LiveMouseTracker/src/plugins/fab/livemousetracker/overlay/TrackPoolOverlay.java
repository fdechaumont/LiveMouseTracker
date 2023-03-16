/**
  	@author Fabrice de Chaumont
 	copyright Fabrice de Chaumont @ Institut Pasteur

 	This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package plugins.fab.livemousetracker.overlay;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import icy.canvas.Canvas2D;
import icy.canvas.IcyCanvas;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.main.Icy;
import icy.painter.Overlay;
import icy.roi.BooleanMask2D;
import icy.roi.ROI2D;
import icy.roi.ROIUtil;
import icy.sequence.Sequence;
import icy.system.SystemUtil;
import icy.system.profile.Chronometer;
import icy.type.point.Point5D;
import icy.type.point.Point5D.Double;
import icy.util.GraphicsUtil;
import javafx.scene.layout.Background;
import plugins.fab.livemousetracker.Animal;
import plugins.fab.livemousetracker.FrameInfo;
import plugins.fab.livemousetracker.LiveMouseTracker;
import plugins.fab.livemousetracker.SymetryAngleFinder.Score;
import plugins.fab.livemousetracker.Util;
import plugins.fab.livemousetracker.detection.MouseDetection;
import plugins.fab.livemousetracker.detection.Spine;
import plugins.fab.livemousetracker.detection.SpineSpecialPoint;
import plugins.fab.livemousetracker.detection.SubPartDescriptor;
import plugins.fab.livemousetracker.liveanalysis.chronogram.ChronoConstant;
import plugins.fab.livemousetracker.liveanalysis.chronogram.ChronoStop;
import plugins.fab.livemousetracker.machinelearning.IdentityResult;
import plugins.fab.livemousetracker.machinelearning.MachineLearningTrackIdentityThread;
import plugins.fab.livemousetracker.misc.Clock;
import plugins.fab.livemousetracker.rfid.RFIDAntenna;
import plugins.fab.livemousetracker.rfid.Antenna;
import plugins.fab.livemousetracker.rfid.AntennaReadEvent;
import plugins.fab.livemousetracker.track.AnimalPool;
import plugins.fab.livemousetracker.track.AnonymousPool;
import plugins.fab.livemousetracker.track.TrackSegment;
import plugins.kernel.roi.roi2d.ROI2DArea;
import plugins.kernel.roi.roi2d.ROI2DLine;
import plugins.kernel.roi.roi2d.ROI2DRectangle;

public class TrackPoolOverlay extends Overlay {

	AnonymousPool anonymousTrackPool;
	AnimalPool animalPool;
	Clock clock ;
	public TrackPoolOverlay(String name, AnonymousPool anonymousTrackSegmentPool, AnimalPool animalPool , Clock clock ) {
		super(name);

		this.anonymousTrackPool = anonymousTrackSegmentPool ;
		this.animalPool =  animalPool ;
		this.clock = clock;
		this.setPriority( OverlayPriority.TOPMOST );
		this.setReceiveKeyEventOnHidden( true );
		updateDisplayMode();
	}

	public static void drawCenteredHint( Graphics2D g, String text, int x, int y, Color bgColor, Color textColor )
	{
		Rectangle2D textRect = GraphicsUtil.getStringBounds(g, text);
		x = (int)(x - textRect.getWidth()/2);
		GraphicsUtil.drawHint( g, text, x, y, bgColor, textColor );
	}

	Font bigFont = new Font("Arial", Font.BOLD , 16 );
	Font smallFont = new Font("Arial", Font.PLAIN , 8 );

	Stroke small = new BasicStroke(2.0f );
	Stroke big = new BasicStroke(3.0f );



	static DecimalFormat df = new DecimalFormat( "000" );

	public static String format( double d )
	{
		return df.format( d );
	}

	boolean testSubPartMode = false;
	boolean testAnimalIdentityMode = false;

	int display = 0;
	int maxDisplay = 10;

	@Override
	public void keyPressed(KeyEvent e, Point2D imagePoint, IcyCanvas canvas) {
		if ( e.getKeyChar()=='+' )
		{
			display++;
			if ( display > maxDisplay ) display = 0;
		}
		if ( e.getKeyChar() == '-' )
		{
			display--;
			if ( display < 0 ) display = maxDisplay;
		}
	}

	@Override
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas) {

		try{
			paint2( g , sequence , canvas );
		}catch( NullPointerException e )
		{
			System.err.println("Null pointer exception in TrackPoolOverlayDraw");
			e.printStackTrace();
		}

	}

	@Override
	public void mouseClick(MouseEvent e, Double imagePoint, IcyCanvas canvas) {

		if ( LiveMouseTracker.MODE_TEST_ANTENNA )
		{
			Antenna closestAntenna = LiveMouseTracker.rfidManager.getClosestAntenna( imagePoint.toPoint2D() );
			LiveMouseTracker.rfidManager.activateOnlyAntenna( closestAntenna );
		}

		/*
		if ( e.isControlDown() )
		{
			RFIDAntenna closestAntenna = LiveMouseTracker.rfidManager.getClosestAntenna( imagePoint.toPoint2D() );

			closestAntenna.setFaulty( );
			LiveMouseTracker.addEvent( new Event( "MAKE ANTENNA FAULTY", Color.white, closestAntenna.getLocation() ));

		}
		*/

		super.mouseClick(e, imagePoint, canvas);
	}

	boolean DRAW_HUD = false;
	boolean DRAW_RFID = false;
	boolean DRAW_FULL_DETECTION = false;

	private void updateDisplayMode() {

		if ( displayMode > 4 ) displayMode = 0;

		switch( displayMode )
		{
		case 0:
			DRAW_HUD=false;
			DRAW_RFID = false;
			DRAW_FULL_DETECTION = false;
			break;
		case 1:
			DRAW_HUD=false;
			DRAW_RFID = false;
			DRAW_FULL_DETECTION = true;
			break;
		case 2:
			DRAW_HUD=false;
			DRAW_RFID = true;
			DRAW_FULL_DETECTION = true;
			break;
		case 3:
			DRAW_HUD= true;
			DRAW_RFID = false;
			DRAW_FULL_DETECTION = true;
			break;
		case 4:
			DRAW_HUD= true;
			DRAW_RFID = true;
			DRAW_FULL_DETECTION = true;
			break;
		}
		this.painterChanged();

	}

	private void paint2(Graphics2D g, Sequence sequence, IcyCanvas canvas)  {

//		Chronometer paintChronometer = new Chronometer("Track pool overlay paint");

		Canvas2D ca = (Canvas2D) canvas;
		Graphics2D gAbsolute = (Graphics2D)g.create();
		gAbsolute.setFont( new Font("Arial", Font.BOLD , 12 ) );
		gAbsolute.transform( ca.getInverseTransform() );

//		g.drawString("+/- to change display.", 0, 0 );
//		g.drawString("current display: " + display + "/" + maxDisplay , 0, 10 );

		if ( testSubPartMode )
		{
			drawCenteredHint(gAbsolute,
					"*** SUBPART MODE TEST ON ***",
					canvas.getWidth()/2, 10, Color.red, Color.white);
		}

		if ( testAnimalIdentityMode )
		{
			drawCenteredHint(gAbsolute,
					"*** ANIMAL MODE TEST ON ***",
					canvas.getWidth()/2, 60, Color.red, Color.white);
		}

		String memoryTxt="";

		String cacheTxt="Track cache ";
		for ( Animal animal : LiveMouseTracker.trackContainer.animalTrackSegmentPool.animalList )
		{
			cacheTxt+= animal.getName() + ":"+animal.getTrackSegments().size()+" ";
		}
		cacheTxt+= "anonymous:"+LiveMouseTracker.trackContainer.anonymousTrackSegmentPool.trackSegmentArrayList.size();

		int availableMemory = (int)( ( SystemUtil.getJavaUsedMemory() ) / ( 1024 * 1024 ) );

		String availableMemoryString = String.format("%06d", availableMemory  );

		memoryTxt+= "Memory:"+ availableMemoryString +"/"+
					(int)( SystemUtil.getJavaMaxMemory() / ( 1024 * 1024 ) ) +"MB";
//		gAbsolute.setFont( new Font("Arial", Font.BOLD , 16 ) );
//		drawCenteredHint(gAbsolute, memoryTxt, canvas.getWidth()/2, 30, Color.black, Color.white);
//		gAbsolute.setFont( new Font("Arial", Font.BOLD , 18 ) );

		if ( DRAW_RFID )
		{
			drawRFIDAntenna( g );
		}

		if ( !LiveMouseTracker.getBackgroundHeightMapBuider().isReady() )
		{
			drawCenteredHint(gAbsolute,
					"*** STANDBY : Computing Background ***",
					canvas.getWidth()/2, 10, Color.red, Color.white);
		}

		int initLearningT = LiveMouseTracker.getInitLearningT();
		int currentT = LiveMouseTracker.getT();
		if ( currentT < initLearningT )
		{
			int frame = initLearningT - currentT;
			drawCenteredHint(gAbsolute,
					"*** Machine learning STANDBY : Init in " + frame + " frames ***",
					canvas.getWidth()/2, 30, Color.red, Color.white);
		}


		int cpu = LiveMouseTracker.getLastMainThreadComputationTimeMs();
		String cpuStr = format( cpu );

		{
			Color bgColor = Color.black;
			if ( cpu > 33 )
			{
				bgColor = Color.red.darker();
			}


			SimpleDateFormat dateFormatter = new SimpleDateFormat("E dd MMMM(MM) yyyy - HH:mm:ss", Locale.US );
			Date date = new Date();
			drawCenteredHint(gAbsolute,
					dateFormatter.format( date ) + " - " + memoryTxt,
					canvas.getWidth()/2, 0, Color.black , Color.white);

			/*
			drawCenteredHint(gAbsolute,
					dateFormatter.format( date ) + " - " + cacheTxt,
					canvas.getWidth()/2, 30, Color.black , Color.white);
			 */

			drawCenteredHint(gAbsolute,
					"temp:"+ LiveMouseTracker.sensorMonitor.getTemperature() + " �C "+
					"hum:"+LiveMouseTracker.sensorMonitor.getHumidity() + "% "+
					"snd:"+LiveMouseTracker.sensorMonitor.getSoundLevel() + " "+
					"light(all):"+LiveMouseTracker.sensorMonitor.getLightInfraredAndVisible() + " "+
					"light(vis):"+LiveMouseTracker.sensorMonitor.getLightVisible()
					,
					canvas.getWidth()/2, 18, Color.black , Color.white);


			Period diffStartAndCurrentTime = new Period( LiveMouseTracker.getStartTime() , new DateTime() );
//			SimpleDateFormat dateFormatter = new SimpleDateFormat("HH:mm:ss");
			PeriodFormatter formatter = new PeriodFormatterBuilder()
					.appendHours()
				    .appendSuffix("h")
				    .appendMinutes()
				    .appendSuffix("m")
				    .appendSeconds()
				    .appendSuffix("s")
				    .toFormatter();



			drawCenteredHint(gAbsolute,
					"t:"+LiveMouseTracker.getT() + " " + Util.getTimeStamp(LiveMouseTracker.getT()) +
					" rt:" + formatter.print( diffStartAndCurrentTime ) +
					" cpu:" + cpuStr +"ms "+SystemUtil.getCpuLoad()+"%"
					//" nbOver: " + LiveMouseTracker.nbOver
					//+" !proc: " + (int)( LiveMouseTracker.nbImageGrabbed-LiveMouseTracker.nbImageProcessed )
					,
					canvas.getWidth()/2, canvas.getHeight()-50, bgColor , Color.white);

			if( aviSoftInfoDisplayCounter > 0 )
			{
				drawCenteredHint(gAbsolute,
						"aviSoft:"+aviSoftInfoString,
						canvas.getWidth()/2, canvas.getHeight()-80, bgColor , Color.orange);
			}
			
			if( UDPEventInfoStringDisplayCounter > 0 )
			{
				drawCenteredHint(gAbsolute,
						"UDP Event:"+UDPEventInfoString,
						canvas.getWidth()/2, canvas.getHeight()-80, bgColor , Color.yellow);
			}
			
			if( RFIDStopEventDisplayCounter > 0 )
			{
				drawCenteredHint(gAbsolute,
						"RFID STOPPED: "+RFIDStopEventInfoString,
						canvas.getWidth()/2, canvas.getHeight()-100, bgColor , Color.red);
			}


			
			
			if( LiveMouseTracker.LOCK_BACKGROUND )
			{
				drawCenteredHint(gAbsolute,
						"BG Locked",
						canvas.getWidth()/4, canvas.getHeight()-80, bgColor , Color.orange);
			}

		}

		if ( getLooseTrackCounter() > 0 )
		{
			GraphicsUtil.drawHint(g, "FORCE MISS DETECTION \n remaining frames:" + getLooseTrackCounter(), 100, 50, Color.BLACK, Color.yellow);
		}

		ArrayList<AbsoluteHint> absoluteHintArrayList = LiveMouseTracker.getAbsoluteHintArrayList();
		synchronized ( absoluteHintArrayList ) {
			for ( AbsoluteHint absoluteHint : absoluteHintArrayList )
			{
				if ( absoluteHint.centerx )
				{
					drawCenteredHint( gAbsolute , absoluteHint.text, canvas.getWidth()/2, (int)absoluteHint.y, absoluteHint.backGroundColor, absoluteHint.textColor );
				}else
				{
					GraphicsUtil.drawHint( gAbsolute , absoluteHint.text, (int)absoluteHint.x, (int)absoluteHint.y, absoluteHint.backGroundColor, absoluteHint.textColor );
				}
			}
		}

		// Draw from error track pool
		{
			AnonymousPool errorTrackPool = LiveMouseTracker.errorDetectionTrackPool;

			if ( errorTrackPool == null ) return;

			Stroke bigStroke = new BasicStroke(3.0f );
			g.setStroke( bigStroke );

			for ( TrackSegment ts : errorTrackPool.getTrackSegments() )
			{
				MouseDetection previous = null;
				for ( MouseDetection detection : ts.getDetectionList() )
				{
					if ( previous != null )
					{
						Line2D line = new Line2D.Double(
								previous.getMassCenter().toPoint2D() ,
								detection.getMassCenter().toPoint2D() );
						g.setColor( ts.getColor() );
						g.draw( line );
					}
					previous = detection;
				}
				if ( previous != null )
				{
					g.drawString("Error track: (l="+ts.getLength() + ")" , (int)previous.getMassCenter().getX(), (int)previous.getMassCenter().getY() );
				}
			}
		}

		// Draw from anonymous track segment
		if ( LiveMouseTracker.DRAW_ANONYMOUS_TRACKS && DRAW_FULL_DETECTION )
		{
			AnonymousPool tp = anonymousTrackPool;

			if ( tp == null ) return;

			g.setStroke( big );

			for ( TrackSegment ts : tp.getTrackSegments() )
			{
				MouseDetection previous = null;
				for ( MouseDetection detection : ts.getDetectionList() )
				{
					if ( previous != null )
					{
						Line2D line = new Line2D.Double(
								previous.getMassCenter().toPoint2D(),
								detection.getMassCenter().toPoint2D() );
						g.setColor( Color.white );
						g.draw( line );
					}
					previous = detection;

//					if ( detection.getT() == LiveMouseTracker.getT() )
//					{
//						detection.paint( g,  canvas );
//					}

				}
//				drawMustBeCanBe(ts, g );
			}
		}
//
		if ( !DRAW_FULL_DETECTION )
		{
			for ( MouseDetection detection : animalPool.getAllDetectionAt( LiveMouseTracker.getT() ) )
			{
				detection.paintNose(g , canvas);
			}
		}
//			if ( DRAW_FULL_DETECTION )
//			{
//				detection.paint( g, canvas);
//			}
//			/*else
//			{
//				detection.paintNose(g , canvas);
//			}*/
//		}


		// Draw from Animals
		if ( DRAW_FULL_DETECTION )
		{

			AnimalPool animalPool = this.animalPool;
			if ( animalPool == null ) return;

			int t = clock.getT() ;

			int y = 100;
			for ( Animal animal : animalPool.getAnimalListActive() )
			{				
				GraphicsUtil.drawHint( g, animal.getRfidID(), 100, y, Color.black, Color.orange );
				y+=20;
			}
			
			for ( Animal animal : animalPool.getAnimalList() )
			{
//				System.out.println("paint Animal " + animal.getName() );
				MouseDetection lastDetection = null;
				g.setColor( animal.getColor() );
				for ( TrackSegment ts : animal.getTrackSegments() )
				{
					if ( ts.getLastTimePoint() < t - LiveMouseTracker.NBFRAME_TRACK_WINDOW_DISPLAY ) continue;

					GeneralPath path = new GeneralPath();
//					GeneralPath headPath = new GeneralPath();

					for ( MouseDetection detection : ts.getDetectionList() )
					{
						if ( detection == null )
							{
							System.err.println("Warning: Null detection in tracksegment list while painting");
							continue;
							}

						if ( detection.getT() < t- LiveMouseTracker.NBFRAME_TRACK_WINDOW_DISPLAY ) continue;

						// draw mass center trajectory
						if ( path.getCurrentPoint() == null )
						{
							path.moveTo( detection.getMassCenter().getX() , detection.getMassCenter().getY() );
						} else
						{
							path.lineTo( detection.getMassCenter().getX() , detection.getMassCenter().getY() );
						}

						if ( lastDetection == null || lastDetection.getT() < detection.getT() )
						{
							lastDetection = detection;
						}
					}

					g.setStroke( big );
					g.setColor( Color.black );
					g.draw( path );
					g.setStroke( small );
					g.setColor( animal.getColor() );
					g.draw( path );

//					ChronoStop chronoStop = new

//					drawMustBeCanBe( ts , g ) ;
				}
				// Debug speed
//
//				MouseDetection detectionAprev = animal.getDetectionAt( t - ChronoConstant.FRAME_WINDOW - 4 ); // -4 to be in past else this will never show as we are already at t
//				MouseDetection detectionAnext = animal.getDetectionAt( t + ChronoConstant.FRAME_WINDOW - 4 );
//				MouseDetection detectionAcurrent = animal.getDetectionAt( t -4  );
//				//String speed in pix/s
//				String speedInPxPerSecond ="";
//				if( detectionAprev !=null && detectionAnext != null && detectionAcurrent != null  )
//				{
//					float distance = (float)detectionAprev.getMassCenter().toPoint2D().distance( detectionAnext.getMassCenter().toPoint2D() );
//					float speed = distance / (float)( ChronoConstant.FRAME_WINDOW *2 +1 );
//					speedInPxPerSecond = "" + speed;
//
//					//System.out.println("Animal " + animal + " / Speed = " + speed );
//					String out = "Animal " + animal + " / Speed(-4) = " + speed;
//					g.drawString( out, (int)detectionAcurrent.getMassCenter().getX(),
//							(int)detectionAcurrent.getMassCenter().getY() );
//				}

//				System.out.println("LAST DETECTION: " + lastDetection );
				if ( lastDetection != null )
				{
					if ( lastDetection.getT() == LiveMouseTracker.getT() )
					{
//						g.drawString( ""+animal.getName(),
//								(int)lastDetection.getMassCenter().getX(),
//								(int)lastDetection.getMassCenter().getY() );
						lastDetection.paint( g , canvas);
//						g.drawString( speedInPxPerSecond, (int)lastDetection.getMassCenter().getX(), (int)lastDetection.getMassCenter().getY() );
					}
					{
						g.setColor( animal.getColor() );
						TrackSegment trackContainingDetection = animal.getTrackContainingDetection( lastDetection );
						g.drawString( 
								animal.getName() + " " + 
						trackContainingDetection.nbFrameSinceLastRFIDReading
								//trackContainingDetection.getIdentityAffectedBy()
						//+" " + trackContainingDetection.getOrientationAffectedBy()
						,
						(int)lastDetection.getMassCenter().getX(),
						(int)lastDetection.getMassCenter().getY() );
					}
				}


			}
			
			
			
		}

		// Draw track as a time line.
		boolean DRAW_TIMELINE = false;
		if ( DRAW_TIMELINE )
		{
			int tWindow = 30;
			Rectangle2D displayRect = new Rectangle2D.Double( 0,  10 , 500, 100 );
			int stepX = 20;
			int y = (int) displayRect.getMinY();

			// tracks from animals
			{
				AnimalPool animalPool = this.animalPool;
				for ( Animal animal : animalPool.getAnimalList() )
				{
					g.setColor( animal.getColor() );
					g.drawString( ""+animal, (int)displayRect.getMaxX(), y );
					ArrayList<TrackSegment> trackList = animal.getTracks( currentT- tWindow , currentT );
					for ( TrackSegment ts : trackList )
					{
						drawTrackLine( g, ts , y , animal.getColor() ,
								displayRect, currentT , tWindow , stepX );
						//					drawMustBeCanBe( ts , g ) ;
					}
					y+=10;
				}
			}
			// tracks from anonymous
			{
				ArrayList<TrackSegment> trackList = anonymousTrackPool.getTracks(  currentT- tWindow , currentT );

				for ( TrackSegment ts : trackList )
				{
					drawTrackLine( g, ts , y , ts.getColor() ,
							displayRect, currentT , tWindow , stepX );
					//					drawMustBeCanBe( ts , g ) ;
					y+=10;
					
					try
					{
						MouseDetection lastDetection = ts.getDetection( ts.getLastTimePoint() );
						g.drawString( "" + ts.nbFrameSinceLastRFIDReading,
								(int)lastDetection.getMassCenter().getX(),
								(int)lastDetection.getMassCenter().getY() );
					}catch( Exception e )
					{
						
					}
				}
			}

		}

		/*
		// draw nose-nose events.
		for ( MouseDetection d1 : animalPool.getAllDetectionAt( LiveMouseTracker.getT() ) )
		{
			for ( MouseDetection d2 : animalPool.getAllDetectionAt( LiveMouseTracker.getT() ) )
			{
				if ( d1 == d2 ) continue;
				if ( d1.getFrontPoint() == null ) continue;
				if ( d2.getFrontPoint() == null ) continue;


				if( d1.getFrontPoint().toPoint2D().distance( d2.getFrontPoint().toPoint2D() )
						< ChronoConstant.MAX_DISTANCE_HEAD_HEAD_GENITAL_THRESHOLD )
				{
					drawCenteredHint(g, "Nose-nose event", 512/2, 100, Color.black, Color.yellow );

					double x = ( d1.getFrontPoint().getX() + d2.getFrontPoint().getX() ) /2f;
					double y = ( d1.getFrontPoint().getY() + d2.getFrontPoint().getY() ) /2f;
					g.drawOval((int)x-10, (int)y-10, 20, 20);

				}


			}
		}
		*/

		gAbsolute.scale( 1 , 1 );
		gAbsolute.translate(10, 50 );

		//boolean DRAW_HUD = true;
		if ( DRAW_HUD )
		{
			int hudIndex = 0;
			for ( Animal animal : animalPool.getAnimalListActive() )
			{
				Graphics2D gHud = (Graphics2D) g.create();

				//switch( animalPool.getAnimalList().indexOf( animal ) )
				switch( hudIndex )
				{
				case 0:
					gHud.translate( 0 , 50 );
					break;
				case 1:
					gHud.translate( 0 , 230 );
					break;
				case 2:
					gHud.translate( 430 , 50 );
					break;
				case 3:
					gHud.translate( 430 , 230 );
					break;
				}

				// draw zoom of the animal
				MouseDetection lastDetection = animal.getDetectionAt( animal.getLastTimePoint() );
				/*
			for ( TrackSegment ts : animal.getTrackSegmentList() ) // FIXME: cost too much !
			{
				if ( lastDetection == null )
				{
					lastDetection = ts.getDetection( ts.getLastTimePoint() );
				}
				if ( lastDetection != null )
				{
					if ( ts.getLastTimePoint() >
					lastDetection.getT() )
					{
						lastDetection = ts.getDetection( ts.getLastTimePoint() );
					}
				}
			}*/

				if ( lastDetection == null ) continue;

				gHud.setColor( Color.gray );
				gHud.fillRect( -10, 0, 95, 175 );

				// draw mouse and clean rotation.

				IcyBufferedImage infraRendered =
						IcyBufferedImageUtil.getSubImage( LiveMouseTracker.getInfraOut().getImage( 0, 0),
								(int)lastDetection.getMassCenter().getX()-50,
								(int)lastDetection.getMassCenter().getY()-50,
								100, 100 );

				BufferedImage scaledImage = null;
				try{
					scaledImage = IcyBufferedImageUtil.getARGBImage( infraRendered ,
							LiveMouseTracker.getInfraOut().getFirstViewer().getLut() );
				}catch( NullPointerException e) {};

				//AffineTransform transform = gHud.getTransform();
				Graphics2D gHudRotated = (Graphics2D) gHud.create();

				if ( lastDetection.getFrontPoint() != null && lastDetection.getBackPoint() != null )
				{
					Point2D nose = lastDetection.getFrontPoint().toPoint2D();
					Point2D tail = lastDetection.getBackPoint().toPoint2D();
					if ( nose != null && tail != null )
					{
						double angle = Math.atan2( nose.getY() - tail.getY() , nose.getX() - tail.getX() );
						gHudRotated.translate( -10 , 10 );
						gHudRotated.rotate( -angle - Math.PI/2d, 50 , 50 );
					}
				}


				Shape clip = gHudRotated.getClip();
				Ellipse2D oval = new Ellipse2D.Double( 0, 0, 100, 100 );
				gHudRotated.setClip( oval );
				if ( scaledImage != null )
				{
					gHudRotated.drawImage( scaledImage , null, 0, 0 );
				}
				gHudRotated.setClip( clip );

				gHudRotated.setStroke( new BasicStroke( 2 ) );
				gHudRotated.setColor( animal.getColor() );

				/*
			if ( lastDetection.getT() == LiveMouseTracker.getT() )
			{
				gHudRotated.setColor( animal.getColor() );
			}else
			{
				gHudRotated.setColor( Color.black );
			}
				 */

				gHudRotated.draw( oval );

				{ // draw detection
					Graphics2D gTmp = (Graphics2D) gHudRotated.create();
					gTmp.translate( -lastDetection.getMassCenter().getX()+50, -lastDetection.getMassCenter().getY()+50 );
					lastDetection.paint( gTmp , canvas );
				}

				//			{
				//				// draw knowledge % bar
				//				int nbDetection = animal.getNumberOfLearningDetection();
				//				double ratio = (double)nbDetection / (double)LiveMouseTracker.LEARNING_NB_DETECTION_FOR_LEARNING_PER_ANIMAL;
				//				ratio *=100;
				//				if ( ratio > 100 ) ratio = 100;
				//				gAbsolute.fillRect( x+0, y + ( 100 - (int) ratio ) , 3 ,  (int)ratio );
				//			}

				gHud.setFont( bigFont );
				gHud.setColor( animal.getColor() );
				gHud.drawString( animal.getName(), -5, 65);
				gHud.setFont( smallFont );

				if ( animal.getRfidID() == null )
				{
					gHud.setColor( Color.white );
					gHud.drawString( "No RFID assigned", -5, 120);
				}else
				{
					gHud.setColor( Color.white );
					gHud.drawString( "RFID:"+animal.getRfidID(), -5, 120);
				}
				//			gAbsolute.drawString( "mVol :"+ (int)animal.getMeanVolume(), x+2, y+90);   // too heavy
				//			gAbsolute.drawString( "mSurf:"+ (int)animal.getMeanSurface(), x+2, y+100); // too heavy
				{
					gHud.setColor( Color.black );
					if ( lastDetection.isRearing() )
					{
						gHud.setColor( Color.white );
					}
					gHud.drawString("Rearing", 37 , 135 );
				}

				{
					gHud.setColor( Color.black );
					if ( lastDetection.isLookingUp() )
					{
						gHud.setColor( Color.white );
					}
					gHud.drawString("Look Up", 37 , 150 );
				}

				{
					gHud.setColor( Color.black );
					if ( lastDetection.isLookingDown() )
					{
						gHud.setColor( Color.white );
					}
					gHud.drawString("Look Down", 37 , 165 );
				}

				{
					gHud.setColor( Color.black );
					if ( lastDetection.canBeUsedForLearning() )
					{
						gHud.setColor( Color.white );
					}
					gHud.drawString("Learn", -5 , 170 );
				}

				{
					gHud.setColor( Color.red );

					int nb = 0;
					try{
						nb = animal.getMachineLearningSubPartDataSet().numInstances();
					}catch( NullPointerException  e )
					{}
					if ( nb > LiveMouseTracker.MIN_TO_START_USING_MACHINE_LEARNING_NB_DETECTION_FOR_LEARNING_SUBPARTS_PER_ANIMAL )
					{
						gHud.setColor( Color.green );
					}
					gHud.drawString( "HML "+ nb , 37 , 174 );
				}

				// draw z profile
				//			System.out.println("---");
				{
					Graphics2D gProfilePanel = (Graphics2D)gHud.create();
					gProfilePanel.translate( -5 , 55 );
					float scale = 0.25f;
					gProfilePanel.setColor( Color.white );
					double lengthFactor = lastDetection.getZSpine().lengthInMM / Spine.NB_SPINE_POINT ;
					// scale grid
					{
						g.setColor( Color.pink );

						for ( int i = 100 ; i >= 0 ; i-=50 )
						{
							gProfilePanel.drawLine( 83-83 , 100-(int)(i*scale) , 120-83, 100-(int)(i*scale) );
						}
					}

					gProfilePanel.setColor( Color.black );
					for ( int i = 0 ; i < lastDetection.getZSpine().z.length ; i++ )
					{
						int z = (int)( lastDetection.getZSpine().z[i] * scale );
						gProfilePanel.drawRect( 85+(int)(i*lengthFactor*scale)-83, 100-z, 1, z );
					}
					// draw specific points.
					//				System.out.println("--ssp");
					for ( SpineSpecialPoint ssp : lastDetection.getZSpine().spineSpecialPointList )
					{
						gProfilePanel.setColor( ssp.color );
						int i = ssp.spineIndex;
						//					System.out.println(i);
						int z = (int)( lastDetection.getZSpine().z[i] * scale );
						//gAbsolute.drawRect(x+85+i, y+100-z, 1, z );
						Ellipse2D e = new Ellipse2D.Double( 85+(int)(i*lengthFactor*scale)-1-83, 100-z-1,3,3 );
						gProfilePanel.draw( e );
					}


				}
				hudIndex++;
			}

		}

		try
		{
//			g.setColor( Color.gray );
//			g.fillRect( 420, 400, 95, 30 );
//			dg.setColor( Color.white );
//			g.drawString("#Particles:"+ LiveMouseTracker.currentFrameInfo.getNbParticle() , 435, 415 );
		}catch( NullPointerException e ) {};

//		drawCenteredHint(gAbsolute,
//				"Identity decisions: Manual: " + LiveMouseTracker.number_of_manual_correction
//				+ " Auto: " + LiveMouseTracker.number_of_auto_correction,
//				canvas.getWidth()/2, -30, Color.black, Color.white);

//		paintChronometer.displayMs();
	}

	String aviSoftInfoString = "";
	/** If the counter is <= 0, don't display */
	int aviSoftInfoDisplayCounter = 0;

	public void setAvisoftInfoString( String aviSoftInfoString )
	{
		aviSoftInfoDisplayCounter = 30;
		this.aviSoftInfoString = aviSoftInfoString;
	}
	
	String UDPEventInfoString = "";
	/** If the counter is <= 0, don't display */
	int UDPEventInfoStringDisplayCounter = 0;

	public void setUDPEventInfoString( String UDPEventInfoString )
	{
		UDPEventInfoStringDisplayCounter = 30;
		this.UDPEventInfoString = UDPEventInfoString;
	}
	
	String RFIDStopEventInfoString = "";
	/** If the counter is <= 0, don't display - RFID is locked if value > 0 */
	public int RFIDStopEventDisplayCounter = 0;

	public void setRFIDStopEventInfoString( String RFIDStopEventInfoString )
	{
		RFIDStopEventDisplayCounter = 30;
		this.RFIDStopEventInfoString = RFIDStopEventInfoString;
	}
	
	public void frameTick() // called at each frame
	{
		if ( aviSoftInfoDisplayCounter > 0 )
		{
			aviSoftInfoDisplayCounter--;
		}
		if ( UDPEventInfoStringDisplayCounter > 0 )
		{
			UDPEventInfoStringDisplayCounter--;
		}
		if ( RFIDStopEventDisplayCounter > 0 )
		{			
			RFIDStopEventDisplayCounter--;
		}
	}

	private void drawRFIDAntenna( Graphics2D g ) {

		if ( LiveMouseTracker.rfidManager != null )
		{
			for ( Antenna antenna : LiveMouseTracker.rfidManager.getAntennaList() )
			{
				antenna.paint( g );
			}
		}

	}

	private void drawTrackLine( Graphics2D g , TrackSegment ts, int y, Color color, Rectangle2D displayRect , int currentT , int tWindow , int stepX ) {

		try{
		GeneralPath path = new GeneralPath();
		g.setColor( color );

		for ( MouseDetection detection : ts.getDetectionList() )
		{
			if ( detection.getT() < currentT- tWindow ) continue;
			int x = (int)displayRect.getMaxX() - ( stepX * ( currentT - detection.getT() ) );

			if ( path.getCurrentPoint() == null )
			{
				g.drawString( ""+ ts , x, y);
				path.moveTo( x, y );
			}else
			{
				path.lineTo( x, y );
			}
		}

		g.setStroke( big );
		g.setColor( Color.black );
		g.draw( path );
		g.setStroke( small );
		g.setColor( color );
		g.draw( path );
		}catch( NullPointerException e) {
			System.err.println( "FIXME:Null pointer exception to correct");
			e.printStackTrace();
		}

	}

	private void drawMustBeCanBe(TrackSegment ts, Graphics2D g) {

		MouseDetection firstDetection = ts.getDetectionList().get(0 );
		// DRAW INFOS ON TRACK
		if ( firstDetection != null )
		{
			String infos ="*";
			if ( ts.getMustBeAnimal() != null )
			{
				infos += "Must be: " + ts.getMustBeAnimal() + " ";
			}
			if ( ts.getCannotBeAnimalList().size() != 0 )
			{
				infos+= "Cannot be: ";
				for ( Animal a : ts.getCannotBeAnimalList() )
				{
					infos += a;
				}
			}
			g.drawString( ""+ infos ,
					(int)firstDetection.getMassCenter().getX(), (int)firstDetection.getMassCenter().getY() );
		}

	}

	private void plotGraph(Graphics2D g, IdentityResult identityResult, double x, double y) {

		ArrayList<Animal> animalList = LiveMouseTracker.getMainAnimalPool().getAnimalList();
		int BAR_HEIGHT = 20;
		int BAR_WIDTH = 4;

		for ( int i = 0 ; i< animalList.size() ; i++ )
		{
			g.setColor( animalList.get( i ).getColor() );
			int barHeight = (int)( identityResult.proba[i] * BAR_HEIGHT );
			g.fillRect( (int)x+BAR_WIDTH*i , (int)y+BAR_HEIGHT-barHeight , BAR_WIDTH, barHeight );
		}
		g.setColor( Color.WHITE );
		g.drawRect( (int)x, (int)y, animalList.size()*BAR_WIDTH, BAR_HEIGHT );

	}

	/* if >0 then the live should forget detection to mimic lost of detection */
	int looseTrackCounter = 0;

	public int getLooseTrackCounter() {
		return looseTrackCounter;
	}

	public void setLooseTrackCounter(int looseTrackCounter) {
		this.looseTrackCounter = looseTrackCounter;
	}

	public void decreaseLooseTrackCounter()
	{
		looseTrackCounter--;
		if ( looseTrackCounter < 0 ) looseTrackCounter = 0;
	}

	int letterIndex( char c )
	{
		if(c>='A' && c<='F')
		{
			return ((int)c - 'A' );
		}
		if(c>='a' && c<= 'f')
		{
			return ((int)c - 'a' );
		}
	    return -1;
	}

	@Override
	public void mouseClick(MouseEvent e, Point2D imagePoint, IcyCanvas canvas) {

//		if ( e.isShiftDown() )
//		{
//			RFIDEvent rfidEvent = new RFIDEvent( LiveMouseTracker.getT(), 6 , "955000004064575", imagePoint , 10 );
//			LiveMouseTracker.rfidManager.addEventToQueue( rfidEvent );
//			Event event = new Event("Artificial RFID", Color.PINK, imagePoint );
//			LiveMouseTracker.addEvent( event );
//		}


	}

	int displayMode = 4;

	@Override
	public void keyPressed(KeyEvent e, Double imagePoint, IcyCanvas canvas) {

//		if ( e.getKeyChar() == 'i' )
//		{
//			ArrayList<MouseDetection> detectionList = LiveMouseTracker.trackContainer.getAllDetectionAt( LiveMouseTracker.getT() );
//			for ( MouseDetection mouseDetection : detectionList )
//			{
//				Sequence sequence = new Sequence( mouseDetection.infraPatch );
//				sequence.setName("patch");
//				Sequence sequence2 = new Sequence( mouseDetection.getInfraPatchRotated( mouseDetection.angle ) );
//				sequence2.setName( "rotated");
//				Icy.getMainInterface().addSequence(sequence);
//				Icy.getMainInterface().addSequence(sequence2);
//
//			}
//		}

		if ( e.getKeyChar() == 'd' )
		{
			displayMode++;
			updateDisplayMode();
			return;
		}
	

		if ( e.getKeyChar() == '*' )
		{
			LiveMouseTracker.LOCK_BACKGROUND =!LiveMouseTracker.LOCK_BACKGROUND;
			System.out.println("Lock background mode: " + LiveMouseTracker.LOCK_BACKGROUND );
			return;
		}


//		if ( e.getKeyChar() == '.' )
//		{
//			LiveMouseTracker.MODE_TEST_ANTENNA = !LiveMouseTracker.MODE_TEST_ANTENNA;
//			System.out.println("MODE_TEST_ANTENNA: " + LiveMouseTracker.MODE_TEST_ANTENNA );
//			return;
//		}



//		if ( e.getKeyChar() == '*' )
//		{
//			testSubPartMode = !testSubPartMode;
//			painterChanged();
//			return;
//		}

//		if ( e.getKeyChar() == '/' )
//		{
//			testAnimalIdentityMode = !testAnimalIdentityMode;
//			painterChanged();
//			return;
//		}

		if ( e.getKeyCode() == KeyEvent.VK_ENTER )
		{
			// action disabled (for dev)
			//LiveMouseTracker.getKinectStreamer().play();
			return;
		}

//		if ( e.getKeyChar() =='t')
//		{
//			getTrackIndentityEvaluation( imagePoint );
//			return;
//		}

		if ( e.getKeyChar() =='r')
		{
			System.out.println("reset background and antennas");
			LiveMouseTracker.resetBackGround();
			LiveMouseTracker.rfidManager.switchOffAllAntennas();
			return;
		}

//		if ( e.getKeyChar() =='z')
//		{
//			LiveMouseTracker.cacheAllAnimalMachineLearning( true );
//			return;
//		}


//		if ( e.getKeyChar() =='m')
//		{
//			looseTrackCounter = 10;
//			painterChanged();
//			return;
//		}



//		if ( e.getKeyChar() == 'v')
//		{
//			getVolumeInfo( imagePoint.toPoint2D() );
//			return;
//		}

//		if ( e.getKeyChar() == 's')
//		{
//			//getSymetry( imagePoint.toPoint2D() , true );
//
//			// swap 2 animals.
//
//			System.out.println("SWAP animals");
//			Animal a = LiveMouseTracker.trackContainer.animalTrackSegmentPool.animalList.get( 0 );
//			TrackSegment ta = a.getTrackContaining( LiveMouseTracker.getT()-1 );
//
//			Animal b = LiveMouseTracker.trackContainer.animalTrackSegmentPool.animalList.get( 1 );
//			TrackSegment tb = b.getTrackContaining( LiveMouseTracker.getT()-1 );
//
//			if ( ta == null || tb == null )
//			{
//				System.out.println("SWAP : null tracks.");
//				return;
//			}
//			LiveMouseTracker.trackContainer.setTrackAnonymous( a , ta );
//			LiveMouseTracker.trackContainer.setTrackAnonymous( b , tb );
//
//			LiveMouseTracker.trackContainer.setTrackIdentity( ta , b );
//			LiveMouseTracker.trackContainer.setTrackIdentity( tb , a );
//
//			LiveMouseTracker.number_of_manual_correction ++;
//
//			return;
//		}

//		if ( e.getKeyChar() == 'x')
//		{
//			getSymetryInArea( imagePoint.toPoint2D() );
//			return;
//		}

//		if ( e.getKeyChar() == '-')
//		{
//
//			for ( Animal animal : animalPool.animalList )
//			{
//				LiveMouseTracker.trackContainer.setTrackAnonymous(
//						animal, LiveMouseTracker.getT() - 250 , LiveMouseTracker.getT() );
//				System.out.println("Making anonymous last 250 frames");
//			}
//
////			System.out.println("Move all track to anonymous");
////			for ( Animal animal : LiveMouseTracker.getMainAnimalPool().animalList )
////			{
////				for ( TrackSegment ts : animal.getTrackSegmentList() )
////				{
////					LiveMouseTracker.trackContainer.setTrackAnonymous(animal, ts);
////				}
////			}
////			painterChanged();
////			return;
//		}

		// Set the track under the cursor to a specific animal.
//		if ( e.isShiftDown() )
//		{
//			System.out.println("Forcing Animal Segment identity.");
////			forceClosestAnimalTrack( imagePoint, e );
//		}else
//		{
//			System.out.println("Forcing TrackPool Segment identity.");
//			forceClosestUnidentifiedTrack( imagePoint, e );
//		}



		painterChanged();

	}

	/**
	 * Force a track in the track pool of an animal ( in the last 10 seconds )
	 */
//	private void forceClosestAnimalTrack(Double imagePoint, KeyEvent e) {
//
//		// search closest track in animalpool
//		TrackSegment bestTrack = LiveMouseTracker.getMainAnimalPool().getClosestTrack( imagePoint.toPoint2D() , 10 * 30 );
//
//		int animalIndex = 0;
//		animalIndex = letterIndex( e.getKeyChar() );
//
//		if ( animalIndex < 0 )
//		{
//			System.out.println("Can't process key >" + e.getKeyChar() );
//			return;
//		}
//
//		if ( bestTrack == null )
//		{
//			System.out.println("Can't find animal track.");
//			return;
//		}
//
//		System.out.println("Giving index to track: " + animalIndex );
//
//		Animal targetAnimal = animalPool.getAnimalList().get( animalIndex );
//		Animal sourceAnimal = animalPool.getAnimalOwningTrack( bestTrack );
//
//		System.out.println("Source animal is " + sourceAnimal.getName() );
//		// As we force an identity, we check if no existing concurrent identity exists in the target animal.
//		// If they exists, we remove them from the animal an move it to the trackSegmentPool with no Id.
//
//
//		for ( TrackSegment track : targetAnimal.getTrackSegmentList() )
//		{
//			if ( bestTrack.overlapInT( track ) )
//			{
//				System.out.println("The animal is " + targetAnimal.getName() );
//				System.out.println("A track is overlapping " + track );
//
//				targetAnimal.removeTrackSegment( track );
//				anonymousTrackPool.add( track );
//			}
//		}
//
//		sourceAnimal.removeTrackSegment( bestTrack );
//		targetAnimal.addTrackSegment( bestTrack );
//
//	}



	/**
	 * Force a track in the track pool of unidentified track to be assigned to an animal.
	 */
	private void forceClosestUnidentifiedTrack(Point5D imagePoint, KeyEvent e) {

		TrackSegment bestTrack = anonymousTrackPool.getClosestTrack( imagePoint.toPoint2D() );

		int animalIndex = 0;

		animalIndex = letterIndex( e.getKeyChar() );
		if ( animalIndex < 0 )
		{
			System.out.println("Can't process key >" + e.getKeyChar() );
			return;
		}

		if ( bestTrack == null )
		{
			System.out.println("Can't find track.");
			return;
		}

		System.out.println("Giving index to track: " + animalIndex );
		Animal animal = animalPool.getAnimalList().get( animalIndex );
		animal.addTrackSegment( bestTrack );
		anonymousTrackPool.removeTrack( bestTrack );

	}

	private void getSymetryInArea(Point2D point2d) {

		Point2D bestPoint = null;
		int bestScore = Integer.MAX_VALUE;

		for ( int x= -5 ; x < 5 ; x++ )
			for ( int y= -5 ; y < 5 ; y++ )
		{
				Point2D p = new Point2D.Double( point2d.getX() + x , point2d.getY() + y );

				Score score = getSymetry( p , false );

			if ( score.score < bestScore )
			{
				bestScore = score.score;
				bestPoint = p;
			}
		}

		getSymetry( bestPoint , true );



	}

	private Score getSymetry(Point2D point2d , boolean drawResult ) {

		// create buffer

		IcyBufferedImage image = LiveMouseTracker.depthImage;

		ROI2D roiMask = null;
		double roiDistance= java.lang.Double.MAX_VALUE;
		for ( ROI2D roi : LiveMouseTracker.getInfraOut().getROI2Ds() )
		{
			if ( roi.getName().startsWith("seg ok"))
			{
				double distance = ROIUtil.getMassCenter( roi ).distance( point2d );
				if ( distance < roiDistance )
				{
					roiDistance = distance;
					roiMask = roi;
				}
			}

		}

		short[] buffer = new short[21*21];
		boolean[] maskBuffer = new boolean[21*21];

		for ( int x = 0 ; x < 21 ; x++ )
		{
			for ( int y = 0 ; y < 21 ; y++ )
			{
				int px = (int)(point2d.getX()+ x-10);
				int py = (int)(point2d.getY() + y-10);
				buffer[y*21 + x] =
						image.getDataAsShort( px, py , 0 );
				maskBuffer[ y*21 + x ] = roiMask.contains( px , py );
			}
		}

		Score score = LiveMouseTracker.symetryAngleFinder.score( buffer , maskBuffer );

		if ( drawResult )
		{
			System.out.println( score );
			ROI2DLine lineRoi = new ROI2DLine( point2d,
					new Point2D.Double(
							(int)(point2d.getX() + (Math.cos( score.angle ) * 11d)) ,
							(int)(point2d.getY() + (Math.sin( score.angle ) * 11d))
							)
					);
			lineRoi.setColor( Color.MAGENTA );
			lineRoi.setName("tmp angle");
			LiveMouseTracker.addROIToInfraSequence( lineRoi );
		}

		return score;

	}

	/*
	private void getTrackIndentityEvaluation(Point5D imagePoint) {

		TrackSegment bestTrack = anonymousTrackPool.getClosestTrack( imagePoint.toPoint2D() );
		System.out.println("Giving track identity evaluation for track " + bestTrack + " from trackpool ");

		if( bestTrack != null )
		{
			for ( MouseDetection d : bestTrack.getDetectionList() )
			{

				ROI2DRectangle roi = new ROI2DRectangle( new Rectangle2D.Double(
						d.getMassCenter().getX()-1,
						d.getMassCenter().getY()-1 ,3,3 ) );
				roi.setName("tmp export");
				roi.setColor( Color.RED );

				Icy.getMainInterface().getActiveSequence().addROI( roi );
			}

			MachineLearningTrackIdentityThread mltit = new MachineLearningTrackIdentityThread(animalPool, bestTrack);
			mltit.start();

//			MachineLearningTrackIdentity mlti = new MachineLearningTrackIdentity( animalPool );
//			mlti.findIdentity( bestTrack );

		}

	}
	*/



	private void getVolumeInfo(Point2D  point ) {

		System.out.println("DEV: Get volume info in ROI labeled volinfo");

		Sequence sequence = Icy.getMainInterface().getActiveSequence();
		ArrayList<ROI2D> rois = sequence.getROI2Ds();

		TrackSegment ts = animalPool.getClosestTrack( point );

		for ( ROI2D roi : rois )
		{
			if ( roi.getName().startsWith("volinfo" ) )
			{
				System.out.println("Detection Volume");
				{
					for ( MouseDetection d : ts.getDetectionList() )
					{
						if ( roi.contains( d.getMassCenter().toPoint2D() ) )
						{
							System.out.println( "" + (int)d.getVolume() );

							ROI2DRectangle roi2 = new ROI2DRectangle( new Rectangle2D.Double(
									d.getMassCenter().getX()-1,
									d.getMassCenter().getY()-1 ,3,3 ) );
							roi.setColor( Color.RED );
							roi2.setName("tmp vol export");

							Icy.getMainInterface().getActiveSequence().addROI( roi2 );

						}

					}
				}
			}
		}

	}

	@Override
	public void mouseMove(MouseEvent e, Double imagePoint, IcyCanvas canvas) {

		if ( testAnimalIdentityMode )
		{

		}

		if ( testSubPartMode )
		{
			ArrayList<ROI2D> r2D = canvas.getSequence().getROI2Ds();
			for ( ROI2D roiCandidate : r2D )
			{
				if ( roiCandidate.getName().startsWith("tmp subparttest") )
				{
					canvas.getSequence().removeROI( roiCandidate );
				}
			}

			ROI2DRectangle roiRect = new ROI2DRectangle(
					imagePoint.getX()-12, imagePoint.getY()-12,
					imagePoint.getX()+12, imagePoint.getY()+12);
			ROI2DArea roi = new ROI2DArea( roiRect.getBooleanMask(true) );

			r2D = canvas.getSequence().getROI2Ds();
			for ( ROI2D roiCandidate : r2D )
			{
				if ( roiCandidate.contains( imagePoint.getX(), imagePoint.getY() ) )
				{
					roi = (ROI2DArea) roi.getIntersection( roiCandidate );
				}
			}

			roi.setName("tmp subparttest");
			roi.setColor( Color.yellow );
			canvas.getSequence().addROI( roi );

			SubPartDescriptor spd = new SubPartDescriptor();
			BooleanMask2D roiMask = roi.getBooleanMask(true);
			Point[] roiPoints = roiMask.getPoints();
			spd.infraHisto = MouseDetection.buildHistogram(roiPoints, LiveMouseTracker.infraImage, "infra subpart" );
			spd.depthHisto = MouseDetection.buildHistogram(roiPoints, LiveMouseTracker.depthImage, "depth subpart" );

			System.out.println("----");
			for ( Animal animal: LiveMouseTracker.getMainAnimalPool().animalList )
			{
				if ( animal.getMachineLearningSubPartDataSet() != null )
				{
					int nbInstance = animal.getMachineLearningSubPartDataSet().numInstances();
					System.out.println("Animal " + animal.getName() + " nbInstances sub part: " + nbInstance );

					java.lang.Double[] r = MouseDetection.checkSubPart( null , spd, animal , true );
					//System.out.println( r );
					if ( r != null )
					{
						System.out.println( "p(front)=" + r [0] + " p(back)=" + r [1] );
					}
				}


			}


		}
	}


}
