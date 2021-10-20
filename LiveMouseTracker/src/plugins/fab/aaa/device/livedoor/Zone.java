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

import plugins.fab.livemousetracker.detection.MouseDetection;
import icy.roi.ROI2D;

public class Zone {

	ROI2D roi = null;

	public Zone( ROI2D roi ) {
		this.roi = roi;
	}

	@Override
	public String toString() {
		return "[Zone] " + roi.getName();
	}

	public boolean containsDetection(ArrayList<MouseDetection> detectionList) {

		for ( MouseDetection detection : detectionList )
		{
			if ( roi.contains( detection.getMassCenter().toPoint2D() ) ) return true;
		}

		return false;
	}


}
