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
public class RFIDReaderTest_uRFIDUSBpriority1design_v3 extends PluginActionable
implements PluginThreaded , AntennaReadListener {

	ArrayList<RFIDAntenna> antennaList = new ArrayList<RFIDAntenna>();

	@Override
	public void run() {


		System.out.println("RFIDReaderTest_uRFIDUSBpriority1design version 3 test running...");

		System.out.println("Check available ports.");
		for ( String string : SerialPortList.getPortNames() )
		{
			System.out.println("Port found: " + string );
		}



//		antennaList.add( new RFIDAntenna2( new Point2D.Double( 100,  50 ) , 0 , "COM5" ) );
//		antennaList.add( new RFIDAntenna2( new Point2D.Double(  50,   0 ) , 0 , "COM7" ) );
//		antennaList.add( new RFIDAntenna2( new Point2D.Double(  50, 100 ) , 0 , "COM8" ) );
		antennaList.add( new RFIDAntenna( new Point2D.Double(   0,  50 ) , 0 , "COM8", null ) );

//		antennaList.add( new RFIDAntenna( new Point2D.Double( 133,   81 ) , 20 , "COM16" ) );
//		antennaList.add( new RFIDAntenna2( new Point2D.Double( 214,   81 ) , 20 , "COM25" ) );
//		antennaList.add( new RFIDAntenna2( new Point2D.Double( 296,   81 ) , 20 , "COM13" ) );
//		antennaList.add( new RFIDAntenna2( new Point2D.Double( 378,   81 ) , 20 , "COM12" ) );
//
//		rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 133,  165 ) , 20 , "COM24" ) );
//		rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 214,  165 ) , 20 , "COM16" ) );
//		rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 296,  165 ) , 20 , "COM17" ) );
//		rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 378,  165 ) , 20 , "COM11" ) );
//
//		rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 133,  246 ) , 20 , "COM20" ) );
//		rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 214,  246 ) , 20 , "COM19" ) );
//		rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 296,  246 ) , 20 , "COM16" ) );
//		rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 378,  246 ) , 20 , "COM14" ) );
//
//		rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 133,  324 ) , 20 , "COM21" ) );
//		rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 214,  324 ) , 20 , "COM22" ) );
//		rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 296,  324 ) , 20 , "COM15" ) );
//		rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 378,  324 ) , 20 , "COM18" ) );
		for ( RFIDAntenna antenna : antennaList )
		{
			antenna.addRFIDAntennaListener( this );
		}

		antennaList.get( 0 ).setEnabled( true );
		//antennaList.get( 0 ).readFrequency();

//		System.out.println("Releasing comp port.");
//
//		for ( RFIDAntenna2 antenna : antennaList )
//		{
//			antenna.shutdown();
//		}
//
//		System.out.println("End");

	}

	@Override
	public void antennaEvent(AntennaReadEvent rfidEvent) {
		System.out.println("RFID event received: " + rfidEvent );
	}
}
