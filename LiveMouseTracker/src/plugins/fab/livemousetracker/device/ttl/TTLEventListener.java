package plugins.fab.livemousetracker.device.ttl;


import java.util.ArrayList;
import java.util.HashMap;

import jssc.SerialPort;
import jssc.SerialPortException;
import plugins.fab.livemousetracker.LiveMouseTracker;

public class TTLEventListener extends Thread {

    SerialPort serial = null;
    boolean faulty = false;

    String comPort;
    String currentReadBuffer ="";
    boolean testMode = false;

    public String getComPort() {
        return comPort;
    }

    public TTLEventListener( String comPort, boolean testMode ) {
        super("Thread TTL event Listener on com port " + comPort);

        this.comPort = comPort;

        System.out.println("Starting TTL event listener on port " + comPort );
        serial = new SerialPort( comPort );
        try {
            serial.openPort();
            serial.setParams( 115200, 8, 1, 0 ); // 1000000 , 115200
        } catch (SerialPortException e) {

            System.out.println("No Arduino device found on port: " + comPort );
            faulty = true;
        }

        this.testMode = testMode;
        System.out.println("TTL Event listener Running in test mode: " + testMode );
        start();
    }

    @Override
    public void run() {

    	setPriority( Thread.MIN_PRIORITY );


		while ( true )
		{
			if ( faulty ) return;
			if ( !enabled ) return;

			read();

			if ( testMode )
			{
				currentT ++;
			}

			try {
				Thread.sleep( 30 );
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}

    }

    public void read()
    {
        String s =" ";
        while ( s!=null )
        {
            try {
                s = serial.readString();

                if ( s!= null )
                {
                	currentReadBuffer+= s;
                }

            } catch (SerialPortException e) {
                e.printStackTrace();
            }
        }

        parseBuffer();
    }



    private void parseBuffer() {

    	String[] lineArray = currentReadBuffer.split("\n");

    	currentReadBuffer = "";

    	for ( String line : lineArray )
    	{
    		if ( line.contains("#") && line.contains( "$" ) )
    		{
    			//#startEvent*event name$
    			String[] tokenArray = line.split("\\*");
    			String state = tokenArray[0].substring( 1 );
    			String name = tokenArray[1].split("\\$")[0];

    			System.out.println("Received: " + state + " " + name  );

    			manageEvent( state, name );
    		}
    		else
    		{
    			currentReadBuffer+=line; // incomplete line kept for next parse.
    		}
    	}

	}

    ArrayList<TTLReceivedEvent> ttlReceivedEventList = new ArrayList<>();

    int currentT = 0;
	private void manageEvent(String state, String name )
	{
		if ( testMode )
		{
			System.out.println("TTL Receiver test mode: current T : " + currentT );
		}

		if ( testMode == false )
		{
			currentT = LiveMouseTracker.getT();
		}

		// find existing event.
		TTLReceivedEvent existingEvent  = findEvent( name );
		if ( existingEvent == null )
		{
			TTLReceivedEvent newEvent = new TTLReceivedEvent(name );
			if ( state.equals( "startEvent" ) )
			{
				ttlReceivedEventList.add( newEvent );
				newEvent.startFrame = currentT;
			}

			if ( state.equals( "endEvent" ) )
			{
				System.out.println("TTL Receiver event error: an event is ending but has never began.");
				System.out.println("TTL Receiver Event name: " + name );
			}
		}
		if ( existingEvent != null )
		{
			if ( state.equals( "startEvent" ) )
			{
				System.out.println("TTL Receiver event error: an event is starting but already exists.");
				System.out.println("TTL Receiver Event name: " + name );
			}
			if ( state.equals( "endEvent" ) )
			{
				ttlReceivedEventList.remove( existingEvent );
				existingEvent.endFrame = currentT;
				// register event
				LiveMouseTracker.addEventLogToDataBase( existingEvent.toEventLog() );
				System.out.println("TTLReceiver : Adding event " + existingEvent );
			}
		}
	}

	private TTLReceivedEvent findEvent(String name) {

		for ( TTLReceivedEvent event : ttlReceivedEventList )
		{
			if ( event.name.equals( name ) )
			{
				return event;
			}
		}

		return null;
	}

	public void shutdown() {

        if ( faulty ) return;

        try {
            serial.closePort();
            System.out.println("TTL event listener "+ comPort + " shutdown.");
        } catch (SerialPortException e) {
            e.printStackTrace();
        }
    }

	boolean enabled = true;
	public void setEnable(boolean b) {
		this.enabled = b;
	}


}
