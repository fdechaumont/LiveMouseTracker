package plugins.fab.livemousetracker.device.ttl;

import icy.plugin.interface_.PluginThreaded;
import plugins.fab.livemousetracker.device.ttl.TTLSynchronizer.TTL_SIGNAL;
import plugins.fab.livemousetracker.serial.SerialDriverPlugin;
import icy.plugin.abstract_.PluginActionable;

public class TTLEventListenerTest extends PluginActionable implements PluginThreaded {

	TTLEventListener ttlEventListener = null;

	public void run() {

		new SerialDriverPlugin();

		System.out.println("Starting ttlEventListener for test purposes...");
		ttlEventListener = new TTLEventListener("COM27", true );

	};

}
