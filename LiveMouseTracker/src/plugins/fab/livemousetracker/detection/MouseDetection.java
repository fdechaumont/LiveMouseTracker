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
package plugins.fab.livemousetracker.detection;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import icy.canvas.IcyCanvas;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.roi.BooleanMask2D;
import icy.roi.BooleanMask2DIterator;
import icy.type.point.Point3D;
import icy.util.XMLUtil;
import plugins.fab.livemousetracker.Animal;
import plugins.fab.livemousetracker.BackgroundHeightMapBuilder;

import plugins.fab.livemousetracker.LiveMouseTracker;
import plugins.fab.livemousetracker.LiveMouseTrackerPanel;
import plugins.fab.livemousetracker.ROI2DAreaX;
import plugins.fab.livemousetracker.SQLiteSavedData;
import plugins.fab.livemousetracker.Util;
import plugins.fab.livemousetracker.MPEGRecorder.MPEGMedaillonRecorder;
import plugins.fab.livemousetracker.histogram.Histogram;
import plugins.fab.livemousetracker.machinelearning.MachineLearningSubPartBuilder;
import plugins.fab.livemousetracker.morpho.BooleanMaskUtil;
import plugins.fab.livemousetracker.morpho.Moment;
import plugins.fab.livemousetracker.morpho.MorphoROITools;
import plugins.fab.livemousetracker.overlay.Event;
import plugins.fab.livemousetracker.track.TrackSegment;
import plugins.kernel.roi.roi2d.ROI2DArea;
import plugins.kernel.roi.roi2d.ROI2DLine;
import weka.core.Instance;

public class MouseDetection {

	/** Angle of vector massCenter to Head (in radian)*/
	public double angle;

	public Point2D endTailPoint;

	ArrayList<BooleanMask2D> discriminantPartsComponentList = new ArrayList<BooleanMask2D>();
	ArrayList<BooleanMask2D> earComponentList = new ArrayList<BooleanMask2D>();
	ArrayList<BooleanMask2D> noseComponentList = new ArrayList<BooleanMask2D>();

	public double[] infraHisto;
	public double[] depthHisto;

	float histogram[]; // histogram equivalent to TrackId (not used anymore, implemented for comparaison)
	IcyBufferedImage histogramImage;
	Point2D instantSpeedVector ;

	double longAxis = 0;
	Point2D majorAxis = null;
	Point2D minorAxis = null;

	/** The infra patch is the image of the mouse taken with its backGround to solve head/tail/reverse issues. */
	public IcyBufferedImage infraPatch;
	//public IcyBufferedImage infraPatchRotated;

	public IcyBufferedImage getInfraPatchRotated( double angle )
	{
		IcyBufferedImage infraPatchRotated = new IcyBufferedImage( 100, 100, 1 , infraPatch.getDataType_() );
		int infraWidth = infraPatch.getWidth();
		Graphics2D gRotated = (Graphics2D) infraPatchRotated.getGraphics();
		gRotated.translate( -22 , -22 );
		gRotated.rotate( -angle - Math.PI/2d, infraWidth/2,infraWidth/2 );
		gRotated.drawImage( infraPatch.getImage( 0 ), null, 0, 0 );
//		gRotated.translate( -50 , -50 );
//		gRotated.rotate( -angle - Math.PI/2d, 100 , 100 );
//		gRotated.drawImage( infraPatch.getImage( 0 ), null, 0, 0 );
		return infraPatchRotated;
	}

	Point3D massCenter = null;

	double maxInfraIntensity;
	double minInfraIntensity;

	double meanDepth ;
	double meanInfraIntensity ;

	ROI2DArea mouseROI;
	BooleanMask2D mouseROIMask;
	Point[] mouseROIPoints;

	MouseType mouseType = null;
	// POST PROCESS DATA (Computed optionally if associated to a track)
	/** Point at the front of the mouse, respecting major axis of the animal */
	public Point3D frontPoint;
	/** Point at the back of the mouse, respecting major axis of the animal */
	public Point3D backPoint;

	MainAxis mainAxis = new MainAxis();

	private Double meanIntensityForEar = null;

	ArrayList<SubPartDescriptor> subPartDescriptorList = new ArrayList<SubPartDescriptor>();

	boolean rearing = false;
	boolean lookingDown = false;
	boolean lookingUp = false;

	boolean postProcessed = false;

	double shorterAxis = 0;
	double surface;

	private boolean thisDetectionCanBeUsedForLearning = true;
	private boolean builtBySplitter = false;

	public boolean isLookingDown() {
		return lookingDown;
	}

	public boolean isLookingUp() {
		return lookingUp;
	}


	public void setCanBeUsedForLearning( boolean thisDetectionCanBeUsedForLearning) {
		this.thisDetectionCanBeUsedForLearning = thisDetectionCanBeUsedForLearning;
	}

	public boolean canBeUsedForLearning() {
		return thisDetectionCanBeUsedForLearning;
	}

	/** TimePoint */
	int t;

	Tail tail = null;

	private BooleanMask2D tailMask = null;

	/** spine is describing the Z along the back */
	Spine spine = new Spine();

	double volume;

	// tmp value to display probability of head is head and back is back.
	private Double pBEqualBack;
	private Double pAEqualHead;

	/**
	 * Proportion of the machine learning filter to distinguish this detection from an error.
	 * */
	public double detectionChanceWithMLFilter = -1;

	public Spine getZSpine()
	{
		return spine;
	}

	public Point2D getInstantSpeedVector() {
		return instantSpeedVector;
	}

	public Double getInstantSpeed() {
		if ( instantSpeedVector == null ) return null;
		return instantSpeedVector.distance( 0 , 0 );
	}

	public MouseDetection(ROI2DArea roi , BooleanMask2D roiMask, int t ) {
		//		Sequence infraSequence = LiveMouseTracker.getInfraOut();
		//		Sequence depthSequence = LiveMouseTracker.getDepthOut();
		BackgroundHeightMapBuilder backgroundHeightMapBuider = LiveMouseTracker.getBackgroundHeightMapBuider();

		this.mouseROI = roi;
		this.mouseROIMask = roiMask;
		this.mouseROIPoints = roiMask.getPoints();
		this.t = t;

//		System.out.println("new detection build");
		infraHisto = buildHistogram( mouseROIPoints, LiveMouseTracker.infraImage, "infra det construct");
		depthHisto = buildHistogram( mouseROIPoints, LiveMouseTracker.depthImage, "depth det construct" );

//		infraHisto = buildHisto( mouseROI , new Sequence( LiveMouseTracker.infraImage ) , "infra det construct");
//		depthHisto = buildHisto( mouseROI , new Sequence( LiveMouseTracker.depthImage ) , "depth det construct" );

		BooleanMask2DIterator maskIt = new BooleanMask2DIterator(mouseROIMask);
		short[] infraData = LiveMouseTracker.infraImage.getDataXYAsShort(0);
	    long x = 0L, y = 0L;
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        long sum = 0L;
        long npPoint = 0L;
        final int width = LiveMouseTracker.infraImage.getWidth();

        while(!maskIt.done())
		{
        	int xi = maskIt.getX();
        	int yi = maskIt.getY();
        	int val = infraData[xi + (yi * width)] & 0xFFFF;

            x += xi;
            y += yi;
            sum += val;
            if (val > max) max = val;
            else if (val < min) min = val;
            npPoint++;

        	maskIt.next();
		}

        if (sum > 0)
        {
	        massCenter = new Point3D.Double((double) x / (double) npPoint, (double) y / (double) npPoint, 0);
			meanInfraIntensity = (double) sum / (double) npPoint;
			maxInfraIntensity = max;
			minInfraIntensity = min;
        }
        else
        {
            massCenter = new Point3D.Double();
    		meanInfraIntensity = 0;
    		maxInfraIntensity = 0;
    		minInfraIntensity = 0;
        }


//		Point2D massCenter2D = ROIUtil.getMassCenter( mouseROI );
//		this.massCenter = new Point3D.Double( massCenter2D.getX(), massCenter2D.getY(), 0 );
//
////				meanInfraIntensity = ROIUtil.getMeanIntensity( infraSequence , mouseROI ) ;
////				maxInfraIntensity = ROIUtil.getMaxIntensity( infraSequence , mouseROI ) ;
//		{
//			Sequence tmpInfra = new Sequence ( LiveMouseTracker.infraImage );
//			meanInfraIntensity = ROIUtil.getMeanIntensity( tmpInfra , mouseROI );
//			maxInfraIntensity = ROIUtil.getMaxIntensity( tmpInfra , mouseROI );
//			minInfraIntensity = ROIUtil.getMinIntensity( tmpInfra , mouseROI );
//		}
		//		meanDepth = backgroundHeightMapBuider.getVolume( depthSequence.getImage( 0, 0 ), mouseROI.getBooleanMask( true ) );

		surface = npPoint;
		volume = backgroundHeightMapBuider.getVolume( LiveMouseTracker.depthImage, mouseROIMask );
		meanDepth = volume / surface;
		// test with more values


		buildMassCenterZ();
		//System.out.println( massCenter + " " + massCenterZ );


		findMinorAndMajorAxis();

		// build histogram features.

		if ( LiveMouseTracker.ID_TRACKER_LIKE_HISTOGRAM_FEATURES_ENABLED )
		{
			Histogram histogramBuilder = new Histogram( mouseROI, LiveMouseTracker.infraImage );
			this.histogram = histogramBuilder.getIntensityHistogram1D();
			this.histogramImage = histogramBuilder.getIntensityHistogramAsImage();
		}

		detectMouseType();

		if( mouseType == MouseType.BLACK )
		{
			detectBlackAnimalsEarsAndNose();
		}

		if( mouseType == MouseType.WHITE )
		{
			detectWhiteEyes();
		}

		// store current patch around the mouse
		int patchWidth=142;
		infraPatch = IcyBufferedImageUtil.getSubImage( LiveMouseTracker.getInfraOut().getImage( 0, 0),
						(int)massCenter.getX()-patchWidth/2,
						(int)massCenter.getY()-patchWidth/2,
						patchWidth, patchWidth );

		// create a rotated patch
//		double angle = Math.PI/4d; //  Math.atan2( nose.getY() - tail.getY() , nose.getX() - tail.getX() );
		//gHudRotated.translate( -10 , 10 );

//		infraPatchRotated = new IcyBufferedImage( 100, 100, 1 , infraPatch.getDataType_() );
//				//IcyBufferedImageUtil.getCopy( infraPatch ); // could be faster to create with fill 0 ?
//
//		Graphics2D gRotated = (Graphics2D) infraPatchRotated.getGraphics();
//		gRotated.translate( -50 , -50 );
//		gRotated.rotate( -angle - Math.PI/2d, 100 , 100 );
//		gRotated.drawImage( infraPatch.getImage( 0 ), null, 0, 0 );


		// 29/08/2016: remove auto find head.
		//findAhead();

		//findTail(tailCandidateArrayList, false);

		// buildSubPartsDescriptors();

	}

	public MouseDetection(ROI2DArea roi , int t ) {
		this(roi, roi.getBooleanMask(true), t);
	}

	private ArrayList<SubPartDescriptor> buildSubPartsDescriptors() {

		ArrayList<SubPartDescriptor> subPartDescriptorList = new ArrayList<SubPartDescriptor>();

//		if ( nosePoint == null || tailPoint == null )
//		{
//			return;
//		}
		// build A and B. A=head.
//		Point2D nose = new Point2D.Double( nosePoint.getX() , nosePoint.getY() );
//		Point2D tail = new Point2D.Double( tailPoint.getX() , tailPoint.getY() );

		Rectangle bounds = mouseROIMask.bounds;
		int w = mouseROIMask.bounds.width;
		Point2D nose = mainAxis.pA;
		Point2D tail = mainAxis.pB;

// FIXME: should not be nose but p1 p2 from ellipse fit.
// TODO: extend to quadran

		// part A. (head)
		BooleanMask2D partAMask= (BooleanMask2D) mouseROIMask.clone();
		// part B. (tail)
		BooleanMask2D partBMask= (BooleanMask2D) mouseROIMask.clone();

		for ( Point p : mouseROIPoints)
		{
			int offset = (p.x - bounds.x) + ((p.y - bounds.y) * w);

			if ( p.distance( nose ) < p.distance( tail ) )
				partBMask.mask[offset] = false; // remove point from tail
			else
				partAMask.mask[offset] = false; // remove point from head
		}

		partAMask.optimizeBounds();
		partBMask.optimizeBounds();

		// display parts as debugs
		/*
		{
			ROI2DArea partAROI = new ROI2DArea( partAMask );
			partAROI.setName("tmp part A");
			partAROI.setShowName( true );
			LiveMouseTracker.addROIToInfraSequence( partAROI );

			ROI2DArea partBROI = new ROI2DArea( partBMask );
			partBROI.setName("tmp part B");
			partBROI.setShowName( true );
			LiveMouseTracker.addROIToInfraSequence( partBROI );
		}*/

//		// part A. (head)
//		ROI2DArea partAROI = (ROI2DArea) mouseROI.getCopy();
//		// part B. (tail)
//		ROI2DArea partBROI = (ROI2DArea) mouseROI.getCopy();

//		partAROI.beginUpdate();
//		partBROI.beginUpdate();
//
//		for ( Point p : mouseROIPoints)
//		{
//			if ( p.distance( nose ) < p.distance( tail ) )
//				partBROI.removePoint( p ); // remove point from tail
//			else
//				partAROI.removePoint( p ); // remove point from head
//		}
//		partAROI.endUpdate();
//		partBROI.endUpdate();

		subPartDescriptorList.clear();

//		System.out.println("build A");
		subPartDescriptorList.add( buildSubPartDescriptor( partAMask) );
//		System.out.println("build B");
		subPartDescriptorList.add( buildSubPartDescriptor( partBMask) );

		return subPartDescriptorList;
//		this.subPartDescriptorList = subPartDescriptorList;

	}


	public ArrayList<SubPartDescriptor> getSubPartDescriptorList() {
		return subPartDescriptorList;
	}

	private SubPartDescriptor buildSubPartDescriptor( BooleanMask2D partMask) {
//	private SubPartDescriptor buildSubPartDescriptor( ROI2DArea partROI ) {

//		BackgroundHeightMapBuilder backgroundHeightMapBuider = LiveMouseTracker.getBackgroundHeightMapBuider();

//		Sequence tmpInfra = new Sequence ( LiveMouseTracker.infraImage );
		SubPartDescriptor subpartDescriptor = new SubPartDescriptor();
//		subpartDescriptor.surface = partROI.getNumberOfPoints();
//		subpartDescriptor.contour = partROI.computeNumberOfContourPoints();
//		double volume = backgroundHeightMapBuider.getVolume( LiveMouseTracker.depthImage, mouseROIMask );
//		subpartDescriptor.volume = backgroundHeightMapBuider.getVolume( LiveMouseTracker.depthImage, mouseROI.getBooleanMask( true ) );
//		subpartDescriptor.maxInfraIntensity = ROIUtil.getMaxIntensity( tmpInfra, partROI );
//		subpartDescriptor.meanIntensityArea = meanInfraIntensity = ROIUtil.getMeanIntensity( tmpInfra , mouseROI );
//		subpartDescriptor.meanDepth = volume / subpartDescriptor.surface;
//		BooleanMask2D mask = partROI.getBooleanMask(true);
		Point[] points = partMask.getPoints();
		subpartDescriptor.infraHisto = buildHistogram( points , LiveMouseTracker.infraImage , "infra subpart" );
		subpartDescriptor.depthHisto = buildHistogram( points , LiveMouseTracker.depthImage , "depth subpart" );
//		subpartDescriptor.infraHisto = buildHisto( partROI , new Sequence( LiveMouseTracker.infraImage ) , "infra subpart" );
//		subpartDescriptor.depthHisto = buildHisto( partROI , new Sequence( LiveMouseTracker.depthImage ) , "depth subpart" );

		return subpartDescriptor;
	}

	//double[] buildHisto( ROI2DArea roi , Sequence s ) // SubPartDescriptor subPartDescriptor
	public static double[] buildHistogram( Point[] roiPoints, IcyBufferedImage image, String info )
//	public static double[] buildHisto( ROI2DArea roi , Sequence s, String info )
	{
//		// test
//		{
//			Double m = null;
//			double minNbp = Double.MAX_VALUE; //roi.getNumberOfPoints();
//			double maxNbp = Double.MIN_VALUE; //roi.getNumberOfPoints();
//			// test min return value
//			for ( int i = 0 ; i < 10 ; i ++ )
//			{
//				double min = ROIUtil.getMinIntensity( s, roi , 0 , 0 ,0  );
//
//				double su = roi.getNumberOfPoints();
//				if( su > maxNbp ) maxNbp = su;
//				if ( su < minNbp ) minNbp = su;
//
//				if ( m == null )
//				{
//					m = min;
//				}
//				else
//				{
//					if ( m!= min )
//					{
//						System.err.println("Dif in MIN !! : m="+m + " min:"+min );
//						System.err.println("min: " + minNbp + " max:"+maxNbp );
//					}
//				}
//			}
//		}

		int w = image.getWidth();
		short[] imageData = image.getDataXYAsShort(0);
		double min = Integer.MAX_VALUE;
		double max = Integer.MIN_VALUE;

		for(Point p: roiPoints)
		{
			// get as unsigned
			int val = imageData[p.x + (p.y * w)] & 0xFFFF;

			if (val > max) max = val;
			else if (val < min) min = val;
		}

		icy.math.Histogram histo = new icy.math.Histogram(min, max, LiveMouseTracker.NB_SIGNATURE_HISTO_BIN, false);

		for(Point p: roiPoints)
			histo.addValue(imageData[p.x + (p.y * w)] & 0xFFFF);

		double maxHisto = 0;

		for(int val: histo.getBins())
			if (val > maxHisto) maxHisto = val;

		double[] histoDouble = new double[LiveMouseTracker.NB_SIGNATURE_HISTO_BIN];

		for (int i = 0 ; i < histoDouble.length ; i++)
			histoDouble[i] = (double)histo.getBinSize(i) / maxHisto;

//		//Sequence s = new Sequence ( LiveMouseTracker.infraImage );
//		double[] histo = new double[LiveMouseTracker.NB_SIGNATURE_HISTO_BIN];
//		double min = ROIUtil.getMinIntensity( s, roi , 0 , 0 , 0 );
//		double max = ROIUtil.getMaxIntensity( s, roi , 0 , 0 , 0 );
//		double bin = (max-min) / LiveMouseTracker.NB_SIGNATURE_HISTO_BIN;
//		try
//		{
//		IcyBufferedImage image = s.getImage( 0 , 0 );
//		for( Point p: roi.getBooleanMask( true ).getPoints() )
//		{
////			double val = s.getData( 0, 0 , 0, p.y , p.x ); // FIXME: remove this (SLOW)
//			double val = image.getDataAsShort( p.x, p.y, 0 ) & 0xFFFF;
////			System.out.print( val + ", " );
//			val = val - min ;
//			if ( val > 0 ) val--; // just to remove the specific val=max case.
//			int index = (int)(val/bin);
//			if ( index <0 )
//			{
//				System.out.println("info: " + info );
//				System.err.println( "error while building histo: val ="+val + " bin=" + bin + " min="+min + " max=" + max );
//				System.out.println( "cur min: " + ROIUtil.getMinIntensity( s, roi ) );
//				System.out.println( "cur max: " + ROIUtil.getMaxIntensity( s, roi ) );
//			}
//			histo[index]++;
//		}
//		}catch( Exception e )
//		{
//			System.out.println("FIX THIS HIST PROBLEM !");
//			e.printStackTrace();
//		}

		// normalize

//		double maxHisto = 0;
//		for ( int i = 0 ; i < histo.length ; i++ )
//		{
//			if (histo[i] > maxHisto) maxHisto = histo[i];
//		}
//		if ( maxHisto > 0 )
//		{
//			for ( int i = 0 ; i < histo.length ; i++ )
//			{
//				histo[i]/= maxHisto;
//			}
//		}


//		System.out.println("");
//		System.out.println( "histo: " + Arrays.toString( histo ) );
		return histoDouble;
//		return histo;

	}


	private void buildMassCenterZ() {
		Util.setZ( massCenter , 2 , mouseROI );
	}

	private void buildFrontPointZ() {
		if ( frontPoint != null )
		{
			Util.setZ( frontPoint, 2 , mouseROI );
		}
	}

	private void buildBackPointZ() {
		if ( backPoint != null )
		{
			Util.setZ( backPoint , 2, mouseROI );
		}
	}

	private void computeInstantSpeedVector(TrackSegment trackSegment) {

		MouseDetection previous = trackSegment.getDetection( getT() -1 );
		MouseDetection next = trackSegment.getDetection( getT() +1 );

		if ( previous !=null )
		{
			instantSpeedVector = getVector( previous, this );
			return;
		}

		if ( next !=null )
		{
			instantSpeedVector = getVector( this, next );
			return;
		}

	}

	/**
	 * Compute the nose and tail position when speed is ready.
	 *
	 */
	public void computeNoseAndTailPositionWithSpeedInfo( )
	{
		// don't compute if the speed is too low.
		// FIXME: CONST to remove or estimate ( factor to animal size ?)
		// we must be very confident in this detection as we will train the
		// classifier for head and tail with it.
		if ( instantSpeedVector.distance( 0 , 0 ) < 3 ) return;

		double angle = Math.atan2(
				instantSpeedVector.getY() ,
				instantSpeedVector.getX()
				);

		// estimate nose position nose and tail

		Point2D nose = new Point2D.Double();
		Point2D tail = new Point2D.Double();
		double maxNoseDistance = 0;
		double maxTailDistance = 0;

		// Compute the angle vector
		double vectorAngleX = Math.cos ( angle );
		double vectorAngleY = Math.sin ( angle );
		Point2D massCenter2d = massCenter.toPoint2D();

		for ( Point p : mouseROIPoints)
		{
			// TODO: add a "same angle criteria"
			// TODO: add a "max edge location criteria (pour le piqué du nez)"
			double distance = p.distance(massCenter2d);

			if ( distance > maxNoseDistance )
			{
				double vX = p.getX() - massCenter.getX();
				double vY = p.getY() - massCenter.getY();

				// projection orthogonale: xA*yA + xB*yB / norme( AB )
				if( vX * vectorAngleX + vY * vectorAngleY > 0 )
				{
					nose = p;
					maxNoseDistance = distance;
					continue;
				}
			}

			if ( distance > maxTailDistance )
			{
				double vX = p.getX() - massCenter.getX();
				double vY = p.getY() - massCenter.getY();

				// projection orthogonale: xA*yA + xB*yB / norme( AB )
				if( vX * vectorAngleX + vY * vectorAngleY <= 0 )
				{
					tail = p;
					maxTailDistance = distance;
					continue;
				}
			}
		}

		// Record

		frontPoint = new Point3D.Double();
		frontPoint.setX( nose.getX() );
		frontPoint.setY( nose.getY() );

		backPoint = new Point3D.Double();
		backPoint.setX( tail.getX() );
		backPoint.setY( tail.getY() );

	}

	/** Try to detect the ears and optionally the nose.
	 * */
	private void detectBlackAnimalsEarsAndNose() { // (and pink legs !)

		if ( LiveMouseTracker.ANIMAL_IS_WIRED ) return;

//		if( LiveMouseTracker.HEAD_DETECTION_METHOD == HeadDetectionMethod.ANIMAL_IS_WIRED ) return;

		// For black mice,
		// - Ears are detected in the infra
		// - They consist in a light spot
		// - There is a constraint of distance.
		// - Nose - ears creates a triangle.

		IcyBufferedImage infraImage = LiveMouseTracker.infraImage;
		discriminantPartsComponentList = new ArrayList<BooleanMask2D>();
		earComponentList = new ArrayList<BooleanMask2D>();
		noseComponentList = new ArrayList<BooleanMask2D>();

		//const ear Threshold

		// TMP Detection des oreilles

		int EAR_THRESHOLD = 4000;

		BooleanMask2D booleanEar = thresholdInROI( infraImage , mouseROIMask , EAR_THRESHOLD );
//		BooleanMask2D booleanEar = thresholdInROI( infraImage , mouseROI , EAR_THRESHOLD );
		// threshold et fait le et avec !(le perimetre).
		ROI2DArea mouseROIEroded = MorphoROITools.erodeROI( mouseROI, 2, 2 , 1 );

		if ( booleanEar == null || mouseROIEroded == null ) return;
		booleanEar = booleanEar.getIntersection( mouseROIEroded.getBooleanMask( true ) );


		for ( BooleanMask2D b2 : booleanEar.getComponents() )
		{
			Point[] points = b2.getPoints();

			if ( points.length > 4 && points.length < 100 )
//			if ( b2.getPoints().length > 4 && b2.getPoints().length < 100 )
			{
				// those detected elements must be at a given altitude minimum, else it is background
				double volume = LiveMouseTracker.getBackgroundHeightMapBuider().getVolume(
						LiveMouseTracker.depthImage , b2);
				volume/= points.length;

				if ( volume < 10 ) continue;

				//				System.out.println("Nb : " + b2.getPoints().length + " VolumeNormalized: " + volume );
				discriminantPartsComponentList.add( b2 );

				//				ROI2DArea tmpEar= new ROI2DArea( b2 );
				//				tmpEar.setName("tmp ear");
				//				tmpEar.setColor( Color.YELLOW );
				//				LiveMouseTracker.getInfraOut().addROI( tmpEar );
			}

		}

		//		double earInfraIntensitySum = 0;
		//		double nbEarAreaFound = 0;

		//		for ( BooleanMask2D comp1 : eyesNoseEarComponentList  )
		//		{
		//			for ( BooleanMask2D comp2 : eyesNoseEarComponentList  )
		//			{
		//				if ( comp1 != comp2 )
		//				{
		//					Point2D m1 = getBooleanMask2DMassCenter( comp1 );
		//					Point2D m2 = getBooleanMask2DMassCenter( comp2 );
		//					double distance = m1.distance( m2 );
		//
		//					if ( distance < 15 && distance > 6 )
		//					{
		////						ROI2DLine line = new ROI2DLine( m1 , m2 );
		////						line.setName("tmp ear");
		////						line.setColor( Color.red );
		////						LiveMouseTracker.getInfraOut().addROI( line );
		//
		////						ROI2DArea tmpEar= new ROI2DArea( comp1 );
		////
		////						earInfraIntensitySum+= ROIUtil.getMeanIntensity( LiveMouseTracker.getInfraOut(), tmpEar );
		////						nbEarAreaFound++;
		//					}
		//				}
		//			}
		//			if ( nbEarAreaFound > 0 )
		//			{
		//				//this.meanIntensityForEar = this.volume; //new Double( earInfraIntensitySum / nbEarAreaFound );
		//				//System.out.println( "" + (int)Math.round((this.meanIntensityForEar)) );
		//			}
		//		}

		/*
		ROI2DArea tmpEar= new ROI2DArea( booleanEar );
		tmpEar.setName("tmp ear");
		tmpEar.setColor( Color.YELLOW );
		LiveMouseTracker.getInfraOut().addROI( tmpEar );
		 */

		// create earList

		{
			// select mask matching with ear and nose
			ArrayList<BooleanMask2D> selectedList = new ArrayList<BooleanMask2D>();

			HashMap<BooleanMask2D, Point2D> mapBoolPoint = new HashMap<BooleanMask2D, Point2D>();
			for ( BooleanMask2D comp : discriminantPartsComponentList  )
				mapBoolPoint.put( comp, getBooleanMask2DMassCenter( comp ) );

			for ( BooleanMask2D comp1 : discriminantPartsComponentList  )
			{
				for ( BooleanMask2D comp2 : discriminantPartsComponentList  )
				{
					if ( comp1 != comp2 )
					{
//						Point2D m1 = getBooleanMask2DMassCenter( comp1 );
//						Point2D m2 = getBooleanMask2DMassCenter( comp2 );
						Point2D m1 = mapBoolPoint.get( comp1 );
						Point2D m2 = mapBoolPoint.get( comp2 );

						double distance = m1.distance( m2 );

						if ( distance < 17 && distance > 6 ) // distance between ears should respect this
						{
							if ( !selectedList.contains( comp1 ) )
							{
								selectedList.add( comp1 );
							}
							if ( !selectedList.contains( comp2 ) )
							{
								selectedList.add( comp2 );
							}
						}
					}
				}
			}

			if ( selectedList.size() == 3 ) // we have the ears and nose.
			{ // nose is the farest from mass center

				Point2D m = massCenter.toPoint2D();
				BooleanMask2D bestMask = null;
				double maxDist = -Double.MAX_VALUE;

				for ( BooleanMask2D comp : selectedList  )
				{
					Point2D c = mapBoolPoint.get( comp );
//					Point2D c = getBooleanMask2DMassCenter( comp );

					double dist = c.distance( m );
					if ( dist > maxDist )
					{
						maxDist = dist;
						bestMask = comp;
					}
				}

				noseComponentList.add( bestMask );
				selectedList.remove( bestMask );
				earComponentList.addAll(selectedList);

//				Point2D m1 = getBooleanMask2DMassCenter( comp1 );
//				Point2D m2 = getBooleanMask2DMassCenter( comp2 );
//				double distance = m1.distance( m2 );
//
//				for ( BooleanMask2D com : selectedList  )
//				{
//					noseComponentList.add( com );
//				}
			}
			else if ( selectedList.size() == 2 ) // we only have ears.
			{
				for ( BooleanMask2D com : selectedList  )
				{
					earComponentList.add( com );
				}
			}
		}
	}

	private void detectMouseType() {

//		if ( LiveMouseTracker.ANIMAL_COLOR_DETECTION_MODE == ColorMode.AUTO_OR_MIX )
		{
			if ( getMeanInfraIntensity() < 8000 )
			{
				setMouseType( MouseType.BLACK );
			}else
			{
				setMouseType( MouseType.WHITE );
			}
			return;
		}
/*
		if ( LiveMouseTracker.ANIMAL_COLOR_DETECTION_MODE == ColorMode.BLACK )
		{
			setMouseType( MouseType.BLACK );
			return;
		}

		if ( LiveMouseTracker.ANIMAL_COLOR_DETECTION_MODE == ColorMode.WHITE )
		{
			setMouseType( MouseType.WHITE );
			return;
		}
*/
	}

	private void detectWhiteEyes() {
		IcyBufferedImage infraImage = LiveMouseTracker.infraImage;
		discriminantPartsComponentList = new ArrayList<BooleanMask2D>();

		short[] infraBuffer = infraImage.getDataXYAsShort( 0 );

		BooleanMask2D mask = mouseROIMask;
		Rectangle bounds = mask.bounds;

		int minX = (int)bounds.getMinX();
		int maxX = (int)bounds.getMaxX();

		int minY = (int)bounds.getMinY();
		int maxY = (int)bounds.getMaxY();

		short [] resultBuffer = new short[ mask.mask.length ];

		final int width = infraImage.getWidth();

		int resultBufferOffset = 0;
		int offset = (minY * width) + minX;
		for ( int y = minY ; y < maxY ; y++ )
		{
			for ( int x = minX, imgOffset = offset; x < maxX ; x++, imgOffset++)
			{
				resultBuffer[resultBufferOffset++] =
						(short) (20000 - ((infraBuffer[imgOffset + 1] - infraBuffer[imgOffset])
										 + (infraBuffer[imgOffset - 1] - infraBuffer[imgOffset])));
			}

			offset += width;
		}

		// create components.
		boolean [] resultMaskBuffer = new boolean[ mask.mask.length ];

		{
			resultBufferOffset = 0;
			for ( int y = minY ; y < maxY ; y++ )
			{
				for ( int x = minX ; x < maxX ; x++ )
				{
					if ( mask.mask[resultBufferOffset ] )
					{
						if ( resultBuffer[ resultBufferOffset ] > 25000 )
							//							&& infraBuffer[ y* 512 + x ] > 16000 )
						{
							resultMaskBuffer[ resultBufferOffset ] = true;
							//infraBuffer[ y* 512 + x ] = 0; // resultBuffer[ resultBufferOffset ];
						}
					}

					resultBufferOffset++;
				}
			}

			BooleanMask2D resultBooleanMask = new BooleanMask2D( bounds, resultMaskBuffer );
			for ( BooleanMask2D b : resultBooleanMask.getComponents() )
			{
				//				ROI2D roi = new ROI2DArea( b );
				//				roi.setColor( Color.YELLOW );
				//				roi.setName("tmp eyes");
				//				LiveMouseTracker.getInfraOut().addROI( roi );
				discriminantPartsComponentList.add( b );
				// FIXME: DRAW EYE AND THINGS
			}

			//infraImage.dataChanged();
		}
	}

	/**
	 * Provide the min distance to the shape.
	 *
	 * OPTIMIZE: could compute with square distance, get the closest point, and then only compute one the
	 * real distance
	 *
	 * @param point
	 * @return
	 */
	public double getMinDistanceToShape( Point2D point )
	{
		double distance = Double.MAX_VALUE;

		for ( Point p : mouseROIPoints)
		{
			double dist = p.distance( point );
			if ( dist < distance )
			{
				distance = dist;
			}
		}

		return distance;
	}

	/*
	private void findAhead() {

		// set the proper list to process
		ArrayList<BooleanMask2D> componentList = discriminantPartsComponentList;
		if ( componentList == null )
		{
			componentList = discriminantPartsComponentList;
		}
		if ( componentList == null ) return;

		// process

		Point2D p1 = new Point2D.Double( massCenter.getX() + Math.cos( angle ) * longAxis /2d ,
				massCenter.getY() + Math.sin( angle ) * longAxis /2d
				);
		Point2D p2 = new Point2D.Double( massCenter.getX() - Math.cos( angle ) * longAxis / 2d ,
				massCenter.getY() - Math.sin( angle ) * longAxis / 2d
				);

		if ( componentList.size() != 0 )
		{
			//massCenter.getX() + Math.cos( angle ) * longAxis /2d  ;
			double p1dist = 0;
			double p2dist = 0;

			for ( BooleanMask2D b : componentList )
			{
				for ( Point p : b.getPoints() )
				{
					p1dist+=p.distanceSq( p1 );
					p2dist+=p.distanceSq( p2 );
				}
			}

			if ( p1dist < p2dist )
			{
				frontPoint = new Point3D.Double( p1.getX(), p1.getY(), 0 );
				backPoint = new Point3D.Double( p2.getX(), p2.getY(), 0 );
			}else
			{
				frontPoint = new Point3D.Double( p2.getX(), p2.getY(), 0 );
				backPoint = new Point3D.Double( p1.getX(), p1.getY(), 0 );
			}

//			buildNosePointZ();
//			buildTailPointZ();
//			buildSpineZ();

		}

	}
	*/

	/** return true if the mainAxis is correct.
	 * Meaning that A should be nose and B tail. return false if no decision is taken.
	 * */
	boolean findAheadWithPatternCorrelation( TrackSegment trackSegment )
	{
		// Check with pattern correlation
		{

			MouseDetection previousDetection = trackSegment.getDetection( t-1 );
			if ( previousDetection == null ) return false;
			// Check if the previous detection has an head position, if not, the angle value has no sense to compare to the current one.
			if ( previousDetection.getFrontPoint() == null )
			{
				return false;
			}

			IcyBufferedImage previousInfraPatchRotated = previousDetection.getInfraPatchRotated( previousDetection.angle );
			IcyBufferedImage currentInfraPatchRotated = this.getInfraPatchRotated( angle );

			double scores[] = getCorrelationScores( previousInfraPatchRotated, currentInfraPatchRotated );

			if ( scores[1] > 0.5 && scores[0]<0.5 )
			{
				mainAxis.swap();
				LiveMouseTracker.addEvent( new Event( "Pattern swap", Color.white, this.getMassCenter().toPoint2D() ));

				return true;
			}
			//return true; // can be confident in data.

		}
		/*
		// If fail, find AHEAD with feature detection ( ear and nose detection ).
		// check speed but at a lower threshold than pure speed method
		boolean USE_EAR_AND_EYES_STUFF_TO_DETECT_ORIENTATION = false;
		if ( USE_EAR_AND_EYES_STUFF_TO_DETECT_ORIENTATION )
		{
			MouseDetection previousDetection = trackSegment.getDetection( t-1 );
			if ( previousDetection == null ) return false;
			HeadComputation hc = getA_B_MC_Vectors( previousDetection , this );

			double MIN_SPEED_FOR_FEATURE_DETECTION = 0;
			if ( hc.projectedSpeedMassCenter > MIN_SPEED_FOR_FEATURE_DETECTION &&
					hc.projectedSpeedA > MIN_SPEED_FOR_FEATURE_DETECTION &&
					hc.projectedSpeedB > MIN_SPEED_FOR_FEATURE_DETECTION ) // if all MC,A,B are fast enough
			{
//				if ( LiveMouseTracker.HEAD_DETECTION_METHOD == HeadDetectionMethod.AUTO )
				{
					if ( earComponentList.size() == 2 && frontPoint == null ) // We don't use this method if the head is already found.
					{
						// check if they are on the right side.
						Point2D e1 = getBooleanMask2DMassCenter( earComponentList.get( 0 ) );

						double de1A = e1.distanceSq( mainAxis.pA );
						double de1B = e1.distanceSq( mainAxis.pB );

						if ( de1B < de1A )
						{
							mainAxis.swap();
						}
						//				System.out.println("[MouseDetection] Find head location with ears");
						//trackSegment.setOrientationAffectedBy("Feature");
						return true;
					}
				}
			}
		}
*/

		return false;


	/*
		MouseDetection previousDetection = trackSegment.getDetection( t-1 );
		if ( previousDetection == null ) return false;

		// Check learning

		// If learning is not ready yet,
		// Check speed of all points of the mouse.

//		double pASpeed = previousDetection.mainAxis.pA.distance( mainAxis.pA );
//		double pBSpeed = previousDetection.mainAxis.pB.distance( mainAxis.pB );

		Point2D pASpeedVector = Util.createVector( previousDetection.mainAxis.pA, mainAxis.pA );
		Point2D pBSpeedVector = Util.createVector( previousDetection.mainAxis.pB, mainAxis.pB );

		Point2D mainAxisVector = Util.createVector( mainAxis.pB , mainAxis.pA );

		// FIXME: vectorProjectRatio seems to give opposite result as expected :/
//		double ratio = Util.vectorProjectRatio( new Point2D.Double(0,0), mainAxisVector , getInstantSpeedVector() );

		double scalarMainAxis_InstantSpeedM = Util.getScalarProduct( mainAxisVector , getInstantSpeedVector() );
		double scalarMainAxis_InstantSpeedA = Util.getScalarProduct( mainAxisVector , pASpeedVector );
		double scalarMainAxis_InstantSpeedB = Util.getScalarProduct( mainAxisVector , pBSpeedVector );

//		ROI2DLine lineMainAxis = new ROI2DLine(
//				(int)mainAxis.pB.getX(),
//				(int)mainAxis.pB.getY(),
//				(int)(mainAxis.pB.getX() + mainAxisVector.getX() ),
//				(int)(mainAxis.pB.getY() + mainAxisVector.getY() ) );
//		lineMainAxis.setName( "tmp vector M" );
//		lineMainAxis.setColor( Color.yellow );
//		LiveMouseTracker.addROIToInfraSequence( lineMainAxis );
//
//		ROI2DLine lineA = new ROI2DLine(
//				(int)mainAxis.pA.getX(),
//				(int)mainAxis.pA.getY(),
//				(int)(mainAxis.pA.getX() + pASpeedVector.getX()  ),
//				(int)(mainAxis.pA.getY() + pASpeedVector.getY()  ) );
//		lineA.setName( "tmp vector A" );
//		lineA.setColor( Color.red );
//		LiveMouseTracker.addROIToInfraSequence( lineA );
//
//		ROI2DLine lineB = new ROI2DLine(
//				(int)mainAxis.pB.getX(),
//				(int)mainAxis.pB.getY(),
//				(int)(mainAxis.pB.getX() + pBSpeedVector.getX()  ),
//				(int)(mainAxis.pB.getY() + pBSpeedVector.getY()  ) );
//		lineB.setName( "tmp vector B " );
//		lineB.setShowName( true );
//		lineB.setColor( Color.green );
//		LiveMouseTracker.addROIToInfraSequence( lineB );

//		double projectedSpeedM = Math.sqrt( Math.abs( Util.getScalarProduct( mainAxisVector, instantSpeedVector ) ) );
//		double projectedSpeedA = Math.sqrt( Math.abs( Util.getScalarProduct( mainAxisVector, pASpeedVector ) ) );
//		double projectedSpeedB = Math.sqrt( Math.abs( Util.getScalarProduct( mainAxisVector, pBSpeedVector ) ) );

		Point2D projectedSpeedVectorMassCenter = Util.project( new Point2D.Double(0,0) , mainAxisVector ,instantSpeedVector ) ;
		Point2D projectedSpeedVectorA = Util.project( new Point2D.Double(0,0) , mainAxisVector, pASpeedVector );
		Point2D projectedSpeedVectorB = Util.project( new Point2D.Double(0,0) , mainAxisVector, pBSpeedVector );

		double projectedSpeedMassCenter = projectedSpeedVectorMassCenter.distance( 0 , 0 );
		double projectedSpeedA = projectedSpeedVectorA.distance( 0 , 0 );
		double projectedSpeedB = projectedSpeedVectorB.distance( 0 , 0 );


//		ROI2DLine linePMassCenter = new ROI2DLine(
//				(int)getMassCenter().getX(),
//				(int)getMassCenter().getY(),
//				(int)(getMassCenter().getX() + projectedSpeedVectorMassCenter.getX() ),
//				(int)(getMassCenter().getY() + projectedSpeedVectorMassCenter.getY() ) );
//		linePMassCenter.setName( "tmp vector PM" );
//		linePMassCenter.setColor( Color.yellow );
//		LiveMouseTracker.addROIToInfraSequence( linePMassCenter );
//
//		ROI2DLine linePA = new ROI2DLine(
//				(int)mainAxis.pA.getX(),
//				(int)mainAxis.pA.getY(),
//				(int)(mainAxis.pA.getX() + projectedSpeedVectorA.getX()  ),
//				(int)(mainAxis.pA.getY() + projectedSpeedVectorA.getY()  ) );
//		linePA.setName( "tmp vector PA" );
//		linePA.setColor( Color.red );
//		LiveMouseTracker.addROIToInfraSequence( linePA );
//
//		ROI2DLine linePB = new ROI2DLine(
//				(int)mainAxis.pB.getX(),
//				(int)mainAxis.pB.getY(),
//				(int)(mainAxis.pB.getX() + projectedSpeedVectorB.getX()  ),
//				(int)(mainAxis.pB.getY() + projectedSpeedVectorB.getY() ) );
//		linePB.setName( "tmp vector PB " );
//		linePB.setShowName( true );
//		linePB.setColor( Color.green );
//		LiveMouseTracker.addROIToInfraSequence( linePB );


//		System.out.println("[HEAD/TAIL PostProcess] M:" + projectedSpeedMassCenter + " A:" + projectedSpeedA + " B:" + projectedSpeedB );

		if ( Util.isSameSign( scalarMainAxis_InstantSpeedM, scalarMainAxis_InstantSpeedA ) // Check if all vector are heading the same direction.
				&&
			Util.isSameSign( scalarMainAxis_InstantSpeedA, scalarMainAxis_InstantSpeedB )
				)
		{
			int MIN_SPEED = 4;
			if ( projectedSpeedMassCenter > MIN_SPEED && projectedSpeedA > MIN_SPEED && projectedSpeedB > MIN_SPEED ) // if all MC,A,B are fast enough
			{
				if ( scalarMainAxis_InstantSpeedM >= 0 ) // correct way.
				{
					return true;
				}

				if ( scalarMainAxis_InstantSpeedM < 0 ) // opposite way
				{
					//				int MIN_SPEED = 3;
					//				if ( getInstantSpeed() > MIN_SPEED
					//				&& pASpeedVector.distance( 0, 0 ) > MIN_SPEED
					//				&& pBSpeedVector.distance( 0, 0 ) > MIN_SPEED )
//					if ( projectedSpeedMassCenter > MIN_SPEED && projectedSpeedA > MIN_SPEED && projectedSpeedB > MIN_SPEED )
//					{
					System.out.println("[HEAD/TAIL PostProcess] SWAP");
					mainAxis.swap();
					return true;
//					}
				}
			}
		}

		return false;
*/

		// FIXME: semble pas être la bonne formule. Ici c'est pas la bonne projection.
		// FIXME: Util.vectorProjectRatio
		/*
		double scalarAB =
				previousDetection.mainAxis.pA.getX() * previousDetection.mainAxis.pB.getX()
				+
				previousDetection.mainAxis.pA.getY() * previousDetection.mainAxis.pB.getY();

		double scalarInstantSpeedAndA =
				previousDetection.mainAxis.pA.getX() * instantSpeedVector.getX()
				+
				previousDetection.mainAxis.pA.getY() * instantSpeedVector.getY();


		if ( getInstantSpeed() > LiveMouseTracker.MIN_INSTANT_SPEED_FOR_SUB_PART_CALCULATION
				&&
				pASpeed >  LiveMouseTracker.MIN_INSTANT_SPEED_FOR_SUB_PART_CALCULATION
				&&
				pBSpeed > LiveMouseTracker.MIN_INSTANT_SPEED_FOR_SUB_PART_CALCULATION
				&&
				scalarAB > 0
				&&
				scalarInstantSpeedAndA > 0
				)
		{
			// Compute where the speed vector is heading.

			// Computes where the final speed vector point is located from the center of mass.
			// we * by const to avoid small drift effect while computing mass center.

			Point2D speedPoint =
					new Point2D.Double(
							getMassCenter().getX() + instantSpeedVector.getX() * 5d,
							getMassCenter().getY() + instantSpeedVector.getY() * 5d
							);

			double dA = speedPoint.distanceSq( mainAxis.pA );
			double dB = speedPoint.distanceSq( mainAxis.pB );

			if ( dB < dA )
			{
				mainAxis.swap();
			}
			return true;

		}

		// If fail,
		// Find AHEAD with feature detection ( ear and nose detection ).

		{
			if ( earComponentList.size() == 2 && frontPoint == null ) // We don't use this method if the head is already found.
			{
				// check if they are on the right side.
				Point2D e1 = getBooleanMask2DMassCenter( earComponentList.get( 0 ) );

				double de1A = e1.distanceSq( mainAxis.pA );
				double de1B = e1.distanceSq( mainAxis.pB );

				if ( de1B < de1A )
				{
					mainAxis.swap();
				}
				System.out.println("[MouseDetection] Find head location with ears");
				return true;
			}
		}
*/
//		return false;

		//System.out.println("Find head post process");
		//if ( nosePoint != null ) return; // the nose is already found.

		// if clues about ear/nose/eye is known, don't process.
		//if ( eyesNoseEarComponentList.size() > 1 ) return;

		// try to find with the previous one.

//		MouseDetection previousDetection = trackSegment.getDetection( t-1 );



//		if ( previousDetection == null ) return;



		//		{	// Watch the difference between current and previous angle.
		//			double difAngleInT = Math.atan2(
		//					Math.sin( this.angle- previousDetection.angle ),
		//					Math.cos( this.angle - previousDetection.angle )) ;
		//			System.out.println(
		//					"prev: " + previousDetection.angle
		//					+ "current: " + this.angle
		//					+  " dif angle in T: " + difAngleInT );
		//		}


//		if ( previousDetection.nosePoint == null ) return;
//
//		Point2D p1 = new Point2D.Double( massCenter.getX() + Math.cos( angle ) * longAxis /2d ,
//				massCenter.getY() + Math.sin( angle ) * longAxis /2d
//				);
//		Point2D p2 = new Point2D.Double( massCenter.getX() - Math.cos( angle ) * longAxis / 2d ,
//				massCenter.getY() - Math.sin( angle ) * longAxis / 2d
//				);
//
//		double distP1N = p1.distanceSq( previousDetection.nosePoint.toPoint2D() );
//		double distP2N = p2.distanceSq( previousDetection.nosePoint.toPoint2D() );
//
//		if ( distP1N < distP2N )
//		{
//			nosePoint = new Point3D.Double( p1.getX(), p1.getY(), 0 );
//			tailPoint = new Point3D.Double( p2.getX(), p2.getY(), 0 );
//		}else
//		{
//			nosePoint = new Point3D.Double( p2.getX(), p2.getY(), 0 );
//			tailPoint = new Point3D.Double( p1.getX(), p1.getY(), 0 );
//		}





	}

	/**
	 * return score for same way and opposite way.
	 * */
	private double[] getCorrelationScores(IcyBufferedImage patch1, IcyBufferedImage patch2 ) {
		double scores[] = new double[2];

		short[] shortData1 = patch1.getDataXYAsShort( 0 );
		short[] shortData2 = patch2.getDataXYAsShort( 0 );

		double corrSameWay = MathUtil.correlation( shortData1, shortData2, patch1.getWidth(), false );

		double corrOppositeWay = MathUtil.correlation( shortData1, shortData2, patch1.getWidth(), true );

		System.out.println("Corr same way: " + corrSameWay + " / " + corrOppositeWay );

		scores[0] = corrSameWay;
		scores[1] = corrOppositeWay;

		return scores;
	}

	class HeadComputation
	{
		double scalarMainAxis_InstantSpeedM;
		double scalarMainAxis_InstantSpeedA;
		double scalarMainAxis_InstantSpeedB;

		double projectedSpeedMassCenter;
		double projectedSpeedA;
		double projectedSpeedB;
	}

	HeadComputation getA_B_MC_Vectors( MouseDetection previousDetection , MouseDetection detection )
	{
		HeadComputation hc = new HeadComputation();

		Point2D pASpeedVector = Util.createVector( previousDetection.mainAxis.pA, detection.mainAxis.pA );
		Point2D pBSpeedVector = Util.createVector( previousDetection.mainAxis.pB, detection.mainAxis.pB );

		Point2D mainAxisVector = Util.createVector( detection.mainAxis.pB , detection.mainAxis.pA );

		hc.scalarMainAxis_InstantSpeedM = Util.getScalarProduct( mainAxisVector , detection.getInstantSpeedVector() );
		hc.scalarMainAxis_InstantSpeedA = Util.getScalarProduct( mainAxisVector , pASpeedVector );
		hc.scalarMainAxis_InstantSpeedB = Util.getScalarProduct( mainAxisVector , pBSpeedVector );

		Point2D projectedSpeedVectorMassCenter = Util.project( new Point2D.Double(0,0) , mainAxisVector , detection.instantSpeedVector ) ;
		Point2D projectedSpeedVectorA = Util.project( new Point2D.Double(0,0) , mainAxisVector, pASpeedVector );
		Point2D projectedSpeedVectorB = Util.project( new Point2D.Double(0,0) , mainAxisVector, pBSpeedVector );

		hc.projectedSpeedMassCenter = projectedSpeedVectorMassCenter.distance( 0 , 0 );
		hc.projectedSpeedA = projectedSpeedVectorA.distance( 0 , 0 );
		hc.projectedSpeedB = projectedSpeedVectorB.distance( 0 , 0 );

		return hc;
	}

	/**
	 *
	 * @param previousDetection
	 * @param detection
	 * @return true if the head location can be computed thanks to the speed of the animal
	 */
	private boolean testA_B_MC_Speed_ForHeadLocation(
			MouseDetection previousDetection , MouseDetection detection,
			TrackSegment trackSegment
			) {

//		MouseDetection previousDetection = trackSegment.getDetection( t-1 );
		if ( previousDetection == null ) return false;

		HeadComputation hc = getA_B_MC_Vectors( previousDetection , detection );
		float MIN_SPEED = 2; // 0.1f;//3

//		String name = "in";


		// Check learning

		// If learning is not ready yet,
		// Check speed of all points of the mouse.

//		double pASpeed = previousDetection.mainAxis.pA.distance( mainAxis.pA );
//		double pBSpeed = previousDetection.mainAxis.pB.distance( mainAxis.pB );

//		Point2D pASpeedVector = Util.createVector( previousDetection.mainAxis.pA, detection.mainAxis.pA );
//		Point2D pBSpeedVector = Util.createVector( previousDetection.mainAxis.pB, detection.mainAxis.pB );
//
//		Point2D mainAxisVector = Util.createVector( detection.mainAxis.pB , detection.mainAxis.pA );

		// FIXME: vectorProjectRatio seems to give opposite result as expected :/
//		double ratio = Util.vectorProjectRatio( new Point2D.Double(0,0), mainAxisVector , getInstantSpeedVector() );

//		double scalarMainAxis_InstantSpeedM = Util.getScalarProduct( mainAxisVector , detection.getInstantSpeedVector() );
//		double scalarMainAxis_InstantSpeedA = Util.getScalarProduct( mainAxisVector , pASpeedVector );
//		double scalarMainAxis_InstantSpeedB = Util.getScalarProduct( mainAxisVector , pBSpeedVector );

//		ROI2DLine lineMainAxis = new ROI2DLine(
//				(int)mainAxis.pB.getX(),
//				(int)mainAxis.pB.getY(),
//				(int)(mainAxis.pB.getX() + mainAxisVector.getX() ),
//				(int)(mainAxis.pB.getY() + mainAxisVector.getY() ) );
//		lineMainAxis.setName( "tmp vector M" );
//		lineMainAxis.setColor( Color.yellow );
//		LiveMouseTracker.addROIToInfraSequence( lineMainAxis );
//
//		ROI2DLine lineA = new ROI2DLine(
//				(int)mainAxis.pA.getX(),
//				(int)mainAxis.pA.getY(),
//				(int)(mainAxis.pA.getX() + pASpeedVector.getX()  ),
//				(int)(mainAxis.pA.getY() + pASpeedVector.getY()  ) );
//		lineA.setName( "tmp vector A" );
//		lineA.setColor( Color.red );
//		LiveMouseTracker.addROIToInfraSequence( lineA );
//
//		ROI2DLine lineB = new ROI2DLine(
//				(int)mainAxis.pB.getX(),
//				(int)mainAxis.pB.getY(),
//				(int)(mainAxis.pB.getX() + pBSpeedVector.getX()  ),
//				(int)(mainAxis.pB.getY() + pBSpeedVector.getY()  ) );
//		lineB.setName( "tmp vector B " );
//		lineB.setShowName( true );
//		lineB.setColor( Color.green );
//		LiveMouseTracker.addROIToInfraSequence( lineB );

//		double projectedSpeedM = Math.sqrt( Math.abs( Util.getScalarProduct( mainAxisVector, instantSpeedVector ) ) );
//		double projectedSpeedA = Math.sqrt( Math.abs( Util.getScalarProduct( mainAxisVector, pASpeedVector ) ) );
//		double projectedSpeedB = Math.sqrt( Math.abs( Util.getScalarProduct( mainAxisVector, pBSpeedVector ) ) );

//		Point2D projectedSpeedVectorMassCenter = Util.project( new Point2D.Double(0,0) , mainAxisVector , detection.instantSpeedVector ) ;
//		Point2D projectedSpeedVectorA = Util.project( new Point2D.Double(0,0) , mainAxisVector, pASpeedVector );
//		Point2D projectedSpeedVectorB = Util.project( new Point2D.Double(0,0) , mainAxisVector, pBSpeedVector );
//
//		double projectedSpeedMassCenter = projectedSpeedVectorMassCenter.distance( 0 , 0 );
//		double projectedSpeedA = projectedSpeedVectorA.distance( 0 , 0 );
//		double projectedSpeedB = projectedSpeedVectorB.distance( 0 , 0 );


//		ROI2DLine linePMassCenter = new ROI2DLine(
//				(int)getMassCenter().getX(),
//				(int)getMassCenter().getY(),
//				(int)(getMassCenter().getX() + projectedSpeedVectorMassCenter.getX() ),
//				(int)(getMassCenter().getY() + projectedSpeedVectorMassCenter.getY() ) );
//		linePMassCenter.setName( "tmp vector PM" );
//		linePMassCenter.setColor( Color.yellow );
//		LiveMouseTracker.addROIToInfraSequence( linePMassCenter );
//
//		ROI2DLine linePA = new ROI2DLine(
//				(int)mainAxis.pA.getX(),
//				(int)mainAxis.pA.getY(),
//				(int)(mainAxis.pA.getX() + projectedSpeedVectorA.getX()  ),
//				(int)(mainAxis.pA.getY() + projectedSpeedVectorA.getY()  ) );
//		linePA.setName( "tmp vector PA" );
//		linePA.setColor( Color.red );
//		LiveMouseTracker.addROIToInfraSequence( linePA );
//
//		ROI2DLine linePB = new ROI2DLine(
//				(int)mainAxis.pB.getX(),
//				(int)mainAxis.pB.getY(),
//				(int)(mainAxis.pB.getX() + projectedSpeedVectorB.getX()  ),
//				(int)(mainAxis.pB.getY() + projectedSpeedVectorB.getY() ) );
//		linePB.setName( "tmp vector PB " );
//		linePB.setShowName( true );
//		linePB.setColor( Color.green );
//		LiveMouseTracker.addROIToInfraSequence( linePB );


//		System.out.println("[HEAD/TAIL PostProcess] M:" + projectedSpeedMassCenter + " A:" + projectedSpeedA + " B:" + projectedSpeedB );

		if ( Util.isSameSign( hc.scalarMainAxis_InstantSpeedM, hc.scalarMainAxis_InstantSpeedA , hc.scalarMainAxis_InstantSpeedB ) // Check if all vector are heading the same direction.
//				&&
//				Util.isSameSign( hc.scalarMainAxis_InstantSpeedA, hc.scalarMainAxis_InstantSpeedB )
				)
		{
//			name="Sign";
			if ( hc.projectedSpeedMassCenter > MIN_SPEED && hc.projectedSpeedA > MIN_SPEED && hc.projectedSpeedB > MIN_SPEED ) // if all MC,A,B are fast enough
			{
//				name="SpOk";
				if ( hc.scalarMainAxis_InstantSpeedM >= 0 ) // correct way.
				{
//					System.out.println("[HEAD/TAIL PostProcess] HEAD CORRECT WAY FOUND");
//					trackSegment.setOrientationAffectedBy("SC SPEED");
//					LiveMouseTracker.addEvent( new Event( "HC OK", Color.white, this.getMassCenter().toPoint2D() ));

					return true;
				}

				if ( hc.scalarMainAxis_InstantSpeedM < 0 ) // opposite way
				{
					//				int MIN_SPEED = 3;
					//				if ( getInstantSpeed() > MIN_SPEED
					//				&& pASpeedVector.distance( 0, 0 ) > MIN_SPEED
					//				&& pBSpeedVector.distance( 0, 0 ) > MIN_SPEED )
//					if ( projectedSpeedMassCenter > MIN_SPEED && projectedSpeedA > MIN_SPEED && projectedSpeedB > MIN_SPEED )
//					{
//					System.out.println("[HEAD/TAIL PostProcess] SWAP");

//					if( !isBuiltByDetectionSplitter() ) // not allowed during split
					{
						mainAxis.swap();
//						LiveMouseTracker.addEvent( new Event( "swap speed", Color.orange, this.getMassCenter().toPoint2D() ));
					}
//					LiveMouseTracker.addEvent( new Event( "HC SWAP", Color.white, this.getMassCenter().toPoint2D() ));

					return true;
//					}
				}
			}
		}
//		LiveMouseTracker.addEvent( new Event( name, Color.white, this.getMassCenter().toPoint2D() ));

		return false;

	}


	private void buildSpine() {

		if ( backPoint == null || frontPoint == null ) return;

		// using scale considering the animal at the bottom of the cage. 10cm = 57px;
		spine.lengthInMM = mainAxis.pA.distance( mainAxis.pB ) * 100 / 57;

		Point2D point = new Point2D.Double( backPoint.getX() , backPoint.getY() );
		Point2D vector = new Point2D.Double(
				(frontPoint.getX() - backPoint.getX()) / (double) Spine.NB_SPINE_POINT ,
				(frontPoint.getY() - backPoint.getY()) / (double) Spine.NB_SPINE_POINT
				);

		for ( int i = 0 ; i < Spine.NB_SPINE_POINT ; i++ )
		{
			point.setLocation( backPoint.getX()+ (float)i* vector.getX() , backPoint.getY() + (float)i * vector.getY() );
			spine.z[i] = Util.getMeanZ( point , 2 , mouseROI );
		}

		// add remarquable points.
		{
			for ( BooleanMask2D mask : earComponentList )
			{
				//			Point2D projected = Util.project( tailPoint.toPoint2D(), nosePoint.toPoint2D(), getBooleanMask2DMassCenter( mask ) );
				double ratio = Util.vectorProjectRatio( backPoint.toPoint2D(), frontPoint.toPoint2D(), getBooleanMask2DMassCenter( mask ) );
				if ( ratio > 0.9 )
				{
					lookingDown = true;
				}

				int index = (int) ( ratio * (Spine.NB_SPINE_POINT -1) );
				if ( index > Spine.NB_SPINE_POINT - 1 ) index = Spine.NB_SPINE_POINT -1;
				if ( index < 0 ) index = 0;
				spine.spineSpecialPointList.add( new SpineSpecialPoint( index , 0 , Color.red ) );
			}
			for ( BooleanMask2D mask : noseComponentList )
			{
				//			Point2D projected = Util.project( tailPoint.toPoint2D(), nosePoint.toPoint2D(), getBooleanMask2DMassCenter( mask ) );
				double ratio = Util.vectorProjectRatio( backPoint.toPoint2D(), frontPoint.toPoint2D(), getBooleanMask2DMassCenter( mask ) );
				int index = (int) ( ratio * (Spine.NB_SPINE_POINT -1) );
				if ( index > Spine.NB_SPINE_POINT - 1 ) index = Spine.NB_SPINE_POINT -1;
				if ( index < 0 ) index = 0;
				spine.spineSpecialPointList.add( new SpineSpecialPoint( index , 0 , Color.green ) );
				lookingUp = true;
			}
		}
	}




	private void findMinorAndMajorAxis() {

		Moment moment = new Moment(
				mouseROIMask, LiveMouseTracker.infraImage );

		angle = moment.aoipar.theta;
		longAxis = moment.aoipar.longAxis;
		shorterAxis = moment.aoipar.shorterAxis;

		mainAxis.pA = new Point2D.Double( massCenter.getX() + Math.cos( angle ) * longAxis /2d ,
				massCenter.getY() + Math.sin( angle ) * longAxis /2d
				);
		mainAxis.pB = new Point2D.Double( massCenter.getX() - Math.cos( angle ) * longAxis / 2d ,
				massCenter.getY() - Math.sin( angle ) * longAxis / 2d
				);

	}

	private void findTail(ArrayList<BooleanMask2D> tailCandidateArrayList , boolean postProcess ) {

		if ( tailCandidateArrayList == null ) return;
		if ( backPoint == null ) return;

		double bestDist = Double.MAX_VALUE;
		BooleanMask2D bestTail = null;

		for ( BooleanMask2D tailCandidate : tailCandidateArrayList )
		{
			double distance = BooleanMaskUtil.getMinDistanceToContour( backPoint.toPoint2D() , tailCandidate );
			if ( distance < bestDist )
			{
				bestDist = distance;
				bestTail = tailCandidate;
			}

		}

		if ( bestDist < 40 )
		{
			tail = new Tail( backPoint.toPoint2D() ,
					getCenterOfMassTailVector() );
			endTailPoint = new Point2D.Double( bestTail.bounds.getCenterX() , bestTail.bounds.getCenterY() );
			ROI2DLine lineROI = new ROI2DLine( endTailPoint, backPoint.toPoint2D() );
			lineROI.setName("tmp tail " );
			lineROI.setColor( Color.PINK );
			if ( postProcess )
			{
				lineROI.setColor( Color.RED );
			}
			LiveMouseTracker.addROIToInfraSequence( lineROI );
			tailMask = bestTail;
		}


	}

	private void fitTail(TrackSegment trackSegment) {

		if ( LiveMouseTracker.TAIL_TRACKING_ASSOCIATION_TO_DETECTION_ENABLED )
		{
			MouseDetection previousDetection = trackSegment.getDetection( t-1 );
			if ( previousDetection == null ) return;

			if ( previousDetection.tail != null )
			{
				this.tail = previousDetection.tail.getCopy();
				tail.shift( backPoint.toPoint2D() , getCenterOfMassTailVector() );
			}

			if ( this.tail == null )
			{
				findTail( LiveMouseTracker.tailCandidateArrayList , true );
			}

			if ( tail != null )
			{
				if ( LiveMouseTracker.TAIL_FIT_ENABLED )
				{
					tail.fitToMask( tailMask );
				}
			}
		}

	}

	private Point2D getBooleanMask2DMassCenter(BooleanMask2D mask) {
		double x = 0;
		double y = 0;
		long nbPoint = 0L;
		for ( Point p : mask.getPoints() )
		{
			x+= p.getX();
			y+= p.getY();
			nbPoint++;
		}

		if ( nbPoint == 0L ) return null;

		return new Point2D.Double( x / (double)nbPoint , y / (double)nbPoint );

	}

	Point2D getCenterOfMassTailVector()
	{
		return new Point2D.Double( backPoint.getX() - massCenter.getX() , backPoint.getY() - massCenter.getY() );
	}

	public float[] getHistogram() {
		return histogram;
	}

	public IcyBufferedImage getHistogramImage() {
		return histogramImage;
	}

	public Point3D getMassCenter() {
		return massCenter;
	}

	public double getMaxInfraIntensity() {
		return maxInfraIntensity;
	}

	public double getMinInfraIntensity() {
		return minInfraIntensity;
	}

	public double getMeanDepth() {
		return meanDepth;
	}

	public double getMeanInfraIntensity() {
		return meanInfraIntensity;
	}

	public Point3D getFrontPoint() {
		return frontPoint;
	}

	public ROI2DArea getROI2DArea() {
		return mouseROI;
	}

	public BooleanMask2D getBooleanMask() {
		return mouseROIMask;
	}

	public double getSurface() {
		return surface;
	}

	public int getT() {
		return t;
	}

	public Point3D getBackPoint() {
		return backPoint;
	}

	private Point2D getVector(MouseDetection previous,
			MouseDetection next) {

		return new Point2D.Double(
				next.getMassCenter().getX() - previous.getMassCenter().getX(),
				next.getMassCenter().getY() - previous.getMassCenter().getY() );

	}

	public double getVolume() {
		return volume;
	}

	public boolean isRearing()
	{
		if ( frontPoint == null ) return false;
		if ( backPoint == null ) return false;

		double bodySlope = frontPoint.getZ() - backPoint.getZ();
		int BODY_SLOPE_THRESHOLD = 40;

		if ( Math.abs( bodySlope ) > BODY_SLOPE_THRESHOLD )
			return false;

		return true;

		// opd code
//		if ( frontPoint == null ) return false;
//		return ( frontPoint.getZ() > 50 );



	}

	public void paintNose( Graphics2D g, IcyCanvas canvas )
	{
		if ( frontPoint != null )
		{
			g.setColor( Color.white );
			Ellipse2D ellipse = new Ellipse2D.Double( frontPoint.getX() -2d, frontPoint.getY() -2d, 5d,5d ) ;
			g.draw( ellipse );
		}
	}

	public void paint( Graphics2D g, IcyCanvas canvas )
	{
		// draw nose point
//		System.out.println("paint frontPoint: " + frontPoint );
//		System.out.println("graphics: " + g );
		paintNose(g, canvas);


//		Ellipse2D ellipse2 = new Ellipse2D.Double( getMassCenter().getX() -2d, getMassCenter().getY() -2d, 5d,5d ) ;
//		g.draw( ellipse2 );

		// head location test purposes
		{
//			Animal animal = LiveMouseTracker.getMainAnimalPool().getAnimalWithDetection( this );
//			try
//			{
//				g.setColor( animal.getColor() );
//				int nbInstance = animal.getMachineLearningSubPartDataSet().numInstances();
//				int minInstance = LiveMouseTracker.LEARNING_NB_DETECTION_FOR_LEARNING_SUBPARTS_PER_ANIMAL;
//				g.drawString( ""+nbInstance+"/"+minInstance , (int)getMassCenter().getX()-50, (int)getMassCenter().getY() );
//				g.drawString(
//						" h:"+(int)(pAEqualA*100d) +
//						" b: "+ (int)(pBEqualB*100d), (int)getMassCenter().getX()-50, (int)getMassCenter().getY()+10 );
//			}catch( Exception e){
//				pAEqualA = 0d;
//				pBEqualB = 0d;
//			}
		}

//		private Double pBEqualB;
//		private Double pAEqualA;

		if ( tail != null )
		{
			tail.paint( g );
		}

//		int mx = (int)getMassCenter().getX();
//		int my = (int)getMassCenter().getY();

//		if ( isBuiltByDetectionSplitter() )
//		{
//			Ellipse2D ellipse = new Ellipse2D.Double(
//					massCenter.getX() - 20,
//					massCenter.getY() - 20,
//					41, 41 ) ;
//			g.setColor( Color.yellow );
//			g.draw( ellipse );
//		}

		// draw instant speed vector
		if ( instantSpeedVector != null )
		{
//			g.setColor( Color.white );
//			g.drawLine( mx, my,
//					mx+ (int)(instantSpeedVector.getX()*10), my+ (int)(instantSpeedVector.getY()*10) );
//
//			if ( getInstantSpeed() !=null )
//			{
//				if ( getInstantSpeed() > LiveMouseTracker.MIN_INSTANT_SPEED_FOR_SUB_PART_CALCULATION )
//				{
//					g.setColor( Color.green );
//				}
//			}
//
//			g.drawString( "InstantSpeed: " + Math.round( getInstantSpeed() ), mx+20, my+20 );

		}

		// draw A and B points.
		if ( false )
		{
			if ( mainAxis.pA != null )
			{
				g.setColor( Color.red );
				Ellipse2D ellipse = new Ellipse2D.Double( mainAxis.pA.getX() -3d, mainAxis.pA.getY() -3d, 7d,7d ) ;
				g.draw( ellipse );
			}
			if ( mainAxis.pB != null )
			{
				g.setColor( Color.green );
				Ellipse2D ellipse = new Ellipse2D.Double( mainAxis.pB.getX() -3d, mainAxis.pB.getY() -3d, 7d,7d ) ;
				g.draw( ellipse );
			}
		}


		if ( mouseROI != null ) // draw number of pixel of detection ( area ).
		{
//			g.setColor( Color.green );
//			g.drawString( "Area: " + (int)mouseROI.getArea(), mx+20, my+20 );
		}

//		int mx = (int) massCenter.getX();
//		int my = (int) massCenter.getY();

		if ( isRearing() )
		{
//						g.setColor( Color.white );
//						g.drawString("rearing", mx -30 , my - 40 );
		}

		{ // Draw Z profile Graph
//			g.setColor( Color.white );
//						g.drawString("tZ: " + (int)tailPointZ, (int)getMassCenter().getX() , (int)getMassCenter().getY() );
//						g.drawString("mZ: " + (int)massCenterZ, (int)getMassCenter().getX() , (int)getMassCenter().getY()+10 );
//						g.drawString("hZ: " + (int)nosePointZ, (int)getMassCenter().getX() , (int)getMassCenter().getY()+20 );
//						g.drawString("Vol: " + (int)volume, (int)getMassCenter().getX() , (int)getMassCenter().getY()+30 );
//						g.drawString("Area: " + (int)surface, (int)getMassCenter().getX() , (int)getMassCenter().getY()+40 );

			// draw 3D shape
			{
//							g.drawLine( mx - 30 , my , mx - 10 , my );
//							g.drawLine( mx - 30 , my - 25 , mx - 10 , my - 25 ); // rearing threshold
//
//							GeneralPath path = new GeneralPath();
//							path.moveTo( mx - 30 , my - tailPointZ /2);
//							path.lineTo( mx - 20 , my - massCenterZ /2);
//							path.lineTo( mx - 10 , my - nosePointZ /2);
//							g.draw( path );
//
//							{
//								g.setColor( Color.BLACK );
//								Ellipse2D ellipse = new Ellipse2D.Double( mx - 30 -2, my - tailPointZ /2 -2d, 5d,5d ) ;
//								g.draw( ellipse );
//							}
//
//							{
//								g.setColor( Color.RED );
//								Ellipse2D ellipse = new Ellipse2D.Double( mx - 10 -2, my - nosePointZ /2 -2d, 5d,5d ) ;
//								g.draw( ellipse );
//							}
			}
		}


		// draw tail point
		/*
		if ( backPoint != null )
		{
			Ellipse2D ellipse = new Ellipse2D.Double( backPoint.getX() -1d, backPoint.getY() -1d, 3d,3d ) ;
			g.setColor( Color.black );
			g.draw( ellipse );
		}
		*/

		// draw main axis
		if ( LiveMouseTracker.DRAW_DETECTION_AXIS )
		{
			// draw Main axis
			{
				Line2D line = new Line2D.Double(
						massCenter.getX() - Math.cos( angle ) * longAxis /2d,
						massCenter.getY() - Math.sin( angle ) * longAxis /2d,
						massCenter.getX() + Math.cos( angle ) * longAxis /2d,
						massCenter.getY() + Math.sin( angle ) * longAxis /2d
						) ;

				g.setColor( Color.red );
				g.draw( line );
			}
			// draw short axis
			{
				Line2D line = new Line2D.Double(
						massCenter.getX() - Math.cos( angle+Math.PI/2d ) * shorterAxis /2d,
						massCenter.getY() - Math.sin( angle+Math.PI/2d ) * shorterAxis /2d,
						massCenter.getX() + Math.cos( angle+Math.PI/2d ) * shorterAxis /2d,
						massCenter.getY() + Math.sin( angle+Math.PI/2d ) * shorterAxis /2d
						) ;


				g.setColor( Color.yellow );
				g.draw( line );
			}
		}

		// draw eyes, noses and ear ROI masks
		if ( LiveMouseTracker.DRAW_DETECTION_EYES_NOSE_EAR_ROIS )
		{
			for ( BooleanMask2D mask : discriminantPartsComponentList )
			{
				if ( earComponentList.contains(
						mask
						) ) continue;
				if ( noseComponentList.contains( mask ) ) continue;

				ROI2DAreaX roi = new ROI2DAreaX( mask );
				roi.setColor( Color.orange );
				roi.getPainter().paint( g , null, canvas );
				//g.draw( roi );
				//g.draw( mask );
			}

			for ( BooleanMask2D mask : earComponentList )
			{
				ROI2DAreaX roi = new ROI2DAreaX( mask );
				roi.setColor( Color.red );
				roi.getPainter().paint( g , null, canvas );
			}

			for ( BooleanMask2D mask : noseComponentList )
			{
				ROI2DAreaX roi = new ROI2DAreaX( mask );
				roi.setColor( Color.green );
				roi.getPainter().paint( g , null, canvas );
			}

			// test: project ear on mainaxis
			if ( backPoint != null && frontPoint != null )
			{
				for ( BooleanMask2D mask : earComponentList )
				{
					Point2D projected = Util.project( backPoint.toPoint2D(), frontPoint.toPoint2D(), getBooleanMask2DMassCenter( mask ) );

					Ellipse2D ellipse = new Ellipse2D.Double( projected.getX() -1d, projected.getY() -1d, 3d,3d ) ;
					g.setColor( Color.yellow );
					g.draw( ellipse );

				}
			}

		}

		// Draw all lines between parts.
		if ( false )
		for ( BooleanMask2D comp1 : discriminantPartsComponentList  )
		{
			for ( BooleanMask2D comp2 : discriminantPartsComponentList  )
			{
				if ( comp1 != comp2 )
				{
					Point2D m1 = getBooleanMask2DMassCenter( comp1 );
					Point2D m2 = getBooleanMask2DMassCenter( comp2 );
					double distance = m1.distance( m2 );

					if ( distance < 17 && distance > 6 )
					{
						Line2D line = new Line2D.Double( m1, m2 );
						g.setColor( Color.pink );
						g.draw( line );
						//ROI2DLine line = new ROI2DLine( m1 , m2 );
					}
				}
			}
		}

		//		g.drawLine( (int)massCenter.getX() , (int)massCenter.getY() ,
		//				(int)massCenter.getX() +100, (int)massCenter.getY() +100 );

	}

	public boolean isPostProcessed()
	{
		return postProcessed;
	}

	/**
	 * Once the detection process is performed, when more info are available (like the track in which is
	 * the detection, the post process allow to find more infos like head location)
	 *
	 * No guarantee those optional data are ready !
	 *
	 * Postprocessed is computed once, even in case of multiple calls
	 * */
	public void postProcess( TrackSegment trackSegment )
	{
		if ( ! postProcessed )
		{

			if ( getT() != LiveMouseTracker.getT() )
			{
				System.err.println("Warning: inconsistant postProcessing detection: detT="+getT() +" and LiveT=" + LiveMouseTracker.getT() );
			}
			// computes instant speed vector
			computeInstantSpeedVector( trackSegment );

			// test if the movement is so slow that we don't need to learn.
			{
				Double speed = getInstantSpeed();
				if ( speed != null )
				{
					trackSegment.setOrientationAffectedBy(""+(int)(speed*10f));
					if ( speed < 0.4 ) // 0.3 before
					{
						setCanBeUsedForLearning( false );
					}
				}
			}

			//findAheadPostProcess( trackSegment );
			//fitTail( trackSegment );

			// Track major axis by simple distance change
			trackMajorAxis( trackSegment );

			// affect head if only head was found before (prolongates)
			trackHeadAndTailPostProcess( trackSegment );


			// FIXME: rearing test de activated
			// The rearing test WAS blocking everything the rearing was I guess not set already when we pass here (to check)
//			if ( !isRearing() ) // if the animal is rearing, the head/tail location should not be recomputed
//			if ( !isBuiltByDetectionSplitter() ) // not allowed during split


			// Head process:
			// If speed > threshold > head affected
			// If pattern switch > head affected


			// Check head tail with speed
			{
				boolean canHeadBeComputedOnlyWithAnimalSpeed = testA_B_MC_Speed_ForHeadLocation(
						trackSegment.getDetection( t-1 ) , this, trackSegment );
				if ( canHeadBeComputedOnlyWithAnimalSpeed )
				{
					affectNoseAndTailPoint();
				}
			}

			checkHeadTailWithMachineLearning( trackSegment ) ;



//			if ( findAheadWithPatternCorrelation( trackSegment ) )
//			{
//				affectNoseAndTailPoint();
//			}

/*
			if ( frontPoint == null ) // decide arbitrary of a frontpoint. ( TEST PURPOSES should be replaced by speed init )
			{
				LiveMouseTracker.addEvent( new Event( "AFFECT FRONT", Color.red, this.getMassCenter().toPoint2D() ));
				affectNoseAndTailPoint();
			}
*/

			buildFrontPointZ();
			buildBackPointZ();
			buildSpine();

			//checkHeadTailWithMachineLearning( trackSegment );

			/*
			// clean heavy data from previous detection
			MouseDetection previousDetection = trackSegment.getDetection( t-1 );
			if ( previousDetection != null )
			{
				previousDetection.infraPatch = null;
			}*/

			postProcessed = true;
		}

	}

	public void cleanHeavyData()
	{
		infraPatch = null;
	}


	private void checkHeadTailWithMachineLearning( TrackSegment trackSegment ) {

		if ( LiveMouseTracker.HEAD_TAIL_MACHINE_LEARNING )
		{
			ArrayList<SubPartDescriptor> tmpSubPartDescriptor = buildSubPartsDescriptors();

			if ( frontPoint != null && backPoint !=null )
			{
				// if nose and tailPoint are known, then the mainAxis.A and B are nose and tail respectively.
				// so we can just affect the descriptor to the animal for further dictionary enrichment.
				this.subPartDescriptorList = tmpSubPartDescriptor;
			}

			Double resultA[] = checkSubPart( trackSegment, tmpSubPartDescriptor.get( 0 ) , null , false ); // check subPart A.
			Double resultB[] = checkSubPart( trackSegment, tmpSubPartDescriptor.get( 1 ) , null , false ); // check subPart B.

			try // FIXME: TEMP CODE FOR TEST HEAD PROCESS !!
			{
				pAEqualHead = resultA[0]; // before was pAEqualA (as A standed for Ahead)
				pBEqualBack = resultB[1]; // before was pBEqualB (as A standed for Ahead)
			}catch( Exception e){
				//System.err.println("can't build stats for head/tail choice.");
				}

			if ( resultA != null ) // will work only if machine learning is ready.
			{
				//	System.out.println("test: p(a=a)" + resultA[0] + " p(b=b): " + resultB[1] );
				this.subPartDescriptorList = tmpSubPartDescriptor; // we load the subPartAnyWay.
				//				if ( resultA[0] < 0.2 )

//				if ( resultA[0] * resultB[1] <
//						LiveMouseTracker.HEAD_ML_SWAP_THRESHOLD * LiveMouseTracker.HEAD_ML_SWAP_THRESHOLD
//						//0.15
//						) // we reverse if needed.

				// we reverse if needed.
				if ( resultA[0] < LiveMouseTracker.HEAD_ML_SWAP_THRESHOLD &&
						resultB[1] < LiveMouseTracker.HEAD_ML_SWAP_THRESHOLD )
				{
					if( resultA[1] > LiveMouseTracker.HEAD_ML_SWAP_THRESHOLD &&
							resultB[0] > LiveMouseTracker.HEAD_ML_SWAP_THRESHOLD ) // 0.38
					{
						//System.out.println("*ML SWAP nose/tail");
						swapHeadTail();
						//LiveMouseTracker.addEvent( new Event( "MLSW "+(int)(resultA[1]*100f)+"/"+(int)(resultB[0]*100f), Color.white, this.getMassCenter().toPoint2D() ));

						trackSegment.setOrientationAffectedBy( "ML" );
//						MouseDetection lastDetection = this;
//						Event event = new Event( "ML SWAP HEAD", Color.yellow,
//								lastDetection.getMassCenter().toPoint2D() );
//						LiveMouseTracker.addEvent( event );

					}
				}
			}


			//			if ( result[0] < result[1] )
			//			{
			//				System.out.println("Proba well oriented (REVERSE ?)= " + Arrays.toString(result) );
			//				if ( result[0] < 0.2 )
			//				{
			//					swapHeadTail();
			//				}
			//			}
		}

	}

	private void affectNoseAndTailPoint() {
		frontPoint = new Point3D.Double( mainAxis.pA.getX(), mainAxis.pA.getY(), 0 );
		backPoint = new Point3D.Double( mainAxis.pB.getX(), mainAxis.pB.getY(), 0 );

		computeAngle();
	}

	private void computeAngle() {

		try
		{
			Point2D vector = Util.createVector( backPoint.toPoint2D(), frontPoint.toPoint2D() );

			angle = Math.atan2(
					vector.getY() ,
					vector.getX()
					);
		}catch(NullPointerException e)
		{
			System.out.println("ERROR: CANT'T COMPUTE ANGLE IN MOUSE DETECTION");
		}
	}

	/**
	 * If the head or tail has been found in previous detection,
	 * continue its assignation from the mainAxis.
	 * */
	private void trackHeadAndTailPostProcess( TrackSegment ts ) {

		MouseDetection previousDetection = ts.getDetection( t - 1 );
		if ( previousDetection == null ) return;

		if ( previousDetection.frontPoint != null )
		{
			affectNoseAndTailPoint();
		}
	}

	/** track point A and B. */
	private boolean trackMajorAxis( TrackSegment trackSegment ) {

		MouseDetection previousDetection = trackSegment.getDetection( t-1 );

		if ( previousDetection == null ) return false;

		double distAA = mainAxis.pA.distanceSq( previousDetection.mainAxis.pA );
		double distBA = mainAxis.pB.distanceSq( previousDetection.mainAxis.pA );

		if ( distAA > distBA )
		{
			mainAxis.swap();
			return true;
		}
		return false;

	}

	/** Check the classification of the subpart.
	 * for instance if the check part is really A, result should be [0.9,0.1]
	 * if the subpart is B, should be more [0.1,0.9].
	 * You can provide trackSegment or animal directly.
	 * */
	public static Double[] checkSubPart( TrackSegment trackSegment , SubPartDescriptor subPart , Animal animal , boolean force ) {

		if ( animal == null )
		{
			animal = LiveMouseTracker.getMainAnimalPool().getAnimalOwningTrack( trackSegment );
		}
		if ( animal == null ) return null;
		if ( animal.getMachineLearningSubPartsClassifier() == null ) return null;

		MachineLearningSubPartBuilder mlspb = new MachineLearningSubPartBuilder();
		Instance instance = mlspb.buildSubPartDetectionFeatures( subPart );

		if ( instance == null ) return null;

		// Check if the machine learning contains enough instance to be query.
		// (note that this machine learning is performed in LiveMouseTracker.refreshSubPartClassifier
		int numberOfInstance = animal.getMachineLearningSubPartDataSet().numInstances();


		if ( !force )
		{
			if ( numberOfInstance < LiveMouseTracker.MIN_TO_START_USING_MACHINE_LEARNING_NB_DETECTION_FOR_LEARNING_SUBPARTS_PER_ANIMAL ) return null;
		}

//		System.out.println("Checking animal orientation...");
		instance.setDataset( animal.getMachineLearningSubPartDataSet() );

		try {
			double[] result = animal.getMachineLearningSubPartsClassifier().distributionForInstance( instance );
			Double[] resultDouble = new Double[result.length];
			for ( int i = 0 ; i < result.length ; i ++ )
			{
				resultDouble[i] = result[i];
			}
			return resultDouble;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;

	}

	private void swapHeadTail() {
		System.out.println("SWAP ANIMAL ORIENTATION");
		mainAxis.swap();
		computeAngle();
		// reverse sub part descriptor List !!
		SubPartDescriptor subA = subPartDescriptorList.get( 0 );
		SubPartDescriptor subB = subPartDescriptorList.get( 1 );
		subPartDescriptorList.set( 0 , subB );
		subPartDescriptorList.set( 1 , subA );


		affectNoseAndTailPoint();
	}

	public void setMouseType(MouseType mouseType) {
		this.mouseType = mouseType;
	}

	BooleanMask2D thresholdInROI( IcyBufferedImage image , BooleanMask2D roiMask, int threshold )
//	BooleanMask2D thresholdInROI( IcyBufferedImage image , ROI2DArea maskROI , int threshold )
	{
		short[] imageBuffer = image.getDataXYAsShort( 0 );
		int width = image.getWidth();

//		BooleanMask2D mask = roiMask.getBooleanMask( true );

		boolean[] thresholdMask = new boolean[ roiMask.mask.length ];
		Rectangle bounds = roiMask.bounds;

		int minXMask = (int) bounds.getMinX();
		int minYMask = (int) bounds.getMinY();
		int maxXMask = (int) bounds.getMaxX();
		int maxYMask = (int) bounds.getMaxY();

		int offsetMask = 0;
		int indexImg = minXMask + (minYMask * width);
		for ( int yMask = minYMask ; yMask < maxYMask ; yMask++ )
		{
			for ( int xMask = minXMask, iImg = indexImg ; xMask < maxXMask ; xMask++, iImg++ )
			{
//				thresholdMask[ offsetMask ] = false;

				if ( roiMask.mask[offsetMask] ) // mask test
				{
					if ( imageBuffer[ iImg ] > threshold )
					{
						thresholdMask[ offsetMask ] = true;
					}
				}

				offsetMask++;
			}

			indexImg += width;
		}

		return new BooleanMask2D( bounds, thresholdMask );
	}

	@Override
	public String toString() {
		return "det t=" + getT() + " point:" + getMassCenter() ;
	}

	public Double getMeanIntensityForEar() {
		return meanIntensityForEar;
	}

	/**
	 * for tests !
	 * @deprecated
	 * @param front
	 * @param back
	 */
	public MouseDetection( int t , Point2D massCenter, Point2D front, Point2D back ) {
		this.t = t;
		this.frontPoint = new Point3D.Double( front.getX() , front.getY() , 0 );
		this.backPoint = new Point3D.Double( back.getX() , back.getY() , 0 );
		this.massCenter = new Point3D.Double( massCenter.getX() , massCenter.getY() , 0 );
		computeAngle();
//		Point2D vector = Util.createVector( backPoint.toPoint2D(), frontPoint.toPoint2D() );
//
//		angle = Math.atan2(
//				vector.getY() ,
//				vector.getX()
//				);

	}

	/** Build with the XML Data provided by getAsXMLData */
	public MouseDetection( String XMLdata ) {

//		System.out.println( XMLdata );

		// load ROI
		Document document = XMLUtil.getDocument( XMLdata );
		Node roiNode = XMLUtil.getChild( document.getDocumentElement(), "ROI" );

		ROI2DAreaX roi = new ROI2DAreaX();
		roi.loadFromXML( roiNode );

		mouseROI = roi;
		mouseROIMask = roi.getBooleanMask(true);
		mouseROIPoints = mouseROIMask.getPoints();

		// load various data
		Element elementData = XMLUtil.getElement( document.getDocumentElement(), "DATA" );
		this.t = XMLUtil.getAttributeIntValue( elementData, "t", -1 );

		{
			double x = XMLUtil.getAttributeDoubleValue( elementData, "mass_x", -1 );
			double y = XMLUtil.getAttributeDoubleValue( elementData, "mass_y", -1 );
			double z = XMLUtil.getAttributeDoubleValue( elementData, "mass_z", 0 );
			this.massCenter = new Point3D.Double( x, y, z );
		}

		{
			double x = XMLUtil.getAttributeDoubleValue( elementData, "front_x", -1 );
			double y = XMLUtil.getAttributeDoubleValue( elementData, "front_y", -1 );
			double z = XMLUtil.getAttributeDoubleValue( elementData, "front_z", 0 );
			this.frontPoint = new Point3D.Double( x, y, z );
		}

		{
			double x = XMLUtil.getAttributeDoubleValue( elementData, "back_x", -1 );
			double y = XMLUtil.getAttributeDoubleValue( elementData, "back_y", -1 );
			double z = XMLUtil.getAttributeDoubleValue( elementData, "back_z", 0 );
			this.backPoint = new Point3D.Double( x, y, z );
		}

		findMinorAndMajorAxis();

	}
	/** 1.212341 becomes 1.21 */
	private double lowPrecision(double x) {
		return (int)(Math.round(x * 100)) / 100d;
	}

	/**
	 * Return the detection as an XML string.
	 * */
	public String getAsXMLData( SQLiteSavedData saveLevel ) {

		 Document document = XMLUtil.createDocument(true);

		 Element elementData = XMLUtil.addElement( document.getDocumentElement(), "DATA");
		 XMLUtil.setAttributeDoubleValue( elementData, "mass_x", lowPrecision( getMassCenter().getX() ) );
		 XMLUtil.setAttributeDoubleValue( elementData, "mass_y", lowPrecision( getMassCenter().getY() ) );
		 XMLUtil.setAttributeDoubleValue( elementData, "mass_z", lowPrecision( getMassCenter().getZ() ) );

		 if ( getFrontPoint() != null )
		 {
			 XMLUtil.setAttributeDoubleValue( elementData, "front_x", lowPrecision( getFrontPoint().getX() ) );
			 XMLUtil.setAttributeDoubleValue( elementData, "front_y", lowPrecision( getFrontPoint().getY() ) );
			 XMLUtil.setAttributeDoubleValue( elementData, "front_z", lowPrecision( getFrontPoint().getZ() ) );
		 }

		 if ( getBackPoint() != null )
		 {
			 XMLUtil.setAttributeDoubleValue( elementData, "back_x", lowPrecision( getBackPoint().getX() ) );
			 XMLUtil.setAttributeDoubleValue( elementData, "back_y", lowPrecision( getBackPoint().getY() ) );
			 XMLUtil.setAttributeDoubleValue( elementData, "back_z", lowPrecision( getBackPoint().getZ() ) );
		 }

		 // save ears
		 BooleanMask2D ear1 = null;
		 try{
			 ear1 = earComponentList.get( 0 );
		 saveXMLBooleanMask ( elementData, ear1 , "ear1" );
		 if ( ear1 != null )
		 {
			 double ratio = Util.vectorProjectRatio(
					 backPoint.toPoint2D(),
					 frontPoint.toPoint2D(),
					 getBooleanMask2DMassCenter( ear1 ) );
			 XMLUtil.setAttributeDoubleValue( elementData, "ear1_ratio", lowPrecision( ratio ) );
		 }
		 } catch( Exception e) {};

		 BooleanMask2D ear2 = null;
		 try{
			 ear2 = earComponentList.get( 1 );
			 saveXMLBooleanMask ( elementData, ear2 , "ear2" );
			 if ( ear2 != null )
			 {
				 double ratio = Util.vectorProjectRatio(
						 backPoint.toPoint2D(), frontPoint.toPoint2D(), getBooleanMask2DMassCenter( ear2 ) );
				 XMLUtil.setAttributeDoubleValue( elementData, "ear2_ratio", lowPrecision( ratio ) );
			 }
		 } catch( Exception e) {};

		 BooleanMask2D nose = null;
		 try{
			 nose = noseComponentList.get( 0 );
			 saveXMLBooleanMask ( elementData, nose , "nose" );
			 if ( nose != null )
			 {
				 double ratio = Util.vectorProjectRatio(
						 backPoint.toPoint2D(), frontPoint.toPoint2D(), getBooleanMask2DMassCenter( nose ) );
				 XMLUtil.setAttributeDoubleValue( elementData, "nose_ratio", lowPrecision( ratio ) );
			 }
		 } catch( Exception e) {};

		 XMLUtil.setAttributeBooleanValue( elementData, "isRearing", isRearing() );
		 XMLUtil.setAttributeBooleanValue( elementData, "isLookingUp", isLookingUp() );
		 XMLUtil.setAttributeBooleanValue( elementData, "isLookingDown", isLookingDown() );

		 XMLUtil.setAttributeDoubleValue( elementData, "t", t );

		 if ( saveLevel == SQLiteSavedData.ALL )
		 {
			 // save ROI
			 Element element = XMLUtil.addElement( document.getDocumentElement(), "ROI");
			 mouseROI.saveToXML( element );
		 }
		 return Util.XMLDocumentToString( document );

	}



	private void saveXMLBooleanMask(Element elementData, BooleanMask2D mask, String string) {

		 if ( mask != null )
		 {
			 Point2D maskMass = getBooleanMask2DMassCenter( mask );
			 XMLUtil.setAttributeDoubleValue( elementData, string+"_x", lowPrecision( maskMass.getX() ) );
			 XMLUtil.setAttributeDoubleValue( elementData, string+"_y", lowPrecision( maskMass.getY() ) );
		 }

	}

	public void setBuiltByDetectionSplitter(boolean builtBySplitter ) {
		this.builtBySplitter = builtBySplitter;
		if ( builtBySplitter )
		{
			setCanBeUsedForLearning( false );
		}
	}

	/**
	 * If the detection has been build by the detection splitter, it means this detection had contact with other
	 * detection. Info should be used in tracking to ensure the tracking has not swap identities through the
	 * split detection process. So, out of the contact, individual should be rechecked.
	 * */
	public boolean isBuiltByDetectionSplitter() {
		return builtBySplitter;
	}

	/**
	 * This code only retreive part of the spine for split tests.
	 * @return
	 */
	public ArrayList<Point2D> getSpinePointsForSplit() {

		ArrayList<Point2D> pointList = new ArrayList<Point2D>();
		double nbPoint = 100;

		Point2D vector = new Point2D.Double(
				(mainAxis.pA.getX() - mainAxis.pB.getX()) / nbPoint ,
				(mainAxis.pA.getY() - mainAxis.pB.getY()) / nbPoint
				);

//		Point2D vector = new Point2D.Double(
//				(frontPoint.getX() - backPoint.getX()) / nbPoint ,
//				(frontPoint.getY() - backPoint.getY()) / nbPoint
//				);

		for ( int i = 10 ; i < nbPoint-10 ; i++ )
		//for ( int i = 8 ; i < nbPoint-8 ; i++ )
		{
//			pointList.add( new Point2D.Double(  backPoint.getX()+ (float)i* vector.getX() , backPoint.getY() + (float)i * vector.getY() ) );
			pointList.add( new Point2D.Double(
					mainAxis.pB.getX()+ (float)i* vector.getX() ,
					mainAxis.pB.getY() + (float)i * vector.getY() ) );
		}

		return pointList;
	}

	public MouseDetection getCopy( int forceT ) {

		MouseDetection copy = new MouseDetection( getAsXMLData( SQLiteSavedData.ALL ) );
		copy.t = forceT;
		copy.builtBySplitter = builtBySplitter;
		if ( frontPoint != null )
		{
			copy.frontPoint = (Point3D) frontPoint.clone();
		}
		if ( backPoint != null )
		{
			copy.backPoint = (Point3D) backPoint.clone();
		}
		copy.massCenter = (Point3D) massCenter.clone();
		copy.setCanBeUsedForLearning( false );
		copy.postProcessed = true;
		copy.mainAxis = mainAxis.getCopy();

		return copy;
	}

}
