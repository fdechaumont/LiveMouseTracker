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
package plugins.fab.livemousetracker;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import icy.type.collection.array.ByteArrayConvert;

/**
 * Cage dimension in Unreal engine4:
 *
 * y: -235 to 235
 * x: -235 to 235
 * z= 830
 *
 * @author Fab
 *
 */
public class UDPSender {

	/** Receive data from a 50x50 cage, in cm.
	 * @deprecated for tests
	 * */
	public static void send2( float x, float y, float z, float rotation, float x2 , float y2, float z2, float rotation2 )
	{
		x = x * ( 470f/50f ) - 235f;
		y = y * ( 470f/50f ) - 235f;
		z = 830;

		x2 = x2 * ( 470f/50f ) - 235f;
		y2 = y2 * ( 470f/50f ) - 235f;
		z2 = 830;

		float [] buffFloat = { x,y,z,rotation , x2,y2,z2,rotation2 };
		byte[] buffer = ByteArrayConvert.floatArrayToByteArray( buffFloat, 0, null, 0, -1, true );

		try
		{
			InetAddress address;
			//System.out.println("Send to localhost:8550.");
			address = InetAddress.getByName("127.0.0.1"); //InetAddress.getByName("192.168.1.106");
			DatagramPacket packet = new DatagramPacket( buffer, buffer.length, address, 8550 );
			DatagramSocket datagramSocket = new DatagramSocket();
			datagramSocket.send(packet);
			datagramSocket.close();
			//System.out.println(InetAddress.getLocalHost().getHostAddress());

		} catch (IOException e) {
			e.printStackTrace();

		}
	}

	public static void send( float[] buffFloat )
	{
		byte[] buffer = ByteArrayConvert.floatArrayToByteArray( buffFloat, 0, null, 0, -1, true );

		try
		{
			InetAddress address;
			//System.out.println("Send to localhost:8550.");
			address = InetAddress.getByName("127.0.0.1"); //InetAddress.getByName("192.168.1.106");
			DatagramPacket packet = new DatagramPacket( buffer, buffer.length, address, 8550 );
			DatagramSocket datagramSocket = new DatagramSocket();
			datagramSocket.send(packet);
			datagramSocket.close();
			//System.out.println(InetAddress.getLocalHost().getHostAddress());

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/** Receive data from a 50x50 cage, in cm. */
	public static void send( float x, float y, float z, float rotation )
	{
		x = x * ( 470f/50f ) - 235f;
		y = y * ( 470f/50f ) - 235f;
		z = 830;

		float [] buffFloat = { x,y,z,rotation  };
		byte[] buffer = ByteArrayConvert.floatArrayToByteArray( buffFloat, 0, null, 0, -1, true );

		try
		{
			InetAddress address;
			//System.out.println("Send to localhost:8550.");
			address = InetAddress.getByName("127.0.0.1"); //InetAddress.getByName("192.168.1.106");
			DatagramPacket packet = new DatagramPacket( buffer, buffer.length, address, 8550 );
			DatagramSocket datagramSocket = new DatagramSocket();
			datagramSocket.send(packet);
			datagramSocket.close();
			//System.out.println(InetAddress.getLocalHost().getHostAddress());

		} catch (IOException e) {
			e.printStackTrace();

		}
	}
}
