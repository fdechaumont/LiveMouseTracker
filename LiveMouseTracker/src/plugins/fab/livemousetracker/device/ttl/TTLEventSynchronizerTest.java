package plugins.fab.livemousetracker.device.ttl;

import icy.plugin.interface_.PluginThreaded;
import plugins.fab.livemousetracker.device.ttl.TTLSynchronizer.TTL_SIGNAL;
import plugins.fab.livemousetracker.serial.SerialDriverPlugin;
import icy.plugin.abstract_.PluginActionable;

public class TTLEventSynchronizerTest extends PluginActionable implements PluginThreaded {


	public void run() {

		new SerialDriverPlugin();

		System.out.println("Starting TTL on port COM28...");
		System.out.println("Arduino nano should be connected on port 28 with lmt2arduinoCommander.ino loaded");
		System.out.println("Pin d2 will receive the start synchro");
		System.out.println("Pin d4 and 13 (led) an opposite signal each 33ms (to mimic the 30 fps rate)");
		System.out.println("Pin d6 provide a random signal each 33ms");

		TTLSynchronizer ttlEventSynchronizer = new TTLSynchronizer("COM28");

		ttlEventSynchronizer.sendTTL( TTL_SIGNAL.SYNCHRO_START );

		while( true )
		{

			ttlEventSynchronizer.updateEventState("event test 1", 2, true );
			ttlEventSynchronizer.updateEventState("event test 2", 3, false );
			ttlEventSynchronizer.updateEventState("event random", 13, Math.random() < 0.5d );

			try{
				Thread.sleep( 33 );
			}
			catch( InterruptedException e )
			{
				System.out.println("error");
			}

			ttlEventSynchronizer.updateEventState("event test 1", 2, false );
			ttlEventSynchronizer.updateEventState("event test 2", 3, true );
			ttlEventSynchronizer.updateEventState("event random", 13, Math.random() < 0.5d );

			try{
				Thread.sleep( 33 );
			}
			catch( InterruptedException e )
			{
				System.out.println("error");
			}

			if ( ttlEventSynchronizer.faulty )
			{
				System.out.println("TTL communication faulty.");
				return;
			}
		}

	};

}
