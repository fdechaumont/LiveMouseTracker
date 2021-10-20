package plugins.fab.aaa.voc;

import icy.plugin.abstract_.PluginActionable;
import icy.plugin.interface_.PluginThreaded;

public class TriangulationLauncher extends PluginActionable implements PluginThreaded {

	TriangulationThread triangulationThread = new TriangulationThread();

	@Override
	public void run() {

		triangulationThread.start();

		while ( triangulationThread.isAlive() )
		{
			try {
				Thread.sleep( 100 );
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

}
