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

import plugins.fab.livemousetracker.LiveMouseTracker;
import plugins.fab.livemousetracker.track.AnimalPool;
import plugins.fab.livemousetracker.track.TrackSegment;

public class MachineLearningTrackIdentityThread extends Thread {

	AnimalPool animalPool ;
	TrackSegment track;
	IdentityResult identityResult = null;

	public MachineLearningTrackIdentityThread( AnimalPool animalPool , TrackSegment track ) {

		this.animalPool = animalPool;
		this.track = track;

	}

	@Override
	public void run() {

		setPriority( LiveMouseTracker.SECONDARY_THREAD_PRIORITY );
		MachineLearningTrackIdentity mlti = new MachineLearningTrackIdentity( animalPool , true , track , true );
		if ( mlti.canBeProcessed() )
		{
			identityResult = mlti.findIdentity( );
		}

	}

	public IdentityResult getAnimalFound() {
		return identityResult;
	}

}
