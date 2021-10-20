package plugins.fab.livemousetracker.remotearena.server;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import icy.main.Icy;
import icy.plugin.abstract_.PluginActionable;
import icy.plugin.interface_.PluginThreaded;
import icy.sequence.Sequence;
import plugins.fab.livemousetracker.ImageKinect;
import plugins.fab.livemousetracker.LiveMouseTracker;
import plugins.fab.livemousetracker.remotearena.ImageKinectSerialized;
import plugins.fab.livemousetracker.remotearena.MultiArena;
import plugins.fab.livemousetracker.remotearena.RemoteAntennaMessageSerialized;
import plugins.fab.livemousetracker.remotearena.RemoteSystemMessageSerialized;
import plugins.fab.livemousetracker.remotearena.RemoteAntennaMessageSerialized.CLIENT_ANTENNA_MESSAGE;
import plugins.fab.livemousetracker.remotearena.RemoteAntennaMessageSerialized.SERVER_ANTENNA_MESSAGE;
import plugins.fab.livemousetracker.remotearena.RemoteSystemMessageSerialized.CLIENT_MESSAGE_TYPE;
import plugins.fab.livemousetracker.remotearena.RemoteSystemMessageSerialized.SERVER_REQUEST;
import plugins.fab.livemousetracker.remotearena.client.RemoteAntennaDescriptor;
import plugins.fab.livemousetracker.rfid.Antenna;

public class LMTRemoteAreaServer {

	// Sequence outSequence = null;
	Runnable serverRunnable;
	Thread serverThread;

	LiveMouseTracker liveMouseTracker = null;
//	ObjectOutputStream os = null;
	ArrayList <RegisteredArenaClient> registeredArenaClientList = new ArrayList<>();

	public LMTRemoteAreaServer( boolean showOutSequence,
			//ArrayList<ImageKinect> imageRemoteQueueList,
			LiveMouseTracker liveMouseTracker ) {

		this.liveMouseTracker = liveMouseTracker;

//		if ( showOutSequence )
//		{
//			outSequence = new Sequence("Remote data");
//			Icy.getMainInterface().addSequence( outSequence );
//		}

		serverRunnable = new Runnable() {

			@Override
			public void run() {

				try {
					ServerSocket ssocket = new ServerSocket( MultiArena.NETWORK_PORT );

					while( true )
					{
						System.out.println("[Server] Waiting for client...");
						Socket socket = ssocket.accept();
						System.out.println("[Server] Client accepted with socket: " + socket );
						// create out before input else deadlock

						String remoteIP = socket.getRemoteSocketAddress().toString();
						System.out.println("Registering Arena: remote IP: " +remoteIP );

						synchronized ( registeredArenaClientList )
						{
							// remove existing arena at same IP

							for ( RegisteredArenaClient registredClient : new ArrayList<RegisteredArenaClient>( registeredArenaClientList ) )
							{
								if ( registredClient.socket.getRemoteSocketAddress().toString().equals(
										socket.getRemoteSocketAddress().toString()
										) )
								{
									System.out.println("Removing existing registered client.");
									registredClient.setEnable( false );
									//registeredArenaClientList.remove( registredClient );

								}
							}

							registeredArenaClientList.add(
									new RegisteredArenaClient( showOutSequence , liveMouseTracker, LMTRemoteAreaServer.this , socket )
									);
						}
						// Thread.yield();
					}
					/*

					LMTRemoteAreaServer.this.os = new ObjectOutputStream(socket.getOutputStream());
					os.flush();
					ObjectInputStream is = new ObjectInputStream(socket.getInputStream());

					// init by asking remote info

					RemoteSystemMessageSerialized messageToClient = new RemoteSystemMessageSerialized(
							SERVER_REQUEST.ASK_INIT_INFO, null, null, null, null, "Server say: " );

					synchronized ( os )
					{
						os.writeUnshared( messageToClient );
						os.reset();
					}

					while( true )
					{
						try {

							Object remoteObject = is.readObject();

							// System.out.println( "available in is: " + is.available() );

							if ( remoteObject instanceof ImageKinectSerialized )
							{
								ImageKinectSerialized imageKinect = (ImageKinectSerialized) remoteObject;

								if ( imageRemoteQueueList != null )
								{
									synchronized( imageRemoteQueueList )
									{
										imageRemoteQueueList.add( new ImageKinect( imageKinect.getInfraImage(), imageKinect.getDepthImage() ) );
									}
								}

								if ( outSequence != null )
								{
									outSequence.setImage( 0, 0, imageKinect.getInfraImage() );
								}

//								double test = Math.random();
//								if ( test < 0.1d )
//								{
//
//									RFIDMessageSerialized message = new RFIDMessageSerialized( null, null, null, "Server say: " + test );
//									os.writeUnshared( message );
//									os.reset();
//								}

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

										if( liveMouseTracker != null )
										{
											RemoteAntenna remoteAntenna = new RemoteAntenna( messageFromClient.getRemoteSetupInfo(),
													rad.location, rad.ray, rad.identifier , LMTRemoteAreaServer.this );

											remoteAntennaList.add( remoteAntenna );

											System.out.println("Registering remote antenna in LMT...");
											LiveMouseTracker.rfidManager.addAntenna( remoteAntenna );

										}

									}
								}

							}

							if ( remoteObject instanceof RemoteAntennaMessageSerialized )
							{
								RemoteAntennaMessageSerialized messageFromClient = (RemoteAntennaMessageSerialized) remoteObject;

								if( messageFromClient.getClientAntennaMessage() == CLIENT_ANTENNA_MESSAGE.RFID_READ )
								{
									RemoteAntenna remoteAntenna = getAntenna( messageFromClient.getIdentifier() );

									remoteAntenna.fireRFIDEvent( messageFromClient.getParameter() );
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


						try {
							Thread.sleep( 0 );
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}
					// FIX: should close sockets.
					//ssocket.close();
					//socket.close();

					 */

				} catch (IOException e1) {
					e1.printStackTrace();
				}


			}


		};

		serverThread = new Thread( serverRunnable );
		serverThread.start();
	}

	public RemoteAntenna getAntenna(String identifier) {
		for ( RegisteredArenaClient client : registeredArenaClientList )
		{
			for ( RemoteAntenna antenna : client.remoteAntennaList )
			{
				if ( antenna.getIdentifier().equals( identifier ) )
				{
					return antenna;
				}
			}
		}
		return null;
	}

	public void unRegisterClient(RegisteredArenaClient registeredArenaClient) {

		synchronized ( registeredArenaClientList ) {
			registeredArenaClientList.remove( registeredArenaClient );
		}

	}

	public ArrayList<RegisteredArenaClient> getRegisteredClientList() {

		synchronized ( registeredArenaClientList ) {
			return new ArrayList<RegisteredArenaClient>( registeredArenaClientList );
		}
	}
}
