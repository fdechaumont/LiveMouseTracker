package plugins.fab.livemousetracker.remotearena.client;

import java.awt.Color;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import javax.swing.JFrame;
import javax.swing.JTextField;

import icy.gui.frame.IcyFrame;
import icy.gui.frame.IcyFrameEvent;
import icy.gui.frame.IcyFrameListener;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.main.Icy;
import icy.plugin.abstract_.PluginActionable;

import icy.sequence.Sequence;
import icy.system.profile.Chronometer;
import icy.type.collection.array.Array1DUtil;
import plugins.fab.kinectdriver.KinectData;
import plugins.fab.kinectdriver.KinectEvent;
import plugins.fab.kinectdriver.KinectListener;
import plugins.fab.kinectdriver.KinectStreamer;
import plugins.fab.livemousetracker.remotearena.ImageKinectSerialized;
import plugins.fab.livemousetracker.remotearena.MultiArena;
import plugins.fab.livemousetracker.remotearena.RemoteAntennaMessageSerialized;
import plugins.fab.livemousetracker.remotearena.RemoteArenaInfo;
import plugins.fab.livemousetracker.remotearena.RemoteSystemMessageSerialized;
import plugins.fab.livemousetracker.remotearena.RemoteAntennaMessageSerialized.SERVER_ANTENNA_MESSAGE;
import plugins.fab.livemousetracker.remotearena.RemoteSystemMessageSerialized.CLIENT_MESSAGE_TYPE;
import plugins.fab.livemousetracker.remotearena.RemoteSystemMessageSerialized.SERVER_REQUEST;
import plugins.fab.livemousetracker.rfid.Antenna;
import plugins.fab.livemousetracker.rfid.RFIDAntenna;
import plugins.fab.livemousetracker.rfid.RFIDManager2;
import plugins.kernel.roi.roi2d.ROI2DPolygon;
import plugins.kernel.roi.roi2d.ROI2DRectangle;


/**
 * TCP Client for networkAreaExtender
 * @author Fab
 *
 */
public class MultiArenaClient extends PluginActionable implements KinectListener, ActionListener, IcyFrameListener {

	KinectStreamer kinectStreamer = new KinectStreamer( false );
	RemoteClientRFIDManager clientRFIDManager = new RemoteClientRFIDManager( this );
	MultiArenaClientPanel multiArenaClientPanel;
	String serverIP;
	RemoteArenaInfo remoteArenaInfo = null; //new RemoteArenaInfo();
	Socket socket;
	ObjectOutputStream os;
	ObjectInputStream is;
	boolean connectionError = false;

	@Override
	public void run() {

		// start the stream.
		System.out.println("Starting... remote client.");

		IcyFrame mainFrame = new IcyFrame("Multi Arena Client");
		mainFrame.addFrameListener( this );
		multiArenaClientPanel = new MultiArenaClientPanel();
		mainFrame.getContentPane().add( multiArenaClientPanel );
		mainFrame.pack();
		mainFrame.addToDesktopPane();
		mainFrame.center();
		mainFrame.setVisible( true );
		mainFrame.addFrameListener( this );
		multiArenaClientPanel.getBtnStart().addActionListener( this );
		connectionError = false;

	}

	Thread readingOrderThread = null;
//	boolean clientReady = false;

	ROI2DRectangle cropROI ;

	void setStatus( String status )
	{
		multiArenaClientPanel.getStatusLabel().setText( status );
	}

	void initRFID()
	{
		setStatus("Starting RFID...");
		System.out.println("Starting RFID...");

		clientRFIDManager = new RemoteClientRFIDManager( this );
		clientRFIDManager.addAntenna( new RFIDAntenna( new Point2D.Double( 133,   81 ) , 30 , "COM30" ) ); // 23
		clientRFIDManager.addAntenna( new RFIDAntenna( new Point2D.Double( 214,   81 ) , 30 , "COM31" ) ); // 25
		clientRFIDManager.addAntenna( new RFIDAntenna( new Point2D.Double( 296,   81 ) , 30 , "COM32" ) ); // 13
		clientRFIDManager.addAntenna( new RFIDAntenna( new Point2D.Double( 378,   81 ) , 30 , "COM33" ) ); // 12

		clientRFIDManager.addAntenna( new RFIDAntenna( new Point2D.Double( 133,  166 ) , 30 , "COM34" ) ); // 24
		clientRFIDManager.addAntenna( new RFIDAntenna( new Point2D.Double( 214,  166 ) , 30 , "COM35" ) ); // 26
		clientRFIDManager.addAntenna( new RFIDAntenna( new Point2D.Double( 296,  166 ) , 30 , "COM36" ) ); // 17
		clientRFIDManager.addAntenna( new RFIDAntenna( new Point2D.Double( 378,  166 ) , 30 , "COM37" ) ); // 11

		clientRFIDManager.addAntenna( new RFIDAntenna( new Point2D.Double( 133,  249 ) , 30 , "COM38" ) ); // 20
		clientRFIDManager.addAntenna( new RFIDAntenna( new Point2D.Double( 214,  249 ) , 30 , "COM39" ) ); // 19
		clientRFIDManager.addAntenna( new RFIDAntenna( new Point2D.Double( 296,  249 ) , 30 , "COM40" ) ); // 16
		clientRFIDManager.addAntenna( new RFIDAntenna( new Point2D.Double( 378,  249 ) , 30 , "COM41" ) ); // 14

		clientRFIDManager.addAntenna( new RFIDAntenna( new Point2D.Double( 133,  336 ) , 30 , "COM42" ) ); // 21
		clientRFIDManager.addAntenna( new RFIDAntenna( new Point2D.Double( 214,  336 ) , 30 , "COM43" ) ); // 22
		clientRFIDManager.addAntenna( new RFIDAntenna( new Point2D.Double( 296,  336 ) , 30 , "COM44" ) ); // 15
		clientRFIDManager.addAntenna( new RFIDAntenna( new Point2D.Double( 378,  336 ) , 30 , "COM45" ) ); // 18

	}

	private ROI2DRectangle getROIFromText( String txt ) {

		String[] textSplitted = txt.split(",");
		int x1 = Integer.parseInt( textSplitted[0] );
		int y1 = Integer.parseInt( textSplitted[1] );
		int x2 = Integer.parseInt( textSplitted[2] );
		int y2 = Integer.parseInt( textSplitted[3] );
		Rectangle rectangle = new Rectangle( x1 , y1, x2-x1 , y2 - y1 );

		ROI2DRectangle roi = new ROI2DRectangle( rectangle );
		roi.setColor( Color.yellow );
		roi.setReadOnly( true );

		return roi;


	}

	void startClient( String serverIP )
	{
		setStatus("Starting...");
		String offsetText = multiArenaClientPanel.getOffsetTextField().getText();
		String[] offsetTextSplitted = offsetText.split(",");
		double offsetArenaX = Double.parseDouble( offsetTextSplitted[0] );
		double offsetArenaY = Double.parseDouble( offsetTextSplitted[1] );
		Point2D offsetArena = new Point2D.Double( offsetArenaX, offsetArenaY );

		String cropText = multiArenaClientPanel.getCropTextField().getText();
		String[] cropTextSplitted = cropText.split(",");
		int cropX1 = Integer.parseInt( cropTextSplitted[0] );
		int cropY1 = Integer.parseInt( cropTextSplitted[1] );
		int cropX2 = Integer.parseInt( cropTextSplitted[2] );
		int cropY2 = Integer.parseInt( cropTextSplitted[3] );
		Rectangle cropRectangleArena = new Rectangle( cropX1 , cropY1, cropX2-cropX1 , cropY2 - cropY1 );

		cropROI = new ROI2DRectangle( cropRectangleArena );
		cropROI.setColor( Color.red );
		cropROI.setReadOnly( true );

		// ROI informative
		/*
		ROI2DRectangle cageFloorROI = getROIFromText( multiArenaClientPanel.getTextFieldCageFloor().getText() );
		cageFloorROI.setColor( Color.yellow.darker() );
		ROI2DRectangle cageROI = getROIFromText( multiArenaClientPanel.getTextFieldROICage().getText() );
		cageROI.setColor( Color.yellow );
		*/

		/*
				int middle = 206;

		ROI2DPolygon roiCage50x50 = new ROI2DPolygon( new Point2D.Double( 86-5+3, 55-5-17 ) );
		roiCage50x50.addNewPoint( new Point2D.Double( 420+5+3, 55-5 -17), false);

		// corridor
		roiCage50x50.addNewPoint( new Point2D.Double( 420+5+3, middle - corridorThickness ), false);
		roiCage50x50.addNewPoint( new Point2D.Double( 512, middle - corridorThickness ), false);
		roiCage50x50.addNewPoint( new Point2D.Double( 512, middle + corridorThickness ), false);
		roiCage50x50.addNewPoint( new Point2D.Double( 420+5+3, middle + corridorThickness ), false);

		roiCage50x50.addNewPoint( new Point2D.Double( 420+5+3, 395+5 -17), false);
		roiCage50x50.addNewPoint( new Point2D.Double(  86-5+3, 395+5 -17), false);
		roiCage50x50.setCreating( false );

		this.ROICage = roiCage50x50;

		ROI2DPolygon roiCage50x50Floor = new ROI2DPolygon( new Point2D.Double( 86-5+3 +30, 55-5-17 +30 ) );
		roiCage50x50Floor.addNewPoint( new Point2D.Double( 420+5+3 -30 , 55-5 -17 +30 ), false);

		// corridor
		roiCage50x50Floor.addNewPoint( new Point2D.Double( 420+5+3-30, middle - corridorThickness ), false);
		roiCage50x50Floor.addNewPoint( new Point2D.Double( 512, middle - corridorThickness ), false);
		roiCage50x50Floor.addNewPoint( new Point2D.Double( 512, middle + corridorThickness ), false);
		roiCage50x50Floor.addNewPoint( new Point2D.Double( 420+5+3-30, middle + corridorThickness ), false);

		roiCage50x50Floor.addNewPoint( new Point2D.Double( 420+5+3 -30 , 395+5 -17 -30 ), false);
		roiCage50x50Floor.addNewPoint( new Point2D.Double(  86-5+3 +30 , 395+5 -17 -30 ), false);
		roiCage50x50Floor.setCreating( false );

		 */

		// Multi with corridor en right 50x50 cage
		int corridorThickness = 13;
		int middle = 206;

		Polygon roiCage50x50 = new Polygon();
		roiCage50x50.addPoint( 86-5+3, 55-5-17 );
		roiCage50x50.addPoint( 420+5+3, 55-5 -17);
		roiCage50x50.addPoint( 420+5+3, 395+5 -17);
		roiCage50x50.addPoint(  86-5+3, 395+5 -17);

		// corridor
		roiCage50x50.addPoint( 86-5+3, (int) ( middle + corridorThickness ) );
		roiCage50x50.addPoint( 0, ( int ) ( middle + corridorThickness ) );
		roiCage50x50.addPoint( 0, (int) ( middle - corridorThickness  ) );
		roiCage50x50.addPoint( 86-5+3, (int) ( middle - corridorThickness ) );



		Polygon roiCage50x50Floor = new Polygon();
		roiCage50x50Floor.addPoint( 86-5+3 +30, 55-5-17 +30 );
		roiCage50x50Floor.addPoint( 420+5+3 -30 , 55-5 -17 +30 );
		roiCage50x50Floor.addPoint( 420+5+3 -30 , 395+5 -17 -30 );
		roiCage50x50Floor.addPoint( 86-5+3 +30 , 395+5 -17 -30 );

		// corridor
		roiCage50x50Floor.addPoint( 86-5+3+30, middle + corridorThickness );
		roiCage50x50Floor.addPoint( 0, middle + corridorThickness );
		roiCage50x50Floor.addPoint( 0, middle - corridorThickness );
		roiCage50x50Floor.addPoint( 86-5+3+30, middle - corridorThickness );



		this.remoteArenaInfo = new RemoteArenaInfo(
				offsetArena , cropRectangleArena, "Client LMT", roiCage50x50 , roiCage50x50Floor );

		this.serverIP = serverIP;

		System.out.println("Starting client to server at IP: " + serverIP );

		initRFID();

//		connect();

		setStatus("Starting Kinect...");
		System.out.println("Starting Kinect feed...");

		kinectStreamer.addKinectListener( MultiArenaClient.this );
		kinectStreamer.startLive();

		System.out.println("Kinect feed started.");
//				clientReady = true;
//			}
//		};

//		starter.run();
//		startingThread = new Thread( starter );
//		startingThread.start();

		Runnable readingOrderRunnable = new Runnable() {

			@Override
			public void run() {

				System.out.println("Read order thread started.");

				while ( true )
				{
					if ( shutDown )
					{
						setStatus("Shutting down");
						System.out.println("Shutting down client");
						clientRFIDManager.shutDownAll();
						kinectStreamer.stopLive();
						return;
					}

					if ( connectionError )
					{
						System.out.println("Managing connection error...");
						setStatus("Connect error");
						connectNbAttempt=0;
						connectionError = false;

						try {
							socket.close();
							clientRFIDManager.shutDownAll();
							initRFID();
							socket = null;
						//} catch (IOException e | NullPointerException e2 ) {
						} catch ( Exception e ) {
							System.out.println("Close socket error / null socket.");
							//e.printStackTrace();
						}
					}


					//if( socket == null || socket.isConnected() == false )
					if ( !isConnected() )
					{
						try {
							Thread.sleep( 1000 ); // to avoid connect attempt spam
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						connect();
					}

					if ( isConnected() )
					{
						readServerRequest();
					}

					Thread.yield();
				}
			}
		};

		readingOrderThread = new Thread( readingOrderRunnable );
		readingOrderThread.start();

	}



	boolean isConnected()
	{
		if ( socket == null )
		{
			return false;
		}
		if ( socket.isClosed() )
		{
			return false;
		}
		if ( socket.isConnected() ) // is connected means "has been ever connected"
		{
			return true;
		}
		return false;
	}

	int connectNbAttempt = 0;

	private void connect() {

		try {
			connectNbAttempt++;
			String status = "Connecting...( attempt #" + connectNbAttempt + " )";
			setStatus( status );
			System.out.println( status );
			System.out.println("Open socket");
			socket = new Socket( serverIP , MultiArena.NETWORK_PORT );
//			socket.setSoTimeout( 2000 );
			System.out.println("Open output stream");
			os = new ObjectOutputStream(socket.getOutputStream());
			os.flush();
			System.out.println("Open input stream");
			is = new ObjectInputStream(socket.getInputStream());

			if( is != null && os!=null )
			{
				setStatus("Connected.");
				connectNbAttempt = 0;
			}
		} catch (IOException e) {
//			e.printStackTrace();
			System.out.println("Can't connect to " + serverIP + " nb attempt: " + connectNbAttempt );
		}

	}

	Thread startingThread = null;

	public void stop()
	{
		shutDown = true;
		kinectStreamer.stopLive();
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static Sequence infraOut = null;
	private static Sequence depthOut = null;
	boolean DISPLAY_DEPTH_SEQUENCE = false;
	boolean lutInfraDone = false;
	ArrayList<ImageKinectSerialized> imageQueueList = new ArrayList<ImageKinectSerialized>();

	@Override
	public void kinectChange(Sequence sourceSequence, KinectData kinectData, KinectEvent kinectEvent) {

		if ( kinectEvent == KinectEvent.NEW_DEPTH_SEQUENCE )
		{
			depthOut = sourceSequence;
			if ( DISPLAY_DEPTH_SEQUENCE )
			{
				addSequence( depthOut );
				depthOut.addROI( cropROI );
			}
		}

		if ( kinectEvent == KinectEvent.NEW_INFRARED_SEQUENCE )
		{
			infraOut = sourceSequence;

			addSequence( infraOut );
			infraOut.addROI( cropROI );

			ROI2DPolygon roiCage = new ROI2DPolygon( this.remoteArenaInfo.getUntranslatedCagePolygon() );
			roiCage.setReadOnly( true );
			roiCage.setColor( Color.yellow );
			infraOut.addROI( roiCage );

			ROI2DPolygon roiCageFloor = new ROI2DPolygon( this.remoteArenaInfo.getUntranslatedCageFloorPolygon() );
			roiCageFloor.setReadOnly( true );
			roiCageFloor.setColor( Color.yellow );
			infraOut.addROI( roiCageFloor );
/*
			ROI2DRectangle roiCage = new ROI2DRectangle( this.remoteArenaInfo.getUntranslatedCageRectangle() );
			roiCage.setReadOnly( true );
			roiCage.setColor( Color.yellow );
			infraOut.addROI( roiCage );

			ROI2DRectangle roiCageFloor = new ROI2DRectangle( this.remoteArenaInfo.getUntranslatedCageFloorRectangle() );
			roiCageFloor.setReadOnly( true );
			roiCageFloor.setColor( Color.yellow );
			infraOut.addROI( roiCageFloor );
*/
		}

		if ( kinectEvent == KinectEvent.NEW_INFRARED_CAPTURE )
		{
//			System.out.println("New capture");
			if (!lutInfraDone && (infraOut.getFirstViewer() != null))
			{
				infraOut.getFirstViewer().getLut().getLutChannel(0).setMinMax( 0, 32000 );
				lutInfraDone = true;
			}

//			IcyBufferedImage img;
//			img = infraOut.getImage( 0 , 0 );
//			IcyBufferedImage newInfraImage = new IcyBufferedImage(img.getWidth(), img.getHeight(), Array1DUtil.copyOf(img.getDataXY(0)));
//			img = depthOut.getImage( 0 , 0 );
//			IcyBufferedImage newDepthImage = new IcyBufferedImage(img.getWidth(), img.getHeight(), Array1DUtil.copyOf(img.getDataXY(0)));

			Rectangle cropRectangle = remoteArenaInfo.cropRectangle;

			IcyBufferedImage newInfraImage = IcyBufferedImageUtil.getSubImage(
					infraOut.getImage( 0 ,  0 ) , cropRectangle ) ;
			IcyBufferedImage newDepthImage = IcyBufferedImageUtil.getSubImage(
					depthOut.getImage( 0 ,  0 ) , cropRectangle ) ;

			synchronized ( imageQueueList )
			{
				imageQueueList.add( new ImageKinectSerialized( newInfraImage , newDepthImage ) );
			}

			sendImageData();

		}

		if ( kinectEvent == KinectEvent.NEW_DEPTH_CAPTURE )
		{
			// WARNING: DO NOT PROCESS ON THIS EVENT, DEPTH COMES BEFORE INFRARED
			// WAIT FOR INFRARED TO BE SYNCHRONIZED
		}

		if ( kinectEvent == KinectEvent.KINECT_STOPPED )
		{
			System.out.println("Kinect stopped");
			//stop();
		}

	}

	private void readServerRequest() {

        RemoteSystemMessageSerialized messageFromServer = null;

        if( is == null )
        {
        	return;
        }

        try
        {
        	try {

        		Object remoteObject = null;
        		synchronized( is )
        		{
        			remoteObject = is.readObject();
        		}

        		if ( remoteObject instanceof RemoteSystemMessageSerialized )
        		{
        			messageFromServer = (RemoteSystemMessageSerialized) remoteObject;
        			System.out.println( "[Client] Message from server: " + messageFromServer.getMessage() );

        			if ( messageFromServer.getServerRequest() == SERVER_REQUEST.ASK_INIT_INFO )
        			{
        				// return the list of antenna, and the system info (offset x,y)

        				System.out.println("[Client] client answering to ASK_INIT_INFO server's demand.");
        				System.out.println("[Client] Remote Arena Info: " + remoteArenaInfo );
        				RemoteSystemMessageSerialized answer =
        						new RemoteSystemMessageSerialized( null,
        								CLIENT_MESSAGE_TYPE.INIT_INFO ,
        								null, remoteArenaInfo, null, "Client send remote arena info" );

        				for ( Antenna antenna : clientRFIDManager.getAntennaList() )
        				{
        					answer.addRemoteAntennaDescriptor( antenna );
        				}

        				System.out.println("[Client] Answer: " + answer );

        				synchronized( os )
        				{
        					System.out.println("[Client] Writing answer." );
        					os.writeUnshared( answer );
        					System.out.println("[Client] Resetting outputBuffer" );
        					os.reset();
        					System.out.println("[Client] Answer finished" );
        				}
        			}

        		}

        		if ( remoteObject instanceof RemoteAntennaMessageSerialized )
        		{
        			RemoteAntennaMessageSerialized messageFromClient = (RemoteAntennaMessageSerialized) remoteObject;
        			Antenna antenna = getAntenna( messageFromClient.getIdentifier() );
        			if ( antenna == null )
        			{
        				System.err.println("ERROR: MultiArenaClient: remote antenna received is null. (messageIdentifier=" + messageFromClient.getIdentifier() );
        			}else
        			{
        				if( messageFromClient.getServerAntennaMessage() == SERVER_ANTENNA_MESSAGE.SWITCH_OFF )
        				{
        					//					Antenna antenna = getAntenna( messageFromClient.getIdentifier() );
        					antenna.switchOff();
        				}

        				if( messageFromClient.getServerAntennaMessage() == SERVER_ANTENNA_MESSAGE.SHUTDOWN )
        				{
        					//					Antenna antenna = getAntenna( messageFromClient.getIdentifier() );
        					antenna.shutdown();
        				}

        				if( messageFromClient.getServerAntennaMessage() == SERVER_ANTENNA_MESSAGE.ENABLED_TRUE )
        				{
        					//					Antenna antenna = getAntenna( messageFromClient.getIdentifier() );
        					antenna.setEnabled( true );
        				}

        				if( messageFromClient.getServerAntennaMessage() == SERVER_ANTENNA_MESSAGE.ENABLED_FALSE )
        				{
        					//					Antenna antenna = getAntenna( messageFromClient.getIdentifier() );
        					antenna.setEnabled( false );
        				}
        			}
        		}


        	} catch (ClassNotFoundException | IOException e ) {
        		e.printStackTrace();
        		connectionError = true;
        	}
        }catch( NullPointerException e )
        {
        	e.printStackTrace();
        }

		if ( messageFromServer == null ) return;

	}

	public void sendMessageToServer( RemoteAntennaMessageSerialized message ) {

		if ( os == null )
		{
			return;
		}

		synchronized( os )
		{
			try {
				os.writeUnshared( message );
				os.reset();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private Antenna getAntenna(String identifier) {
		for ( Antenna antenna : clientRFIDManager.getAntennaList() )
		{
			if ( antenna.getIdentifier().equals( identifier ) )
			{
				return antenna;
			}
		}
		return null;
	}

	private void sendImageData() {

		if ( !isConnected() )
		{
			return;
		}
//		Chronometer sendDataChrono = new Chronometer("Send data");

		try {

			synchronized ( imageQueueList ) {
				ImageKinectSerialized imageKinect = null;

				if ( imageQueueList.size() > 0 )
				{
					imageKinect = imageQueueList.get( 0 );
					imageQueueList.remove( 0 );
//					System.out.println("Number of image in Queue: " + imageQueueList.size() );
					imageQueueList.clear();
				}

				if ( imageKinect!=null )
				{
					if( os != null )
					{
						synchronized( os )
						{
							os.writeUnshared( imageKinect );
							os.reset();
						}
					}
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
//		sendDataChrono.displayMs();

	}

	public static int sizeof(Object obj) throws IOException {

	    ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
	    ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteOutputStream);

	    Chronometer sendDataChrono = new Chronometer("Write object");
	    objectOutputStream.writeObject(obj);
	    sendDataChrono.displayMs();

	    objectOutputStream.flush();
	    objectOutputStream.close();

	    return byteOutputStream.toByteArray().length;
	}

	Thread startThread = null;
	@Override
	public void actionPerformed(ActionEvent arg0) {
		Runnable startRunnable = new Runnable() {

			@Override
			public void run() {
				startClient( multiArenaClientPanel.getTxtServerIP().getText() );
			}
		};
		startThread = new Thread( startRunnable );
		startThread.start();
	}

	@Override
	public void icyFrameClosing(IcyFrameEvent e) {
		stop();
	}

	@Override
	public void icyFrameOpened(IcyFrameEvent e) {}

	boolean shutDown = false;
	@Override
	public void icyFrameClosed(IcyFrameEvent e) {
		shutDown = true;
	}

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
