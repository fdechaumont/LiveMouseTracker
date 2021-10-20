package plugins.fab.livemousetracker.device.sensor;

import java.awt.Graphics2D;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import jssc.SerialPort;
import jssc.SerialPortException;
import plugins.fab.livemousetracker.serial.SerialDriverPlugin;

public class SensorMonitor extends Thread {

	SerialPort serial = null;
	boolean faulty = false;

	String comPort;

	public String getComPort() {
		return comPort;
	}

	public SensorMonitor( String comPort ) {
		super("Thread Monitor Sensors" + comPort);

		new SerialDriverPlugin();
		this.comPort = comPort;

		System.out.println("Starting Sensor (temp/humidity/sound level/light) monitor on port " + comPort );
		serial = new SerialPort( comPort );
		try {
			serial.openPort();
			serial.setParams( 115200, 8, 1, 0 );
		} catch (SerialPortException e) {

			System.out.println("No sensor device found on port: " + comPort );
			faulty = true;
		}

		start();
	}

	boolean stopReadingThread = false;

	public boolean isFaulty() {
		return faulty;
	}

	@Override
	public void run() {

		setPriority( Thread.MIN_PRIORITY );

		while ( true )
		{
			if ( faulty ) return;

			if ( stopReadingThread ) return;

			readData();

			try {
				Thread.sleep( 500 );
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	String readData ="";

	double lightVisible = 0;
	double lightIRandVisible = 0;
	double humidity = 0;
	double temperature = 0;
	double soundLevel = 0;

	/** Indice of the amount of visible and infrared light received */
	public double getLightInfraredAndVisible() {
		return lightIRandVisible;
	}
	/** Indice of the amount of visible light received */
	public double getLightVisible() {
		return lightVisible;
	}

	/** percentage of relative humidity in the range of 0 to 80 RH */
	public double getHumidity() {
		return humidity;
	}

	/** temp in degrees Celcius */
	public double getTemperature() {
		return temperature;
	}

	/** the value of the microphone is read continuously, the value reported here is from 0 to 100
	 * 100 is the maximum between min an max reding during a sampling window of 800ms.
	 * @return
	 */
	public double getSoundLevel() {
		return soundLevel;
	}



    Date lastReadDate = null;

    public Date getLastReadDate() {
		return lastReadDate;
	}

    private void readOk()
    {
    	lastReadDate = Calendar.getInstance().getTime();
    }

    /**
     * Provide last reading date (any available reading)
     * @return
     */
    public String getReadableLastReadDate()
    {
    	if ( lastReadDate == null ) return "None";
    	return new SimpleDateFormat("dd:MM:yyyy - HH:mm:ss.SSS").format( lastReadDate );
    }

	void readData( )
	{

		try {
			String string = serial.readString();

			if ( string == null ) return;

			String lines[] = string.split("\\r?\\n");

			for ( int i = 0 ; i < lines.length-1 ; i++ ) // skip the last one that may be incomplete
			{
				String line = lines[i];

				if ( line.contains("lum visible:") )
				{
					try
					{
						lightVisible = Double.parseDouble( line.split(":")[1] );
						readOk();
					}
					catch( NumberFormatException e ) {}
					catch( ArrayIndexOutOfBoundsException e ) {}
				}
				if ( line.contains("lum IR+visible:") )
				{
					try
					{
						lightIRandVisible = Double.parseDouble( line.split(":")[1] );
						readOk();
					}
					catch( NumberFormatException e ) {}
					catch( ArrayIndexOutOfBoundsException e ) {}
				}
				if ( line.contains("Humidity:") )
				{
					try
					{
						humidity = Double.parseDouble( line.split(":")[1] );
						readOk();
					}
					catch( NumberFormatException e ) {}
					catch( ArrayIndexOutOfBoundsException e ) {}
				}
				if ( line.contains("Temperature:") )
				{
					try
					{
						temperature = Double.parseDouble( line.split(":")[1] );
						readOk();
					}
					catch( NumberFormatException e ) {}
					catch( ArrayIndexOutOfBoundsException e ) {}
				}
				if ( line.contains("Sound level:") )
				{
					try
					{
						soundLevel = Double.parseDouble( line.split(":")[1] );
						readOk();
					}
					catch( NumberFormatException e ) {}
					catch( ArrayIndexOutOfBoundsException e ) {}
				}
			}

			// parse data:
			/* null if nothing
lum visible: 5033
lum IR+visible: 33334
Humidity: 37.25
Temperature: 27.45
Sound level: 6.25
			 */

		}

		catch (SerialPortException e) {
			e.printStackTrace();
		}

	}

	public void shutdown() {

		if ( faulty ) return;

		try {
			stopReadingThread = true;
			serial.closePort();
			System.out.println("Sensor monitor "+ comPort + " shutdown.");
		} catch (SerialPortException e) {
			e.printStackTrace();
		}
	}

	public void paint(Graphics2D g) {

		/*
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


		DrawUtil.drawCenteredString( g, comPort, (int)location.getX(), (int)location.getY()-10 );
		DrawUtil.drawCenteredString( g, ""+onOffstate, (int)location.getX(), (int)location.getY() );
//		g.drawString( comPort + "/" + onOffstate, (int)location.getX(), (int)location.getY() );
		g.drawString( "f:"+frequency, (int)location.getX(), (int)location.getY()+10 );
		// try read / nb of event
		DrawUtil.drawCenteredString( g, ""+nbTryRead+"/" + nbEvent , (int)location.getX(), (int)location.getY()+10 );
//		g.drawString( "r/e%: "+ (int)( 100d * nbEvent / nbTryRead ) , (int)location.getX(), (int)location.getY()+30 );
		Util.drawCenteredString( g, comPort, (int)location.getX(), (int)location.getY()-10 );
		Util.drawCenteredString( g, ""+onOffstate, (int)location.getX(), (int)location.getY() );

		Util.drawCenteredString( g, ""+nbTryRead+"/" + nbEvent , (int)location.getX(), (int)location.getY()+10 );

		*/
	}

}
