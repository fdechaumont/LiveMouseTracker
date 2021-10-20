package plugins.fab.livemousetracker.remotearena;


import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;

import plugins.fab.livemousetracker.remotearena.RemoteSystemMessageSerialized.CLIENT_MESSAGE_TYPE;
import plugins.fab.livemousetracker.remotearena.client.RemoteAntennaDescriptor;
import plugins.fab.livemousetracker.rfid.Antenna;

public class RemoteSystemMessageSerialized implements Serializable {

	private static final long serialVersionUID = 5436486930959326313L;

	Point2D localAntennaLocation;
	RemoteArenaInfo remoteArenaInfo;
	String rfidRead;
	SERVER_REQUEST serverRequest = null;
	CLIENT_MESSAGE_TYPE clientMessageType = null;

	public enum SERVER_REQUEST
	{
		ASK_INIT_INFO
	}

	public enum CLIENT_MESSAGE_TYPE
	{
		INIT_INFO
	}

    public RemoteSystemMessageSerialized( SERVER_REQUEST serverRequest, CLIENT_MESSAGE_TYPE clientReturn,
    		Point2D localAntennaLocation , RemoteArenaInfo remoteSetupInfo , String RFIDread, String message  ){
        this.localAntennaLocation = localAntennaLocation;
        this.remoteArenaInfo = remoteSetupInfo;
        this.message = message;
        this.serverRequest = serverRequest;
        this.clientMessageType = clientReturn;
    }

    public SERVER_REQUEST getServerRequest() {
		return serverRequest;
	}

    public Point2D getGlobalAntennaLocation()
    {
    	Point2D globalLocation = new Point2D.Double(
    			remoteArenaInfo.localization.getX()+localAntennaLocation.getX(),
    			remoteArenaInfo.localization.getY()+localAntennaLocation.getY()
    			);
    	return globalLocation;
    }

    public String getRFIDread()
    {
    	return rfidRead;
    }

    public RemoteArenaInfo getRemoteArenaInfo() {
		return remoteArenaInfo;
	}

    String message;
	public String getMessage() {
		return message;
	}

	public void setMessage(String string) {
		this.message = string;
	}

	ArrayList<RemoteAntennaDescriptor> remoteAntennaDescriptorList = new ArrayList<>();

	public void addRemoteAntennaDescriptor(Antenna antenna) {

		RemoteAntennaDescriptor rad = new RemoteAntennaDescriptor( antenna.getLocation(), antenna.getRay(), antenna.getIdentifier() );
		System.out.println("Adding remote antenna descriptor: " + antenna.getIdentifier() );
		remoteAntennaDescriptorList.add( rad );
	}

	public CLIENT_MESSAGE_TYPE getClientMessageType() {
		return clientMessageType;
	}

	public ArrayList<RemoteAntennaDescriptor> getRemoteAntennaDescriptorList() {
		return remoteAntennaDescriptorList;
	}


}
