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
package plugins.fab.livemousetracker.rfid;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import plugins.fab.livemousetracker.Animal;
import plugins.fab.livemousetracker.LiveMouseTracker;
import plugins.fab.livemousetracker.detection.MouseDetection;
import plugins.fab.livemousetracker.experiment.EventLog;
import plugins.fab.livemousetracker.overlay.Event;
import plugins.fab.livemousetracker.track.AnonymousPool;
import plugins.fab.livemousetracker.track.TrackSegment;

/**
 * Associates RFID measures to track association decision.
 */
public class RFIDSolver2 {

	public static void rfidManagment( // ArrayList<RawMouseDetection> rawMouseDetectionList ,
			RFIDManager2 rfidManager ) {

		// manage rfid events in queue.

		if ( !rfidManager.getEventList().isEmpty() )
		{
			LiveMouseTracker.disableCurrentMachineLearning();
		}

		for ( AntennaReadEvent rfidEvent : rfidManager.getEventList() )
		{
			manageEvent( rfidEvent ); //, rawMouseDetectionList );
		}

		rfidManager.clearEvents();

	}

	private static void manageEvent(AntennaReadEvent rfidEvent //,
			//ArrayList<RawMouseDetection> rawMouseDetectionList
			)
	{
//		System.out.println("Working with RFID Event " + rfidEvent );

//		int difT = Math.abs( rfidEvent.getCorrectedT() - LiveMouseTracker.getT() );
//		if ( difT > 10 )
//		{
//			System.out.println("Discarding RFID event: too far from current t: dif(t)=" + difT );
//			return;
//		}

		Point2D rfidLocation = rfidEvent.getLocation();
		Event event = new Event( rfidEvent.getRFID(), Color.WHITE, rfidLocation );
//		LiveMouseTracker.addEvent( event );

		// check if only 1 rawDetection is around the location of the antenna.

		MouseDetection bestDetection = null;
//		TrackSegment bestTrack = null;

		float ray = rfidEvent.getRay();

		// Get all tracks in previous time windows
		ArrayList<TrackSegment> trackList = LiveMouseTracker.trackContainer.getAllTracks( rfidEvent.getCorrectedT() );
		ArrayList<MouseDetection> mouseDetectionList = new ArrayList<MouseDetection>();
		// create the detection list.
		for ( TrackSegment ts : trackList )
		{
			mouseDetectionList.add( ts.getDetection( rfidEvent.getCorrectedT() ) );
		}

		for ( MouseDetection detection : mouseDetectionList )
		{
			if ( isDetectionCloseToRFID( detection, rfidLocation, ray * LiveMouseTracker.RFID_ANTENNA_RAY_MULTIPLICATOR ) )
			{
				if ( bestDetection == null )
				{
					bestDetection = detection;
				}else
				{
					// 2 detections have been found at the same time for the same antenna.
					// No conclusion can be set. Drop the info.
//					event.setText("RFID: Too many animals closed to the RFID");
//					System.out.println("[RFID] Too many animals closed to the RFID");
					return;
				}
			}
		}


		// check how many tracks interact with RFID
//		int nbTrackCloseToRFID = 0;
//		TrackSegment RFIDTrackSegment = null;
//		for ( TrackSegment ts : trackList )
//		{
//			System.out.println("[RFID] Checking track " + ts );
//			for ( int t = LiveMouseTracker.getT() -10 ; t <= LiveMouseTracker.getT() ; t++ )
//			{
//				RawMouseDetection detection = ts.getDetection( t );
//				if ( detection!= null )
//				if ( isDetectionCloseToRFID( detection , rfidLocation , ray ) )
//				{
//					RFIDTrackSegment = ts;
//					nbTrackCloseToRFID++;
//					break;
//				}
//			}
//		}
//
//		System.out.println("[RFID] Number of interacting track found: " + nbTrackCloseToRFID );
//
//		if ( nbTrackCloseToRFID > 1 )
//		{
//			event.setText("RFID: Too many tracks closed to the RFID");
//			return;
//		}

		// Check for isolated detection, in time and space.
//		for ( int t = LiveMouseTracker.getT() -10 ; t <= LiveMouseTracker.getT() ; t++ )
		{
//			ArrayList<RawMouseDetection> detectionList = TrackContainer.getAllDetectionAt( t );
//			for ( RawMouseDetection detection : detectionList )
			// USED WHEN TIME WINDOW WAS NOT CONSIDERED
//			for ( RawMouseDetection detection : rawMouseDetectionList )
//			{
//				//if ( detection.getMassCenter().distance( rfidLocation ) < ray * 2 )
//				if ( isDetectionCloseToRFID( detection, rfidLocation, ray ) )
//				{
//					if ( bestDetection == null )
//					{
//						bestDetection = detection;
//					}else
//					{
//						// 2 detections have been found at the same time for the same antenna.
//						// No conclusion can be set. Drop the info.
//						event.setText("RFID: Too many animals closed to the RFID");
//						System.out.println("[RFID] Too many animals closed to the RFID");
//						return;
//					}
//				}
//			}
		}

		if ( bestDetection == null )
		{
//			event.setText("[RFID]: No detection found");
			return;
		}

		// Show which detection is processed ( important as there is now a latency effect to consider ! )
//		Event event = null;
//		Event eventDetection = new Event("Det(RFID)", Color.PINK, bestDetection.getMassCenter().toPoint2D() , 1 );
//		LiveMouseTracker.addEvent(eventDetection);

//		System.out.println("[RFID]: Processing association");
		// check who is owning the detection.
		Animal animalWithDetection = LiveMouseTracker.getMainAnimalPool().getAnimalWithDetection( bestDetection );

		if ( animalWithDetection != null )
		{
//			System.out.println("[RFID] Process track own by animal");
			processTrackOwnByAnAnimal( animalWithDetection, rfidEvent, event , bestDetection );
		}else
		{
//			System.out.println("[RFID] Process track anonymous");
			processTrackAnonymous( animalWithDetection, rfidEvent, event , bestDetection );
		}
//		System.out.println("[RFID] End");
	}

	private static boolean isDetectionCloseToRFID(MouseDetection detection,
			Point2D rfidLocation, float ray) {

		boolean test = detection.getMinDistanceToShape( rfidLocation ) < ray;
		//boolean test = detection.getMassCenter().distance( rfidLocation ) < ray ;

//		System.out.println("Testing detection " + detection +" too close=" + test );

		return test ;
	}

	private static void processTrackAnonymous(
			Animal animalWithDetection, AntennaReadEvent rfidEvent, Event event, MouseDetection bestDetection ) {
		{
			// THE TRACK TO BE ASSOCIATED IS IN THE TRACKPOOL

			// FIXME: SHOULD STOP MACHINE LEARNING SOLVERS TO AVOID ERROR ?
			// FIXME: OR THE SOLVER SHOULD CHECK THAT THE TRACKS ARE NOW ASSOCIATED ?

			AnonymousPool anonymousTrackSegmentPool = LiveMouseTracker.trackContainer.anonymousTrackSegmentPool;
			TrackSegment trackSegment = anonymousTrackSegmentPool.getTrackSegmentsContaining( bestDetection );
			if ( trackSegment == null )
			{
				event.setText("No track segment to match");
//				System.out.println("[RFID] No trackSegment to watch.");
				return;
			}

			// associate if the RFID is associated to animal.
			Animal animal =  LiveMouseTracker.getMainAnimalPool().getAnimalWithRFID( rfidEvent.getRFID() );
			if ( animal == null )
			{
//				System.out.println("[RFID] Animal with this RFID not known yet.");

				// assign a free animal to this RFID.

				Animal animalWithoutRFID = LiveMouseTracker.getMainAnimalPool().getFirstAnimalWithoutRFID();
				if ( animalWithoutRFID == null )
				{
					event.setText("ERROR: New RFID found but max animal number already reached !");
				}else
				{
					animalWithoutRFID.setRfidID( rfidEvent.getRFID() );
					animal = animalWithoutRFID;
				}

				// means it cannot be the ones having an RFID already.
//				String cannotBeString ="";
//				for ( Animal a : LiveMouseTracker.getMainAnimalPool().getAnimalList() )
//				{
//					if ( a.hasRfidID() )
//					{
//						trackSegment.addCannotBeAnimal( a );
//						cannotBeString+= a+" ";
//					}
//				}
//				event.setText( "cannotBe:" + cannotBeString );

				// no animal exists with this RFID.
				// Create it !
//				Animal animalFree =  LiveMouseTracker.getMainAnimalPool().getFreeAnimal();
//				if ( animalFree != null )
//				{
//					event.setText("Create new animal");
//					trackSegmentPool.remove( trackSegment );
//					animalFree.addTrackSegment( trackSegment );
//					animalFree.setRfidID( rfidEvent.getId() );
//				}
			}

			if ( animal != null )
			{
				// set the track to the animal

				// FIXME: CHECK IF THIS IS WORKING
				//TrackContainer.setTrackIdentity( trackSegment, animal );

				// If the animal does not have this tracksegment,
				// then remove the tracksegment he is currently having, which is obviously a wrong association.
				if ( !animal.contain( trackSegment ) )
				{
					LiveMouseTracker.trackContainer.setTrackAnonymous(
							animal, trackSegment.getFirstTimePoint(), trackSegment.getLastTimePoint() );
				}
				event.setText("Must be: " + animal );
				LiveMouseTracker.addEventLogToDataBase( new EventLog( "RFID ASSIGN ANONYMOUS TRACK" , animal ) );
				trackSegment.mustBe( animal );
				trackSegment.nbFrameSinceLastRFIDReading = 0;

//				trackSegmentPool.remove( trackSegment );
//				animal.addTrackSegment( trackSegment );

//				LiveMouseTracker.trackContainer.animalTrackSegmentPool.co
//				correctTrackWithAnimal(animalWithDetection, track);

				// TODO: Add a check for double animal assignation (2 tracks with same animal).
				LiveMouseTracker.trackContainer.setTrackIdentity( trackSegment, animal, "RFID" );
				//Anonymous( animalWithDetection , trackRFID );

//				System.out.println("Animal MustBe: " + animal );
			}
		}


	}

	private static void processTrackOwnByAnAnimal(
			Animal animalWithDetection, AntennaReadEvent rfidEvent, Event event, MouseDetection bestDetection) {

		if ( animalWithDetection == null )
		{
//			System.err.println("[RFID]Process track own by animal: Animal cannot be null.");
			return;
		}

		TrackSegment trackRFID = animalWithDetection.getTrackContainingDetection( bestDetection );

		if ( trackRFID == null )
		{
//			System.err.println("[RFID]Process track own by animal: the track associated with RFID cannot be null.");
			return;
		}

		// THE ANIMAL IS KNOWN
//		System.out.println("[RFID] Animal detected in tracking is: " + animalWithDetection.getName() );
		Animal animalOwningRFID = LiveMouseTracker.getMainAnimalPool().getAnimalWithRFID( rfidEvent.rfid );
//		System.out.println("[RFID] Animal owning RFID " + rfidEvent.id + " is: " + animalOwningRFID );

		// animal are the same. RFID match.
		if ( animalWithDetection == animalOwningRFID )
		{
//			System.out.println("[RFID] Match" + animalWithDetection + "/" + animalOwningRFID );
			event.setText("RFID Match" );
			trackRFID.nbFrameSinceLastRFIDReading = 0;
			LiveMouseTracker.addEventLogToDataBase( new EventLog( "RFID MATCH" , animalWithDetection ) );
			event.setColor( Color.green );

			trackRFID.mustBe( animalOwningRFID );
			return;
		}

		boolean assignSuccess = LiveMouseTracker.getMainAnimalPool().setRFID( animalWithDetection, rfidEvent.getRFID() );

		if ( assignSuccess )
		{
//			System.out.println("[RFID] RFID assigned to animal " + animalWithDetection.getName() );
			// animal has received the RFID with no conflict.
			event.setText("RFID Assigned");
			event.setColor( Color.yellow );
			return;
		}


		//
		if ( animalOwningRFID != null )
		{
			// An animal with this RFID affected exists.
			// The track should then be affected to this animal.
			// 1. We set the track as 'must be' with the animal.
			// 2. We set the track anonymous
			event.setText("RFID MisMatch/ must be " + animalOwningRFID );

			event.setColor( Color.red );
			// TODO: SPLIT TRACK BEFORE THIS.
			// TODO: Must be only the good part of the track !!


			trackRFID.mustBe( animalOwningRFID );
			trackRFID.addCannotBeAnimal( animalWithDetection );

//			event.setText("MUST BE: " + animalOwningRFID );

			LiveMouseTracker.trackContainer.setTrackAnonymous(
					animalOwningRFID, trackRFID.getFirstTimePoint() , trackRFID.getLastTimePoint() );
			LiveMouseTracker.trackContainer.setTrackAnonymous( animalWithDetection , trackRFID );
			LiveMouseTracker.trackContainer.setTrackIdentity( trackRFID, animalOwningRFID, "RFID" );
			trackRFID.nbFrameSinceLastRFIDReading = 0;
			LiveMouseTracker.addEventLogToDataBase( new EventLog( "RFID MISMATCH" , animalWithDetection ) );

		}else
		{			
			System.err.println("* [RFIDSOLVER 2]");
			//System.err.println("** This should not happend anymore. It is managed by the ASSIGN SCENARIO before.");
			String rfid = rfidEvent.getRFID();
			System.err.println("** There is a new animal not expected.. adding animal and its rfid " + rfid );
			LiveMouseTracker.getMainAnimalPool().addDynamicAnimal( rfid );
			
			
			// No animal own this RFID
			// So, this animal cannot be an animal who has already an RFID.
			/*
			String cannotBeString ="";
			for ( Animal animal : LiveMouseTracker.getMainAnimalPool().getAnimalList() )
			{
				if ( animal.hasRfidID() )
				{
					trackRFID.addCannotBeAnimal( animal );
					cannotBeString+= animal+" ";
				}
			}
			event.setText("RFID Miss Match + cannot be: " + cannotBeString );
			LiveMouseTracker.trackContainer.setTrackAnonymous( animalWithDetection , trackRFID );

//			LiveMouseTracker.trackContainer.setTrackIdentity( trackRFID, animalOwningRFID, "RFID" );

			LiveMouseTracker.addEventLogToDataBase( new EventLog( "RFID MISMATCH" , animalWithDetection ) );
			*/
		}


//		// no animal has this RFID yet. Assign it.
//		if ( animalOwningRFID == null )
//		{
//			if ( LiveMouseTracker.getMainAnimalPool().setRFID( animalWithDetection, rfidEvent.getId() ) )
//			{
//				// RFID ASSIGNED
//				System.out.println("[RFID] RFID assigned to animal " + animalWithDetection.getName() );
//				// animal has received the RFID with no conflict.
//				event.setText("RFID Assigned");
//				event.setColor( Color.green );
//				return;
//			}
//			else
//			{
//				// ANOTHER ANIMAL IS ALREADY USING THIS RFID.
//				System.out.println("[RFID] ID Affectation Error: Making track anonymous.");
//				TrackSegment ts = animalWithDetection.getTrackContainingDetection( bestDetection );
//
//				TrackContainer.setTrackAnonymous( animalWithDetection, ts.getFirstTimePoint() , ts.getLastTimePoint() );
//			}
//		}



//		event.setText("RFID Miss Match");
//		event.setColor( Color.red );
//		System.out.println("[RFID]Transfer ownerShip.");
//		TrackContainer.transferOwnerShipWithRFID( animalWithDetection , animalOwningRFID, bestDetection );


//		if ( animalWithDetection.getRfidID() == null )
//		{
//			// Set the RFID value if not already set.
//			System.out.println("[RFID] Animal " + animalWithDetection.getName() + " has no RFID assignment yet.");
//			// Check if an animal is already using this RFID
//			if ( LiveMouseTracker.getMainAnimalPool().setRFID( animalWithDetection, rfidEvent.getId() ) )
//			{
//				System.out.println("[RFID] Rfid assigned to animal " + animalWithDetection.getName() );
//				// animal has received the RFID with no conflict.
//				event.setText("RFID Assigned");
//			}else
//			{
//				// The animal cannot be this one as this id is already used by another animal.
//				System.out.println("[RFID]: Swapping track ownership");
//				TrackContainer.transferOwnerShipWithRFID( bestDetection, rfidEvent.getId() );
//
//				event.setText("RFID: swapping track ownership");
//			}
//		}
//		else
//		{
//			System.out.println("[RFID] Animal " + animalWithDetection.getName() + " has already an RFID assignment.");
//
//			// Animal has already an RFID
//			if ( animalWithDetection.getRfidID().equals( rfidEvent.getId() ))
//			{
//				// RFID are the same
//				System.out.println("[RFID] Match");
//				event.setText("RFID Match");
//				event.setColor( Color.green );
//			}else
//			{
//				// RFID are not the same, there is an error.
//				System.out.println("[RFID] Miss Match");
//				event.setText("RFID Miss Match");
//				event.setColor( Color.red );
//				TrackContainer.transferOwnerShipWithRFID( bestDetection, rfidEvent.getId() );
//
//			}
//		}

	}

}
