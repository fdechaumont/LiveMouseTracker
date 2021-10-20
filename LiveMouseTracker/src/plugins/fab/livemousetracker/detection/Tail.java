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

import icy.roi.BooleanMask2D;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import plugins.fab.livemousetracker.Util;
import plugins.fab.livemousetracker.morpho.BooleanMaskUtil;
import plugins.fab.livemousetracker.morpho.Result;

public class Tail {

	ArrayList<Point2D> pointList;
	double angle;
	static final float SEGMENT_SIZE = 4f;

	public ArrayList<Point2D> getPointList() {
		return pointList;
	}

	public Tail( Point2D startPoint , Point2D vector ) {

		angle = Math.atan2( vector.getY() , vector.getX() );
		vector = Util.normVector( vector , SEGMENT_SIZE );

		pointList = new ArrayList<Point2D>();
		for ( int i = 0 ; i < 12 ; i ++ )
		{
			pointList.add( new Point2D.Double( startPoint.getX() + i*vector.getX(),
					startPoint.getY() + i*vector.getY() ) );
		}

	}

	public Tail(ArrayList<Point2D> pointList2 , double angle ) {
		this.pointList = pointList2;
		this.angle = angle;
	}

	public void paint( Graphics2D g )
	{
		GeneralPath path = new GeneralPath();
		for ( Point2D point : pointList )
		{
			if ( path.getCurrentPoint() == null )
			{
				path.moveTo( point.getX(), point.getY() );
			}else
			{
				path.lineTo( point.getX(), point.getY() );
			}
		}
		g.draw( path );
	}

	public Tail getCopy() {

		return new Tail( new ArrayList<Point2D>( pointList ) , angle );

	}

	/**
	 *
	 * @param tailPoint
	 * @param vector is the current massCenter to TailVector.
	 * This parameter is used to find the angle shift between 2 mouse positions.
	 */
	public void shift(Point2D tailPoint , Point2D vector ) {

		Point2D startPoint = pointList.get( 0 );
		double newAngle = Math.atan2( vector.getY() , vector.getX() );

		AffineTransform transform = new AffineTransform();

		double difAngle = newAngle-angle;

		System.out.println( "DifAngle: " + difAngle );
		transform.rotate( difAngle , startPoint.getX() , startPoint.getY() );
		transform.translate( -startPoint.getX() , -startPoint.getY() );
		transform.translate( tailPoint.getX() , tailPoint.getY() );

		for ( Point2D p : pointList )
		{
			transform.transform( p , p );
		}

		System.out.println( "p:" + pointList.get( 3).getX() +" , " + pointList.get( 3 ).getY() );

		angle= newAngle;

	}

	/** Try to fit the tail to the given mask
	 * The method process point per point and rotates the remaining point.
	 * This is to mimic the dif information that we get.
	 * */
	public void fitToMask(BooleanMask2D tailMask) {

		if ( tailMask == null ) return;


		for ( int i = 2; i < pointList.size() ; i++ )
			//int i = 3;
		{
			// the goal is to locate p2 considering the pivot point p1.
			Point2D p0 = pointList.get( i-2 );
			Point2D p1 = pointList.get( i-1 );
//			Point2D p2 = pointList.get( i );

			// check if the min distance to the mask is less than SEGMENT
			Point2D nextPoint = null;
			Result result = BooleanMaskUtil.getMinDistanceToMass( p1 , tailMask );
			if ( result.distance < SEGMENT_SIZE )
			{
				// The point is accessible. We can now try to get it.
				// We ask a point at SEGMENT_SIZE distance from p1 and far from p0 (to search a candidate in the proper direction)
				Result r = BooleanMaskUtil.getConstrainedPoint( p0 , p1 , SEGMENT_SIZE , tailMask );

				if ( r.point != null ) // if a point is found with the constraints
				{
					nextPoint = r.point;

//					Point2D p2 = pointList.get( i );
//					//pointList.set( i , r.point ); // we set it
//
//					double angle = computeAngle( Util.createVector( p1 , p2) , Util.createVector( p1, r.point ) );
//					AffineTransform t = new AffineTransform();
//					t.rotate( angle , p1.getX() , p1.getY() );
//
//					for ( int j = i ; j < pointList.size() ; j++ )
//					{
//						Point2D pToTransform = pointList.get( j );
//						t.transform( pToTransform , pToTransform );
//					}

				}
			}else
			{
				Point2D vector = Util.normVector( Util.createVector( p1 , result.point ) , SEGMENT_SIZE );
				Point2D pNext = new Point2D.Double( p1.getX() + vector.getX(), p1.getY() + vector.getY() );
//				pointList.set( i , pNext ); // we set it
				nextPoint = pNext;
			}

			if ( nextPoint != null )
			{
				Point2D p2 = pointList.get( i );
				//pointList.set( i , r.point ); // we set it

				double angle = computeAngle( Util.createVector( p1 , p2) , Util.createVector( p1, nextPoint ) );
				AffineTransform t = new AffineTransform();
				t.rotate( angle , p1.getX() , p1.getY() );

				for ( int j = i ; j < pointList.size() ; j++ )
				{
					Point2D pToTransform = pointList.get( j );
					t.transform( pToTransform , pToTransform );
				}
			}



		}


	}

	/** Compute the angle created by two vectors */
	private double computeAngle( Point2D vA , Point2D vB ) {

		double Na = Math.sqrt( vA.getX() * vA.getX() + vA.getY() * vA.getY() );
		double Nb = Math.sqrt( vB.getX() * vB.getX() + vB.getY() * vB.getY() );
		double c = ( vA.getX() * vB.getX() + vA.getY() * vB.getY() ) / ( Na * Nb );
		double s = ( vA.getX() * vB.getY() - vA.getY() * vB.getX() );
		return Math.sin( s ) * Math.acos( c );
				/*
		Na = sqrt(Xa*Xa+Ya*Ya);
		Nb = sqrt(Xb*Xb+Yb*Yb);
		C = (Xa*Xb+Ya*Yb)/(Na*Nb);
		S = (Xa*Yb-Ya*Xb);
		BÂC = sign(S)*acos(C);
		*/

	}

}
