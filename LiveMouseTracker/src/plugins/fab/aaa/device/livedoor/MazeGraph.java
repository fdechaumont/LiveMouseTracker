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
package plugins.fab.aaa.device.livedoor;

import java.util.ArrayList;

/**
 * Graph that describes the setup
 *
 */
public class MazeGraph {

	ArrayList<Zone> zoneList = new ArrayList<Zone>();
	ArrayList<Segment> segmentList = new ArrayList<Segment>();

	/** If the zone is booked by a PathManager, this will be set to true
	 * TODO: maybe set which instance of PathManager is actually booking.
	 * */
	boolean booked = false;

	public void addZone(Zone zone ) {
		zoneList.add( zone );

	}

	public void addSegment(Segment segment) {
		segmentList.add( segment );

	}

	public Door getDoor(Zone zone1, Zone zone2) {

		for ( Segment segment : segmentList )
		{
			if ( segment.contains( zone1 ) && segment.contains( zone2 ) )
			{
				return segment.getDoor();
			}
		}

		return null;
	}




}
