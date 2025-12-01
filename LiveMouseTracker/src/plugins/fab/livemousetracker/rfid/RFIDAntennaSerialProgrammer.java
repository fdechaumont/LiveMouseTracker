package plugins.fab.livemousetracker.rfid;

import jssc.SerialPort;
import jssc.SerialPortException;


public class RFIDAntennaSerialProgrammer {

	SerialPort serial = null;
	
	public RFIDAntennaSerialProgrammer( String comPort , String innerSerial ) 
	{
		System.out.println("Programming RFID reader inner serial: " + innerSerial + " --> "+ comPort +" ..." );


		if ( innerSerial.length() != 4 )
		{
			System.out.println("Inner serial must be 4 hex long 0-F ");
			return;
		}
		
		serial = new SerialPort( comPort );
		try {
			serial.openPort();
			serial.setParams( 9600, 8, 1, 0 );
			write( "SSN"+innerSerial );

		} catch (SerialPortException e) {
			//e.printStackTrace();
			System.err.println("Error while programming " + comPort );			
		}
		
		try {
			serial.closePort();
		}catch (SerialPortException e) {
			System.err.println("Can't close serial " + comPort );
		}
		
		System.out.println("Programming " + comPort +" done" );
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
	

}
