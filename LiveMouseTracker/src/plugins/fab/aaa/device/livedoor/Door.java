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

import icy.canvas.IcyCanvas;
import icy.painter.Overlay;
import icy.sequence.Sequence;
import icy.util.GraphicsUtil;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import SimpleDynamixel.Servo;

public class Door
{

	private static final boolean EMMERGENCY_ENABLED = false;
	int doorId;
	int OPEN_POSITION = 390;
	int CLOSE_POSITION = 203;
	private int speed;
	private int limitTorque;
	Servo servo;
	int DOOR_POSITION_ACCURACY = 5;
	JDoorControlPanel controlPanel;
	String name;
	DoorStatus status = DoorStatus.INIT;
	Order internalOrder = Order.DONE;
	Order mainOrder = Order.DONE;
	ErrorStatus errorStatus = ErrorStatus.NO_ERROR;
	DoorTimer doorTimer;
	/** this ROI is the area watched to open the door to a mouse willing to enter the gate */
	DoorOverlay overlay = null;

	public enum DoorStatus {
		INIT, INIT_DONE, OPENING, CLOSING, CLOSED, OPENED
	}

	public Door( String name, int doorId , int openPosition, int closePosition ,
			int speed, int limitTorque,
			Servo servo
			)
	{
		this.name = name;
		this.doorId = doorId;
		this.OPEN_POSITION = openPosition;
		this.CLOSE_POSITION = closePosition;
		this.speed = speed;
		this.limitTorque = limitTorque;
		this.servo = servo;
		init();
		controlPanel = new JDoorControlPanel( this );
		doorTimer = new DoorTimer();
		doorTimer.start();
	}

	public void createOverlay( Point2D location )
	{
		overlay = new DoorOverlay( location );
	}

	public DoorOverlay getOverlay() {
		return overlay;
	}

	class DoorTimer extends Thread
	{
		@Override
		public void run() {
			while ( !isInterrupted() )
			{
				timerProcess();
				try {
					Thread.sleep( 50 );
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	class DoorOverlay extends Overlay {

		Point2D location;

		public DoorOverlay( Point2D location ) {
			super( Door.this.getName() );
			this.location = location;
		}

		@Override
		public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas) {

			g.setColor( Color.orange );
			GraphicsUtil.drawCenteredString(g, Door.this.getName() ,
					(int)location.getX(), (int)location.getY()-120, false );

			GraphicsUtil.drawCenteredString(g, ""+Door.this.getStatus() ,
					(int)location.getX(), (int)location.getY()-100, false );

			GraphicsUtil.drawCenteredString(g, ""+Door.this.getInternalOrder() ,
					(int)location.getX(), (int)location.getY()-80, false );

			GraphicsUtil.drawCenteredString(g, ""+ (int) ( Door.this.getCachedDoorLockPourcentage() * 100 ) +"%" ,
					(int)location.getX(), (int)location.getY()-60, false );

			g.setColor( Color.red );
			g.fillRect( (int)location.getX(), (int)location.getY(), 5 , (int) (35*Door.this.getCachedDoorLockPourcentage() ) );

		}

	}

	private void init() {
		servo.setAlarmLed( doorId, 0 );
		servo.setTorqueEnable( doorId, false );
		servo.setDelayTime( doorId, 0 );

		servo.setTorqueEnable( doorId, true );
		servo.setMovingSpeed( doorId, speed );
		servo.setMaxTorque( doorId, limitTorque );
		servo.setTorqueLimit( doorId , limitTorque );
	}

	private void timerProcess() {

		//Chronometer doorProcessTimer = new Chronometer("Door " + getName() +" process " );
		getPresentPosition();
//		int position = servo.presentPosition( doorId );
//		boolean moving = servo.moving( doorId );
//		int dGain = servo.dGain( doorId );
//		int iGain = servo.iGain( doorId );
//		int presentVolt = servo.presentVolt( doorId );
//		int torqueLimit = servo.torqueLimit( doorId );
//		boolean torqueEnabled = servo.torqueEnable( doorId );
//		int alarmLed = servo.alarmLed( doorId );
		int load = servo.presentLoad( doorId ) ; //& 0x1FF;
		//int direction = ( servo.presentLoad( DOOR_ID ) & 0x200 ) >> 8;
		if ( load > 1024 ) load = -load+1024;

		checkStatus();

		if ( controlPanel != null )
		{
			controlPanel.repaint();

		}

	}

	ArrayList<Integer> positionHistory = new ArrayList<Integer>();
	/** cached position refreshed at each getPosition Call */
	private int cachedPosition;

	private void checkStatus() {

		if ( openInternal ) // One shot event, that may be asked by user and transfered to thread via boolean flag
		{
			servo.setGoalPosition( doorId , OPEN_POSITION );
			status = DoorStatus.OPENING;
			internalOrder = Order.OPEN;
			openInternal = false;
		}

		if ( closeInternal ) // One shot event, that may be asked by user and transfered to thread via boolean flag
		{
			servo.setGoalPosition( doorId , CLOSE_POSITION );
			status = DoorStatus.CLOSING;
			internalOrder = Order.CLOSE;
			closeInternal = false;
		}
		// check emmergency
		if ( EMMERGENCY_ENABLED )
		{
			if ( internalOrder == Order.DONE && mainOrder == Order.DONE )
			{
				int position = getPresentPosition();
				positionHistory.add( position );

				if ( positionHistory.size() > 10 ) positionHistory.remove( 0 );
				if ( positionHistory.size() == 10 )
				{
					int pos = positionHistory.get( 0 );
					for ( int i = 0 ; i< positionHistory.size() ;i++ )
					{
						if ( Math.abs( pos - positionHistory.get( i ) ) > 1 )
						{
							openInternal();
							// TODO: LAUNCH EMMERGENCY ERROR
						}
					}
				}
				//System.out.println( positionHistory.size() );
			}else
			{
				positionHistory.clear();
			}
		}

		// check door logic
		if ( internalOrder != Order.DONE )
		{
			if ( isMoving() == false )
			{
				// A goal is reached
				if ( isOpen() )
				{
					status = DoorStatus.OPENED;
					internalOrder = Order.DONE;
				}

				if ( isClosed() )
				{
					status = DoorStatus.CLOSED;
					internalOrder = Order.DONE;
				}

				// Check if the door is either open or closed.
				if ( status != DoorStatus.OPENED && status != DoorStatus.CLOSED )
				{
					//System.out.println("t");
					if ( mainOrder == Order.OPEN )
					{
						errorStatus = ErrorStatus.CANT_OPEN;
						error();
					}
					if ( mainOrder == Order.CLOSE )
					{
						errorStatus = ErrorStatus.CANT_CLOSE;
						error();
					}
					reverseInternalOrder();
				}

			}
		}

		if ( internalOrder == Order.DONE )
		{
			if ( mainOrder != Order.DONE )
			{
				// check if the mainOrder is reached
				if ( status == DoorStatus.CLOSED )
				{
					if ( mainOrder == Order.OPEN )
					{
						openInternal();
					}else
					{
						errorStatus = ErrorStatus.NO_ERROR;
						mainOrder = Order.DONE;
					}
				}
				if ( status == DoorStatus.OPENED )
				{
					if ( mainOrder == Order.CLOSE )
					{
						closeInternal();
					}else
					{
						errorStatus = ErrorStatus.NO_ERROR;
						mainOrder = Order.DONE;
					}
				}
			}
		}



	}

	public int getLoad()
	{
		int load = servo.presentLoad( doorId ) ; //& 0x1FF;
		//	int direction = ( servo.presentLoad( DOOR_ID ) & 0x200 ) >> 8;
		if ( load > 1024 ) load = -load+1024;
		return load;
	}


	private void error() {

		System.out.println( getName() + " Error: " + getErrorStatus() + " load: " + getLoad() );

	}

	private void reverseInternalOrder() {

		if ( internalOrder == Order.CLOSE )
		{
			openInternal();
			return;
		}
		if ( internalOrder == Order.OPEN )
		{
			closeInternal();
			return;
		}

	}


	public JDoorControlPanel getControlPanel() {
		return controlPanel;
	}

	public void open() {
		if ( status == DoorStatus.OPENED ) return;

		mainOrder = Order.OPEN;
		openInternal();
	}

	public void close() {
		if ( status == DoorStatus.CLOSED ) return;

		mainOrder = Order.CLOSE;
		closeInternal();
	}

	boolean openInternal = false;

	private void openInternal() {
		openInternal = true;
	}

	boolean isMoving()
	{
		return servo.moving( doorId );
	}

	public boolean isOpen() {
		int position = getPresentPosition();
		if ( Math.abs( position - OPEN_POSITION ) < DOOR_POSITION_ACCURACY )
		{
			return true;
		}
		return false;
	}

	boolean closeInternal = false;
	private void closeInternal() {
		closeInternal = true;
	}

	public boolean isClosed() {
		int position = getPresentPosition();
		if ( Math.abs( position - CLOSE_POSITION ) < DOOR_POSITION_ACCURACY )
		{
			return true;
		}
		return false;
	}

	public String getName() {
		return name;
	}

	public int getId() {
		return doorId;
	}

	public Order getOrder() {
		return mainOrder;
	}

	public Order getInternalOrder() {
		return internalOrder;
	}

	/**
	 * returns how much the door is closed.
	 * 1 is locked, 0 is open. */
	public double getDoorLockPercentage() {
		int position = getPresentPosition(); //servo.presentPosition( doorId );
		return ( ( 1d / ( OPEN_POSITION - CLOSE_POSITION ) ) * ( OPEN_POSITION - position ) );
	}

	/** get current position and cache it. */
	public int getPresentPosition()
	{
		int position = servo.presentPosition( doorId );
		//System.out.println("Door: " + getName() + " pos: " + position );
		cachedPosition = position;
		return position;
	}

	public double getCachedDoorLockPourcentage()
	{
		//return getDoorLockPercentage();
		return ( ( 1d / ( OPEN_POSITION - CLOSE_POSITION ) ) * ( OPEN_POSITION - cachedPosition ) );
	}

	ArrayList<DoorListener> doorListenerList = new ArrayList<DoorListener>();

	private void fireDoorChanged()
	{
		for ( DoorListener doorListener : doorListenerList )
		{
			doorListener.doorChanged();
		}
	}

	public void addDoorListener( DoorListener doorListener )
	{
		doorListenerList.add( doorListener );
	}

	public void removeDoorListener( DoorListener doorListener )
	{
		doorListenerList.remove( doorListener );
	}

	public DoorStatus getStatus() {
		return status;
	}

	public ErrorStatus getErrorStatus() {
		return errorStatus;
	}

	public void setSpeed(int speed) {
		this.speed = speed;

	}

	public void setLimitTorque(int limitTorque) {
		this.limitTorque = limitTorque;

	}




}
