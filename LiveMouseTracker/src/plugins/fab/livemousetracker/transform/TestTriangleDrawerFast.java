package plugins.fab.livemousetracker.transform;

import java.awt.geom.Point2D;

import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.main.Icy;
import icy.plugin.abstract_.PluginActionable;
import icy.sequence.Sequence;
import plugins.kernel.roi.roi2d.ROI2DPolygon;

public class TestTriangleDrawerFast extends PluginActionable {


	@Override
	public void run() {

		IcyBufferedImage image = getActiveImage();

		IcyBufferedImage target = IcyBufferedImageUtil.getCopy( image );
		for ( int y = 0 ; y < target.getHeight() ; y++)
		{
			for ( int x = 0 ; x < target.getWidth() ; x++ )
			{
				target.setData(x, y, 0, 0 );
			}
		}

		int topXOffset = 30;
		int topYOffset = 0;
		int bottomXOffset = -10;
		int bottomYOffset = -55;

		Point2D uvTL = new Point2D.Double( topXOffset, 0 + topYOffset );
		Point2D uvTR = new Point2D.Double( 512 - topXOffset, 0 + topYOffset );
		Point2D uvBL = new Point2D.Double( bottomXOffset, 424 + bottomYOffset );
		Point2D uvBR = new Point2D.Double( 512 - bottomXOffset , 424 +  bottomYOffset );

		TriangleDrawerFast tdf = new TriangleDrawerFast();

		tdf.drawTriangle( target, image,
				new Point2D.Double( 0, 0),
				new Point2D.Double( 512, 424),
				new Point2D.Double( 0, 424),
				uvTL, uvBR, uvBL );


		tdf.drawTriangle( target, image,
				new Point2D.Double( 0, 0),
				new Point2D.Double( 512, 0),
				new Point2D.Double( 512, 424),
				uvTL, uvTR, uvBR );

		tdf.applyTransform( target, image );

		Sequence outSequence = new Sequence( target );
		Icy.getMainInterface().addSequence( outSequence );

		setROI( outSequence );
	}

	private void setROI(Sequence outSequence) {

		ROI2DPolygon roiCage50x50 = new ROI2DPolygon( new Point2D.Double( 86-5+3, 55-5-17 ) );
		roiCage50x50.addNewPoint( new Point2D.Double( 420+5+3, 55-5 -17), false);
		roiCage50x50.addNewPoint( new Point2D.Double( 420+5+3, 395+5 -17), false);
		roiCage50x50.addNewPoint( new Point2D.Double(  86-5+3, 395+5 -17), false);
		roiCage50x50.setCreating( false );

		outSequence.addROI( roiCage50x50 );

		ROI2DPolygon roiCage50x50Floor = new ROI2DPolygon( new Point2D.Double( 86-5+3 +30, 55-5-17 +30 ) );
		roiCage50x50Floor.addNewPoint( new Point2D.Double( 420+5+3 -30 , 55-5 -17 +30 ), false);
		roiCage50x50Floor.addNewPoint( new Point2D.Double( 420+5+3 -30 , 395+5 -17 -30 ), false);
		roiCage50x50Floor.addNewPoint( new Point2D.Double(  86-5+3 +30 , 395+5 -17 -30 ), false);
		roiCage50x50Floor.setCreating( false );



		outSequence.addROI( roiCage50x50Floor );

	}



}
