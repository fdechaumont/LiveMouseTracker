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
package plugins.fab.aaa.device.turretopto;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Timer;

import SimpleDynamixel.Servo;
import icy.gui.frame.IcyFrame;
import icy.gui.util.GuiUtil;
import icy.plugin.abstract_.PluginActionable;

public class TestTurret2 extends PluginActionable {

	Servo servo;

	public TestTurret2() {

	}

	@Override
	public void run() {

		init();


		servo.setMovingSpeed( 10, 2000 );
		servo.setMovingSpeed( 11, 80000 );

		// full vertical
		servo.setGoalPosition( 10 , 2048 );
		servo.setGoalPosition( 11 , 2048 );

		try {
			Thread.sleep( 1000 );
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		ArrayList<Point2D> pointList = new ArrayList<Point2D>();
		for ( int i = 0 ; i<5 ; i++ )
		{
			int top = (int) ( 2200 + Math.random()*600d );
			int second = (int) ( 2048 + Math.random()*500d );
			pointList.add( new Point2D.Double( top , second ) );
		}


		// 10 is upper turret
		for ( int i = 0 ; i < 100 ; i++ )
		{
			System.out.println("Cycle start");
			for ( Point2D p : pointList )
			{
				int top = (int)p.getX();
				int second = (int)p.getY();
				System.out.println("top:"+top+ " second:" + second );
				servo.setGoalPosition( 10 , top ); // 2200 > to the top 2800 > to the bottom
				servo.setGoalPosition( 11 , second ); //

				try {
					Thread.sleep( 1000 );
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		}

	}


	private void init() {

		servo = new Servo();
		servo.init( "COM16", 57600 );

		int [] servoList = servo.pingRange( 0 , 11 );
		System.out.println( "List of servo id: " +  Arrays.toString( servoList ) );

		if ( servoList.length == 0 )
		{
			System.out.println("no Servo found.");
			return;
		}


		for ( int i = 0 ; i < servoList.length ; i++ )
		{
//			servo.setGoalPosition( servoList[i] , 128 );
			System.out.println("ID: " + servoList[i] );
			//servo.setLed( servoList[i] , true );
			//servo.setAlarmLed( SERVO_ID, 0 );
			//servo.setTorqueEnable( servoList[i], false );
			//servo.setDelayTime( servoList[i], 0 );
			System.out.println( servo.presentPosition( servoList[i] ) );
		}

	}


}
