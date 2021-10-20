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

import java.awt.Point;

import plugins.fab.livemousetracker.Animal;
import plugins.fab.livemousetracker.detection.MouseDetection;


public class ChronoOralOral extends ChronoFeatureAbstract implements ProcessAnimalPairFeature {

	public ChronoOralOral() {
		super ( "Oral-oral Contact" , "Animals' head are touching each other." , TimeLineDataType.BOOLEAN );
	}

	public void process( Animal animalA , Animal animalB , Integer frameStart, Integer frameEnd )
	{
//		description = animalA.getName() + " and " + animalB.getName() + " 's heads are touching";
		eventTimeLine = new EventTimeLine( this.getName() , timeLineDataType );

		int tMax = Math.max( animalA.getLastTimePoint() , animalB.getLastTimePoint() );

		if( frameEnd == null ) frameEnd = tMax;
		if( frameStart == null ) frameStart = 0;

		for ( int t = frameStart ; t < frameEnd ; t++ )
		{
			MouseDetection detectionA = animalA.getDetectionAt( t );
			MouseDetection detectionB = animalB.getDetectionAt( t );

			if ( detectionA != null && detectionB != null )
			{
				// TODO: double check this (should be sqrt(2) - diagonal proof )
				try
				{
					if ( detectionA.getFrontPoint().toPoint2D().distance(
							detectionB.getFrontPoint().toPoint2D() ) < ChronoConstant.MAX_DISTANCE_HEAD_HEAD_GENITAL_THRESHOLD )
					{
						eventTimeLine.addPunctualEvent( t );
					}
				}catch( NullPointerException e )
				{
					// FIXME: add a can't build event.
					//System.err.println("OralOral : can't process: missing front point in detection");
				}
			}
		}
	}

}
