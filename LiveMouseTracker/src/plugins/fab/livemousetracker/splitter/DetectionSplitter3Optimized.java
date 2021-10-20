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
package plugins.fab.livemousetracker.splitter;

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import icy.image.IcyBufferedImage;
import icy.roi.BooleanMask2D;
import icy.roi.ROIUtil;
import icy.system.profile.Chronometer;
import plugins.fab.livemousetracker.LiveMouseTracker;
import plugins.fab.livemousetracker.ROI2DAreaX;
import plugins.fab.livemousetracker.Util;
import plugins.fab.livemousetracker.detection.MouseDetection;
import plugins.fab.livemousetracker.detection.MouseDetector;
import plugins.fab.livemousetracker.track.TrackSegment;
import plugins.kernel.roi.roi2d.ROI2DArea;

/**
 * New version of detection splitter.
 *
 */
public class DetectionSplitter3Optimized {

	/**
	 * try to find how many animals are involved in this rawDetection, and then split them.
	 *
	 */
	public static ArrayList<MouseDetection> splitDetectionWithSeed( ROI2DArea tooBigROI , IcyBufferedImage difDepthImage ) {


		if ( LiveMouseTracker.LOG_SPLIT )
		{
			System.out.println("[SPLIT] t:" + LiveMouseTracker.getT() );
		}
		ArrayList<MouseDetection> detectionCreated = new ArrayList<MouseDetection>();

//		Rectangle roiBounds = tooBigROI.getBounds();

		// too large ROI, cannot split from it...
//		if( (roiBounds.width * roiBounds.height) > LiveMouseTracker.TOO_BIG_DETECTION_REJECT_SIZE )
//			return detectionCreated;

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
					tooBigROI.intersects( ts.getDetection( ts.getLastTimePoint() ).getROI2DArea() ) )
//					tooBigROI.contains(
//					ts.getDetection( ts.getLastTimePoint() ).getMassCenter().toPoint2D() ) )
			{
				tsListCandidateInROI.add( ts );
			}
		}

		BooleanMask2D tooBigROIMask = tooBigROI.getBooleanMask( true );
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
				ROI2DAreaX roi2DAreaX = MouseDetector.createROI2DAreaX( tooBigROIMask);
				roi2DAreaX.setName("seg ok");
				// check if ROI name is ok
				detectionCreated.add( new MouseDetection( roi2DAreaX, tooBigROIMask, LiveMouseTracker.getT() ) );
			}
			//System.out.println(""+ nbObject + " object: can't split");
			return detectionCreated;
		}

		//System.out.println("splitting in " + nbObject + " objects");

		// fill using dif depth map infos.

		ArrayList<SplitObject> previousDetectionSplitList = new ArrayList<SplitObject>();
		for ( TrackSegment ts: tsListCandidateInROI ) // Build the split list based on previous detection list.
		{
			previousDetectionSplitList.add( new SplitObject( ts.getDetection( ts.getLastTimePoint() ), ts ) );
		}

		// moving power parts removed

		// init ROIs
		for ( SplitObject splitObject : previousDetectionSplitList )
		{
			splitObject.newROI2DArea = (ROI2DArea) splitObject.detection.getROI2DArea().getCopy();
		}


		{
			// Main split processing.
//			Chronometer c = new Chronometer("[SPLIT] Core split");
			DetectionSplitter3Core coreSplitter = new DetectionSplitter3Core( tooBigROI , tooBigROIMask, previousDetectionSplitList );
//			c.displayMs();
		}

			// Init seed
//			System.out.println("[SPLIT] New object");

		/*
		Chronometer c = new Chronometer("Core split");
			for ( SplitObject splitObject : previousDetectionSplitList )
			{
				splitObject.newROI2DArea = new ROI2DArea( );

				// Add the mass center as a start for split.
//				Point2D p = splitObject.detection.getMassCenter().toPoint2D();
//				splitObject.newROI2DArea.addPoint( (int)p.getX() , (int)p.getY() );

				{


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
						System.err.println("Spine point for split: null (no front and back point)");
					}
					if ( !init )
					{
						Point2D p = splitObject.detection.getMassCenter().toPoint2D();
						splitObject.newROI2DArea.addPoint( (int)p.getX() , (int)p.getY() );
						init = true;
					}
				}

			}

			DecimalFormat df = new DecimalFormat("000");

			int stepZ = 2;

		//	for ( int i = 0 ; i < 40 ; i++ )
			int i = 0;
			{
				{
					for ( SplitObject splitObject : previousDetectionSplitList )
					{
						splitObject.setBeforeProcessROI();
					}

					for ( int z = 100 ; z > 01 ; z-=stepZ ) // first dilate high parts of the animal, and then bottom parts.
					//int z = 1;
					{
						for ( SplitObject splitObject : previousDetectionSplitList )
						{
							splitObject.newROI2DArea = MorphoROITools.dilateROIWithZConstraint( splitObject.newROI2DArea, 1, 1, z );
						}
						applyMorphoConstraints( tooBigROI, previousDetectionSplitList  );

					}

				}

			}

			c.displayMs();
			*/




		for ( SplitObject splitObject : previousDetectionSplitList )
		{
			splitObject.newROI2DArea.setName("tmp sR");
			BooleanMask2D newMask = splitObject.newROI2DArea.getBooleanMask( true );
			//getInfraOut().addROI( splitObject.newROI2DArea );
			ROI2DAreaX roi2DAreaX = MouseDetector.createROI2DAreaX(newMask);
			roi2DAreaX.setName("seg ok");
			MouseDetection detection = new MouseDetection( roi2DAreaX , newMask, LiveMouseTracker.getT() );
//			detection.setCanBeUsedForLearning( false ); // now made with setBuiultBySplitter
			detection.setBuiltByDetectionSplitter( true );
			detectionCreated.add( detection );
			roi2DAreaX.setColor( Color.GRAY );

//			ROI2DAreaX roi2DAreaX2 = MouseDetector.createROI2DAreaX( splitObject.newROI2DArea.getBooleanMask( true ) );
//			roi2DAreaX2.setColor( Color.getHSBColor( (float)Math.random(), 0.9f, 0.9f ) );
//			roi2DAreaX2.setName("BAM SPLIT " + LiveMouseTracker.getT() );
//			Icy.getMainInterface().getActiveSequence().addROI( roi2DAreaX2 );
		}

		//chronoSplitDetection.displayMs();

		return detectionCreated;
	}


	private static void applyMorphoConstraints( ROI2DArea tooBigROI, ArrayList<SplitObject> previousDetectionSplitList ) {

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


	private static ArrayList<TrackSegment> getAllTrackSegmentFinishingAt( int t ) {

		//System.out.println("getAllTrackSegmentFinishingAt");
		//System.out.println("Argument is t=" + t );
		ArrayList<TrackSegment> resultList = LiveMouseTracker.getMainAnimalPool().getAllTrackSegmentFinishingAt( t );
		resultList.addAll( LiveMouseTracker.trackContainer.anonymousTrackSegmentPool.getTrackSegmentFinishingAt( t ) );

		return resultList;

	}


	private static void shuffle(ArrayList<SplitObject> list) {

		for ( int z = 0 ; z < 5 ; z++ )
		{
			swap ( list ,
					(int)Math.random() * list.size() ,
					(int)Math.random() * list.size() );
		}

	}

	private static void swap(ArrayList<SplitObject> list, int i, int j) {
		SplitObject a = list.get( i );
		SplitObject b = list.get( j );
		list.set( i ,  b );
		list.set( j ,  a );
	}

	static class SplitObject
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
		TrackSegment trackSegment;

		public SplitObject(MouseDetection detection, TrackSegment trackSegment ) {
			this.detection = detection;
			this.trackSegment = trackSegment;
		}

		public Point2D getSpeedVector() {

			Point2D speedVector = new Point2D.Double( 0, 0 );

			MouseDetection previousDetection = trackSegment.getDetection( detection.getT() -1 );
			if ( previousDetection != null )
			{
				speedVector = Util.createVector(
						previousDetection.getMassCenter().toPoint2D() , detection.getMassCenter().toPoint2D() );
			}

			return speedVector;
		}


	}

}
