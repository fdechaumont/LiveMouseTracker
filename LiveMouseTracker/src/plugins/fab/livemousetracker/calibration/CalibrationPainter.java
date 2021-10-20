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
package plugins.fab.livemousetracker.calibration;

import icy.canvas.IcyCanvas;
import icy.painter.Overlay;
import icy.sequence.Sequence;
import icy.type.point.Point5D.Double;
import plugins.kernel.roi.roi2d.ROI2DPolygon;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;

public class CalibrationPainter extends Overlay {

	enum Setup {
		Cage_50x50cm,
		Cage_50x50cm_WithPerspective,
		Cage_50x50cm_WithCorrection,
		Cage_InfraRed_2x3_70x60cm,
		Cage_InfraRed_2x2_70x40cm,
	}

	int setupNumber = 0;
	Setup setup = Setup.values()[0];

	public CalibrationPainter() {
		super("Calibration");
	}

	Font font = new Font( "Arial" , Font.BOLD , 10 );
	Font smallFont = new Font( "Arial" , Font.PLAIN , 6 );

	double[][] errorMap;

	public void setErrorMap(double[][] errorMap) {
			this.errorMap = errorMap;
	}

	@Override
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas) {

		g.setColor( Color.yellow );
		g.setFont( font );
		g.drawString( "Hit 'C' key to switch Calibration setup.", 20, 20 );
		g.drawString( "Current setup displayed: " + setup.toString() , 20, 30 );

		int midX = sequence.getWidth() /2 ;
		int midY = sequence.getHeight() /2 ;

		// vertical
		g.drawLine( midX, 0, midX, sequence.getHeight() );
		// horizontal
		g.drawLine( 0, midY , sequence.getWidth(), midY );

		switch ( setup )
		{
		case Cage_50x50cm:
			// quad 50x50cm
			g.drawString( "50x50cm ground cage limits.", 112, 65 );
			//g.drawRect( 112, 70, sequence.getWidth()-112*2, sequence.getHeight() -70*2 );
			g.drawRect( 114, 63, 398-114, 353-63 );

			//114, 63
			// 398, 353

			break;
		case Cage_50x50cm_WithPerspective:
			// quad 50x50cm (with perspective)
			g.drawString( "50x50cm ground cage limits (with perspective).", 112, 65 );

			Polygon p = new Polygon();
			p.addPoint( 256-122, 51 );
			p.addPoint( 256+122, 51 );
			p.addPoint( 256+142 , 314 );
			p.addPoint( 256-142 , 314 );
			g.draw( p );
			break;

		case Cage_50x50cm_WithCorrection:
			// quad 50x50cm
			g.drawString( "50x50cm ground cage limits (compensated perspective).", 112, 65 );
			g.drawRect( 112, 70, sequence.getWidth()-112*2, sequence.getHeight() -70*2 );

			break;

		case Cage_InfraRed_2x2_70x40cm:
			g.setColor( Color.YELLOW );
			g.drawString( "Lines represent top cage limits (at 35 cm from the ground).", 112, 65 );
			// horizontal
//			g.drawLine( 0, 97, 512, 97);
//			g.drawLine( 0, 321, 512, 321);
			// vertical
			g.drawLine( 512, 0, 512, 440 );

			g.setColor( Color.orange );
			g.drawString( "Cage 1", 60, 92+10 );
			g.drawRect( 55,92, 195,111);
			g.drawString( "Cage 2", 263, 92+10 );
			g.drawRect( 263,92, 195,111);

			g.drawString( "Cage 3", 60, 214+10 );
			g.drawRect( 55,214, 195,111);
			g.drawString( "Cage 4", 263, 214+10 );
			g.drawRect( 263,214, 195,111);
			break;

		case Cage_InfraRed_2x3_70x60cm:
			g.setColor( Color.YELLOW );
			g.drawString( "Lines represent top cage limits (at 35 cm from the ground).", 112, 65 );
			// horizontal
			g.drawLine( 0, 97, 512, 97);
			g.drawLine( 0, 321, 512, 321);
			// vertical
			g.drawLine( 512, 0, 512, 440 );

			g.setColor( Color.orange );
			g.drawString( "Cage 1", 60, 34+10 );
			g.drawRect( 55,34, 195,111);
			g.drawString( "Cage 2", 263, 34+10 );
			g.drawRect( 263,34, 195,111);

			g.drawString( "Cage 3", 60, 152+10 );
			g.drawRect( 55,152, 195,111);
			g.drawString( "Cage 4", 263, 152+10 );
			g.drawRect( 263,152, 195,111);

			g.drawString( "Cage 5", 60, 269+10 );
			g.drawRect( 55,269, 195,111);
			g.drawString( "Cage 6", 263, 269+10 );
			g.drawRect( 263,269, 195,111);

			break;
		}


		if ( errorMap != null )
		{
			g.setFont( smallFont );
			for ( int x = 0 ; x < 14 ; x++ )
				for ( int y = 0 ; y < 14 ; y++ )
			{
//					g.setColor( Color.orange );
				double error = errorMap[x][y]-620;

				g.setColor( Color.green );
				String text="";
				text+= ""+ (int)errorMap[x][y];

				if ( Math.abs( error ) > 20 )
				{
					if ( error< 0 )
					{
						g.setColor( Color.red );
//						text = "close";
					}else
					{
						g.setColor( Color.orange );
//						text = "far";
					}

				}

				g.fillOval( x*20 +126-3, y*20 +82-3, 5, 5 );
//				g.drawString( ""+(int)errorMap[x][y],
//						x*20 +120-3,
//						y*20 +95-3 );
				g.drawString( text,
						x*20 +123-3,
						y*20 +95-3 );
			}
		}

//		g.setColor( Color.orange );
//		g.drawString( "50x50cm wall cage limits (30cm high).", 112, 65 );
//		g.drawRect(
//				50,
//				25,
//				sequence.getWidth()-50*2,
//				sequence.getHeight() -25*2 );


	}

	@Override
	public void keyPressed(KeyEvent e, Double imagePoint, IcyCanvas canvas) {

		if ( e.getKeyChar()=='c' )
		{
			setupNumber++;
			if ( setupNumber >= Setup.values().length )
			{
				setupNumber = 0;
			}
			setup = Setup.values()[setupNumber];
		}

	}
}
