package plugins.fab.livemousetracker.device.ttl;

import java.util.HashMap;

import jssc.SerialPort;
import jssc.SerialPortException;

public class TTLSynchronizer extends Thread {

    SerialPort serial = null;
    boolean faulty = false;

    String comPort;

    public String getComPort() {
        return comPort;
    }

    public TTLSynchronizer( String comPort ) {
        super("Thread TTL synchro on com port " + comPort);

        this.comPort = comPort;

        System.out.println("Starting TTL synchro on port " + comPort );
        serial = new SerialPort( comPort );
        try {
            serial.openPort();
            serial.setParams( 1000000, 8, 1, 0 ); // 1000000 , 115200
        } catch (SerialPortException e) {

            System.out.println("No Arduino TTL device found on port: " + comPort );
            faulty = true;
        }

        start();
    }

    public enum TTL_SIGNAL {
    	SYNCHRO_START,
    	SYNCHRO_FRAME,
    	SYNCHRO_SHUTDOWN
    	};

    public void sendTTL( TTL_SIGNAL signal )
    {
    	if ( faulty ) return;
    	if ( !this.serial.isOpened() )
    	{
    		return;
    	}
    	// send signal ( u for up, l for low, and number for pin )
    	switch( signal )
    	{
    	case SYNCHRO_START:
    		setPin(2, true );
    		setPin(2, false );
    		break;
    	case SYNCHRO_FRAME:
    		setPin(3, true );
    		setPin(3, false );
    		break;
    	case SYNCHRO_SHUTDOWN:
    		setPin(2, true );
    		setPin(2, false );
    		break;
    	default:
    		break;
    	}


        flushRead();
    }

    public void flushRead()
    {
        String s =" ";
        while ( s!=null )
        {
        	if ( !this.serial.isOpened() )
        	{
        		break;
        	}
            try {
                s = serial.readString();
            } catch (SerialPortException e) {
                e.printStackTrace();
            }
        }
    }


    public void shutdown() {

        if ( faulty ) return;

        try {
            serial.closePort();
            System.out.println("TTL synchro "+ comPort + " shutdown.");
        } catch (SerialPortException e) {
            e.printStackTrace();
        }
    }

    HashMap<String,Boolean> eventStateMap = new HashMap<String,Boolean>();

	public void updateEventState(String eventName, int digitalPin, boolean eventActive ) {

		boolean isEventAlreadyActive = false;
		boolean eventIsRegistered = false;
		String eventNameRegistered = null;

		if ( eventStateMap.keySet() != null )
		{
			for ( String eventNameCandidate : eventStateMap.keySet() )
			{
				if ( eventNameCandidate.equals( eventName ) )
				{
					eventNameRegistered = eventNameCandidate;
					eventIsRegistered = true;
					isEventAlreadyActive = eventStateMap.get( eventNameCandidate );
				}
			}
		}

		if ( ! eventIsRegistered )
		{
			// register event and apply state.
			eventStateMap.put( eventName, eventActive );
			setPin( digitalPin, eventActive );
			return;
		}

		if ( isEventAlreadyActive == eventActive ) return; // no state change.

		if ( isEventAlreadyActive )
		{
			setPin( digitalPin , false );
			eventStateMap.put( eventNameRegistered, false );

		}else
		{
			setPin( digitalPin, true );
			eventStateMap.put( eventNameRegistered, true );
		}

	}

	private void setPin(int digitalPin, boolean high) {

		if ( faulty ) return;

		String s = "l";
		if ( high )
		{
			s = "u";
		}
		s+=""+digitalPin+"-";

		try {
			serial.writeString( s );
//			System.out.println( s );

			//System.out.println( s );
			//Integer i = new Integer( digitalPin );
			//serial.writeString( ""+(char)(13&0xFF) );
			//serial.writeByte( (byte) 13 ); // i.byteValue() );
			//serial.writeByte( (byte)(digitalPin & 0xFF ) );
			/*
			if ( high )
			{
				serial.writeString( "u" );
			}else
			{
				serial.writeString( "l" );
			}
			if ( digitalPin < 10 )
			{
				serial.writeString( "0" );
			}
			serial.writeByte( digitalPin );
	*/
			//serial.writeByte( (byte)(digitalPin & 0xFF) );
			//serial.writeByte( (byte)digitalPin );
//			serial.writeByte( (byte)'-' );
			//serial.writeString( s + "" + digitalPin );
			//System.out.println("ttl sending: " + s );
			//serial.writeByte( s );
			//serial.writeByte((byte) digitalPin);
			//System.out.println( "ttl monitor sending: " + s );


			//System.out.println("received: " + serial.readString() );

		} catch (SerialPortException e) {
			faulty=true;
			e.printStackTrace();
		}


	}


}
