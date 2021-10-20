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
import java.net.SocketException;

import icy.file.FileUtil;
import icy.plugin.abstract_.PluginActionable;
import icy.plugin.interface_.PluginThreaded;

public class LiveUDPReceiver extends PluginActionable implements PluginThreaded {

	@Override
	public void run() {

		System.out.println("UDP Receiver.");

		DatagramSocket serverSocket;
		try {
			serverSocket = new DatagramSocket(8550);
        byte[] receiveData = new byte[1024];
        byte[] sendData = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

        while(true)
           {
              serverSocket.receive(receivePacket);
              String sentence = new String( receivePacket.getData(), receivePacket.getOffset(), receivePacket.getLength());
              System.out.println("RECEIVED: " + sentence);
              Thread.yield();
           }

		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
