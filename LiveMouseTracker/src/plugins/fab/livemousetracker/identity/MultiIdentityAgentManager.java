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
package plugins.fab.livemousetracker.identity;

import java.awt.Color;
import java.util.ArrayList;

import icy.system.profile.Chronometer;
import plugins.fab.livemousetracker.Animal;
import plugins.fab.livemousetracker.LiveMouseTracker;
import plugins.fab.livemousetracker.detection.MouseDetection;
import plugins.fab.livemousetracker.overlay.Event;
import plugins.fab.livemousetracker.overlay.Message;
import plugins.fab.livemousetracker.overlay.PerfLoggerOverlay;
import plugins.fab.livemousetracker.track.TrackSegment;
import plugins.fab.livemousetracker.track.AnonymousPool;

public class MultiIdentityAgentManager {

//	Identifier identifier = null;

	Identifier identifierArray[] = null;

	// Number of identifier.
	// FIXME: Multiple identifier can lead to double/conflicting id affectation !
	int NB_MAX_IDENTIFIER = 2;

	public MultiIdentityAgentManager() {

		identifierArray = new Identifier[NB_MAX_IDENTIFIER];
	}

	/** tries to associate obvious tracks. */
	private boolean findObviousTrackAssociation(TrackSegment tsCandidate) {

//		System.out.println("Enter find obvious association.");
		{
			// if track has no concurrent.
			ArrayList<Animal> potentialAnimalList = new ArrayList<Animal>( LiveMouseTracker.getMainAnimalPool().getAnimalList() );
			for ( Animal animal : LiveMouseTracker.getMainAnimalPool().getAnimalList() )
			{
				for ( TrackSegment tsAnimal : animal.getTrackSegments() )
				{
					if ( tsAnimal.overlapInT( tsCandidate ) )
					{
						potentialAnimalList.remove( animal );
					}
				}
			}
			if ( potentialAnimalList.size() == 1 )
			{
				LiveMouseTracker.trackContainer.setTrackIdentity( tsCandidate , potentialAnimalList.get( 0 ), "_ML" );
				System.out.println("Obvious tracking association.");

				try
				{
					MouseDetection lastDetection = tsCandidate.getDetection( LiveMouseTracker.getT()-1 );
					/*
					Event event = new Event( "Obvious association", Color.PINK,
							lastDetection.getMassCenter().toPoint2D() );
					LiveMouseTracker.addEvent( event );
					*/
				}catch( NullPointerException e )
				{
					// There is no detection at the given t point.
				}

				return true;
			}
		}

		return false;
	}

	public void process( AnonymousPool trackSegmentPool )
	{
		Chronometer chrono = new Chronometer("MultiIdentity Chronometer");
//		Message message = PerfLoggerOverlay.addMessage( new Message( "Multi id agent." ));

		try
		{

//		System.out.println("MultiId Processing.");
		{
			// Manage obvious associations
			TrackSegment trackSegmentToProcess = trackSegmentPool.trackSegmentArrayList.get(trackSegmentPool.trackSegmentArrayList.size() -1 );
			if ( findObviousTrackAssociation( trackSegmentToProcess ) )
			{
				return;
			}
		}


//			System.out.println("[MULTI AGENT IDENTIFIER MANAGER] Start.");

			// Drop finished identifier.
			int workingIdentifier = 0;
			for ( int i = 0 ; i < NB_MAX_IDENTIFIER ; i++ )
			{
				Identifier identifier = identifierArray[i];
				if ( identifier != null )
				{
					if ( identifier.isFinished() )
					{
						// drop identifier
						identifier = null;
						identifierArray[i] = null;
					}else
					{
						workingIdentifier++;
					}
				}
			}

			if ( !LiveMouseTracker.USE_MULTIPLE_IDENTITY_RECOVERY_WITH_MACHINE_LEARNING )
			{
				return;
			}

//			System.out.println( "Max Agent: " + NB_MAX_IDENTIFIER + " current t: " + LiveMouseTracker.getT() );
//			System.out.println( "Working Agent-identifier: " + workingIdentifier );

			// Assign new identifier if slot available and problem exists.
			for ( int i = 0 ; i < NB_MAX_IDENTIFIER ; i++ )
			{
				System.out.println("Checking identity agent #"+ i + " / "  );
				chrono.displayMs();

				System.out.println("END SYNCHRONIZED STARTED");
				Identifier identifier = identifierArray[i];
//				System.out.println("Identifier: " + identifier );
				if ( identifier!=null )
				{
//					System.out.println("Is finished: " + identifier.finished );
				}

				boolean startAssociationIdentityProcess = false;

				if ( identifier == null )
				{
					startAssociationIdentityProcess = true;
				}
				//			System.out.println("Start ID process: " + startAssociationIdentityProcess );
				if ( startAssociationIdentityProcess )
				{
					//System.out.println("*************** START IDENTIFICATION PROCESS");
					TrackSegment trackSegmentToProcess = null;

					synchronized (trackSegmentPool.trackSegmentArrayList) {
						System.out.println("SYNCHRONIZED STARTED");
						chrono.displayMs();

						switch (i) {
						case 0: // Will launch a solver on the last track found
//						System.out.println("Strategy: Solving most recent tracks.");
							//trackSegmentToProcess = trackSegmentPool.trackSegmentArrayList.get(trackSegmentPool.trackSegmentArrayList.size() -1 );
							ArrayList<TrackSegment> tracksAtCurrentT = trackSegmentPool.getTrackSegmentsContaining( LiveMouseTracker.getT()-1 );
							if ( tracksAtCurrentT != null )
							{
								if ( tracksAtCurrentT.size() > 0 )
								{
									trackSegmentToProcess = tracksAtCurrentT.get( 0 );
								}
							}
//							System.out.println( "[MultiId] case 0 process: " + trackSegmentToProcess );
							break;

						default:
							// Will launch a solver anywhere in the past
//						System.out.println("Strategy: Solving track in past");
							trackSegmentToProcess = trackSegmentPool.trackSegmentArrayList.get((int)( Math.random() *(trackSegmentPool.trackSegmentArrayList.size() - 1)));
							break;
						}
						chrono.displayMs();
						System.out.println("END SYNCHRONIZED STARTED");
					}

					if ( trackSegmentToProcess == null )
					{
						continue;
					}

					// check if this problem already solved by an identifier
//					System.out.println("Checking co-working of identifier." );
					boolean anIdentifierIsAlreadyWorkingOnThisTrack = false;
					for ( int r = 0 ; r < identifierArray.length ; r++ )
					{
						Identifier id = identifierArray[r];
						if ( id != null )
						{
							if ( id.trackSegmentToProcess == trackSegmentToProcess ) // FIXME: should TEST on all segments managed by this agent, not only the starting on !
							{

								anIdentifierIsAlreadyWorkingOnThisTrack = true;
//								System.out.println("Identifier already working on part of this problem.");
								break;
							}
						}
					}

					if ( !anIdentifierIsAlreadyWorkingOnThisTrack )
					{
						System.out.println("Multi: Starting identifier");
						final TrackSegment trackSegmentToProcessFinal = trackSegmentToProcess;
						final int index = i;
						LiveMouseTracker.threadExecutor.execute( new Runnable() {

							@Override
							public void run() {
								Identifier identifier = new Identifier( trackSegmentPool , trackSegmentToProcessFinal );
								identifierArray[index] = identifier;
//						System.out.println("Identifier is not conflicting.");
//						System.out.println("Starting identifier");
								identifier.setPriority( LiveMouseTracker.SECONDARY_THREAD_PRIORITY );
								identifier.start();
							}
						});
					}
				}
			}
		} finally
		{
//			PerfLoggerOverlay.removeMessage( message );
			// System.out.println("[MULTI AGENT IDENTIFIER MANAGER] Finished.");
			System.out.println("Total multi agent identifier time: "  );
			chrono.displayMs();
		}
		chrono.displayMs();
	}

	public void disableAllCurrentRunningAgent( ) {
		System.out.println("Disable machine learning identity agent.");

		int workingIdentifier = 0;
		for ( int i = 0 ; i < NB_MAX_IDENTIFIER ; i++ )
		{
			Identifier identifier = identifierArray[i];
			if ( identifier != null )
			{
				if ( identifier.isFinished() )
				{
					// drop identifier
					identifier = null;
					identifierArray[i] = null;
				}else
				{
					workingIdentifier++;
					identifier.disable();
				}
			}
		}
		System.out.println("Number of agent disabled: " + workingIdentifier );
	}

}
