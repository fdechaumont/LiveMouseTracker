package plugins.fab.livemousetracker.device.ttl;

import plugins.fab.livemousetracker.experiment.EventLog;

public class TTLReceivedEvent {

	int startFrame;
	int endFrame;

	String name;

	public TTLReceivedEvent( String name ) {
		this.name = name;
	}

	public EventLog toEventLog() {

		return new EventLog( name , null ,startFrame, endFrame,"" );

	}

	@Override
	public String toString() {

		return "TTL Received event " + name + " startFrame: " + startFrame + " endFrame:" + endFrame;
	}



}
