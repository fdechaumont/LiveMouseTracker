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
package plugins.fab.livemousetracker.detection;

import java.util.ArrayList;

public class Spine {

	public static int NB_SPINE_POINT = 30;
	public double z[] = new double[NB_SPINE_POINT];

	public ArrayList<SpineSpecialPoint> spineSpecialPointList = new ArrayList<SpineSpecialPoint>();

	/** Apparent length of the animal (in millimeter) */
	public double lengthInMM;


}
