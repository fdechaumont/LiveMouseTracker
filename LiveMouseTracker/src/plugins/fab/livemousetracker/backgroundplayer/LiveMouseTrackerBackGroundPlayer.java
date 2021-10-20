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
package plugins.fab.livemousetracker.backgroundplayer;

import icy.gui.frame.progress.AnnounceFrame;
import icy.image.IcyBufferedImage;
import icy.plugin.abstract_.PluginActionable;
import icy.plugin.interface_.PluginBundled;
import icy.sequence.Sequence;
import icy.type.DataType;
import plugins.fab.livemousetracker.LiveMouseTracker;

/**
 * Designed to process images of background recorded in png.
 * @author Fab
 *
 */
public class LiveMouseTrackerBackGroundPlayer extends PluginActionable {

	@Override
	public void run() {

		Sequence sequence = getActiveSequence();

		if ( sequence == null )
		{
			new AnnounceFrame("Load a background set of image before launching the plugin.");
			return;
		}

		Sequence outSequence = new Sequence();
		int t = 0;
		for ( IcyBufferedImage image : sequence.getAllImage() )
		{
			IcyBufferedImage outImage = new IcyBufferedImage( image.getWidth(), image.getHeight(), 1, DataType.FLOAT );
			short[] data = image.getDataXYAsShort( 0 );
			float[] out = outImage.getDataXYAsFloat( 0 );

			for ( int i = 0 ; i<data.length ; i++ )
			{
				float v = data[i];
				v-=600;
				v=-v+100;
				v = Math.max( v , 0 );
				out[i] = v;
			}
			outSequence.addImage( t ,  outImage );
			t++;
		}
		addSequence( outSequence );

	}

}
