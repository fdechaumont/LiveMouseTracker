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

import plugins.fab.kinectdriver.KinectData;
import plugins.fab.kinectdriver.KinectEvent;
import plugins.fab.kinectdriver.KinectListener;
import plugins.fab.kinectdriver.KinectStreamer;
import icy.image.IcyBufferedImage;
import icy.plugin.abstract_.PluginActionable;
import icy.sequence.Sequence;
import icy.type.DataType;

public class Live3DTest extends PluginActionable implements KinectListener {

	KinectStreamer kinectStreamer = null;
	Sequence depthSequence = null;
	Sequence depthCalibratedSequence = new Sequence();
	IcyBufferedImage calibImage = new IcyBufferedImage( 512, 424, 1, DataType.FLOAT );



	@Override
	public void run() {

		kinectStreamer = new KinectStreamer( true );
		kinectStreamer.addKinectListener( this );
		depthCalibratedSequence.setImage( 0 , 0 , calibImage );
		addSequence( depthCalibratedSequence );
	}

	@Override
	public void kinectChange(Sequence sourceSequence, KinectData kinectData, KinectEvent kinectEvent) {

		if ( kinectEvent == KinectEvent.NEW_DEPTH_SEQUENCE )
		{
			depthSequence = sourceSequence;
			addSequence( depthSequence );
		}

		if ( kinectEvent == KinectEvent.NEW_DEPTH_CAPTURE )
		{
			//System.out.println( kinectData.xyz.length );
			//System.out.println( kinectData.uv.length );


			float [] calibBuffer = calibImage.getDataXYAsFloat( 0 );

			for ( int i = 0 ; i < calibBuffer.length ; i++ )
			{
				calibBuffer[i] = 0;
			}

			int indexUVBuffer = 0;
			for ( int i = 0 ; i < kinectData.xyz.length ; i+=3 )
			{
				float x = kinectData.xyz[i] * 1024f +512f;
				float y = -kinectData.xyz[i+1] * 424f*2f + 212f*2f;
				float z = kinectData.xyz[i+2];

				float u = kinectData.uv[indexUVBuffer];
				float v = kinectData.uv[indexUVBuffer+1];


				//calibBuffer[indexCalibBuffer] = z;

				//System.out.print( "[" + x + "," + y +"," + z + "]" );

				if ( (x >= 0) && (x <512) && (y>= 0) && (y < 424 ) )
				{
					calibBuffer[(int)x+(int)y*512] = z;
				}
				indexUVBuffer++;

			}
			calibImage.dataChanged();
			//System.out.println();

		}

	}

}
