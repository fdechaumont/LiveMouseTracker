package plugins.fab.livemousetracker.remotearena;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import icy.plugin.abstract_.PluginActionable;
import icy.plugin.interface_.PluginThreaded;

public class testClient extends PluginActionable implements PluginThreaded {

	@Override
	public void run() {

		Socket socket;
		ObjectOutputStream os;
		ObjectInputStream is;


		System.out.println("Connecting...");
		try {

			socket = new Socket( "localhost" , MultiArena.NETWORK_PORT );
			os = new ObjectOutputStream(socket.getOutputStream());
			os.flush();
			is = new ObjectInputStream(socket.getInputStream());
			int i = 0;
			while( true )
			{

				RemoteSystemMessageSerialized message = new RemoteSystemMessageSerialized( null, null, null , null, null, "message from client #"+i++ );
				os.writeObject( message );
				os.reset();

				Object remoteObject = is.readObject();
				if ( remoteObject instanceof RemoteSystemMessageSerialized )
				{
					RemoteSystemMessageSerialized messageReceived = (RemoteSystemMessageSerialized) remoteObject;
					System.out.println("Message from server: " +  message.getMessage() );
				}

				Thread.sleep( 100 );
			}

		} catch (IOException e) {

			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}



}
