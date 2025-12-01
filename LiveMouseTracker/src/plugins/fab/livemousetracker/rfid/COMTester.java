package plugins.fab.livemousetracker.rfid;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;
import plugins.fab.livemousetracker.LiveMouseTracker;
import plugins.fab.livemousetracker.overlay.Event;

public class COMTester extends Thread {

	// This class test if an RFID reader is on a port COM
	
	SerialPort serial = null;
	String comPort;
	String innerSerialNumber = null;
	boolean error = false;
	Thread watchDog;
	
	public COMTester( String comPort ) {
		super("Thread COMTESTER " + comPort);
	
		this.comPort = comPort;
		
		log("COM Tester starting.");
		log("instanciate port");
		serial = new SerialPort( comPort );
		try {
			log("open port");
			serial.openPort();
			serial.setParams( 9600, 8, 1, 0 );
			write( "ST2" ); // Set read animal tags.
			write( "SB1" ); // Disable read buzzer
			write( "SL4" ); // Leds off						
			switchOff();

		} catch (SerialPortException e) {
			//e.printStackTrace();
			System.err.println("COM PORT ERROR (disabling.): " + comPort );
			error = true;
			return;
		}
		
		this.start();
	
		watchDog = new Thread( new Runnable() {
			
			@Override
			public void run() {
				try {
					Thread.sleep( 2000 );
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				try {
					if ( serial.isOpened() )
					{	
						log("timeout: forcing close port.");						
						serial.closePort();
					}
				} catch (SerialPortException e) {
					e.printStackTrace();
				}
			}
		} );
		watchDog.start();
	
		// peut etre mettre un timer pour stopper le serial au bout de 1 seconde
		
	}
	
	private void log( String s )
	{
		String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date());
		System.out.println( timeStamp + " - COM Tester " + comPort + " : " + s );
	}
	
	@Override
	public void run() {

		setPriority( Thread.MIN_PRIORITY );
		/*
		log("COM Tester starting.");
		log("instanciate port");
		serial = new SerialPort( comPort );
		*/
		//try {
			/*
			log("open port");
			//serial.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
			serial.openPort();
			log("set params");
			serial.setParams( 9600, 8, 1, 0 );
			
			write( "ST2" ); // Set read animal tags.
			write( "SB1" ); // Disable read buzzer
			write( "SL4" ); // Leds off		
			for ( int i =0 ; i<10; i++ )
			{
				write( "SRD"); // switch off
			}
			*/
			
						
		log("read inner serial");
		readInnerSerialNumber();

		try
		{
			log("close port");
			serial.closePort();
		}catch( SerialPortException e )
		{
			System.err.println("COM PORT Tester: error while closing : " + comPort );
		}
		log("COM Tester end: " + comPort );
		
	}
	
	
	
	/** This should not be called if the antenna is enabled. */	
	private String readInnerSerialNumber()
	{
		if ( error ) return "Faulty";

		try {
			Thread.sleep( 300 ); // 70
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		log("flush data");
		flushData();
		write("RSN");
		
		try {
			Thread.sleep( 500 );
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		log("read response");

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
				log( this.innerSerialNumber );
				try {
				this.innerSerialNumber = this.innerSerialNumber.subSequence( 0 , 4 ).toString();
				}
				catch(Exception e)
				{
					log("Error in reading com port serial number");
					// something went wrong during conversion
					this.innerSerialNumber = "ERRO";
				}
			}

		} catch (SerialPortException e ) {
			e.printStackTrace();
		}catch( NumberFormatException e ){
			e.printStackTrace();
		}
		
		return this.innerSerialNumber;
	}
	
	String readData ="";
	
	private void flushData() {
		if ( this.error ) return;

		try {
			serial.readBytes( serial.getInputBufferBytesCount() , 10 );
		} catch (SerialPortException e) {
			//e.printStackTrace();
			System.out.println("[Serial t:"+LiveMouseTracker.getT()+"] Read error on port " + comPort );
			this.error = true;
		} catch (SerialPortTimeoutException e) {
			// TimeOut sur lecture du port com.
			System.out.println("[Serial t:"+LiveMouseTracker.getT()+"] Time out on port " + comPort );
		} catch ( NegativeArraySizeException e2 )
		{
			System.out.println("[Serial t:"+LiveMouseTracker.getT()+"] Negative Arrray size exception. " + comPort );
			
			for ( int i = 0 ; i< 10 ; i++ )
			{
				switchOff();
				try {
					Thread.sleep( 10 ); // 70 // 10
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}			
			this.error = true;
			
		}
		readData ="";
	}

	public void switchOff()
	{
		write("SRD");
	}
	
	void write( String string )
	{
		try {
			serial.writeString( string );
			serial.writeByte( (byte) 13 );
		} catch (SerialPortException e) {
			e.printStackTrace();
		}
	}

	public boolean isRFIDReader() {
		
		if ( this.innerSerialNumber == null )
			return false;
		return true;
		
		
	}
	
}
