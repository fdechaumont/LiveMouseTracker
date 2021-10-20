package plugins.fab.livemousetracker.remotearena.client;

import java.util.ArrayList;

import icy.common.listener.AcceptListener;
import icy.main.Icy;
import plugins.fab.livemousetracker.rfid.RFIDAntenna;
import plugins.fab.livemousetracker.rfid.AntennaReadListener;
import plugins.fab.livemousetracker.remotearena.RemoteAntennaMessageSerialized;
import plugins.fab.livemousetracker.remotearena.RemoteAntennaMessageSerialized.CLIENT_ANTENNA_MESSAGE;
import plugins.fab.livemousetracker.rfid.Antenna;
import plugins.fab.livemousetracker.rfid.AntennaReadEvent;

/**
 *
 * @author Fab
 *
 */
public class RemoteClientRFIDManager implements AntennaReadListener, AcceptListener {

	ArrayList<RFIDAntenna> antennaList = new ArrayList<RFIDAntenna>();
	ArrayList<AntennaReadEvent> eventList = new ArrayList<AntennaReadEvent>();
	MultiArenaClient multiArenaClient = null;

	public RemoteClientRFIDManager(MultiArenaClient multiArenaClient) {
		this.multiArenaClient = multiArenaClient;
		Icy.getMainInterface().addCanExitListener( this );
	}

	public void addAntenna( RFIDAntenna rfidAntenna )
	{
		synchronized ( antennaList) {
			antennaList.add( rfidAntenna );
		}
		rfidAntenna.addRFIDAntennaListener( this );
	}

	@Override
	public boolean accept(Object source) {
		for ( RFIDAntenna antenna : antennaList )
		{
			antenna.shutdown();
		}
		return true;
	}

	@Override
	public void antennaEvent(AntennaReadEvent rfidEvent) {

		RemoteAntennaMessageSerialized message = new RemoteAntennaMessageSerialized(
				null, CLIENT_ANTENNA_MESSAGE.RFID_READ, rfidEvent.getAntenna().getIdentifier() , rfidEvent.getRFID() );

		multiArenaClient.sendMessageToServer( message );

	}

	public ArrayList<RFIDAntenna> getAntennaList() {
		return antennaList;
	}

	public void shutDownAll() {
		for ( Antenna antenna : antennaList )
		{
			antenna.shutdown();
		}
		
		// clean
		
		antennaList.clear();
		eventList.clear();
		multiArenaClient = null;

	}


}
