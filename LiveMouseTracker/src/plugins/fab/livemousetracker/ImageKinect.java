package plugins.fab.livemousetracker;

import icy.image.IcyBufferedImage;
import java.awt.Rectangle;

public class ImageKinect
{

	public IcyBufferedImage infraImage;
	public IcyBufferedImage depthImage;
	public int offsetX;
	public int offsetY;
	public Rectangle cropRect = null;

	public ImageKinect(IcyBufferedImage infraImage, IcyBufferedImage depthImage, int offsetX, int offsetY ) {
		this.infraImage = infraImage;
		this.depthImage = depthImage;
		this.offsetX = offsetX;
		this.offsetY = offsetY;		
	}

}
