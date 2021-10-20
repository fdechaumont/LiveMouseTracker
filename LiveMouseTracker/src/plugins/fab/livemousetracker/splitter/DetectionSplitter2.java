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

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.util.ArrayList;

import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.roi.ROIUtil;
import plugins.fab.livemousetracker.detection.MouseDetection;
import plugins.fab.livemousetracker.detection.MouseDetector;
import plugins.fab.livemousetracker.morpho.MorphoROITools;
import plugins.fab.livemousetracker.track.TrackSegment;
import plugins.kernel.roi.roi2d.ROI2DArea;

/**
 * New version of detection splitter.
 *
 */
public class DetectionSplitter2 {

	/**
	 * try to find how many animals are involved in this rawDetection, and then split them.
	 *
	 * idea to improve: mean shift very sub track of objects.
	 *
	 * @param rawMouseDetection
	 */
	public ArrayList<MouseDetection> splitDetectionWithSeed( ROI2DArea tooBigROI , IcyBufferedImage difDepthImage ) {

		if ( LiveMouseTracker.LOG_SPLIT )
		{
			System.out.println("[SPLIT] t:" + LiveMouseTracker.getT() );
		}
		ArrayList<MouseDetection> detectionCreated = new ArrayList<MouseDetection>();

		// find the number of animal involved considering the tracking.

		ArrayList<TrackSegment> tsFinishingAtTList = getAllTrackSegmentFinishingAt( LiveMouseTracker.getT()-1 );

		if ( LiveMouseTracker.LOG_SPLIT )
		{
			System.out.println("[SPLIT] nbtsFinishingAtTList: " + tsFinishingAtTList.size() );
		}

		ArrayList<TrackSegment> tsListCandidateInROI = new ArrayList<TrackSegment>();

		for ( TrackSegment ts : tsFinishingAtTList )
		{
			if ( LiveMouseTracker.LOG_SPLIT )
			{
				System.out.println("[SPLIT] detection point tested in big ROI: "+ ts.getDetection( ts.getLastTimePoint() ).getMassCenter().toPoint2D() );
			}

			if (
					tooBigROI.contains(
					ts.getDetection( ts.getLastTimePoint() ).getMassCenter().toPoint2D() ) )
			{
				tsListCandidateInROI.add( ts );
			}
		}

		int nbObject = tsListCandidateInROI.size() ;
		tooBigROI.setName( "tmp split " + nbObject );

		if ( LiveMouseTracker.LOG_SPLIT )
		{
			System.out.println("[SPLIT] number of objects: " + nbObject );
		}

		if ( nbObject <= 1 )
		{
			// We could say this is a good detection
			if ( LiveMouseTracker.LOG_SPLIT )
			{
				System.out.println("[SPLIT] number of object <= 1");
			}

			if( LiveMouseTracker.ACCEPT_TOO_BIG_DETECTION_IF_ONLY_ONE_TRACK_CONCURRENCY_IN_SPLIT )
			{
//				tooBigROI.setName("seg ok");
				ROI2DAreaX roi2DAreaX = MouseDetector.createROI2DAreaX( tooBigROI.getBooleanMask( true ) );
				roi2DAreaX.setName("seg ok");
				// check if ROI name is ok
				detectionCreated.add( new MouseDetection( roi2DAreaX, LiveMouseTracker.getT() ) );
			}
			//System.out.println(""+ nbObject + " object: can't split");
			return detectionCreated;
		}

		//System.out.println("splitting in " + nbObject + " objects");

		// fill using dif depth map infos.

		ArrayList<SplitObject> previousDetectionSplitList = new ArrayList<SplitObject>();
		for ( TrackSegment ts: tsListCandidateInROI ) // Build the split list based on previous detection list.
		{
			previousDetectionSplitList.add( new SplitObject( ts.getDetection( ts.getLastTimePoint() ) ) );
		}

		//IcyBufferedImage difDepthImage = difDepthInTimeSequence.getImage( 0 , 0 );

		if ( false )
		{
			double sumPower = 0;
			for ( SplitObject splitObject : previousDetectionSplitList )
			{
				// find the "moving power" of each split object
				ROI2DArea roi = splitObject.detection.getROI2DArea();
				Point[] pointArray = roi.getBooleanMask( true ).getPoints();
				double movingPower = 0;
				for ( int i = 0 ; i < pointArray.length ; i++ )
				{
					double val = Math.abs( difDepthImage.getData( pointArray[i].x , pointArray[i].y , 0 ) );
					//if ( val < 0 ) movingPower+= -val;
					if ( val > 5 ) movingPower+= Math.abs( val );
				}
				//movingPower/= (double) pointArray.length; // normalize power by surface
				splitObject.movingPower = movingPower;
				sumPower += movingPower;
			}

			// normalize power.
			for ( SplitObject splitObject : previousDetectionSplitList )
			{
				splitObject.movingPower/=sumPower; // Sum of all moving power = 1
				//System.out.println("Moving power %: " + splitObject.movingPower );
			}
		}


		// init ROIs
		for ( SplitObject splitObject : previousDetectionSplitList )
		{
			splitObject.newROI2DArea = (ROI2DArea) splitObject.detection.getROI2DArea().getCopy();
		}


		{
			// New algorithm
			// Try to find the masscenter point of animal and try to go along the animal by a minimal cost of gradient.
			// 1. Find mass center
			// 2. Find it's z
			// 3. Expand
			//	- with a regularity cost
			//	- grab head if found ?
			//	- until a volume is reached ?

			// IDEA: TRY TO START THE WATERSHED FOR DIFFERENT INIT VALUE DEPENDING ON ANIMALS HEIGHT


//			for ( SplitObject splitObject : previousDetectionSplitList )
//			{
//				ROI2DArea roi = new ROI2DArea( splitObject.detection.getMassCenter() );
//				roi.setName("tmp center of mass");
//				addROIToInfraSequence( roi );
//			}

			// Init seed
//			System.out.println("[SPLIT] New object");
			for ( SplitObject splitObject : previousDetectionSplitList )
			{
				splitObject.newROI2DArea = new ROI2DArea( );

				// Add the mass center as a start for split.
				/*
				Point2D p = splitObject.detection.getMassCenter().toPoint2D();
				splitObject.newROI2DArea.addPoint( (int)p.getX() , (int)p.getY() );
				*/

				{
					/** Try to use the spine points as a start for the split, and use
					 * the mass center is not possible.
					 * */

					boolean init = false;
					try
					{
						for ( Point2D point : splitObject.detection.getSpinePointsForSplit() )
						{
							//					System.out.println("[SPLIT] Adding points.");

							splitObject.newROI2DArea.addPoint( (int)point.getX() , (int)point.getY() );
							init = true;
						}
					}catch ( NullPointerException e )
					{
						// FIXME: this should not append.
						System.err.println("[FIXME] Spine point for split: null (no a and b point in detection)");
					}
					if ( !init )
					{
						Point2D p = splitObject.detection.getMassCenter().toPoint2D();
						splitObject.newROI2DArea.addPoint( (int)p.getX() , (int)p.getY() );
						init = true;
					}
				}

//				Icy.getMainInterface().getActiveSequence().addROI( splitObject.newROI2DArea );
//				splitObject.newROI2DArea.setName("roi split init");
//				splitObject.newROI2DArea.setColor( Color.yellow );

			}

			DecimalFormat df = new DecimalFormat("000");

			int stepZ = 2;
//			for ( int z = 100 ; z > 20 ; z-=stepZ )
			for ( int i = 0 ; i < 40 ; i++ )
			//int i = 0;
			{

//				shuffle ( previousDetectionSplitList );
//				for ( int i = 0 ; i < stepZ ; i++ )
				{
/*
					for ( SplitObject splitObject : previousDetectionSplitList )
					{
						double currentVolume =
								LiveMouseTracker.getBackgroundHeightMapBuider().getVolume( LiveMouseTracker.depthImage,
										splitObject.newROI2DArea.getBooleanMask( true ) ) ;

						//double targetVolume = 31500;
						//double targetVolume = 60500;
						double targetVolume = LiveMouseTracker.DETECTION_SPLIT_TARGET_VOLUME;

						//splitObject.detection.getVolume()
						// if ( currentVolume < targetVolume )
						{
							//withZ;
//							splitObject.newROI2DArea = MorphoROITools.dilateROIWithZConstraint( splitObject.newROI2DArea, 1, 1, z ); //z ?

							splitObject.newROI2DArea = MorphoROITools.dilateROIWithZConstraint( splitObject.newROI2DArea, 1, 1, 1 ); //z ?


//							splitObject.newROI2DArea.setName("tmp " + df.format( z ) );
//							LiveMouseTracker.addROIToInfraSequence( splitObject.newROI2DArea );
						}

					}*/

					for ( SplitObject splitObject : previousDetectionSplitList )
					{
						splitObject.setBeforeProcessROI();
					}

					//for ( int z = 100 ; z > 01 ; z-=stepZ ) // first dilate high parts of the animal, and then bottom parts.
					int z = 1;
					{
						for ( SplitObject splitObject : previousDetectionSplitList )
						{
							splitObject.newROI2DArea = MorphoROITools.dilateROIWithZConstraint( splitObject.newROI2DArea, 1, 1, z );
						}
						applyMorphoConstraints( tooBigROI, previousDetectionSplitList  );
					}
/*
					for ( SplitObject splitObject : previousDetectionSplitList )
					{
						// prevent from growing more than the original detection.
						splitObject.newROI2DArea = (ROI2DArea) tooBigROI.getIntersection( splitObject.newROI2DArea );
					}

					for ( SplitObject splitObject : previousDetectionSplitList )
					{
						splitObject.storeROI();
					}

//					ArrayList<SplitObject> previousDetectionSplitList2 = new ArrayList<SplitObject>();

					for ( SplitObject splitObject : previousDetectionSplitList )
					{
						// Create the union of all the other objects.

						ArrayList<ROI2DArea> listOfOtherROIs = new ArrayList<ROI2DArea>();
						for ( SplitObject s2 : previousDetectionSplitList )
						{
							if ( s2 != splitObject )
							{
								listOfOtherROIs.add( s2.storedROI2DArea );
							}
						}
						ROI2DArea unionOfOthers = (ROI2DArea) ROIUtil.getUnion( listOfOtherROIs );

						splitObject.newROI2DArea = (ROI2DArea) splitObject.storedROI2DArea.getSubtraction( unionOfOthers );

					}
*/
					// check if no evolution of the number of points of the ROIs.
					boolean evolution = false;

					for ( SplitObject splitObject : previousDetectionSplitList )
					{
						if ( splitObject.beforeProcessROI.getNumberOfPoints() <= splitObject.newROI2DArea.getNumberOfPoints() )
						{
							evolution = true;
							break;
						}
					}

//					System.out.println("EVOLUTION: iteration:" +i + " / " + evolution );
					if ( !evolution )
					{
						//break;
					}

					for ( SplitObject splitObject : previousDetectionSplitList )
					{
						// DEBUG DISPLAY

						ROI2DAreaX roi2DAreaX2 = MouseDetector.createROI2DAreaX( splitObject.newROI2DArea.getBooleanMask( true ) );
						roi2DAreaX2.setColor( Color.getHSBColor( (float)Math.random(), 0.9f, 0.9f ) );
						roi2DAreaX2.setName("tmp SPLIT #"+i + " t" + LiveMouseTracker.getT() );
						Icy.getMainInterface().getActiveSequence().addROI( roi2DAreaX2 );

					}

				}

				// TO watch evolution of the ROIs
				// DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG
				{
					try {
						Thread.sleep( 50 );
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					LiveMouseTracker.cleanTemporaryROIs();
				}

			}

			// final fill to use final unused space in detection.
//			for ( int i = 0 ;i < 10 ; i++ )
//			{
//				for ( SplitObject splitObject : previousDetectionSplitList )
//				{
//					splitObject.newROI2DArea = MorphoROITools.dilateROI( splitObject.newROI2DArea , 1, 1, 1 );
//				}
//				for ( SplitObject splitObject : previousDetectionSplitList )
//				{
//					splitObject.newROI2DArea = (ROI2DArea) tooBigROI.getIntersection( splitObject.newROI2DArea );
//					for ( SplitObject s2 : previousDetectionSplitList ) // substract with all others
//					{
//						if ( s2 == splitObject ) continue;
//						splitObject.newROI2DArea = (ROI2DArea) splitObject.newROI2DArea.getSubtraction( s2.newROI2DArea );
//					}
//				}
//			}

//			// final re-dilatation
			/*
			for ( SplitObject splitObject : previousDetectionSplitList )
			{
				splitObject.newROI2DArea = MorphoROITools.dilateROI( splitObject.newROI2DArea , 1, 1, 1 );
				splitObject.newROI2DArea = (ROI2DArea) tooBigROI.getIntersection( splitObject.newROI2DArea );
			}
			*/




			//		System.out.println("SPLIT");
			// 		process morpho
//			for ( int i = 0 ; i < 50 ; i++ ) // force progression limit FIXME: grow test
//			{
//				shuffle ( previousDetectionSplitList );
//
//				for ( SplitObject splitObject : previousDetectionSplitList )
//				{
//					// erode phase
//					//				splitObject.newROI2DArea = MorphoROITools.erodeROI( splitObject.newROI2DArea, 1, 1 , 1d );
//					//splitObject.newROI2DArea = MorphoROITools.dilateROI( splitObject.newROI2DArea, 1, 1 , 1d );
//				}
//				for ( SplitObject splitObject : previousDetectionSplitList )
//				{
//					double targetDetectionSurface = splitObject.detection.getSurface();
//					double currentSurface = splitObject.newROI2DArea.getAsBooleanMask().getNumberOfPoints();
//					double allSurface = tooBigROI.getAsBooleanMask().getNumberOfPoints();
//
//					targetDetectionSurface = allSurface / previousDetectionSplitList.size();
//					//				System.out.println("Target surface: " + targetDetectionSurface + " current: " + currentSurface );
//
//					//				if ( targetDetectionSurface < // if the surface is less than the expected surface, ok to dilate
//					//						currentSurface )
//					{
//						splitObject.newROI2DArea = MorphoROITools.dilateROI( splitObject.newROI2DArea, 1, 1 , 1d );
//					}
//					//				else
//					{
//						//					splitObject.newROI2DArea = MorphoROITools.dilateROI( splitObject.newROI2DArea, 1, 1 , 0.5d/2d );
//					}
//
//					splitObject.newROI2DArea = (ROI2DArea) tooBigROI.getIntersection( splitObject.newROI2DArea );
//					// substract with all others
//					for ( SplitObject s2 : previousDetectionSplitList )
//					{
//						if ( s2 == splitObject ) continue;
//						splitObject.newROI2DArea = (ROI2DArea) splitObject.newROI2DArea.getSubtraction( s2.newROI2DArea );
//					}
//				}
//			}
		}

		// previous split algo
		{
//			for ( int i = 0 ; i < 50 ; i++ )
//			{
//				shuffle ( previousDetectionSplitList );
//				for ( SplitObject splitObject : previousDetectionSplitList )
//				{
//					double targetDetectionSurface = splitObject.detection.getSurface();
//					double currentSurface = splitObject.newROI2DArea.getAsBooleanMask().getNumberOfPoints();
//					double allSurface = tooBigROI.getAsBooleanMask().getNumberOfPoints();
//
//					targetDetectionSurface = allSurface / previousDetectionSplitList.size();
//					{
//						splitObject.newROI2DArea = MorphoROITools.dilateROI( splitObject.newROI2DArea, 1, 1 , 1d );
//					}
//
//					splitObject.newROI2DArea = (ROI2DArea) tooBigROI.getIntersection( splitObject.newROI2DArea );
//					// substract with all others
//					for ( SplitObject s2 : previousDetectionSplitList )
//					{
//						if ( s2 == splitObject ) continue;
//						splitObject.newROI2DArea = (ROI2DArea) splitObject.newROI2DArea.getSubtraction( s2.newROI2DArea );
//					}
//				}
//			}
		}


		for ( SplitObject splitObject : previousDetectionSplitList )
		{
			splitObject.newROI2DArea.setName("tmp sR");
			//getInfraOut().addROI( splitObject.newROI2DArea );
			ROI2DAreaX roi2DAreaX = MouseDetector.createROI2DAreaX( splitObject.newROI2DArea.getBooleanMask( true ) );
			roi2DAreaX.setName("seg ok");
			MouseDetection detection = new MouseDetection( roi2DAreaX , LiveMouseTracker.getT() );
//			detection.setCanBeUsedForLearning( false ); // now made with setBuiultBySplitter
			detection.setBuiltByDetectionSplitter( true );
			detectionCreated.add( detection );
			roi2DAreaX.setColor( Color.GRAY );

//			ROI2DAreaX roi2DAreaX2 = MouseDetector.createROI2DAreaX( splitObject.newROI2DArea.getBooleanMask( true ) );
//			roi2DAreaX2.setColor( Color.getHSBColor( (float)Math.random(), 0.9f, 0.9f ) );
//			roi2DAreaX2.setName("BAM SPLIT " + LiveMouseTracker.getT() );
//			Icy.getMainInterface().getActiveSequence().addROI( roi2DAreaX2 );
		}



		return detectionCreated;
	}


	private void applyMorphoConstraints( ROI2DArea tooBigROI, ArrayList<SplitObject> previousDetectionSplitList ) {

		for ( SplitObject splitObject : previousDetectionSplitList )
		{
			// prevent from growing more than the original detection.
			splitObject.newROI2DArea = (ROI2DArea) tooBigROI.getIntersection( splitObject.newROI2DArea );
		}

		for ( SplitObject splitObject : previousDetectionSplitList )
		{
			splitObject.storeROI();
		}

//		ArrayList<SplitObject> previousDetectionSplitList2 = new ArrayList<SplitObject>();

		for ( SplitObject splitObject : previousDetectionSplitList )
		{
			// Create the union of all the other objects.

			ArrayList<ROI2DArea> listOfOtherROIs = new ArrayList<ROI2DArea>();
			for ( SplitObject s2 : previousDetectionSplitList )
			{
				if ( s2 != splitObject )
				{
					listOfOtherROIs.add( s2.storedROI2DArea );
				}
			}
			ROI2DArea unionOfOthers = (ROI2DArea) ROIUtil.getUnion( listOfOtherROIs );

			splitObject.newROI2DArea = (ROI2DArea) splitObject.storedROI2DArea.getSubtraction( unionOfOthers );

		}

	}


	private ArrayList<TrackSegment> getAllTrackSegmentFinishingAt( int t ) {

		//System.out.println("getAllTrackSegmentFinishingAt");
		//System.out.println("Argument is t=" + t );
		ArrayList<TrackSegment> resultList = LiveMouseTracker.getMainAnimalPool().getAllTrackSegmentFinishingAt( t );
		resultList.addAll( LiveMouseTracker.trackContainer.anonymousTrackSegmentPool.getTrackSegmentFinishingAt( t ) );

		return resultList;

	}


	private void shuffle(ArrayList<SplitObject> list) {

		for ( int z = 0 ; z < 5 ; z++ )
		{
			swap ( list ,
					(int)Math.random() * list.size() ,
					(int)Math.random() * list.size() );
		}

	}

	private void swap(ArrayList<SplitObject> list, int i, int j) {
		SplitObject a = list.get( i );
		SplitObject b = list.get( j );
		list.set( i ,  b );
		list.set( j ,  a );
	}

	class SplitObject
	{
		MouseDetection detection;
		double movingPower = 0;
		/** Roi created in the split process */

		/** Use to keep the previous area in n*n process (for instance substracting all ROIs) */
		ROI2DArea beforeProcessROI = null;
		ROI2DArea storedROI2DArea = null;
		ROI2DArea newROI2DArea = null;

		public void storeROI( )
		{
			storedROI2DArea = newROI2DArea;
		}

		public void setBeforeProcessROI( )
		{
			beforeProcessROI = newROI2DArea;
		}


//		double targetVolume = 0;
//		double targetSurface = 0;

		public SplitObject(MouseDetection detection ) {
			this.detection = detection;

		}
	}

}
