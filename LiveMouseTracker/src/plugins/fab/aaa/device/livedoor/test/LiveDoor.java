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

import icy.gui.frame.IcyFrame;
import icy.gui.util.GuiUtil;
import icy.plugin.abstract_.PluginActionable;
import icy.plugin.interface_.PluginThreaded;
import plugins.fab.aaa.device.livedoor.Door;
import plugins.fab.livemousetracker.device.control.ServoUtil;
import plugins.fab.livemousetracker.device.control.ServoUtil.SERVO_TYPE;

import javax.swing.BoxLayout;

import SimpleDynamixel.Servo;

public class LiveDoor extends PluginActionable implements PluginThreaded
{

	private Servo servo;
	private Door doorG1;
	private Door doorG2;
	private Door doorG3;
	private Door doorG4;

	@Override
	public void run() {


		IcyFrame mainFrame = new IcyFrame("Live Door", true, true , true ,true);

		servo = ServoUtil.initServo( SERVO_TYPE.PLASTIC, "COM3" );

		// Torque voulu = 200
		// Speed voulu = 200
		// speed, torque
		doorG1 = new Door( "Door G1" , 2, 952 , 714, 200, 200, servo );
		doorG2 = new Door( "Door G2" , 4, 359 , 110, 200, 200, servo );
		doorG3 = new Door( "Door G3" , 1, 654 , 420, 200, 200, servo );
		doorG4 = new Door( "Door G4" , 3, 962 , 700, 200, 200, servo );


		mainFrame.getContentPane().setLayout( new BoxLayout( mainFrame.getContentPane(), BoxLayout.PAGE_AXIS ) );
		mainFrame.getContentPane().add( GuiUtil.createLineBoxPanel(
				doorG1.getControlPanel() ,
				doorG2.getControlPanel() ,
				doorG3.getControlPanel() ,
				doorG4.getControlPanel()
				) );
		mainFrame.setFocusable( true );

		mainFrame.pack();
		mainFrame.setVisible( true );
		mainFrame.addToDesktopPane();
		mainFrame.setLocation( 0 , 100 );

//		System.out.println( doorG3.getServoPosition() );

		while( true )
		{

			doorG1.open();
			doorG2.open();
			doorG3.open();
			doorG4.open();

			try {
				Thread.sleep( 5000 );
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			doorG1.close();
			doorG2.close();
			doorG3.close();
			doorG4.close();

			try {
				Thread.sleep( 5000 );
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}


		/*
		for ( int i = 0 ; i < servoList.length ; i++ )
		{
			servo.setAlarmLed( DOOR_ID, 0 );
			servo.setTorqueEnable( servoList[i], false );
			servo.setDelayTime( servoList[i], 0 );
		}*/

	}

}
