package plugins.fab.aaa.voc;

import java.util.ArrayList;

public class FrequencyCancelerOldByLine {

	private ArrayList<FrequencyCancel> cancelList = new ArrayList<FrequencyCancel>();

	public FrequencyCancelerOldByLine( AudioFFTProcessing fftProcessing ) {

		if ( fftProcessing.magnitude.length > 1 )
		{
			System.err.println("AudioVocDetection is only working for 1 channel.");
			return;
		}

		double[][] magnitude = fftProcessing.getMagnitudeDenoised( 0 );

		// computeFrequencyToCancel()
		this.cancelList = computeCancelFrequencyListWithMagnitude( magnitude );


	}

	public ArrayList<FrequencyCancel> getCancelList() {
		return cancelList;
	}

	private ArrayList<FrequencyCancel> computeCancelFrequencyListWithMagnitude(double[][] magnitude) {

		ArrayList<FrequencyCancel> cancelList = new ArrayList<FrequencyCancel>();

		int width = magnitude.length;
		int height = magnitude[0].length;

		// compute frequency to cancel in binned data.
		// work using window of continuous data
		int window = 1172; // 1s /2
		double[] lineSignal = new double[window];

		// Evaluate std and mean in a patch

		ArrayList<Double> valueList = new ArrayList<Double>();
		for ( int x = 0 ; x < 500 || width < width ; x++ )
		{
			for ( int y = (int)(height*0.25) ; y < (int)(height*0.75) ; y++ )
			{
				valueList.add( magnitude[x][y] );
			}
		}
		double[] valueArray = new double[valueList.size()];
		for ( int x = 0 ; x < valueList.size(); x++ )
		{
			valueArray[x] = valueList.get( x );
		}

		double meanPatch = MathUtil.mean( valueArray );
		double stdPatch = MathUtil.mean( valueArray );

		for ( int freq = (int)(height*0.2) ; freq < (int)(height*0.8) ; freq++ )
		{
			for ( int i = 0 ; i < width ; i+=window/2 )
			{
				int nbOver = 0;
				if ( i+window >= width ) continue; // end of line band would be reached



				for ( int w = 0 ; w < window ; w ++ )
				{
					lineSignal[w] = magnitude[i+w][freq];

					if ( lineSignal[w] > 0.1 )
					{
						nbOver++;
					}
				}

				if ( nbOver > window *0.75 )
				{
					cancelList.add( new FrequencyCancel( i, i+window, freq , nbOver , 0 )  );
				}

				/*
				double mean = MathUtil.mean( lineSignal );
				//        		System.out.println( mean );
				if ( mean > 0.1 ) // 0.4 // + meanPatch + 5 * stdPatch
				{
					double stdv = MathUtil.stddev( lineSignal ); // to check constancy

					if ( stdv < 0.1 )
					{
						cancelList.add( new FrequencyCancel( i, i+window, freq , mean , stdv )  );
					}

//					//        		System.out.println("freq: " + freq + "\t stdv:" + stdv + "\t mean: " + mean );
//					if ( stdv < 0.35 ) // 0.35
//					{
//					}
				}
				*/
			}
		}

		return cancelList;
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
