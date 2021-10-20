package plugins.fab.livemousetracker.remotearena;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import icy.plugin.abstract_.PluginActionable;
import icy.plugin.interface_.PluginThreaded;
import plugins.fab.livemousetracker.ImageKinect;

public class testServer extends PluginActionable implements PluginThreaded {

	@Override
	public void run() {

		try {
			ServerSocket ssocket = new ServerSocket( MultiArena.NETWORK_PORT );
			System.out.println("[Server] Waiting for client...");
			Socket socket = ssocket.accept();
			System.out.println("[Server] Client accepted with socket: " + socket );
			// create out before input else deadlock
			ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
			os.flush();
			ObjectInputStream is = new ObjectInputStream(socket.getInputStream());

			while( true )
			{
				try {

					Object remoteObject = is.readObject();
					System.out.println( "Server: available in is: " + is.available() );

					if ( remoteObject instanceof RemoteSystemMessageSerialized )
					{
						RemoteSystemMessageSerialized message = (RemoteSystemMessageSerialized) remoteObject;
						System.out.println("Message from client: " +  message.getMessage() );

						// do somothing with it

						message.setMessage( message.getMessage()+ " echo.");
						os.writeObject( message );

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
		} catch (IOException e1) {
			e1.printStackTrace();
		}

	}

}
