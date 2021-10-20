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
package plugins.fab.livemousetracker.rfid;

import java.awt.geom.Point2D;

public class AntennaReadEvent {

	String rfid;
	/** This RFID event has been recorded/replayed at T*/
	int measuredAtT;
	/** latency of the measure (for a 200ms RFID reading, the delay is 6frame *33ms, latency=6 */
	int latency;
	float ray;
	Point2D location;
	Antenna antenna;

	public AntennaReadEvent(int tEvent, int latency, String rfid, Point2D point, float ray, Antenna antenna ) {
		this.rfid = rfid;
		this.location = point;
		this.ray = ray;
		this.measuredAtT = tEvent;
		this.latency = latency;
		this.antenna = antenna;
	}

	public String getRFID()
	{
		return rfid;
	}

	public Point2D getLocation() {
		return location;
	}

	public float getRay() {
		return ray;
	}

	public int getMeasuredT()
	{
		return measuredAtT;
	}

	public int getCorrectedT()
	{
		return measuredAtT - latency;
	}

	@Override
	public String toString() {
		return "[RFIDEvent] id:"+rfid + " " + location + " t:" +measuredAtT + " corrected T: " + getCorrectedT();
	}

	public Antenna getAntenna() {
		return antenna;
	}

}
