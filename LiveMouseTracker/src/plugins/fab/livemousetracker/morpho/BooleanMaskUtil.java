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
package plugins.fab.livemousetracker.morpho;

import java.awt.Point;
import java.awt.geom.Point2D;

import icy.roi.BooleanMask2D;

public class BooleanMaskUtil {

	public static double getCompactness( BooleanMask2D booleanMask )
	{
		double perimeter = booleanMask.getContourPoints().length;
		double area = booleanMask.getNumberOfPoints();
		return area / ( Math.pow( perimeter , 2 ) );
	}

	public static double getCircularity( BooleanMask2D booleanMask )
	{
		return Math.PI * 4d * getCompactness( booleanMask );
	}

	public static double getMinDistanceToContour( BooleanMask2D a , BooleanMask2D b )
	{
		double minDistSq = Double.MAX_VALUE;
		Point paBest = null;
		Point pbBest = null;

		Point[] contourPointB = b.getContourPoints();
		for ( Point pa : a.getContourPoints() )
		{
			for ( Point pb : contourPointB )
			{
				double distanceSq = pa.distanceSq( pb );
				if ( distanceSq < minDistSq )
				{
					minDistSq = distanceSq;
					paBest = pa;
					pbBest = pb;
				}
			}
		}

		double distance = Double.MAX_VALUE;
		if ( paBest != null )
		{
			distance = paBest.distance( pbBest );
		}

		return distance;
	}

	public static double getMinDistanceToContour(Point2D point, BooleanMask2D mask) {

		double minDistSq = Double.MAX_VALUE;
		Point pBest = null;

		for ( Point p : mask.getContourPoints() )
		{
			double distanceSq = p.distanceSq( point );
			if ( distanceSq < minDistSq )
			{
				minDistSq = distanceSq;
				pBest = p;
			}
		}


		double distance = Double.MAX_VALUE;
		if ( pBest != null )
		{
			distance = pBest.distance( point );
		}

		return distance;

	}

	public static Result getMinDistanceToMass(Point2D point, BooleanMask2D mask ) {

		double minDistSq = Double.MAX_VALUE;
		Point pBest = null;

		for ( Point p : mask.getPoints() )
		{
			double distanceSq = p.distanceSq( point );
			if ( distanceSq < minDistSq )
			{
				minDistSq = distanceSq;
				pBest = p;
			}
		}

		double distance = Double.MAX_VALUE;
		if ( pBest != null )
		{
			distance = pBest.distance( point );
		}

		return new Result( pBest , distance );

	}

	/**
	 * Provide a point that is at a distance dist-1<d<dist+1 of point.
	 * AND where point is closer to the candidate point than p0.
	 * In the tail detection process,
	 * this means that the point found is in the proper direction, then we don't propagate backward.
	 *
	 * @param p0
	 * @param point
	 * @param segmentSize
	 * @param tailMask
	 * @return
	 */
	public static Result getConstrainedPoint(Point2D p0, Point2D point,
			float dist, BooleanMask2D mask) {

		double minDist = Double.MAX_VALUE;
		Point pBest = null;

		for ( Point p : mask.getPoints() )
		{
			double distance = p.distance( point );
			double distanceToP0 = p.distance( p0 );
			if ( distance < minDist
					&& distance > dist -1f
					&& distance < dist +1f
					&& distance < distanceToP0 )
			{
				minDist = distance;
				pBest = p;
			}
		}

		double distance = Double.MAX_VALUE;
		if ( pBest != null )
		{
			distance = pBest.distance( point );
		}

		return new Result( pBest , distance );


	}

}
