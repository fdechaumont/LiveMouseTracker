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
package plugins.fab.livemousetracker.rfid;

import icy.plugin.abstract_.PluginActionable;
import icy.plugin.interface_.PluginThreaded;

import java.util.Enumeration;

import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;
import plugins.fab.livemousetracker.serial.SerialDriverPlugin;

/**
 * 9600 BAUDS , 8 BITS , 1 STOPS , NO PARITY
 * 15 digits
 * @author Fab
 */
public class RFIDReaderTest extends PluginActionable implements PluginThreaded {

	@Override
	public void run() {

		new SerialDriverPlugin();

		try {

			SerialPortList list = new SerialPortList();
			System.out.println("RFID test running...");

			for ( String string : list.getPortNames() )
			{
				System.out.println("Port found: " + string );
			}

			System.out.println("Starting test on port COM52");
			SerialPort serial = new SerialPort( "COM52" );
			serial.openPort();
			serial.setParams( 9600, 8, 1, 0 );

			String id ="";

			while ( true )
			{
				byte[] bArray = serial.readBytes();
				if ( bArray != null )
				{
					for ( byte b : bArray )
					{
					//	System.out.println( b );
						if ( b >= '0' & b <='9' )
						{
							id+=(char)b;
						}
						if ( b == 13 || b == 10 )
						{
							id="";
						}

						if ( id.length() == 15 )
						{
							System.out.println("Read ready: " + id );
							id ="";
						}
					}
				}
			}

		} catch (SerialPortException e) {
			e.printStackTrace();
		}


	    System.out.println("RFID test ended.");

	}
}
