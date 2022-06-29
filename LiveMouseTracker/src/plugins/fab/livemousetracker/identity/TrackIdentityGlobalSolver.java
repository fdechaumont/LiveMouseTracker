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
import java.awt.geom.Point2D;
import java.util.ArrayList;

import icy.main.Icy;
import plugins.fab.livemousetracker.Animal;
import plugins.fab.livemousetracker.LiveMouseTracker;
import plugins.fab.livemousetracker.detection.MouseDetection;
import plugins.fab.livemousetracker.experiment.EventLog;
import plugins.fab.livemousetracker.overlay.Event;
import plugins.fab.livemousetracker.track.TrackSegment;
import plugins.kernel.roi.roi2d.ROI2DRectangle;

public class TrackIdentityGlobalSolver {

	ArrayList<Chain> chainList = new ArrayList<Chain>( );

	public TrackIdentityGlobalSolver(
			ArrayList<TrackIdentityScorer> trackIdentityProblemList, Identifier identifier ) {

		//System.out.println("Track identity problem size: " + trackIdentityProblemList.size() );

		try
		{
//			 FIXME: Should be removed as this test is performed elsewhere (in identifier.java)
//			for ( TrackIdentityProblem tip : trackIdentityProblemList )
//			{
//				System.out.println("track length: " + tip.getTrack().getLength() );
//
//				if ( tip.getTrack().getLength() < LiveMouseTracker.MIN_TRACK_LENGTH_TO_PROCESS_ID_CHECK )
//				{
//					System.out.println("THIS SHOULD NOT APPEAR");
//					System.out.println("Problem dropped: too small tracks. (<"+LiveMouseTracker.MIN_TRACK_LENGTH_TO_PROCESS_ID_CHECK+")" );
//					return;
//				}
//
//			}

//			for ( TrackIdentityProblem tip : trackIdentityProblemList )
//			{
//				MouseDetection detection = tip.getTrack().getDetectionList().get( 0 );
//				{
//					String animalStringList ="";
//					for ( Animal animal : tip.getCandidateAnimalList() )
//					{
//						animalStringList+= animal.getName() + " ";
//					}
//
////					Event eventIdentity = new Event( "Tip: "+animalStringList, Color.GREEN, new Point2D.Double( detection.getMassCenter().getX() , detection.getMassCenter().getY()  ) );
////					LiveMouseTracker.addEvent( eventIdentity );
////					eventIdentity.setCanRemove( true );
//				}
//			}

			recurs( trackIdentityProblemList , 0 , new Chain() );

			// compute all scores

			for ( Chain chain : chainList )
			{
				chain.computeScore();
			}

			// Check best score
			//System.out.println("Score list:");
			double bestScore = 0;
			Chain bestChain = null;
			double totalScore = 0;
			for ( Chain chain : chainList )
			{
				if ( chain.getScore() == null ) continue; // the chain could not be calculated

				if( LiveMouseTracker.LOG_CHAIN )
				{
					//System.out.println( chain );
				}
				if ( chain.getScore() > bestScore )
				{
					bestScore = chain.getScore();
					bestChain = chain;
				}
				totalScore+= chain.getScore();
			}
			if ( bestChain == null )
			{
				//System.out.println("No best chain found. ChainList size: " + chainList.size() );

				return;
			}

			//System.out.println("Best chain: " + bestChain );
			double proportion = 100d * bestChain.getScore() / totalScore;
			//System.out.println("Best chain score : " + bestChain.getScore() + " over a total of " + totalScore + " Prop= " + proportion + "%");

//			System.out.println("Tracking association decision would be: ");
			for ( TrackIdentityProblemToAnimal tipToAnimal : bestChain.tipToAnimalList )
			{
				if( LiveMouseTracker.LOG_CHAIN ){
					//System.out.println( tipToAnimal.animal.getName() );
				}
				TrackSegment ts = tipToAnimal.tip.getTrack();
				Point2D massCenter = ts.getDetection( ts.getLastTimePoint() ).getMassCenter().toPoint2D();
				String txt = tipToAnimal.animal.getName() + ": " + massCenter;

				if ( LiveMouseTracker.LOG_CHAIN ){
					//System.out.println( txt );
				}

//				ROI2DRectangle roi = new ROI2DRectangle( massCenter );
//				roi.setName( "tmp " + tipToAnimal.animal.getName() + " " + (int)proportion + "%" );
//				roi.setShowName( true );
//				roi.setColor( tipToAnimal.animal.getColor() );
//				try{
//					Icy.getMainInterface().getActiveSequence().addROI( roi );
//				}catch( NullPointerException e )
//				{
					// The sequence cannot be found
//				}
			}

			if ( proportion > LiveMouseTracker.LEARNING_ID_ASSOCIATION_PROPORTION_THRESHOLD )
			{
				//System.out.println("Taking association decision.");
				LiveMouseTracker.number_of_auto_correction++;
				for ( TrackIdentityProblemToAnimal tipToAnimal : bestChain.tipToAnimalList )
				{
					if( !identifier.disabled )
					{
						LiveMouseTracker.trackContainer.setTrackIdentity(
								tipToAnimal.tip.getTrack() , tipToAnimal.animal, "ML" );
						LiveMouseTracker.addEventLogToDataBase( new EventLog("MACHINE LEARNING ASSOCIATION", tipToAnimal.animal ));
					}else
					{
						//System.out.println("set track identity disabled.");
					}
//					LiveMouseTracker.setTrackIdentity( tipToAnimal.tip.getTrack() , tipToAnimal.animal );
				}

			}

			//		eventIdentity.setCanRemove( true );

			//		Event event = new Event( "Asso: " + (int)proportion, Color.PINK, new Point2D.Double( 500/2,424/3 ) );
			//		event.setCanRemove( true );
			//		LiveMouseTracker. event );

//			for ( TrackIdentityProblem tip : trackIdentityProblemList )
//			{
//				MouseDetection detection = tip.getTrack().getDetectionList().get( 0 );
//				{
////					Event eventIdentity = new Event( "Asso: " + (int)proportion, Color.ORANGE , new Point2D.Double( detection.getMassCenter().getX() , detection.getMassCenter().getY()  ) );
////					LiveMouseTracker.addEvent( eventIdentity );
////					eventIdentity.setCanRemove( true );
//				}
//			}

		}finally
		{
//			for ( TrackIdentityProblem tip : trackIdentityProblemList )
//			{
//				tip.getTrack().setBlink( false );
//			}

			//			for ( ROI roi : roiList )
			//			{
			//				LiveMouseTracker.removeROIToInfraSequence( roi );
			//			}
		}

	}

	public Chain recurs( ArrayList<TrackIdentityScorer> trackIdentityProblemList ,
			int depth , Chain chain ) // String result
	{
		if ( chain == null ) return null ;
		if ( !chain.isChainValid() ) return null;

		if ( trackIdentityProblemList.size() == depth )
		{
			//System.out.println( chain );
			chainList.add( chain );
			return null; // stop propagating
		}

		TrackIdentityScorer tip = trackIdentityProblemList.get( depth );

		for ( Animal animal : tip.getCandidateAnimalList() )
		{
			recurs ( trackIdentityProblemList , depth+1 ,
					new Chain( chain , new TrackIdentityProblemToAnimal( animal, tip ) ) );
					//new String( result ) + animal.getName() );
		}

		return chain; // result

	}


}
