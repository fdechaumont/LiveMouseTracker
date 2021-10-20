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
package plugins.fab.livemousetracker.postprocessdatabase;

import icy.plugin.abstract_.PluginActionable;
import icy.plugin.interface_.PluginThreaded;
import icy.sequence.Sequence;

public class UnfoldAnalysis extends PluginActionable implements PluginThreaded {

	@Override
	public void run() {

		Sequence sequence = getActiveSequence();

		System.out.println("Sequence: " + sequence );

		double result[][] = new double[4][360];

		for ( int c = 0 ; c < sequence.getSizeC() ; c++ )
		{
			//System.out.println( "********** id: " + (int)(c+1) );
			for ( int angle = 0 ; angle < 360 ; angle ++ )
			{
				double value = 0;
				for ( float ray = 0 ; ray < 100 ; ray ++ )
				{
					double x = 500 + Math.cos( Math.toRadians( angle ) ) * ray;
					double y = 500 + Math.sin( Math.toRadians( angle ) ) * ray;

					value+= sequence.getData( 0 , 0, c, (int)y, (int)x);
//					sequence.getImage( 0, 0 ).setData( (int)x, (int)y, c, 0 );
				}
				//System.out.println( value );
				result[c][angle] = value;
			}
		}

		// print

		for ( int c = 0 ; c < sequence.getSizeC() ; c++ )
		{
			System.out.print("ID"+(c+1)+"\t");
		}

		System.out.println("");
		for ( int angle = 0 ; angle < 360 ; angle ++ )
		{
			for ( int c = 0 ; c < sequence.getSizeC() ; c++ )
			{
				System.out.print( result[c][angle] + "\t");
			}
			System.out.println("");
		}




	}



}
