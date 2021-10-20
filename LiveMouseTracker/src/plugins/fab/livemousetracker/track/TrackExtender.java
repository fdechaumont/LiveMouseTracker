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
package plugins.fab.livemousetracker.track;

import java.util.ArrayList;

import plugins.fab.livemousetracker.Animal;
import plugins.fab.livemousetracker.LiveMouseTracker;
import plugins.fab.livemousetracker.detection.MouseDetection;

// previously call prolongator... not english :/
public class TrackExtender {


	/** Add detection and perfom basic tracking
	 * Find and assign best match between set of incoming detection and set of unfinished tracks.
	 * */
	public static TrackSegment addDetection( MouseDetection mouseDetection , ArrayList<TrackSegment> trackSegmentArrayList )
	{
		// check the candidate tracks.
		TrackSegment bestTrack = null;
		double minDistance = Double.MAX_VALUE;

		int t = mouseDetection.getT();

//		System.out.println("Prolongator for det " + mouseDetection );
		for ( TrackSegment ts : trackSegmentArrayList )
		{
			if ( !ts.canBeProlongated ) continue;
//			if ( ts.getLastTimePoint() == t )
//			{
//				System.err.println( "last==detetime: " + ts.getLastTimePoint() + " ts: " + ts );
//			}
			//System.out.println( "last: " + ts.getLastTimePoint() + " ts: " + ts );

			// System.out.println("Current T "  + t);
			if ( ts.getLastTimePoint() == t -1 ) // last detection is at previous frame >>> ok.
			{
				//System.out.println("pass last point t-1 " + ts );
				// check best distances.
				MouseDetection candidateDetection = ts.getDetection( t-1 );
				// System.out.println("Candidate detection " + candidateDetection );
				double distance = candidateDetection.getMassCenter().toPoint2D().distance( mouseDetection.getMassCenter().toPoint2D() );
				if ( distance < minDistance && distance
						< LiveMouseTracker.MAX_DISTANCE_FOR_TRACKING_DIRECT_ASSO_IN_TRACK_PROLONGATOR ) // FIXME HardThreshold should be computed with speed
				{
					bestTrack = ts;
					minDistance = distance;
				}
			}
		}

		boolean addDetection = true;
		if ( bestTrack != null )
		{
			if( LiveMouseTracker.BREAK_TRACK_CONTINUITY_AFTER_ANIMAL_CONTACT )
			{
//				int t = mouseDetection.getT();
				if ( bestTrack.getLastTimePoint() == t -1 )
				{
					MouseDetection lastDetection = bestTrack.getDetection( bestTrack.getLastTimePoint() );
					{
						// if the last detection is produced by split and the new one is not, force a break of track.
						if ( lastDetection.isBuiltByDetectionSplitter()
								&&
								!mouseDetection.isBuiltByDetectionSplitter()
								)
						{
							bestTrack = null;
						}
					}
				}
			}

			if ( LiveMouseTracker.CREATE_NEW_TRACK_AFTER_SPLIT_WITH_ID_CONTINUITY )
			{
//				int t = mouseDetection.getT();
				if ( bestTrack.getLastTimePoint() == t -1 )
				{
//					System.out.println("Best track last t: " + bestTrack.getLastTimePoint() );
					MouseDetection lastDetection = bestTrack.getDetection( t-1) ; //bestTrack.getLastTimePoint() );
					{
						// if the last detection is produced by split and the new one is not, force a break of track.
						if ( lastDetection.isBuiltByDetectionSplitter()
								&&
								!mouseDetection.isBuiltByDetectionSplitter()
								)
						{
							bestTrack.canBeProlongated = false;
							Animal animal = LiveMouseTracker.getMainAnimalPool().getAnimalOwningTrack( bestTrack );

							// Takes the last detection to build the new track. Then add the new detection
							// so that the post process can be called in the track.
							// Then the detection is correct, we remove everything and let the other part of the algo
							// add the detection naturally.
							TrackSegment newContinuityTrackSegment = new TrackSegment( lastDetection );
//							System.out.println("[Track Prolongator] last detection : " + lastDetection );
//							System.out.println("[Track Prolongator] mouse detection : " + mouseDetection );
//							newContinuityTrackSegment.addDetection( lastDetection );
							newContinuityTrackSegment.addDetection( mouseDetection );
							newContinuityTrackSegment.removeDetection( lastDetection );
//							newContinuityTrackSegment.removeDetection( mouseDetection );

							LiveMouseTracker.trackContainer.addTrackSegment ( animal , newContinuityTrackSegment );
							bestTrack = newContinuityTrackSegment;
							addDetection = false;
						}
					}
				}

			}

		}

		if ( addDetection ) // if the CREATE_NEW_TRACK_AFTER_SPLIT_WITH_ID_CONTINUITY is active we may not need this
		{
			if ( bestTrack != null )
			{
				bestTrack.addDetection( mouseDetection );
			}
		}
		else
		{
//			System.err.println("Track prolongator fail: min=" + minDistance +" detect: " + mouseDetection );
//			for ( TrackSegment ts : trackSegmentArrayList )
//			{
//				if ( ts.getLastTimePoint() < t-10 ) continue;
//
//				System.err.println("ts: " + ts );
//				System.err.println( "last: " + ts.getLastTimePoint() );
//				MouseDetection detection = ts.getDetection( ts.getLastTimePoint() );
//				System.err.println("det: " + detection);
//				double dis = detection.getMassCenter().toPoint2D().distance( mouseDetection.getMassCenter().toPoint2D() );
//				System.err.println("dis: " + dis );
//
//			}
		}

//		System.out.println( "track prolongator best track: " + bestTrack );
//		System.out.println( "--" );
		return bestTrack;

	}



}
