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
package plugins.fab.livemousetracker.dataplayer;

import java.awt.Color;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import icy.system.profile.Chronometer;
import plugins.fab.livemousetracker.Animal;
import plugins.fab.livemousetracker.ROI2DAreaX;
import plugins.fab.livemousetracker.detection.MouseDetection;
import plugins.fab.livemousetracker.experiment.Experiment;
import plugins.fab.livemousetracker.liveanalysis.chronogram.ChronoHuddling;
import plugins.fab.livemousetracker.liveanalysis.chronogram.ChronoGroup3;
import plugins.fab.livemousetracker.liveanalysis.chronogram.ChronoHeadDetected;
import plugins.fab.livemousetracker.liveanalysis.chronogram.ChronoNest3;
import plugins.fab.livemousetracker.liveanalysis.chronogram.ChronoNest4;
import plugins.fab.livemousetracker.liveanalysis.chronogram.Event;
import plugins.fab.livemousetracker.liveanalysis.chronogram.EventTimeLine;
import plugins.fab.livemousetracker.liveanalysis.chronogram.EventType;
import plugins.fab.livemousetracker.liveanalysis.chronogram.TimeLineDataType;
import plugins.fab.livemousetracker.track.TrackContainer;

/** Recompute all the events of a database */
public class DataBaseRecomputeOnlySpecitifEvents {

	/**
	 * RECOMPUTE CONTACTS !
	 * */
	public static void mergeEvent( String eventName , Connection connection , Integer idAnimalA , Integer idAnimalB )
	{
		synchronized ( connection )
		{
			try {
				connection.setAutoCommit( false );
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		System.out.println( eventName + " : " + idAnimalA + " / " + idAnimalB  );

		String newEventName = eventName+"2";

		try {

			// DELETE PREVIOUSLY REBUILD EVENTS
			{
				String sql = "DELETE FROM EVENT WHERE NAME=?";
				if( idAnimalA  != null )
				{
					sql+= " AND IDANIMALA=?";
				}

				if( idAnimalB  != null )
				{
					sql+=" AND IDANIMALB=?";
				}

				PreparedStatement ps;
				ps = connection.prepareStatement( sql );
				ps.setString( 1 , newEventName );

				if ( idAnimalA != null )
				{
					ps.setInt( 2 , idAnimalA );
				}

				if ( idAnimalB != null )
				{
					ps.setInt( 3 , idAnimalB );
				}
				ps.executeUpdate();
			}

			// GRAB ORIGINAL EVENT
			String sql = "SELECT * FROM EVENT WHERE NAME=?";

			if( idAnimalA  != null )
			{
				sql+=" AND IDANIMALA=?";
			}

			if( idAnimalB  != null )
			{
				sql+=" AND IDANIMALB=?";
			}
			PreparedStatement ps = connection.prepareStatement( sql );
			ps.setString( 1 , eventName );

			if ( idAnimalA != null )
			{
				ps.setInt( 2 , idAnimalA );
			}

			if( idAnimalB  != null )
			{
				ps.setInt( 3 , idAnimalB );
			}

			ResultSet rs = ps.executeQuery( );

			EventTimeLine timeLine = new EventTimeLine( eventName , TimeLineDataType.BOOLEAN );

			while ( rs.next() )
			{
//				System.out.println("test");
				int endFrame = rs.getInt( "endFrame" );
				int startFrame = rs.getInt( "startFrame" );
				for ( int t = startFrame ; t <= endFrame ; t++ )
				{
					timeLine.addPunctualEvent(t);
				}
			}

    	rs.close();

    	timeLine.removeEventLessThanLength( 4 );

    	System.out.println("nb event: " + timeLine.getBooleanEventList().size() );

		Experiment.saveTimeLine( connection , newEventName , "" , timeLine , idAnimalA , idAnimalB, null, null );


//    	for ( Event event : timeLine.getBooleanEventList() )
//    	{
//    			System.out.println( event.getLength() );
//    	}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		try {
			connection.commit();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}


	public static void delEvents( Connection connection, String eventName )
	{
		// delete all events in database.
		System.out.println("Deleting event " + eventName );
		String sql = "DELETE FROM EVENT WHERE NAME=?";
		PreparedStatement ps;
		try {
			ps = connection.prepareStatement( sql );
			ps.setString( 1 , eventName );
			ps.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}



	/**
	 *
	 * @param connection
	 * @param frameStart
	 * @param frameEnd
	 */
	public static void recomputeHuddlingEvents( Connection connection , int frameStart, int frameEnd )
	{
		Chronometer chrono = new Chronometer("Recompute Huddling event " + frameStart + " / " + frameEnd );
		// Rebuild data to rebuild events

		TrackContainer trackContainer = new TrackContainer();

		// load animals

		ArrayList<DBAnimal> db_animalList;
		try {
			db_animalList = DataUtil.loadMice( connection );
		} catch (SQLException e) {
			e.printStackTrace();
			return;
		}

		// rebuild animal data

		for ( DBAnimal db_animal : db_animalList )
		{
			Animal animal = new Animal("name");
			db_animal.animal = animal;
			animal.setRfidID( db_animal.RFID );
			animal.setDataBaseId( db_animal.id );
		//	System.out.println("ID: " + db_animal.id );
			trackContainer.animalTrackSegmentPool.addAnimal( animal );
		}

		// load detections
		ArrayList<MouseDetectionX> detectionList ;

		try {
			detectionList = LiveDataPlayer.loadDetection( connection, frameStart, frameEnd , false, null );
		} catch (SQLException e1) {
			e1.printStackTrace();
			return;
		}

		HashMap<Integer, DetectAtT> detectMap = new HashMap<Integer, DetectAtT>();

		for ( MouseDetectionX detection : detectionList )
		{
			int t = detection.mouseDetection.getT();
			DetectAtT map = detectMap.get( t );
			if ( map == null )
			{
				map = new DetectAtT();
				detectMap.put( t , map );
			}
			map.detection.add( detection );
		}

		for ( MouseDetectionX detection : detectionList )
		{
			long animalId = detection.animalId;

			if ( animalId == 0 )
			{
				// anonymous animal. Skip.
				continue;
			}

			DBAnimal animal = DataUtil.getAnimal( animalId , db_animalList );
			if ( animal == null )
			{
				System.out.println("Error: " + detection.mouseDetection );
				System.out.println("ID: " + animalId );
				continue;
			}
			animal.addDetection( detection );
		}

		for ( DBAnimal db_animal : db_animalList )
		{
			db_animal.computeTracks();
			db_animal.animal.addAll( db_animal.trackList );
		}

		// recomputing events
		System.out.println("Computing and Saving...");

		ArrayList<Animal> animalList = new ArrayList<Animal>();
		for ( DBAnimal animalDb : db_animalList )
		{
			animalList.add( animalDb.animal );
		}

		synchronized ( connection )
		{
			try {
				connection.setAutoCommit( false );
				{
					for ( Animal animalToCheck : animalList )
					{
						ChronoHuddling chronoBall = new ChronoHuddling();
						chronoBall.process( animalToCheck, frameStart , frameEnd );
						Experiment.saveChronoEvent( connection , chronoBall , animalToCheck , null, null, null );
					}
				}
				connection.commit();
			} catch (SQLException e) {
				System.err.println("Error in comit ! Concurent Exception ?");
				e.printStackTrace();
			}
		}

		chrono.displayInSeconds();
	}


	/**
	 *
	 * @param connection
	 * @param frameStart
	 * @param frameEnd
	 */
	public static void recomputeNestEvents( Connection connection , int frameStart, int frameEnd )
	{
		Chronometer chrono = new Chronometer("Recompute ONLY some events + " + frameStart + " / " + frameEnd );
		// Rebuild data to rebuild events

		TrackContainer trackContainer = new TrackContainer();

		// load animals

		ArrayList<DBAnimal> db_animalList;
		try {
			db_animalList = DataUtil.loadMice( connection );
		} catch (SQLException e) {
			e.printStackTrace();
			return;
		}

		// rebuild animal data

		for ( DBAnimal db_animal : db_animalList )
		{
			Animal animal = new Animal("name");
			db_animal.animal = animal;
			animal.setRfidID( db_animal.RFID );
			animal.setDataBaseId( db_animal.id );
		//	System.out.println("ID: " + db_animal.id );
			trackContainer.animalTrackSegmentPool.addAnimal( animal );
		}

		// load detections
		ArrayList<MouseDetectionX> detectionList ;

		try {
			detectionList = LiveDataPlayer.loadDetection( connection, frameStart, frameEnd , false, null );
		} catch (SQLException e1) {
			e1.printStackTrace();
			return;
		}

		HashMap<Integer, DetectAtT> detectMap = new HashMap<Integer, DetectAtT>();

		for ( MouseDetectionX detection : detectionList )
		{
			int t = detection.mouseDetection.getT();
			DetectAtT map = detectMap.get( t );
			if ( map == null )
			{
				map = new DetectAtT();
				detectMap.put( t , map );
			}
			map.detection.add( detection );
		}

		for ( MouseDetectionX detection : detectionList )
		{
			long animalId = detection.animalId;

			if ( animalId == 0 )
			{
				// anonymous animal. Skip.
				continue;
			}

			DBAnimal animal = DataUtil.getAnimal( animalId , db_animalList );
			if ( animal == null )
			{
				System.out.println("Error: " + detection.mouseDetection );
				System.out.println("ID: " + animalId );
				continue;
			}
			animal.addDetection( detection );
		}

		for ( DBAnimal db_animal : db_animalList )
		{
			db_animal.computeTracks();
			db_animal.animal.addAll( db_animal.trackList );
		}

		// recomputing events

    	System.out.println("Saving...");

    	ArrayList<Animal> animalList = new ArrayList<Animal>();
    	for ( DBAnimal animalDb : db_animalList )
    	{
    		animalList.add( animalDb.animal );
    	}

    	synchronized ( connection )
    	{
    		try {
    			connection.setAutoCommit( false );
    			//Experiment.saveEvents(connection, trackContainer, frameStart , frameEnd  );
    			{
    				ChronoNest4 chronoNest4 = new ChronoNest4();
    				chronoNest4.setDetectionTMap( detectMap );
    				chronoNest4.processGroup( animalList, frameStart , frameEnd );
    				Experiment.saveChronoEvent( connection , chronoNest4 , null , null, null, null );
    			}
    			for ( Animal animalToCheck : animalList )
    			{
    				ChronoNest3 chronoNest3 = new ChronoNest3();
    				chronoNest3.animalToCheck = animalToCheck;
    				chronoNest3.setDetectionTMap( detectMap );
    				chronoNest3.processGroup( animalList, frameStart , frameEnd );
    				Experiment.saveChronoEvent( connection , chronoNest3 , animalToCheck , null, null, null );
    			}
    			connection.commit();
    		} catch (SQLException e) {
    			e.printStackTrace();
    		}
    	}

		chrono.displayInSeconds();
	}

	/** @deprecated
	 *
	 * @param connection
	 * @param frameStart
	 * @param frameEnd
	 */
	public static void recomputeGroupEvents( Connection connection , int frameStart, int frameEnd )
	{
		Chronometer chrono = new Chronometer("Recompute group events + " + frameStart + " / " + frameEnd );
		// Rebuild data to rebuild events

		TrackContainer trackContainer = new TrackContainer();

		// load animals

		ArrayList<DBAnimal> db_animalList;
		try {
			db_animalList = DataUtil.loadMice( connection );
		} catch (SQLException e) {
			e.printStackTrace();
			return;
		}

		// rebuild animal data

		for ( DBAnimal db_animal : db_animalList )
		{
			Animal animal = new Animal("name");
			db_animal.animal = animal;
			animal.setRfidID( db_animal.RFID );
			animal.setDataBaseId( db_animal.id );
		//	System.out.println("ID: " + db_animal.id );
			trackContainer.animalTrackSegmentPool.addAnimal( animal );
		}

		// load detections
		ArrayList<MouseDetectionX> detectionList ;

		try {
			detectionList = LiveDataPlayer.loadDetection( connection, frameStart, frameEnd , false, null );
		} catch (SQLException e1) {
			e1.printStackTrace();
			return;
		}

		HashMap<Integer, DetectAtT> detectMap = new HashMap<Integer, DetectAtT>();

		for ( MouseDetectionX detection : detectionList )
		{
			int t = detection.mouseDetection.getT();
			DetectAtT map = detectMap.get( t );
			if ( map == null )
			{
				map = new DetectAtT();
				detectMap.put( t , map );
			}
			map.detection.add( detection );
		}

		for ( MouseDetectionX detection : detectionList )
		{
			long animalId = detection.animalId;

			if ( animalId == 0 )
			{
				// anonymous animal. Skip.
				continue;
			}

			DBAnimal animal = DataUtil.getAnimal( animalId , db_animalList );
			if ( animal == null )
			{
				System.out.println("Error: " + detection.mouseDetection );
				System.out.println("ID: " + animalId );
				continue;
			}
			animal.addDetection( detection );
		}

		for ( DBAnimal db_animal : db_animalList )
		{
			db_animal.computeTracks();
			db_animal.animal.addAll( db_animal.trackList );
		}

		// recomputing events
		System.out.println("computing events...");

    	ArrayList<Animal> animalList = new ArrayList<Animal>();
    	for ( DBAnimal animalDb : db_animalList )
    	{
    		animalList.add( animalDb.animal );
    	}

    	synchronized ( connection )
    	{
    		try {
    			connection.setAutoCommit( false );

    			for ( Animal animalToCheck : animalList )
    			{
    				ChronoGroup3 chronoGroup3 = new ChronoGroup3();
    				chronoGroup3.animalToCheck = animalToCheck;
    				chronoGroup3.setDetectionTMap( detectMap );
    				chronoGroup3.processGroup( animalList, frameStart , frameEnd );
    				Experiment.saveChronoEvent( connection , chronoGroup3 , animalToCheck , null, null, null );
    			}
    			connection.commit();
    		} catch (SQLException e) {
    			e.printStackTrace();
    		}
    	}



		chrono.displayInSeconds();
	}

}
