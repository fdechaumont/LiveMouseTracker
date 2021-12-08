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
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import icy.image.IcyBufferedImage;
import icy.roi.BooleanMask2D;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import icy.system.profile.Chronometer;
import plugins.fab.livemousetracker.BackgroundHeightMapBuilder;
import plugins.fab.livemousetracker.FrameInfo;
import plugins.fab.livemousetracker.LiveMouseTracker;
import plugins.fab.livemousetracker.ROI2DAreaX;
import plugins.fab.livemousetracker.morpho.Moment;
import plugins.fab.livemousetracker.morpho.MorphoROITools;
import plugins.fab.livemousetracker.overlay.Event;
import plugins.kernel.roi.roi2d.ROI2DArea;

public class MouseDetector {

    Sequence backgroundSequence;
    Sequence infraOut;
    Sequence depthOut;
    BackgroundHeightMapBuilder backgroundHeightMapBuilder;
    ArrayList<ROI2DArea> tooBigSpuriousMaskList = new ArrayList<ROI2DArea>();

    public MouseDetector( Sequence backgroundSequence , Sequence infraOut, Sequence depthOut, BackgroundHeightMapBuilder backgroundHeightMapBuider ) {
        this.backgroundSequence = backgroundSequence;
        this.infraOut = infraOut;
        this.depthOut = depthOut;
        this.backgroundHeightMapBuilder = backgroundHeightMapBuider;

        System.out.println("[MouseDetector] Depth sensitivity is " + LiveMouseTracker.DEPTH_SENSITIVITY );
    }

    public ArrayList<ROI2DArea> getTooBigSpuriousMaskList() {
        return tooBigSpuriousMaskList;
    }

    boolean mask[] = null;
    /**
     * Detect mouse and correct background with spurious detections
     * @param depthImage
     * @param tailCandidateArrayList
     * @param tailImage
     *
     * @return
     */
    public ArrayList<MouseDetection> detectMice(
            IcyBufferedImage depthImage , IcyBufferedImage infraImage ,
            int t, ArrayList<BooleanMask2D> tailCandidateArrayList ) {

    	//Chronometer detectionChrono = new Chronometer("Detection Chrono");
        tooBigSpuriousMaskList.clear();
        //detectionChrono.displayMs();
        //LiveMouseTracker.performanceMonitor.stepDone("Detection : clear list");
        try
        {
            infraOut.beginUpdate();
//			Chronometer debugChrono = new Chronometer("debug Chrono");
            // Find the mouse as a ROI
//			float[] depthBackgroundImageData = backgroundSequence.getImage( 0 , 0 ).getDataXYAsFloat( 0 );
            short[] depthBackgroundImageData = backgroundSequence.getImage( 0 , 0 ).getDataXYAsShort( 0 );
            short[] depthBuffer = depthImage.getDataXYAsShort( 0 );
            short[] infraBuffer = infraImage.getDataXYAsShort( 0 );
//			boolean mask[] = new boolean[depthBuffer.length];
            if ((mask == null) || (mask.length != depthBuffer.length)) mask
            = new boolean[depthBuffer.length];

            // Grab cage ROI
            //ROI2D cageROI = null;

            //detectionChrono.displayMs();
            //System.out.println("Detection : Removing ROIs");
            for ( ROI2D roi : infraOut.getROI2Ds() )
            {
                if ( roi.getName().startsWith("seg") )
                {
                    infraOut.removeROI( roi );
                }
//				if ( roi.getName().startsWith("cage limits") )
//				{
//					cageROI = roi ;
//				}
            }

            //LiveMouseTracker.performanceMonitor.stepDone("Detection : remove ROIs");
            //detectionChrono.displayMs();

//			// Threshold depth and remove saturated values
//			for ( int i = 0 ; i < depthBackgroundImageData.length ; i++ )
//			{
//				if( (int)depthBackgroundImageData[i] - (int)depthBuffer[i]
//						> LiveMouseTracker.DEPTH_SENSITIVITY ) // in mm.
//				{
//					if ( ( infraBuffer[i] & 0xFFFF ) == 65535 )
//					{
//						mask[i] = false;
//					}else
//					{
//						mask[i] = true;
//					}
//
//				}else
//				{
//					mask[i] = false;
//				}
//			}

//
//			// Remove all saturated values
//			for ( int i = 0 ; i < infraBuffer.length ; i++ )
//			{
//				if ( ( infraBuffer[i] & 0xFFFF ) == 65535 )
//				{
//					mask[i] = false;
//				}
//			}
            // Threshold depth ************* OLD CODE
//                for ( int i = 0 ; i < depthBackgroundImageData.length ; i++ )
//                {
//					if( depthBackgroundImageData[i] - depthBuffer[i]
//							> LiveMouseTracker.DEPTH_SENSITIVITY ) // in mm.
//					{
//						mask[i] = true;
//						continue;
//					}
//					mask[i] = false;
//                }
//
//                // Remove all saturated values
//				for ( int i = 0 ; i < infraBuffer.length ; i++ )
//				{
//					if ( ( infraBuffer[i] & 0xFFFF ) == 65535 )
//					{
//						mask[i] = false;
//					}
//				}


            //System.out.println("Detection : Create maps");

            for ( int i = 0 ; i < depthBackgroundImageData.length ; i++ )
            {
                float height = depthBackgroundImageData[i] - depthBuffer[i];

                mask[i] = (height > LiveMouseTracker.DEPTH_SENSITIVITY);

//            	if ( LiveMouseTracker.ANIMAL_IS_WIRED )
//                {
//            		// removes detection of too high objects such as the cable.
//            		if ( mask[i] )
//            		{
//            			if ( height > 30 )
//            			{
//            				mask[i] = false;
//            			}
//            		}
//                }
            }
            //LiveMouseTracker.performanceMonitor.stepDone("Detection : Create maps");

            //detectionChrono.displayMs();

            //System.out.println("Detection : Remove saturated values");
            // Remove all saturated values
			for ( int i = 0 ; i < infraBuffer.length ; i++ )
			{
				if ( infraBuffer[i] == Short.MIN_VALUE)
				{
					mask[i] = false;
				}
			}
			//LiveMouseTracker.performanceMonitor.stepDone("Detection : Remove saturated values");

			BooleanMask2D allMiceMask = new BooleanMask2D( infraImage.getBounds(), mask) ;
			//BooleanMask2D allMiceMask = new BooleanMask2D( new Rectangle( 512, 424 ), mask) ;
            if ( LiveMouseTracker.cageROIMask != null ) // clip with cage if available
            	allMiceMask.intersect(LiveMouseTracker.cageROIMask);

            //detectionChrono.displayMs();
            //System.out.println("Detection : Detect in Mask");
            //LiveMouseTracker.performanceMonitor.stepDone("Detection : create detection mask");

            // opti 1
//            ROI2DArea roi = new ROI2DArea( new BooleanMask2D( new Rectangle( 512, 424 ), mask) );
//
//            if ( LiveMouseTracker.cageROI != null ) // clip with cage if available
//            {
//                roi = (ROI2DArea) roi.getIntersection( LiveMouseTracker.cageROI );
//            }

            ArrayList<MouseDetection> rawMouseDetectionArrayList = new ArrayList<MouseDetection>();

            // filter detections
            {
                //BooleanMask2D[] maskList = ErodeDilateTools.dilateROI( mask , xRadius, yRadius)

                BooleanMask2D[] maskArrayFull = allMiceMask.getComponents();
//                BooleanMask2D[] maskArrayFull = roi.getBooleanMask( true ).getComponents();
                //LiveMouseTracker.performanceMonitor.stepDone("Detection : get component");

                Map<BooleanMask2D, Integer> validMask = new HashMap<BooleanMask2D,Integer>();
//                ArrayList<BooleanMask2D> maskList = new ArrayList<BooleanMask2D>();
                ArrayList<BooleanMask2D> spuriousList = new ArrayList<BooleanMask2D>();

                // remove too small components
                BooleanMask2D cageFloorMask = LiveMouseTracker.cageFloorMask;
//                ROI cageFloor = LiveMouseTracker.getROICageFloor();

                for ( BooleanMask2D maskCandidate : maskArrayFull )
                {
                	//detectionChrono.displayMs();

                    int nbPoint = maskCandidate.getNumberOfPoints();
                    //System.out.println("Detection : Processing Mask " + nbPoint );

                    if ( nbPoint > LiveMouseTracker.MIN_SIZE_SEG_OK )
//                    if ( nbPoint > 50 )
                    {
                    	validMask.put(maskCandidate,Integer.valueOf(nbPoint));
//                        maskList.add( maskCandidate );
                    }
                    else
                    {
                        if (nbPoint > 3 ) // update background with only things a bit big.
                        {
                            backgroundHeightMapBuilder.correctBackGround( depthImage , maskCandidate );

                            boolean spuriousOk = true;
                            if ( cageFloorMask != null )
                            {
                                if ( !cageFloorMask.contains(  maskCandidate ) )
                                {
                                    spuriousOk = false;
                                }
                            }
//                            if ( cageFloor != null )
//                            {
//                                if ( !cageFloor.contains( new ROI2DArea( maskCandidate ) ) )
//                                {
//                                    spuriousOk = false;
//                                }
//                            }
                            if ( spuriousOk )
                            {
                                FrameInfo frameInfo = LiveMouseTracker.currentFrameInfo;
                                if ( frameInfo != null )
                                {
                                    frameInfo.addParticle();
                                }
                                spuriousList.add( maskCandidate );
                            }
                        }
                    }
                }
                //LiveMouseTracker.performanceMonitor.stepDone("Detection : build spurious");

                if ( LiveMouseTracker.SHOW_SMALL_SPURIOUS_DETECTION_ENABLED )
                {
                    for ( BooleanMask2D spuriousMask : spuriousList )
                    {
                        ROI2DAreaX roiSpuriousParticle = new ROI2DAreaX(spuriousMask);
                        roiSpuriousParticle.setName("tmp spurious particle");
                        roiSpuriousParticle.setColor( Color.yellow );
                        LiveMouseTracker.addROIToInfraSequence( roiSpuriousParticle );
                    }
                }

                List<BooleanMask2D> toRemove = new ArrayList<BooleanMask2D>();

                // code to remove wires from detection (opto..)
                if ( LiveMouseTracker.ANIMAL_IS_WIRED )
                {
                    for ( Entry<BooleanMask2D,Integer> entry: validMask.entrySet())
                    {
                        BooleanMask2D maskCandidate = entry.getKey();

                        boolean reject= false;

                        // reject if the cable come from the top (which is the case with the perspective)

                        if ( maskCandidate.bounds.y < 36 // top
                            	|| maskCandidate.bounds.x > 425 // right side
                            	|| maskCandidate.bounds.x < 85 // left side
                        		)
                        {
                        	reject = true;
                        }

                        if ( reject )
                        {
                        	//toRemove.add(maskCandidate);
                            //maskList.remove( i );
                            //backgroundHeightMapBuilder.correctBackGround( depthImage , maskCandidate );
                        	validMask.remove( entry );

                            ROI2DArea spuriousDetectionROI = new ROI2DArea( maskCandidate );
                            spuriousDetectionROI.setColor( Color.pink );
                            spuriousDetectionROI.setName("tmp wire");
                            spuriousDetectionROI.setShowName( true );
//                            System.out.println("REJECT " + reason );
                            infraOut.addROI( spuriousDetectionROI );
                        }

                        /*

//                        BooleanMask2D maskCandidate = maskList.get( i );
//                        System.out.println( "Volume: "+ volume + " nbPoint: " + nbPoint + " meanVol: " + meanVolume );


                        double nbContourPoint = maskCandidate.getContourPoints().length;
                        double surface = maskCandidate.getNumberOfPoints();
                        double ratio = surface / nbContourPoint;

                        double volume = LiveMouseTracker.getBackgroundHeightMapBuider().getVolume( LiveMouseTracker.depthImage,
                        		maskCandidate );
                        double meanVolume = volume/surface;

                        ROI2DArea erodedROI = (ROI2DArea) MorphoROITools.erodeROI( new ROI2DArea( maskCandidate ) , 2, 2 , 1 );
                        try
                        {
	                        if ( erodedROI.getNumberOfPoints() > 10 )
	                        {
	                        	ratio = erodedROI.getNumberOfPoints() / erodedROI.getNumberOfContourPoints();
	                        	if  ( ratio < 3 )
	                        	reject = true;
	                        }

	                        erodedROI.setColor( Color.orange );
	                        erodedROI.setName( "tmp" );
                            infraOut.addROI( erodedROI );

                        }
                        catch ( NullPointerException e)
                        {
                        	reject = true;
                        	// the roi is empty.
                        }
                        /*
                        if ( ratio < 3 )
                        	reject = true;

                        if ( maskCandidate.bounds.getHeight() < 15 && maskCandidate.bounds.getWidth() < 15 )
                        	reject = false;
*/


                        /*
                        //					System.out.println("surface: " + surface + " cont: " + nbContourPoint + " s/c: " + ratio );
                        String reason ="";

                        if ( !reject )
                        if ( meanVolume > 20 )
                        {
                        	reject = true;
                        	System.out.println("WIRE - VOLUME - " + meanVolume );
                        }

                        if ( !reject )
                        if ( ratio < 1.5 )
                        {
                        	reject = true;
                        	System.out.println("WIRE - RATIO - " + ratio );
//                        	LiveMouseTracker.addEvent(
//    								new Event(
//    										"RATIO", Color.RED,
//    										new Point2D.Double(
//    										maskCandidate.getOptimizedBounds().getCenterX(),
//    										maskCandidate.getOptimizedBounds().getCenterY()
//    										)));
                        }

                        if ( !reject )
                        {
                            Moment moment = new Moment( maskCandidate ); //, LiveMouseTracker.infraImage );
                            double longAxis = moment.aoipar.longAxis;
                            double shorterAxis = moment.aoipar.shorterAxis;

                            if ( longAxis > 50 )
                            {
                                reject = true;
                                System.out.println("WIRE - LONG AXIS - " + longAxis );
//    							LiveMouseTracker.addEvent(
//								new Event(
//										"ma_"+longAxis, Color.white,
//										new Point2D.Double(
//										maskCandidate.getOptimizedBounds().getCenterX(),
//										maskCandidate.getOptimizedBounds().getCenterY()
//										)));
//                                reason = "MATL";
                            }

                        }
                        */


                    }
                }

//				System.out.println( "mask list: " + maskList.size() );

                // smooth ROIs
//                for ( int i = 0 ; i < maskList.size() ; i++ )
//                {
//                    ROI2DArea roiTmp = (ROI2DArea )MorphoROITools.dilateROI( new ROI2DArea( maskList.get(i) ) , 1, 1 , 1 );
//                    roiTmp = (ROI2DArea )MorphoROITools.erodeROI( roiTmp , 1, 1 , 1 );
//                    maskList.set( i , roiTmp.getBooleanMask( true ) );
//                }



//				for ( int i = 0 ; i < maskList.length ; i++ )
//				{
//					ROI2DArea roiTmp = (ROI2DArea )MorphoROITools.dilateROI( new ROI2DArea( maskList[i] ) , 1, 1 , 1 );
//
//					roiTmp = (ROI2DArea )MorphoROITools.erodeROI( roiTmp , 1, 1 , 1 );
//
//					maskList[i] = roiTmp.getBooleanMask( true );
//
//				}
                // first check if data can be split if detection are too big, and spurious data

//                ArrayList<BooleanMask2D> maskArrayList = new ArrayList<BooleanMask2D>();

                for ( Entry<BooleanMask2D,Integer> entry: validMask.entrySet())
//                for ( BooleanMask2D maskCandidate : maskList )
                {
                    int nbPoint = entry.getValue().intValue();
                    BooleanMask2D maskCandidate = entry.getKey();

                    if ( nbPoint > LiveMouseTracker.MAX_SIZE_OF_CANDIDATE_DETECTION )
                    // if ( maskCandidate.getPoints().length > LiveMouseTracker.MAX_SIZE_OF_CANDIDATE_DETECTION )
                    {
                        /*
                    ArrayList<ROI2DArea> splitList = detectionSplitter( maskCandidate );
                    if ( splitList != null )
                    for ( ROI2DArea roi1 : splitList )
                    {
                        maskArrayList.add( roi1.getBooleanMask( true ) );
                    }
                         */
                        ROI2DArea spuriousDetectionROI = new ROI2DArea( maskCandidate );
                        spuriousDetectionROI.setColor( Color.pink );
                        spuriousDetectionROI.setName("tmp too big spurious");
                        spuriousDetectionROI.setShowName( true );
                        tooBigSpuriousMaskList.add( spuriousDetectionROI );

//                        infraOut.addROI( spuriousDetectionROI );
                        toRemove.add(maskCandidate);
                    }
//                    else
//                    {
//                        maskArrayList.add( maskCandidate );
//                    }
                }
                //LiveMouseTracker.performanceMonitor.stepDone("Detection : splitter");

                // remove invalid
                for(BooleanMask2D mask: toRemove)
                	validMask.remove(mask);

                // check detection are ok.

//                ArrayList<BooleanMask2D> detectionOkArrayList = new ArrayList<BooleanMask2D>();

//                for ( BooleanMask2D maskCandidate : maskArrayList )
//                {
//                    if ( maskCandidate.getPoints().length > LiveMouseTracker.MIN_SIZE_SEG_OK )
//                    {
//                        detectionOkArrayList.add( maskCandidate );
//                    }
//                    else
//                    {
//                        // This is not a mouse. So Integrate it in the ground.
//
//                        backgroundHeightMapBuider.correctBackGround( depthImage , maskCandidate );
//                        if ( LiveMouseTracker.SHOW_SMALL_SPURIOUS_DETECTION_ENABLED )
//                        {
//                            ROI2DArea spuriousDetectionROI = new ROI2DArea( maskCandidate );
//                            spuriousDetectionROI.setColor( Color.yellow );
//                            spuriousDetectionROI.setName("tmp spurious");
//                            infraOut.addROI( spuriousDetectionROI );
//                        }
//                    }
//                }

                if ( LiveMouseTracker.BUILD_TAIL_MAP )
                {
                    // remove tail candidates that are too much overlapping with detections
                    for( BooleanMask2D tail : new ArrayList<BooleanMask2D>( tailCandidateArrayList ) )
                    {
                        for ( BooleanMask2D maskCandidate : validMask.keySet())
//                        for ( BooleanMask2D maskCandidate : detectionOkArrayList )
                        {
                            if ( ! tailCandidateArrayList.contains( tail ) )
                            {
                                continue;
                            }

                            BooleanMask2D intersection = tail.getIntersection( maskCandidate );

                            // if the overlap of the tail with the intersection > const% then remove it
                            if ( intersection.getNumberOfPoints() > tail.getNumberOfPoints() * 0.1d )
                            {
                                tailCandidateArrayList.remove( tail );
                                continue;
                            }

                        }
                    }

                    // display filtered tail candidates
                    for( BooleanMask2D tail : tailCandidateArrayList )
                    {
                        ROI2DAreaX tailROI = new ROI2DAreaX( tail );
                        tailROI.setColor( Color.CYAN );
                        tailROI.setName( "tmp tail candidate");
                        infraOut.addROI( tailROI );
                    }

                    // process tails.
                    {

                    }
                }

                for ( BooleanMask2D maskCandidate : validMask.keySet())
//                for ( BooleanMask2D maskCandidate : detectionOkArrayList )
                {
                    ROI2DAreaX seg = createROI2DAreaX( maskCandidate );

                    rawMouseDetectionArrayList.add(
                            new MouseDetection( seg ,maskCandidate, t ) );

                }
                //LiveMouseTracker.performanceMonitor.stepDone("Detection : create detection");

            }
//			debugChrono.displayMs();

            return rawMouseDetectionArrayList;
        }
        finally
        {
            infraOut.endUpdate();
        }
    }

    public static ROI2DAreaX createROI2DAreaX(BooleanMask2D maskCandidate) {
        ROI2DAreaX seg = new ROI2DAreaX( maskCandidate );
        //seg.setShowName( true );
        seg.setName( "seg ok" );
        seg.setColor( Color.WHITE );
        LiveMouseTracker.addROIToInfraSequence( seg  );
        return seg;
    }

    /** @deprecated now use the DetectionSplitter class */
    public ArrayList<ROI2DArea> detectionSplitter(BooleanMask2D bigDetection ) {

        // FIXME: assume a 2 mice split. (should/ can be n)
        // FIXME 2: Should use as a seed the previous position or estimated of mice.
        int nbSplitWanted = 2;

        ROI2DArea inputROI = new ROI2DArea( new ROI2DArea( bigDetection ) );

        // step 1: shrink ROIS until we have 2.

        ArrayList<ROI2DArea> roiList = new ArrayList<ROI2DArea>();

        int watchDog = 0;

        ROI2DArea erodedROI = (ROI2DArea) inputROI.getCopy();
        //erodedROI = (ROI2DArea) ErodeDilateTools.dilateROI( erodedROI, 1, 1 );

        //System.out.println("split");
        while ( roiList.size() < nbSplitWanted )
        {
            if ( watchDog++ > 200 ) break;
            roiList.clear();
            //System.out.println("#" + watchDog );
            erodedROI = (ROI2DArea) MorphoROITools.erodeROI( erodedROI , 1, 1 , 1 );
            if ( erodedROI == null ) return null;
            BooleanMask2D mask = erodedROI.getBooleanMask( true );
            BooleanMask2D[] maskArray = mask.getComponents();
            for ( int i = 0 ; i< maskArray.length ; i++ )
            {
                //System.out.print( maskArray[i].getPoints().length + " ");
                roiList.add( new ROI2DArea( maskArray[i] ) );
            }
            //System.out.println();
        }

        // keep biggest

        while ( roiList.size() > nbSplitWanted )
        {
            ROI minROI = null;
            double minSize = Double.MAX_VALUE;
            for ( ROI2DArea roi : new ArrayList<ROI2DArea>( roiList )  )
            {
                if ( roi.getNumberOfPoints() < minSize )
                {
                    minSize = roi.getNumberOfPoints();
                    minROI = roi;
                }
            }
            roiList.remove( minROI );
        }

        // step 2: grow ROIs.

        for ( int i = 0 ; i< 30 ; i++ ) // Should be a global checkGrow Test.
        {
            // dilate
            for ( ROI2DArea roi : new ArrayList<ROI2DArea>( roiList )  )
            {
                roiList.remove( roi );
                roi = (ROI2DArea) MorphoROITools.dilateROI( roi , 1 , 1 , 1 );
                roiList.add( roi );
            }

            // limit growing with inputROI

            for ( ROI2DArea roi : new ArrayList<ROI2DArea>( roiList )  )
            {
                roiList.remove( roi );
                roi = (ROI2DArea) roi.getIntersection( inputROI );
                roiList.add( roi );
            }

            // limit with other ROIs

            ROI2DArea roi[] = new ROI2DArea[ roiList.size() ];
            ROI2DArea roiOriginal[] = new ROI2DArea[ roiList.size() ];

            for ( int j = 0 ; j< roiList.size() ; j++ )
            {
                roi[j] = roiList.get( j );
                roiOriginal[j] = (ROI2DArea) roiList.get( j ).getCopy();
            }

            for ( int j = 0 ; j< roi.length ; j++ )
            {
                for ( int k = 0 ; k< roi.length ; k++ )
                {
                    if ( j!=k )
                    {
                        roi[k] = (ROI2DArea) roi[k].getSubtraction( roiOriginal[j] );
                    }
                }
            }

            roiList = new ArrayList<ROI2DArea>( );
            for ( int j = 0 ; j< roi.length ; j++ )
            {
                roiList.add( roi[j ] ) ;
            }


            // end limit ROIs

        }

        for ( ROI2DArea roi : roiList )
        {
            roi.setName( "seg split" );
            roi.setColor( Color.getHSBColor( (float) Math.random() , 1.0f, 0.7f ) );
            //infraOut.addROI(roi);
        }

        return roiList;

    }

}
