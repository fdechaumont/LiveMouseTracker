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

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import icy.util.GraphicsUtil;
import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;

import plugins.fab.livemousetracker.DrawUtil;
import plugins.fab.livemousetracker.LiveMouseTracker;
import plugins.fab.livemousetracker.Util;
import plugins.fab.livemousetracker.network.NetworkUtil;
import plugins.fab.livemousetracker.overlay.Event;

/**
 * Designed for uRFID_USB_PriorityDesign
 *
 * */
public class RFIDAntenna extends Thread implements Antenna {

	SerialPort serial = null;
	/** The location of the center of the RFID antenna in the image */
	Point2D location;
	/** The area covered by the RFID expressed as a ray */
	private float ray;
	String comPort;
	ArrayList<AntennaReadListener> rfidAntennaListenerList = new ArrayList<AntennaReadListener>();
	boolean faulty = false;
	int nbEvent = 0;
	int nbTryRead = 0;
	String innerSerialNumber =""; 			// serial number of the connected reader
	String expectedInnerSerialNumber =""; 	// serial number that should match this reader

	String lastReadOrder = "";
	
//	String state="start";

	enum AntennaState
	{
		ON, OFF, UNKNOWN
	}

	AntennaState onOffstate = AntennaState.UNKNOWN;

	public Point2D getLocation() {
		return location;
	}

	public float getRay()
	{
		return ray;
	}

	public String getComPort() {
		return comPort;
	}

	public RFIDAntenna( Point2D location , float ray , String comPort, String expectedInnerSerialNumber ) {
		super("Thread " + comPort);

		this.ray = ray;
		this.location = location;
		this.comPort = comPort;
		this.expectedInnerSerialNumber = expectedInnerSerialNumber;

		System.out.println("Starting RFID reader on port " + comPort );
		serial = new SerialPort( comPort );
		try {
			System.out.println("Starting RFID open port " + comPort );
			serial.openPort();
			serial.setParams( 9600, 8, 1, 0 );
			System.out.println("Starting RFID params ok " + comPort );
			write( "ST2" ); // Set read animal tags.
			write( "SB1" ); // Disable read buzzer
			write( "SL4" ); // Leds off						
			switchOff();

		} catch (SerialPortException e) {
			//e.printStackTrace();
			System.err.println("COM PORT ERROR (disabling.): " + comPort );
			faulty = true;
		}

		
		start();
	}

	boolean stopReadingThread = false;
	boolean enabled = false;
	private double frequency = -1;

	public double getFrequency() {
		return frequency;
	}

	public void setEnabled(boolean enabled) {
		if ( faulty ) return;

		this.enabled = enabled;
//		if( ! enabled )
//		{
//			flushData();
//		}
	}

	public boolean isFaulty() {
		return faulty;
	}

	private void flushData() {
		if ( faulty ) return;

		try {
			serial.readBytes( serial.getInputBufferBytesCount() , 10 );
		} catch (SerialPortException e) {
			//e.printStackTrace();
			System.out.println("[Serial t:"+LiveMouseTracker.getT()+"] Read error on port " + comPort );
			faulty = true;
		} catch (SerialPortTimeoutException e) {
			// TimeOut sur lecture du port com.
			System.out.println("[Serial t:"+LiveMouseTracker.getT()+"] Time out on port " + comPort );
		} catch ( NegativeArraySizeException e2 )
		{
			System.out.println("[Serial t:"+LiveMouseTracker.getT()+"] Negative Arrray size exception. " + comPort );
			LiveMouseTracker.addEvent( new Event( "Faulty antenna step 1", Color.red, this.location ) );
			
			for ( int i = 0 ; i< 10 ; i++ )
			{
				switchOff();
				try {
					Thread.sleep( 10 ); // 70 // 10
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			LiveMouseTracker.addEvent( new Event( "Faulty antenna step 2", Color.red, this.location ) );
			faulty = true;
			
		}
		readData ="";
	}

	
	
	public String getInnerSerialNumber()
	{
		return this.innerSerialNumber;
	}
	
	public void setInnerSerialNumber( String innerSerialNumber )
	{
		this.innerSerialNumber = innerSerialNumber ;
	}
	
	
	
	/** **/
	
	public void programInnerSerial()
	{
		
		System.out.println("Programming RFID reader inner serial: " + expectedInnerSerialNumber + " --> "+ comPort +" ..." );

		if ( expectedInnerSerialNumber.length() != 4 )
		{
			System.out.println("Inner serial must be 4 hex long 0-F ");
			return;
		}
		
		for ( int i = 0; i<10; i++ )
		{
			write( "SSN"+expectedInnerSerialNumber );
		}
		
		System.out.println("Programming " + comPort +" done" );
	}
	
	/** This should not be called if the antenna is enabled. */	
	private String readInnerSerialNumber()
	{
		if ( faulty ) return "Faulty";

		try {
			Thread.sleep( 300 ); // 70
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		flushData();
		write("RSN");
		
		try {
			Thread.sleep( 500 );
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		byte[] data;
		try {

			data = serial.readBytes();
			if ( data != null )
			{
				for ( byte b : data )
				{
					if ( b!= 13 )
					{
						readData+= (char)b;
					}
				}
				//System.out.println("DATA READ: " + readData );
			}
			if ( readData.length() > 0 )
			{
				this.innerSerialNumber = readData;
				System.out.println( this.innerSerialNumber );
				this.innerSerialNumber = this.innerSerialNumber.subSequence( 0 , 4 ).toString();
			}

		} catch (SerialPortException e ) {
			e.printStackTrace();
		}catch( NumberFormatException e ){
			e.printStackTrace();
		}
		
		return this.innerSerialNumber;
	}
	
	/** This should not be called if the antenna is enabled. */
	public double readFrequency()
	{
		if ( faulty ) return -1;

		if ( enabled )
		{
			System.out.println("Antenna should be disabled to monitor its frequency.");
		}
		double value = -1;

		switchOn();

		try {
			Thread.sleep( 300 ); // 70
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		if ( stopReadingThread ) return 0;

		flushData();
		write("MOF");

		try {
			Thread.sleep( 2000 );
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		if ( stopReadingThread ) return 0;
		
		byte[] data;
		try {

			data = serial.readBytes();
			if ( data != null )
			{
				for ( byte b : data )
				{
					if ( b!= 13 )
					{
						readData+= (char)b;
					}
				}
				//System.out.println("DATA READ: " + readData );
			}
			if ( readData.length() > 0 )
			{
				value = Double.parseDouble( readData );
			}

		} catch (SerialPortException e ) {
			e.printStackTrace();
		}catch( NumberFormatException e ){
			e.printStackTrace();
		}

		switchOff();

		this.frequency = value;
		System.out.println("Antenna on port " + this.comPort + " is tuned to " + this.frequency + " / 134.2 kHz");

		return value;
	}

	@Override
	public String toString() {
		return "Antenna " + this.comPort + " innerSerial: " + this.innerSerialNumber;
	}
	
	@Override
	public void run() {

		setPriority( Thread.MIN_PRIORITY );
		readInnerSerialNumber();
		readFrequency();

		
		

		/** Workaround over the switch off that is not working each time
		 * to count the number of cycle in disable mode, to send several switch off orders */
		int disableCount = 0;

		while ( true )
		{
			if ( faulty ) return;

			if ( stopReadingThread ) return;


			if ( enabled )
			{
				disableCount = 0;
				//System.out.println("RFID working " + location );
				//				System.out.println("O");

				//		Thread.sleep( 100 ); // 15
				
				// 

				if ( ! lastReadOrder.startsWith("SRA" ) )  // SRA = switch On. If the last order was switch on
				{
					switchOff();
					switchOn();
				}
				
				sendReadOrder();
				try {
					Thread.sleep( 100 ); // 70
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				readData();
				
				
				//readData();
				
				/*//previous version: 
				switchOn();

				try {
					Thread.sleep( 100 ); // 70
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				sendReadOrder();
				//		Thread.sleep( 80 );
				readData();
				//		Thread.sleep( 100 );
				switchOff();
				//		Thread.sleep( 50 );
				 *
				 */
			}else
			{
				flushData();

				disableCount++;
				if ( disableCount < 10 )
				{

					// FIX: CONTiNuOUS SWITCH OFF IF NOT ENABLE
					switchOff();
				}

				try {
					Thread.sleep( 10 ); // 70
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

	}

	String readData ="";

	void sendReadOrder( )
	{
		nbTryRead ++;
//		System.out.println("Read try: " + nbTryRead );
		write("RAT");
	}

	void readData( )
	{
		lastReadOrder = "RFIDREAD";
		
		try {
		//	System.out.println( "1:" + serial.readString() );

			byte[] data = serial.readBytes();
			if ( data != null )
			{
				for ( byte b : data )
				{
					if ( b!= 13 )
					{
//						if ( b >= '0' & b <='9' )
//						{
							readData+= (char)b;
//						}
							if ( readData.endsWith("?1") ) // an error occured
							{
								readData =""; // reset reading
							}
							if ( readData.endsWith("OK") ) // we don't need it
							{
								readData =""; // reset reading
							}
					}
					else
					{
						// proper RFID code pattern is 955 000003455212
//						System.out.println( "" + comPort + " read " + readData );
						String[] strings = readData.split("_");
						for ( String s : strings )
						{
							if ( s.length() == 12 )
							{
//								System.out.println("" + comPort + " / National RFID tag: " + s );
								// this lastReadOrder was affected only if something was read.
								// lastReadOrder = "RFIDREAD"; // this is to reset the lastReadOrder value SRA by RFIDOrder so that the reader gets switch off and on again
								String id=s;
								AntennaReadEvent event = new AntennaReadEvent(
										LiveMouseTracker.getT(),
										LiveMouseTracker.RFID_DEFAULT_LATENCY,
										id, this.location , ray, this );
								fireRFIDEvent( event );
								nbEvent++;
								//LiveMouseTracker.addEvent( new Event( "Read", Color.green, this.location ) );
							}
						}
						readData ="";
					}
				}
			}

		} catch (SerialPortException e) {
			e.printStackTrace();
		}

	}

	void switchOn()
	{
		{
			write("SRA");		// Activate antenna.
			onOffstate = AntennaState.ON;
		}
	}

	public void switchOff()
	{
//		for ( int i = 0 ; i< 4 ; i++ )
		{
			write("SRD");		// Desactivate antenna.
			onOffstate = AntennaState.OFF;

//			try {
//				Thread.sleep( 50 );
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
		}
	}

	void write( String string )
	{
		try {
			lastReadOrder = string;
			serial.writeString( string );
			serial.writeByte( (byte) 13 );
		} catch (SerialPortException e) {
			e.printStackTrace();
		}
	}

	public void shutdown() {

		if ( faulty ) return;

		try {
			stopReadingThread = true;
			try {
				Thread.sleep( 1000 ); // wait for the longest operation that could occur like read frequency
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			serial.closePort();
			System.out.println("RFID Antenna "+ comPort + " shutdown.");
		} catch (SerialPortException e) {
			System.out.println("Serial already closed: " + comPort );
			// e.printStackTrace();
		}
	}

	public void paint(Graphics2D g) {

		g = (Graphics2D) g.create();
		
		if ( enabled )
		{
			g.setColor( Color.green.darker() );
		}else
		{
			g.setColor( Color.gray );
		}
		if ( faulty )
		{
			g.setColor( Color.red.darker() );
		}
		g.setStroke( new BasicStroke( 2 ) ) ;

		Ellipse2D ellipse2D = new Ellipse2D.Double(
				location.getX()-ray, location.getY()-ray,
				ray*2+1, ray*2+1 );
		g.draw( ellipse2D );
		g.setColor( Color.yellow );

		GraphicsUtil.mixAlpha(g, AlphaComposite.SRC_OVER, 0.5f);
		
		DrawUtil.drawCenteredString( g, comPort.substring(3) + "/" + this.innerSerialNumber , (int)location.getX(), (int)location.getY()-10 );
		//DrawUtil.drawCenteredString( g, ""+onOffstate, (int)location.getX(), (int)location.getY() );
//		g.drawString( comPort + "/" + onOffstate, (int)location.getX(), (int)location.getY() );
/*		g.drawString( "f:"+frequency, (int)location.getX(), (int)location.getY()+10 );
		// try read / nb of event
*/
		DrawUtil.drawCenteredString( g, ""+nbTryRead+"/" + nbEvent , (int)location.getX(), (int)location.getY() );
//		g.drawString( "r/e%: "+ (int)( 100d * nbEvent / nbTryRead ) , (int)location.getX(), (int)location.getY()+30 );
		/*
		Util.drawCenteredString( g, comPort, (int)location.getX(), (int)location.getY()-10 );
		Util.drawCenteredString( g, ""+onOffstate, (int)location.getX(), (int)location.getY() );

		Util.drawCenteredString( g, ""+nbTryRead+"/" + nbEvent , (int)location.getX(), (int)location.getY()+10 );
*/
	}

	public void addRFIDAntennaListener( AntennaReadListener rfidAntennaListener )
	{
		synchronized ( rfidAntennaListenerList ) {
			rfidAntennaListenerList.add( rfidAntennaListener );
		}
	}
	
	public void removeRFIDAntennaListener( AntennaReadListener rfidAntennaListener )
	{
		synchronized ( rfidAntennaListenerList ) {
			rfidAntennaListenerList.remove( rfidAntennaListener );
		}
	}

	public void fireRFIDEvent( AntennaReadEvent rfidEvent )
	{
		synchronized ( rfidAntennaListenerList ) {
			for ( AntennaReadListener listener : rfidAntennaListenerList )
			{
				listener.antennaEvent( rfidEvent );
			}
		}
	}

	@Override
	public String getIdentifier() {

		String ip = null;
		while ( ip==null)
		{
			ip = NetworkUtil.getLocalIP();
			if ( ip ==null )
			{
				try {
					System.out.println("Network Waiting for IP adress");
					Thread.sleep( 1000 );
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}


		return NetworkUtil.getLocalIP() +" "+getComPort();
	}


}
