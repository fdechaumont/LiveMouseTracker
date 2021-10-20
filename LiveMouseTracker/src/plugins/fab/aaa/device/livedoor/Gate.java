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

import icy.roi.ROI;

/**
 *@deprecated
 */
public class Gate {

	private ROI room;


	ArrayList<Door> doorList = new ArrayList<Door>();

	Door targetDoor ;
	Door sourceDoor;

	GateState state;

	/**
	 * Here is the doc
	 *
	 * @param room
	 * @param mainROI
	 * @param corridorWest
	 * @param corridorEast
	 */
	public Gate( ROI room ) {
		this.room = room;
	}

	public void addDoor( Door door )
	{
		doorList.add( door );
	}

	public void process()
	{
		if ( targetDoor == null || sourceDoor == null )
		{
			return;
		}




		//if ( state == )

	}

	public int getRFIDIdentity()
	{
		// FIXME
		return 0;
	}

	public int getNumberOfAnimalIn( GatePart gatePart )
	{
/*
		switch ( gatePart ) {
		case ROOM:
			return getNumberOfAnimalInROI( room );
		case CORRIDOR_EAST:
			return getNumberOfAnimalInROI( corridorEast );
		case CORRIDOR_WEST:
			return getNumberOfAnimalInROI( corridorWest );
		case ALL_AREA:
			return getNumberOfAnimalInROI( mainROI );
		default:
			return 0;
		}*/
		// FIXME
		return 0;
	}


	private int getNumberOfAnimalInROI(ROI roi) {

		// TODO

		return 0;
	}

	/**
	 * Where the mouse should go when it enters the room
	 */
	public void setDirection( Door sourceDoor , Door targetDoor )
	{
		this.sourceDoor = sourceDoor;
		this.targetDoor = targetDoor;

		for ( Door door : doorList )
		{
			if ( door!= sourceDoor )
			{
				door.close();
			}
		}

		sourceDoor.open();

	}


}
