package plugins.fab.aaa.voc;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;

import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.roi.BooleanMask2D;
import icy.sequence.Sequence;
import icy.type.DataType;
import ij.gui.Roi;
import plugins.kernel.roi.roi2d.ROI2DArea;

public class FrequencyCancelerAndSTD {

	private ArrayList<FrequencyCancel> cancelList = new ArrayList<FrequencyCancel>();
	public double valuesDif[];
	/** Fused voc list */
	ArrayList<BooleanMask2D> vocMaskList = null;

	/** get mask of vocs (fused) */
	public ArrayList<BooleanMask2D> getVocMaskList() {
		return vocMaskList;
	}

	public FrequencyCancelerAndSTD( AudioFFTProcessing fftProcessing , boolean pupMode, float detectionThreshold, boolean cancelFrequency ) {

		/*
		if ( fftProcessing.magnitude.length > 1 )
		{
			System.err.println("AudioVocDetection is only working for 1 channel.");
			return;
		}
		*/

		double[][] magnitude = fftProcessing.getMagnitudeDenoised( 0 );

		// computeFrequencyToCancel()
		this.cancelList = computeCancelFrequencyListWithMagnitude( magnitude , fftProcessing , pupMode, detectionThreshold, cancelFrequency );

		// to test if the system can work without it.
//		cancelList.clear();

	}

	public ArrayList<FrequencyCancel> getCancelList() {
		return cancelList;
	}

	private ArrayList<FrequencyCancel> computeCancelFrequencyListWithMagnitude(double[][] magnitude ,  AudioFFTProcessing fftProcessing,
			boolean pupMode , float detectionThreshold, boolean cancelFrequency ) {

		//float detectionThreshold = 0.1f;

//		if ( pupMode ) // now in fullvocprocessor
//		{
//			//Constant.MAX_Y_IN_SPECTRUM = 512;
//			// detectionThreshold = 0.05f;
//			//detectionThreshold = 0.001f;
//		}

		ArrayList<FrequencyCancel> cancelList = new ArrayList<FrequencyCancel>();

		int width = magnitude.length;
		int height = magnitude[0].length;

		// compute frequency to cancel in binned data.
		// work using window of continuous data
		int lenY = (Constant.MAX_Y_IN_SPECTRUM - Constant.MIN_Y_IN_SPECTRUM);

		double[] allValues = new double[ lenY * width ];

		int offsetAll=0;
		for ( int freq = 0 ; freq < lenY ; freq++ )
		{
			for ( int x = 0 ; x < width; x++ )
			{
				double val = magnitude[x][freq+Constant.MIN_Y_IN_SPECTRUM ];

				allValues[offsetAll++]=val;
			}
		}

		//

		double std = MathUtil.stddev( allValues );
		double mean = MathUtil.mean( allValues );
		System.out.println("STD freq canceller: " + std );
		System.out.println("Mean: " + mean );

		Sequence seq = new Sequence("Over STD");
		IcyBufferedImage image = new IcyBufferedImage(width, height, 1 , DataType.DOUBLE );
		double[] buffer = image.getDataXYAsDouble( 0 );

//		if ( std > 0.004 )
		
		// Frequency cancelling
		System.out.println("Cancel frequency enabled: " + cancelFrequency );
		
		{			
			double threshold = mean+0.15d*std; //0.12
			for ( int freq = 0 ; freq < height ; freq++ )
			{
				// compute the number of value over for the line
				int nbValOver = 0;
				for ( int x = 0 ; x < width; x++ )
				{
					double val = magnitude[x][height - freq - 1];
					if ( val > threshold )
					{
						nbValOver++;
						buffer[x+ ( freq )*width ] = val-threshold;

					}else
					{
						buffer[x+ ( freq)*width ] = 0;
					}
				}
				
				if ( cancelFrequency )
				{
					if( nbValOver > width * 0.4d )
					{
						cancelList.add( new FrequencyCancel( 0, width, height-freq-1, mean, std ) );
						// fill of 0 the line
						for ( int x = 0 ; x < width ; x++ )
						{
							buffer[x+ ( freq)*width ] = 0;
							buffer[x+ ( freq-1)*width ] = 0;
							buffer[x+ ( freq+1)*width ] = 0;
						}
						System.out.println("FREQ FOUND:" + freq );
					}
				}
			}
		}

		// Post filtering

		// remove vertical noise
		for ( int x = 0 ; x < width ; x++ )
		{
			double sum = 0;
			for ( int y = 0 ; y < height ; y++ )
			{
				double val = buffer[x+ ( y )*width ];
				sum+=val;
			}
			double m = sum/height;
			for ( int y = 0 ; y < height ; y++ )
			{
				buffer[x+ ( y )*width ]-=m*20d;
			}
		}


		// remove small values
		for ( int x = 0 ; x < width ; x++ )
		{
			for ( int y = 0 ; y < height ; y++ )
			{
				double val = buffer[x+ ( y )*width ];
				if ( val < detectionThreshold ) // remove very small values
				{
					buffer[x+ ( y )*width ] = 0;
				}
			}
		}

		// perform detection
		{
			// create boolean Mask
			boolean[] maskImage = new boolean[buffer.length];
			for ( int i = 0 ; i<buffer.length;i++)
			{
				maskImage[i] = buffer[i] >= detectionThreshold;
			}

			BooleanMask2D allSoundMask = new BooleanMask2D( image.getBounds(), maskImage) ;
			ArrayList<BooleanMask2D> maskList = new ArrayList<>( );
			for ( BooleanMask2D mask : allSoundMask.getComponents() )
			{
				maskList.add( mask );
			}

			ArrayList<BooleanMask2D> keptMaskList = new ArrayList<BooleanMask2D>();

			
			// display and filter all
			for ( BooleanMask2D mask : maskList )
			{
				ROI2DArea roi = new ROI2DArea(mask);

				// Show roi with filtering color code.
//				seq.addROI( roi );

				roi.setColor( Color.yellow );
				int nbPoint = mask.getNumberOfPoints();
				int nbInSameVertical = getHowManyMaskInSameVertical( mask , maskList );

				
				MeanSTD meanSTD = getMeanAndSTD( mask, image );

				if ( meanSTD.mean < 0.15 )
				{
					roi.setColor( Color.red );
				}

				if ( nbPoint < 5 )
				{
					roi.setColor( Color.red );
				}

				if ( meanSTD.mean > 1)
				{
					roi.setColor( Color.green );
				}

				if ( meanSTD.mean >= 0.15 && meanSTD.mean <= 1 )
				{
					if ( nbPoint > 50 )
					{
						roi.setColor( Color.pink );
					}
				}


				if ( nbInSameVertical == 0 && roi.getColor() != Color.red )
				{
					roi.setColor( Color.cyan );
				}

				if ( nbInSameVertical > 3 && nbPoint < 150 )
				{
					roi.setColor( Color.gray );
				}
				
				// keep cyan, green, pink
				if ( roi.getColor().equals( Color.cyan )
						|| roi.getColor().equals( Color.green )
						|| roi.getColor().equals( Color.pink ) )
				{					
					keptMaskList.add( mask );
				}
				
			}
			

			// merge close candidates
			boolean hasFused = true;
			while ( hasFused == true )
			{
				System.out.println("Fusing...");
				hasFused = false;

				HashMap< BooleanMask2D, StartEnd > maskToStartEnd = new HashMap<BooleanMask2D, StartEnd>();
				System.out.println("Start");
				for ( BooleanMask2D mask  : keptMaskList )
				{
					maskToStartEnd.put( mask , new StartEnd(
							mask.getOptimizedBounds().getMinX( ),
							mask.getOptimizedBounds().getMaxX( ) )
							);
				}
				System.out.println("End");

				System.out.println("Fusing...");
				for ( int i = 0 ; i< keptMaskList.size(); i++ )
				{
					BooleanMask2D vocA = keptMaskList.get( i );

					if ( vocA == null ) continue;
					BooleanMask2D vocB = getNextMask( keptMaskList, vocA , maskToStartEnd );
					if ( vocB == null ) continue;

					double vocBMinX = maskToStartEnd.get( vocB ).start;
					double vocAMaX = maskToStartEnd.get( vocA ).end;
					double distanceInPx = vocBMinX - vocAMaX;

//					if ( distanceInPx < 0 ) continue;

					//double distanceInPx = vocB.getOptimizedBounds().getMinX() - vocA.getOptimizedBounds().getMaxX();
					//vocA.getDurationInMs()

					double distanceDurationInMs = fftProcessing.xTimeInMs * distanceInPx;
					//double distanceDurationInMs = distanceInPx; // FIXME : RECONVERT IN MS !
//					System.out.println("DistMS: " + distanceDurationInMs );
					if ( distanceDurationInMs <= Constant.MAX_GAP_DURATION_BETWEEN_SEQUENCE_TO_FUSE_VOC_IN_MS )
					{
						//					System.out.println("Fused");
						//Voc newVoc = Voc.fuseVoc( vocA , vocB );
//						BooleanMask2D maskFused = new BooleanMask2D( );
//						maskFused.add( vocA );
//						maskFused.add( vocB );
						BooleanMask2D maskFused = BooleanMask2D.getUnion( vocA, vocB );
						keptMaskList.remove( vocA );
						keptMaskList.remove( vocB );
						keptMaskList.add( maskFused );
						maskToStartEnd.put( maskFused , new StartEnd(
								maskFused.getOptimizedBounds().getMinX( ),
								maskFused.getOptimizedBounds().getMaxX( ) )
								);
						hasFused = true; // FIXME : put back to true after test
						//break;
					}
				}
			}

			for ( BooleanMask2D mask : keptMaskList )
			{
				ROI2DArea roi = new ROI2DArea(mask);
				seq.addROI( roi );
			}

			this.vocMaskList = keptMaskList;

		}

		fftProcessing.processedImage = image;

		seq.addImage( image );
//		Icy.addSequence( seq );

		System.out.println("Number of cancel Freq: " + cancelList.size() );
		System.out.println("End of cancel list");

//		cancelList.add( new FrequencyCancel( 0, width, 100, 0, 0));



//		double[] lineSignal = new double[window];

		/*
		for ( int freq = Constant.MIN_Y_IN_SPECTRUM ; freq <= Constant.MAX_Y_IN_SPECTRUM ; freq++ )
		{
			for ( int i = 0 ; i < width; i+=window )
			{

				//if ( i+window >= width ) continue; // end of line band would be reached
				double val = 0;
				int nbVal = 0;
				for ( int w = 0 ; w < window && i+w < width ; w ++ )
				{
					val += magnitude[i+w][freq];
					nbVal++;
				}

				if ( nbVal > 0 )
				{
					double mean = val / nbVal;

					for ( int w = 0 ; w < window && i+w < width ; w ++ )
					{
						magnitude[i+w][freq] -= mean;
					}
				}




			}
		}
*/

		return cancelList;
	}

	/** get the voc next the one provided in the list (or overlapping)
	 * @param maskToStartEnd */
	public BooleanMask2D getNextMask(ArrayList<BooleanMask2D> maskList, BooleanMask2D mask, HashMap<BooleanMask2D, StartEnd> maskToStartEnd ) {

		BooleanMask2D bestMask = null;
		double minDistance = Float.MAX_VALUE;
		double maskMinX = maskToStartEnd.get( mask ).start;
		double maskMaxX = maskToStartEnd.get( mask ).end;

		for ( BooleanMask2D maskCandidate : maskList )
		{
			if ( maskCandidate == mask ) continue;

			double maskCandidateMinX = maskToStartEnd.get( maskCandidate ).start;

			if ( maskCandidateMinX >= maskMinX && maskCandidateMinX <= maskMaxX )
			{
				return maskCandidate; // overlap case  => send it immediately.
			}

			//if ( maskCandidate.getStartX() < mask.getEndX() )
			//if ( maskCandidate.getOptimizedBounds().getMinX() < mask.getOptimizedBounds().getMaxX() )
			if ( maskCandidateMinX < maskMaxX )
			{
				// the vocCandidate is in the past.
				continue;
			}

			//double distanceInPx = maskCandidate.getOptimizedBounds().getMinX() - mask.getOptimizedBounds().getMaxX();
			double distanceInPx = maskCandidateMinX - maskMaxX;

			if ( distanceInPx < minDistance )
			{
				minDistance = distanceInPx;
				bestMask = maskCandidate;
			}
		}

		return bestMask;
	}

	private int getHowManyMaskInSameVertical(BooleanMask2D mask, ArrayList<BooleanMask2D> maskList) {

		int nbMaskInVertical = 0;
		double centerX = mask.getOptimizedBounds().getCenterX();
		for ( BooleanMask2D maskCandidate : maskList )
		{
			if ( maskCandidate == mask ) continue;
			if ( Math.abs( maskCandidate.getOptimizedBounds().getCenterX() - centerX ) < 5 )
			{
				nbMaskInVertical++;
			}
		}

		return nbMaskInVertical;
	}

	private MeanSTD getMeanAndSTD(BooleanMask2D mask, IcyBufferedImage image) {

		double[] val = new double[mask.getPoints().length];
		int i =0;
		for ( java.awt.Point p : mask.getPoints() )
		{
			val[i] = image.getData( p.x, p.y, 0);
			i++;
		}
		double mean= MathUtil.mean( val );
		double std= MathUtil.stddev( val );
		return new MeanSTD(mean, std);

	}

	public boolean contain(int x, int y) {

		for ( FrequencyCancel fc : cancelList )
		{
			if ( fc.contain( x, y ) )
				return true;
		}
		return false;
	}



}
