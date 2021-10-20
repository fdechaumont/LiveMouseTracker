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
package plugins.fab.aaa.device.livedoor.test;

import javax.swing.BoxLayout;
import javax.swing.JLabel;

import SimpleDynamixel.Servo;
import icy.gui.frame.IcyFrame;
import icy.gui.util.GuiUtil;
import icy.plugin.abstract_.PluginActionable;
import plugins.fab.aaa.device.livedoor.Door;

public class DoorTest extends PluginActionable{

	Servo servo;
	Door door = null;

	IcyFrame mainFrame = new IcyFrame("Door Test", true, true , true ,true);

	@Override
	public void run() {


		servo = new Servo();
		servo.init( "COM3", 1000000 );

//		door = new Door( "Door A" , 3, 390, 203, 200, 200, servo );
		door = new Door( "Door A" , 3, 390, 203, 1000, 1000, servo);


		mainFrame.getContentPane().setLayout( new BoxLayout( mainFrame.getContentPane(), BoxLayout.PAGE_AXIS ) );
		mainFrame.getContentPane().add( GuiUtil.createLineBoxPanel( door.getControlPanel() ) );
		mainFrame.setFocusable( true );

		mainFrame.pack();
		mainFrame.setVisible( true );
		mainFrame.addToDesktopPane();
		mainFrame.setLocation( 0 , 100 );

	}

}
