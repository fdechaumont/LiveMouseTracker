package plugins.fab.livemousetracker;

import icy.image.IcyBufferedImage;

public class ImageKinect
{

	IcyBufferedImage infraImage;
	IcyBufferedImage depthImage;
	int offsetX;
	int offsetY;

	public ImageKinect(IcyBufferedImage infraImage, IcyBufferedImage depthImage, int offsetX, int offsetY ) {
		this.infraImage = infraImage;
		this.depthImage = depthImage;
		this.offsetX = offsetX;
		this.offsetY = offsetY;

	}

}
