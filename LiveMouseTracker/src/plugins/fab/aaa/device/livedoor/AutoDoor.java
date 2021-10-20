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

package plugins.fab.aaa.device.livedoor;


import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Timer;



import com.sun.org.apache.bcel.internal.generic.GOTO;

import SimpleDynamixel.Servo;
import SimpleDynamixel.ServoViz;
import SimpleDynamixel.SimpleDynamixelMain;
import icy.gui.dialog.MessageDialog;
import icy.gui.frame.IcyFrame;
import icy.gui.util.GuiUtil;
import icy.plugin.abstract_.PluginActionable;
import icy.plugin.interface_.PluginThreaded;
import plugins.fab.aaa.device.livedoor.Door.DoorStatus;

public class AutoDoor extends PluginActionable implements ActionListener {

	int DOOR_ID = 1;

	IcyFrame mainFrame = new IcyFrame("Door test", true, true , true ,true);
	JButton openButton = new JButton("open");
	JButton closeButton = new JButton("close");
	JButton playButton = new JButton("play");
	JTextArea textArea = new JTextArea(20 , 40 );
	Timer timer = new Timer( 100 , this );
	JTextField speedField = new JTextField("200");
	JTextField limitTorqueField = new JTextField("200");
	int OPEN_POSITION = 390;
	int CLOSE_POSITION = 203;

public AutoDoor() {
	// TODO Auto-generated constructor stub
}

@Override
public void run() {

	mainFrame.getContentPane().setLayout( new BoxLayout( mainFrame.getContentPane(), BoxLayout.PAGE_AXIS ) );
	mainFrame.getContentPane().add( GuiUtil.createLineBoxPanel( openButton ) );
	mainFrame.getContentPane().add( GuiUtil.createLineBoxPanel( closeButton ) );
	mainFrame.getContentPane().add( GuiUtil.createLineBoxPanel( playButton ) );
	mainFrame.getContentPane().add( GuiUtil.createLineBoxPanel( new JLabel("Speed: " ) , speedField ) );
	mainFrame.getContentPane().add( GuiUtil.createLineBoxPanel( new JLabel("MaxTorque: " ) , limitTorqueField ) );
	mainFrame.getContentPane().add( GuiUtil.createLineBoxPanel( textArea ) );

	openButton.addActionListener( this );
	closeButton.addActionListener( this );
	playButton.addActionListener( this );

	mainFrame.pack();
	mainFrame.setVisible( true );
	mainFrame.addToDesktopPane();
	mainFrame.setLocation( 0 , 100 );

	Toolkit.getDefaultToolkit().beep();

	init();
	timer.start();
}

int DOOR_POSITOIN_ACCURACY = 10;
DoorStatus status = DoorStatus.CLOSED;
Order order = Order.DONE;
ArrayList<Integer> positionWatchList = new ArrayList<Integer>();
boolean watchIsRunning = false;

@Override
public void actionPerformed(ActionEvent e) {

	if ( e.getSource() == timer )
	{
		int position = servo.presentPosition( DOOR_ID );
		boolean moving = servo.moving( DOOR_ID );
		int dGain = servo.dGain( DOOR_ID );
		int iGain = servo.iGain( DOOR_ID );
		int presentVolt = servo.presentVolt( DOOR_ID );
		int torqueLimit = servo.torqueLimit( DOOR_ID );
		boolean torqueEnabled = servo.torqueEnable( DOOR_ID );
		int alarmLed = servo.alarmLed( DOOR_ID );
		int load = servo.presentLoad( DOOR_ID ) ; //& 0x1FF;
		//int direction = ( servo.presentLoad( DOOR_ID ) & 0x200 ) >> 8;
		if ( load > 1024 ) load = -load+1024;

		int speed = Integer.parseInt( speedField.getText() );
		int limitTorque = Integer.parseInt( limitTorqueField.getText() );

		servo.setTorqueEnable( DOOR_ID, true );
		servo.setMovingSpeed( DOOR_ID, speed );
		servo.setMaxTorque( DOOR_ID, limitTorque );
		servo.setTorqueLimit( DOOR_ID , limitTorque );

		textArea.setText(
				"\n Position:" + position +
				"\n Moving: " + moving +
				"\n d Gain: " + dGain +
				"\n i Gain: " + iGain +
				"\n present volt: " + presentVolt +
				"\n torque enabled: " + torqueEnabled +
				"\n torque limit: " + torqueLimit +
				"\n alarm led: " + Integer.toBinaryString( alarmLed ) +
				"\n load: " + load +
				"\n status: " + status
				);

		if ( moving == false )
		{
			if ( isOpen() )
			{
				status = DoorStatus.OPENED;
				order = Order.DONE;
				if ( ! watchIsRunning )
				{
					initWatch();
				}
			}
			if ( isClosed() )
			{
				status = DoorStatus.CLOSED;
				order = Order.DONE;
				if ( ! watchIsRunning )
				{
					initWatch();
				}
			}
			if ( ! isOpen() && ! isClosed() )
			{
//				status = Status.ERROR;
				reverseOrder();
			}

			// could be on the done event
			if ( order == Order.DONE && watchIsRunning )
			{
				watch();
			}

		}

	}

	if ( e.getSource() == openButton )
	{
		open();
	}

	if ( e.getSource() == closeButton )
	{
		close();
	}

}

private void watch() {

	if ( watchIsRunning )
	{
		int position = servo.presentPosition( DOOR_ID );
		//positionWatchList.add( position );
		System.out.println("size: " + positionWatchList.size() );
		if ( positionWatchList.size() > 10 )
		{
			positionWatchList.remove( 0 );
		}
		// check if something is wrong.
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		System.out.println("-");
		for ( Integer i : positionWatchList )
		{
			//System.out.println(position);
			if ( i < min ) min = i;
			if ( i > max ) max = i;
		}

		if ( Math.abs( min-max ) > 1 )
		{
			open();
			watchIsRunning = false;
		}
	}

}

private void initWatch() {

	positionWatchList.clear();
	watchIsRunning = true;

}

private void reverseOrder() {
	if ( order == Order.CLOSE )
	{
		open();
		return;
	}
	if ( order == Order.OPEN )
	{
		close();
		return;
	}

}

private void open() {
	servo.setGoalPosition( DOOR_ID , OPEN_POSITION );
	status = DoorStatus.OPENING;
	order = Order.OPEN;
}

private boolean isOpen() {
	int position = servo.presentPosition( DOOR_ID );
	if ( Math.abs( position - OPEN_POSITION ) < DOOR_POSITOIN_ACCURACY )
	{
		return true;
	}
	return false;
}

private void close() {
	servo.setGoalPosition( DOOR_ID , CLOSE_POSITION );
	status = DoorStatus.CLOSING;
	order = Order.CLOSE;
}

private boolean isClosed() {
	int position = servo.presentPosition( DOOR_ID );
	if ( Math.abs( position - CLOSE_POSITION ) < DOOR_POSITOIN_ACCURACY )
	{
		return true;
	}
	return false;
}


Servo servo;

private void init() {

	servo = new Servo();
	servo.init( "COM3", 1000000 );
	//servo.init( "COM3", 57600 );

	System.out.println("Pinging servos...");
	//int [] servoList = servo.pingAll();
	int [] servoList = servo.pingRange( 0 , 50 );

	if ( servoList.length == 0 )
	{
		System.out.println("No servo found.");
		return;
	}

	System.out.println( "List of servo id: " +  Arrays.toString( servoList ) );

	System.out.println("ID: " + servoList[0] );

	for ( int i = 0 ; i < servoList.length ; i++ )
	{
		servo.setAlarmLed( DOOR_ID, 0 );
		servo.setTorqueEnable( servoList[i], false );
		servo.setDelayTime( servoList[i], 0 );
	}

}




//	ArrayList<ServoPosition> servoPositionList = new ArrayList<AutoDoor.ServoPosition>();
	/*
	class ServoPosition
	{
		int[] pos = new int[4];

		public void setPosition( int firstAngle , int secondAngle , int thirdAngle , int penAngle ) {

			pos[0] = firstAngle;
			pos[1] = secondAngle;
			pos[2] = thirdAngle;
			pos[3] = penAngle;

		}

		public void setServoPos( Servo servo )
		{
			servo.setGoalPosition( DOOR_ID , pos[0 ] );
			servo.setGoalPosition( SECOND , pos[1 ] );
			servo.setGoalPosition( THIRD , pos[2 ] );
			servo.setGoalPosition( PEN , pos[3 ] +40 );
		}

		public void readServoPos ( Servo servo )
		{
			pos[0] = servo.presentPosition( DOOR_ID );
			pos[1] = servo.presentPosition( SECOND );
			pos[2] = servo.presentPosition( THIRD );
			pos[3] = servo.presentPosition( PEN );

		}

		@Override
		public String toString() {
			return "a:"+ pos[0] + " b:" + pos[1] + " c:" + pos[2] + " d:" +pos[3];
		}

		public int difference(ServoPosition sp) {

			int max = 0;
			for ( int i = 0 ; i<4 ; i++ )
			{
				max = Math.max( pos[i], max );
			}

			return max;
		}


	}*/


	/*
	boolean recording = false;

	class Learn implements Runnable
	{
		boolean stop = false;

		@Override
		public void run() {

			while ( !stop )
			{

				ServoPosition sp = new ServoPosition();
				sp.readServoPos( servo );
				System.out.println( sp );

				ServoPosition last = null;
				if ( servoPositionList.size() > 0 ) last = servoPositionList.get( servoPositionList.size() - 1 );

				boolean rec = true;

//				if ( last != null )
//				{
//					int dif = sp.difference( last );
//					if ( dif < 40 ) rec = false;
//				}

				if ( rec )
				{
					servoPositionList.add( sp );
				}

				try {
					Thread.sleep( 50 );
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			System.out.println("Stop record");

		}

		public void stop() {
			stop = true;
		}
	}


	public void play() {


		for ( ServoPosition sp : servoPositionList )
		{

			System.out.println( sp );
			// use pen
			//if ( sp.pos[3] > 400 ) sp.pos[3] +=50;

			sp.setServoPos( servo );

			try {
				Thread.sleep( 10 );
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	@Override
	public void run() {

		mainFrame.getContentPane().setLayout( new BoxLayout( mainFrame.getContentPane(), BoxLayout.PAGE_AXIS ) );
		mainFrame.getContentPane().add( GuiUtil.createLineBoxPanel( learnButton ) );
		mainFrame.getContentPane().add( GuiUtil.createLineBoxPanel( stopButton ) );
		mainFrame.getContentPane().add( GuiUtil.createLineBoxPanel( playButton ) );

		learnButton.addActionListener( this );
		stopButton.addActionListener( this );
		playButton.addActionListener( this );

		mainFrame.pack();
		mainFrame.setVisible( true );
		mainFrame.addToDesktopPane();
		mainFrame.setLocation( 0 , 300 );

		Toolkit.getDefaultToolkit().beep();

		init();


	}

	Thread learnThread = null;
	Learn learn = null;

	@Override
	public void actionPerformed(ActionEvent e) {
		if ( e.getSource() == learnButton )
		{
			servoPositionList.clear();
//			recording = true;
			learn = new Learn();
			learnThread = new Thread( learn );
			learnThread.start();
		}

		if ( e.getSource() == stopButton )
		{
			learn.stop();
		}

		if ( e.getSource() == playButton )
		{
			servo.setTorqueEnable( DOOR_ID, true );
			servo.setTorqueEnable( SECOND, true );
			servo.setTorqueEnable( THIRD, true );
			servo.setTorqueEnable( PEN, true );

			play();

			servo.setTorqueEnable( DOOR_ID, false );
			servo.setTorqueEnable( SECOND, false );
			servo.setTorqueEnable( THIRD, false );
			servo.setTorqueEnable( PEN, false );


		}
	}

	private void init() {

		servo = new Servo();

		servo.init( "COM3", 1000000 );

		int [] servoList = servo.pingRange( 0 , 5 );

		System.out.println( "List of servo id: " +  Arrays.toString( servoList ) );

		System.out.println("ID: " + servoList[0] );

		for ( int i = 0 ; i < servoList.length ; i++ )
		{
			servo.setTorqueEnable( servoList[i], false );
			servo.setDelayTime( servoList[i], 0 );
		}


	}
*/
	/*
	Servo servo = null;

	public void test() {


		System.out.println("Starting auto door");

		servo = new Servo();

		servo.init( "COM3", 1000000 );

		int [] servoList = servo.pingRange( 0 , 5 );

		System.out.println( "List of servo id: " +  Arrays.toString( servoList ) );

		System.out.println("ID: " + servoList[0] );

		for ( int i = 0 ; i < servoList.length ; i++ )
		{
			servo.setTorqueEnable( servoList[i], false );
			servo.setDelayTime( servoList[i], 0 );
		}




		//servo.setId( servoList[0] , 5 );

/*
		servo.setMovingSpeed( FIRST , 100 );
		servo.setMovingSpeed( SECOND , 100 );
		servo.setMovingSpeed( THIRD , 100 );
		servo.setMovingSpeed( PEN , 100 );


		for ( int i = 0 ; i < 100 ; i++ )
		{
			servo.setGoalPosition( FIRST , 512 + i );
			servo.setGoalPosition( SECOND , 512 + i );
			servo.setGoalPosition( THIRD , 512 + i );

			try {
				Thread.sleep( 10 );
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}


		servo.setGoalPosition( FIRST , 512 );
		servo.setGoalPosition( SECOND , 512 );
		servo.setGoalPosition( THIRD , 512 );
		servo.setGoalPosition( PEN , 512 );
	*/

		/*
		for ( int i = 0 ; i< 1024 ; i++ )
		{
			servo.setGoalPosition( servoList[0], i );
			//servo.setTorqueEnable( servoList[0 ], false );
//			System.out.println( "load :"+servo.presentLoad( servoList[0] ));
//			System.out.println( "position :"+servo.presentPosition( servoList[0] ));
//			System.out.println( "speed :"+servo.presentSpeed( servoList[0] ));

			try {
				Thread.sleep( 10 );
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}/

		servo.setGoalPosition( servoList[0], 0 );

		/*
		for ( int i = 0 ; i< 40 ; i++ )
		{
			servo.setGoalPosition( servoList[1], i );

			try {
				Thread.sleep( 100 );
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		servo.setGoalPosition( servoList[1], 0 );
		*/

	/*
		int			servoRange = 0x3FF; // ax-12 10bit
		int			servoDeadAngle = 60;

		System.out.println("Waiting");
		try {
			Thread.sleep( 5000 );
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("Finished");

		*/
	//	ServoViz sv = new ServoViz( servo, servoList[0], servoRange, servoDeadAngle );

		//sv.

		//ServoViz sv = new ServoViz(arg0, arg1, arg2, arg3)

		/*
		ServoViz

		  servoVizList = new ServoViz[servoList.length];
		  for(int i=0;i < servoVizList.length;i++)
		  {
			// disable the torque
			servo.setTorqueEnable(servoList[i],false);

			// set the return delay time for the status packet
			servo.setDelayTime(servoList[i],0);	// 2us * x
			//servo.setDelayTime(servoList[i],0xFE);	// default 2us * 0xFE = 2 * 250 = 0.5ms


			// init the servo viz
			servoVizList[i] = new ServoViz(servo,servoList[i],servoRange,servoDeadAngle);
		  }

		  if(servoVizList.length == 1)
			vizRadius = (height - 2 * vizDist) * .2f;
		  else
			vizRadius = (width - 2 * vizDist - (servoVizList.length -1) * vizDist) / servoVizList.length * .5f;



	}

	*/

}
