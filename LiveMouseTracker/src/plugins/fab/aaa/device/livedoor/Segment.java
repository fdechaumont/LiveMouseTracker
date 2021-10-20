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

public class Segment {

	private Door door;
	private Zone zone1;
	private Zone zone2;

	public Segment(Zone zone1, Door door, Zone zone2 ) {
		this.door = door;
		this.zone1 = zone1;
		this.zone2 = zone2;
	}

	public boolean contains(Zone zone ) {
		return ( ( zone1 == zone ) || ( zone2 == zone ) );
	}

	public Zone otherZone(Zone zone ) {
		if ( zone1 == zone ) return zone2;
		if ( zone2 == zone ) return zone1;
		return null;
	}

	public Door getDoor() {
		return door;
	}

}
