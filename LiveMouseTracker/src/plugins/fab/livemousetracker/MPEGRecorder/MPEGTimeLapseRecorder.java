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

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import icy.image.IcyBufferedImageUtil;
import icy.image.lut.LUT;
import icy.main.Icy;
import icy.system.thread.ThreadUtil;
import plugins.fab.livemousetracker.LiveMouseTracker;
import plugins.fab.livemousetracker.TimeLapseMP4Save;
import plugins.fab.livemousetracker.Util;

public class MPEGTimeLapseRecorder {

	MPEGRecorderFramePerFrame mpegRecorder = null;
//	Thread recordMPEGStreamThread = null;

	ArrayList<BufferedImage> imageToRecordList = new ArrayList<BufferedImage>();
	boolean shutDown = false;
	boolean grabCurrent = false;
	boolean saveOverlay = true;
	String baseName = "video_t";

	Thread mpegSaver = new Thread() {

		@Override
		public void run() {

			while( !isInterrupted() )
			{
				if ( grabCurrent )
				{
					try
					{
						BufferedImage imageToSave = grabImage( saveOverlay );

						synchronized (imageToRecordList) {
							// check if size is same as the previous image.
//							if ( imageToRecordList.size() > 0 )
//							{
//								BufferedImage existingImage = imageToRecordList.get( 0 );
//								if ( existingImage.getWidth() != imageToSave.getWidth() ||
//										existingImage.getHeight() != imageToSave.getHeight() )
//								{
//									// the images are not size-compatible there is probably a new client changing the image.
//									// set null to force a save and continue with the new format
//									imageToSave = null;
//								}
//							}

							imageToRecordList.add( imageToSave );
						}
					}
					catch( NullPointerException e)
					{
						System.out.println("[MPEG TimeLapseRecorder] can't grab image.");
					}
					grabCurrent = false;
				}

				BufferedImage imageToRecord = null;

				if ( shutDown )
				{
					if ( mpegRecorder != null )
					{
						mpegRecorder.close();
						System.out.println( "Number of unsaved images: " + imageToRecordList.size() );
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

				boolean imageSizeChanged = false;
				if ( mpegRecorder != null && imageToRecord != null )
				{
					if ( ! mpegRecorder.isMatchingSize( imageToRecord ) )
					{
						System.out.println("MPEG TimeLapse Recorder: change size.");
						mpegRecorder.close();
						mpegRecorder = null;
					}
				}


				if ( imageToRecord == null || imageSizeChanged ) // close and create a new MPEG
				{
					mpegRecorder.close();
					mpegRecorder = null;
				}

				if ( imageToRecord != null ) // save image
				{
					if( mpegRecorder == null )
					{
						int fps = 30;
						/*
						if ( LiveMouseTracker.cageMode == LiveMouseTracker.CAGE_MODE.RATS_25 )
						{
							fps = 15;
						}
						*/
						/*
						mpegRecorder = new MPEGRecorderFramePerFrame(
								LiveMouseTracker.BASE_FOLDER +
								LiveMouseTracker.getExperimentName() + "/video_t" +LiveMouseTracker.getT() ,
								30 / Integer.parseInt( LiveMouseTracker.guiPanel.getSaveToMp4SkipFrame().getText() ) );
						*/
						mpegRecorder = new MPEGRecorderFramePerFrame(
								LiveMouseTracker.BASE_FOLDER +
								LiveMouseTracker.getExperimentName() + "/" + baseName +LiveMouseTracker.getT() ,
								fps / Integer.parseInt( LiveMouseTracker.guiPanel.getSaveToMp4SkipFrame().getText() ) );
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
							// Can't save image. Put it back in list to save.
//							synchronized (imageToRecordList) {
//								System.out.println("Images in buffer: " + imageToRecordList.size() );
//								imageToRecordList.add( 0 , imageToRecord );
//							}
						}

					}


				}

				Thread.yield();
			}

		}

	};

	public MPEGTimeLapseRecorder( String baseName, boolean saveOverlay ) {

		this.baseName = baseName;
		this.saveOverlay = saveOverlay;
		
		mpegSaver.setPriority( LiveMouseTracker.SECONDARY_THREAD_PRIORITY );
		mpegSaver.start();
//		Util.runSingle( mpegSaver, mpegSaverRunnable );
//		ThreadUtil.runSingle( mpegSaver );
	}

	public void recordMP4TimeLapse() {
		if ( !LiveMouseTracker.guiPanel.getSaveToMp4CheckBox().isSelected() ) return;

		if ( LiveMouseTracker.getT() % 18000 == 0 ) // 10 minutes
		{
			synchronized (imageToRecordList) {
				imageToRecordList.add( null );
			}
		}

		if ( LiveMouseTracker.getT() % 1000 ==0 )
		{
			System.out.println("MPEGTimeLapse Recorder: number of Image(s) Waiting for record: " + imageToRecordList.size() );
		}

		if ( LiveMouseTracker.getT() % Integer.parseInt( LiveMouseTracker.guiPanel.getSaveToMp4SkipFrame().getText() ) == 0 )
		{
			// build the image to save.
			grabCurrent = true;

		}
	}



	private BufferedImage grabImage( boolean withOverlay ) {

//		LUT lut = LiveMouseTracker.getInfraOut().createCompatibleLUT();
//		lut.getLutChannel( 0 ).setMinMax( 0 , 32000 );


		if ( withOverlay )
		{
			//BufferedImage renderedImage = Icy.getMainInterface().getActiveViewer().getCanvas().getRenderedImage( 0 , 0 , -1 , false );
			BufferedImage renderedImage = LiveMouseTracker.getInfraOut().getFirstViewer().getCanvas().getRenderedImage( 0 , 0 , -1 , false );
			return renderedImage;
//			mpegRecorder.record( renderedImage );
		}else
		{

			double min = LiveMouseTracker.getInfraOut().getFirstViewer().getLut().getLutChannel( 0 ).getMin();
			double max = LiveMouseTracker.getInfraOut().getFirstViewer().getLut().getLutChannel( 0 ).getMax();
			
			LUT lut = LiveMouseTracker.getInfraOut().createCompatibleLUT();
			lut.getLutChannel( 0 ).setMinMax( min , max );
			return IcyBufferedImageUtil.getARGBImage( LiveMouseTracker.infraImage, lut );
//			mpegRecorder.record(
//					IcyBufferedImageUtil.getARGBImage(infraImage, lut));
		}

	}


	public void shutDown() {

		System.out.println("Shutting down MPEG Record...");
		shutDown = true;
		System.out.println("MPEG Record shutDown.");

	}

}
