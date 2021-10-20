package plugins.fab.livemousetracker.transform;

import java.awt.geom.Point2D;

import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.type.DataType;

public class PerspectiveCompensator {

	TriangleDrawerFast tdf = null;
	IcyBufferedImage infraImageCompensated = null;
	IcyBufferedImage depthImageCompensated = null;

	public IcyBufferedImage compensateInfra( IcyBufferedImage infraImage )
	{
		if ( infraImageCompensated == null )
		{
			infraImageCompensated = new IcyBufferedImage( 512,424, 1, DataType.USHORT );
		}
		compensate( infraImage, infraImageCompensated );
		infraImageCompensated.dataChanged();
		return infraImageCompensated;
	}

	public IcyBufferedImage compensateDepth( IcyBufferedImage depthImage )
	{
		if ( depthImageCompensated == null )
		{
			depthImageCompensated = new IcyBufferedImage( 512,424, 1, DataType.USHORT );
		}
		compensate( depthImage, depthImageCompensated );
		compensateZPerspective( depthImageCompensated );
		depthImageCompensated.dataChanged();
		return depthImageCompensated;
	}

	private IcyBufferedImage compensate( IcyBufferedImage source , IcyBufferedImage target ) {

		// init transform on first call
		if ( tdf == null )
		{
			tdf = new TriangleDrawerFast();

			int topXOffset = 30;
			int topYOffset = 0;
			int bottomXOffset = -10;
			int bottomYOffset = -55;

			Point2D uvTL = new Point2D.Double( topXOffset, 0 + topYOffset );
			Point2D uvTR = new Point2D.Double( 512 - topXOffset, 0 + topYOffset );
			Point2D uvBL = new Point2D.Double( bottomXOffset, 424 + bottomYOffset );
			Point2D uvBR = new Point2D.Double( 512 - bottomXOffset , 424 +  bottomYOffset );

			tdf.drawTriangle( target, source,
					new Point2D.Double( 0, 0),
					new Point2D.Double( 512, 424),
					new Point2D.Double( 0, 424),
					uvTL, uvBR, uvBL );


			tdf.drawTriangle( target, source,
					new Point2D.Double( 0, 0),
					new Point2D.Double( 512, 0),
					new Point2D.Double( 512, 424),
					uvTL, uvTR, uvBR );

		}

		tdf.applyTransform( target, source );
		return target;

	}

	/** compensate Z perspective with camera slightly titled
	 * floor fit: z = 757-0.4y
	 * Must be called *after* image is compensated in perspective
	 *  */
	private void compensateZPerspective( IcyBufferedImage image ) {

		short[] imageData = image.getDataXYAsShort( 0 );
		for ( int y = 0 ; y < 424 ; y++ )
		{
			for ( int x = 0 ; x < 512; x++ )
			{
				int index = y*512+x;
				float value = (float) ( imageData[index] + 620-175 - 584+y*0.4f );
				imageData[index] = (short) value;
			}
		}

	}

}
