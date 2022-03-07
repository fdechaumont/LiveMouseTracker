/**
 	@author Fabrice de Chaumont @ Institut Pasteur

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

package plugins.fab.livemousetracker.device.arduinoCommander;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;
import plugins.fab.livemousetracker.LiveMouseTracker;
import plugins.fab.livemousetracker.Util;
import plugins.fab.livemousetracker.overlay.Event;
import plugins.fab.livemousetracker.serial.SerialDriverPlugin;

/**
 * Designed for uRFID_USB_PriorityDesign
 *
 * */
public class ArduinoManager extends Thread {

	SerialPort serial = null;
	String comPort;

	boolean faulty = false;

	enum ArduinoState
	{
		ON, OFF, UNKNOWN
	}

	ArduinoState onOffstate = ArduinoState.UNKNOWN;

	public ArduinoManager( String comPort ) {

		super("Thread " + comPort);

		new SerialDriverPlugin();

		this.comPort = comPort;

		System.out.println("Starting LMT to Arduino Manager on port " + comPort );
		serial = new SerialPort( comPort );

		try {
			serial.openPort();
			//serial.setParams( 9600, 8, 1, 0 );
			//serial.setParams( 115200, 8, 1, 0 );
			serial.setParams( 250000, 8, 1, 0 );
			write( "init" );

		} catch (SerialPortException e) {
			//e.printStackTrace();
			System.err.println("COM PORT ERROR (disabling.): " + comPort );
			faulty = true;
		}

		setEnabled( true );

		start();
	}

	boolean stopReadingThread = false;
	boolean enabled = false;

	public void setEnabled(boolean enabled) {
		if ( faulty ) return;

		this.enabled = enabled;

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
			
		}
		readData ="";
	}

	@Override
	public void run() {

		setPriority( Thread.MIN_PRIORITY );

		boolean blinkTest = false;

		int test = 0;

		while ( true )
		{
			if ( faulty ) return;
			if ( stopReadingThread ) return;

			if ( enabled )
			{

				try {
					Thread.sleep( 30 );
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				readData();

				test++;
				// once each second
				if ( test > 30 )
				{
					try {

						if ( blinkTest )
						{
							System.out.println("u13");
							serial.writeString("u13");
						}else
						{
							System.out.println("l13");
							serial.writeString("l13");
						}
						System.out.println( blinkTest );
						blinkTest = !blinkTest;

					} catch (SerialPortException e) {
						e.printStackTrace();
					}
					test=0;
				}

				flushData();

			}
				/*
				//System.out.println("RFID working " + location );
				//				System.out.println("O");

				//		Thread.sleep( 100 ); // 15
				switchOn();

				try {
					Thread.sleep( 100 ); // 70
				} catch (InterruptedException e) {
					e.printStackTrace();
				}


				//		Thread.sleep( 80 );
				readData();
				//		Thread.sleep( 100 );
				switchOff();
				//		Thread.sleep( 50 );
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
			*/
		}

	}

	String readData ="";


	void readData( )
	{
		try {
			String received = serial.readString();
			if ( received != null )
			{
				System.out.println("Str received: " + received );
			}




		} catch (SerialPortException e) {
			e.printStackTrace();
		}




		/*
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
								String id=s;

								RFIDEvent event = new RFIDEvent(
										LiveMouseTracker.getT(),
										LiveMouseTracker.RFID_DEFAULT_LATENCY,
										id, this.location , ray );
								fireRFIDEvent( event );
								nbEvent++;

							}
						}
						readData ="";
					}
				}
			}

		} catch (SerialPortException e) {
			e.printStackTrace();
		}
		*/

	}

	void switchOn()
	{
		{
			write("SRA");		// Activate antenna.
			//onOffstate = RFIDAntenna2State.ON;
		}
	}

	void switchOff()
	{
		{
			write("SRD");		// Desactivate antenna.
			//onOffstate = RFIDAntenna2State.OFF;
		}
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

	public void shutdown() {

		stopReadingThread = true;

		if ( faulty ) return;

		try {
			serial.closePort();
			System.out.println("RFID Antenna "+ comPort + " shutdown.");
		} catch (SerialPortException e) {
			e.printStackTrace();
		}
	}




}
