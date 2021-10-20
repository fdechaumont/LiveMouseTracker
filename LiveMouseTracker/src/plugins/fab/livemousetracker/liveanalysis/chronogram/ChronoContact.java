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


public class ChronoContact extends ChronoFeatureAbstract implements ProcessAnimalPairFeature {

	public ChronoContact() {
		super ( "Contact" , "Animals are touching each other. (The mask of the two animals is in contact)" , TimeLineDataType.BOOLEAN );
	}

	ChronoDistanceBetweenAnimal chronoDistanceBetweenAnimals = null;

	public void setChronoDistanceBetweenAnimals(ChronoDistanceBetweenAnimal chronoDistanceBetweenAnimals) {
		this.chronoDistanceBetweenAnimals = chronoDistanceBetweenAnimals;
	}

	public void process( Animal animalA , Animal animalB , Integer frameStart, Integer frameEnd )
	{
//		description = animalA.getName() + " and " + animalB.getName() + " are in contact";
		// legacy of Label 2
		// Distance of mice < inferior to THRESHOLD_CONTACT

		eventTimeLine = new EventTimeLine( this.getName() , timeLineDataType );

		int tMax = Math.max( animalA.getLastTimePoint() , animalB.getLastTimePoint() );

		if( frameEnd == null ) frameEnd = tMax;
		if( frameStart == null ) frameStart = 0;

		if ( chronoDistanceBetweenAnimals == null )
		{
			chronoDistanceBetweenAnimals = new ChronoDistanceBetweenAnimal();
			chronoDistanceBetweenAnimals.process( animalA , animalB , frameStart, frameEnd );
		}

		for ( int t = frameStart ; t < frameEnd ; t++ )
		{
			Double distance = chronoDistanceBetweenAnimals.eventTimeLine.getValueAt( t );
			if ( distance != null )
			{
				if ( distance < 2 )
				{
					eventTimeLine.addPunctualEvent( t );
				}
			}

//			MouseDetection detectionA = animalA.getDetectionAt( t );
//			MouseDetection detectionB = animalB.getDetectionAt( t );
//
//			if ( detectionA != null && detectionB != null )
//			{
//				double minDistance = Float.MAX_VALUE;
//
//				for ( Point pA : detectionA.getROI2DArea().getBooleanMask( true ).getPoints() )
//				{
//					for ( Point pB : detectionB.getROI2DArea().getBooleanMask( true ).getPoints() )
//					{
//						double dist = pA.distance( pB );
//						if ( dist < minDistance )
//						{
//							minDistance = dist;
//						}
//					}
//				}
//
//				if ( minDistance < 2 ) // TODO: double check this (should be sqrt(2) - diagonal proof )
//				{
//					eventTimeLine.addPunctualEvent( t );
//				}
//			}
		}
	}

}
