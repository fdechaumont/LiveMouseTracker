package plugins.fab.livemousetracker.network;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class NetworkUtil {

	static String localIP = null;

	/** gets local IP and cache it */
	public static String getLocalIP()
	{
		if ( localIP != null )
		{
			return localIP;
		}

		try {
			//InetAddress ip = InetAddress.getLocalHost();
			//localIP = InetAddress.getLocalHost().toString();
			localIP = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		/*

		try( final DatagramSocket socket = new DatagramSocket() )
		{
			socket.connect(InetAddress.getByName("8.8.8.8"), 10002);

			localIP = socket.getLocalAddress().getHostAddress();
			System.out.println("local ip: " + localIP );

		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		return null;
	}

}
