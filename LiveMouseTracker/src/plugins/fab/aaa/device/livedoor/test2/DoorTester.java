package plugins.fab.aaa.device.livedoor.test2;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import SimpleDynamixel.Servo;
import icy.gui.frame.IcyFrame;
import icy.main.Icy;
import icy.plugin.abstract_.PluginActionable;
import plugins.fab.livemousetracker.device.control.ServoUtil;
import plugins.fab.livemousetracker.device.control.ServoUtil.SERVO_TYPE;

public class DoorTester extends PluginActionable {

	Servo servo;

	@Override
	public void run() {

		servo = new Servo();
		servo.init( "COM3", 1000000 );

		System.out.println("Pinging servos...");
		//int [] servoList = servo.pingAll();
		int [] servoList = servo.pingRange( 0 , 10 );


		if ( servoList.length == 0 )
		{
			System.out.println("No servo found.");
			return;
		}

		System.out.println( "List of servo id: " +  Arrays.toString( servoList ) );

		System.out.println("ID: " + servoList[0] );

		for ( int i = 0 ; i < servoList.length ; i++ )
		{
			new DoorTest( servoList[i] , servo );
		}

	}




}
