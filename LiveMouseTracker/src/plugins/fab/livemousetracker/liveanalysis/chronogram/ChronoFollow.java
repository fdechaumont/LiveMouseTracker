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
import plugins.fab.livemousetracker.detection.MouseDetection;


public class ChronoFollow extends ChronoFeatureAbstract implements ProcessAnimalPairFeature {

	public ChronoFollow() {
		super ( "Follow" , "Mouse A and B are not stopped. "
				+ "Mouse A is behind, in contact" , TimeLineDataType.BOOLEAN );
	}

	ChronoMouseSpeed chronoSpeedA = null;
	ChronoMouseSpeed chronoSpeedB = null;
	ChronoBehind chronoABehindB = null;
	ChronoContact chronoContactAandB = null;
//	ChronoSideBySide chronoASideBySideB = null;

	public void setChronoSpeedA(ChronoMouseSpeed chronoSpeedA) {
		this.chronoSpeedA = chronoSpeedA;
	}

	public void setChronoSpeedB(ChronoMouseSpeed chronoSpeedB) {
		this.chronoSpeedB = chronoSpeedB;
	}

	public void setChronoABehindB(ChronoBehind chronoABehindB) {
		this.chronoABehindB = chronoABehindB;
	}

//	public void setChronoASideBSameWay( ChronoSideBySide chronoASideBySideB) {
//		this.chronoASideBySideB = chronoASideBySideB;
//	}

	public void setChronoAContactB(ChronoContact chronoContactAandB ) {
		this.chronoContactAandB = chronoContactAandB;
	}

	public void process( Animal animalA , Animal animalB , Integer frameStart , Integer frameEnd )
	{

		if ( chronoSpeedA == null )
		{
			chronoSpeedA = new ChronoMouseSpeed();
			chronoSpeedA.process( animalA, frameStart , frameEnd );
		}
		EventTimeLine speedTimeLineA = chronoSpeedA.getEventTimeLine();

		if ( chronoSpeedB == null )
		{
			chronoSpeedB = new ChronoMouseSpeed();
			chronoSpeedB.process( animalB, frameStart, frameEnd );
		}
		EventTimeLine speedTimeLineB = chronoSpeedB.getEventTimeLine();

		/*
		if ( chronoABehindB == null )
		{
			chronoABehindB = new ChronoBehind();
			chronoABehindB.process( animalA, animalB, frameStart, frameEnd );
		}
		*/

		if ( chronoContactAandB == null )
		{
			chronoContactAandB = new ChronoContact();
			chronoContactAandB.process( animalA, animalB, frameStart, frameEnd );
		}

//		if ( chronoASideBySideB == null )
//		{
//			chronoASideBySideB = new ChronoSideBySide();
//			chronoASideBySideB.process( animalA, animalB, frameStart, frameEnd );
//		}

		eventTimeLine = new EventTimeLine( this.getName() , timeLineDataType );
		int tMax = Math.max( animalA.getLastTimePoint() , animalB.getLastTimePoint() );

		if( frameEnd == null ) frameEnd = tMax;
		if( frameStart == null ) frameStart = 0;

		for ( int t = frameStart ; t < frameEnd ; t++ )
		{
//			MouseDetection detectionAprev = animalA.getDetectionAt( t - ChronoConstant.FRAME_WINDOW );
//			MouseDetection detectionAnext = animalA.getDetectionAt( t + ChronoConstant.FRAME_WINDOW );
//
//			MouseDetection detectionBprev = animalB.getDetectionAt( t - ChronoConstant.FRAME_WINDOW );
//			MouseDetection detectionBnext = animalB.getDetectionAt( t + ChronoConstant.FRAME_WINDOW );

			Double detectionASpeed = speedTimeLineA.getValueAt( t );
			Double detectionBSpeed = speedTimeLineB.getValueAt( t );

			if (
//					detectionAprev != null &&
//					detectionAnext != null &&
//					detectionBprev != null &&
//					detectionBnext != null &&
					detectionASpeed != null &&
					detectionBSpeed != null
					)
			{

				if ( detectionASpeed > ChronoConstant.STOP_SPEED_THRESHOLD )
				if ( detectionBSpeed > ChronoConstant.STOP_SPEED_THRESHOLD )
				if ( chronoABehindB.getEventTimeLine().eventPresentAt( t )
//						||
//						chronoASideBySideB.getEventTimeLine().eventPresentAt( t )
						)
				if ( chronoContactAandB.getEventTimeLine().eventPresentAt( t ) )
				{
					eventTimeLine.addPunctualEvent( t );
				}

			}

		}

	}

}
