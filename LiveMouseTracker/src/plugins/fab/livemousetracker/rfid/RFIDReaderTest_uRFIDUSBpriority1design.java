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

/**
 * 9600 BAUDS , 8 BITS , 1 STOPS , NO PARITY
 * 15 digits
 * @author Fab
 */
public class RFIDReaderTest_uRFIDUSBpriority1design extends PluginActionable implements PluginThreaded {

	@Override
	public void run() {

		try {

//			SerialPortList list = new SerialPortList();
			System.out.println("RFIDReaderTest_uRFIDUSBpriority1design test running...");

			for ( String string : SerialPortList.getPortNames() )
			{
				System.out.println("Port found: " + string );
			}

			String com1 = "COM6";
			System.out.println("Starting test on port " + com1 );
			SerialPort serial1 = new SerialPort( com1 );
			serial1.openPort();
			serial1.setParams( 9600, 8, 1, 0 );

			String com2 = "COM7";
			System.out.println("Starting test on port " + com2 );
			SerialPort serial2 = new SerialPort( com2 );
			serial2.openPort();
			serial2.setParams( 9600, 8, 1, 0 );


//			serial.writeString("VER");
//			serial.writeByte( (byte) 13 );
//			Thread.sleep( 1000 );
//			System.out.println( serial.readString() );

			serial1.writeString("ST2");
			serial1.writeByte( (byte) 13 );
			serial1.writeString("SRA");
			serial1.writeByte( (byte) 13 );
			Thread.sleep( 1000 );
			System.out.println( serial1.readString() );

			serial2.writeString("ST2");
			serial2.writeByte( (byte) 13 );
			serial2.writeString("SRA");
			serial2.writeByte( (byte) 13 );
			Thread.sleep( 1000 );
			System.out.println( serial2.readString() );

			boolean swapantenna= true;
			int swaptime = 70;

			for ( int i = 0 ; i < 1000 ; i++ )
			{
				//serial.writeString("RAD");
				if ( swapantenna )
				{
					serial1.writeString("SRA");
					serial1.writeByte( (byte) 13 );
					Thread.sleep( swaptime );
				}
				serial1.writeString("RAT");
				serial1.writeByte( (byte) 13 );
				Thread.sleep( 15 );
				System.out.println( "1:" + serial1.readString() );
				byte[] data = serial1.readBytes();
				if (data != null )
				{
					System.out.println( "flush read: " + data.length );
				}
				if ( swapantenna )
				{
					serial1.writeString("SRD");
					serial1.writeByte( (byte) 13 );
				//	Thread.sleep( 15 );
				}
				Thread.sleep( 15 );

				if ( swapantenna )
				{
					serial2.writeString("SRA");
					serial2.writeByte( (byte) 13 );
					Thread.sleep( swaptime );
				}
				serial2.writeString("RAT");
				serial2.writeByte( (byte) 13 );
				Thread.sleep( 15 );
				System.out.println( "2:" + serial2.readString() );
				byte[] data2 = serial2.readBytes();
				if (data2 != null )
				{
					System.out.println( "flush read 2: " + data2.length );
				}
				if ( swapantenna )
				{
					serial2.writeString("SRD");
					serial2.writeByte( (byte) 13 );
				//	Thread.sleep( 15 );
				}
				Thread.sleep( 15 );

			}


//			serial.writeByte( (byte)'i' );

/*
			String id ="";

			serial.writeString("ST2");



			Thread.sleep( 1000 );

			System.out.println( serial.readString() );

			serial.writeString("RAD");

			Thread.sleep( 1000 );

			System.out.println( serial.readString() );
*/
			/*
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
			}*/

			serial1.closePort();
			serial2.closePort();

		} catch (SerialPortException e1) {
			e1.printStackTrace();
		} catch (InterruptedException e2) {
			e2.printStackTrace();
		}

	    System.out.println("RFID test ended.");

	}
}
