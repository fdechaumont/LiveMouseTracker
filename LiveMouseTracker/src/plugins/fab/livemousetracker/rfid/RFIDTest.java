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

import icy.plugin.abstract_.PluginActionable;
import icy.plugin.interface_.PluginStartAsThread;
import jssc.SerialPortList;

public class RFIDTest extends PluginActionable implements AntennaReadListener , PluginStartAsThread {

//	public static RFIDManager2 rfidManager;

	public RFIDTest() {

	}

	@Override
	public void run() {

//		rfidManager = new RFIDManager2();
		//rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 214,   81 ) , 20 , "COM3" ) );

		SerialPortList list = new SerialPortList();
		System.out.println("RFID test running...");
		for ( String string : list.getPortNames() )
		{
			System.out.println("Port found: " + string );
		}

		String port = list.getPortNames()[0];
		System.out.println("Start on port " + port );

		RFIDAntenna antenna = new RFIDAntenna( new Point2D.Double( 214,   81 ) , 20 , port );
		antenna.addRFIDAntennaListener( this );
		antenna.setEnabled( true );

//		try {
//			Thread.sleep( 3000 );
//		} catch (InterruptedException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}



		System.out.println("go");

		while ( true )
		{
			try {
				Thread.sleep( 100 );
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//			antenna.switchOn();

		}

	}

	@Override
	public void antennaEvent(AntennaReadEvent rfidEvent) {

		System.out.println( rfidEvent );

	}

}
