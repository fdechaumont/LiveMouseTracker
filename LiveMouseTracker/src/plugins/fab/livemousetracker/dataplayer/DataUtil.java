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

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Callable;

import javax.swing.JFileChooser;

import icy.plugin.abstract_.Plugin;
import icy.system.thread.ThreadUtil;
import plugins.fab.livemousetracker.Animal;
import plugins.fab.livemousetracker.dataplayer.DBAnimal;
import plugins.fab.livemousetracker.track.TrackContainer;

public class DataUtil {

	public static ArrayList<DBAnimal> loadMice( Connection connection ) throws SQLException {

		ArrayList<DBAnimal> animalList = new ArrayList<DBAnimal>();

    	String sql = "SELECT * FROM ANIMAL ORDER BY ID";
    	PreparedStatement ps;
    	ps = connection.prepareStatement( sql );

    	ResultSet rs = ps.executeQuery( );

    	animalList.clear();

    	while ( rs.next() )
    	{
    		String rfid = rs.getString( "rfid" );
    		int id = rs.getInt( "id" );
    		String genotype = "";
    		try
    		{
    			genotype = rs.getString( "genotype" );
    		} catch ( SQLException e )
    		{
    			genotype = "NO GENOTYPE";
    			System.err.println("WARNING: NO GENOTYPE FIELD IN DATABASE");
    		}

    		animalList.add( new DBAnimal( id, rfid , genotype ) );
    	}
    	rs.close();

    	return animalList;

    }

	public static DBAnimal getAnimal(long id , ArrayList<DBAnimal> animalList ) {

		for( DBAnimal animal : animalList )
		{
			if ( animal.id == id ) return animal;
		}

		System.out.println("getAnimal : Can't find id: " + id );

		return null;
	}

	public static int getMaxNumberOfFrame(Connection connection) {

		int max = 0;

		try {

			String sql = "SELECT MAX(FRAMENUMBER) AS MAXFRAMENUMBER FROM DETECTION";
			PreparedStatement ps;
			ps = connection.prepareStatement( sql );

			ResultSet rs = ps.executeQuery( );

			while ( rs.next() )
			{
				max = rs.getInt( "MAXFRAMENUMBER" );
				System.out.println( "Max number : " + max );
			}
			rs.close();


    	} catch (SQLException e) {
    		e.printStackTrace();
    	}

		return max;

	}

	public static void closeConnection( Connection connection ) {

		if ( connection!=null )
		{
			try {
				if ( ! connection.getAutoCommit() )
				{
					connection.commit();
				}
				connection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		connection = null;

	}

	public static File[] selectDataBaseFiles( final Plugin plugin ) {

		try {
			return ThreadUtil.invokeNow( new Callable<File[]>()  {

				@Override
				public File[] call() throws Exception {
					File dataBaseFiles[] = null;

					JFileChooser fileChooser = new JFileChooser();

					fileChooser.setDialogTitle("Choose the database");

					String directory = plugin.getPreferences("folder").get("prefFolder", "");

					fileChooser.setCurrentDirectory( new File( directory ) ) ;
					fileChooser.setMultiSelectionEnabled( true );

					if ( fileChooser.showOpenDialog( null ) == JFileChooser.APPROVE_OPTION )
					{
						dataBaseFiles = fileChooser.getSelectedFiles();
						System.out.println("Files = " + dataBaseFiles );
						plugin.getPreferences("folder").put( "prefFolder", dataBaseFiles[0].getAbsolutePath() );
					}

					return dataBaseFiles;
				}
			});

		} catch (Exception e) {

			e.printStackTrace();
		}
		return null;

	}

	/** Connect to database and return its connection.
	 * If already connected. Does nothing.
	 */
	public static Connection connectDataBase( Connection connection , File dataBaseFile ) {

		if ( connection != null ) return connection;

		try {
			Class.forName("org.sqlite.JDBC");
			connection = DriverManager.getConnection("jdbc:sqlite:"+dataBaseFile.getAbsoluteFile() );
//			System.out.println("Opened database successfully");

		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);

		}
		return connection;

	}

	public static TrackContainer loadAnimalData( Connection connection , int frameStart, int frameEnd )
	{
		TrackContainer trackContainer = new TrackContainer();

		// load animals

		ArrayList<DBAnimal> db_animalList;
		try {
			db_animalList = DataUtil.loadMice( connection );
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}

		// rebuild animal data

		for ( DBAnimal db_animal : db_animalList )
		{
			Animal animal = new Animal("name");
			db_animal.animal = animal;
			animal.setRfidID( db_animal.RFID );
			animal.setDataBaseId( db_animal.id );
			trackContainer.animalTrackSegmentPool.addAnimal( animal );
		}

		// load detections
		ArrayList<MouseDetectionX> detectionList ;

		try {
			detectionList = LiveDataPlayer.loadDetection( connection, frameStart, frameEnd , true , null );
		} catch (SQLException e1) {
			e1.printStackTrace();
			return null;
		}

//		HashMap<Integer, DetectAtT> detectMap = new HashMap<Integer, DetectAtT>();
//
//		for ( MouseDetectionX detection : detectionList )
//		{
//			int t = detection.mouseDetection.getT();
//			DetectAtT map = detectMap.get( t );
//			if ( map == null )
//			{
//				map = new DetectAtT();
//				detectMap.put( t , map );
//			}
//			map.detection.add( detection );
//		}

		// recreate tracks
		// populate detection per animals

		synchronized( detectionList )
		{
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
		}


			for ( DBAnimal db_animal : db_animalList )
			{
				db_animal.computeTracks();
				db_animal.animal.addAll( db_animal.trackList );
			}


		return trackContainer;

	}

	public static TrackContainer loadSimpleAnimalData( Connection connection , int frameStart, int frameEnd )
	{
		TrackContainer trackContainer = new TrackContainer();

		// load animals

		ArrayList<DBAnimal> db_animalList;
		try {
			db_animalList = DataUtil.loadMice( connection );
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}

		// rebuild animal data

		for ( DBAnimal db_animal : db_animalList )
		{
			Animal animal = new Animal("name");
			db_animal.animal = animal;
			animal.setRfidID( db_animal.RFID );
			animal.setDataBaseId( db_animal.id );
			trackContainer.animalTrackSegmentPool.addAnimal( animal );
		}

		// load detections

		ArrayList<MouseDetectionX> detectionList ;

		try {
			detectionList = LiveDataPlayer.loadSimpleDetection( connection, frameStart, frameEnd , true);
		} catch (SQLException e1) {
			e1.printStackTrace();
			return null;
		}

		// recreate tracks
		// populate detection per animals

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


		return trackContainer;

	}


}
