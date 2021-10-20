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


public class ChronoDistanceBetweenAnimal extends ChronoFeatureAbstract implements ProcessAnimalPairFeature {

	public ChronoDistanceBetweenAnimal() {
		super ( "Distance between animals" , "Minimal distance between animals (using mask)" , TimeLineDataType.ANALOGIC );
	}

	public void process( Animal animalA , Animal animalB , Integer frameStart , Integer frameEnd )
	{
//		description = "Min distance between "+ animalA.getName() + " and " + animalB.getName();
		eventTimeLine = new EventTimeLine( this.getName() , timeLineDataType );
		int tMax = Math.max( animalA.getLastTimePoint() , animalB.getLastTimePoint() );

		if( frameEnd == null ) frameEnd = tMax;
		if( frameStart == null ) frameStart = 0;

		for ( int t = frameStart ; t < frameEnd ; t++ )
		{
			MouseDetection detectionA = animalA.getDetectionAt( t );
			MouseDetection detectionB = animalB.getDetectionAt( t );
//			System.out.println( "dist calculation: " + t );
			if ( detectionA != null && detectionB != null )
			{
				double minDistance = Float.MAX_VALUE;

				Point[] detectionAPointList = detectionA.getBooleanMask().getContourPoints();
				Point[] detectionBPointList = detectionB.getBooleanMask().getContourPoints();

//				System.out.println( "set A: " + detectionAPointList.length );
//				System.out.println( "set B: " + detectionBPointList.length );

				for ( Point pA : detectionAPointList )
				{
					for ( Point pB : detectionBPointList )
					{
						double dist = pA.distanceSq( pB );
						if ( dist < minDistance )
						{
							minDistance = dist;
						}
					}
				}
				minDistance = Math.sqrt( minDistance );
				eventTimeLine.addValue( t, minDistance );
			}
		}
	}




}
