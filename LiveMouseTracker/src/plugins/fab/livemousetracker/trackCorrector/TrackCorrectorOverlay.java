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
package plugins.fab.livemousetracker.trackCorrector;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import icy.canvas.IcyCanvas;
import icy.painter.Overlay;
import icy.sequence.Sequence;
import plugins.fab.livemousetracker.Animal;
import plugins.fab.livemousetracker.detection.MouseDetection;
import plugins.fab.livemousetracker.experiment.Experiment;
import plugins.fab.livemousetracker.track.AnimalPool;
import plugins.fab.livemousetracker.track.TrackSegment;

public class TrackCorrectorOverlay extends Overlay {

	Experiment experiment;
	TrackCorrector trackCorrector;
	int t;

	public TrackCorrectorOverlay(Experiment experiment, TrackCorrector trackCorrector) {
		super("track corrector overlay");
		this.experiment = experiment;
		this.trackCorrector = trackCorrector;
	}

	public void setT(int t) {
		this.t = t;
	}

	Stroke small = new BasicStroke(3.0f , BasicStroke.CAP_BUTT , BasicStroke.JOIN_BEVEL );
	Stroke big = new BasicStroke(4.0f , BasicStroke.CAP_BUTT , BasicStroke.JOIN_BEVEL );

	Stroke smallTimeLine = new BasicStroke(6.0f , BasicStroke.CAP_BUTT , BasicStroke.JOIN_BEVEL );
	Stroke bigTimeLine = new BasicStroke(8.0f , BasicStroke.CAP_BUTT , BasicStroke.JOIN_BEVEL );

	TrackSegment selectedTrackSegment = null;

	@Override
	public void mouseMove(MouseEvent e, Point2D imagePoint, IcyCanvas canvas) {

		TrackSegment previousSelectedTrack = selectedTrackSegment;
		selectedTrackSegment = findTrack( imagePoint.getX(), imagePoint.getY() );
		if ( previousSelectedTrack != selectedTrackSegment )
		{
//			System.out.println("dif");
//			System.out.println("track selected: " + selectedTrackSegment );
			painterChanged();
		}

	}

	@Override
	public void mouseClick(MouseEvent e, Point2D imagePoint, IcyCanvas canvas) {

		if (e.isConsumed())
			return;

		if ((e.getClickCount() == 1) && (e.getButton() == MouseEvent.BUTTON3))
		{

			JPopupMenu popup = new JPopupMenu();
			popup.setLocation( (int)imagePoint.getX() , (int) imagePoint.getY() );

			Animal animalOwning = experiment.getAnimalPool().getAnimalOwningTrack( selectedTrackSegment );

			if ( animalOwning != null )
			{
				// track is owned by an animal
				popup.add( new JSeparator() );
				for ( Animal a : experiment.getAnimalPool().getAnimalList() )
				{
					if ( a != animalOwning )
					{
						JMenuItem item = new JMenuItem("Swap " + animalOwning + " with " + a + "(apply to futur)");
						popup.add( item );
					}
				}
			}else
			{
				// track is anonymous
				popup.add( new JMenuItem("Anonymous") );
			}

			popup.setLocation(e.getLocationOnScreen());
			popup.show(e.getComponent(), e.getX(), e.getY());

		}
	}

	@Override
	public void keyPressed(KeyEvent e, Point2D imagePoint, IcyCanvas canvas) {

		if ( e.getKeyChar() >= 'a' && e.getKeyChar() <='f' )
		{
			int animalIndex = e.getKeyChar() - 'a';
			//System.out.println("Animal index: " + animalIndex );
			//setIdentity( animalIndex );

			if ( experiment.getAnimalPool().getAnimalList().size() >= animalIndex+1 )
			{
				Animal targetAnimal = experiment.getAnimalPool().getAnimalList().get( animalIndex );
				setSelectedTrackIdentity( targetAnimal );
			}else
			{
				System.out.println("Animal index is unknown: index asked: " +animalIndex );
			}

			painterChanged();
		}

		if ( e.getKeyChar() == '0' )
		{
			setSelectedTrackIdentity( null );
			painterChanged();
		}

		if ( e.getKeyCode() == KeyEvent.VK_DELETE )
		{
			experiment.remove( selectedTrackSegment );
			painterChanged();
		}

		int offset = 1;
		if ( e.isShiftDown() )
		{
			offset = 10;
		}

		if ( e.getKeyCode() == KeyEvent.VK_LEFT )
		{
			trackCorrector.slider.setValue( trackCorrector.slider.getValue() - offset );
		}

		if ( e.getKeyCode() == KeyEvent.VK_RIGHT )
		{
			trackCorrector.slider.setValue( trackCorrector.slider.getValue() + offset );
		}

	}

	private void setSelectedTrackIdentity( Animal targetAnimal ) {

		experiment.setTrackIdentity( selectedTrackSegment.getDataBaseId() , targetAnimal );

	}

	private TrackSegment findTrack(double x, double y ) {

		TrackSegment closestTrack = null;
		double minDistance = Double.MAX_VALUE;

		for ( TrackSegment ts : experiment.getAnonymousTrackList() )
		{
			if ( canDisplayTrack ( ts, false ) )
			{
				for ( MouseDetection detection : ts.getDetectionList() )
				{
					if ( canDisplayDetection( detection, false ) )
					{
						double distance = detection.getMassCenter().toPoint2D().distance( x , y );
						if ( distance < minDistance )
						{
							closestTrack = ts;
							minDistance = distance;
						}
					}
				}
			}
		}

		for ( TrackSegment ts : experiment.getAnimalPool().getTrackSegments() )
		{
			if ( canDisplayTrack ( ts, false ) )
			{
				for ( MouseDetection detection : ts.getDetectionList() )
				{
					if ( canDisplayDetection( detection, false ) )
					{
						double distance = detection.getMassCenter().toPoint2D().distance( x , y );
						if ( distance < minDistance )
						{
							closestTrack = ts;
							minDistance = distance;
						}
					}
				}
			}
		}

		return closestTrack;

	}

	@Override
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas) {

//		Canvas2D c = (Canvas2D) canvas;
//		Graphics2D gAbsolute = (Graphics2D)g.create();
//		gAbsolute.setFont( new Font("Arial", Font.BOLD , 20 ) );
//		gAbsolute.transform( c.getInverseTransform() );
//
//		GraphicsUtil.drawHint( gAbsolute,
//				"Anonymous track remaining:" + experiment.getAnonymousTrackList().size(),
//				0 , canvas.getHeight()-60, Color.black, Color.white );

//		g.setColor( Color.yellow );
//		g.drawString( experiment.getBaseFolder() , 20, 20);
//		g.drawString( "current T: " + t , 20, 40);

		// draw tracks.

		// Draw from anonymous track segment
		{
			g.setStroke( big );

			for ( TrackSegment ts : experiment.getAnonymousTrackList() )
			{
				MouseDetection previous = null;

				if ( !canDisplayTrack( ts, false ) ) continue;

				for ( MouseDetection detection : ts.getDetectionList() )
				{
					if ( !canDisplayDetection( detection, false ) ) continue;

					if ( previous != null )
					{
						Line2D line = new Line2D.Double( previous.getMassCenter().toPoint2D() , detection.getMassCenter().toPoint2D() );
						g.setColor( ts.getColor() );
						if ( ts == selectedTrackSegment )
						{
							g.setColor( Color.white );
						}
						g.draw( line );
					}
					previous = detection;

					if ( detection.getT() == t )
					{
						detection.getROI2DArea().setColor( ts.getColor() );
						if ( ts == selectedTrackSegment )
						{
							detection.getROI2DArea().setColor( Color.white );
						}
						detection.getROI2DArea().getPainter().paint(g, sequence, canvas);
					}

				}

			}
		}

		// Draw from Animals
		{

			AnimalPool animalPool = experiment.getAnimalPool();

			for ( Animal animal : animalPool.getAnimalList() )
			{
				MouseDetection lastDetection = null;
				g.setColor( animal.getColor() );
				for ( TrackSegment ts : animal.getTrackSegments() )
				{
					if ( !canDisplayTrack( ts, false ) ) continue;

					GeneralPath path = new GeneralPath();

					for ( MouseDetection detection : ts.getDetectionList() )
					{
						if ( !canDisplayDetection( detection , false ) ) continue;

						if ( path.getCurrentPoint() == null )
						{
							path.moveTo( detection.getMassCenter().getX() , detection.getMassCenter().getY() );
						} else
						{
							path.lineTo( detection.getMassCenter().getX() , detection.getMassCenter().getY() );
						}

						lastDetection = detection;

						if ( detection.getT() == t )
						{
							detection.getROI2DArea().setColor( animal.getColor() );
							if ( ts == selectedTrackSegment )
							{
								detection.getROI2DArea().setColor( Color.white );
							}
							detection.getROI2DArea().getPainter().paint(g, sequence, canvas);
						}

					}

					g.setStroke( big );
					g.setColor( Color.black );
					if ( selectedTrackSegment == ts )
					{
						g.setColor( Color.white );
					}
					g.draw( path );
					g.setStroke( small );
					g.setColor( animal.getColor() );
					g.draw( path );

					//drawMustBeCanBe( ts , g ) ;
				}
				if ( lastDetection != null )
				{
					g.drawString( ""+animal.getName(),
							(int)lastDetection.getMassCenter().getX(),
							(int)lastDetection.getMassCenter().getY() );
				}

			}
		}

		// Draw track as a time line.
		{
			//int tWindow = 30;
			Rectangle2D displayRect = new Rectangle2D.Double( 0,  0 , 512, 100 );
			int stepX = 2;
			int y = (int) displayRect.getMinY();

			// tracks from animals
			{
				AnimalPool animalPool = experiment.getAnimalPool();
				for ( Animal animal : animalPool.getAnimalList() )
				{
					g.setColor( animal.getColor() );
					g.drawString( ""+animal, (int)displayRect.getMaxX(), y );
					ArrayList<TrackSegment> trackList = animal.getTrackSegments();
					for ( TrackSegment ts : trackList )
					{
						if ( !canDisplayTrack( ts , true ) ) continue;
						drawTrackLine( g, ts , y , animal.getColor() ,
								displayRect, animal, stepX );
						//					drawMustBeCanBe( ts , g ) ;
					}
					y+=10;
				}
			}

			// tracks from anonymous
			{
				ArrayList<TrackSegment> trackList = experiment.getAnonymousTrackList();

				for ( TrackSegment ts : trackList )
				{
					if ( !canDisplayTrack( ts , true ) ) continue;
					drawTrackLine( g, ts , y , ts.getColor() ,
							displayRect, null, stepX );
					//					drawMustBeCanBe( ts , g ) ;
					y+=10;
				}
			}

			// draw middle tick.
			Line2D line = new Line2D.Double( 256 , 0 , 256 , y );
			g.setColor( Color.black );
			g.setStroke( big );
			g.draw( line );
			g.setColor( Color.white );
			g.setStroke( small );
			g.draw( line );
		}


	}

	private void drawTrackLine( Graphics2D g , TrackSegment ts,
			int y, Color color, Rectangle2D displayRect , Animal animal , int stepX ) {

		GeneralPath path = new GeneralPath();
		g.setColor( color );

		for ( MouseDetection detection : ts.getDetectionList() )
		{
			if ( !canDisplayDetection( detection , true ) ) continue;

			int x = (int)displayRect.getMaxX() - ( stepX * ( t - detection.getT() ) ) - 256 ;

			if ( path.getCurrentPoint() == null )
			{
				if ( animal != null )
				{
					g.drawString( "" + animal.getName() , x, y);
				}
				path.moveTo( x, y );
			}else
			{
				path.lineTo( x, y );
			}
		}

		g.setStroke( bigTimeLine );
		g.setColor( Color.black );
		if ( ts == selectedTrackSegment )
		{
			g.setColor( Color.white );
		}
		g.draw( path );
		g.setStroke( smallTimeLine );
		g.setColor( color );
		g.draw( path );

	}


	/** shifted: shift of halt in future (used for timeline) */
	private boolean canDisplayDetection(MouseDetection detection , boolean shifted ) {

		int shift = 0;
		if ( shifted ) shift+= TrackCorrector.NB_FRAME_TO_DISPLAY / 2;
		if ( detection.getT() < t- TrackCorrector.NB_FRAME_TO_DISPLAY + shift ) return false;
		if ( detection.getT() > t + shift ) return false;
		return true;

	}

	/** shifted: shift of halt in future (used for timeline) */
	private boolean canDisplayTrack(TrackSegment ts, boolean shifted ) {

		int shift = 0;
		if ( shifted ) shift+= TrackCorrector.NB_FRAME_TO_DISPLAY / 2;
		if ( ts.getLastTimePoint() < t - TrackCorrector.NB_FRAME_TO_DISPLAY + shift ) return false;
		if ( ts.getFirstTimePoint() > t + shift ) return false;

		return true;


	}

}
