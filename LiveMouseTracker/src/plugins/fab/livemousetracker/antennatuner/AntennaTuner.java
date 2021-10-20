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
import java.awt.geom.Point2D.Double;

import com.fazecast.jSerialComm.SerialPort;

import icy.plugin.abstract_.PluginActionable;
import icy.plugin.interface_.PluginBundled;
import icy.plugin.interface_.PluginThreaded;
//import jssc.SerialPortList;
import plugins.fab.livemousetracker.LiveMouseTracker;
import plugins.fab.livemousetracker.rfid.RFIDAntenna;
import plugins.fab.livemousetracker.serial.SerialDriverPlugin;

/* Warning: THIS PLUGIN USES com.fazecast.jSerialComm.SerialPort to discover ports */

public class AntennaTuner extends PluginActionable implements PluginThreaded  {

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

		System.out.println("RFID antenna tuner running...");

		while ( true )
		{
			SerialPort[] list = SerialPort.getCommPorts();

//			String[] portNameList= SerialPortList.getPortNames();
//
//			System.out.println( "Number of portname: " + portNameList.length );
//			for ( String portName : portNameList )
//			{
//				System.out.println( portName );
//			}

			//for ( String string : list.getPortNames() )

			for ( SerialPort port : list )
			{
				RFIDAntenna a = new RFIDAntenna( new Point2D.Double(0,0), 10, port.getSystemPortName() );
				try {
					Thread.sleep( 3000 );
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				a.shutdown();
			}

//			for ( String portName : portNameList )
//			{
//				RFIDAntenna a = new RFIDAntenna( new Point2D.Double(0,0), 10, portName );
//				try {
//					Thread.sleep( 3000 );
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//				a.shutdown();
//			}

			try {
				Thread.sleep( 1000 );
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}

	}



}
