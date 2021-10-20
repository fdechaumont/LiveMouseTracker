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

import java.awt.geom.Point2D;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import plugins.fab.livemousetracker.liveanalysis.chronogram.EventTimeLine;
import plugins.fab.livemousetracker.liveanalysis.chronogram.TimeLineDataType;

/** loads x,y data for all the timeline of an animal for simple processes */

public class SimpleAnimalLocation {

	HashMap<Integer,Point2D> timeToPointHashMap = new HashMap<Integer,Point2D>();
	int id;

	public SimpleAnimalLocation(Connection connection, int id ) {
		this( connection , id, 0 , Integer.MAX_VALUE );
	}

	public SimpleAnimalLocation(Connection connection, int id , int startFrame, int endFrame ) {

		this.id = id;
		System.out.println("Loading simpleAnimalLocation id: " + id );

		try {
			String sql = "SELECT FRAMENUMBER,MASS_X,MASS_Y FROM DETECTION WHERE ANIMALID=? AND FRAMENUMBER>=? AND FRAMENUMBER<=?";
			PreparedStatement ps;
			ps = connection.prepareStatement( sql );
			ps.setInt( 1 , id );
			ps.setInt( 2 , startFrame );
			ps.setInt( 3 , endFrame );

			ResultSet rs = ps.executeQuery( );

			while ( rs.next() )
			{
				int frame = rs.getInt( "frameNumber" );
				int mass_x = rs.getInt( "mass_x" );
				int mass_y = rs.getInt( "mass_y" );
				timeToPointHashMap.put( frame, new Point2D.Double( mass_x, mass_y ) );
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		System.out.println("Load done.");

	}

	public Point2D getPoint(int t) {

		return timeToPointHashMap.get( t );

	}

}
