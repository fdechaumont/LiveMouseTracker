package plugins.fab.livemousetracker.perf;

import java.text.DecimalFormat;

public class TimeStep implements Comparable<TimeStep> {

	float duration;
	String name;

	public TimeStep( String name , float duration ) {
		this.name = name;
		this.duration = duration;
	}

	@Override
	public int compareTo(TimeStep otherTimeStep) {
		return (int)(this.duration - otherTimeStep.duration);
	}

	@Override
	public String toString() {
		DecimalFormat df = new DecimalFormat("#.##");
		return name + "\t" + df.format( this.duration ) + " ms";
	}

}
