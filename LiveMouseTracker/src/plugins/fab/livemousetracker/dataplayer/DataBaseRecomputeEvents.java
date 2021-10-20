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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

import icy.system.profile.Chronometer;
import plugins.fab.livemousetracker.Animal;
import plugins.fab.livemousetracker.experiment.Experiment;
import plugins.fab.livemousetracker.track.TrackContainer;

/** Recompute all the events of a database */
public class DataBaseRecomputeEvents {

	public static void recomputeEvents( Connection connection )
	{
		Chronometer chrono = new Chronometer("Recompute all events");
		// Rebuild data to rebuild events

		TrackContainer trackContainer = new TrackContainer();

		// load animals

		ArrayList<DBAnimal> db_animalList;
		try {
			db_animalList = DataUtil.loadMice(connection);
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
			System.out.println("ID: " + db_animal.id );
			trackContainer.animalTrackSegmentPool.addAnimal( animal );
		}

		// load detections
		ArrayList<MouseDetectionX> detectionList ;

		try {
			detectionList = LiveDataPlayer.loadDetection( connection, 0, Integer.MAX_VALUE  , false, null );
		} catch (SQLException e1) {
			e1.printStackTrace();
			return;
		}

		// recreate tracks

		// search max t value in detection

		int max = max ( detectionList );

		// populate detection per animals

		for ( MouseDetectionX detection : detectionList )
		{
			long animalId = detection.animalId;

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

		// delete all events in database.

		System.out.println("Deleting all events...");
		// DELETE * FROM EVENTS

		String sql = "DELETE FROM EVENT";
    	PreparedStatement ps;
    	try {

    		ps = connection.prepareStatement( sql );
    		ps.executeUpdate();

    	} catch (SQLException e) {
    		e.printStackTrace();
    	}

		// recomputing all events

    	System.out.println("Saving...");
    	int maxValue = 0;

    	// get max T value

    	for ( DBAnimal db_animal : db_animalList )
		{
			maxValue = Math.max( maxValue , db_animal.maxTValue );
		}

    	System.out.println("Max T value : " + maxValue );
		try {

			connection.setAutoCommit( false );
			Experiment.saveEvents(connection, trackContainer, 0, maxValue );
			connection.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		chrono.displayInSeconds();
	}

	private static int max(ArrayList<MouseDetectionX> detectionList) {

		int max = 0 ;

		for ( MouseDetectionX mouseDetection : detectionList )
		{
			if ( mouseDetection.mouseDetection.getT() > max )
			{
				max = mouseDetection.mouseDetection.getT();
			}
		}

		return max;
	}

}
