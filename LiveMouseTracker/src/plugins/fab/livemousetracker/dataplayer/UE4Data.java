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
package plugins.fab.livemousetracker.dataplayer;

import java.awt.geom.Point2D;
import java.util.ArrayList;

import plugins.fab.livemousetracker.Util;

class UE4Data
{
	float x,y,z,angle;

	public UE4Data( MouseDetectionX det ) {

		x = (float) ( det.mouseDetection.getMassCenter().getX() - 114 );
		x =  x * ( 50f/ 284f );

		y = (float) ( det.mouseDetection.getMassCenter().getY() - 63 );
		y =  y * ( 50f/ 290f );

		angle = 0;
		try
		{
			Point2D vect = Util.createVector( det.mouseDetection.getMassCenter().toPoint2D(),
					det.mouseDetection.getFrontPoint().toPoint2D() );

			angle =
					(float)
					Math.toDegrees(
							Math.atan2( vect.getY(), vect.getX() ) );
		} catch( NullPointerException e)
		{
		}

		float swap = x;
		x = y;
		y = swap;

		x = 50 -x;
		angle=angle - 90+180;

		x = x * ( 470f/50f ) - 235f;
		y = y * ( 470f/50f ) - 235f;
		z = 830;
	}

	public ArrayList<Float> getAsList() {
		ArrayList<Float> list = new ArrayList<Float>();
		list.add( x );
		list.add( y );
		list.add( z );
		list.add( angle );
		return list;
	}
}