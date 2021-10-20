package plugins.fab.aaa.device.livedoor.test2;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import SimpleDynamixel.Servo;
import icy.gui.frame.IcyFrame;
import icy.main.Icy;
import icy.plugin.abstract_.PluginActionable;
import plugins.fab.livemousetracker.device.control.ServoUtil;
import plugins.fab.livemousetracker.device.control.ServoUtil.SERVO_TYPE;

public class DoorTest implements ActionListener, ChangeListener {



	public DoorTest( int doorId , Servo servo ) {
		this.doorId = doorId;
		this.servo = servo;
		run();
	}

	Thread timer = new Thread( new Runnable() {

		@Override
		public void run() {
			while ( true )
			{
				timerProcess();
				try {
					Thread.sleep( 50 );
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	});

	Thread openCloseTimer = new Thread( new Runnable() {

		@Override
		public void run() {
			while ( true )
			{
				if ( doorPanel.repeatChckbx().isSelected() )
				{
					servo.setGoalPosition( doorId , start );
				}
				try {
					Thread.sleep( 3000 );
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				if ( doorPanel.repeatChckbx().isSelected() )
				{
					servo.setGoalPosition( doorId , end );
				}
				try {
					Thread.sleep( 3000 );
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	});


	DoorPanel doorPanel = new DoorPanel();
	Servo servo;
	int doorId = 3;
	int start;
	int end;
	int speed;
	int maxTorque;

	void timerProcess()
	{
		int position = servo.presentPosition( doorId );
		doorPanel.positionLabel().setText(""+position);

		doorPanel.courseProgressBar().setMinimum( start );
		doorPanel.courseProgressBar().setMaximum( end );
		doorPanel.courseProgressBar().setValue( position );

		int load = servo.presentLoad( doorId );
		doorPanel.forceLabel().setText( "" + load );
	}


	public void run() {

		//servo = ServoUtil.initServo( SERVO_TYPE.PLASTIC, "COM3" );

		IcyFrame mainFrame = new IcyFrame("Door test");
		doorPanel.getTitle().setText("Door test ID:" + doorId );

		//doorG1 = new Door( "Door G1" , 2, 952 , 714, 200, 200, servo );

		mainFrame.getContentPane().add( doorPanel );
		mainFrame.pack();
		mainFrame.addToDesktopPane();
		mainFrame.setVisible( true );

		doorPanel.startSlider().setValue( 79 );
		doorPanel.endSlider().setValue( 317 );

		doorPanel.maxTorqueSlider().setValue( 300 );
		doorPanel.speedSlider().setValue( 300 );

		doorPanel.startSlider().addChangeListener( this );
		doorPanel.endSlider().addChangeListener( this );
		doorPanel.maxTorqueSlider().addChangeListener( this );
		doorPanel.speedSlider().addChangeListener( this );

		doorPanel.upButton().addActionListener( this );
		doorPanel.downButton().addActionListener( this );

		doorPanel.repeatChckbx().addActionListener( this );

		timer.start();
		readAllParameters();
		openCloseTimer.start();
	}

	void init()
	{
		servo.setAlarmLed( doorId, 0 );
		servo.setTorqueEnable( doorId, false );
		servo.setDelayTime( doorId, 0 );

		servo.setTorqueEnable( doorId, true );
		servo.setMovingSpeed( doorId, speed );
		servo.setMaxTorque( doorId, maxTorque );
		servo.setTorqueLimit( doorId , maxTorque );
	}



	@Override
	public void actionPerformed(ActionEvent e) {

		if ( e.getSource() == doorPanel.upButton() )
		{
			servo.setGoalPosition( doorId , start );
		}

		if ( e.getSource() == doorPanel.downButton() )
		{
			servo.setGoalPosition( doorId , end );
		}


	}

	@Override
	public void stateChanged(ChangeEvent e ) {

		readAllParameters();

	}

	private void readAllParameters() {

		start = doorPanel.startSlider().getValue();

		end = doorPanel.endSlider().getValue();

		speed = doorPanel.speedSlider().getValue();

		maxTorque = doorPanel.maxTorqueSlider().getValue();

		init();

	}




}
