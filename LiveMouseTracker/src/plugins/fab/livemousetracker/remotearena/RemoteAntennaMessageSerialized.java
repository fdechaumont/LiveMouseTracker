package plugins.fab.livemousetracker.remotearena;


import java.io.Serializable;

public class RemoteAntennaMessageSerialized implements Serializable {

	private static final long serialVersionUID = 8533271169347676642L;

	SERVER_ANTENNA_MESSAGE serverAntennaMessage;
	CLIENT_ANTENNA_MESSAGE clientAntennaMessage;
	String identifier;
	String parameter;

	/** messages from server to client */
	public enum SERVER_ANTENNA_MESSAGE
	{
		SWITCH_OFF, SHUTDOWN, ENABLED_TRUE, ENABLED_FALSE
	}

	/** messages from client to server */
	public enum CLIENT_ANTENNA_MESSAGE
	{
		RFID_READ
	}

    public RemoteAntennaMessageSerialized( SERVER_ANTENNA_MESSAGE serverAntennaMessage, CLIENT_ANTENNA_MESSAGE clientAntennaMessage, String identifier, String parameter  ){
        this.serverAntennaMessage = serverAntennaMessage;
        this.identifier = identifier;
        this.parameter = parameter;
        this.clientAntennaMessage = clientAntennaMessage;
    }

    public SERVER_ANTENNA_MESSAGE getServerAntennaMessage() {
		return serverAntennaMessage;
	}

    public CLIENT_ANTENNA_MESSAGE getClientAntennaMessage() {
		return clientAntennaMessage;
	}

    public String getIdentifier() {
		return identifier;
	}

    public String getParameter() {
		return parameter;
	}

}
