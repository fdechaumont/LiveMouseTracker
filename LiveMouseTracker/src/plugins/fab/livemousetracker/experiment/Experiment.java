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
package plugins.fab.livemousetracker.experiment;

import java.awt.Color;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import icy.file.FileUtil;
import icy.gui.frame.progress.ProgressFrame;
import icy.system.profile.Chronometer;
import plugins.fab.livemousetracker.Animal;
import plugins.fab.livemousetracker.FrameInfo;
import plugins.fab.livemousetracker.LiveMouseTracker;
import plugins.fab.livemousetracker.SQLiteSavedData;
import plugins.fab.livemousetracker.detection.MouseDetection;
import plugins.fab.livemousetracker.liveanalysis.chronogram.ChronoApproach;
import plugins.fab.livemousetracker.liveanalysis.chronogram.ChronoBehind;
import plugins.fab.livemousetracker.liveanalysis.chronogram.ChronoBreakContact;
import plugins.fab.livemousetracker.liveanalysis.chronogram.ChronoContact;
import plugins.fab.livemousetracker.liveanalysis.chronogram.ChronoDistanceBetweenAnimal;
import plugins.fab.livemousetracker.liveanalysis.chronogram.ChronoEscape;
import plugins.fab.livemousetracker.liveanalysis.chronogram.ChronoFeatureAbstract;
import plugins.fab.livemousetracker.liveanalysis.chronogram.ChronoFollow;
import plugins.fab.livemousetracker.liveanalysis.chronogram.ChronoGetInContact;
import plugins.fab.livemousetracker.liveanalysis.chronogram.ChronoHeadDetected;
import plugins.fab.livemousetracker.liveanalysis.chronogram.ChronoHeadDown;
import plugins.fab.livemousetracker.liveanalysis.chronogram.ChronoMouseSpeed;
import plugins.fab.livemousetracker.liveanalysis.chronogram.ChronoDetectionPresent;
import plugins.fab.livemousetracker.liveanalysis.chronogram.ChronoOralGenital;
import plugins.fab.livemousetracker.liveanalysis.chronogram.ChronoOralOral;
import plugins.fab.livemousetracker.liveanalysis.chronogram.ChronoRearing;
import plugins.fab.livemousetracker.liveanalysis.chronogram.ChronoSideBySide;
import plugins.fab.livemousetracker.liveanalysis.chronogram.ChronoSideBySideOpposite;
import plugins.fab.livemousetracker.liveanalysis.chronogram.ChronoStop;
import plugins.fab.livemousetracker.liveanalysis.chronogram.Event;
import plugins.fab.livemousetracker.liveanalysis.chronogram.EventTimeLine;
import plugins.fab.livemousetracker.liveanalysis.chronogram.TimeLineDataType;
import plugins.fab.livemousetracker.track.AnimalPool;
import plugins.fab.livemousetracker.track.AnonymousPool;
import plugins.fab.livemousetracker.track.TrackContainer;
import plugins.fab.livemousetracker.track.TrackSegment;

public class Experiment {

//	String name = "";
	String baseFolder = "";
	/**
	 * The database can be share by several experiment
	 * */
	File dataBaseFile;

	TrackContainer trackContainer ;

	public Experiment( String baseFolder //, String name
			) {
//		this.name = name;
		this.baseFolder = baseFolder;
		System.out.println( "Experiment : " + this.baseFolder );
	}

//	public String getName() {
//		return name;
//	}

	public String getBaseFolder() {
		return baseFolder;
	}

	public String getDataBaseAbsoluteFilePath()
	{
		return getBaseFolder()
				//+ getName()
				//+ "/data.sqlite";
				+ "/"+ FileUtil.getFileName( getBaseFolder() ) + ".sqlite";
	}

//	ArrayList<TrackSegment> anonymousTrackList = null;
//	AnimalPool animalPool = new AnimalPool();

	public AnimalPool getAnimalPool() {
//		System.out.println("TrackContainer : " + trackContainer );
		return trackContainer.animalTrackSegmentPool;
	}

	public ArrayList<TrackSegment> getAnonymousTrackList() {
		return trackContainer.anonymousTrackSegmentPool.getTrackSegments();
	}



	public void load( ProgressFrame pf )
	{
		System.out.println("Load experiment");
		try {
			Connection connection = connectDataBase();
			AnimalPool animalPool = new AnimalPool();
			String sql = "SELECT * FROM ANIMAL";
			PreparedStatement ps;
			ps = connection.prepareStatement( sql );
			ResultSet rs = ps.executeQuery( );
			while ( rs.next() ) {
				int animalId = rs.getInt("id");
				String name = rs.getString("name");
				if ( pf != null ) pf.setMessage("Loading animal " + name );
				String rfid = rs.getString("rfid");
				System.out.println( "animalID = " + animalId );
				System.out.println( "NAME = " + name );
				System.out.println( "RFID = " + rfid );
				System.out.println();

				// load tracks.
				Animal animal = new Animal( name, new Color( Color.HSBtoRGB( (float)Math.random() , (float)0.8, (float)0.8 )  ) );
				animal.setDataBaseId( animalId );
				animalPool.addAnimal( animal );
				//animalPool.addAnimal( animal );
				animal.setRfidID( rfid );
				animal.addAll( loadTracks( connection, animalId ) );
			}
			rs.close();

			this.trackContainer = new TrackContainer();
			if ( pf != null ) pf.setMessage("Loading anonymous data" );
			System.out.println("Load anonymous tracks");
			trackContainer.anonymousTrackSegmentPool = new AnonymousPool( loadTracks( connection,  null ) );

			//			anonymousTrackList = loadTracks( connection, null );


			trackContainer.animalTrackSegmentPool= animalPool;

			System.out.println( "Track container at end of load: " + this.trackContainer );

//			TrackSegmentPool trackPoolAnonymous = new TrackSegmentPool( );
			//trackContainer.anonymousTrackSegmentPool = new TrackSegmentPool( anonymousTrackList );
			connection.close();
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Load tracks of animalID from connection.
	 * if animalId is null, load anonymous tracks.
	 * */
	private ArrayList<TrackSegment> loadTracks(Connection connection, Integer animalId) throws SQLException {

		ArrayList<TrackSegment> trackSegmentList = new ArrayList<TrackSegment>();

		PreparedStatement ps;

		String sql = "SELECT * FROM TRACK WHERE ANIMALID=?";
		if ( animalId == null ) // track loaded will be the one not associated to animals ( animalid = null )
		{
			sql = "SELECT * FROM TRACK WHERE ANIMALID IS NULL";
		}
		ps = connection.prepareStatement( sql );

		if ( animalId != null )
		{
			ps.setInt( 1 , animalId );
		}

		ResultSet rs = ps.executeQuery( );
		while ( rs.next() ) {
			int trackId = rs.getInt("id");
			System.out.println( "track ID = " + trackId );
			System.out.println();

			TrackSegment trackSegment = new TrackSegment();
			trackSegment.setDataBaseId( trackId );
			trackSegmentList.add( trackSegment );
			// load detections.
			trackSegment.addAll( loadDetections( connection, trackId ) );
		}
		rs.close();

		System.out.println( "number of track loaded: "+ trackSegmentList.size() );
		ps.close();
		return trackSegmentList;

	}

	private ArrayList<MouseDetection> loadDetections(Connection connection, int trackId) throws SQLException {

		ArrayList<MouseDetection> detectionList = new ArrayList<MouseDetection>();

		String sql = "SELECT * FROM DETECTION WHERE TRACKID=? ORDER BY TIME";
		PreparedStatement ps;
		ps = connection.prepareStatement( sql );
		ps.setInt( 1 , trackId );

		ResultSet rs = ps.executeQuery( );
		while ( rs.next() ) {
//			int t = rs.getInt("time");
//			int x = rs.getInt("x");
//			int y = rs.getInt("y");
			String data = rs.getString( "data" );
			detectionList.add( new MouseDetection( data ) );
		}
		rs.close();
		ps.close();
		return detectionList;
	}

	/**
	 * Save all the tracks in database.
	 * Save frame infos
	 * Save animal events
	 * streamSave argument will add and remove tracks from current scene. (classic save DOES NOT remove tracks)
	 * with TRIGGER
	 * @param anonymousTrackSegmentPool
	 **/
	public void save( TrackContainer trackContainer , boolean streamSave )//AnimalPool animalPool, TrackSegmentPool anonymousTrackSegmentPool ) {
	{
		if ( trackContainer == null )
		{
			System.out.println("Track container is null");
			return;
		}

		try {
			Connection connection = connectDataBase();
			connection.setAutoCommit( false );

			// This is moved to the main thread because track may change while this part of code is called
			LiveMouseTracker.trackContainer.breakTooLongTrack( LiveMouseTracker.getFilterUpperFrameLimit() ); // FIX : CHECK if this is okay while tracking

			saveAnimals(connection, trackContainer );
			saveFrameData( connection, trackContainer, streamSave );
			saveLog( connection );
			saveEvents( connection, trackContainer, streamSave );
			saveAnimalTrack( connection, trackContainer, streamSave );
			saveAnonymousTrack( connection, trackContainer, streamSave );

			connection.commit();
			connection.close();

		}catch (SQLException e) {
			System.err.println("COULD NOT SAVE DATABASE.");
			e.printStackTrace();
		}
		System.out.println("Experiment saved. name: " + this.getBaseFolder() );

	}

	private void saveLog(Connection connection) {

		for ( EventLog event : LiveMouseTracker.getEventLogToDataBaseList() )
		{
			try {
				String sql = "INSERT INTO EVENT ( NAME, DESCRIPTION, STARTFRAME, ENDFRAME,"
						+ " IDANIMALA, IDANIMALB, IDANIMALC, IDANIMALD ) VALUES (?,?,?,?,?,?,?,?);";
				PreparedStatement ps;
				ps = connection.prepareStatement( sql, Statement.RETURN_GENERATED_KEYS );

				ps.setString( 1 , event.name );
				ps.setString( 2 , event.description );
				ps.setInt( 3, event.getStartFrame() );
				ps.setInt( 4, event.getEndFrame() );

				if ( event.animalA != null )
				{
					ps.setInt( 5, (int) event.animalA.getDataBaseId() );
				}else
				{
					ps.setNull( 5 , java.sql.Types.INTEGER );
				}

				if ( event.animalB != null )
				{
					ps.setInt( 6, (int) event.animalB.getDataBaseId() );
				}else
				{
					ps.setNull( 6 , java.sql.Types.INTEGER );
				}

				if ( event.animalC != null )
				{
					ps.setInt( 7, (int) event.animalC.getDataBaseId() );
				}else
				{
					ps.setNull( 7 , java.sql.Types.INTEGER );
				}

				if ( event.animalD != null )
				{
					ps.setInt( 8, (int) event.animalD.getDataBaseId() );
				}else
				{
					ps.setNull( 8 , java.sql.Types.INTEGER );
				}

				ps.executeUpdate();
				ps.close();
				LiveMouseTracker.removeEventLogToDataBase(event);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

	}

	private void saveAnimals( Connection connection, TrackContainer trackContainer ) {

		AnimalPool animalPool = trackContainer.animalTrackSegmentPool;
		for ( Animal animal : animalPool.getAnimalList() )
		{
			insertAnimal( animal , connection );
		}

	}

	/** Save events
	 * Extensive use of cache to speed up save process.
	 * */
	public void saveEvents(Connection connection, TrackContainer trackContainer, boolean streamSave) {

		Integer startFrame = trackContainer.getFirstDetectionT();
		System.out.println("Save Event start frame: " + startFrame );
		Integer endFrame = LiveMouseTracker.getFilterUpperFrameLimit();

		if ( !streamSave )
		{
			startFrame = null;
			endFrame = null;
		}

		saveEvents(
				connection,
				trackContainer,
				startFrame ,
				endFrame );

	}

	public static void saveEvents(Connection connection, TrackContainer trackContainer, Integer startFrame , Integer endFrame )
	{
		AnimalPool animalPool = trackContainer.animalTrackSegmentPool;

		// save events
		for ( Animal animalA : animalPool.getAnimalList() )
		{
			// animalA speed.
			ChronoMouseSpeed chronoSpeedA = new ChronoMouseSpeed();
			chronoSpeedA.process( animalA , startFrame, endFrame );
			saveChronoEvent( connection , chronoSpeedA , animalA , null , null , null );

			// animalA no detection.
			ChronoDetectionPresent chronoNoDetection = new ChronoDetectionPresent();
			chronoNoDetection.process( animalA , startFrame , endFrame );
			saveChronoEvent( connection , chronoNoDetection , animalA , null, null , null );

			// animalA head detected.
			ChronoHeadDetected chronoHeadDetected = new ChronoHeadDetected();
			chronoHeadDetected.process( animalA , startFrame , endFrame );
			saveChronoEvent( connection , chronoHeadDetected , animalA , null, null , null );

			// animalA rearing
			ChronoRearing chronoRearing = new ChronoRearing();
			chronoRearing.process( animalA , startFrame , endFrame );
			saveChronoEvent( connection , chronoRearing , animalA , null, null , null);

			// animalA head down
			ChronoHeadDown chronoHeadDown = new ChronoHeadDown();
			chronoHeadDown.process( animalA , startFrame , endFrame );
			saveChronoEvent( connection , chronoHeadDown , animalA , null, null , null );

			// animalA stops.
			ChronoStop chronoStopA = new ChronoStop();
			chronoStopA.setChronoSpeedA( chronoSpeedA ); // cache
			chronoStopA.process( animalA , startFrame , endFrame );
			saveChronoEvent( connection , chronoStopA , animalA , null, null , null );

			for ( Animal animalB : animalPool.getAnimalList() )
			{
				if ( animalA == animalB ) continue; // we don't check the animal vs itself.

				// animalB speed. (computed for cache)

				ChronoMouseSpeed chronoSpeedB = new ChronoMouseSpeed();
				chronoSpeedB.process( animalB , startFrame, endFrame );

				ChronoApproach chronoAapproachB = new ChronoApproach();
				chronoAapproachB.setChronoSpeedA(chronoSpeedA); // cache
				chronoAapproachB.setChronoSpeedB(chronoSpeedB); // cache
				chronoAapproachB.process( animalA , animalB , startFrame, endFrame );
				saveChronoEvent( connection , chronoAapproachB , animalA, animalB, null , null );

				ChronoDistanceBetweenAnimal chronoDistanceBetweenAnimals = new ChronoDistanceBetweenAnimal();
				chronoDistanceBetweenAnimals.process( animalA , animalB , startFrame, endFrame );
				saveChronoEvent( connection , chronoDistanceBetweenAnimals , animalA, animalB, null , null );

				ChronoContact chronoContactAB = new ChronoContact();
				chronoContactAB.setChronoDistanceBetweenAnimals( chronoDistanceBetweenAnimals ); // cache
				chronoContactAB.process( animalA , animalB , startFrame, endFrame );
				saveChronoEvent( connection , chronoContactAB , animalA, animalB, null , null );

				ChronoEscape chronoEscapeAFromB = new ChronoEscape();
				chronoEscapeAFromB.setChronoSpeedA(chronoSpeedA); // cache
				chronoEscapeAFromB.setChronoSpeedB(chronoSpeedB); // cache
				chronoEscapeAFromB.process( animalA , animalB , startFrame, endFrame );
				// saveChronoEvent( connection , chronoEscapeAFromB , animalA, animalB, null , null );

				/*
				ChronoBehind chronoBehind = new ChronoBehind();
				chronoBehind.process( animalA , animalB , startFrame, endFrame );
				saveChronoEvent( connection , chronoBehind , animalA, animalB, null , null );
*/
				{
					ChronoBreakContact chronoBreakContact = new ChronoBreakContact();
					chronoBreakContact.setChronoContactAB(chronoContactAB);	// cache
					chronoBreakContact.setChronoEscapeAfromB( chronoEscapeAFromB ); // cache
					chronoBreakContact.process( animalA , animalB , startFrame, endFrame ); // cache
					saveChronoEvent( connection , chronoBreakContact , animalA, animalB, null , null );
				}

				ChronoSideBySide chronoSideBySide = new ChronoSideBySide();
				chronoSideBySide.process( animalA , animalB , startFrame, endFrame );
				saveChronoEvent( connection , chronoSideBySide , animalA, animalB, null , null );


				{
//					ChronoFollow chronoFollow = new ChronoFollow();
//					chronoFollow.setChronoSpeedA(chronoSpeedA); // cache
//					chronoFollow.setChronoSpeedB(chronoSpeedB); // cache
//					chronoFollow.setChronoAContactB(chronoContactAB); // cache
//					chronoFollow.setChronoABehindB(chronoBehind); // cache
//					//chronoFollow.setChronoASideBSameWay(chronoSideBySide);
//
//					chronoFollow.process( animalA , animalB , startFrame, endFrame );
//					saveChronoEvent( connection , chronoFollow , animalA, animalB, null , null );
				}

				{
//					ChronoGetInContact chronoGetInContact = new ChronoGetInContact();
//					chronoGetInContact.setChronoContactAB(chronoContactAB); // cache
//					chronoGetInContact.setChronoAapproachB(chronoAapproachB); // cache
//					chronoGetInContact.process( animalA , animalB , startFrame, endFrame );
//					saveChronoEvent( connection , chronoGetInContact , animalA, animalB, null , null );
				}

				{
					ChronoOralGenital chronoOralGenital = new ChronoOralGenital();
					chronoOralGenital.process( animalA , animalB , startFrame, endFrame );
					saveChronoEvent( connection , chronoOralGenital , animalA, animalB, null , null );
				}
				{
					ChronoOralOral chronoOralOral = new ChronoOralOral();
					chronoOralOral.process( animalA , animalB , startFrame, endFrame );
					saveChronoEvent( connection , chronoOralOral , animalA, animalB, null , null );
				}
				{
					ChronoSideBySideOpposite chronoSideBySideOpposite = new ChronoSideBySideOpposite();
					chronoSideBySideOpposite.process( animalA , animalB , startFrame, endFrame );
					saveChronoEvent( connection , chronoSideBySideOpposite , animalA, animalB, null , null );
				}

			}

		}
	}

	public static void delEventAt(Connection connection, String name, int t, Integer animalIdA, Integer animalIdB, Integer animalIdC, Integer animalIdD) {

		try {

			String sql = "DELETE FROM EVENT WHERE NAME=? AND STARTFRAME<=? AND ENDFRAME>=?";
			if ( animalIdA != null )
			{
				sql += " AND IDANIMALA="+animalIdA;
			}
			if ( animalIdB != null )
			{
				sql += " AND IDANIMALB="+animalIdB;
			}
			if ( animalIdC != null )
			{
				sql += " AND IDANIMALC="+animalIdC;
			}
			if ( animalIdD != null )
			{
				sql += " AND IDANIMALD="+animalIdD;
			}

			PreparedStatement ps;
			ps = connection.prepareStatement( sql, Statement.RETURN_GENERATED_KEYS );

			ps.setString( 1 , name );
			ps.setInt( 2, t );
			ps.setInt( 3, t );

			ps.executeUpdate();
			ps.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static void saveTimeLine(Connection connection, String name, String description ,
			EventTimeLine eventTimeLine, Integer animalIdA, Integer animalIdB, Integer animalIdC, Integer animalIdD ) {

		for ( Event event : eventTimeLine.getBooleanEventList() )
			{
				try {

					String sql = "INSERT INTO EVENT ( NAME, DESCRIPTION, STARTFRAME, ENDFRAME,"
							+ " IDANIMALA, IDANIMALB, IDANIMALC, IDANIMALD ) VALUES (?,?,?,?,?,?,?,?);";
					PreparedStatement ps;
					ps = connection.prepareStatement( sql, Statement.RETURN_GENERATED_KEYS );

					ps.setString( 1 , name );
					//ps.setString( 2 , description );
					ps.setString( 2 , "" );
					ps.setInt( 3, event.getStartFrame() );
					ps.setInt( 4, event.getEndFrame() );

					if ( animalIdA != null )
					{
						ps.setInt( 5, animalIdA );
					}else
					{
						ps.setNull( 5 , java.sql.Types.INTEGER );
					}

					if ( animalIdB != null )
					{
						ps.setInt( 6, animalIdB );
					}else
					{
						ps.setNull( 6 , java.sql.Types.INTEGER );
					}

					if ( animalIdC != null )
					{
						ps.setInt( 7, animalIdC );
					}else
					{
						ps.setNull( 7 , java.sql.Types.INTEGER );
					}

					if ( animalIdD != null )
					{
						ps.setInt( 8, animalIdD );
					}else
					{
						ps.setNull( 8 , java.sql.Types.INTEGER );
					}

					ps.executeUpdate();
					ps.close();

				} catch (SQLException e) {
					e.printStackTrace();
				}
			}

	}

	public static void saveEventTimeLine(Connection connection, EventTimeLine timeLine, String name, String description , Integer idAnimalA, Integer idAnimalB , Integer idAnimalC, Integer idAnimalD ) {

		try {
			connection.setAutoCommit( false );
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		for ( Event event : timeLine.getBooleanEventList() )
		{
			try {

				String sql = "INSERT INTO EVENT ( NAME, DESCRIPTION, STARTFRAME, ENDFRAME,"
						+ " IDANIMALA, IDANIMALB, IDANIMALC, IDANIMALD ) VALUES (?,?,?,?,?,?,?,?);";
				PreparedStatement ps;
				ps = connection.prepareStatement( sql, Statement.RETURN_GENERATED_KEYS );

				ps.setString( 1 , name );
				//ps.setString( 2 , description );
				ps.setString( 2 , "" );
				ps.setInt( 3, event.getStartFrame() );
				ps.setInt( 4, event.getEndFrame() );

				if ( idAnimalA != null )
				{
					ps.setInt( 5, (int) idAnimalA );
				}else
				{
					ps.setNull( 5 , java.sql.Types.INTEGER );
				}

				if ( idAnimalB != null )
				{
					ps.setInt( 6, (int) idAnimalB );
				}else
				{
					ps.setNull( 6 , java.sql.Types.INTEGER );
				}

				if ( idAnimalC != null )
				{
					ps.setInt( 7, idAnimalC );
				}else
				{
					ps.setNull( 7 , java.sql.Types.INTEGER );
				}

				if ( idAnimalD != null )
				{
					ps.setInt( 8, idAnimalD );
				}else
				{
					ps.setNull( 8 , java.sql.Types.INTEGER );
				}

				ps.executeUpdate();
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public static void saveChronoEvent(Connection connection, ChronoFeatureAbstract chrono, Animal animalA, Animal animalB, Animal animalC, Animal animalD ) {

//		Chronometer chronometer = new Chronometer("Saving chrono feature " + chrono.getName() + "/" + chrono.getDescription() );
//		System.out.println("Saving chronoFeature " + chrono.getName() + "/" + chrono.getDescription() );

		if ( chrono.getTimeLineDataType() == TimeLineDataType.BOOLEAN )
		{
			for ( Event event : chrono.getEventTimeLine().getBooleanEventList() )
			{
				try {

					String sql = "INSERT INTO EVENT ( NAME, DESCRIPTION, STARTFRAME, ENDFRAME,"
							+ " IDANIMALA, IDANIMALB , IDANIMALC, IDANIMALD ) VALUES (?,?,?,?,?,?,?,?);";
					PreparedStatement ps;
					ps = connection.prepareStatement( sql, Statement.RETURN_GENERATED_KEYS );

					ps.setString( 1 , chrono.getName() );
					//ps.setString( 2 , chrono.getDescription() );
					ps.setString( 2 , "" );
					ps.setInt( 3, event.getStartFrame() );
					ps.setInt( 4, event.getEndFrame() );

					if ( animalA != null )
					{
						ps.setInt( 5, (int) animalA.getDataBaseId() );
					}else
					{
						ps.setNull( 5 , java.sql.Types.INTEGER );
					}

					if ( animalB != null )
					{
						ps.setInt( 6, (int) animalB.getDataBaseId() );
					}else
					{
						ps.setNull( 6 , java.sql.Types.INTEGER );
					}

					if ( animalC != null )
					{
						ps.setInt( 7,  (int) animalC.getDataBaseId()  );
					}else
					{
						ps.setNull( 7 , java.sql.Types.INTEGER );
					}

					if ( animalD != null )
					{
						ps.setInt( 8,  (int) animalD.getDataBaseId()  );
					}else
					{
						ps.setNull( 8 , java.sql.Types.INTEGER );
					}

					ps.executeUpdate();
					ps.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}

		}


	}

	private void saveAnonymousTrack(Connection connection, TrackContainer trackContainer, boolean streamSave) {
		AnonymousPool anonymousTrackSegmentPool = trackContainer.anonymousTrackSegmentPool;
		// insert anonymous tracks
		{
			System.out.println("Save Anonymous tracks.");
			//ArrayList<TrackSegment> recordedTrackSegment = new ArrayList<TrackSegment>();

			for ( TrackSegment trackSegment: anonymousTrackSegmentPool.getTrackSegments() )
			{
				try{
					
					boolean filterTrack = false;
					
					if ( filterRecordStreamingTrack( trackSegment , streamSave ) )
					{
						filterTrack = true;
					}
					if ( trackSegment.getLength() > LiveMouseTracker.NUMBER_OF_FRAME_BEFORE_SEND_DATA_TO_STREAM )
					{
						filterTrack = false; // the track will be flushed to database and removed.
						System.out.println("Removing too long anonymous track for the sake of memory");
					}					
					
					if ( filterTrack )
					{
						continue; // The track is skipped, not flush from memory
					}
					
					//	recordedTrackSegment.add( trackSegment );

					String sql ="";
					PreparedStatement ps = connection.prepareStatement( sql, Statement.RETURN_GENERATED_KEYS );
					//					String sql = "INSERT INTO TRACK ( ANIMALID ) VALUES ( ? );";
					//					PreparedStatement ps = connection.prepareStatement( sql, Statement.RETURN_GENERATED_KEYS );
					//					ps.setNull( 1, java.sql.Types.INTEGER );
					//					ps.executeUpdate();

					//					long trackId = -1;
					//					ResultSet generatedKeys = ps.getGeneratedKeys();
					//					if (generatedKeys.next()) {
					//						trackId = generatedKeys.getLong(1);
					//					}
					//					System.out.println("Track ID: " + trackId );
					System.out.println("Track ID: " + trackSegment );

					// add detection
					for ( MouseDetection detection: trackSegment.getDetectionList() )
					{
						/*
						sql = "INSERT INTO DETECTION (TIME, X, Y,DATA, TRACKID ) VALUES (?,?,?,?,?);";
						ps = connection.prepareStatement( sql, Statement.RETURN_GENERATED_KEYS );
						ps.setInt( 1 , detection.getT() );
						ps.setInt( 2 , (int)detection.getMassCenter().getX() ); // FIX INT EN DOUBLE
						ps.setInt( 3 , (int)detection.getMassCenter().getY() ); // FIX INT EN DOUBLE
						String data = detection.getAsXMLData(
								(SQLiteSavedData) LiveMouseTracker.guiPanel.getStreamSQLSaveType().getSelectedItem()
								);
						ps.setString( 4 , data );
						ps.setLong( 5 , trackId );
						ps.executeUpdate();
						 */
						sql = "INSERT INTO DETECTION ( FRAMENUMBER, ANIMALID, MASS_X,MASS_Y,MASS_Z, FRONT_X, FRONT_Y, FRONT_Z, BACK_X, BACK_Y, BACK_Z, REARING, LOOK_UP, LOOK_DOWN, DATA ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,? );";
						ps = connection.prepareStatement( sql, Statement.RETURN_GENERATED_KEYS );
						ps.setInt( 1 , detection.getT() );
						ps.setNull( 2 , java.sql.Types.INTEGER ); // anonymous animal
						ps.setDouble( 3 , lowPrecision( detection.getMassCenter().getX() ) );
						ps.setDouble( 4 , lowPrecision( detection.getMassCenter().getY() ) );
						ps.setDouble( 5 , lowPrecision( detection.getMassCenter().getZ() ) );

						if ( detection.getFrontPoint() != null )
						{
							ps.setDouble( 6 , lowPrecision( detection.getFrontPoint().getX() ) );
							ps.setDouble( 7 , lowPrecision( detection.getFrontPoint().getY() ) );
							ps.setDouble( 8 , lowPrecision( detection.getFrontPoint().getZ() ) );
						}else
						{
							ps.setDouble( 6 , -1 );
							ps.setDouble( 7 , -1 );
							ps.setDouble( 8 , -1 );
						}

						if ( detection.getBackPoint() != null )
						{
							ps.setDouble( 9 , lowPrecision( detection.getBackPoint().getX() ) );
							ps.setDouble( 10 , lowPrecision( detection.getBackPoint().getY() ) );
							ps.setDouble( 11 , lowPrecision( detection.getBackPoint().getZ() ) );
						}else
						{
							ps.setDouble( 9 , -1 );
							ps.setDouble( 10 , -1 );
							ps.setDouble( 11 , -1 );
						}

						ps.setInt( 12 , detection.isRearing() ? 1 : 0 );
						ps.setInt( 13 , detection.isLookingUp() ? 1 : 0 );
						ps.setInt( 14 , detection.isLookingDown() ? 1 : 0 );

						String data = detection.getAsXMLData( SQLiteSavedData.ALL );

						//						String data = detection.getAsXMLData( (SQLiteSavedData) LiveMouseTracker.guiPanel.getStreamSQLSaveType().getSelectedItem() );

						ps.setString( 15 , data );
						//						ps.setLong( 16 , trackId );
						ps.executeUpdate();
					}
					ps.close();
					anonymousTrackSegmentPool.removeTrack( trackSegment );

				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			//				if ( streamSave )
			//				{
			//					anonymousTrackSegmentPool.removeAllTrack( recordedTrackSegment );
			//				}
		}


	}

	// TODO: protect SQL query by query
	private void saveAnimalTrack(Connection connection, TrackContainer trackContainer, boolean streamSave) {

		AnimalPool animalPool = trackContainer.animalTrackSegmentPool;
		// insert animals
		for ( Animal animal : animalPool.getAnimalList() )
		{
//			insertAnimal( animal , connection );
			long animalId = animal.getDataBaseId();

			// record of track that will be saved. (so that we can flush them after).
			//ArrayList<TrackSegment> recordedTrackSegment = new ArrayList<>();
			// insert tracks.
			for ( TrackSegment trackSegment: animal.getTrackSegments() )
			{
				try {
					// filter track that will be recorded.
					// we only saved track at least finished 1 minute ago
					if ( filterRecordStreamingTrack( trackSegment, streamSave ) ) continue;

					PreparedStatement ps;
					String sql ="";

					//ps = connection.prepareStatement( sql, Statement.RETURN_GENERATED_KEYS );


					System.out.println("Saving track : " + trackSegment );


					for ( MouseDetection detection: trackSegment.getDetectionList() )
					{
						sql = "INSERT INTO DETECTION (FRAMENUMBER, ANIMALID, MASS_X,MASS_Y,MASS_Z, FRONT_X, FRONT_Y, FRONT_Z, BACK_X, BACK_Y, BACK_Z, REARING, LOOK_UP, LOOK_DOWN, DATA ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,? );";
						ps = connection.prepareStatement( sql, Statement.RETURN_GENERATED_KEYS );
						ps.setInt( 1 , detection.getT() );
						ps.setLong( 2 , animalId );
						ps.setDouble( 3 , lowPrecision( detection.getMassCenter().getX() ) );
						ps.setDouble( 4 , lowPrecision( detection.getMassCenter().getY() ) );
						ps.setDouble( 5 , lowPrecision( detection.getMassCenter().getZ() ) );

						if ( detection.getFrontPoint() != null )
						{
							ps.setDouble( 6 , lowPrecision( detection.getFrontPoint().getX() ) );
							ps.setDouble( 7 , lowPrecision( detection.getFrontPoint().getY() ) );
							ps.setDouble( 8 , lowPrecision( detection.getFrontPoint().getZ() ) );
						}else
						{
							ps.setDouble( 6 , -1 );
							ps.setDouble( 7 , -1 );
							ps.setDouble( 8 , -1 );
						}

						if ( detection.getBackPoint() != null )
						{
							ps.setDouble( 9 , lowPrecision( detection.getBackPoint().getX() ) );
							ps.setDouble( 10 , lowPrecision( detection.getBackPoint().getY() ) );
							ps.setDouble( 11 , lowPrecision( detection.getBackPoint().getZ() ) );
						}else
						{
							ps.setDouble( 9 , -1 );
							ps.setDouble( 10 , -1 );
							ps.setDouble( 11 , -1 );
						}

						ps.setInt( 12 , detection.isRearing() ? 1 : 0 );
						ps.setInt( 13 , detection.isLookingUp() ? 1 : 0 );
						ps.setInt( 14 , detection.isLookingDown() ? 1 : 0 );

						String data = detection.getAsXMLData( SQLiteSavedData.ALL );

						//						String data = detection.getAsXMLData( (SQLiteSavedData) LiveMouseTracker.guiPanel.getStreamSQLSaveType().getSelectedItem() );

						ps.setString( 15 , data );
						//						ps.setLong( 16 , trackId );
						ps.executeUpdate();
						//						ps.setString( 14 , data );
						//						ps.setLong( 15 , trackId );
						//						ps.executeUpdate();

						ps.close();
					}

					if ( streamSave )
					{
						animal.removeTrackSegment( trackSegment );
					}
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			// remove all recorded tracks.
			//				if ( streamSave )
			//				{
			//					animal.removeAllTrackSegment( recordedTrackSegment );
			//				}
		}


	}

	private double lowPrecision(double x) {
		return (int)(Math.round(x * 100)) / 100d;
	}


	private void saveFrameData(Connection connection, TrackContainer trackContainer, boolean streamSave) {

//		synchronized ( LiveMouseTracker.frameInfoList ) {
//			System.out.println("Frame info size: " + LiveMouseTracker.frameInfoList.size() );
//		}

		{ // insert frameData
			ArrayList<FrameInfo> recordedFrameInfo = new ArrayList<FrameInfo>();
			ArrayList<FrameInfo> frameInfoListCopy = null;
			synchronized ( LiveMouseTracker.frameInfoList )
			{
				frameInfoListCopy = new ArrayList<FrameInfo>( LiveMouseTracker.frameInfoList );
			}


			for( FrameInfo frameInfo : frameInfoListCopy )
			{
				if ( filterTimePoint( frameInfo.getFrameNumber() , streamSave ) ) continue;
				recordedFrameInfo.add( frameInfo );
			}


			for( FrameInfo frameInfo : recordedFrameInfo )
			{
				String sql ="";
				try
				{
					PreparedStatement ps;

					sql = "INSERT INTO FRAME ( FRAMENUMBER, TIMESTAMP, NUMPARTICLE , PAUSED, TEMPERATURE, HUMIDITY,SOUND,LIGHTVISIBLE,LIGHTVISIBLEANDIR) VALUES ( ? , ? , ? , ?,?,?,?,?,? );";
					ps = connection.prepareStatement( sql, Statement.RETURN_GENERATED_KEYS );
					ps.setInt( 1 , frameInfo.getFrameNumber() );
					ps.setDate( 2 , frameInfo.getDateTime() );
					ps.setInt( 3 , frameInfo.getNbParticle() );
					ps.setBoolean( 4 , frameInfo.isPauseAllProcess() );
					ps.setDouble( 5 , frameInfo.getTemperature() );
					ps.setDouble( 6 , frameInfo.getHumidity() );
					ps.setDouble( 7 , frameInfo.getSound() );
					ps.setDouble( 8 , frameInfo.getLightVisible() );
					ps.setDouble( 9 , frameInfo.getLightVisibleAndInfrared() );
					ps.executeUpdate();
					ps.close();
				}
				catch ( SQLException e )
				{
					System.err.println("Error in SQL query: " + sql );
					e.printStackTrace();
				}
			}

			// remove all recorded frameInfos if in streaming
			if ( streamSave )
			{
				//System.out.println("[FRAMEINFO] Saving " + frameInfo.getFrameNumber() );
				synchronized ( LiveMouseTracker.frameInfoList ) {
//					for ( FrameInfo frameInfo : LiveMouseTracker.frameInfoList )
//					{
//						LiveMouseTracker.frameInfoList.remove( frameInfo );
//					}
					LiveMouseTracker.frameInfoList.removeAll( recordedFrameInfo );
				}
			}
		}

	}

	/** return true if track must be filtered (i.e. not recorded )*/
	private boolean filterRecordStreamingTrack(TrackSegment trackSegment  , boolean streamMode ) {

		//if ( trackSegment.getLastTimePoint() > LiveMouseTracker.getT() - 60*60 ) // Fixme: put it to 5 minutes.

		return filterTimePoint( trackSegment.getLastTimePoint() , streamMode );

//		{
//			return true;
//		}
//		return false;
	}

	

	/** return true if time point must be filtered (i.e. not recorded )*/
	private boolean filterTimePoint( int t , boolean streamMode )
	{
		if ( !streamMode ) return false; // if we are not in stream mode, say filter nothing.

		if ( t > LiveMouseTracker.getFilterUpperFrameLimit() ) //LiveMouseTracker.getT() - LiveMouseTracker.NUMBER_OF_FRAME_BEFORE_SEND_DATA_TO_STREAM )
		{
			return true;
		}
		return false;
	}


	private void insertAnimal( Animal animal, Connection connection ) {

		if ( animal.getDataBaseId() != -1 )
		{
			System.out.println("Animal is already in base. Updating RFID.");
			String sql = "UPDATE ANIMAL SET RFID = ? WHERE ID = ?;";
			PreparedStatement ps;
//			long animalId = -1;

			try {
				ps = connection.prepareStatement( sql, Statement.RETURN_GENERATED_KEYS );
				ps.setString( 1 , animal.getRfidID() );
				ps.setLong( 2 , animal.getDataBaseId() );
				ps.executeUpdate();
				ps.close();
//				ResultSet generatedKeys = ps.getGeneratedKeys();
//				if (generatedKeys.next()) {
//					animalId = generatedKeys.getLong(1);
//					animal.setDataBaseId( animalId );
//				}
				System.out.println("Animal " + animal.getName() + " databaseId: " + animal.getDataBaseId() );

			} catch (SQLException e) {
				e.printStackTrace();
			}

			return;
		}

		String sql = "INSERT INTO ANIMAL (RFID,NAME) "
				+ "VALUES ( ? , ? );";
		System.out.println("Insert animal in base.");
		PreparedStatement ps;
		long animalId = -1;

		try {
			ps = connection.prepareStatement( sql, Statement.RETURN_GENERATED_KEYS );
			ps.setString( 1 , animal.getRfidID() );
			ps.setString( 2 , animal.getName() );
			ps.executeUpdate();

			ResultSet generatedKeys = ps.getGeneratedKeys();
			if (generatedKeys.next()) {
				animalId = generatedKeys.getLong(1);
				animal.setDataBaseId( animalId );
				System.out.println("Animal " + animal.getName() + " databaseId: " + animal.getDataBaseId() );
			}
			generatedKeys.close();
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	private Connection connectDataBase() {

		String dataBaseFilePath = getDataBaseAbsoluteFilePath();
		System.out.println("Connecting to " + dataBaseFilePath );
		FileUtil.ensureParentDirExist( dataBaseFilePath );

		Connection connection = null;
		try {
			Class.forName("org.sqlite.JDBC");
			connection = DriverManager.getConnection("jdbc:sqlite:"+dataBaseFilePath );
			System.out.println("Opened database successfully");

		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);

		}
		createDataBase(connection);
		return connection;

	}

	// DATABASE


	private void executeSQL(Connection c, String sql) {

		Statement statement;
		try {
			statement = c.createStatement();
			statement.execute( sql );
			statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	private void createDataBase(Connection c) {

		int numberOfTable = getNumberOfTable( c );
		if ( numberOfTable >= 3 )
		{
			System.out.println("Database already created.");
			return;
		}else
		{
			System.out.println("Creating database.");
		}

		Statement statement = null;
		try {
			statement = c.createStatement();
			String sql = "CREATE TABLE 'ANIMAL' ( "
					+ " 'ID'	INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,"
					+ " 'RFID'	TEXT,"
					+ " 'GENOTYPE'	TEXT,"
					+ " 'NAME'	TEXT );";
			statement.execute(sql);

			/*
			 * CREATE TABLE "DETECTION" (
	`ID`	INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
	`ANIMALID`	INTEGER,
	`FRAMEID`	INTEGER,
	`MASS_X`	REAL,
	`MASS_Y`	REAL,
	`MASS_Z`	REAL,
	`FRONT_X`	REAL,
	`FRONT_Y`	REAL,
	`FRONT_Z`	REAL,
	`BACK_X`	REAL,
	`BACK_Y`	REAL,
	`BACK_Z`	REAL,
	`REARING`	INTEGER,
	`LOOK_UP`	INTEGER,
	`LOOK_DOWN`	INTEGER,
	`DATA`	TEXT NOT NULL,
	FOREIGN KEY(`ANIMALID`) REFERENCES `ANIMAL`(`ID`),
	FOREIGN KEY(`FRAMEID`) REFERENCES FRAME('ID')
)
			 */

			sql = "CREATE TABLE 'DETECTION' ( "
					+ "'ID' INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, "
					+ " 'FRAMENUMBER' INTEGER, "
					+ " 'ANIMALID' INTEGER, "
					+ " 'MASS_X'	REAL, "
					+ " 'MASS_Y'	REAL, "
					+ " 'MASS_Z'	REAL, "
					+ " 'FRONT_X'	REAL, "
					+ " 'FRONT_Y'	REAL, "
					+ " 'FRONT_Z'	REAL, "
					+ " 'BACK_X'	REAL, "
					+ " 'BACK_Y'	REAL, "
					+ " 'BACK_Z'	REAL, "
					+ " 'REARING'	INTEGER, "
					+ " 'LOOK_UP'	INTEGER, "
					+ " 'LOOK_DOWN'	INTEGER, "
					+ " 'DATA'	TEXT NOT NULL, "
					+ " FOREIGN KEY('ANIMALID') REFERENCES ANIMAL('ID'), "
					+ " FOREIGN KEY('FRAMENUMBER') REFERENCES FRAME('FRAMENUMBER') "
					+ ");";
			statement.execute(sql);

//			sql = "CREATE TABLE 'TRACK' ("
//					+ "	'ID'	INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT ,"
//					+ " 'ANIMALID'	INTEGER, "
//					+ " FOREIGN KEY('ANIMALID') REFERENCES ANIMAL(ID) ); ";
//
//			statement.execute(sql);

			sql = "CREATE TABLE 'RFIDEVENT' ("
					+ " 'ID'	INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ " 'RFID'	TEXT,"
					+ " 'TIME'	INTEGER,"
					+ " 'X'	INTEGER,"
					+ " 'Y'	INTEGER );";
			statement.execute(sql);

			sql = "CREATE TABLE 'FRAME' ("
					+"'FRAMENUMBER'	INTEGER NOT NULL UNIQUE,"
					+"'TIMESTAMP'	INTEGER,"
					+"'NUMPARTICLE'	INTEGER,"
					+"'PAUSED'	INTEGER,"
					+"'TEMPERATURE'	REAL,"
					+"'HUMIDITY' REAL,"
					+"'SOUND' REAL,"
					+"'LIGHTVISIBLE' REAL,"
					+"'LIGHTVISIBLEANDIR' REAL,"
					+"PRIMARY KEY(FRAMENUMBER) );";
			statement.execute(sql);

			sql = "CREATE TABLE 'EVENT' ("
				+"'ID'	INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,"
				+"'NAME'	TEXT," // should be a list
				+"'DESCRIPTION'	TEXT,"
				+"'STARTFRAME'	INTEGER,"
				+"'ENDFRAME'	INTEGER,"
				+"'IDANIMALA'	INTEGER," // should be a list
				+"'IDANIMALB'	INTEGER,"
				+"'IDANIMALC'	INTEGER,"
				+"'IDANIMALD'	INTEGER,"
				+"'METADATA'	TEXT );";
			statement.execute(sql);

			// indexes
			sql = "CREATE INDEX 'animalIndex' ON 'ANIMAL' ('ID' );";
			statement.execute(sql);

			sql = "CREATE INDEX 'detectionIndex' ON 'DETECTION' ('ID' ASC,'FRAMENUMBER' ASC);";
			statement.execute(sql);

			sql = "CREATE INDEX 'detetIdIndex' ON 'DETECTION' ('ID' ASC);";
			statement.execute(sql);

			sql = "CREATE INDEX 'detframenumberIndex' ON 'DETECTION' ('FRAMENUMBER' ASC);";
			statement.execute(sql);

			sql = "CREATE INDEX 'eventEndFrameIndex' ON 'EVENT' ('ENDFRAME' ASC);";
			statement.execute(sql);

			sql = "CREATE INDEX 'eventIndex' ON 'EVENT' ('ID' ASC,'STARTFRAME' ASC,'ENDFRAME' ASC);";
			statement.execute(sql);

			sql = "CREATE INDEX 'eventStartFrameIndex' ON 'EVENT' ('STARTFRAME' ASC);";
			statement.execute(sql);

			sql = "CREATE INDEX 'eventstartendIndex' ON 'EVENT' ('STARTFRAME' ASC,'ENDFRAME' ASC);";
			statement.execute(sql);

			sql = "CREATE INDEX 'indexeventidIndex' ON 'EVENT' ('ID' ASC);";
			statement.execute(sql);

			sql = "CREATE INDEX 'detectionFastLoadXYIndex' ON 'DETECTION' ('ANIMALID' ,'FRAMENUMBER' ASC,'MASS_X' ,'MASS_Y' );";
			statement.execute(sql);

			statement.close();

		} catch (Exception e) {

			e.printStackTrace();
		}
	}

	private int getNumberOfTable(Connection c) {

		Statement st;
		try {
			st = c.createStatement();
			ResultSet rs = st.executeQuery("select count(*) from sqlite_master as tables where type='table' AND name != 'android_metadata' AND name != 'sqlite_sequence'");
			int nb = rs.getInt(1);
			st.close();
			return nb;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public String getInfos() {

		String infos = "";
		infos+="Name: " ; //+ name;
		infos+="\nDate: TODO";

		infos+="\nNumber of Animal(s): " + getAnimalPool().getAnimalList().size();
		infos+="\nAnimal list:";

		for ( Animal animal: getAnimalPool().getAnimalList() )
		{
			infos+="\n"+animal.getName() + " - RFID:" + animal.getRfidID();
		}

		return infos;

	}

	public int getNumberOfFrame() {

		int maxT = 0;

//		System.out.println("track for animal:");
//		System.out.println( animalPool.getAnimalList().get( 0 ).getTrackSegmentList().size() );

		for ( TrackSegment ts: getAnimalPool().getTrackSegments() )
		{
			int t = ts.getLastTimePoint();
			if ( t > maxT ) maxT = t;
		}
		synchronized (trackContainer.anonymousTrackSegmentPool.trackSegmentArrayList) {
			for ( TrackSegment ts: trackContainer.anonymousTrackSegmentPool.trackSegmentArrayList )
			{
				int t = ts.getLastTimePoint();
				if ( t > maxT ) maxT = t;
			}
		}

//		System.out.println( "maxT :" + maxT );
		return maxT;

	}

	public String getInfraRawImageFile(int value) {

		//name
		String filePath = baseFolder + "/infra/infra" + plugins.fab.kinectdriver.NumberFormat.format( value )+".png";
		return filePath;

	}

	public TrackSegment getPreviousAnonymousTrack(int t) {

		int maxFound = Integer.MIN_VALUE;
		TrackSegment bestTrackSegment = null;

		for ( TrackSegment ts : getAnonymousTrackList() )
		{
			int trackT = ts.getFirstTimePoint();
			if ( trackT < t )
			{
				if ( trackT > maxFound )
				{
					maxFound = trackT;
					bestTrackSegment = ts;
				}
			}
		}

		return bestTrackSegment;


	}

	public TrackSegment getNextAnonymousTrack(int t) {

		int minFound = Integer.MAX_VALUE;
		TrackSegment bestTrackSegment = null;

		for ( TrackSegment ts : getAnonymousTrackList() )
		{
			int trackT = ts.getFirstTimePoint();
			if ( trackT > t )
			{
				if ( trackT < minFound )
				{
					minFound = trackT;
					bestTrackSegment = ts;
				}
			}
		}

		return bestTrackSegment;

	}

	/** FIXME: should be in track container ? */
	public void remove(TrackSegment trackSegment ) {

		try {
			Connection connection = connectDataBase();
			String sql = "DELETE FROM TRACK WHERE ID=?;";
			PreparedStatement ps;
			ps = connection.prepareStatement( sql );
			ps.setInt( 1 , trackSegment.getDataBaseId() );
			ps.execute();
			ps.close();
			connection.close();
			trackContainer.removeTrack( trackSegment );

		} catch (SQLException e) {
			e.printStackTrace();
		}
		System.out.println("track removed");
	}

//	/** FIXME: this all thing should be manage by the TrackContainer object
//	 *
//	 * Should test if operation is possible
//	 *
//	 *@deprecated use the setAnonymousTrack
//	 * */
//		public void setAnonymousTrackIdentity( int trackId , Animal animal )
//		{
//			// find track to identify in anonymous track Set
//			for ( TrackSegment ts : getAnonymousTrackList() )
//			{
//				if ( ts.getDataBaseId() == trackId )
//				{
//					try {
//						Connection connection = connectDataBase();
//						String sql = "UPDATE TRACK SET ANIMALID=? WHERE ID=?;";
//						PreparedStatement ps;
//						ps = connection.prepareStatement( sql );
//						ps.setInt( 1 , animal.getDataBaseId() );
//						ps.setInt( 2 , trackId );
//						ps.execute();
//
//						trackContainer.anonymousTrackSegmentPool.removeTrack( ts );
////						anonymousTrackList.remove( ts );
//						animal.addTrackSegment( ts );
//
//						System.out.println("ID affect ok.");
//
//					} catch (SQLException e) {
//						e.printStackTrace();
//					}
//
//
//					return;
//				}
//				System.out.println("Can't find track.");
//			}
//
//		}

	public void setTrackIdentity(int trackId, Animal targetAnimal) {

		TrackSegment ts = trackContainer.getTrackId( trackId );
		if ( ts == null )
		{
			System.out.println("set track identity error: Can't find track segment.");
		}


		try {
			Connection connection = connectDataBase();
			String sql = "UPDATE TRACK SET ANIMALID=? WHERE ID=?;";
			PreparedStatement ps;
			ps = connection.prepareStatement( sql );
			if ( targetAnimal == null )
			{	// set anonymous
				ps.setNull( 1 , java.sql.Types.INTEGER );
			}else
			{
				ps.setLong( 1 , targetAnimal.getDataBaseId() );
			}
			ps.setInt( 2 , trackId );
			ps.execute();
			ps.close();

			trackContainer.removeTrack( ts );
			if ( targetAnimal == null )
			{
				trackContainer.anonymousTrackSegmentPool.add( ts );
			}else
			{
				targetAnimal.addTrackSegment( ts, false ); // without security check
			}

			System.out.println("ID affect ok.");

		} catch (SQLException e) {
			e.printStackTrace();
		}

	}







}
