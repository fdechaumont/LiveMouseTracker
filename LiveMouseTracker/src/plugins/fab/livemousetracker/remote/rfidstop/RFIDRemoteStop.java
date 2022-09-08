/**
 	@author Fabrice de Chaumont @ Institut Pasteur

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

package plugins.fab.livemousetracker.remote.rfidstop;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import icy.file.FileUtil;
import plugins.fab.livemousetracker.LiveMouseTracker;
import plugins.fab.livemousetracker.experiment.EventLog;

/*

*/

public class RFIDRemoteStop extends Thread {

	public RFIDRemoteStop() {
		start();
	}

	@Override
	public void run() {

		System.out.println("Starting UDP RFID STOP Server on port 8552.");

		DatagramSocket serverSocket;
		try {
			serverSocket = new DatagramSocket(8552);			
			byte[] receiveData = new byte[1024];

			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);			

			int startFrame = 0;
			while( !isInterrupted() )
			{
				String sentence = "";
				serverSocket.receive(receivePacket);
				sentence = new String( receivePacket.getData(), receivePacket.getOffset(), receivePacket.getLength());
				
				if ( receivePacket.getAddress().toString().contains("127.0.0.1") )
				{
					System.out.println( "UDP RFID STOP server: string received: " + sentence );				
					{					
						if ( sentence.toLowerCase().contains("rfid stop") )
						{
							startFrame = LiveMouseTracker.getT();					
							LiveMouseTracker.addEventLogToDataBase( new EventLog( sentence, null, startFrame ));
							LiveMouseTracker.getTrackPoolOverlay().setRFIDStopEventInfoString( sentence );
						}
					}
				}else
				{
					System.out.println( "UDP RFID STOP server: string received (Ignored, not local): " + sentence );
				}

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
