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

import java.awt.geom.Point2D;
import java.util.ArrayList;

import icy.plugin.abstract_.PluginActionable;
import icy.plugin.interface_.PluginThreaded;
import jssc.SerialPortList;

/**
 * 9600 BAUDS , 8 BITS , 1 STOPS , NO PARITY
 * 15 digits
 *
 * Version 2.
 *
 * @author Fab
 */
public class RFIDReaderTest_uRFIDUSBpriority1design_v2 extends PluginActionable implements PluginThreaded {

	ArrayList<RFIDAntenna> antennaList = new ArrayList<RFIDAntenna>();

	@Override
	public void run() {

		try {

			System.out.println("RFIDReaderTest_uRFIDUSBpriority1design version 2 test running...");

			System.out.println("Check available ports.");
			for ( String string : SerialPortList.getPortNames() )
			{
				System.out.println("Port found: " + string );
			}

//			serial.writeString("VER");
//			serial.writeByte( (byte) 13 );
//			Thread.sleep( 1000 );
//			System.out.println( serial.readString() );

			antennaList.add( new RFIDAntenna( new Point2D.Double( 0, 0) , 0 , "COM6", null ) );
			antennaList.add( new RFIDAntenna( new Point2D.Double( 0, 0) , 0 , "COM7", null ) );
			antennaList.add( new RFIDAntenna( new Point2D.Double( 0, 0) , 0 , "COM8", null ) );
			antennaList.add( new RFIDAntenna( new Point2D.Double( 0, 0) , 0 , "COM9", null ) );

//			antennaList.get( 0 ).switchOn();

			for ( int i = 0 ; i < 1000 ; i++ )
			{
				for ( RFIDAntenna antenna : antennaList )
				{
					//if ( antenna != antennaList.get( 0 ) ) continue;
//					Thread.sleep( 100 ); // 15
					antenna.switchOn();
					Thread.sleep( 100 ); // 70
					antenna.sendReadOrder();
//					Thread.sleep( 80 );
					antenna.readData();
//					Thread.sleep( 100 );
					antenna.switchOff();
//					Thread.sleep( 50 );
				}
			}

			System.out.println("Releasing comp port.");

			for ( RFIDAntenna antenna : antennaList )
			{
				antenna.shutdown();
			}

			System.out.println("End");

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

//			serial1.closePort();
//			serial2.closePort();

			for ( RFIDAntenna antenna : antennaList )
			{
				antenna.shutdown();
			}

		} catch ( InterruptedException e) {
			e.printStackTrace();
		}


	    System.out.println("RFID test ended.");

	}
}
