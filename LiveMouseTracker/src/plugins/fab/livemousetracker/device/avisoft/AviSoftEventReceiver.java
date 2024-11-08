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

package plugins.fab.livemousetracker.device.avisoft;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import icy.file.FileUtil;
import plugins.fab.livemousetracker.LiveMouseTracker;
import plugins.fab.livemousetracker.experiment.EventLog;

/*

To make it work: add in avisoft ctrl out trigger:

 "run.exe"

C:\Program Files (x86)\PacketSender\PacketSender.exe -ua localhost 8550 "start_
C:\Program Files (x86)\PacketSender\PacketSender.exe -ua localhost 8550 "end_

and add "append filename" (if not working in avisoft do configuration>reset in avisoft)

note:

you can receive several times the same file. This is due to the hold time parameter in AviSoft.
hold time fuses wav files together but send separated event information trigger.
This means that for 1 file you can have multiple calls.


*/

public class AviSoftEventReceiver extends Thread {

	public AviSoftEventReceiver() {
		start();
	}

	@Override
	public void run() {

		System.out.println("AviSoft UDP Receiver on port 8550.");

		DatagramSocket serverSocket;
		try {
			serverSocket = new DatagramSocket(8550);
			byte[] receiveData = new byte[1024];

			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

			int startFrame = 0;
			while( !isInterrupted() )
			{
				String sentence = "";
				serverSocket.receive(receivePacket);
				sentence = new String( receivePacket.getData(), receivePacket.getOffset(), receivePacket.getLength());
				System.out.println( "AviSoft UDP Receiver: RECEIVE: " + sentence );
				if ( sentence.contains("start") )
				{
					startFrame = LiveMouseTracker.getT();
					try
					{
						LiveMouseTracker.getTrackPoolOverlay().setAvisoftInfoString( right( sentence ) );
					}
					catch( Exception e)
					{
						System.out.println("Avisoft receiver : track pool overlay not ready");
					}
				}

				if ( sentence.contains("end") )
				{
					String fileName = FileUtil.getFileName( sentence, false );
		            System.out.println("File: " + fileName );

					LiveMouseTracker.addEventLogToDataBase(
							new EventLog("USV seq", null, startFrame, LiveMouseTracker.getT() , fileName ));
					try
					{
						LiveMouseTracker.getTrackPoolOverlay().setAvisoftInfoString( sentence );
					}
					catch( Exception e)
					{
						System.out.println("Avisoft receiver : track pool overlay not ready");
					}
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

	private String right(String sentence) {
		if ( sentence == null ) return null;
		if ( sentence.length() > 20 )
		{
			return sentence.substring( sentence.length() - 20 );
		}
		return sentence;
	}

}
