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

import icy.gui.frame.progress.ProgressFrame;
import icy.main.Icy;
import icy.sequence.Sequence;

import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.IRational;


public class MPEGRecorderSequence {

//	private static IRational FRAME_RATE=IRational.make(3,1);
	private static IRational FRAME_RATE=IRational.make(15,1);


    /**
     *
     * @param outFile File to export mpeg. Must be a .mp4 file.
     */
	public MPEGRecorderSequence( String outFile , Sequence sequence ) {

		Date date = new Date();
		String dateString = new SimpleDateFormat("yyyy-MM-dd HH'h'mm-ss's'").format(date);

		outFile = outFile + " " + dateString + ".mp4";

		System.out.println("Record file is: " + outFile );


		final IMediaWriter writer = ToolFactory.makeWriter(outFile);

		writer.addVideoStream(0, 0,
				FRAME_RATE,
				Icy.getMainInterface().getActiveViewer().getSequence().getWidth(),
				Icy.getMainInterface().getActiveViewer().getSequence().getHeight()
				);

		long startTime = System.nanoTime();

		ProgressFrame progress = new ProgressFrame("Recording mp4...");

		for ( int t=0 ; t < sequence.getSizeT() ; t++ )
		{
			BufferedImage screen = sequence.getFirstViewer().getCanvas().getRenderedImage( t, 0, -1 , false );
			// convert to the right image type
			BufferedImage bgrScreen = convertToType(screen, BufferedImage.TYPE_3BYTE_BGR);
			// encode the image
			writer.encodeVideo(0,bgrScreen, System.nanoTime()-startTime, TimeUnit.NANOSECONDS);
			progress.setPosition( 100d* (double)t / (double)sequence.getSizeT() );
		}

		progress.close();
		System.out.println("Closing writer...");

		writer.close();

		System.out.println("Record finished");

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


}
