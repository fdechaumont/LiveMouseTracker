package plugins.fab.livemousetracker.remotearena;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

import icy.plugin.abstract_.PluginActionable;
import icy.plugin.interface_.PluginThreaded;
import icy.sequence.Sequence;
import plugins.fab.livemousetracker.remotearena.server.LMTRemoteAreaServer;

public class TestLMTRemoteAreaServer extends PluginActionable implements PluginThreaded {

	Sequence outSequence = new Sequence("Receiving data");

	@Override
	public void run() {

		// TODO/FIX: check if that anonymous is somehow hold by icy plugin's call
		//new LMTRemoteAreaServer( true , null, null );
		new LMTRemoteAreaServer( true ,  null );
	}

}
