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
package plugins.fab.livemousetracker.calibration;

import java.awt.geom.Point2D;

import plugins.fab.kinectdriver.KinectData;
import plugins.fab.kinectdriver.KinectEvent;
import plugins.fab.kinectdriver.KinectListener;
import plugins.fab.kinectdriver.KinectStreamer;
import plugins.fab.livemousetracker.LiveMouseTracker;
//UNRELEASED PERSPECTIVE
//import plugins.fab.livemousetracker.transform.PerspectiveCompensator;
import plugins.kernel.roi.roi2d.ROI2DPolygon;
import plugins.kernel.roi.roi2d.ROI2DRectangle;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.plugin.abstract_.PluginActionable;
import icy.plugin.interface_.PluginBundled;
import icy.roi.ROIUtil;
import icy.sequence.Sequence;

public class LiveMouseTrackerCalibration extends PluginActionable implements KinectListener {

	private static Sequence infraOutOriginal = null;
	private static Sequence depthOutOriginal = null;
	private static Sequence infraOutCorrected = null;
	private static Sequence depthOutCorrected = null;
	public static IcyBufferedImage infraImage;
	public static IcyBufferedImage depthImage;
	CalibrationPainter calibrationPainter = new CalibrationPainter();
	KinectStreamer kinectStreamer = new KinectStreamer( false );

	@Override
	public void run() {

		kinectStreamer.addKinectListener( this );
		kinectStreamer.startLive();
	}


	@Override
	public void kinectChange(Sequence sourceSequence, KinectData kinectData , KinectEvent kinectEvent) {

		if ( kinectEvent == KinectEvent.NEW_DEPTH_SEQUENCE )
		{
			depthOutOriginal = sourceSequence;
			depthOutCorrected = new Sequence();
			depthOutCorrected.addOverlay( calibrationPainter );
			depthOutCorrected.setName("Depth corrected");

		//	addCageROI( depthOutCorrected );

//			addSequence ( depthOutCorrected );
			//addSequence( depthOutOriginal );

//			tryToInit();
		}

		if ( kinectEvent == KinectEvent.NEW_INFRARED_SEQUENCE )
		{
			infraOutOriginal = sourceSequence;
			infraOutCorrected = new Sequence();
			addSequence ( infraOutCorrected );
			infraOutCorrected.setName("Infra corrected");
			infraOutCorrected.addOverlay( calibrationPainter );

			//addCageROI( infraOutCorrected );
			//addSequence( infraOutOriginal );
//			tryToInit();
		}

		if ( kinectEvent == KinectEvent.NEW_INFRARED_CAPTURE )
		{
			process();
		}

		if ( kinectEvent == KinectEvent.NEW_DEPTH_CAPTURE )
		{
			// WARNING: DO NOT PROCESS ON THIS EVENT, DEPTH COMES BEFORE INFRARED
			// WAIT FOR INFRARED TO BE SYNCHRONIZED
		}

		if ( kinectEvent == KinectEvent.KINECT_STOPPED )
		{
			System.out.println("Kinect stopped");
//			rfidManager.kinectStopped();
		}

	}
	// UNRELEASED PERSPECTIVE
//	PerspectiveCompensator perspectiveCompensator = null;

	private void process() {

		setupDisplayLUTViewers();

		depthImage = IcyBufferedImageUtil.getCopy( depthOutOriginal.getImage( 0 , 0 ) );
		infraImage = IcyBufferedImageUtil.getCopy( infraOutOriginal.getImage( 0 , 0 ) );

		LiveMouseTracker.correctInvalidZValue( depthImage );
		LiveMouseTracker.compensateZIntensityError( depthImage , infraImage );

		if ( calibrationPainter.setup == calibrationPainter.setup.Cage_50x50cm_WithCorrection )
		{
			// UNRELEASED PERSPECTIVE
//			if ( perspectiveCompensator == null )
//			{
//				perspectiveCompensator = new PerspectiveCompensator();
//			}
//			infraImage = perspectiveCompensator.compensateInfra( infraImage );
//			depthImage = perspectiveCompensator.compensateDepth( depthImage );

		}

		depthOutCorrected.setImage( 0 , 0 , depthImage );
		infraOutCorrected.setImage( 0 , 0 , infraImage );

		computeErrorMap();

	}


	private void computeErrorMap() {

		double errorMap[][] = new double[27][27];

		for ( int x = 0 ; x < 14 ; x++ )
			for ( int y = 0 ; y < 14 ; y++ )
		{
				ROI2DRectangle roiRect = new ROI2DRectangle(
						x*20 +126-3, y*20 +82-3,
						x*20 +126-3+5, y*20 +82-3+5
						);

				errorMap[x][y] = ROIUtil.getMeanIntensity( depthOutCorrected, roiRect );
		}
		calibrationPainter.setErrorMap(errorMap);


	}


	/**
	 * Correct the diverse Look up table of the viewers.
	 */
	private void setupDisplayLUTViewers() {

		// FIXME: PUT IT BACK.
		try{
		depthOutCorrected.getFirstViewer().getLut().getLutChannel(0).setMinMax( 600, 800 );
		} catch( NullPointerException e ){};

		try{
		infraOutCorrected.getFirstViewer().getLut().getLutChannel(0).setMinMax( 0, 32000 );
		} catch( NullPointerException e ){};

	}



}
