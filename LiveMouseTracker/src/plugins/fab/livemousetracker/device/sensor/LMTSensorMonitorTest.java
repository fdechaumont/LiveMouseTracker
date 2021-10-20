package plugins.fab.livemousetracker.device.sensor;

import icy.plugin.interface_.PluginThreaded;
import plugins.fab.livemousetracker.serial.SerialDriverPlugin;
import icy.plugin.abstract_.PluginActionable;

public class LMTSensorMonitorTest extends PluginActionable implements PluginThreaded {


	public void run() {

		System.out.println("Starting sensor reading on port COM29..");
		SensorMonitor sensorMonitor = new SensorMonitor("COM29");

		new SerialDriverPlugin();

		while( true )
		{
			System.out.println("Last reading: " + sensorMonitor.getReadableLastReadDate() );
			System.out.println("Temperature : " + sensorMonitor.getTemperature() );
			System.out.println("Humidity : " + sensorMonitor.getHumidity() );
			System.out.println("Sound level : " + sensorMonitor.getSoundLevel() );
			System.out.println("Light visible : " + sensorMonitor.getLightVisible() );
			System.out.println("Light IR+visible : " + sensorMonitor.getLightInfraredAndVisible() );

			try{
				Thread.sleep( 1000 );
			}
			catch( InterruptedException e )
			{

			}
		}

	};

}
