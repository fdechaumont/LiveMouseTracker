package plugins.fab.livemousetracker.remotearena.server;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import icy.main.Icy;
import icy.sequence.Sequence;
import plugins.fab.livemousetracker.ImageKinect;
import plugins.fab.livemousetracker.LiveMouseTracker;
import plugins.fab.livemousetracker.remotearena.ImageKinectSerialized;
import plugins.fab.livemousetracker.remotearena.MultiArena;
import plugins.fab.livemousetracker.remotearena.RemoteAntennaMessageSerialized;
import plugins.fab.livemousetracker.remotearena.RemoteArenaInfo;
import plugins.fab.livemousetracker.remotearena.RemoteSystemMessageSerialized;
import plugins.fab.livemousetracker.remotearena.RemoteAntennaMessageSerialized.CLIENT_ANTENNA_MESSAGE;
import plugins.fab.livemousetracker.remotearena.RemoteSystemMessageSerialized.CLIENT_MESSAGE_TYPE;
import plugins.fab.livemousetracker.remotearena.RemoteSystemMessageSerialized.SERVER_REQUEST;
import plugins.fab.livemousetracker.remotearena.client.RemoteAntennaDescriptor;

public class RegisteredArenaClient {

	ObjectOutputStream os = null;
	ArrayList<ImageKinect> imageRemoteQueueList = new ArrayList<>();
	Sequence outSequence = null;
	ArrayList <RemoteAntenna> remoteAntennaList = new ArrayList<>();
	RemoteArenaInfo remoteArenaInfo = null;
	boolean showOutSequence;
	private LiveMouseTracker liveMouseTracker;
	private LMTRemoteAreaServer lmtRemoteServer;
	Socket socket;
	Thread processClientThread;
	boolean enabled=true;

	public RegisteredArenaClient( boolean showOutSequence , LiveMouseTracker liveMouseTracker, LMTRemoteAreaServer lmtRemoteServer, Socket socket )
	{
		this.showOutSequence = showOutSequence;
		this.liveMouseTracker = liveMouseTracker;
		this.lmtRemoteServer = lmtRemoteServer;
		this.socket = socket;

		this.processClientThread = new Thread() {

			@Override
			public void run() {
				processClient();
			}
		};

		this.processClientThread.start();
	}

	public void processClient()
	{
		try {

			System.out.println("[RegisteredArenaClient] started. Client accepted with socket: " + socket );


			//if ( socket.getRemoteSocketAddress().toString() )

			// create out before input else deadlock
			this.os = new ObjectOutputStream(socket.getOutputStream());
			os.flush();
			ObjectInputStream is = new ObjectInputStream(socket.getInputStream());

			askInfoToClient();

			if ( showOutSequence )
			{
				outSequence = new Sequence("Remote client " + socket.getInetAddress() );
				Icy.getMainInterface().addSequence( outSequence );
			}

			while( enabled )
			{
				//if ( is.available()>0 )
				{
					try {

						{
							Object remoteObject = is.readObject();

							// System.out.println( "available in is: " + is.available() );

							if ( remoteObject instanceof ImageKinectSerialized )
							{
								if( this.remoteArenaInfo == null )
								{
									System.out.println("Remote Arena info not received yet...");
									askInfoToClient();
									try {
										Thread.sleep( 200 );
									} catch (InterruptedException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
									continue;
								}

								ImageKinectSerialized imageKinect = (ImageKinectSerialized) remoteObject;

								synchronized( imageRemoteQueueList )
								{
									imageRemoteQueueList.add(
											new ImageKinect( imageKinect.getInfraImage(), imageKinect.getDepthImage() ,
													//											(int)remoteArenaInfo.localization.getX() + (int)remoteArenaInfo.cropRectangle.x,
													//											(int)remoteArenaInfo.localization.getY() + (int)remoteArenaInfo.cropRectangle.y ) );
													(int)remoteArenaInfo.localization.getX() ,
													(int)remoteArenaInfo.localization.getY() ) );

									if ( imageRemoteQueueList.size() > LiveMouseTracker.MAX_IMAGES_IN_REMOTE_ARENA_IMAGELIST )
									{
										imageRemoteQueueList.remove( 0 );
									}
								}

								if ( outSequence != null )
								{
									outSequence.setImage( 0, 0, imageKinect.getInfraImage() );
								}

							}

							if ( remoteObject instanceof RemoteSystemMessageSerialized )
							{
								RemoteSystemMessageSerialized messageFromClient = (RemoteSystemMessageSerialized) remoteObject;
								System.out.println("Message from client: " +  messageFromClient.getMessage() );

								if ( messageFromClient.getClientMessageType() == CLIENT_MESSAGE_TYPE.INIT_INFO )
								{
									System.out.println("Server is initializing RFID antenna.");
									remoteAntennaList.clear();
									for ( RemoteAntennaDescriptor rad : messageFromClient.getRemoteAntennaDescriptorList() )
									{
										System.out.println("Remote declared antenna is: " + rad.identifier );

										this.remoteArenaInfo = messageFromClient.getRemoteArenaInfo();

										RemoteAntenna remoteAntenna = new RemoteAntenna( messageFromClient.getRemoteArenaInfo(),
												rad.location, rad.ray, rad.identifier , this );

										synchronized ( remoteAntennaList ) {

											remoteAntennaList.add( remoteAntenna );

										}

										if( liveMouseTracker != null )
										{
											//									RemoteAntenna remoteAntenna = new RemoteAntenna(
											//											messageFromClient.getRemoteSetupInfo(),
											//											rad.location, rad.ray, rad.identifier , lmtRemoteServer );


											System.out.println( "RegisterArenaClient LiveMouseTRacker.rfidManager: " + LiveMouseTracker.rfidManager );
											System.out.println( "Registering remote antenna in LMT..." );

											// if rfidmanager is not ready yet, wait for it.
											/*
									while ( LiveMouseTracker.rfidManager == null )
									{
										System.out.println("Waiting RFID manager to be ready.");
										try {
											Thread.sleep(1000);
										} catch (InterruptedException e) {
											e.printStackTrace();
										}
									}
											 */
											LiveMouseTracker.rfidManager.addAntenna( remoteAntenna );

										}

									}
									LiveMouseTracker.updateAllROICage();
								}

							}

							if ( remoteObject instanceof RemoteAntennaMessageSerialized )
							{
								RemoteAntennaMessageSerialized messageFromClient = (RemoteAntennaMessageSerialized) remoteObject;

								if( messageFromClient.getClientAntennaMessage() == CLIENT_ANTENNA_MESSAGE.RFID_READ )
								{
									RemoteAntenna remoteAntenna = lmtRemoteServer.getAntenna( messageFromClient.getIdentifier() );

									remoteAntenna.fireRFIDEvent( messageFromClient.getParameter() );
								}
							}

						}
					} catch (ClassNotFoundException e) {
						System.out.println("can't deal with object sent.");
						e.printStackTrace();
					}
					catch (EOFException e) {
						System.out.println("Nothing to read");
						//e.printStackTrace();
					}
				}
			}


			Thread.yield();


		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}



		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		lmtRemoteServer.unRegisterClient( this );
		for ( RemoteAntenna remoteAntenna : remoteAntennaList )
		{
			LiveMouseTracker.rfidManager.removeAntenna( remoteAntenna );
		}

		LiveMouseTracker.updateAllROICage();

	}

	private void askInfoToClient() {

		System.out.println("[RegisteredClient] Asking arena info to client.");
		// init by asking remote info
		RemoteSystemMessageSerialized messageToClient = new RemoteSystemMessageSerialized(
				SERVER_REQUEST.ASK_INIT_INFO, null, null, null, null, "Server say: " );

		synchronized ( os )
		{
			try {
				os.writeUnshared( messageToClient );
				os.reset();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void sendAntennaMessageToClient( RemoteAntennaMessageSerialized messageToClient ) {

		synchronized ( os )
		{
			try {
				os.writeUnshared( messageToClient );
				os.reset();
			} catch (IOException e) {
				//e.printStackTrace();
				System.out.println("Client is disconnected");
				this.setEnable( false );
			}
		}
	}

	/*
	public ArrayList<ImageKinect> getImageRemoteQueueList() {

		synchronized( imageRemoteQueueList )
		{
			return new ArrayList<ImageKinect> ( imageRemoteQueueList );
		}

	}

	public void consumeOneImage() {
		synchronized( imageRemoteQueueList )
		{
			imageRemoteQueueList.remove( 0 );
		}
	}
	*/

	int MAX_IMAGES_HOLD = 60;
	int currentHoldNumber = MAX_IMAGES_HOLD;
	ImageKinect imageHold = null;

	/**
	 * This function provide the next remote image available, but also use an hold
	 * to provide an image if there is no image remaining. This allows to avoid blinking if setup are
	 * not synchronized and let the system catch up.
	 * @return
	 */
	public ImageKinect getNextRemoteImage() {

		ImageKinect imageToReturn = null;

		synchronized( imageRemoteQueueList )
		{
			if ( imageRemoteQueueList.size() > 0 )
			{
				imageToReturn = imageRemoteQueueList.get( 0 );
				currentHoldNumber = MAX_IMAGES_HOLD;
				imageHold = imageToReturn;
				imageRemoteQueueList.remove( 0 );
			}else
			{
				if ( currentHoldNumber > 0 )
				{
					imageToReturn = imageHold;
					System.out.println("RegisteredArena: Using HOLD image (hold number=" + currentHoldNumber +")");
				}else
				{
					System.out.println("RegisteredArena: FRAME MISSED (hold number=" + currentHoldNumber +")");
				}
				currentHoldNumber--;
			}

		}

		if ( currentHoldNumber < 0 )
		{
			currentHoldNumber = 0;
		}


		return imageToReturn;


	}

	public RemoteArenaInfo getRemoteArenaInfo() {
		return remoteArenaInfo;

	}

	public void setEnable(boolean b) {

		enabled = false;
	}

}
