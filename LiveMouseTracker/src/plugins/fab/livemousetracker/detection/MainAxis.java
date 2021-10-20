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

import java.awt.geom.Point2D;

/**
 * The main axis of the animal consist in the definition of the points along the main axis
 * after fitting with an ellipse.
 *
 * The duty of this structure is just to record those point, and then track pA and pB,
 * dis regarding whether they are head or tail point.
 * */
public class MainAxis {

	public Point2D pA;
	public Point2D pB;

	/** swap the 2 points. */
	public void swap() {

		Point2D p = pA;
		pA = pB;
		pB = p;
	}

	public MainAxis getCopy() {
		MainAxis copy = new MainAxis();
		copy.pA = (Point2D) pA.clone();
		copy.pB = (Point2D) pB.clone();
		return copy;
	}

}
