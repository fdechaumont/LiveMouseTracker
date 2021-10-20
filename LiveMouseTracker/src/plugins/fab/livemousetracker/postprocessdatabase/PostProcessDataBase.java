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
package plugins.fab.livemousetracker.postprocessdatabase;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import icy.plugin.abstract_.PluginActionable;
import icy.plugin.interface_.PluginThreaded;
import icy.system.SystemUtil;
import icy.system.profile.Chronometer;
import icy.system.thread.Processor;
import icy.util.XMLUtil;
import plugins.fab.livemousetracker.Animal;
import plugins.fab.livemousetracker.dataplayer.DBAnimal;
import plugins.fab.livemousetracker.dataplayer.DataBaseRecomputeEvents;
import plugins.fab.livemousetracker.dataplayer.DataBaseRecomputeOnlySpecitifEvents;
import plugins.fab.livemousetracker.dataplayer.DataUtil;
import plugins.fab.livemousetracker.experiment.Experiment;
import plugins.fab.livemousetracker.liveanalysis.chronogram.ChronoDetectionPresent;
import plugins.fab.livemousetracker.liveanalysis.chronogram.ChronoNest4;
import plugins.fab.livemousetracker.liveanalysis.chronogram.EventTimeLine;
import plugins.fab.livemousetracker.track.TrackContainer;

public class PostProcessDataBase extends PluginActionable implements PluginThreaded {

	private static final boolean RECOMPUTE_ALL_EVENTS = false;
	private static final boolean RECOMPUTE_DETECTION_EVENT = false; // TRANSFERED IN PYTHON
	private static final boolean COMPUTE_HUDDLING_EVENTS = true;
	private static final boolean COMPUTE_NEST_EVENTS = false;

	private static final boolean COMPUTE_GROUP_EVENTS = false;
	private static final boolean MERGE_EVENTS = false;
	private static final boolean REMOVE_DOUBLONS = false;
	private static final boolean VACUUM_DATABASE = false;
	Connection connection = null;

	@Override
	public void run() {



		System.out.println("Post process database");
		File files[] = DataUtil.selectDataBaseFiles( this );

		for ( File dataBaseFile : files )
		{
			System.out.println("*************************************************");
			System.out.println("Processing file: " + dataBaseFile.getAbsolutePath() );
			System.out.println("*************************************************");

			//File dataBaseFile = DataUtil.selectDataBaseFile( this );
			connection = null;
			connection = DataUtil.connectDataBase( connection, dataBaseFile );

			/*
			if ( RECOMPUTE_DETECTION_EVENT )
			{
				try {
					connection.setAutoCommit( false );
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				DataBaseRecomputeOnlySpecitifEvents.delEvents(connection , "Detection" );

				int max = DataUtil.getMaxNumberOfFrame( connection );
				int window = 30 * 60 * 5; // 5 minutes.

				ChronoDetectionPresent chronoDetection[] = new ChronoDetectionPresent[5];
				for ( int id = 1 ; id < 5 ; id ++ )
				{
					chronoDetection[id] = new ChronoDetectionPresent();
					chronoDetection[id].eventTimeLine = new EventTimeLine( this.getName() , chronoDetection[id].getTimeLineDataType() );
				}

				for ( int t = 0 ; t < max ; t+= window )
				{
					System.out.println( t + " / " + max + " " + ( 100d/max*t) + "%" );
					TrackContainer trackContainer = DataUtil.loadAnimalData( connection, t , t+window );
					for ( Animal animal : trackContainer.animalTrackSegmentPool.animalList )
					{
						int nbAdded= 0;
						for ( int tt = t; tt<=t+window; tt++ )
						{
							// FIXME: DEAD SLOW. The get detection is slow,
							// the add punctual event is slow
							if ( animal.getDetectionAt( tt ) != null )
							{
								chronoDetection[(int)animal.getDataBaseId()].
								getEventTimeLine().
								addPunctualEvent( tt );
								nbAdded++;
							}
						}
						System.out.println("Added: " + nbAdded );
						System.out.println( animal + " nbDetection: "
						+ chronoDetection[(int)animal.getDataBaseId()].getEventTimeLine().getAllEventLength() );
					}
				}

				for ( int id = 1 ; id < 5 ; id ++ )
				{
					Animal a = new Animal("");
					a.setDataBaseId( id );
					Experiment.saveChronoEvent( connection , chronoDetection[id] , a , null, null, null );
				}
				try {
					connection.commit();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
			*/


			if ( RECOMPUTE_ALL_EVENTS )
			{
				DataBaseRecomputeEvents.recomputeEvents(connection);
			}


//			if ( COMPUTE_GROUP_EVENTS )
//			{
//				DataBaseRecomputeOnlySpecitifEvents.delEvents(connection , "Group3" );
//				final int max = DataUtil.getMaxNumberOfFrame( connection );
//
//				final int window = 2 * 60 * 30; // tranche de 2 minutes
//
//				Processor processor = new Processor( Integer.MAX_VALUE , SystemUtil.getNumberOfCPUs() );
//
//				for ( int t = 0 ; t < max ; t+= window )
//				{
//					final int tStart = t;
//
//					processor.submit(
//
//					new Runnable() {
//
//						@Override
//						public void run() {
//
//							System.out.println("******* : " + tStart * ( 100f / max ) + " % " );
//							int upperLimit = tStart+window -1;
//							if ( upperLimit > max )
//							{
//								upperLimit = max;
//							}
//							DataBaseRecomputeOnlySpecitifEvents.recomputeGroupEvents( connection , tStart , upperLimit );
//							String txt = "Mem:"+(int)( ( SystemUtil.getJavaTotalMemory() - SystemUtil.getJavaFreeMemory() ) / ( 1024 * 1024 ) ) +"/"+
//									(int)( SystemUtil.getJavaMaxMemory() / ( 1024 * 1024 ) ) +"MB";
//							System.err.println( txt );
//
//
//						}
//					} );
//				}
//
//				while ( processor.isProcessing() )
//				{
//					System.out.println("Remaining tasks: " + processor.getWaitingTasksCount() );
//					try {
//						Thread.sleep( 10000 );
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
//				}
//				System.out.println("All thread finished");
//				System.out.println("Finished.");
//			}

			if ( COMPUTE_HUDDLING_EVENTS )
			{
				DataBaseRecomputeOnlySpecitifEvents.delEvents(connection , "Huddling" );
				final int max = DataUtil.getMaxNumberOfFrame( connection );
				final int window = 2 * 60 * 30; // tranche de 2 minutes

				Chronometer recomputationChrono = new Chronometer("recomputation");

				Processor processor = new Processor( Integer.MAX_VALUE , SystemUtil.getNumberOfCPUs() );
				processor.setThreadName("Post Process database");

				for ( int t = 0 ; t < max ; t+= window )
				{
					final int tStart = t;

					processor.submit(

					new Runnable() {

						@Override
						public void run() {

							System.out.println("******* : " + tStart * ( 100f / max ) + " % " );
							int upperLimit = tStart+window -1;
							if ( upperLimit > max )
							{
								upperLimit = max;
							}
							DataBaseRecomputeOnlySpecitifEvents.recomputeHuddlingEvents( connection , tStart , upperLimit );
							String txt = "Mem:"+(int)( ( SystemUtil.getJavaTotalMemory() - SystemUtil.getJavaFreeMemory() ) / ( 1024 * 1024 ) ) +"/"+
									(int)( SystemUtil.getJavaMaxMemory() / ( 1024 * 1024 ) ) +"MB";
							System.err.println( txt );
						}
					} );
				}

				while ( processor.isProcessing() )
				{
					System.out.println("Remaining tasks: " + processor.getWaitingTasksCount() );
					try {
						Thread.sleep( 10000 );
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				System.out.println("All thread finished");
				recomputationChrono.displayInSeconds();

				System.out.println("Finished.");

			}

			if ( COMPUTE_NEST_EVENTS )
			{
				try {
					ArrayList<DBAnimal> mouseList = DataUtil.loadMice( connection );

					if ( mouseList.size() > 2 )
					{
						System.out.println("Compute nest");
						computeNestEvents();
					}else
					{
						System.out.println("nb mice: "+ mouseList.size() + " --> Skip nest calculation.");
					}

				} catch (SQLException e1) {
					e1.printStackTrace();
				}


			}

			// RECOMPUTE TOO SHORTS EVENTS BECAUSE OF THE 500 TIME POINT STREAMING
			if ( MERGE_EVENTS )
			{
//				ArrayList<EventTypeInBase> eventInBase = getEventTypeInBase( connection );
//
//				for ( EventTypeInBase event : eventInBase )
//				{
//					System.out.println( event );
//				}

				int nbAnimals =0 ;
				try {
					nbAnimals = DataUtil.loadMice( connection ).size();
				} catch (SQLException e) {
					e.printStackTrace();
				}

				{
					DataBaseRecomputeOnlySpecitifEvents.mergeEvent( "Nest4" , connection , null, null );

					for ( int idA = 1 ; idA < nbAnimals+1 ;idA++ )
					{
						DataBaseRecomputeOnlySpecitifEvents.mergeEvent( "Nest3" , connection , idA, null );
						DataBaseRecomputeOnlySpecitifEvents.mergeEvent( "Stop" , connection , idA, null );
						DataBaseRecomputeOnlySpecitifEvents.mergeEvent( "Rearing" , connection , idA, null );

						for ( int idB = 1 ; idB < nbAnimals+1 ;idB++ )
						{
							if ( idA == idB ) continue;

							DataBaseRecomputeOnlySpecitifEvents.mergeEvent( "Contact" , connection , idA, idB );
							DataBaseRecomputeOnlySpecitifEvents.mergeEvent( "Oral-oral Contact" , connection , idA, idB );
							DataBaseRecomputeOnlySpecitifEvents.mergeEvent( "Oral-genital Contact" , connection , idA, idB );
						}
					}
				}

				System.out.println("Too shorts events update finished.");
			}

			if ( REMOVE_DOUBLONS )
			{
				int nbDoublonRemoved = -1;
				int wave = 1;
				while ( nbDoublonRemoved != 0 )
				{
					// delete all events in database.
					System.out.println("* WAVE " + wave );
					Chronometer chrono = new Chronometer( "Deleting doublons" );

					String sql = "DELETE from event where id in ( "
							+"SELECT e1.id FROM event e1 join event e2  on "
							+"((e1.ID <> e2.ID) AND (e1.NAME = e2.NAME) AND (e1.DESCRIPTION = e2.DESCRIPTION ) "
							+"AND (e1.IDANIMALA = e2.IDANIMALA) AND (e1.IDANIMALB = e2.IDANIMALB ) "
							+"AND (e1.STARTFRAME = e2.STARTFRAME) AND (e1.ENDFRAME = e2.ENDFRAME)) "
							+"GROUP BY e1.name , e1.description , e1.idanimala, e1.idanimalb, e1.startframe, e1.endframe "
							+"order by e1.id )";
					PreparedStatement ps;
					try {
						ps = connection.prepareStatement( sql );
						nbDoublonRemoved = ps.executeUpdate();

					} catch (SQLException e) {
						e.printStackTrace();
					}
					chrono.displayInSeconds();
					System.out.println("Doublon removed: " + nbDoublonRemoved ) ;
					wave++;
				}
				System.out.println("Finshed ! Doublon removed !");



				/*

 select *, count(framenumber) from frame group by framenumber order by count(framenumber) desc

SELECT *, Mass_X, COUNT(*) c FROM Detection GROUP BY Mass_x HAVING c > 1;

doublon detection:
SELECT d1.id, d1.framenumber, d1.animalid, d1.mass_x, d2.id, d2.framenumber, d2.animalid, d2.mass_x FROM Detection d1 join Detection d2 on
(d1.ID <> d2.ID AND d1.ANIMALID==d2.ANIMALID AND d1.FRAMENUMBER == d2.FRAMENUMBER AND d1.MASS_X == d2.MASS_X AND d1.MASS_Y == d2.MASS_Y)

doublons events:
SELECT e1.*,e2.* FROM event e1 join event e2  on
(e1.ID <> e2.ID AND e1.NAME == e2.NAME AND e1.DESCRIPTION == e2.DESCRIPTION
AND e1.IDANIMALA == e2.IDANIMALA AND e1.IDANIMALB == e2.IDANIMALB
AND e1.STARTFRAME == e2.STARTFRAME AND e1.ENDFRAME == e2.ENDFRAME)

doublon de frame:
SELECT f1.*,f2.* FROM frame f1 join frame f2  on
(f1.framenumber <> f2.framenumber AND f1.timestamp == f2.timestamp )

dans un group by eee = null est true.

suppression doublons:

--DELETE from event where id in (
SELECT e1.* FROM event e1 join event e2  on
((e1.ID <> e2.ID) AND (e1.NAME = e2.NAME) AND (e1.DESCRIPTION = e2.DESCRIPTION )
AND (e1.IDANIMALA = e2.IDANIMALA) AND (e1.IDANIMALB = e2.IDANIMALB )
AND (e1.STARTFRAME = e2.STARTFRAME) AND (e1.ENDFRAME = e2.ENDFRAME))
GROUP BY e1.name , e1.description , e1.idanimala, e1.idanimalb, e1.startframe, e1.endframe
order by e1.id
--)


--SELECT *, COUNT(*) c FROM event GROUP BY name , description , idanimala, idanimalb, startframe, endframe HAVING (c > 1) order by id


				 */


			}

			if( VACUUM_DATABASE )
			{
				System.out.println("Vacuuming database");
				String sql = "VACUUM";
				PreparedStatement ps;
				try {
					ps = connection.prepareStatement( sql );
					ps.executeUpdate();
				} catch (SQLException e) {
					e.printStackTrace();
				}
				System.out.println("Vacuuming finished");
			}

			try {
				connection.commit();
				connection.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			System.out.println("*********************************");
			System.out.println("PostProcessDataBase: all finished");
			System.out.println("*********************************");

		}

	}

	private void computeNestEvents() {

		DataBaseRecomputeOnlySpecitifEvents.delEvents(connection , "Nest4" );
		DataBaseRecomputeOnlySpecitifEvents.delEvents(connection , "Nest3" );
		final int max = DataUtil.getMaxNumberOfFrame( connection );

		final int window = 2 * 60 * 30; // tranche de 2 minutes

		Chronometer recomputationChrono = new Chronometer("recomputation");

		Processor processor = new Processor( Integer.MAX_VALUE , SystemUtil.getNumberOfCPUs() );
		processor.setThreadName("Compute Nest event");

		for ( int t = 0 ; t < max ; t+= window )
		{
			final int tStart = t;

			processor.submit(

			new Runnable() {

				@Override
				public void run() {

					System.out.println("******* : " + tStart * ( 100f / max ) + " % " );
					int upperLimit = tStart+window -1;
					if ( upperLimit > max )
					{
						upperLimit = max;
					}
					DataBaseRecomputeOnlySpecitifEvents.recomputeNestEvents( connection , tStart , upperLimit );
					String txt = "Mem:"+(int)( ( SystemUtil.getJavaTotalMemory() - SystemUtil.getJavaFreeMemory() ) / ( 1024 * 1024 ) ) +"/"+
							(int)( SystemUtil.getJavaMaxMemory() / ( 1024 * 1024 ) ) +"MB";
					System.err.println( txt );


				}
			} );



//			System.out.println("******* : " + t * ( 100f / max ) + " % " + recomputationChrono.getNanos()/ 1000000000f + "s" );
//			int upperLimit = t+window -1;
//			if ( upperLimit > max )
//			{
//				upperLimit = max;
//			}
//			DataBaseRecomputeOnlySpecitifEvents.recomputeNestEvents( connection , t , upperLimit );
//			String txt = "Mem:"+(int)( ( SystemUtil.getJavaTotalMemory() - SystemUtil.getJavaFreeMemory() ) / ( 1024 * 1024 ) ) +"/"+
//					(int)( SystemUtil.getJavaMaxMemory() / ( 1024 * 1024 ) ) +"MB";
//			System.err.println( txt );
		}

		while ( processor.isProcessing() )
		{
			System.out.println("Remaining tasks: " + processor.getWaitingTasksCount() );
			try {
				Thread.sleep( 10000 );
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("All thread finished");
		recomputationChrono.displayInSeconds();

		System.out.println("Finished.");


	}

	private ArrayList<EventTypeInBase> getEventTypeInBase(Connection connection ) {

		System.out.println("Grabbing all events nature in base...");
		ArrayList<EventTypeInBase> eventList = new ArrayList<EventTypeInBase>();
		try {

			// GRAB ORIGINAL EVENT
			String sql = "SELECT * FROM EVENT GROUP BY NAME, IDANIMALA, IDANIMALB";
			PreparedStatement ps = connection.prepareStatement( sql );

			ResultSet rs = ps.executeQuery( );

			while ( rs.next() )
			{
				eventList.add( new EventTypeInBase( rs.getInt( "IDANIMALA" ) , rs.getInt( "IDANIMALB" ) , rs.getString( "NAME" ) ) );
			}

			rs.close();


		} catch (SQLException e) {
			e.printStackTrace();
		}

		return eventList;
	}


}
