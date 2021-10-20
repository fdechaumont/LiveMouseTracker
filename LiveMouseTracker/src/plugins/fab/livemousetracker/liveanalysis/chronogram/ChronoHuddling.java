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
package plugins.fab.livemousetracker.liveanalysis.chronogram;

import plugins.fab.livemousetracker.Animal;
import plugins.fab.livemousetracker.LiveMouseTracker;
import plugins.fab.livemousetracker.detection.MouseDetection;
import plugins.fab.livemousetracker.morpho.Moment;


public class ChronoHuddling extends ChronoFeatureAbstract implements ProcessAnimalSingleFeature {

	public ChronoHuddling() {
		super ( "Huddling" , "" , TimeLineDataType.BOOLEAN );
	}

	@Override
	public void process(Animal animalA, Integer frameStart, Integer frameEnd) {

		// get all animals

		eventTimeLine = new EventTimeLine( this.getName() , timeLineDataType );

		for ( int t = frameStart ; t < frameEnd ; t++ )
		{

			MouseDetection detection = animalA.getDetectionAt( t );

			Moment moment = new Moment( detection.getROI2DArea().getBooleanMask( true ) , LiveMouseTracker.infraImage );
			if ( moment.aoipar.circularity >0.75d )
			{
				eventTimeLine.addPunctualEvent( t );
			}

		}


	}




}
