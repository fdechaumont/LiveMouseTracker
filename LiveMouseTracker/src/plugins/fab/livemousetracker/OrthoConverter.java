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

import java.awt.geom.Point2D;

import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;
import icy.type.DataType;

/** convert a perspective image to an orthoImage */
public class OrthoConverter {

	IcyBufferedImage infraOrtho;
	IcyBufferedImage depthOrtho;
	Point2D center = new Point2D.Double( 512/2, 424/2 );

	Sequence infraOrthoSequence = null;
	Sequence depthOrthoSequence = null;

	/**
	 * Ortho Converter : Problème: la cage reflete et les murs ne sont pas mesurés correctement
	 * > problème de mesure de Z pour compenser l'ortho
	 */
	public OrthoConverter() {

		infraOrtho = new IcyBufferedImage( 512, 424, 1, DataType.FLOAT );
		depthOrtho = new IcyBufferedImage( 512, 424, 1, DataType.FLOAT );

		infraOrthoSequence = new Sequence("InfraOrtho", infraOrtho );
		depthOrthoSequence = new Sequence("DepthOrtho", depthOrtho );


	}

	public void convert( IcyBufferedImage infraImage , IcyBufferedImage depthImage )
	{
		float dataInfraOrtho[] = infraOrtho.getDataXYAsFloat( 0 );
		float dataDepthOrtho[] = depthOrtho.getDataXYAsFloat( 0 );

		for ( int i = 0 ; i < dataInfraOrtho.length ; i++ )
		{
			// reset data
			dataInfraOrtho[i] = 0;
			dataDepthOrtho[i] = 0;
		}

		// depth to consider: 933 to 990

		float minDepth = 930;
		float maxDepth = 990;

		short[] depthSourceData = depthImage.getDataXYAsShort( 0 );
		short[] infraSourceData = infraImage.getDataXYAsShort( 0 );

		int x = 0;
		int y = 0;
		for ( int i = 0 ; i < depthSourceData.length ; i++ )
		{
			float value = depthSourceData[i] ;
			//if ( ( value > minDepth ) && ( value < maxDepth ) )
			{
				// compute vector to center point
				float vx = (float) (x-center.getX() );
				float vy = (float) (y-center.getY() );
				//float factor = - ( value - maxDepth ) * 1.01f / 70f;
				//float factor = 0.9f;
				//float factor = -(value-maxDepth);
				//factor/=30f;

				// for maxDepth factor is 1;
//				float factor = 1f - ( - ( value - maxDepth ) ) * 0.02f;
				float factor = - ( value - maxDepth ) ;

				vx*=factor;
				vy*=factor;

				int newX = (int) (center.getX() + vx);
				int newY = (int) (center.getY() + vy);

				dataDepthOrtho[x + y * 512]
						=  factor ; //value;

				if ( newX > 0 && newX < 512 && newY > 0 && newY < 424 )
				{
					dataInfraOrtho[newX + newY * 512]
							= infraSourceData[i];
				//	dataDepthOrtho[x + y * 512]
				//			=  value; // x+y; // factor ; //value;
				}
			}

			x++;
			if ( x >= 512 )
			{
				x=0;
				y++;
			}
		}


		infraOrtho.dataChanged();
		depthOrtho.dataChanged();

	}

	public Sequence getDepthOrthoSequence() {
		return depthOrthoSequence;
	}

	public Sequence getInfraOrthoSequence() {
		return infraOrthoSequence;
	}

}
