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

import icy.image.IcyBufferedImage;
import icy.roi.BooleanMask2D;
import icy.sequence.Sequence;
import icy.type.DataType;

import java.awt.Rectangle;
import java.awt.geom.Point2D;

/**
 * This class creates an average map of the field.
 */
public class BackgroundHeightMapBuilder {

	boolean ready = false;

	IcyBufferedImage backgroundImage = null;

	// Mettre une confiance sur chaque pixel ?
	// Comment avoir une grande confiance sur la carte ?
	// check volume transfer d'une frame à l'autre ?

	int nbImageIntegrated = 0;
	Sequence backgroundSequence = null;

	public BackgroundHeightMapBuilder() {

		backgroundSequence = new Sequence ( "Background"  , backgroundImage );

	}

	void initMap( IcyBufferedImage candidateImage )
	{

//		backgroundImage = new IcyBufferedImage( 512, 424 , 1 , DataType.FLOAT );
		backgroundImage = new IcyBufferedImage( candidateImage.getWidth(), candidateImage.getHeight() , 1 , DataType.USHORT );
//		float[] data = backgroundImage.getDataXYAsFloat( 0 );
		short[] data = backgroundImage.getDataXYAsShort( 0 );
		for ( int i = 0 ; i < data.length ; i++ )
		{
			data[i] = 0; // distance 0 (at the sensor)
		}
		backgroundImage.dataChanged();
		backgroundSequence.setImage( 0, 0 , backgroundImage );
//		backgroundSequence = new Sequence ( "Background"  , backgroundImage );
	}

	/** Compute the new background map with the given image */
	public void integrateNewDepthMapImage( IcyBufferedImage candidateImage )
	{
		if ( LiveMouseTracker.LOCK_BACKGROUND ) return;

		if ( backgroundImage == null )
		{
			initMap( candidateImage );
		}

		// if the map size change, reinit map.
		if ( backgroundImage.getWidth() != candidateImage.getWidth() || backgroundImage.getHeight() != candidateImage.getHeight() )
		{
			initMap( candidateImage );
		}

		short candidateBuffer[] = candidateImage.getDataXYAsShort( 0 );
		short[] data = backgroundImage.getDataXYAsShort( 0 );

		for ( int i = 0 ; i < data.length ; i++ )
		{
			// TEST 2506
			if ( candidateBuffer[i] == 10000 ) // evite la mise a jour de la map pour les valeurs impossibles
			{
				continue;
			}
			
			if ( data[i] < candidateBuffer[i] )
			{
				data[i] = candidateBuffer[i];
			}
		}

		nbImageIntegrated++;
		if ( nbImageIntegrated > LiveMouseTracker.NUMBER_OF_FRAME_USED_FOR_BACKGROUND_INIT )
		{
			ready = true;
		}
	}

	public IcyBufferedImage getBackgroundImage()
	{
		return backgroundImage;
	}

	/**
	 * I choose to reverse the Z axis of this image. This means that the height of animals is positive.
	 * Which is simpler for further computation/representation.
	 * */
	public IcyBufferedImage getSubstractedImage()
	{
		//IcyBufferedImage substractedImage = new IcyBufferedImage( 512 , 424, 1 , DataType.SHORT );

		IcyBufferedImage substractedImage = new IcyBufferedImage( backgroundImage.getWidth() , backgroundImage.getHeight(), 1 , DataType.SHORT );


		// FIXME: should maybe add a check different
		// size if the call is made exactly when a new client comes and change the are size.

		short[] substractedImageBuffer = substractedImage.getDataXYAsShort( 0 );

		IcyBufferedImage depthImage = LiveMouseTracker.depthImage;
		short[] depthImageBuffer = depthImage.getDataXYAsShort( 0 );
		short[] backGroundBuffer = backgroundImage.getDataXYAsShort( 0 );

		for ( int i = 0 ; i < substractedImageBuffer.length ; i++ )
		{
			// TEST 2506
			if ( depthImageBuffer[i] == 10000 )
			{
				substractedImageBuffer[i] = 0;
				continue;
			}
			// FIN TEST 2506
			substractedImageBuffer[i] = (short)( backGroundBuffer[i] - depthImageBuffer[i]);
		}
		substractedImage.dataChanged();

		return substractedImage;
	}


	public boolean isReady() {
		return ready;
	}

	public Sequence getBackgroundSequence() {
		return backgroundSequence;
	}

	/**
	* This will 'refill' the volume of the background at the specified location
	*
	*
	 * @param depthImage
	 * @param maskCandidate
	 */
	public void correctBackGround(IcyBufferedImage depthImage,
			BooleanMask2D maskCandidate) {

		if ( LiveMouseTracker.LOCK_BACKGROUND ) return;

		short candidateBuffer[] = depthImage.getDataXYAsShort( 0 );
		short[] data = backgroundImage.getDataXYAsShort( 0 );

		Rectangle rectMask = maskCandidate.bounds;

		int minY = (int) rectMask.getMinY();
		int maxY = (int) rectMask.getMaxY();
		int minX = (int) rectMask.getMinX();
		int maxX = (int) rectMask.getMaxX() ;

		final int width = depthImage.getWidth();

		int i = minY * width + minX;
		int maskIndex = 0;
		for ( int y = minY ; y < maxY ; y++ )
		{
			for ( int x = minX, j = i ; x < maxX ; x++, j++ )
			{
				if ( maskCandidate.mask[maskIndex++])
				{
					data[ j ] = candidateBuffer[j];
				}
			}

			i += width;
		}

	}

	public double getVolume(IcyBufferedImage depthImage,
			BooleanMask2D maskCandidate) {

		short candidateBuffer[] = depthImage.getDataXYAsShort( 0 );
		short[] data = backgroundImage.getDataXYAsShort( 0 );

		Rectangle rectMask = maskCandidate.bounds;

		int minY = (int) rectMask.getMinY();
		int maxY = (int) rectMask.getMaxY();
		int minX = (int) rectMask.getMinX();
		int maxX = (int) rectMask.getMaxX() ;

		final int width = depthImage.getWidth();

		int i = minY * width + minX;
		int maskIndex = 0;
		double volume = 0;

		for ( int y =  minY ; y < maxY ; y++ )
		{
			for ( int x = minX, j = i; x < maxX ; x++, j++ )
			{
				if ( maskCandidate.mask[maskIndex++] )
				{
					volume += data[j] - candidateBuffer[j] ; // as z is reversed
				}
			}
			i+= width;
		}

		return volume;
	}

	public double getVolume(IcyBufferedImage depthImage , Point2D point) {

		double volume = 0;

		short depthImageBuffer[] = depthImage.getDataXYAsShort( 0 );
//		float[] backGroundBuffer = backgroundImage.getDataXYAsFloat( 0 );
		short[] backGroundBuffer = backgroundImage.getDataXYAsShort( 0 );

		final int width = depthImage.getWidth();

		int i = (int)point.getY() * width + (int)point.getX();

		// substractedImageBuffer[i] = - depthImageBuffer[i] + backGroundBuffer[i];
		volume = backGroundBuffer[i] - depthImageBuffer[i] ;

		return volume;
	}

	public short[] getRectangleVolume(IcyBufferedImage depthImage , Rectangle rect ) {

		short depthImageBuffer[] = depthImage.getDataXYAsShort( 0 );
//		float[] backGroundBuffer = backgroundImage.getDataXYAsFloat( 0 );
//		float[] zBuffer = new float[rect.height*rect.width];
		short[] backGroundBuffer = backgroundImage.getDataXYAsShort( 0 );
		short[] zBuffer = new short[rect.height*rect.width];

		final int width = depthImage.getWidth();

		int index = rect.y * width + rect.x;
		int indZ = 0;
		for ( int y = 0 ; y < rect.height ; y++ )
		{
			for ( int x = 0, i = index ; x < rect.width ; x++, i++ )
			{
				zBuffer[indZ++] = (short)(backGroundBuffer[i] - depthImageBuffer[i] );
			}

			index += width;
		}

		return zBuffer;
	}


}
