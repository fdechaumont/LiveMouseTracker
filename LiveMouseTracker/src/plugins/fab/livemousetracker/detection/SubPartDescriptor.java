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

import plugins.fab.livemousetracker.LiveMouseTracker;

public class SubPartDescriptor {

	public static final int NUMBER_OF_ATTRIBUTES = 0+LiveMouseTracker.NB_SIGNATURE_HISTO_BIN*2;

//	remove for a while:
//	public double surface;
//	public double contour;
//	public double meanDepth;

//	public double meanIntensityArea;
//	public double maxInfraIntensity;
//	public double volume;

	public double[] infraHisto;
	public double[] depthHisto;

}
