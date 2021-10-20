package plugins.fab.livemousetracker.identity;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import plugins.fab.livemousetracker.Animal;
import plugins.fab.livemousetracker.LiveMouseTracker;
import plugins.fab.livemousetracker.overlay.Event;
import plugins.fab.livemousetracker.track.TrackSegment;

public class DiadicBlackAndWhiteIdentity {

	/**
	 * The diadic BlackAndWhiteIdentity is designed for an experiment with highly contrasted animals, and only 2 animals.
	 * It sets identities depending on the average color of the animal, the animal 0 is the blackest the animal 1 the whitest
	 * */
	public static void diadicBlackAndWhiteIdentity() {

		Animal blackestAnimal = LiveMouseTracker.getMainAnimalPool().animalList.get( 0 );
		Animal whitestAnimal = LiveMouseTracker.getMainAnimalPool().animalList.get( 1 );

		// recover the last 60 frames of the 2 existing tracks
		ArrayList<TrackSegment> tsList = LiveMouseTracker.trackContainer.getAllTracks( LiveMouseTracker.getT() );

		// check number of track
		if ( tsList.size() != 2 ) return;

		// check trackSegment List
		for ( TrackSegment ts : tsList )
		{
			if ( ts.getLength() < LiveMouseTracker.MIN_TRACK_LENGTH_TO_PROCESS_ID_CHECK ) return;
		}

		// data are ok. Proceed.

		TrackSegment ts0 = tsList.get( 0 );
		TrackSegment ts1 = tsList.get( 1 );

		TrackSegment highIntensityTrack = null;
		TrackSegment lowIntensityTrack = null;

		if ( ts0.getMeanIntensity( LiveMouseTracker.MIN_TRACK_LENGTH_TO_PROCESS_ID_CHECK ) <
				ts1.getMeanIntensity( LiveMouseTracker.MIN_TRACK_LENGTH_TO_PROCESS_ID_CHECK ) )
		{
			lowIntensityTrack = ts0;
			highIntensityTrack = ts1;
		}else
		{
			lowIntensityTrack = ts1;
			highIntensityTrack = ts0;
		}

		if ( LiveMouseTracker.trackContainer.animalTrackSegmentPool.getAnimalOwningTrack( highIntensityTrack ) == blackestAnimal )
		{
			// wrong association > set to anonymous
			LiveMouseTracker.trackContainer.setTrackAnonymous( blackestAnimal,
					LiveMouseTracker.getT()-LiveMouseTracker.MIN_TRACK_LENGTH_TO_PROCESS_ID_CHECK, LiveMouseTracker.getT() );
//			LiveMouseTracker.addEvent( new Event( "BW SET ANONYMOUS ", Color.green, new Point2D.Double( 200, 200 ) ));

		}

		if ( LiveMouseTracker.trackContainer.animalTrackSegmentPool.getAnimalOwningTrack( lowIntensityTrack ) == whitestAnimal )
		{
			// wrong association > set to anonymous
			LiveMouseTracker.trackContainer.setTrackAnonymous( whitestAnimal,
					LiveMouseTracker.getT()-LiveMouseTracker.MIN_TRACK_LENGTH_TO_PROCESS_ID_CHECK, LiveMouseTracker.getT() );
//			LiveMouseTracker.addEvent( new Event( "BW SET ANONYMOUS ", Color.green, new Point2D.Double( 200, 200 ) ));
		}

		if ( LiveMouseTracker.trackContainer.animalTrackSegmentPool.getAnimalOwningTrack( highIntensityTrack ) == null )
		{
			// the track is anonymous. Set it.
			LiveMouseTracker.trackContainer.setTrackIdentity( highIntensityTrack , whitestAnimal, "_BW" );
		}

		if ( LiveMouseTracker.trackContainer.animalTrackSegmentPool.getAnimalOwningTrack( lowIntensityTrack ) == null )
		{
			// the track is anonymous. Set it.
			LiveMouseTracker.trackContainer.setTrackIdentity( lowIntensityTrack , blackestAnimal, "_BW" );
		}



		//swapCurrentAnimalTracks( whiteAnimal , blackAnimal );

		// check existing track and also anonymous tracks.

		// check anonymous tracks:
//		{
//			for ( TrackSegment track : trackContainer.anonymousTrackSegmentPool.trackSegmentArrayList )
//			{
//				if ( track.getLength() < LiveMouseTracker.MIN_TRACK_LENGTH_TO_PROCESS_ID_CHECK ) continue;
//
//				double sumIntensity = 0;
//				double nb = 0;
//				for ( MouseDetection detection : track.getDetectionList() )
//				{
//					sumIntensity += detection.getMeanInfraIntensity();
//					nb++;
//				}
//				if ( nb > 0 )
//				{
//					double mean = sumIntensity / nb;
//					if ( mean > 100 ) // WHITE MOUSE
//					{
//						Animal animalWhite = trackContainer.animalTrackSegmentPool.animalList.get( 0 );
//						LiveMouseTracker.trackContainer.setTrackIdentity( track , animalWhite, "_IR" );
//					}else
//					{
//						Animal animalBlack = trackContainer.animalTrackSegmentPool.animalList.get( 0 );
//						LiveMouseTracker.trackContainer.setTrackIdentity( track , animalBlack, "_IR" );
//					}
//				}
//			}
//		}
		// check existing track

		/*
		ArrayList<TrackSegment> identifiedTracks = trackContainer.getAllTracks( LiveMouseTracker.getT() );
		{
			for ( TrackSegment tsCandidate: identifiedTracks )
			{
				Animal owner = trackContainer.animalTrackSegmentPool.getAnimalOwningTrack( tsCandidate );
				if ( owner == null ) continue;

				Double mean = tsCandidate.getMeanIntensity( 30 );
				if ( mean != null )
				{
					if ( mean > 100 && owner.dataBaseId!=1 ) // WHITE MOUSE
					{
						LiveMouseTracker.trackContainer.setTrackAnonymous( owner, tsCandidate.getFirstTimePoint() , tsCandidate.getLastTimePoint() );
						LiveMouseTracker.trackContainer.setTrackAnonymous( animalWithDetection , trackRFID );
						LiveMouseTracker.trackContainer.setTrackIdentity( trackRFID, animalOwningRFID, "RFID" );

					}else
					{
//						Animal animalBlack = trackContainer.animalTrackSegmentPool.animalList.get( 0 );
//						LiveMouseTracker.trackContainer.setTrackIdentity( track , animalBlack, "_IR" );
					}
				}
			}
		}
		*/


	}

}
