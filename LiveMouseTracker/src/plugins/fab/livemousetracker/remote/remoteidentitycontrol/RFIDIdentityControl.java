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

package plugins.fab.livemousetracker.remote.remoteidentitycontrol;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import icy.file.FileUtil;
import plugins.fab.livemousetracker.LiveMouseTracker;
import plugins.fab.livemousetracker.experiment.EventLog;


public class RFIDIdentityControl extends Thread {

	/*
	This class manages the calls from script calling on UDP to set whether an animal should be searched or not.
	The client is expected to send the full RFID number.
	If the number does not exists yet in LMT, the number will be assigned to an anonymous animal.
	 */
	
	public RFIDIdentityControl() {
		start();
	}

	@Override
	public void run() {

		String serverName = "UDP RFID Identity Server";
		int port = 8553;
		System.out.println("Starting "+serverName+" on port " + Integer.toString( port ) );

		DatagramSocket serverSocket;
		try {
			serverSocket = new DatagramSocket(port);			
			byte[] receiveData = new byte[1024];

			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);			

			int startFrame = 0;
			while( !isInterrupted() )
			{
				String sentence = "";
				serverSocket.receive(receivePacket);
				sentence = new String( receivePacket.getData(), receivePacket.getOffset(), receivePacket.getLength());
				
				if ( receivePacket.getAddress().toString().contains("127.0.0.1") ) // localhost security
				{
					System.out.println( serverName+" : string received: " + sentence );				
					{					
						// message should contain 'rfid in' or 'rfid out' and the RFID number into star (*) symbols
						// example: rfid in *1234556778*
						//sentence = sentence.toLowerCase();
						LiveMouseTracker.addEventLogToDataBase( new EventLog( serverName + " " + sentence, null, LiveMouseTracker.getT() ));
						String rfid = "";
						//try {
							rfid = sentence.split("\\*")[1];							
							System.out.println ( serverName + " RFID decoded " + rfid );
//						}catch( Exception e ) 
//						{
//							System.out.println( serverName + ": Can't decode rfid in message");
//							continue;
//						}

						if ( sentence.contains("rfid in") )
						{							
							LiveMouseTracker.trackContainer.animalTrackSegmentPool.setRFIDAnimalEnabled( rfid , true );
						}
						if ( sentence.contains("rfid out") )
						{

							LiveMouseTracker.trackContainer.animalTrackSegmentPool.setRFIDAnimalEnabled( rfid , false );
						}
					}
				}else
				{
					System.out.println( serverName + ": string received (Ignored, not local): " + sentence );
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
