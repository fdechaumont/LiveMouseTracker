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

import java.util.ArrayList;

import plugins.fab.livemousetracker.Animal;
import plugins.fab.livemousetracker.detection.MouseDetection;


public class ChronoApproach extends ChronoFeatureAbstract implements ProcessAnimalPairFeature {

	public ChronoApproach() {
		super ( "Approach" , "Mouse A Speed > Mouse B Speed & Mouse A getting to B" , TimeLineDataType.BOOLEAN );
	}

	ChronoMouseSpeed chronoSpeedA = null;
	ChronoMouseSpeed chronoSpeedB = null;

	public void setChronoSpeedA(ChronoMouseSpeed chronoSpeedA) {
		this.chronoSpeedA = chronoSpeedA;
	}

	public void setChronoSpeedB(ChronoMouseSpeed chronoSpeedB) {
		this.chronoSpeedB = chronoSpeedB;
	}

	public void process( Animal animalA , Animal animalB , Integer frameStart, Integer frameEnd )
	{
//		description = animalA.getName() + "'s speed > " + animalB.getName() + " and " + animalA.getName() + " go to " + animalB.getName();

		eventTimeLine = new EventTimeLine( this.getName() , timeLineDataType );
		int tMax = Math.max( animalA.getLastTimePoint() , animalB.getLastTimePoint() );

		if( frameEnd == null ) frameEnd = tMax;
		if( frameStart == null ) frameStart = 0;

		if ( chronoSpeedA == null )
		{
			chronoSpeedA = new ChronoMouseSpeed();
			chronoSpeedA.process( animalA , frameStart , frameEnd );
		}
		EventTimeLine speedTimeLineA = chronoSpeedA.getEventTimeLine();

		if ( chronoSpeedB == null )
		{
			chronoSpeedB = new ChronoMouseSpeed();
			chronoSpeedB.process( animalB , frameStart, frameEnd );
		}
		EventTimeLine speedTimeLineB = chronoSpeedB.getEventTimeLine();

		for ( int t = frameStart ; t < frameEnd ; t++ )
		{
			MouseDetection detectionAprev = animalA.getDetectionAt( t - ChronoConstant.FRAME_WINDOW );
			MouseDetection detectionAnext = animalA.getDetectionAt( t + ChronoConstant.FRAME_WINDOW );

			MouseDetection detectionBprev = animalB.getDetectionAt( t - ChronoConstant.FRAME_WINDOW );
			MouseDetection detectionBnext = animalB.getDetectionAt( t + ChronoConstant.FRAME_WINDOW );

			Double detectionASpeed = speedTimeLineA.getValueAt( t );
			Double detectionBSpeed = speedTimeLineB.getValueAt( t );

			if (
					detectionAprev != null &&
					detectionAnext != null &&
					detectionBprev != null &&
					detectionBnext != null &&
					detectionASpeed != null &&
					detectionBSpeed != null
					)
			{
				double distanceTPrev = detectionAprev.getMassCenter().toPoint2D().distance( detectionBprev.getMassCenter().toPoint2D() );
				double distanceTNext = detectionAnext.getMassCenter().toPoint2D().distance( detectionBnext.getMassCenter().toPoint2D() );

				if ( detectionASpeed > detectionBSpeed )
				{
					if ( distanceTPrev > distanceTNext )
					{
						eventTimeLine.addPunctualEvent( t );
					}
				}
			}

		}

	}


}
