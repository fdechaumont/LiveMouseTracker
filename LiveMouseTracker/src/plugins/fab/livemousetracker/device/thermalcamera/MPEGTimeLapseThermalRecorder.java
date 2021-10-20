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
package plugins.fab.livemousetracker.device.thermalcamera;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.image.colormodel.IcyColorModel;
import icy.image.lut.LUT;
import icy.main.Icy;
import icy.system.thread.ThreadUtil;
import icy.type.DataType;
import plugins.fab.livemousetracker.LiveMouseTracker;
import plugins.fab.livemousetracker.TimeLapseMP4Save;
import plugins.fab.livemousetracker.Util;
import plugins.fab.livemousetracker.MPEGRecorder.MPEGRecorderFramePerFrame;

public class MPEGTimeLapseThermalRecorder {

	MPEGRecorderFramePerFrame mpegRecorder = null;

	ArrayList<BufferedImage> imageToRecordList = new ArrayList<BufferedImage>();
	boolean shutDown = false;
	boolean grabCurrent = false;

	Thread mpegSaver = new Thread() {

		@Override
		public void run() {

			while( !isInterrupted() )
			{
				if ( grabCurrent )
				{
					try
					{
						BufferedImage imageToSave = grabImage( );

						synchronized (imageToRecordList) {

							imageToRecordList.add( imageToSave );
						}
					}
					catch( NullPointerException e)
					{
						System.out.println("[MPEG TimeLapseRecorder Thermal] can't grab image.");
					}
					grabCurrent = false;
				}

				BufferedImage imageToRecord = null;

				if ( shutDown )
				{
					if ( mpegRecorder != null )
					{
						mpegRecorder.close();
						System.out.println( "Number of unsaved images (thermal): " + imageToRecordList.size() );
					}
					break;
				}

				synchronized (imageToRecordList) {
					if ( imageToRecordList.size() > 0 )
					{
						imageToRecord = imageToRecordList.get( 0 );
						imageToRecordList.remove( 0 );
					}else
					{
						Thread.yield();	// no image to save so we wait and reloop.
						continue;
					}
				}

				if ( imageToRecord == null ) // close and create a new MPEG
				{
					mpegRecorder.close();
					mpegRecorder = null;
				}

				if ( imageToRecord != null ) // save image
				{
					if( mpegRecorder == null )
					{
						mpegRecorder = new MPEGRecorderFramePerFrame(
								LiveMouseTracker.BASE_FOLDER +
								LiveMouseTracker.getExperimentName() + "/thermal_t" +LiveMouseTracker.getT() ,
								10 );
						mpegRecorder.record( imageToRecord );
					}
					else
					{
						if ( mpegRecorder.isOpen() )
						{
							mpegRecorder.record( imageToRecord );
						}
						else
						{
							System.err.println("MPEG RECORDER Error: Recorder is not open.");
						}

					}

				}

				Thread.yield();
			}

		}

	};

	public MPEGTimeLapseThermalRecorder() {

		mpegSaver.setPriority( LiveMouseTracker.SECONDARY_THREAD_PRIORITY );
		mpegSaver.start();
//		Util.runSingle( mpegSaver, mpegSaverRunnable );
//		ThreadUtil.runSingle( mpegSaver );
	}

	public void recordMP4TimeLapseThermal() {

		if ( LiveMouseTracker.getT() % 18000 == 0 ) // 10 minutes
		{
			synchronized (imageToRecordList) {
				imageToRecordList.add( null );
			}
		}

		if ( LiveMouseTracker.getT() % 1000 ==0 )
		{
			System.out.println("MPEGTimeLapse Thermal Recorder: number of Image(s) Waiting for record: " + imageToRecordList.size() );
		}

		if ( LiveMouseTracker.getT() % 3 == 0 ) // to get from 30 fps to 10 fps.
		{
			// build the image to save.
			grabCurrent = true;

		}
	}



	private BufferedImage grabImage( ) {

		BufferedImage renderedImage = null;
		// with overlay
		try
		{
			renderedImage = LiveMouseTracker.getThermalSequence().getFirstViewer().getCanvas().getRenderedImage( 0 , 0 , -1 , false );
		}catch( Exception e )
		{
			IcyBufferedImage image = new IcyBufferedImage( 640, 240, 1 , DataType.BYTE );
			renderedImage = image.getARGBImage();
			System.out.println("No thermal image ready");
		}
		return renderedImage;

	}


	public void shutDown() {

		System.out.println("Shutting down MPEG Thermal Record...");
		shutDown = true;
		System.out.println("MPEG Thermal Record shutDown.");

	}

}
