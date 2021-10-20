package plugins.fab.livemousetracker.perf;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import icy.system.profile.Chronometer;
import plugins.fab.livemousetracker.LiveMouseTracker;

/**
 * Dedicated to find where the program is lagging.
 * @author Fab
 *
 */

public class PerformanceMonitor {

	String name;
	long startTimeInNs;
	ArrayList<TimeStep> timeStepList = new ArrayList<>();
	long lastTimeInNs;
	long endTimeInNs;
	int step = 0;

	public PerformanceMonitor( String name ) {
		this.name = name;
		this.start();
	}

	public void start()
	{
		this.startTimeInNs = System.nanoTime();
		this.lastTimeInNs = System.nanoTime();
	}

	public void stepDone( String name )
	{
		long currentTime = System.nanoTime();
		float duration = ( currentTime - lastTimeInNs) / 1000000f;
		this.lastTimeInNs = currentTime;
		step++;
		this.timeStepList.add( new TimeStep( ""+step + " : " + name, duration ) );
	}

	public void finish()
	{
		this.endTimeInNs = System.nanoTime();
	}

	public float getTotalDurationMs()
	{
		float durationMs = ( endTimeInNs - startTimeInNs ) / 1000000f;
		System.out.println("Total duration = " + durationMs );
		return durationMs;
	}

	public float getMs()
	{
		float total = ( System.nanoTime() - startTimeInNs ) / 1000000f;
		return total;
	}



	public void printReport()
	{
		DecimalFormat df = new DecimalFormat("#.##");
		System.out.println("=== Performance monitor for " + this.name + " Total: " + getTotalDurationMs() + " ms" );

		// build map
		Map<Float, TimeStep> timeMap = new HashMap<>();
		float maxDuration = 0;
		for ( TimeStep timeStep : timeStepList )
		{
			timeMap.put( timeStep.duration, timeStep );
			if ( timeStep.duration > maxDuration )
			{
				maxDuration = timeStep.duration;
			}
		}
		TreeMap<Float, TimeStep> timeTreeMap = new TreeMap<>( timeMap );

		float cumul=0;
		boolean overPassed = false;
		for ( TimeStep timeStep : timeStepList )
		{
			cumul +=timeStep.duration;

			String over = "";
			if ( !overPassed )
			{
				if ( cumul > 30 )
				{
					over = "\t --------------- OVER COMPUTATION";
					overPassed = true;
					LiveMouseTracker.nbOver ++;
				}
			}

			String maxCost = "";
			if ( timeStep.duration == maxDuration )
			{
				maxCost = "\t -MAX- " + df.format( timeStep.duration ) + " ms";
			}

			System.out.println( timeStep + " \t cum:" + df.format( cumul ) + " ms" + over + maxCost );
			//System.out.println( timeStep + " \t cum:" + cumul + " ms"  );
		}

		/*
		for ( Float f: timeTreeMap.keySet() )
		{
			cumul +=f;
			System.out.println( timeTreeMap.get( f ) + " \t" + cumul );
		}*/

	}

}
