package plugins.fab.aaa.voc;

import java.awt.geom.Point2D;
import java.util.ArrayList;

/** Classify vocs */
public class VocalizationClassifier {

	public VocalizationClassifier() {

	}

	public void classify( Voc voc )
	{

		// find short duration.
		if ( voc.getDurationInMs() < 5 )
		{
			if ( voc.getFrequencyDynamicInHz( ) <= 6250 )
			{
				voc.addClassificationDescription("Short"); // (Duration="+voc.getDurationInMs()+"ms - range: " + voc.getFrequencyRangeInHz( ) + " Hz" );
			}
		}

		findMinorAndMajorAxis( voc );
		testVocContinuity( voc );
		detectJump( voc );

		if ( voc.getDurationInMs() >= 5 )
		{
			// upward

			if ( isUpward( voc ) )
			{
				voc.addClassificationDescription("Upward" );
			}

			// downward

			if ( isDownward( voc ) )
			{
				voc.addClassificationDescription("Downward" );
			}

			if ( voc.jumpList.size() > 0 )
			{
				voc.addClassificationDescription("Jump" );// + voc.jumpList.size() );
			}

			if ( isModulated( voc ) )
			{
				voc.addClassificationDescription("Modulated" );
			}

			if ( voc.containsHarmonics() )
			{
				voc.addClassificationDescription("Harmonics" );
			}

		}


	}

	private boolean isModulated(Voc voc) {
			double slope = ( voc.pB.getY() - voc.pA.getY() ) / ( voc.pB.getX() - voc.pA.getX() );
			//slope = -slope; // point A & B are reversed.
			double y = voc.pB.getY();
			int threshold = 3;
			double bestDistance = 0;
			int bestX = 0;
			int w = 0; // width of the side
			int side = 0; // side sign
			int sidePrevious = 0;
			int xMin = (int)Math.min( voc.pA.getX() , voc.pB.getX() );
			int xMax = (int)Math.max( voc.pA.getX() , voc.pB.getX() );

			for ( int x = xMin ; x <= xMax ; x++ )
			{

				Point p = voc.getPointAt( x );
				if ( p != null )
				{
					w++;
					double dif = y - p.y;

					boolean ok = false;
					if ( dif > threshold )
					{
						side = 1;
						ok=true;
					}

					if ( dif < -threshold )
					{
						side = -1;
						ok = true;
					}
					if ( ok )
					{
						if ( side == sidePrevious )
						{
							if ( Math.abs( dif ) > bestDistance )
							{
								bestX = x;
								bestDistance = Math.abs( dif );
							}
						}
						else
						{
							if ( Math.abs( bestDistance ) > threshold )
							{								
								voc.nbModulation++;
								voc.modulationList.add( bestX );								
							}
							bestDistance = 0;
							w=0;
						}
					}
					sidePrevious=side;
				}

				y+=slope;

			}
			// finish
			if ( Math.abs( bestDistance ) > threshold )
			{
					voc.modulationList.add( bestX );
			}
		if ( voc.nbModulation > 0 )
		{
			return true;
		}
		return false;

	}

	private boolean sameSign(float a, float b) {

		if ( a == 0 ) return true;
		if ( b == 0 ) return true;

		if ( a < 0 && b < 0 ) return true;
		if ( a > 0 && b > 0 ) return true;

		return false;
	}

	public boolean isUpward( Voc voc ) {

		if ( voc.getFrequencyDynamicInHz() > 6500 )
		{
			float freqStart = voc.getStartFrequencyInHz();
			float freqEnd = voc.getEndFrequencyInHz();
			if ( freqStart < freqEnd )
			{
				if ( voc.linearityIndex < 2 )
				{
					return true;
				}
			}

		}

		return false;
	}


	private void testVocContinuity(Voc voc) {

		double sumDistance = 0;

		for ( Point p : voc.pointList )
		{
			sumDistance+= distanceToLine( p.x, p.y , voc.pA, voc.pB );
		}
		if ( voc.pointList.size() > 0 )
		{
			voc.linearityIndex = sumDistance / voc.pointList.size();
			return;
		}
		voc.linearityIndex = -1;

	}

	public boolean isDownward( Voc voc ) {

		if ( voc.getFrequencyDynamicInHz() > 6500 )
		{

			float freqStart = voc.getStartFrequencyInHz();
			float freqEnd = voc.getEndFrequencyInHz();
			if ( freqStart > freqEnd )
			{
				if ( voc.linearityIndex < 2 )
				{
					return true;
				}
			}

		}

		return false;
	}



	double distanceToLine( double x, double y, Point2D p1, Point2D p2 ) {

		double x1 = p1.getX();
		double y1 = p1.getY();
		double x2 = p2.getX();
		double y2 = p2.getY();

		double A = x - x1;
		double B = y - y1;
		double C = x2 - x1;
		double D = y2 - y1;

		double dot = A * C + B * D;
		double len_sq = C * C + D * D;
		double param = -1;
		if (len_sq != 0) //in case of 0 length line
			param = dot / len_sq;

		double xx, yy;

		if (param < 0) {
			xx = x1;
			yy = y1;
		}
		else if (param > 1) {
			xx = x2;
			yy = y2;
		}
		else {
			xx = x1 + param * C;
			yy = y1 + param * D;
		}

		double dx = x - xx;
		double dy = y - yy;
		return Math.sqrt(dx * dx + dy * dy);
	}

	private void findMinorAndMajorAxis( Voc voc ) {

		double angle;
		double longAxis;
		double shorterAxis;
		Point2D pA;
		Point2D pB;

		Moment moment = new Moment( voc.pointList );

		angle = moment.aoipar.theta;
		//longAxis = moment.aoipar.longAxis;
		longAxis = voc.pointList.size()*Math.sqrt(2);
		shorterAxis = moment.aoipar.shorterAxis;

		voc.pA = new Point2D.Double( voc.getCenterX() + Math.cos( angle ) * longAxis /2d ,
				voc.getMeanY() + Math.sin( angle ) * longAxis /2d
				);
		voc.pB = new Point2D.Double( voc.getCenterX() - Math.cos( angle ) * longAxis / 2d ,
				voc.getMeanY() - Math.sin( angle ) * longAxis / 2d
				);


	}

	private void detectJump( Voc voc )
	{

		//for ( Voc voc : new ArrayList<>( vocList) )
		{
			float frequencyDynamic = voc.getFrequencyDynamicInHz();
			if ( frequencyDynamic < 10000 )
			{
				return;
			}
			for ( int i = 0 ; i < voc.pointList.size() -1 ; i++ )
			{
				Point a = voc.pointList.get( i );
				Point b = voc.pointList.get( i+1 );

				float shiftHz = Math.abs( a.y-b.y ) * voc.yFrequencyInHz;

				if ( shiftHz > frequencyDynamic / 3 )
				{
					voc.jumpList.add( voc.pointList.get( i ).x );
				}
			}
		}
	}

}
