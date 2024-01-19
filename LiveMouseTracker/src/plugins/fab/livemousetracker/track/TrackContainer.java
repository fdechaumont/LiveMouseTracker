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

import java.awt.Color;
import java.util.ArrayList;

import plugins.fab.livemousetracker.Animal;
import plugins.fab.livemousetracker.LiveMouseTracker;
import plugins.fab.livemousetracker.detection.MouseDetection;

/**
 * Contains all the tracks, from animals and anonymous ones.
 */
public class TrackContainer {

	/** Anonymous track. */
	public AnonymousPool anonymousTrackSegmentPool = null;

	/** Track associated with animals.*/
	public AnimalPool animalTrackSegmentPool = null;


	public TrackContainer() {
		animalTrackSegmentPool = new AnimalPool();
		anonymousTrackSegmentPool = new AnonymousPool();
	}

	/** Provides identity to a track and remove it from anonymous to the pool of an animal */
	public void setTrackIdentity( TrackSegment track , Animal animal, String setBy )
	{
		if ( animal.enabled == false )
		{
			System.out.println("ERROR: THIS ANIMAL IS NOT ENABLED AND CANNOT BE ID-ASSIGNED - reason: " + setBy + " RFID: " + animal.getRfidID() );
			//Thread.dumpStack();
			return;
		}
//		LiveMouseTracker.multiIdentityAgentManager.disableAllCurrentRunningAgent();
		anonymousTrackSegmentPool.removeTrack( track );

		//System.out.println("Identity set by " + setBy + " ts:" + track + " animal:"+animal );

		// FOR DEBUG ONLY
		// CHECK IF ANIMAL IS ALREADY HAVING A DETECTION OVERLAPPING

		for ( TrackSegment animalTrack : animal.getTrackSegments() )
		{
			if ( track.overlapInT( animalTrack ) )
			{
				System.err.println("[SET TRACK IDENTITY ERROR: SEVERAL ANIMALS WITH SAME IDENTITY]");
				Thread.dumpStack();
				//System.out.println("Correcting...");
				LiveMouseTracker.trackContainer.setTrackAnonymous( animal , animalTrack );
			}
		}

		animalTrackSegmentPool.addTrackSegment( animal, track );
		track.setIdentityAffectedBy( setBy );
	}



//	/** Correct a false track association with a track already in the animal pool.
//	 * (Transfer from animal to animal).
//	 *  */
//	public static void transferOwnerShipWithRFID( RawMouseDetection detection, String id ) {
//
//		Animal currentAnimalWithDetection = animalTrackSegmentPool.getAnimalWithDetection( detection );
//		TrackSegment trackSegment = currentAnimalWithDetection.getTrackContainingDetection( detection );
//
//		Animal targetAnimal = LiveMouseTracker.getMainAnimalPool().getAnimalWithRFID( id );
//
//		if ( targetAnimal == null )
//		{
//			// The RFID that has been read is not assigned to any other animal yet.
//			// But
//			// setTrackAnonymous( currentAnimalWithDetection, trackSegment );
//
//			// FORCE RFID ID !
//			//currentAnimalWithDetection.setRfidID( id );
//
//			System.out.println("Cannot transfer ownership with RFID: no animal with this RFID is known yet.");
//			System.out.println("FIXME: this is a problem, the track will be set as anonymous");
//			System.out.println("But the Machine larning will re associate it instantly.");
//			System.out.println("Idea: could go back to the instant where the mismatch occured and inverse animals.");
//			return;
//			//currentAnimalWithDetection.removeTrackSegment( trackSegment );
//		}
//
//		// remove all tracks of the targetAnimal that are conflicting with the track that we will put in it.
//		setTrackAnonymous( targetAnimal , trackSegment.firstTimePoint , trackSegment.lastTimePoint );
//
//		// transfer ownership.
//		currentAnimalWithDetection.removeTrackSegment( trackSegment );
//		setTrackIdentity( trackSegment , targetAnimal );
//
//	}



	/** Take out a track from an animal and put it back in the anonymous track set. */
	public void setTrackAnonymous(Animal animal,
			int firstTimePoint, int lastTimePoint) {

		//System.out.println("[SetTrackAnonymous] First time point: " + firstTimePoint );
		//System.out.println("[SetTrackAnonymous] Last time point: " + lastTimePoint );

		//System.out.println("Debug association: " + animal );
		//System.out.println("Split track start time point");

		// FIRST SPLIT and put back in the animal.

		splitAnimalTrack( animal, firstTimePoint, lastTimePoint, firstTimePoint );
		splitAnimalTrack( animal, firstTimePoint, lastTimePoint, lastTimePoint+1 );

//		ArrayList<TrackSegment> overlappingTracks = animal.getTracks(firstTimePoint, lastTimePoint);
//		for ( TrackSegment ts : overlappingTracks )
//		{
//			animal.removeTrackSegment( ts );
//			for ( TrackSegment tsSplit : ts.split( firstTimePoint ) )
//			{
//				animal.addTrackSegment( tsSplit );
//			}
//		}




		// THEN REMOVE anonymous set.

// 		To consider if we want to use RFID in a back-propagation mode.
//		System.out.println("Split track last time point");
//		animal.splitTrack( lastTimePoint );

		ArrayList<TrackSegment> trackSegmentList = animal.getTracks( firstTimePoint, lastTimePoint );

		// set anonymous.
		for ( TrackSegment ts : trackSegmentList )
		{
			setTrackAnonymous(animal, ts );
		}

	}

	/**
	 * Take the tracks of an animal from firstTimePoint to lastTimePoint, and cut it at t.
	 * @param animal
	 * @param firstTimePoint
	 * @param lastTimePoint
	 * @param t
	 */
	private void splitAnimalTrack(Animal animal, int firstTimePoint , int lastTimePoint , int t ) {

		ArrayList<TrackSegment> overlappingTracks = animal.getTracks(firstTimePoint, lastTimePoint);
		for ( TrackSegment ts : overlappingTracks )
		{
			animal.removeTrackSegment( ts );
			for ( TrackSegment tsSplit : ts.split( t ) )
			{
				animal.addTrackSegment( tsSplit );
			}
		}


	}

	/** Take out a track from an animal and put it back in the anonymous track set. */
	public void setTrackAnonymous(Animal animal, TrackSegment ts) {

		//System.out.println("[TrackContainer] Make anonymous track. " + ts );
		//System.out.println("[TrackContainer] Animal contain track: " + animal.contain( ts ) );
		animal.removeTrackSegment( ts );
		for ( MouseDetection det : ts.getDetectionList() )
		{
			det.getROI2DArea().setColor( Color.white );
		}
		anonymousTrackSegmentPool.add( ts );

	}

//	/** Set a track from the anonymous pool to the animal pool */
//	public void setTrackIdentity(MouseDetection detection, Animal animal ) {
//
//		TrackSegment track = anonymousTrackSegmentPool.getTrackSegmentsContaining( detection );
//		setTrackIdentity( track , animal );
//	}

	/** Get all detection at t, in anonymous or animal track */
	public ArrayList<MouseDetection> getAllDetectionAt(int t) {

		ArrayList<MouseDetection> mouseDetectionList = new ArrayList<MouseDetection>();

		mouseDetectionList.addAll( anonymousTrackSegmentPool.getAllDetectionAt( t ) );
		mouseDetectionList.addAll( animalTrackSegmentPool.getAllDetectionAt( t ) );

		return mouseDetectionList;
	}

	public ArrayList<TrackSegment> getAllTracks(int start, int end) {

		ArrayList<TrackSegment> tsList= new ArrayList<TrackSegment>();

		tsList.addAll( anonymousTrackSegmentPool.getTracks( start , end ) );

		for ( Animal animal : animalTrackSegmentPool.getAnimalList() )
		{
			tsList.addAll( animal.getTracks( start, end ) );
		}

		return tsList;
	}


	public ArrayList<TrackSegment> getAllTracks(int t ) {

		ArrayList<TrackSegment> tsList= new ArrayList<TrackSegment>();

		tsList.addAll( anonymousTrackSegmentPool.getTrackSegmentsContaining( t ) );

		for ( Animal animal : animalTrackSegmentPool.getAnimalList() )
		{
			TrackSegment ts = animal.getTrackContaining( t );
			if ( ts!=null )
			{
				tsList.add( ts );
			}
		}

		return tsList;
	}

	public void removeTrack(TrackSegment trackSegment) {

		anonymousTrackSegmentPool.removeTrack( trackSegment );
		animalTrackSegmentPool.removeTrack( trackSegment );

	}

	public TrackSegment getTrackId(int dataBaseId) {

		TrackSegment ts = anonymousTrackSegmentPool.getTrackWithId( dataBaseId );
		if ( ts==null )
		{
			ts = animalTrackSegmentPool.getTrackWithId( dataBaseId );
		}
		return ts;
	}

	public void addTrackSegment(Animal animal, TrackSegment trackSegment) {

		if ( animal != null )
		{
			animal.addTrackSegment( trackSegment );
		}else
		{
			anonymousTrackSegmentPool.add( trackSegment );
		}

	}

	public int getFirstDetectionT() {

		int minT = Integer.MAX_VALUE;
		for ( TrackSegment ts : anonymousTrackSegmentPool.getTrackSegments() )
		{
			int first = ts.getFirstTimePoint();
			minT = Math.min( minT,  first );
		}

		for ( Animal animal : animalTrackSegmentPool.getAnimalList() )
		{
			for ( TrackSegment ts : animal.getTrackSegments() )
			{
				int first = ts.getFirstTimePoint();
				minT = Math.min( minT,  first );
			}
		}

		return minT;
	}

	/** Create detection to nextT from sourceT */
	public void repeatLastDetection( int sourceT ) {

		for ( Animal animal : animalTrackSegmentPool.getAnimalList() )
		{
			TrackSegment ts = animal.getTrackContaining( sourceT );
			if ( ts != null )
			{
				ts.addDetection( ts.getDetection( sourceT ).getCopy( sourceT+1 ) );
			}
		}
		for ( TrackSegment ts: anonymousTrackSegmentPool.getTrackSegmentFinishingAt( sourceT ) )
		{
			ts.addDetection( ts.getDetection( sourceT ).getCopy( sourceT+1 ) );
		}

  	}

	/** break all tracks at a given t
	 * */
	public void breakTooLongTrack( int t ) {

		//System.out.println("Breaking too long tracks at t=" + t );
		for ( Animal animal : animalTrackSegmentPool.getAnimalList() )
		{
			ArrayList<TrackSegment> trackList = animal.getTrackSegments();

			for ( TrackSegment track : trackList )
			{
				if ( track.containDetectionAt( t ) )
				{
					animal.removeTrackSegment( track );
					for ( TrackSegment tsSplit : track.split( t ) )
					{
						animal.addTrackSegment( tsSplit );
					}
					//System.out.println( "Track splitted: " + track );
				}
			}
		}

		{
			ArrayList<TrackSegment> trackList = anonymousTrackSegmentPool.getTrackSegments();
			for ( TrackSegment track : trackList )
			{
				if ( track.containDetectionAt( t ) )
				{
					anonymousTrackSegmentPool.removeTrack( track );
					for ( TrackSegment tsSplit : track.split( t ) )
					{
						anonymousTrackSegmentPool.add( tsSplit );
					}
					//System.out.println( "Track splitted: " + track );
				}
			}
		}

	}

	public Animal getAnimalWithDataBaseId(int idAnimal ) {

		for ( Animal animal : animalTrackSegmentPool.animalList )
		{
			if ( animal.getDataBaseId() == idAnimal ) return animal;
		}
		return null;

	}













}
