package plugins.fab.livemousetracker.liveanalysis.client;



import icy.system.thread.Processor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import plugins.fab.livemousetracker.liveanalysis.server.ClientSocketRequestRegisterer;

public class LiveRFIDServer implements Runnable {

	@Override
	public void run() {

		// start a server to receive info.

		try {

			System.out.println("Live RFID server started. (provides all rfid reading to clients)");

			ServerSocket socket;
			socket = new ServerSocket( 55045 ); // 7101
			System.out.println("Live RFID server server listenning on port " + socket.getLocalPort() );

			while ( !Thread.interrupted() )
			{
				Socket socketToUse = socket.accept();

				Processor p = new Processor( 1 );
				p.execute( new RFIDListenerSocket( socketToUse ) );
			}

			socket.close();

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
