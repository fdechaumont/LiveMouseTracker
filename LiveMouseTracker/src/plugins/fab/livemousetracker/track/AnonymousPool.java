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

import java.awt.geom.Point2D;
import java.util.ArrayList;

import plugins.fab.livemousetracker.detection.MouseDetection;

/**
 * The trackSegment pool is a pool of correct individual tracks.
 *
 * @author Fab
 *
 */
public class AnonymousPool extends AbstractTrackPool {

	public ArrayList<TrackSegment> trackSegmentArrayList = new ArrayList<TrackSegment>();
//	AnimalPool animalPool;

//	public TrackSegmentPool( AnimalPool animalPool ) {
//		this.animalPool = animalPool;
//	}

	public AnonymousPool( ) {

	}

	/** build pool with the provided list */
	public AnonymousPool( ArrayList<TrackSegment> trackSegmentArrayList) {
		this.trackSegmentArrayList = trackSegmentArrayList;
	}

	/** Add detection and perfom basic tracking
	 * Find and assign best match between set of incoming detection and set of unfinished tracks.
	 * */
	public TrackSegment addDetection( MouseDetection mouseDetection  )
	{
		TrackSegment ts = TrackExtender.addDetection ( mouseDetection , getTrackSegments() );

		if ( ts == null ) // The prolongator was not able to prolongate anything.
			// so we create a new track
		{
			ts = new TrackSegment( mouseDetection );
			add( ts );
		}
		return ts;

	}


	public ArrayList<TrackSegment> getTrackSegments() {

		synchronized ( trackSegmentArrayList ) {
			return new ArrayList<TrackSegment>( trackSegmentArrayList );
		}
	}

	public void clear() {
		synchronized (trackSegmentArrayList) {
			trackSegmentArrayList.clear();
		}
	}

	/** return the track segment that contains a detection at t
	 */
	public ArrayList<TrackSegment> getTrackSegmentFinishingAt(int t) {
		ArrayList<TrackSegment> finishedTrackSegment = new ArrayList<TrackSegment>();

		synchronized (trackSegmentArrayList)
		{
			for ( TrackSegment ts : trackSegmentArrayList )
			{
				//System.out.println( "last t point:" + ts.getLastTimePoint() );
				if ( ts.getLastTimePoint() == t )
				{
					finishedTrackSegment.add( ts );
				}
			}
		}

		return finishedTrackSegment;
	}

	public void add(TrackSegment trackSegment ) {

		synchronized ( trackSegmentArrayList) {
			trackSegmentArrayList.add( trackSegment );
		}

	}


	public void removeTrack(TrackSegment trackSegment ) {
		synchronized ( trackSegmentArrayList) {
			trackSegmentArrayList.remove( trackSegment );
		}
	}

	/**
	 * Gest the closest track to the point given. null if the trackpool is empty
	 * @param point
	 * @return
	 */
	public TrackSegment getClosestTrack( Point2D point )
	{
		//TrackSegmentPool tp = trackPool;
		double minDistance = java.lang.Double.MAX_VALUE;
		TrackSegment bestTrack = null;

		synchronized (trackSegmentArrayList)
		{
			for ( TrackSegment ts : trackSegmentArrayList )
			{
				for ( MouseDetection detection : ts.getDetectionList() )
				{
					double distance = detection.getMassCenter().toPoint2D().distance( point );
					if ( distance < minDistance )
					{
						minDistance = distance;
						bestTrack = ts;
					}
				}
			}
		}

		return bestTrack;
	}

	public ArrayList<TrackSegment> getTrackSegmentsContaining(int t) {
		ArrayList<TrackSegment> resultList = new ArrayList<TrackSegment>();

		synchronized (trackSegmentArrayList)
		{
			for ( TrackSegment ts : trackSegmentArrayList )
			{
				trackLoop:
				for ( MouseDetection detection : ts.getDetectionList() )
				{
					if ( detection.getT() == t )
					{
						resultList.add( ts );
						continue trackLoop;
					}
				}
			}
		}

		return resultList;
	}

	public TrackSegment getTrackSegmentsContaining(
			MouseDetection detection) {

		synchronized (trackSegmentArrayList)
		{
			for ( TrackSegment trackSegment : trackSegmentArrayList )
			{
				if ( trackSegment.containDetection( detection ) ) return trackSegment;
			}
		}

		return null;
	}

	/** Provides all detections for t */
	public ArrayList<MouseDetection> getAllDetectionAt(int t) {

		ArrayList<MouseDetection> detectionList = new ArrayList<MouseDetection>();
		for ( TrackSegment ts : getTrackSegmentsContaining(t) )
		{
			detectionList.add( ts.getDetection( t ) );
		}
		return detectionList;
	}

	public ArrayList<TrackSegment> getTracks(int firstTimePoint, int lastTimePoint) {
		ArrayList<TrackSegment> result = new ArrayList<TrackSegment>();

		synchronized (trackSegmentArrayList)
		{
			for( TrackSegment ts : trackSegmentArrayList )
			{
				if ( ts.overlapInT( firstTimePoint, lastTimePoint ) )
				{
					result.add( ts );
				}
			}
		}

		return result;
	}

	@Override
	public TrackSegment getTrackWithId(int dataBaseId) {
		synchronized (trackSegmentArrayList)
		{
			for ( TrackSegment ts : trackSegmentArrayList )
			{
				if ( ts.dataBaseId == dataBaseId ) return ts;
			}
		}

		return null;
	}

	public void removeAllTrack(ArrayList<TrackSegment> trackSegmentList) {
		synchronized (trackSegmentArrayList)
		{
			trackSegmentArrayList.removeAll( trackSegmentList );
		}
	}


//	/** If the track is assigned to an animal, performs the transfer between this
//	 * pool and the animal dedicated pool.
//	 */
//	public void transferTrackSegmentToAnimal(TrackSegment finishedTrackSegment) {
//
//		animalPool.addTrackSegment( finishedTrackSegment );
//		trackSegmentArrayList.remove( finishedTrackSegment );
//
//	}

//	public void transferTrackSegmentToAnimal(
//			ArrayList<TrackSegment> trackSegmentList) {
//		for ( TrackSegment ts : trackSegmentList )
//		{
//			transferTrackSegmentToAnimal( ts );
//		}
//
//	}


}
