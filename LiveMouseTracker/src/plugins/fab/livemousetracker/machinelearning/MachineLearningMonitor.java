/**
  	@author Fabrice de Chaumont
 	copyright Fabrice de Chaumont @ Institut Pasteur

 	This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package plugins.fab.livemousetracker.machinelearning;

import java.awt.Graphics2D;

import icy.canvas.IcyCanvas;
import icy.painter.Overlay;
import icy.sequence.Sequence;
import icy.system.thread.Processor;
import plugins.fab.livemousetracker.LiveMouseTracker;

/** display machine learning info/state
 * confusion matrix/ out of bag error and others.
 *  */
public class MachineLearningMonitor extends Overlay {

	String info = "";
	Monitor monitor;

	public MachineLearningMonitor() {
		super("Machine Learning Monitor");
		monitor = new Monitor();
		Processor p = new Processor( 1 );
		p.setThreadName("ML Monitor");
		p.execute( monitor );
	}

	@Override
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas) {

		String strList[] = info.split( "\n" );
		for ( int i = 0 ; i < strList.length ; i++ )
		{
			g.drawString( strList[i], 520, i *10 );
		}
	}

	class Monitor implements Runnable
	{
		@Override
		public void run() {

			while ( !Thread.interrupted() )
			{
				try {
					Thread.sleep( 1000 );
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				MachineLearningSetBuilder setBuilder = new MachineLearningSetBuilder();

				setBuilder.buildSet( LiveMouseTracker.getMainAnimalPool() ,
						LiveMouseTracker.getMainAnimalPool().getAnimalList()
						);

				info = setBuilder.evaluate( null, false );
			}
			System.out.println("Monitor exit.");

		}

	}


}
