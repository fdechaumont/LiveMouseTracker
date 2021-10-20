package plugins.fab.livemousetracker.remotearena;

import java.io.Serializable;

import icy.image.IcyBufferedImage;
import icy.type.DataType;

public class ImageKinectSerialized implements Serializable {

	private static final long serialVersionUID = 630603079326488845L;

	private transient IcyBufferedImage infraImage;
	private transient IcyBufferedImage depthImage;
	byte[] infraRawData = null;
	byte[] depthRawData = null;
	int width;
	int height;


	public ImageKinectSerialized(IcyBufferedImage infraImage, IcyBufferedImage depthImage) {

		this.infraImage = infraImage;
		this.depthImage = depthImage;
		this.width = infraImage.getWidth();
		this.height = infraImage.getHeight();

		infraRawData = infraImage.getRawData( false );
		depthRawData = depthImage.getRawData( false );

	}

	public IcyBufferedImage getInfraImage()
	{
		if ( infraImage == null )
		{
			infraImage = new IcyBufferedImage( this.width, this.height, 1 , DataType.USHORT );
			infraImage.setRawData( infraRawData, false);
		}
		return infraImage;

	}

	public IcyBufferedImage getDepthImage()
	{
		if ( depthImage == null )
		{
			depthImage = new IcyBufferedImage( this.width, this.height, 1 , DataType.USHORT );
			depthImage.setRawData( depthRawData, false);
		}
		return depthImage;

	}



}
