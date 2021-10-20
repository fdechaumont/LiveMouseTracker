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
package plugins.fab.livemousetracker.antennatuner;

import java.awt.geom.Point2D;

import com.fazecast.jSerialComm.SerialPort;

import icy.plugin.abstract_.PluginActionable;
import icy.plugin.interface_.PluginThreaded;
import plugins.fab.livemousetracker.rfid.Antenna;
import plugins.fab.livemousetracker.rfid.AntennaReadEvent;
import plugins.fab.livemousetracker.rfid.AntennaReadListener;
import plugins.fab.livemousetracker.rfid.RFIDAntenna;
import plugins.fab.livemousetracker.rfid.RFIDManager2;
import plugins.fab.livemousetracker.serial.SerialDriverPlugin;

/* Warning: THIS PLUGIN USES com.fazecast.jSerialComm.SerialPort to discover ports */


public class AntennaReadingTester extends PluginActionable implements PluginThreaded, AntennaReadListener  {

	@Override
	public void run() {

		new SerialDriverPlugin();

//		RFIDAntenna a = new RFIDAntenna( new Point2D.Double(0,0), 10, "COM4" );
//		try {
//			Thread.sleep( 1000 );
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//		System.out.println( a.getFrequency() );

		System.out.println("RFID antenna reading tester running...");

		//SerialPortList list = new SerialPortList();
		RFIDManager2 rfidManager = new RFIDManager2();

		//for ( String string : list.getPortNames() )
		SerialPort[] list = SerialPort.getCommPorts();
//		String[] list = SerialPortList.getPortNames();


		for ( SerialPort port : list )
		{
			RFIDAntenna rfidAntenna = new RFIDAntenna( new Point2D.Double(0,0), 10, port.getSystemPortName() );
			rfidAntenna.addRFIDAntennaListener( this );
			rfidManager.addAntenna( rfidAntenna );
		}

//		for ( String portName : list )
//		{
//			RFIDAntenna rfidAntenna = new RFIDAntenna( new Point2D.Double(0,0), 10,
//					portName );
//			rfidAntenna.addRFIDAntennaListener( this );
//			rfidManager.addAntenna( rfidAntenna );
//		}

		while ( true )
		{

			for ( Antenna antenna : rfidManager.getAntennaList() )
			{
				if ( antenna instanceof RFIDAntenna )
				{

					RFIDAntenna rfidAntenna = (RFIDAntenna) antenna;
					System.out.println("Activating antenna on port " + rfidAntenna.getComPort() );
					rfidManager.activateOnlyAntenna( antenna );

//					RFIDAntenna rfidAntenna = (RFIDAntenna) antenna;
//					rfidAntenna.setEnabled( false );
					try {
						Thread.sleep( 5000 );
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
/*
			for ( String string : list.getPortNames() )
			{
				System.out.println("Working with antenna "+string );

				RFIDAntenna a = new RFIDAntenna( new Point2D.Double(0,0), 10, string );
				try {
					Thread.sleep( 3000 );
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				a.shutdown();
			}
*/

		}

	}


	@Override
	public void antennaEvent(AntennaReadEvent rfidEvent) {
		System.out.println("RFID evt: " + rfidEvent );
	}



}
