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
package plugins.fab.livemousetracker;

import java.util.ArrayList;

/**
 * Get a patch of 21x21 pixels. Find the angle of the best symetrical axis and provide the score.
 * */
public class SymetryAngleFinder {

	class PixCouple
	{
		// p1 is symetrical to p2
		int x1;
		int y1;
		int x2;
		int y2;

		public PixCouple( int x1 , int y1 , int x2 , int y2 ) {
			this.x1 = x1;
			this.y1 = y1;
			this.x2 = x2;
			this.y2 = y2;
		}
	}

	class AngleSymetryScorer
	{
		ArrayList<PixCouple> pixCoupleList = new ArrayList<PixCouple>();
		double angle;

		public AngleSymetryScorer( double angle ) {
			this.angle = angle;

			// pre computes pixel symetry.

			double nbStep = 20;

			for ( double r=1 ; r<=10; r++)
				for ( double aOffset=0 ; aOffset<=Math.PI; aOffset+= Math.PI / nbStep )
			{
				double x1 = Math.cos( angle - aOffset ) * r;
				double y1 = Math.sin( angle - aOffset ) * r;
				double x2 = Math.cos( angle + aOffset ) * r;
				double y2 = Math.sin( angle + aOffset ) * r;
				// center
				x1+=10;
				y1+=10;
				x2+=10;
				y2+=10;
				// clip
				if ( x1 < 0 ) continue;
				if ( y1 < 0 ) continue;
				if ( x2 < 0 ) continue;
				if ( y2 < 0 ) continue;

				if ( x1 > 20 ) continue;
				if ( y1 > 20 ) continue;

				if ( x2 > 20 ) continue;
				if ( y2 > 20 ) continue;

				addCouple( new PixCouple( (int)x1, (int)y1 , (int)x2, (int)y2 ) );
			}

		}
		public void addCouple( PixCouple pcCandidate )
		{
			// check if exists.
			for ( PixCouple px : pixCoupleList )
			{
				if ( px.x1 == pcCandidate.x1 && px.y1 == pcCandidate.y1 )
				{
					return;
				}
			}
			pixCoupleList.add( pcCandidate );
		}

		@Override
		public String toString() {
			return "[Symetry Scorer] Angle: "+angle +" nbPixCouple: " + pixCoupleList.size();
		}

		/** score the buffer
		 * @param mask */
		public int score( short[] buffer, boolean[] mask )
		{
			int score = 0;

			for ( PixCouple pixCouple: pixCoupleList )
			{
				int p1Offset = pixCouple.x1 + pixCouple.y1 * 21;
				int p2Offset = pixCouple.x2 + pixCouple.y2 * 21;

				if ( mask[p1Offset] && mask[p2Offset] )
				{
					short p1 = buffer[ p1Offset ];
					short p2 = buffer[ p2Offset ];
					score += Math.abs( p1-p2 );
				}else
				{
					score +=50;
				}
/*
				if ( !(mask[p1Offset] || mask[p2Offset]) )
				{
					score+=5000; // FIXME CONST > penalité à ne pas équilibré dans le masque
					// could be the max dif between max and min ?
				}

					if ( ( mask[p1Offset] || mask[p2Offset] ) == false )
					{
						score+=5000; // FIXME CONST > penalité à ne pas avoir de pixel dans le masque
						// could be the max dif between max and min ?
					}
	*/
			}

			return score;
		}

	}

	ArrayList<AngleSymetryScorer> symetryScorerList = new ArrayList<SymetryAngleFinder.AngleSymetryScorer>();

	public SymetryAngleFinder() {

		double nbStep = 20; // set precision
		for ( double angle=0 ; angle<=Math.PI; angle+= Math.PI / nbStep )
		{
			AngleSymetryScorer symetryScorer = new AngleSymetryScorer( angle );
			//System.out.println( symetryScorer );
			symetryScorerList.add( symetryScorer );
		}

	}

	public class Score
	{
		public double angle;
		public int score;

		public Score( double angle , int score ) {
			this.angle = angle;
			this.score = score;
		}

		@Override
		public String toString() {

			return "Symetry Score: " + score + " for angle " + angle;
		}
	}

	/** Best score is the minimum */
	public Score score( short[] buffer , boolean mask[] )
	{
		int bestScore = Integer.MAX_VALUE;
		double bestAngle = 0;

		for ( AngleSymetryScorer angleSysmetryScorer : symetryScorerList )
		{
			int score = angleSysmetryScorer.score( buffer , mask );
			if ( score < bestScore )
			{
				bestScore = score;
				bestAngle = angleSysmetryScorer.angle;
			}
		}

		return new Score( bestAngle , bestScore );

	}


}
