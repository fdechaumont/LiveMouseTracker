package plugins.fab.livemousetracker.remotearena.server;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import plugins.fab.livemousetracker.DrawUtil;
import plugins.fab.livemousetracker.LiveMouseTracker;
import plugins.fab.livemousetracker.remotearena.RemoteAntennaMessageSerialized;
import plugins.fab.livemousetracker.remotearena.RemoteArenaInfo;
import plugins.fab.livemousetracker.remotearena.RemoteSystemMessageSerialized;
import plugins.fab.livemousetracker.remotearena.RemoteAntennaMessageSerialized.SERVER_ANTENNA_MESSAGE;
import plugins.fab.livemousetracker.remotearena.RemoteSystemMessageSerialized.CLIENT_MESSAGE_TYPE;
import plugins.fab.livemousetracker.rfid.Antenna;
import plugins.fab.livemousetracker.rfid.AntennaReadEvent;
import plugins.fab.livemousetracker.rfid.AntennaReadListener;

public class RemoteAntenna implements Antenna {

	Point2D location;
	float ray;
	String name;
	ArrayList<AntennaReadListener> antennaReaderListenerList = new ArrayList<AntennaReadListener>();
	boolean faulty = false;
	boolean enabled = false;
	String identifier;
	//RemoteArenaInfo remoteArenaInfo;
	//LMTRemoteAreaServer server;
	RegisteredArenaClient registeredArenaClient;

	public RemoteAntenna(
			RemoteArenaInfo remoteSystemInfo, Point2D location, float ray, String identifier ,
			//LMTRemoteAreaServer server
			RegisteredArenaClient registeredArenaClient
			) {
		Point2D loc = new Point2D.Double(
				//remoteSystemInfo.localization.getX() - remoteSystemInfo.cropRectangle.x + location.getX(),
				//remoteSystemInfo.localization.getY() - remoteSystemInfo.cropRectangle.y + location.getY() );
				remoteSystemInfo.localization.getX() + location.getX(),
				remoteSystemInfo.localization.getY() + location.getY() );
		this.location = loc;

		this.ray = ray;
		this.identifier = identifier;
		//this.remoteArenaInfo = remoteSystemInfo;

		this.registeredArenaClient = registeredArenaClient;
//		this.server = server;
	}


	@Override
	public void switchOff() {

		registeredArenaClient.sendAntennaMessageToClient(
				new RemoteAntennaMessageSerialized(
						SERVER_ANTENNA_MESSAGE.SWITCH_OFF, null, this.identifier, null
						) );
	}

	@Override
	public void shutdown() {

		registeredArenaClient.sendAntennaMessageToClient(
				new RemoteAntennaMessageSerialized(
						SERVER_ANTENNA_MESSAGE.SHUTDOWN, null, this.identifier, null
						) );
	}

	@Override
	public boolean isFaulty() {
		return faulty;
	}

	@Override
	public Point2D getLocation() {
		return location;
	}

	@Override
	public float getRay() {
		return ray;
	}

	@Override
	public void setEnabled(boolean enabled) {

		SERVER_ANTENNA_MESSAGE message ;
		if ( enabled )
		{
			message = SERVER_ANTENNA_MESSAGE.ENABLED_TRUE;
		}else
		{
			message = SERVER_ANTENNA_MESSAGE.ENABLED_FALSE;
		}
		registeredArenaClient.sendAntennaMessageToClient(
				new RemoteAntennaMessageSerialized(
						message, null, this.identifier, null
						) );

		this.enabled = enabled;
	}

	@Override
	public double readFrequency() {
		// TODO
		return -1;
	}

	public void addRFIDAntennaListener( AntennaReadListener rfidAntennaListener )
	{
		synchronized ( antennaReaderListenerList ) {
			antennaReaderListenerList.add( rfidAntennaListener );
		}
	}

	public void fireRFIDEvent( String rfid )
	{
		AntennaReadEvent event = new AntennaReadEvent(
				LiveMouseTracker.getT(),
				LiveMouseTracker.RFID_DEFAULT_LATENCY,
				rfid, this.location , ray, this );

		fireRFIDEvent( event );
	}

	public void fireRFIDEvent( AntennaReadEvent rfidEvent )
	{
		synchronized ( antennaReaderListenerList ) {
			for ( AntennaReadListener listener : antennaReaderListenerList )
			{
				listener.antennaEvent( rfidEvent );
			}
		}
	}

	public void paint(Graphics2D g) {

		/*
		AlphaComposite alcom = AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, 1 );
        g.setComposite(alcom);
		*/
		//g.setRenderingHint(hintKey, hintValue);
		if ( enabled )
		{
			g.setColor( Color.green.darker() );
		}else
		{
			g.setColor( Color.gray );
		}
		if ( faulty )
		{
			g.setColor( Color.red.darker() );
		}


		g.setStroke( new BasicStroke( 2 ) ) ;
		Ellipse2D ellipse2D = new Ellipse2D.Double(
				location.getX()-ray, location.getY()-ray,
				ray*2+1, ray*2+1 );
		g.draw( ellipse2D );


		g.setColor( Color.yellow );

		String additionalDescription="";
		if ( enabled )
		{
			additionalDescription += " enabled";
		}
		DrawUtil.drawCenteredString( g, "remote"+additionalDescription, (int)location.getX(), (int)location.getY()-10 );

		String[] identifiers = identifier.split(" ");
		int offsetY = 0;
		for ( String s : identifiers )
		{
			offsetY+=10;
			DrawUtil.drawCenteredString( g, s, (int)location.getX(), (int)location.getY()+offsetY );
		}
/*
		DrawUtil.drawCenteredString( g, comPort, (int)location.getX(), (int)location.getY()-10 );
		DrawUtil.drawCenteredString( g, ""+onOffstate, (int)location.getX(), (int)location.getY() );

		DrawUtil.drawCenteredString( g, ""+nbTryRead+"/" + nbEvent , (int)location.getX(), (int)location.getY()+10 );
*/
	}


	@Override
	public String getIdentifier() {
		return identifier;
	}

}
