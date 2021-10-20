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

import icy.roi.BooleanMask2D;
import plugins.fab.livemousetracker.Animal;
import plugins.fab.livemousetracker.Util;
import plugins.fab.livemousetracker.dataplayer.DetectAtT;
import plugins.fab.livemousetracker.dataplayer.MouseDetectionX;
import plugins.kernel.roi.roi2d.ROI2DArea;


public class ChronoNest3 extends ChronoFeatureAbstract implements ProcessAnimalGroupFeature {

	HashMap<Integer, DetectAtT> detectMap;
	HashMap<Integer, ROI2DArea> detectMergeMap = new HashMap<Integer, ROI2DArea>();

	public ChronoNest3() {
		super ( "Nest3" , "All mice are nesting but 1"
				+ "All mice are nesting but 1" , TimeLineDataType.BOOLEAN );
	}

	public Animal animalToCheck;

	@Override
	public void processGroup(ArrayList<Animal> allAnimalList, Integer frameStart, Integer frameEnd) {


		if ( allAnimalList.size() < 3 ) return;

		// get all animals

		eventTimeLine = new EventTimeLine( this.getName() , timeLineDataType );

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
				BooleanMask2D[] components = mergedDetection.getBooleanMask( true ).getComponents();
				if ( components.length == 2 )
				{
					// so there is 2 groups
					// store the biggest
					BooleanMask2D biggest = null;

					for ( BooleanMask2D mask : components )
					{
						if ( biggest == null )
						{
							biggest = mask;
							continue;
						}
						if ( mask.getNumberOfPoints() > biggest.getNumberOfPoints() )
						{
							biggest = mask;
						}
					}

					detectMergeMap.put ( t, new ROI2DArea( biggest ) );

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
					//eventTimeLine.addPunctualEvent( t );

					// check which is not in nest.

//					for ( Animal animal : allAnimalList )
//					{
//					System.out.println("NEST 3 step 1");
//					System.out.println( animalToCheck.getDetectionAt( t ) );
						try
						{
							if ( !detectionAcurrent.contains( animalToCheck.getDetectionAt( t ).getROI2DArea() ) )
							{
//								System.out.println("NEST 3 step 2");
								eventTimeLine.addPunctualEvent( t );
							}
						}catch( NullPointerException e )
						{
							// detection does not exist
						}
//					}

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
