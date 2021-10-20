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
package plugins.fab.livemousetracker.histogram;

public class HistogramUtil {

	/**
	 * Return the sum of the abs(difference of the histograms)
	 * considering intensity.
	 */
	public static float getDistanceHistogramIntensity( Histogram hist1 , Histogram hist2 )
	{
		float histData1[] = hist1.getIntensityHistogram1D();
		float histData2[] = hist2.getIntensityHistogram1D();

		float sum =0;
		for ( int i=0 ; i< histData1.length ; i++ )
		{
			sum+= Math.abs( histData1[i] - histData2[i] );
		}

		return sum / (float) histData1.length;
	}

	public static float getDistanceHistogramIntensity(Histogram hist, float[] f) {

		float histData1[] = hist.getIntensityHistogram1D();
		float histData2[] = f;

		float sum =0;
		for ( int i=0 ; i< histData1.length ; i++ )
		{
			sum+= Math.abs( histData1[i] - histData2[i] );
		}

		return sum / (float) histData1.length;

	}

}
