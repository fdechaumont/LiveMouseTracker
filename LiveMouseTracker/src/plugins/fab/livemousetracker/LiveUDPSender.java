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

import icy.plugin.abstract_.PluginActionable;
import icy.plugin.interface_.PluginThreaded;
import icy.type.collection.array.ArrayUtil;
import icy.type.collection.array.ByteArrayConvert;

public class LiveUDPSender extends PluginActionable implements PluginThreaded {

	@Override
	public void run() {

		System.out.println("UDP Test.");

		while( true )
		{

			try {

				//byte[] buffer = {'c','o','c','o','i','o','p',0};

				float f = 30f;

				float [] buffFloat = { f , f*2 , f*3, f*4  };


				byte[] buffer = ByteArrayConvert.floatArrayToByteArray( buffFloat, 0, null, 0, -1, true );
				//			byte[] buffer = new byte[4000];
				//			for ( int i = 0 ; i < buffer.length ; i++ )
				//			{
				//				buffer[i] = 105;
				//			}

				InetAddress address;
				System.out.println("Send to localhost:8550.");
				address = InetAddress.getByName("127.0.0.1"); //InetAddress.getByName("192.168.1.106");
				DatagramPacket packet = new DatagramPacket( buffer, buffer.length, address, 8550 );

				DatagramSocket datagramSocket = new DatagramSocket();
				datagramSocket.send(packet);
				datagramSocket.close();
				System.out.println(InetAddress.getLocalHost().getHostAddress());

			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("Data sent.");

			try {
				Thread.sleep( 1000 );
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}


}
