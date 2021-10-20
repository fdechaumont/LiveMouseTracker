package plugins.fab.livemousetracker.detection;

import java.awt.Graphics2D;

import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.main.Icy;
import icy.plugin.abstract_.PluginActionable;
import icy.sequence.Sequence;

public class TestMedaillon extends PluginActionable {

	@Override
	public void run() {

		int captureWidth=142;
		// store current patch around the mouse
		IcyBufferedImage infraPatch = IcyBufferedImageUtil.getSubImage( getActiveImage(),
						0,
						0,
						captureWidth, captureWidth );

		// create a rotated patch
		double angle = Math.PI/3d; //  Math.atan2( nose.getY() - tail.getY() , nose.getX() - tail.getX() );
		//gHudRotated.translate( -10 , 10 );

		IcyBufferedImage infraPatchRotated = new IcyBufferedImage( 100, 100, 3 , infraPatch.getDataType_() );
				//IcyBufferedImageUtil.getCopy( infraPatch ); // could be faster to create with fill 0 ?

		Graphics2D gRotated = (Graphics2D) infraPatchRotated.getGraphics();
		gRotated.translate( -22,-22 );
		gRotated.rotate( -angle - Math.PI/2d, captureWidth/2 , captureWidth/2 );
		gRotated.drawImage( infraPatch.getImage( 0 ), null, 0, 0 );

		Sequence s1 = new Sequence( infraPatch );
		Sequence s2 = new Sequence( infraPatchRotated );
		Icy.getMainInterface().addSequence( s1 );
		Icy.getMainInterface().addSequence( s2 );



	}
}
