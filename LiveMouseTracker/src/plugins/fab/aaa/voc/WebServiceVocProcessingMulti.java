package plugins.fab.aaa.voc;

import java.util.ArrayList;


import icy.plugin.abstract_.PluginActionable;
import icy.plugin.interface_.PluginThreaded;

public class WebServiceVocProcessingMulti extends PluginActionable implements PluginThreaded{

	ArrayList<Thread> services = new ArrayList<Thread>();

	@Override
	public void run() {

		// Note: multi is not working yet as it should re-dispatch message from server
		// using processor Id. (todo). Seems they all share same session and also same route :/
		//
		/*
		for ( int i = 0 ; i < 5 ; i++ )
		{
			PluginActionable p = new WebServiceVocProcessing();
			Thread t = new Thread( p );
			t.start();
			services.add( t );
		}
		*/

	}

}
