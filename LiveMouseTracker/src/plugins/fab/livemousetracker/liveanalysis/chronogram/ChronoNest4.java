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
import java.util.HashMap;

import plugins.fab.livemousetracker.Animal;
import plugins.fab.livemousetracker.Util;
import plugins.fab.livemousetracker.dataplayer.DetectAtT;
import plugins.fab.livemousetracker.dataplayer.MouseDetectionX;
import plugins.kernel.roi.roi2d.ROI2DArea;


public class ChronoNest4 extends ChronoFeatureAbstract implements ProcessAnimalGroupFeature {

	HashMap<Integer, DetectAtT> detectMap;
	HashMap<Integer, ROI2DArea> detectMergeMap = new HashMap<Integer, ROI2DArea>();

	public ChronoNest4() {
		super ( "Nest4" , "All mice are nesting. "
				+ "All mice are nesting" , TimeLineDataType.BOOLEAN );
	}

	@Override
	public void processGroup(ArrayList<Animal> allAnimalList, Integer frameStart, Integer frameEnd) {

		// get all animals

		eventTimeLine = new EventTimeLine( this.getName() , timeLineDataType );

		if ( allAnimalList.size() < 4 ) return;

		/*
		for ( int t = frameStart ; t < frameEnd ; t++ )
		{
			boolean isNest = false;
			int nbAnimal = 0;
			for ( Animal animal : allAnimalList )
			{
				if ( animal.getDetectionAt( t ) != null )
				{
					nbAnimal++;
				}
			}
			if ( nbAnimal == 0 )
			{
				// no animal is found, there is no occlusion in this experiment so they are all stop and not detected.
				isNest = true;
			}

			if ( !isNest )
			{
				boolean allInContact = true;
				System.out.println("*****");

				DetectAtT detectionAtT = detectMap.get( t );
				if ( detectionAtT != null )
				{
					ArrayList<MouseDetectionX> detectionList = detectionAtT.detection;

					for ( MouseDetectionX detection : detectionList )
					{
//						System.out.println( "test");
						if ( ! isInContact( detection , detectionList ) )
						{
							allInContact = false;
							//break;
						}
					}
					if ( allInContact )
					{
						isNest = true;
						System.out.println(" all in contact t = " + t );
					}
				}
			}


			// PROBLEMES:
			// le nest peut être
			// 1 gros animal
			// plein de petits en contact mais aussi avec des anonymes dedans.




			if ( isNest )
			{
				eventTimeLine.addPunctualEvent( t );
			}
		}*/

		for ( int t = frameStart ; t < frameEnd ; t++ )
		{
			// merge all detection
			DetectAtT detectionAtT = detectMap.get( t );
			if ( detectionAtT != null )
			{
				ArrayList<MouseDetectionX> detectionList = detectionAtT.detection;
				ROI2DArea mergedDetection = new ROI2DArea();
				for ( MouseDetectionX detection : detectionList )
				{
					mergedDetection.add( detection.mouseDetection.getROI2DArea().getBooleanMask( true ) );
				}

				// check if the mask is in one big piece.
				if ( mergedDetection.getBooleanMask( true ).getComponents().length == 1 )
				{
					// so they were all in contact
					// store it
					detectMergeMap.put ( t, mergedDetection );
				}
			}
		}

		//ChronoMouseSpeed chrono = new ChronoMouseSpeed();

		for ( int t = frameStart ; t < frameEnd ; t++ )
		{
			ROI2DArea detectionAprev = detectMergeMap.get( t - ChronoConstant.FRAME_WINDOW );
			ROI2DArea detectionAnext = detectMergeMap.get( t + ChronoConstant.FRAME_WINDOW );
			ROI2DArea detectionAcurrent = detectMergeMap.get( t );

			if( detectionAprev !=null && detectionAnext != null && detectionAcurrent != null  )
			{
				float distance = (float)Util.getMassCenter( detectionAprev ).distance( Util.getMassCenter( detectionAnext ) );
				float speed = distance / (float)( ChronoConstant.FRAME_WINDOW *2 +1 );
				//eventTimeLine.addValue( t , speed );
				if ( speed < ChronoConstant.STOP_SPEED_THRESHOLD )
				{
					eventTimeLine.addPunctualEvent( t );
//					System.out.println("NEST at " + t );
				}
			}

		}


//		for ( int t = frameStart ; t < frameEnd ; t++ )
//		{
//
//		}
	}


	private boolean isInContact( MouseDetectionX detection , ArrayList<MouseDetectionX> detectionList ) {

		for ( MouseDetectionX detection2 : detectionList )
		{
			if ( detection == detection2 ) continue;

			double minDistance = Util.getMinDistance( detection.mouseDetection , detection2.mouseDetection );
			System.out.println( minDistance );
			if ( minDistance < 2 )
			{
				return true;
			}
		}
		return false;

	}

	public void setDetectionTMap(HashMap<Integer, DetectAtT> detectMap) {
		this.detectMap = detectMap;
	}

	/*
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

		if ( chronoABehindB == null )
		{
			chronoABehindB = new ChronoBehind();
			chronoABehindB.process( animalA, animalB, frameStart, frameEnd );
		}

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
 */

}
