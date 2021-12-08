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

import java.util.ArrayList;

import icy.system.profile.Chronometer;
import icy.system.thread.Processor;
import plugins.fab.livemousetracker.Animal;
import plugins.fab.livemousetracker.LiveMouseTracker;
import plugins.fab.livemousetracker.overlay.Message;
import plugins.fab.livemousetracker.overlay.PerfLoggerOverlay;
import plugins.fab.livemousetracker.track.AnonymousPool;
import plugins.fab.livemousetracker.track.TrackSegment;

public class Identifier extends Thread {

	AnonymousPool trackSegmentPool ;
	TrackSegment trackSegmentToProcess ;

	boolean finished = false;

	public boolean isFinished()
	{
		return finished;
	}

	public Identifier( AnonymousPool trackSegmentPool , TrackSegment trackSegmentToProcess ) {
		this.trackSegmentPool = trackSegmentPool;
		this.trackSegmentToProcess = trackSegmentToProcess;
	}

	@Override
	public void run() {

		setPriority( LiveMouseTracker.SECONDARY_THREAD_PRIORITY );
		Chronometer chrono = null;

//		Message message = PerfLoggerOverlay.addMessage( new Message( "Identifier agent." ));

		try
		{

			if ( LiveMouseTracker.LOG_IDENTIFIER )
			{
				System.out.println("[IDENTIFIER] Starting identification thread " + (Object)this );
			}

			if ( LiveMouseTracker.LOG_IDENTIFIER )
			{
				chrono = new Chronometer("[IDENTIFIER]Identification thread chrono " + (Object)this );
			}

			TrackSegment tsCandidate = trackSegmentToProcess;

//			if ( findObviousTrackAssociation( tsCandidate ) )
//			{
//				return;
//			}


			// 1. Start with a given track of list 1
			// 2. Find number of concurrent of tracks to build problem, "transfer/check flag them as processed" in list 2
			// 3. solve problem
			// 4. update lists and do it again until no tracks to recover id.

			//TrackSegment tsCandidate = trackSegmentPool.getTrackSegments().get( trackSegmentPool.getTrackSegments().size()-1 );

			// Select the track to solve

			//TrackSegment tsCandidate = trackSegmentPool.getTrackSegments().get( (int)( Math.random() * trackSegmentPool.getTrackSegments().size() -1 ) );

			// FIXME ANDTEST: Should be < -1, so it is 2 time point before
			if ( tsCandidate.getLastTimePoint() < LiveMouseTracker.getT() - 1 ) // if the candidate is an old and finished track
			{
				boolean dropTrack = false;

				if ( tsCandidate.getLength() < LiveMouseTracker.MIN_TRACK_LENGTH_TO_PROCESS_ID_CHECK )
				{
					dropTrack = true;
				}

				if ( LiveMouseTracker.PROCESS_ID_CHECK_ON_OLD_TRACK_EVEN_WITH_WRONG_LENGTH )
				{
					dropTrack = false;
				}

				if ( dropTrack )
				{
					trackSegmentPool.removeTrack( tsCandidate );
					if ( LiveMouseTracker.LOG_IDENTIFIER )
					{
						System.out.println("[identifier] Track segment old and too small > drop track.");
					}
					return;
				}
			}

			//System.out.println("Identity recovery: candidate track " + tsCandidate );

			// Create the list of track involved in the problem: means overlapping in time.

			ArrayList<TrackIdentityScorer> trackIdentityProblemList = new ArrayList<TrackIdentityScorer>();

			trackIdentityProblemList.add( new TrackIdentityScorer( tsCandidate ) );

			for ( TrackSegment ts: trackSegmentPool.getTrackSegments() )
			{
				if ( ts == tsCandidate ) continue;

				if ( ts.overlapInT( tsCandidate ) )
				{
					trackIdentityProblemList.add( new TrackIdentityScorer( ts ) );
				}
			}

			for ( TrackIdentityScorer tip : trackIdentityProblemList )
			{

				if ( LiveMouseTracker.LOG_IDENTIFIER )
				{
					//System.out.println("track length: " + tip.getTrack().getLength() );
				}

				if ( LiveMouseTracker.PROCESS_ID_CHECK_ON_OLD_TRACK_EVEN_WITH_WRONG_LENGTH )
				{
					if ( tip.getTrack().getLastTimePoint() < LiveMouseTracker.getT() -1 )
					{
						continue; // therefore bypass length test
					}
				}

				if ( tip.getTrack().getLength() < LiveMouseTracker.MIN_TRACK_LENGTH_TO_PROCESS_ID_CHECK )
				{
					if ( LiveMouseTracker.LOG_IDENTIFIER )
					{
						//System.out.println("Problem dropped: too small tracks. (<"+LiveMouseTracker.MIN_TRACK_LENGTH_TO_PROCESS_ID_CHECK+")" );
					}
					return;
				}

			}

			// For each identity problem track, find possibles animal identities.

			for ( TrackIdentityScorer tip : trackIdentityProblemList )
			{
				// first add all animals.
				tip.getCandidateAnimalList().addAll( LiveMouseTracker.getMainAnimalPool().getAnimalList() );

				// then remove them if they are having a track at an overlapped time point with the problem.
				for ( Animal animal : LiveMouseTracker.getMainAnimalPool().getAnimalList() )
				{
					for ( TrackSegment tsAnimal : animal.getTrackSegments() )
					{
						if ( tsAnimal == null ) continue;

						if ( tsAnimal.overlapInT(
								tip.getTrack() ) )
						{
							tip.getCandidateAnimalList().remove( animal );
						}
					}
				}

				tip.applyTrackConstraints();
				//System.out.println( tip );
			}

			// Compute individual detection track score.

			Processor p = new Processor( );
			p.setPriority( LiveMouseTracker.SECONDARY_THREAD_PRIORITY );
			p.setThreadName("ML: Compute individual detection Track Score");
			for ( TrackIdentityScorer tip : trackIdentityProblemList )
			{
				// FIXME : HERE WE SHOULD ADD SOMETHING TO AVOID THE LAUNCH OF A LOT OF TIP THAT WILL WORK ON SAME CACHE
				// CAUSE THEY WILL ALL LAUNCH IT AT THE SAME TIME, AND THE CACHE WILL NOT WORK.
//				p.submit( tip );

//				p.waitAll(); // FIXME: REMOVES THE MULTITHREAD thanks to this waitAll.


//				if( LiveMouseTracker.LOG_CHAIN ) // REMOVE MultiThreading in log mode.
//				{
//					p.waitAll();
//				}
//			}
				tip.run(); // on main thread. TO CHECK !!
			}
//			p.waitAll();


			//		for ( TrackIdentityProblem tip : trackIdentityProblem )
			//		{
			//			tip.computeDetectionScores();
			//			if ( tip.scoreList.size() == 0 )
			//			{
			//				return;
			//			}
			//		}

			// Compute all association score in another thread.

			if ( disabled )
			{
				if ( LiveMouseTracker.LOG_IDENTIFIER )
				{
					//System.out.println("identity agent abort. reason: disabled.");
				}
				return;
			}

			TrackIdentityGlobalSolver tipGP = new TrackIdentityGlobalSolver( trackIdentityProblemList , this );

			try {
				// Anti flood:
				// Wait a little before giving hand back to avoid too much queries in very short period of time.
				Thread.sleep( 100 );
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}finally
		{
//			PerfLoggerOverlay.removeMessage( message );

			if ( LiveMouseTracker.LOG_IDENTIFIER )
			{
				chrono.displayInSeconds();
			}
			//			System.out.println("----");
			//			System.out.println("DONE");
			//			System.out.println("----");
			finished = true;
		}
	}

//	/** tries to associate obvious tracks. */
//	private boolean findObviousTrackAssociation(TrackSegment tsCandidate) {
//
////		System.out.println("Enter find obvious association.");
//		{
//			// if track has no concurrent.
//			ArrayList<Animal> potentialAnimalList = new ArrayList<>( LiveMouseTracker.getMainAnimalPool().getAnimalList() );
//			for ( Animal animal : LiveMouseTracker.getMainAnimalPool().getAnimalList() )
//			{
//				for ( TrackSegment tsAnimal : animal.getTrackSegments() )
//				{
//					if ( tsAnimal.overlapInT( tsCandidate ) )
//					{
//						potentialAnimalList.remove( animal );
//					}
//				}
//			}
//			if ( potentialAnimalList.size() == 1 )
//			{
//				LiveMouseTracker.trackContainer.setTrackIdentity( tsCandidate , potentialAnimalList.get( 0 ) );
//				System.out.println("Obvious tracking association.");
//				return true;
//			}
//		}
//
//		return false;
//	}


	boolean disabled = false;

	public void disable() {
		disabled = true;
	}




}
