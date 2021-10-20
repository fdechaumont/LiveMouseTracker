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

import icy.main.Icy;
import icy.sequence.Sequence;

import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.IRational;


public class MPEGRecorderStream {

//	private static IRational FRAME_RATE=IRational.make(3,1);
	private static IRational FRAME_RATE=IRational.make(15,1);
	private static final int SECONDS_TO_RUN_FOR = 30;

    String outFile;
    Sequence recordSequence;
    Thread recordingThread;
    private boolean stopRecord = false;
    private boolean recording = false;

    /**
     *
     * @param outFile File to export mpeg. Must be a .mp4 file.
     */
	public MPEGRecorderStream( String outFile , Sequence sequence ) {

		this.recordSequence = sequence;

		Date date = new Date();
		String dateString = new SimpleDateFormat("yyyy-MM-dd HH'h'mm-ss's'").format(date);

		this.outFile = outFile + " " + dateString + ".mp4";

		System.out.println("Record file is: " + this.outFile );

	}

	public void stopRecord()
	{
		stopRecord = true;
		recording = false;
	}

	public void record()
	{
		recordingThread = new Thread( new Runnable() {
			@Override
			public void run() {
				recordingRunnable();
			}
		});
		recordingThread.start();

	}

	public boolean isRecording() {
		return recording;
	};

	public void recordingRunnable()
	{
		recording = true;
		final IMediaWriter writer = ToolFactory.makeWriter(outFile);

		writer.addVideoStream(0, 0,
				FRAME_RATE,
				Icy.getMainInterface().getActiveViewer().getSequence().getWidth(),
				Icy.getMainInterface().getActiveViewer().getSequence().getHeight()
				);

		long startTime = System.nanoTime();

		while ( !stopRecord )
			//for (int index = 0; index < SECONDS_TO_RUN_FOR*FRAME_RATE.getDouble(); index++)
		{

			BufferedImage screen = recordSequence.getFirstViewer().getCanvas().getRenderedImage( 0, 0, -1 , false );


			// convert to the right image type
			BufferedImage bgrScreen = convertToType(screen,
					BufferedImage.TYPE_3BYTE_BGR);

//			BufferedImage bgrScreen = screen;

			// encode the image
			writer.encodeVideo(0,bgrScreen,
					System.nanoTime()-startTime, TimeUnit.NANOSECONDS);

			//System.out.println("encoded image: " +index);

			// sleep for framerate milliseconds
			try {
				Thread.sleep((long) (1000 / FRAME_RATE.getDouble()));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//			System.out.println("..rec..");
		}

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
