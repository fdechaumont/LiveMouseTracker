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
package plugins.fab.livemousetracker.track;

import java.awt.geom.Point2D;
import java.util.ArrayList;

import plugins.fab.livemousetracker.detection.MouseDetection;

public abstract class AbstractTrackPool {

	abstract public void clear();

	abstract public TrackSegment getClosestTrack( Point2D point );

	abstract public ArrayList<TrackSegment> getTrackSegments();

	abstract public TrackSegment addDetection( MouseDetection mouseDetection  );

	abstract public void removeTrack( TrackSegment ts );

	abstract public TrackSegment getTrackWithId(int dataBaseId);

}
