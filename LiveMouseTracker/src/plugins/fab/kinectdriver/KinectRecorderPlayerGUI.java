package plugins.fab.kinectdriver;

import icy.plugin.abstract_.PluginActionable;
import icy.sequence.Sequence;

import java.awt.geom.Point2D;

import plugins.kernel.roi.roi2d.ROI2DPolygon;

public class KinectRecorderPlayerGUI extends PluginActionable implements KinectListener {

	public KinectRecorderPlayerGUI() {

	}

	private static Sequence infraOutOriginal = null;
	private static Sequence depthOutOriginal = null;

	KinectStreamer kinectStreamer = new KinectStreamer(true);

	@Override
	public void run() {
		kinectStreamer.addKinectListener( this );
	}


	@Override
	public void kinectChange(Sequence sourceSequence, KinectData kinectData , KinectEvent kinectEvent) {

		if ( kinectEvent == KinectEvent.NEW_DEPTH_SEQUENCE )
		{
			depthOutOriginal = sourceSequence;
			addSequence( depthOutOriginal );
		}

		if ( kinectEvent == KinectEvent.NEW_INFRARED_SEQUENCE )
		{
			infraOutOriginal = sourceSequence;
			addSequence( infraOutOriginal );
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

	private void addCageROI(Sequence sequence ) {

		// 83,33
		ROI2DPolygon roiCage = new ROI2DPolygon( new Point2D.Double( 86-5+3, 55-5-17 ) );
		roiCage.addNewPoint( new Point2D.Double( 420+5+3, 55-5 -17), false);
		roiCage.addNewPoint( new Point2D.Double( 420+5+3, 395+5 -17), false);
		roiCage.addNewPoint( new Point2D.Double(  86-5+3, 395+5 -17), false);
		roiCage.setCreating( false );
		sequence.addROI( roiCage );

	}


	private void process() {

		setupDisplayLUTViewers();

//		depthImage = IcyBufferedImageUtil.getCopy( depthOutOriginal.getImage( 0 , 0 ) );
//		infraImage = IcyBufferedImageUtil.getCopy( infraOutOriginal.getImage( 0 , 0 ) );
//
//		LiveMouseTracker.correctInvalidZValue( depthImage );
//		LiveMouseTracker.compensateZIntensityError( depthImage , infraImage );
//
//		depthOutCorrected.setImage( 0 , 0 , depthImage );
//		infraOutCorrected.setImage( 0 , 0 , infraImage );
//
//		computeErrorMap();

	}




	/**
	 * Correct the diverse Look up table of the viewers.
	 */
	private void setupDisplayLUTViewers() {

		try{
			depthOutOriginal.getFirstViewer().getLut().getLutChannel(0).setMinMax( 600, 800 );
		} catch( NullPointerException e ){};

		try{
			infraOutOriginal.getFirstViewer().getLut().getLutChannel(0).setMinMax( 0, 32000 );
		} catch( NullPointerException e ){};

	}

}
