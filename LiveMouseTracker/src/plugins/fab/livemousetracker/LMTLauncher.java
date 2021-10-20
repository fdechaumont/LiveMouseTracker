package plugins.fab.livemousetracker;

import icy.plugin.abstract_.PluginActionable;

public class LMTLauncher extends PluginActionable {

	public static boolean launchOK = false;

	@Override
	public void run() {

		this.launchOK = true;
		System.out.println("LMT setup procedure ok.");
	}

}
