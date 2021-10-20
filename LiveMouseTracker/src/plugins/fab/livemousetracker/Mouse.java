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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;

/** @deprecated */
public class Mouse {

	public double x,y;

	Color color = new Color ( Color.HSBtoRGB( (float)Math.random(), 0.8f, 0.5f ) );
	String name = "";

	Spine spine = null;

	public Mouse( double x, double y , String name) {

		this.x = x;
		this.y = y;
		this.name = name;
		spine = new Spine( x , y );
	}

	public void paint( Graphics2D g )
	{
		g.setColor( color );

		g.drawString( name , (int)x, (int)y);

		g.drawLine( (int)x-4 , (int)y-4 , (int)x+4, (int)y+4 );
		g.drawLine( (int)x-4 , (int)y+4 , (int)x+4, (int)y-4 );

		spine.paint( g );

	}

	public class Spine
	{
		ArrayList<Vertebra> vertebraList = new ArrayList<Vertebra>();

		public Spine( double x , double y ) {

			double width[] = new double[]{ 4 , 6 , 5 , 6 , 7 , 8 , 8 , 8 , 6 , 4 , 4 , 2 , 2 , 2 , 2 , 2 , 2 , 2 , 2 , 2 };
			for ( int i = 0 ; i<20 ; i++ )
			{
				// build from head to tail
				vertebraList.add( new Vertebra ( x , y + i * 3 , width[i] ) );
			}

		}

		void paint ( Graphics2D g )
		{
			// draw each vertebra
			for ( Vertebra vertebra : vertebraList )
			{
				Line2D line = new Line2D.Double(
						vertebra.x - Math.cos( vertebra.angle ) * vertebra.width /2d,
						vertebra.y - Math.sin( vertebra.angle ) * vertebra.width /2d ,
						vertebra.x + Math.cos( vertebra.angle ) * vertebra.width /2d ,
						vertebra.y + Math.sin( vertebra.angle ) * vertebra.width /2d );
				g.draw( line );
			}

			// draw spine
			Vertebra previousVertebra = null;
			for ( Vertebra vertebra : vertebraList )
			{
				if ( previousVertebra == null )
				{
					previousVertebra = vertebra;
					continue;
				}

				Line2D line = new Line2D.Double(
						previousVertebra.x ,
						previousVertebra.y ,
						vertebra.x ,
						vertebra.y
						);
				g.draw( line );

			}
		}

	}

	public class Vertebra
	{
		public double x;
		public double y;
		public double z;
		public double angle;
		public double width;

		public Vertebra( double x , double y , double width ) {
			this.x = x;
			this.y = y;
			z = 0;
			angle = 0;
			this.width = width;
		}

	}

}
