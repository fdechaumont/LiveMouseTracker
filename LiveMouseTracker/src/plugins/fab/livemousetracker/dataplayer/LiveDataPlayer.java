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
package plugins.fab.livemousetracker.dataplayer;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import icy.canvas.Canvas2D;
import icy.canvas.IcyCanvas;
import icy.file.FileUtil;
import icy.gui.frame.IcyFrame;
import icy.gui.frame.IcyFrameEvent;
import icy.gui.frame.IcyFrameListener;
import icy.gui.frame.progress.AnnounceFrame;
import icy.image.IcyBufferedImage;
import icy.painter.Overlay;
import icy.plugin.abstract_.PluginActionable;
import icy.plugin.interface_.PluginBundled;
import icy.sequence.Sequence;
import icy.system.SystemUtil;
import icy.system.profile.Chronometer;
import icy.system.thread.Processor;
import icy.system.thread.ThreadUtil;
import icy.type.DataType;
import icy.type.point.Point5D.Double;
import icy.util.ColorUtil;
import icy.util.GraphicsUtil;
//import plugins.fab.aaa.voc.AudioFileViewer;
//import plugins.fab.aaa.voc.FullVocProcessor;
import plugins.fab.livemousetracker.DrawUtil;
import plugins.fab.livemousetracker.LiveMouseTracker;
import plugins.fab.livemousetracker.ROI2DAreaX;
import plugins.fab.livemousetracker.UDPSender;
import plugins.fab.livemousetracker.Util;
import plugins.fab.livemousetracker.detection.MouseDetection;
import plugins.fab.livemousetracker.experiment.Experiment;
import plugins.fab.livemousetracker.liveanalysis.chronogram.EventTimeLine;
import plugins.fab.livemousetracker.liveanalysis.chronogram.TimeLineDataType;
import plugins.kernel.roi.roi2d.ROI2DPolygon;

public class LiveDataPlayer extends PluginActionable implements IcyFrameListener {

    Sequence outSequence = null;
    File dataBaseFile = null;
    LiveDataPlayerPanel mainPanel = new LiveDataPlayerPanel();
    IcyFrame frame;
	Connection connection = null;
	ArrayList<MouseDetectionX> detectionList = new ArrayList<MouseDetectionX>();
	ArrayList<Event> eventList = new ArrayList<Event>();
	int t = 0;

//	HashMap<Integer, ArrayList<MouseDetection>> time2detectionList = new HashMap<Integer, ArrayList<MouseDetection>>();

	HashMap<MouseDetection, Integer> mouse2AnimalId = new HashMap<MouseDetection, Integer>();

	Point2D[] pointInfoOld = new Point2D[30];

	int playSpeedMultiplicator = 1;

	boolean DISPLAY_EVENTS = true;
	int startFrame = -1;
	int endFrame = -1;
	int id1 = -1;
	int id2 = -1;

	class Event{
		String name;
		String description;
		int start;
		int end;
		Integer animalA;
		Integer animalB;
		Integer animalC;
		Integer animalD;

		public Event( String name , String description, int start, int end , Integer animalA , Integer animalB , Integer animalC, Integer animalD ) {
			this.name = name;
			this.description = description;
			this.start = start;
			this.end = end;
			this.animalA = animalA;
			this.animalB = animalB;
			this.animalC = animalC;
			this.animalD = animalD;
		}

		@Override
		public String toString() {
			return ""+name+ "["+start+":"+end+"] " + animalA + "/" + animalB + "/" + animalC + "/" + animalD;
		}

		public int getNumberOfReferringAnimals() {

			int nbReferringAnimals = 0;
			if ( this.animalA != null ) nbReferringAnimals++;
			if ( this.animalB != null ) nbReferringAnimals++;
			if ( this.animalC != null ) nbReferringAnimals++;
			if ( this.animalD != null ) nbReferringAnimals++;
			return nbReferringAnimals;
		}
	}

    @Override
    public void run() {

    	System.err.println("The dataplayer can crash with the following scenario:");
    	System.err.println("play the file, and during caching, launch a next event search. This will crash.");


        outSequence = new Sequence("Live Data Player");
        IcyBufferedImage image = new IcyBufferedImage( 512 , 480 , 1 , DataType.BYTE );
        byte data[] = image.getDataXYAsByte( 0 );
        for ( int i = 0 ; i < data.length ; i++ )
        {
        	data[i] = 80;
        }
        data[0]=0;
        data[1]=127;

        image.dataChanged();
        outSequence.addImage( image );
        outSequence.addOverlay( new PlayerOverlay() );
        addSequence( outSequence );

        DataUtil.closeConnection(connection);
    	mouse2AnimalId.clear();

    	File[] loaded = DataUtil.selectDataBaseFiles( this );
    	if ( loaded == null )
    	{
    		return;
    	}
        dataBaseFile = loaded[0];

        frame = new IcyFrame("Live Data Player");
        frame.getContentPane().add( mainPanel );
        frame.setVisible( true );
        frame.pack();
        frame.addFrameListener( this );

        addIcyFrame( frame );

        playManagment.start();

        t = 0;

        refreshDataRunnable.start();
        try {
        	connect();

//        	connection = DataUtil.connectDataBase( connection, dataBaseFile );
			animalList = DataUtil.loadMice( connection );
			closeConnect();
		} catch (SQLException e) {
			e.printStackTrace();
		}
        refresh();

        mainPanel.getFrameField().addKeyListener( new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {
				if ( e.getKeyChar() != KeyEvent.VK_ENTER ) return;

				try
				{
					t = Integer.parseInt( mainPanel.getFrameField().getText() );
				}catch( Exception e1 )
				{
					// can't interpret number
				}
				refresh();

			}

			@Override
			public void keyReleased(KeyEvent e) {}

			@Override
			public void keyPressed(KeyEvent e) {}
		});

        mainPanel.getBtnPrevious().addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				setNavigationButtonEnabled( false );
//				mainPanel.getBtnNext().setEnabled( false );
//				mainPanel.getBtnPrevious().setEnabled( false );

				Integer tFound = findPreviousEvent();
				if ( tFound == null )
				{
					System.out.println("No more events.");
				}else
				{
					t = tFound;
				}
				refresh();

			}
		});

        mainPanel.getBtnNext().addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				mainPanel.getBtnNext().setEnabled( false );
				mainPanel.getBtnPrevious().setEnabled( false );
				Integer tFound = findNextEvent();
				if ( tFound == null )
				{
					System.out.println("No more events.");
				}else
				{
					t = tFound;
				}
				refresh();
			}
		});

    }

	private void closeConnect() {

		DataUtil.closeConnection(connection);
		connection = null;
	}

	private void connect() {
		if ( connection == null )
		{
			connection = DataUtil.connectDataBase( connection, dataBaseFile );
		}

	}

	private Integer findNextEvent() {

		//if ( !isFilterCompatible() ) return null;

		// with SQL request

		try {
			connect();
			String sql = "SELECT * FROM EVENT WHERE STARTFRAME>? AND NAME LIKE ? ORDER BY STARTFRAME ASC LIMIT 1";
			PreparedStatement ps;
			ps = connection.prepareStatement( sql );
			ps.setInt( 1 , t );

			String likeFilter = mainPanel.getTextFieldEventFilter().getText().trim(); //.toLowerCase();
			likeFilter = likeFilter.split(",")[0];

			likeFilter = "%"+likeFilter+"%";

			ps.setString( 2, likeFilter );

			System.out.println( sql+ "t:" +t+ " filter:"+likeFilter );

			ResultSet rs = ps.executeQuery( );

			while ( rs.next() ) {
				Integer start = rs.getInt( "startframe" );
				return start;
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}finally {
			closeConnect();
		}
		return null;
	}

	private Integer findPreviousEvent() {

		if ( !isFilterCompatible() ) return null;


		// with SQL request

		try {
			connect();
			String sql = "SELECT * FROM EVENT WHERE ENDFRAME<? AND NAME LIKE ? ORDER BY ENDFRAME DESC LIMIT 1";
			PreparedStatement ps;
			ps = connection.prepareStatement( sql );
			ps.setInt( 1 , t );

			String likeFilter = mainPanel.getTextFieldEventFilter().getText().trim(); //.toLowerCase();
			likeFilter = "%"+likeFilter+"%";

			ps.setString( 2, likeFilter );

			ResultSet rs = ps.executeQuery( );

			while ( rs.next() ) {
				Integer end = rs.getInt( "endframe" );
				return end;
			}

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeConnect();
		}
		return null;
	}


    private boolean isFilterCompatible() {
    	String filter[] = mainPanel.getTextFieldEventFilter().getText().split(",");
    	if ( filter.length != 1 )
    	{
    		new AnnounceFrame("Previous and Next do not support ',' split",5 );
    		return false;
    	}
		return true;
	}


	boolean refreshData = false;

    Thread refreshDataRunnable = new Thread() {

		@Override
		public void run() {

			System.out.println("Refresh data started.");

			while( !isInterrupted() )
			{
				if ( refreshData )
				{
//					System.out.println("Loading data");
					connect();
//					connection = DataUtil.connectDataBase( connection , dataBaseFile );
					try {
						detectionList = loadDetection( connection , t - LOADWINDOW, t+LOADWINDOW , false , detectionList );
					} catch (SQLException e) {
						e.printStackTrace();
					}
					try {
						loadEvents();
					} catch (SQLException e) {
						e.printStackTrace();
					}

					refreshData = false;
					closeConnect();
					outSequence.painterChanged( null );

					setNavigationButtonEnabled(true );
//					mainPanel.getBtnNext().setEnabled( true );
//					mainPanel.getBtnPrevious().setEnabled( true );
				}
				Thread.yield();
			}
		}
    };

    public void setNavigationButtonEnabled( boolean enabled )
    {
    	mainPanel.getBtnNext().setEnabled( enabled );
		mainPanel.getBtnPrevious().setEnabled( enabled );
    }

    void refresh()
    {
    	refreshData = true;
    	outSequence.painterChanged( null );
    }

    static final int LOADWINDOW = 200;

    ArrayList<DBAnimal> animalList = new ArrayList<DBAnimal>();


    public static ArrayList<MouseDetectionX> loadDetection(
    		Connection connection, int startFrame, int endFrame , boolean multiThreadOn, //int t, int LOADWINDOW
    		ArrayList<MouseDetectionX> existingDetectionList
    		) throws SQLException {

    	String sql = "SELECT * FROM DETECTION WHERE FRAMENUMBER>="+ startFrame +" AND FRAMENUMBER<=" + endFrame;

    	PreparedStatement ps;
    	ps = connection.prepareStatement( sql );
//    	ps.setInt( 1 , t-LOADWINDOW );
//    	ps.setInt( 2 , t+LOADWINDOW );
//    	ps.setInt( 1 , startFrame );
//    	ps.setInt( 2 , endFrame );



//    	System.out.println( sql );

    	final ResultSet rs = ps.executeQuery( );

    	final ArrayList<MouseDetectionX> newDetectionList = new ArrayList<MouseDetectionX>();
    	newDetectionList.clear();

    	Processor processor = new Processor();

//    	detectionLoader:
    	while ( rs.next() )
    	{
    		// Copy existing data if existing
    		boolean loadedFromCache = false;
    		int frame = rs.getInt("FRAMENUMBER");
    		final int baseId = rs.getInt( "animalid" );
//    		int added = 0;
    		if ( existingDetectionList != null )
    		{
    			synchronized ( existingDetectionList ) {
    				for ( MouseDetectionX det : existingDetectionList )
    				{
    					if ( det.mouseDetection.getT() == frame && det.dataBaseId == baseId )
    					{
//    						added++;
    						newDetectionList.add( det );
    						loadedFromCache = true;
    						break;

    					}
    				}
    			};
    		}
//    		System.out.println(frame + " : " + added);

    		if ( loadedFromCache ) continue;

    		// load

    		Runnable loader = new Runnable() {

    		final String data = rs.getString( "data" );
    		final int animalId = rs.getInt( "animalid" );
    		final long id = rs.getLong( "id" );

				@Override
				public void run() {

					try{
//						String data = rs.getString( "data" );
//						int animalId = rs.getInt( "animalid" );
//						long id = rs.getLong( "id" );
						MouseDetectionX m = new MouseDetectionX( id, animalId , new MouseDetection( data ) );
						//    		ROI2DAreaX rx = ( ROI2DAreaX ) ( m.mouseDetection.getROI2DArea() );
						//    		rx.fill = true;
						synchronized ( newDetectionList) {
							newDetectionList.add( m );
						}

						Color color = Color.black;
						if ( animalId > 0 )
						{
							color = getColor( animalId );
						}
						m.mouseDetection.getROI2DArea().setColor( color );
					}catch( Exception e )
					{
						e.printStackTrace();
						// Crash during detection creation (XML Reading crash ? )
					}

				}


			};
			if ( multiThreadOn )
			{
				while ( processor.isFull() )
				{
					try {
						Thread.sleep( 0 );
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				processor.submit( loader );
			}else
			{
				loader.run();
			}
    	}

		if ( multiThreadOn )
		{
			processor.waitAll();
		}


    	rs.close();
    	return newDetectionList;

    }

    /**
     * For database post analysis only
     * */
    public static ArrayList<MouseDetectionX> loadSimpleDetection(
    		Connection connection, int startFrame, int endFrame , boolean multiThreadOn //int t, int LOADWINDOW
    		) throws SQLException {

    	String sql = "SELECT ID, ANIMALID, MASS_X, MASS_Y, FRONT_X,FRONT_Y, BACK_X,BACK_Y, FRAMENUMBER FROM DETECTION WHERE FRAMENUMBER>=? AND FRAMENUMBER<=?";
    	PreparedStatement ps;
    	ps = connection.prepareStatement( sql );
    	ps.setInt( 1 , startFrame );
    	ps.setInt( 2 , endFrame );

    	final ResultSet rs = ps.executeQuery( );

    	final ArrayList<MouseDetectionX> newDetectionList = new ArrayList<MouseDetectionX>();

    	newDetectionList.clear();

    	while ( rs.next() )
    	{
    		final int animalId = rs.getInt( "animalid" );
    		final long id = rs.getLong( "id" );

    		Point2D mass = new Point2D.Double( rs.getDouble( 3 ) , rs.getDouble( 4 ) );
    		Point2D front = new Point2D.Double( rs.getDouble( 5 ) , rs.getDouble( 6 ) );
    		Point2D back = new Point2D.Double( rs.getDouble( 7 ) , rs.getDouble( 8 ) );
    		int t = rs.getInt( 9 ) ;

//    		System.out.println("---");
//    		System.out.println( mass );
//    		System.out.println( front );
//    		System.out.println( back );


    		MouseDetectionX m = new MouseDetectionX( id, animalId , new MouseDetection( t, mass, front, back ) );

    		newDetectionList.add( m );
    	}




    	rs.close();
    	return newDetectionList;

    }

	public static Color getColor( Integer id) {

		if ( id == null ) return null;
		float h=0;
		h+= (id-1)* ( 2.4 * 1 / (Math.PI * 2f ) );
		Color color = Color.getHSBColor( h, 0.9f, 0.9f );
		return color;

	}

	private void loadEvents() throws SQLException {

		String sql = "SELECT * FROM EVENT WHERE STARTFRAME<=? AND ENDFRAME>=?";
		PreparedStatement ps;
		ps = connection.prepareStatement( sql );
		ps.setInt( 1 , t+LOADWINDOW );
		ps.setInt( 2 , t-LOADWINDOW );

		ResultSet rs = ps.executeQuery( );

		ArrayList<Event> eventListLocal = new ArrayList<Event>();
		eventListLocal.clear();

		while ( rs.next() ) {
			String name = rs.getString( "name" );
			String description = rs.getString( "description" );
			int start = rs.getInt( "startframe" );
			int end = rs.getInt( "endframe" );

			Integer idA = rs.getInt( "idanimalA" );
			if (rs.wasNull() || idA == 0 )
			{
				idA = null;
			}

			Integer idB = rs.getInt( "idanimalB" );
			if (rs.wasNull() || idB == 0 )
			{
				idB = null;
			}

			Integer idC = rs.getInt( "idanimalC" );
			if (rs.wasNull() || idC == 0  )
			{
				idC = null;
			}

			Integer idD = rs.getInt( "idanimalD" );
			if (rs.wasNull() || idD == 0 )
			{
				idD = null;
			}

			if ( ! name.contains("USV seq") ) // keep only description for USV sequence, to get the wav number.
			{
				description ="";
			}else
			{
				// convert wav number of USV to a 7 digit integer.
//				System.out.println( description  );
				try{
					description = String.format("%07d", Integer.parseInt( description ) );
				}catch( NumberFormatException e )
				{
					// not a number. Drop info.
				}
//				System.out.println( description  );
			}


			Event event = new Event( name , description, start , end , idA , idB, idC, idD ) ;
			eventListLocal.add( event );
//			System.out.println( event );
		}

		synchronized ( eventList ) {
			eventList = eventListLocal;
		}
		rs.close();
	}

	enum Status {
		PLAY, STOP;
	}
	Status status = Status.STOP;

	Thread playManagment = new Thread() {

		@Override
		public void run() {

			System.out.println("Play managment started.");

			while ( !isInterrupted() )
			{
				if ( status == Status.PLAY )
				{
					t++;
					refresh();
				}
				try {
					//Thread.sleep( 1000 / 30 );
					Thread.sleep( ( 1000 / 30 ) * playSpeedMultiplicator );
				} catch (InterruptedException e) {
					return;
				}
			}
		}
	};

	private void playStop() {

		if ( status == Status.STOP )
		{
			status = Status.PLAY;
		}else
		{
			status = Status.STOP;
		}

	}

	class PlayerOverlay extends Overlay
	{
		public PlayerOverlay() {
			super("Player Overlay");
		}

		@Override
		public void keyPressed(KeyEvent e, Double imagePoint, IcyCanvas canvas) {

			if ( e.getKeyChar()=='r' )
			{
				synchronized ( detectionList ) {
					detectionList.clear();
				}
				synchronized ( eventList ) {
					eventList.clear();
				}


				e.consume();
				refresh();
			}

			if ( e.getKeyChar()=='+' )
			{
				if ( e.isShiftDown() )
				{
					t+=30 * 60 ; // 1 minute
				}else if ( e.isControlDown() )
				{
					t+=30 * 60 * 60 ; // 1 hour
				}else
				{
					t+=30; // 1 sec
				}

				refresh();
			}

			if ( e.getKeyChar()=='-' )
			{
				if ( e.isShiftDown() )
				{
					t-=30 * 60 ; // 1 minute
				}else if ( e.isControlDown() )
				{
					t-=30 * 60 * 60 ; // 1 hour
				}else
				{
					t-=30; // 1 sec
				}
				refresh();
			}

			if ( e.getKeyChar()=='s' )
			{
				startFrame = t;
				refresh();
			}

			if ( e.getKeyChar()=='e' )
			{
				endFrame = t;
				refresh();
			}

			try
			{
			int keyValue= Integer.parseInt( ""+e.getKeyChar() );
			{
				if ( keyValue >0 )
				{
					e.consume();
					if ( id1 == -1 )
					{
						id1 = keyValue;
						refresh();
						return;
					}
					if ( id2 == -1 )
					{
						id2 = keyValue;
						processIdentitySwap();
						clearStartEndFrame( e );
/*
						startFrame = -1;
						endFrame = -1;
						id1 = -1;
						id2 = -1;
						refresh();
						*/
						return;
					}

				}
			}
			}catch( NumberFormatException e1 )
			{

			}

			if ( e.getKeyChar() == 'j' )
			{
				processManualEvent( mainPanel.getTxtEvent1().getText() );
				clearStartEndFrame( e );
			}
			if ( e.getKeyChar() == 'k' )
			{
				processManualEvent( mainPanel.getTxtEvent2().getText() );
				clearStartEndFrame( e );
			}
			if ( e.getKeyChar() == 'l' )
			{
				processManualEvent( mainPanel.getTxtEvent3().getText() );
				clearStartEndFrame( e );
			}
			if ( e.getKeyChar() == 'm' )
			{
				processManualEvent( mainPanel.getTxtEvent4().getText() );
				clearStartEndFrame( e );
			}
			if ( e.getKeyChar() == 'b' )
			{
				processManualEvent( mainPanel.getTxtEvent5().getText() );
				clearStartEndFrame( e );
			}
			if ( e.getKeyChar() == 'n' )
			{
				processManualEvent( mainPanel.getTxtEvent6().getText() );
				clearStartEndFrame( e );
			}

			if ( e.getKeyChar() == 'J' )
			{
				deleteEventAtCurrentT( mainPanel.getTxtEvent1().getText() );
				clearStartEndFrame( e );
			}
			if ( e.getKeyChar() == 'K' )
			{
				deleteEventAtCurrentT( mainPanel.getTxtEvent2().getText() );
				clearStartEndFrame( e );
			}
			if ( e.getKeyChar() == 'L' )
			{
				deleteEventAtCurrentT( mainPanel.getTxtEvent3().getText() );
				clearStartEndFrame( e );
			}
			if ( e.getKeyChar() == 'M' )
			{
				deleteEventAtCurrentT( mainPanel.getTxtEvent4().getText() );
				clearStartEndFrame( e );
			}
			if ( e.getKeyChar() == 'B' )
			{
				deleteEventAtCurrentT( mainPanel.getTxtEvent5().getText() );
				clearStartEndFrame( e );
			}
			if ( e.getKeyChar() == 'N' )
			{
				deleteEventAtCurrentT( mainPanel.getTxtEvent6().getText() );
				clearStartEndFrame( e );
			}

			if ( e.getKeyChar()=='0' )
			{
				clearStartEndFrame( e );
//				startFrame = -1;
//				endFrame = -1;
//				id1 = -1;
//				id2 = -1;
//				e.consume();
//				refresh();
			}

			if ( e.getKeyCode() == KeyEvent.VK_RIGHT )
			{
				t++;
				e.consume();
				refresh();
			}

			if ( e.getKeyCode() == KeyEvent.VK_LEFT )
			{
				t--;
				e.consume();
				refresh();
			}

			if ( e.getKeyChar()=='*' )
			{
				playSpeedMultiplicator--;
				if ( playSpeedMultiplicator < 1 ) playSpeedMultiplicator = 1;
				refresh();
			}

			if ( e.getKeyChar()=='/' )
			{
				playSpeedMultiplicator++;
				if ( playSpeedMultiplicator > 30 ) playSpeedMultiplicator = 30;
				refresh();
			}

			if ( e.getKeyChar()==' ' )
			{
				playStop();
			}
			if ( e.getKeyChar()=='x' )
			{
				DISPLAY_EVENTS = !DISPLAY_EVENTS;
				refresh();
			}

			/*
			if ( e.getKeyChar()=='%' )
			{
				DataBaseRecomputeEvents.recomputeEvents(connection);
				refresh();
			}

			if ( e.getKeyChar()=='$' )
			{

				Runnable updateNewEvents = new Runnable() {

					@Override
					public void run() {

						for ( int idA = 1 ; idA < 5 ;idA++ )
						{
							for ( int idB = 1 ; idB < 5 ;idB++ )
							{
								if ( idA == idB ) continue;

								DataBaseRecomputeOnlySpecitifEvents.recomputeEventOfTypeContact( "Contact" , connection , idA, idB );
								DataBaseRecomputeOnlySpecitifEvents.recomputeEventOfTypeContact( "Oral-oral Contact" , connection , idA, idB );
								DataBaseRecomputeOnlySpecitifEvents.recomputeEventOfTypeContact( "Oral-genital Contact" , connection , idA, idB );
							}
						}

						System.out.println("UPDATE FINISHED !");

					}
				};

				ThreadUtil.bgRunSingle( updateNewEvents );

//				refresh();
			}

			if ( e.getKeyChar()=='=' )
			{
				Runnable update = new Runnable() {

					@Override
					public void run() {
						DataBaseRecomputeOnlySpecitifEvents.delEvents(connection);
						int max = DataUtil.getMaxNumberOfFrame( connection );
						// int window = 9000; // tranche de 5 minutes

						int window = 2 * 60 * 30; // tranche de 2 minutes

						// 1H de data = 108000

						Chronometer recomputationChrono = new Chronometer("recomputation");
						for ( int t = 0 ; t < max ; t+= window )
						{
							System.out.println("******* : " + t * ( 100f / max ) + " % " + recomputationChrono.getNanos()/ 1000000000f + "s" );
							int upperLimit = t+window -1;
							if ( upperLimit > max )
							{
								upperLimit = max;
							}
							DataBaseRecomputeOnlySpecitifEvents.recomputeNestEvents( connection , t , upperLimit );
							String txt = "Mem:"+(int)( ( SystemUtil.getJavaTotalMemory() - SystemUtil.getJavaFreeMemory() ) / ( 1024 * 1024 ) ) +"/"+
									(int)( SystemUtil.getJavaMaxMemory() / ( 1024 * 1024 ) ) +"MB";
							System.err.println( txt );
						}
						System.out.println("Finished.");
					}
				};

				ThreadUtil.bgRunSingle( update );

				//refresh();
			}
			*/

		}

		/**
		 * Destroy the whole event at specified t.
		 * @param eventTxt
		 */
		private void deleteEventAtCurrentT(String eventTxt ) {
			try
			{
				name = parseEventName( eventTxt );
				Integer ids[]= parseIds( eventTxt );

				while ( connection != null ) // connection busy
				{
					Thread.sleep( 100 );
				}
				connect();
				Experiment.delEventAt( connection , name , t , ids[1] , ids[2], ids[3], ids[4] );
				closeConnect();



			}catch( Exception e )
			{
				e.printStackTrace();
			}
		}

		private Integer[] parseIds(String eventTxt) {

			// grab ids in txt.
			String split[] = eventTxt.split("_");

			String name = split[0];
			String idText = split[1];

			System.out.println("Name: " + name );
			System.out.println("IDText: " + idText );

			Integer ids[]= new Integer[5];

			int i=1;
			for ( int offset=0; offset < idText.length() ; offset++ )
			{
				//int keyValue= Integer.parseInt( ""+e.getKeyChar() );
				String s = idText.substring( offset, offset+1 );
				System.out.println( s );
				ids[i] = Integer.parseInt( s );
				System.out.println( i + ":" + ids[i] );
				i++;
			}

			return ids;

		}

		private String parseEventName(String eventTxt) {

			String split[] = eventTxt.split("_");
			return split[0];
		}

		/**
		 * general case: nameOfEvent_132 should refer to id 1 3 and 2 for animalA,B,C
		 * @param eventTxt
		 */
		private void processManualEvent(String eventTxt ) {

			try
			{
				name = parseEventName( eventTxt );
				Integer ids[]= parseIds( eventTxt );
/*
				// grab ids in txt.
				String split[] = eventTxt.split("_");

				String name = split[0];
				String idText = split[1];

				System.out.println("Name: " + name );
				System.out.println("IDText: " + idText );

				Integer ids[]= new Integer[5];

				int i=1;
				for ( int offset=0; offset < idText.length() ; offset++ )
				{
					//int keyValue= Integer.parseInt( ""+e.getKeyChar() );
					String s = idText.substring( offset, offset+1 );
					System.out.println( s );
					ids[i] = Integer.parseInt( s );
					System.out.println( i + ":" + ids[i] );
					i++;
				}
*/
				EventTimeLine timeLine = new EventTimeLine( name , TimeLineDataType.BOOLEAN );
				timeLine.addEvent( startFrame, endFrame );
				while ( connection != null ) // connection busy
				{
					Thread.sleep( 100 );
				}
				connect();
				Experiment.saveTimeLine( connection , name , "" , timeLine , ids[1] , ids[2], ids[3], ids[4] );
				closeConnect();

			}
			catch( Exception e)
			{
				System.out.println("Can't process manual event");
				e.printStackTrace();
			}




/*
			for ( int t = startFrame ; t<= endFrame ; t++ )
			{

			}
	*/
/*
			for ( MouseDetectionX detection : detectionList )
			{
				if ( detection.mouseDetection.getT() <= endFrame && detection.mouseDetection.getT() >= startFrame )
				{
					boolean changed = false;
					if ( detection.animalId == id1 )
					{
						detection.animalId = id2;
						changed = true;
					}else
					if ( detection.animalId == id2 )
					{
						detection.animalId = id1;
						changed = true;
					}
					if ( changed )
					{
				    	String sql = "UPDATE DETECTION SET ANIMALID=? WHERE ID=?";
				    	PreparedStatement ps;
				    	try {

				    		ps = connection.prepareStatement( sql );
				    		ps.setInt( 1 , detection.animalId );
				    		ps.setLong( 2 , detection.dataBaseId );
				    		ps.executeUpdate();
				    		//ResultSet rs = ps.executeQuery( );
				    		//rs.close();

				    	} catch (SQLException e) {
				    		e.printStackTrace();
				    	}

					}
				}
				refresh();
			}
			*/

		}

		private void clearStartEndFrame( KeyEvent e ) {

			startFrame = -1;
			endFrame = -1;
			id1 = -1;
			id2 = -1;
			e.consume();
			refresh();

		}

		private void processIdentitySwap() {

//			ArrayList<MouseDetectionX> newDetection = new ArrayList<MouseDetectionX>();
//			ArrayList<MouseDetectionX> removedDetection = new ArrayList<MouseDetectionX>();

			if ( connection == null )
			{
				connect();
			}
			for ( MouseDetectionX detection : detectionList )
			{
				if ( detection.mouseDetection.getT() <= endFrame && detection.mouseDetection.getT() >= startFrame )
				{
					boolean changed = false;
					if ( detection.animalId == id1 )
					{
						detection.animalId = id2;
						changed = true;
					}else
					if ( detection.animalId == id2 )
					{
						detection.animalId = id1;
						changed = true;
					}
					if ( changed )
					{
				    	String sql = "UPDATE DETECTION SET ANIMALID=? WHERE ID=?";
				    	PreparedStatement ps;
				    	try {
				    		ps = connection.prepareStatement( sql );
				    		ps.setInt( 1 , detection.animalId );
				    		ps.setLong( 2 , detection.dataBaseId );
				    		ps.executeUpdate();
				    		//ResultSet rs = ps.executeQuery( );
				    		//rs.close();

				    	} catch (SQLException e) {
				    		e.printStackTrace();
				    	}

					}
				}
			}
			//closeConnect();
			refresh();

		}

		Rectangle2D rect50x50cmCageFloor = new Rectangle2D.Double( 114, 63, 398-114, 353-63 );

		@Override
		public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas) {

			Canvas2D ca = (Canvas2D) canvas;
			Graphics2D gAbsolute = (Graphics2D)g.create();
			gAbsolute.setFont( new Font("Arial", Font.BOLD , 20 ) );
			gAbsolute.transform( ca.getInverseTransform() );

			g.setColor( Color.LIGHT_GRAY );
			g.fill( rect50x50cmCageFloor );
//			g.setColor( Color.BLACK );
//			g.draw( rect50x50cmCageFloor );


			if ( animalList != null )
			{
				g.setFont( new Font("Arial", Font.BOLD , 15 ) );
				for ( DBAnimal animal : animalList )
				{
					//System.out.println( animal.toString() );
					g.setColor( getColor( animal.id ) );
					g.drawString( animal.toString() , 350,  10 + animal.id * 15 );
				}

			}

			DrawUtil.drawCenteredHint(gAbsolute, "Play speed: 1/"+playSpeedMultiplicator, canvas.getWidth()/2, 0, Color.black , Color.white);

			if ( startFrame != -1 )
			{
				DrawUtil.drawCenteredHint(gAbsolute, "Start: " + Util.getTimeStamp( startFrame ), canvas.getWidth()/2 -200 , canvas.getHeight()-50, Color.red , Color.white);
			}
			if ( endFrame != -1 )
			{
				DrawUtil.drawCenteredHint(gAbsolute, "End: " + Util.getTimeStamp( endFrame ), canvas.getWidth()/2 +200, canvas.getHeight()-50, Color.blue , Color.white);
			}
			if ( id1 != -1 )
			{
				DrawUtil.drawCenteredHint(gAbsolute, "Switch " + id1 + " to... ( 0 to cancel )", canvas.getWidth()/2, canvas.getHeight()-80, Color.yellow , Color.white);
			}

			if ( refreshData )
			{
				DrawUtil.drawCenteredHint(gAbsolute, "Caching...", canvas.getWidth()/2 +250, canvas.getHeight()-50, Color.blue , Color.white);
			}

			if ( connection!=null )
			{
				DrawUtil.drawHint(gAbsolute, "DB Locked", 0, canvas.getHeight()-50, Color.red , Color.white);
			}else
			{
				DrawUtil.drawHint(gAbsolute, "DB Unlocked", 0, canvas.getHeight()-50, Color.green , Color.white);
			}

			if ( !DISPLAY_EVENTS)
			{
				DrawUtil.drawCenteredHint(gAbsolute, "Display events: off", canvas.getWidth()/2, 20, Color.black , Color.white);
			}

			DrawUtil.drawCenteredHint(g, "Keys:\n\n -: -1s\n +: +1s\nSpace:Play/Stop\n *: Speed up \n /: Slow down \n "
					+ "x: display events on/off\n-> : 1 frame ahead \n <-: 1 frame backward\n"
					+ "\narrow + shift:+1min"
					+ "\narrow + ctrl:+1hour"
					+ "\ns:start\ne:end\nnumber:swap ids\nJ,K,L,M: affect event"
					+ "\n0 : cancel change\n\nr:force base refresh", 600, 0, Color.black , Color.white);

			boolean showPreview = false;


//			if ( startFrame != -1 && endFrame != -1 )
//			{
//				showPreview = true;
//			}

			if ( !showPreview )
			{
				Composite originalComposite = g.getComposite();
				g.setComposite( makeComposite( 0.01f ) );
				for ( MouseDetectionX detection : detectionList )
				{
					if ( Math.abs( detection.mouseDetection.getT() - t ) < 30 )
					{
						detection.mouseDetection.getROI2DArea().getOverlay().paint(g, sequence, canvas);
					}
				}
				g.setComposite( originalComposite );
			}
			else
			{
//				Composite originalComposite = g.getComposite();
//				g.setComposite( makeComposite( 0.01f ) );
				for ( MouseDetectionX detection : detectionList )
				{
					if ( detection.mouseDetection.getT() <= endFrame && detection.mouseDetection.getT() >= startFrame )
					{
						detection.mouseDetection.getROI2DArea().getOverlay().paint(g, sequence, canvas);
					}
				}
//				g.setComposite( originalComposite );
			}


			g.setColor( Color.black );

			DrawUtil.drawCenteredHint(gAbsolute, "t: "+ t + " " +  Util.getTimeStamp( t ), canvas.getWidth()/2, canvas.getHeight()-50, Color.black , Color.white);

			// draw events

			ArrayList<MouseDetectionX> currentFrameDetectionList = new ArrayList<MouseDetectionX>();

			g.setColor( Color.black );

			for ( MouseDetectionX detection : detectionList )
			{
				if ( detection.mouseDetection.getT() == t )
				{
					currentFrameDetectionList.add( detection );
				}
			}

			// create graphics for each mouse
			for ( MouseDetectionX detection : currentFrameDetectionList )
			{
				Graphics2D gMD = (Graphics2D)g.create();
				gMD.translate( (int)detection.mouseDetection.getMassCenter().getX(),
						(int)detection.mouseDetection.getMassCenter().getY() );
				detection.g = gMD;
			}

			if ( DISPLAY_EVENTS )
			{
				//			//	Build detections of the animal between 2 contacts.
				//			for ( Animal animal : animalList )
				//			{
				//				animal.detectionInWindowList.clear();
				//			}
				//
				//			for ( MouseDetectionX detection : currentFrameDetectionList )
				//			{
				//				Animal animal = getAnimal( detection.id );
				//				animal.detectionInWindowList = detectionInWindow( t , detection.id );
				//			}

				// draw events

				// compute panel location

				Point2D centerOfAllMice ;
				{
					float x=0;
					float y=0;
					for ( MouseDetectionX mX : currentFrameDetectionList )
					{
						x+=mX.mouseDetection.getMassCenter().getX();
						y+=mX.mouseDetection.getMassCenter().getY();
					}
					x/= (float)currentFrameDetectionList.size();
					y/= (float)currentFrameDetectionList.size();
					centerOfAllMice = new Point2D.Double( x, y );
				}

				// remove old pointInfo if the animal is not present.
				for ( int i = 1 ; i < pointInfoOld.length ; i++ )
				{
					Point2D old = pointInfoOld[i];
					if ( old == null ) continue;
					boolean found = false;

					for ( MouseDetectionX mX : currentFrameDetectionList )
					{
						if ( mX.animalId == i )
						{
							found = true;
							break;
						}
					}
					if ( !found )
					{
						pointInfoOld[i] = null;
					}
				}

				for ( MouseDetectionX mX : currentFrameDetectionList )
				{
					Point2D mc = mX.mouseDetection.getMassCenter().toPoint2D();
					Point2D vector = Util.createVector( centerOfAllMice, mc );
					Point2D v2 = Util.normVector( vector, 100+ vector.distance( 0 ,0 ) );
					Point2D location = new Point2D.Double( centerOfAllMice.getX() + v2.getX() , centerOfAllMice.getY() + v2.getY() );

					mX.infoText="";

					Point2D previous = pointInfoOld[mX.animalId];

//					System.out.println("---");
//					System.out.println( location );

					if ( previous == null )
					{
						previous = location;
					}
					Point2D vectorToNewLocation = Util.createVector( previous, location );
					Point2D shift = Util.normVector( vectorToNewLocation , vectorToNewLocation.distance( 0 , 0 ) * 0.1f );
					if ( !java.lang.Double.isNaN( shift.getX() ) )
					{
						location.setLocation( previous.getX() + shift.getX() , previous.getY() + shift.getY() );
					}

//					System.out.println( shift );
//					System.out.println( location );

					/*
					if ( previous != null )
					{
						Point2D vectorToNewLocation = Util.createVector( previous, location );
						if ( java.lang.Double.isNaN( vectorToNewLocation.getX() ) )
						{
							System.out.println("--");
							System.out.println( vectorToNewLocation );
							System.out.println( previous );
							System.out.println( location );
						}else
						{
							Point2D shift = Util.normVector( vectorToNewLocation , vectorToNewLocation.distance( 0 , 0 ) * 0.1f );
							location.setLocation( previous.getX() + shift.getX() , previous.getY() + shift.getY() );
						}
					}*/

					mX.displayInfoTarget = location;
//					System.out.println( location );
					pointInfoOld[mX.animalId] = mX.displayInfoTarget;

				}

				//String eventLeft ="";

				ArrayList<Event> eventLeftList = new ArrayList<LiveDataPlayer.Event>();

				// get filter event list.
				String filter[] = mainPanel.getTextFieldEventFilter().getText().split(",");

				//System.out.println("--");
				for ( Event event : eventList )
				{
					//System.out.println( event );
					if ( ! ( event.start <= t && event.end >= t ) ) continue; // event is not active for current T.

					// filter event
					boolean filterEvent = true;

					if ( filter.length != 0 )
					{
						for ( int i = 0 ; i< filter.length ; i++ )
						{
							//System.out.println( filter[i] );
							//if ( !event.name.toLowerCase().contains( filter ) ) continue;
							if ( event.name.toLowerCase().contains( filter[i].toLowerCase() ) ) filterEvent = false;
						}
					}
					if ( filterEvent ) continue;


					MouseDetectionX detectionA = getDetectionWithAnimalId( currentFrameDetectionList, event.animalA );
					MouseDetectionX detectionB = getDetectionWithAnimalId( currentFrameDetectionList, event.animalB );

//					System.out.println( "Event " + event + " detA: " + detectionA + " detB: " + detectionB );
//					System.out.println( event.animalA + " /// " + event.animalB );

					switch ( event.getNumberOfReferringAnimals() )
					{
					case 0:
						eventLeftList.add( event );

					case 1:
						if ( detectionA != null )
						{
							detectionA.infoText+=event.name+ "\n";
						}
						continue;

					case 2:
						eventLeftList.add( event );
						break;
					case 3:
						eventLeftList.add( event );
						break;
					case 4:
						eventLeftList.add( event );
						break;

					}

//					if ( event.getNumberOfReferringAnimals() == 1 )
//					{
//						detectionA.infoText+=event.name+ "\n";
//						System.out.println("OK / NULL");
//						continue;
//					}

//					if ( detectionA != null && detectionB != null )
//					{
//						eventLeftList.add( event );
//
//						System.out.println("OK / OK");
//						//					int xx = (int) (( detectionA.mouseDetection.getMassCenter().getX() + detectionB.mouseDetection.getMassCenter().getX() ) / 2);
//						//					int yy = (int) (( detectionA.mouseDetection.getMassCenter().getY() + detectionB.mouseDetection.getMassCenter().getY() ) / 2);
//						//					g.drawString( event.toString() , xx, yy+centerAdd );
//						//					g.drawLine( (int)detectionA.mouseDetection.getMassCenter().getX(),
//						//							(int)detectionA.mouseDetection.getMassCenter().getY(), xx,yy);
//						//					g.drawLine( (int)detectionB.mouseDetection.getMassCenter().getX(),
//						//							(int)detectionB.mouseDetection.getMassCenter().getY(), xx,yy);
//						//					centerAdd+=10;
//					}

//					if ( detectionA == null && detectionB == null )
//					{
//						System.out.println("NULL / NULL");
//						//					g.drawString( event.toString() , 10 , y );
//						//					y+=10;
//					}

					//eventLeft += event.toString()+ "\n";

				}

				int y = 0;
				for ( Event event : eventLeftList )
				{
					drawEventHint( g, event, 0, y ); //, getColor( event.animalA), getColor( event.animalB ) ,Color.black );
					y+=25;
				}

				for ( MouseDetectionX mE : currentFrameDetectionList )
				{
					if ( mE.infoText == "" ) continue;

					Color color = mE.mouseDetection.getROI2DArea().getColor();
					DrawUtil.drawCenteredHint(g, mE.infoText, (int)mE.displayInfoTarget.getX(), (int)mE.displayInfoTarget.getY(),
							color,
							Color.black );
					g.setColor( color );
					g.drawLine( (int)mE.displayInfoTarget.getX(), (int)mE.displayInfoTarget.getY(),
							(int)mE.mouseDetection.getMassCenter().getX(), (int)mE.mouseDetection.getMassCenter().getY() );
				}
			}

//			drawCenteredHint(g, eventLeft, (int)0, (int)0,
//					Color.white,
//					Color.black );

			// Draw mice

			for ( MouseDetectionX detection : currentFrameDetectionList )
			{
				if( detection.mouseDetection.getT() == t )
				{
					detection.mouseDetection.getROI2DArea().getOverlay().paint(g, sequence, canvas);
				}
			}

			for ( MouseDetectionX detection : currentFrameDetectionList )
			{
				if( detection.mouseDetection.getT() == t )
				{
					detection.mouseDetection.paint(g, canvas);
					g.drawString( ""+detection.animalId,
							(int)detection.mouseDetection.getMassCenter().getX(),
							(int)detection.mouseDetection.getMassCenter().getY() );

					/*
					String description = "N:"+(int)detection.mouseDetection.frontPoint.getZ();
					description+= " M:"+(int)detection.mouseDetection.getMassCenter().getZ();
					description+= " T:"+(int)detection.mouseDetection.backPoint.getZ();
					g.drawString( description,
							(float)detection.mouseDetection.getROI2DArea().getPosition2D().getX(),
							(float)detection.mouseDetection.getROI2DArea().getPosition2D().getY()+10
							);
							*/

				}
			}


			{ // UDP Stuff

				/*
							top left: 114, 63
							bottom right: 398,353
							width / heght: 284, 290
				 */
				MouseDetectionX dets[] = new MouseDetectionX[4];

				for ( MouseDetectionX detection : currentFrameDetectionList )
				{
					if( detection.mouseDetection.getT() == t )
					{
						if ( detection.animalId > 0 )
						{
							try
							{
								dets[detection.animalId-1] = detection;
							}
							catch( IndexOutOfBoundsException e )
							{
								// System.err.println("Warning: An index of detection is not refering an existing animal ! detection animal Id is " + detection.animalId );
							}
						}

//						if( detection.id == 1 ) det1 = detection;
//						if ( detection.id == 2 ) det2 = detection;
					}
				}


/*
				if ( det1 != null )
				{

					UE4Data d = new UE4Data( det1 );
					System.out.println( "UE4 ");
					System.out.println( "x:" + d.x );
					System.out.println( "y:" + d.y );
					System.out.println( "r:" + d.angle );

					float xx = (float) ( det1.mouseDetection.getMassCenter().getX() - 114 );
					xx =  xx * ( 50f/ 284f );

					float yy = (float) ( det1.mouseDetection.getMassCenter().getY() - 63 );
					yy =  yy * ( 50f/ 290f );

					float angle = 0;
					try
					{
						Point2D vect = Util.createVector( det1.mouseDetection.getMassCenter().toPoint2D(),
								det1.mouseDetection.getFrontPoint().toPoint2D() );

						angle =
								(float)
								Math.toDegrees(
										Math.atan2( vect.getY(), vect.getX() ) );
					} catch( NullPointerException e)
					{
					}


	//				UDPSender.send( (float)50-yy, (float)xx, (float)0, (float)angle-90+180 );
					angle=angle-90+180;
					yy = 50-yy;
					System.out.println( "TEST ");
					System.out.println( "x:" + xx );
					System.out.println( "y:" + yy );
					System.out.println( "r:" + angle );
					UDPSender.send( (float)yy, (float)xx, (float)0, (float)angle );

				}
				*/

				{ // Compute command
					ArrayList<Float> floatList = new ArrayList<Float>();
					for ( MouseDetectionX det : dets )
					{
						if ( det != null )
						{
							det.computeUE4Data();
							floatList.addAll( det.ue4Data.getAsList() );
						}else{
							floatList.add( (float) 0 );
							floatList.add( (float) 0 );
							floatList.add( (float) 0 );
							floatList.add( (float) 0 );
						}
					}

					float[] bufferFloat = new float[floatList.size()];
					for ( int i = 0 ;i < floatList.size(); i++ )
					{
						bufferFloat[i] = floatList.get( i );
					}
					UDPSender.send( bufferFloat );
				}
//				if ( det1 != null && det2 != null )
//				{
//					UE4Data d1 = new UE4Data( det1 );
//					UE4Data d2 = new UE4Data( det2 );
//					UDPSender.send2( d1.x,d1.y,d1.z,d1.angle, d2.x,d2.y,d2.z,d2.angle );
//				}

			}
		}

		private void drawEventHint(Graphics2D g, Event event, int x, int y) {

//			drawEventHint( g, event, 0, y ); //, getColor( event.animalA), getColor( event.animalB ) ,Color.black );
			ArrayList<Color> colorList = new ArrayList<Color>();
			if ( event.animalA != null ) { colorList.add( getColor( event.animalA ) ); }
			if ( event.animalB != null ) { colorList.add( getColor( event.animalB ) ); }
			if ( event.animalC != null ) { colorList.add( getColor( event.animalC ) ); }
			if ( event.animalD != null ) { colorList.add( getColor( event.animalD ) ); }
			String displayName = event.name;
			if ( event.description != "" )
			{
				displayName += " / "+event.description;
			}
			DrawUtil.drawHint(g, displayName, x, y, colorList, Color.black );

			if ( event.name.equals( "USV seq" ) )
			{
//				showUSVFile( event.description );
			}
		}

		/*
		public void listFilesForFolder(final File folder) {
		    for (final File fileEntry : folder.listFiles()) {
		        if (fileEntry.isDirectory()) {
		            listFilesForFolder(fileEntry);
		        } else {
		            System.out.println(fileEntry.getName());
		        }
		    }
		}*/

		Sequence showUSVSequence = null;
		String currentWavDisplayed = "";
		/*
		void showUSVFile( String wavNumber )
		{
			if ( showUSVSequence == null )
			{
				showUSVSequence = new Sequence("USV Display");
				addSequence( showUSVSequence );
			}

			if ( currentWavDisplayed.equals( wavNumber ) )
			{
				return;
			}

			currentWavDisplayed = wavNumber;

			// find file.

			String vocDir = FileUtil.getDirectory( dataBaseFile.getAbsolutePath() ) + "/voc";
			File folderFile = new File( vocDir );
			for ( File file : folderFile.listFiles() )
			{
				//System.out.println(file);
				String fileName = FileUtil.getFileName( file.getAbsolutePath(), true );
				if ( fileName.contains( wavNumber ) )
				{
					for ( Overlay overlay : showUSVSequence.getOverlays() )
					{
						showUSVSequence.removeOverlay(overlay);
					}

					FullVocProcessor processor = new FullVocProcessor( true , showUSVSequence );
					processor.process( file );

					break;
				}
			}


		}
		 */
		private AlphaComposite makeComposite(float alpha) {
			  int type = AlphaComposite.SRC_OVER;
			  return(AlphaComposite.getInstance(type, alpha));
			 }


		/** @deprecated */
		private ArrayList<MouseDetectionX> detectionInWindow(int t, int id) {

			ArrayList<MouseDetectionX> list = new ArrayList<MouseDetectionX>();

			int currentT= t+1;
			for ( MouseDetectionX detection : detectionList )
			{
				if ( detection.animalId == id && detection.mouseDetection.getT() == currentT )
				{
					list.add( detection );
					break;
				}
			}

			return list;

		}

//		private DBAnimal getAnimal(int id) {
//			for( DBAnimal animal : animalList )
//			{
//				if ( animal.id == id ) return animal;
//			}
//			return null;
//		}

	}

		private MouseDetectionX getDetectionWithAnimalId(ArrayList<MouseDetectionX> currentFrameDetectionList, Integer id) {

			if ( id == null ) return null;

			for ( MouseDetectionX detection : currentFrameDetectionList )
			{
				if ( detection.animalId == id ) return detection;
			}

			return null;
		}
//
//	public void drawCenteredHint( Graphics2D g, String text, int x, int y, Color bgColor, Color textColor )
//	{
//		Rectangle2D textRect = GraphicsUtil.getStringBounds(g, text);
//		x = (int)(x - textRect.getWidth()/2);
//		drawHint( g, text, x, y, bgColor, textColor );
//	}
//
//	public void drawCenteredHint( Graphics2D g, String text, int x, int y, Color bgColor1 , Color bgColor2 , Color textColor )
//	{
//		Rectangle2D textRect = GraphicsUtil.getStringBounds(g, text);
//		x = (int)(x - textRect.getWidth()/2);
//		drawHint( g, text, x, y, bgColor1 , bgColor2 , textColor );
//	}
//
//	private void drawHint(Graphics2D g, String text, int x, int y, Color bgColor, Color textColor) {
//		ArrayList<Color> bgColorList = new ArrayList<Color>();
//		bgColorList.add( bgColor );
//		drawHint( g,  text,  x,  y, bgColorList, textColor);
//	}
//
//	public static void drawHint( Graphics2D g, String text, int x, int y, ArrayList<Color> bgColorList, Color textColor )
//	{
//		final Graphics2D g2 = (Graphics2D) g.create();
//
//		final Rectangle2D stringRect = GraphicsUtil.getStringBounds(g, text);
//		// calculate hint rect
//		final RoundRectangle2D backgroundRect = new RoundRectangle2D.Double(x, y, (int) (stringRect.getWidth() + 10),
//				(int) (stringRect.getHeight() + 8), 8, 8);
//
////		final RoundRectangle2D backgroundRectLeft = new RoundRectangle2D.Double(x, y, (int) (stringRect.getWidth() /2 ),
////				(int) (stringRect.getHeight() + 8), 8, 8);
//
//		g2.setStroke(new BasicStroke(1.2f));
//
//		if ( bgColorList.size() == 0 )
//		{
//			g2.setColor( Color.white );
//			GraphicsUtil.mixAlpha(g2, AlphaComposite.SRC_OVER, 0.9f);
//			g2.fill( backgroundRect );
//		}
//
//		int shift = 0;
//		for ( int i = 0 ; i< bgColorList.size() ; i++ )
//		{
//			g2.setColor( bgColorList.get( i ) );
//			GraphicsUtil.mixAlpha(g2, AlphaComposite.SRC_OVER, 0.9f);
//
//			final RoundRectangle2D currentRect = new RoundRectangle2D.Double(
//					x+shift,
//					y,
//					(int) ( backgroundRect.getWidth() / bgColorList.size() ) +1,
//					(int) (stringRect.getHeight() + 8),
//
//					8, 8);
//
//			shift+= backgroundRect.getWidth()/bgColorList.size();
//
//			g2.fill( currentRect);
//		}
//
//		/*
//		Color strokeColor = Color.black;
//		if ( bgColorList.size() > 0 )
//		{
//			strokeColor = bgColorList.get( 0 );
//		}
//*/
//		// draw background stroke
//		g2.setColor( Color.black );
//		GraphicsUtil.mixAlpha(g2, AlphaComposite.SRC_OVER, 2f / 1f);
//		g2.draw(backgroundRect);
//
//		// draw text
//		g2.setColor(textColor);
//		GraphicsUtil.drawString(g2, text, x + 5, y + 4, false);
//
//		g2.dispose();
//	}
//
//	public static void drawHint( Graphics2D g, String text, int x, int y, Color bgColorLeft, Color bgColorRight, Color textColor )
//	{
//		ArrayList<Color> bgColorList = new ArrayList<>();
//		bgColorList.add( bgColorLeft );
//		bgColorList.add( bgColorRight );
//		drawHint(g, text, x, y, bgColorList, textColor);
//	}
/*
	public static void drawHint( Graphics2D g, String text, int x, int y, Color bgColorLeft, Color bgColorRight, Color textColor )
	{
		final Graphics2D g2 = (Graphics2D) g.create();

		final Rectangle2D stringRect = GraphicsUtil.getStringBounds(g, text);
		// calculate hint rect
		final RoundRectangle2D backgroundRect = new RoundRectangle2D.Double(x, y, (int) (stringRect.getWidth() + 10),
				(int) (stringRect.getHeight() + 8), 8, 8);

		final RoundRectangle2D backgroundRectLeft = new RoundRectangle2D.Double(x, y, (int) (stringRect.getWidth() /2 ),
				(int) (stringRect.getHeight() + 8), 8, 8);

		g2.setStroke(new BasicStroke(1.3f));

		if ( bgColorLeft.equals( bgColorRight ))
		{
			//draw translucent background right
			g2.setColor(bgColorRight);
			GraphicsUtil.mixAlpha(g2, AlphaComposite.SRC_OVER, 1f / 2f);
			g2.fill(backgroundRect);
		}else
		{
			//draw translucent background right
			g2.setColor(bgColorRight);
//			GraphicsUtil.mixAlpha(g2, AlphaComposite.SRC_OVER, 1f / 2f);
			g2.fill(backgroundRect);

			// draw translucent background left
			g2.setColor(bgColorLeft);
//			GraphicsUtil.mixAlpha(g2, AlphaComposite.SRC_OVER, 1f / 2f);
			g2.fill(backgroundRectLeft);
		}


		// draw background border
		g2.setColor(ColorUtil.mix(bgColorLeft, Color.black));
		GraphicsUtil.mixAlpha(g2, AlphaComposite.SRC_OVER, 2f / 1f);
		g2.draw(backgroundRect);

		// draw text
		g2.setColor(textColor);
		GraphicsUtil.drawString(g2, text, x + 5, y + 4, false);

		g2.dispose();
	}
*/

	@Override
	public void icyFrameClosed(IcyFrameEvent e) {
		refreshDataRunnable.interrupt();
		playManagment.interrupt();
	}

	@Override
	public void icyFrameOpened(IcyFrameEvent e) {}

	@Override
	public void icyFrameClosing(IcyFrameEvent e) {}

	@Override
	public void icyFrameIconified(IcyFrameEvent e) {}

	@Override
	public void icyFrameDeiconified(IcyFrameEvent e) {}

	@Override
	public void icyFrameActivated(IcyFrameEvent e) {}

	@Override
	public void icyFrameDeactivated(IcyFrameEvent e) {}

	@Override
	public void icyFrameInternalized(IcyFrameEvent e) {}

	@Override
	public void icyFrameExternalized(IcyFrameEvent e) {}



}
