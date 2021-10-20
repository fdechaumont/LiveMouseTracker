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
package plugins.fab.livemousetracker.MPEGRecorder;

import icy.file.FileUtil;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.image.lut.LUT;
import icy.main.Icy;
import icy.sequence.Sequence;

import java.awt.image.BufferedImage;
import java.util.concurrent.TimeUnit;

//import com.sun.corba.se.spi.activation.InitialNameServiceOperations;
import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.IRational;


public class MPEGRecorderFramePerFrame {

	//private static IRational FRAME_RATE=IRational.make(15,1);
	//private static IRational FRAME_RATE=IRational.make(30,1);
	private IRational FRAME_RATE;

	String outFile;
    IMediaWriter writer = null;
    double time = 0;
    double timeIncrementer = 0;
    int width;
    int height;
    /**
     *
     * @param outFile File to export (will be append with .mp4)
     */
	public MPEGRecorderFramePerFrame( String outFile , int fps ) {

		outFile = FileUtil.setExtension( outFile, ".mp4");
		this.outFile = outFile ;
		FileUtil.ensureParentDirExist( outFile );
		timeIncrementer = 1000d/fps;
		FRAME_RATE = IRational.make( fps , 1 );
		writer = ToolFactory.makeWriter(outFile);


	}

	boolean frameSetup = false;

//	LUT lut = null;

	/** Set an optional LUT for the rendering */
//	public void setLut( LUT lut )
//	{
//		this.lut = lut;
//	}

	public void record( BufferedImage image )
	{
		if ( !frameSetup )
		{
			this.width = image.getWidth();
			this.height = image.getHeight();

			writer.addVideoStream(0, 0, FRAME_RATE, width, height );
			frameSetup = true;
		}

		/*
		// check if current image still the same format.
		if ( image.getHeight() != height || image.getWidth() != width )
		{
			System.out.println("MPEGRecorder: image size changed.");
			this.width = image.getWidth();
			this.height = image.getHeight();
		}
		*/

		// convert to the right image type
		BufferedImage bgrScreen = convertToType(image, BufferedImage.TYPE_3BYTE_BGR);

		// encode the image

		writer.encodeVideo(0,bgrScreen, (long)time, TimeUnit.MILLISECONDS );

//		System.out.println( time );
		//time+= FRAME_RATE.getDouble();
		time += timeIncrementer;

	}
//	public void record( IcyBufferedImage image )
//	{
//		if ( !frameSetup )
//		{
//			writer.addVideoStream(0, 0, FRAME_RATE, image.getWidth(), image.getHeight() );
//			frameSetup = true;
//		}
//
//		//image.setl
//		//infraOut.getFirstViewer().getLut().getLutChannel(0).setMinMax( 0, 32000 );
//
//		BufferedImage screen = IcyBufferedImageUtil.getARGBImage( image , lut );
//
//		// convert to the right image type
//		BufferedImage bgrScreen = convertToType(screen, BufferedImage.TYPE_3BYTE_BGR);
//
//		// encode the image
//		writer.encodeVideo(0,bgrScreen, (long)time, TimeUnit.MILLISECONDS );
////		System.out.println( time );
//		//time+= FRAME_RATE.getDouble();
//		time += timeIncrementer;
//
//	}

	public void close()
	{
		writer.close();
	}

	public boolean isOpen()
	{
		return writer.isOpen();
	}

	public static BufferedImage convertToType(BufferedImage sourceImage,
		      int targetType)
		  {
		    BufferedImage image;

		    // if the source image is already the target type, return the source image

		    if (sourceImage.getType() == targetType)
		      image = sourceImage;

		    // otherwise create a new image of the target type and draw the new
		    // image

		    else
		    {
		      image = new BufferedImage(sourceImage.getWidth(),
		          sourceImage.getHeight(), targetType);
		      image.getGraphics().drawImage(sourceImage, 0, 0, null);
		    }

		    return image;
		  }

	public boolean isMatchingSize(BufferedImage imageToRecord) {

		return imageToRecord.getWidth() == width && imageToRecord.getHeight() == height;

	}


}
