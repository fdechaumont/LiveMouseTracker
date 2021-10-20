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
package plugins.fab.livemousetracker;

import java.util.ArrayList;

public class AnimalInstancesSetBuilderManager extends Thread {

	ArrayList<AnimalInstancesSetBuilderThread> threadList = new ArrayList<AnimalInstancesSetBuilderThread>();

	@Override
	public void run() {

		while ( true )
		{
			// Launch build of all animals.
			for ( Animal animal: LiveMouseTracker.getMainAnimalPool().getAnimalList() )
			{
				//if ( !animal.isCacheOfInstancesUpToDate() )
				threadList.add( new AnimalInstancesSetBuilderThread(animal) );
			}

			// start threads
			for( AnimalInstancesSetBuilderThread aisbt : threadList )
			{
				aisbt.setPriority( LiveMouseTracker.SECONDARY_THREAD_PRIORITY );
				aisbt.start();
			}

			// wait for all animal to be computed
			for( AnimalInstancesSetBuilderThread aisbt : threadList )
			{
				try {
					aisbt.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			threadList.clear();
			System.out.println("Animal instances updated.");

			try {
				Thread.sleep( 1000 );
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}



	}

}
