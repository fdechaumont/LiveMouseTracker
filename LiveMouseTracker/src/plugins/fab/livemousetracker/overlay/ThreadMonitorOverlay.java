package plugins.fab.livemousetracker.overlay;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.HashSet;
import java.util.Set;

import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;


import icy.canvas.IcyCanvas;
import icy.painter.Overlay;
import icy.sequence.Sequence;
import icy.type.collection.CollectionUtil;

public class ThreadMonitorOverlay extends Overlay {

	public ThreadMonitorOverlay() {
		super("Thread monitor");
	}

	@Override
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas) {

		if ( g==null ) return;

		int active = Thread.activeCount() + 50;
		Thread[] threads = new Thread[active];
		Thread.enumerate(threads);

//		Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
		Set<Thread> threadSet = new HashSet<>(CollectionUtil.asList(threads));

		g.setColor( Color.black );

		/*
		OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
		double systemCPU = os.getSystemLoadAverage();

		OperatingSystemMXBean mbean = (com.sun.management.OperatingSystemMXBean)
		        ManagementFactory.getOperatingSystemMXBean();
		mbean.ge

		RuntimeMXBean r = ManagementFactory.getRuntimeMXBean();
*/
		OperatingSystemMXBean operatingSystemMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
		double cpuSysLoad = operatingSystemMXBean.getSystemCpuLoad();
		double cpuProcessLoad = operatingSystemMXBean.getProcessCpuLoad();

		int y = 15;
		g.drawString( "Sys load: "+ (int)(cpuSysLoad*100f) + " JVM load: " + (int)(cpuProcessLoad*100f) , -300, y );
		y+=15;

		int nbThreads = threadSet.size();
		g.setColor( Color.black );
		g.drawString( "Nb threads: "+nbThreads , -300, y );
		y+=15;
//		g.setColor( Color.green );
		for ( Thread thread : threadSet )
		{
			if (thread == null) continue;
			if ( thread.getState() != Thread.State.RUNNABLE )
			{
				continue;
			}

			g.drawString( ""+thread+ " / " + thread.getPriority() + " / "+ thread.getState()  , -300, y );
			y+=15;


		}

	}

}
