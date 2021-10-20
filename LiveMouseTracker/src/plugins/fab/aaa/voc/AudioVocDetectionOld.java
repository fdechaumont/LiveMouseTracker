package plugins.fab.aaa.voc;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Comparator;



/**
 * Perform voc detection on a FFT processed data
 * */
public class AudioVocDetectionOld {

	ArrayList<Voc> vocList = null;

	// voc fusion
	int MAX_GAP_DURATION_BETWEEN_SEQUENCE_TO_FUSE_VOC_IN_MS = 30;
	boolean FUSE_CLOSE_VOC = true;

	// voc filtering
	int MIN_VOC_DURATION_IN_MS = 2;
	boolean VOC_POST_FILTERING_ENABLED = true;
	boolean VOC_PRE_FILTERING_ENABLED = true;
//	int Y_WINDOW_FOR_VOC_SEARCH = 1;

	// voc detection
	/** magnitude threshold: the smaller the more sensitive */
	double MAGNITUDE_THRESHOLD_FOR_VOC = 0.10; // 0.23 // 0.3; // 0.4; // 0.6; // 0.6;
	boolean SEARCH_FOR_HARMONICS = false;

	ArrayList<Voc> preFilteringVocList = null;

	public ArrayList<Voc> getVocList() {
		return vocList;
	}

	public ArrayList<Voc> getPreFilteringVocList() {
		return preFilteringVocList;
	}

	public AudioVocDetectionOld( AudioFFTProcessing fftProcessing, FrequencyCancelerAndSTD frequencyCanceler ) {

		if ( fftProcessing.magnitude.length > 1 )
		{
			System.err.println("AudioVocDetection is only working for 1 channel.");
			return;
		}

		double[][] magnitude = fftProcessing.getMagnitudeDenoised( 0 );
		//double[][] magnitude = fftProcessing.getMagnitude( 0 );

		// computeFrequencyToCancel()
		this.vocList = computeVocWithMagnitude( magnitude , frequencyCanceler , fftProcessing );
	}

	private ArrayList<Voc> computeVocWithMagnitude(double[][] magnitude, FrequencyCancelerAndSTD frequencyCanceler, AudioFFTProcessing fftProcessing) {

		Voc currentVoc = null;
		int width = magnitude.length;
		int height = magnitude[0].length;
		double[] frequencyMax = new double[width];
		ArrayList<Voc> vocList = new ArrayList<Voc>();
		preFilteringVocList = new ArrayList<Voc>();

		System.out.println("Compute with magnitude");
		for ( int x = 1 ; x < width-1; x++ )
		{
			//System.out.println("x:\t" + x );

			double[] magVertical = magnitude[x] ;

			frequencyMax[x] = 0;
			double maxMagnitude = 0;
			double[] reducedArray = reduceArray( magVertical );
			double std = MathUtil.stddev( reducedArray );
			double mean = MathUtil.mean( reducedArray );

			//if ( std )

			int nbAcceptedMagnitudeForY = 0;
//			if ( std > Constant.MIN_STD_FOR_VERTICAL_DETECTION )
			{

				for ( int y = Constant.MIN_Y_IN_SPECTRUM ; y <= Constant.MAX_Y_IN_SPECTRUM ; y++ )
				{
					double currentMagnitude = magVertical[y];
					//				double currentMagnitude =
					//						magnitude[x][y] + magnitude[x-1][y] + magnitude[x+1][y];
					//				currentMagnitude /= 3d;

//					if ( x == 56 )
//					{
//						System.out.println("y:\t" + y +"\tval:\t" + currentMagnitude );
//					}

					//if ( currentMagnitude > MAGNITUDE_THRESHOLD_FOR_VOC )
//					if ( currentMagnitude < 0.0 ) continue;

					//if ( currentMagnitude > 0.0+ mean+ Constant.STD_MULTIPLICATOR_FOR_DETECTION*std )
					if ( currentMagnitude > 0.2 )
					{
						nbAcceptedMagnitudeForY++;

						if ( currentMagnitude > maxMagnitude )
						{
							if ( !frequencyCanceler.contain( x , y ) )
							{
								maxMagnitude = currentMagnitude;
								frequencyMax[x] = y;
							}
						}
					}
				}
			}
//

			if ( nbAcceptedMagnitudeForY > height * 0.2d ) // 0.15
			{
				// remove the value detected as too many value were matching.
				frequencyMax[x]=0;
			}

			if ( frequencyMax[x]==0 )
			{
				if ( currentVoc != null )
				{
					vocList.add( currentVoc );
					System.out.println( currentVoc );
				}
				currentVoc = null;
			}
			else
			{
				if ( currentVoc == null )
				{
					currentVoc = new Voc(  fftProcessing.xTimeInMs , fftProcessing.yFrequencyInHz );
				}
				currentVoc.add( x , (int)frequencyMax[x] );
			}
		}

		if ( currentVoc != null )
		{
			vocList.add( currentVoc );
		}

		preFilteringVocList = new ArrayList<>( vocList );

		//preFusionFilterVoc( vocList, magnitude );

		fuseVoc( vocList );

		postFusionFilterVoc( vocList, magnitude  );

		System.out.println("Post finished");
//		vocList = recutVoc( vocList , fftProcessing );
//		fuseVoc( vocList );

		if ( SEARCH_FOR_HARMONICS )
		{
			for ( Voc voc : vocList )
			{
				searchForHarmonics( magnitude, frequencyCanceler, fftProcessing , voc );

			}
		}

		sortVocInTime( vocList );

		// compute some quantitative values
		computeVocPower( vocList , magnitude );

		return vocList;

	}



	private ArrayList<Voc> recutVoc(ArrayList<Voc> vocList, AudioFFTProcessing fftProcessing) {

		// FIXME: RECUT les vocs !!

		ArrayList<Voc> vocRecutList = new ArrayList<>();
		Voc currentVoc = null;
		for ( Voc voc : new ArrayList<Voc>( vocList ) )
		{
			for ( int x = voc.getStartX() ; x <= voc.getEndX() ; x++ )
			{
				Point p = voc.getPointAt( x );
				if ( p != null )
				{
					if ( currentVoc == null )
					{
						// start a new voc
						currentVoc = new Voc( fftProcessing.xTimeInMs , fftProcessing.yFrequencyInHz );
						currentVoc.add( p );
						//voc.add( point );
					}else
					{
						currentVoc.add( p );
					}
				}else
				{
					if ( currentVoc != null ) // close voc
					{
						vocRecutList.add( currentVoc );
						currentVoc = null;
					}
				}
			}

			if ( currentVoc != null ) // close eventual last voc
			{
				vocRecutList.add( currentVoc );
				currentVoc = null;
			}
		}
		return vocRecutList;

	}

	private void fuseVoc( ArrayList<Voc> vocList ) {

		if ( FUSE_CLOSE_VOC )
		{
//			System.out.println("Voc Fusion: ms: " + MAX_GAP_DURATION_BETWEEN_SEQUENCE_TO_FUSE_VOC_IN_MS );
//			Voc vocZ = vocList.get( 0 );
//			System.out.println( vocZ.xLengthInMs );


			// fuse close voc.
			boolean hasFused = true;

			while ( hasFused == true ) // && nbFused < 20 )
			{
				hasFused = false;
				//			System.out.println("Voc List size: " + vocList.size() );
				for ( int i = 0 ; i< vocList.size(); i++ )
				{
					Voc vocA = vocList.get( i );
					Voc vocB = getNextVoc( vocList, vocA );
					if ( vocB == null ) continue;

					int distanceInPx = vocB.getStartX() - vocA.getEndX();
					//vocA.getDurationInMs()

					float distanceDurationInMs = vocA.xLengthInMs * distanceInPx;

					//				System.out.println( "distance " + distanceInPx);
					if ( distanceDurationInMs < MAX_GAP_DURATION_BETWEEN_SEQUENCE_TO_FUSE_VOC_IN_MS )
					{
						//					System.out.println("Fused");
						Voc newVoc = Voc.fuseVoc( vocA , vocB );
						vocList.remove( vocA );
						vocList.remove( vocB );
						vocList.add( newVoc );

						hasFused = true; // FIXME : put back to true after test
						//break;
					}
				}
			}
		}

	}

	private double[] reduceArray(double[] magVertical ) {
		int newSize = Constant.MAX_Y_IN_SPECTRUM-Constant.MIN_Y_IN_SPECTRUM;

		double[] reducedArray = new double[ newSize ];

		int shift = Constant.MIN_Y_IN_SPECTRUM;

		for ( int y = 0; y < reducedArray.length ; y++ )
		{
			reducedArray[y] = magVertical[y+shift];
		}

		return reducedArray;
	}

	private void computeVocPower(ArrayList<Voc> vocList2, double[][] magnitude ) {

		for ( Voc voc : vocList2 )
		{

			float power = 0;
			for ( Point p : voc.pointList )
			{
				power += magnitude[p.x][p.y];
			}
			voc.meanPower = power / voc.pointList.size();

		}
	}

	private void sortVocInTime(ArrayList<Voc> vocList2) {

		vocList2.sort( new Comparator<Voc>() {
			@Override
			public int compare(Voc o1, Voc o2) {
				return o1.getStartX() - o2.getStartX();
			}
		});

	}


	private void searchForHarmonics(double[][] magnitude, FrequencyCancelerAndSTD frequencyCanceler, AudioFFTProcessing fftProcessing, Voc voc) {

		// remove existing detection and search for new one at a distance of dist
		//Voc currentVoc = null;
		int width = magnitude.length;
		int height = magnitude[0].length;
		double[] frequencyMax = new double[width];
		//ArrayList<Voc> vocList = new ArrayList<Voc>();

		for ( int x = voc.getStartX() ; x <= voc.getEndX() ; x++ )
		{
			frequencyMax[x] = 0;
			double maxMagnitude = 0;

			for ( int y = (int)(height*0.3) ; y < (int)(height*0.8) ; y++ )
			{
				double currentMagnitude = magnitude[x][y];
				if ( currentMagnitude > MAGNITUDE_THRESHOLD_FOR_VOC )
				{
					/*
					double sum = 0;
					for ( int yy = y - Y_WINDOW_FOR_VOC_SEARCH ; yy <= y + Y_WINDOW_FOR_VOC_SEARCH ; yy++ )
					{
						sum += magnitude[x][yy];
					}
					double mean = sum / (Y_WINDOW_FOR_VOC_SEARCH + 1);
					if ( mean < Y_WINDOW_FOR_VOC_SEARCH )
					{
						continue;
					}
					*/

					if ( currentMagnitude > maxMagnitude )
					{
						if ( !frequencyCanceler.contain( x , y ) )
						{
							if ( !voc.contain( x, y , 10 ) )
							{
								maxMagnitude = currentMagnitude;
								frequencyMax[x] = y;
							}
						}
					}
				}
			}

			if ( frequencyMax[x]==0 )
			{
//				if ( currentVoc != null )
//				{
//					vocList.add( currentVoc );
//				}
//				currentVoc = null;
			}
			else
			{
				voc.pointListHarmonics.add( new Point( x, (int)frequencyMax[x] ) );
//				if ( currentVoc == null )
//				{
//					currentVoc = new Voc(  fftProcessing.xTimeInMs , fftProcessing.yFrequencyInHz );
//				}
//				currentVoc.add( x , (int)frequencyMax[x] );
			}
		}
//		if ( currentVoc != null )
//		{
//			vocList.add( currentVoc );
//		}

		// filter result
		for ( Point p : new ArrayList<Point>( voc.pointListHarmonics ) )
		{
			boolean foundNeigbourh = false;
			for ( Point p2 : voc.pointListHarmonics )
			{
				if ( p2 == p ) continue;
				if ( p.x == p2.x-1 || p.x == p2.x+1 )
				{
					foundNeigbourh = true;
					break;
				}
			}
			if ( !foundNeigbourh )
			{
				voc.pointListHarmonics.remove( p );
			}

		}

	}

	private void preFusionFilterVoc(ArrayList<Voc> vocList, double[][] magnitude ) {

		if ( !VOC_PRE_FILTERING_ENABLED) return;

		// filter voc.

		// remove too Small vocs.
		for ( Voc voc : new ArrayList<>( vocList) )
		{
			if ( voc.getDurationInMs() < MIN_VOC_DURATION_IN_MS )
			{
				vocList.remove( voc );
			}
		}


		// remove small but very vertical vocs.
		for ( Voc voc : new ArrayList<>( vocList) )
		{
			if ( voc.getDurationInMs() < 10 && voc.getFrequencyDynamicInHz() > 12000)
			{
				vocList.remove( voc );
			}
		}


		// remove very flat and short vocs. (and also the one that may subsist to frequencyCanceler)
		for ( Voc voc : new ArrayList<>( vocList) )
		{
			if ( voc.getDurationInMs() < 10 )
			{
				//if ( voc.getMeanFrequencyTVInPix() < 1 )
				if ( voc.areAllValuesTheSame() )
				{
					vocList.remove( voc );
				}
			}
		}


/*
		computeVocPower( vocList, magnitude);
		for ( Voc voc : new ArrayList<>( vocList) )
		{
			if ( voc.getDurationInMs() < 20 )
			{
				if ( voc.power < 0.5 )
				{
					vocList.remove( voc );
				}
			}
		}
*/



		// remove too spiky vocs that are actually noise
/*
		for ( Voc voc : new ArrayList<>( vocList) )
		{
			if ( voc.getMeanFrequencyTVInHz() > 5000 )
			{
				vocList.remove( voc );
			}
		}
		*/


	}

	private void postFusionFilterVoc(ArrayList<Voc> vocList, double[][] magnitude ) {

		if ( !VOC_POST_FILTERING_ENABLED) return;

		removeUpDownNoiseEffectInVoc( vocList );

		removeJumpyPointInVoc( vocList );

		removeIsolatedPointInVoc( vocList );

		// remove very flat vocs. (and also the one that may subsist to frequencyCanceler)
		for ( Voc voc : new ArrayList<>( vocList) )
		{
			if ( voc.getMeanFrequencyTVInPix() < 1 )
			{
				vocList.remove( voc );
			}
		}

//		computeVocPower( vocList, magnitude);
//		for ( Voc voc : new ArrayList<>( vocList) )
//		{
//			if ( voc.power < 0.4 )
//			{
//				vocList.remove( voc );
//			}
//		}

		// remove empty vocs.
		for ( Voc voc : new ArrayList<Voc>( vocList ) )
		{
			if ( voc.pointList.size() == 0 )
			{
				vocList.remove( voc );
			}
		}


		// remove not strong enough voc.
/*

*/
		/*
		// remove too small voc too isolated
		{
			// get Closest voc
			double distance = Double.MAX_VALUE;
			Voc bestVoc = null;
			for ( Voc voc : vocList )
			{
				double d = Math.abs( voc.getCenterX() - mouseX );
				if ( d < distance )
				{
					bestVoc = voc;
					distance = d;
				}
			}
		}
		*/

		// filter voc.
/*
		// remove too Small vocs.
		for ( Voc voc : new ArrayList<>( vocList) )
		{
			if ( voc.getDurationInMs() < MIN_VOC_DURATION_IN_MS )
			{
				vocList.remove( voc );
			}
		}

		// remove small but very vertical vocs.
		for ( Voc voc : new ArrayList<>( vocList) )
		{
			if ( voc.getDurationInMs() < 10 && voc.getFrequencyDynamicInHz() > 20000)
			{
				vocList.remove( voc );
			}
		}



		// remove too spiky vocs that are actually noise

		for ( Voc voc : new ArrayList<>( vocList) )
		{
			if ( voc.getMeanFrequencyTVInHz() > 5000 )
			{
				vocList.remove( voc );
			}
		}
*/

	}


	/**
	 * remove all piece of voc having too strong up/down component on very short time.
	 * */
	private void removeUpDownNoiseEffectInVoc(ArrayList<Voc> vocList ) {

		int window = 4;
		for ( Voc voc : vocList )
		{
			for ( int x = voc.getStartX() ; x < voc.getEndX()-window ; x++ )
			{
				boolean up= false;
				boolean down = false;
				for ( int offsetX = 0 ; offsetX <=window ; offsetX++ )
				{
					if ( voc.getPointAt( x +offsetX ) == null ) continue;
					if ( voc.getPointAt( x +offsetX +1 ) == null ) continue;

					double dif = voc.getPointAt( x + offsetX + 1).y - voc.getPointAt( x + offsetX ).y;
					if ( dif > 5 ) up = true;
					if ( dif < -5 ) down = true;
				}

				if ( up && down )
				{
					for ( int offsetX = 0 ; offsetX <=window ; offsetX++ )
					{
						voc.removePointAtX( x + offsetX );
					}
				}
			}

		}

	}

	/**
	 * Remove point on voc if they consist of 1 or 2 points.
	 * */
	private void removeIsolatedPointInVoc(ArrayList<Voc> vocList ) {

		for ( Voc voc : vocList )
		{
			for ( int x = voc.getStartX() ; x <= voc.getEndX() ; x++ )
			{
				if ( voc.getPointAt( x ) == null ) continue;

				// remove one isolated point
				if ( voc.getPointAt( x - 1 ) == null && voc.getPointAt( x + 1 ) == null )
				{
					voc.removePointAtX( x );
				}

				// remove if only 2 consecutive points isolated.
				if ( voc.getPointAt( x +1 ) != null &&
						voc.getPointAt( x - 1 ) == null && voc.getPointAt( x + 2 ) == null )
				{
					voc.removePointAtX( x );
				}

				// remove if only 3 consecutive points isolated.
				if ( voc.getPointAt( x +1 ) != null &&
						voc.getPointAt( x +2 ) != null &&
						voc.getPointAt( x - 1 ) == null && voc.getPointAt( x + 3 ) == null )
				{
					voc.removePointAtX( x );
				}

			}
		}

	}

	/**
	 * Remove point on voc that jump at edge of signal.
	 * */
	private void removeJumpyPointInVoc(ArrayList<Voc> vocList ) {

		for ( Voc voc : vocList )
		{
			for ( int x = voc.getStartX() ; x <= voc.getEndX() ; x++ )
			{
				if ( voc.getPointAt( x ) == null ) continue;

				// if no point before and point after.
				if ( voc.getPointAt( x - 1 ) == null && voc.getPointAt( x + 1 ) != null )
				{
					double dif = Math.abs( voc.getPointAt( x + 1 ).y - voc.getPointAt( x ).y );
					if ( dif > 10 )
					{
						voc.removePointAtX( x );
					}
				}
				// if no point after and point before.
				if ( voc.getPointAt( x + 1 ) == null && voc.getPointAt( x - 1 ) != null )
				{
					double dif = Math.abs( voc.getPointAt( x - 1 ).y - voc.getPointAt( x ).y );
					if ( dif > 10 )
					{
						voc.removePointAtX( x );
					}
				}

			}



		}

	}





	/** get the voc next the one provided in the list */
	public Voc getNextVoc(ArrayList<Voc> vocList, Voc voc) {

		Voc bestVoc = null;
		float minDistance = Float.MAX_VALUE;

		for ( Voc vocCandidate : vocList )
		{
			if ( vocCandidate == voc ) continue;

			if ( vocCandidate.getStartX() < voc.getEndX() )
			{
				// the vocCandidate is in the past.
				continue;
			}

			int distanceInPx = vocCandidate.getStartX() - voc.getEndX();


			if ( distanceInPx < minDistance )
			{
				minDistance = distanceInPx;
				bestVoc = vocCandidate;
			}
		}

		return bestVoc;
	}

}
