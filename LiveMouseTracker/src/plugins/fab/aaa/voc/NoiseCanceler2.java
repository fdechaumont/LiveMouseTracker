package plugins.fab.aaa.voc;

import java.util.ArrayList;

import icy.system.SystemUtil;
import icy.system.thread.Processor;

/**
 * @deprecated test of noise filtering of deepsqueak
 * @author Fab
 *
 */
public class NoiseCanceler2 {


	public NoiseCanceler2( AudioFFTProcessing fftProcessing ) {

		/*
		if ( fftProcessing.magnitude.length > 1 )
		{
			System.err.println("AudioVocDetection is only working for 1 channel.");
			return;
		}
		*/

		double[][] magnitude = fftProcessing.getMagnitude( 0 );

		// computeFrequencyToCancel()
		computeNoiseCancellingWithMagnitude( magnitude , fftProcessing.getMagnitudeDenoised( 0 ) );

	}

	// facon deepsqueak.
	private void computeNoiseCancellingWithMagnitude(double[][] magnitude , double[][] magnitudeDenoised) {

		int width = magnitude.length;
		int height = magnitude[0].length;

		double[] verticalSignal ;
		double[] verticalSignalDenoised ;

		for ( int x=0; x<width ; x++ )
		{
			verticalSignal = magnitude[x];
			verticalSignalDenoised = magnitudeDenoised[x];

			int startY = 100;
			int endY = height;
			// geometric mean:
			double nbVal = 1;
			double sum = verticalSignal[startY];
			for ( int y = startY+1; y< endY ; y++)
			{
				sum*=verticalSignal[y];
				nbVal++;
			}
			double geometricMean = Math.pow( sum,  1.0/ nbVal );
			// arithmeticalMean
			sum=0;
			nbVal=0;
			for ( int y = startY+1; y< endY ; y++)
			{
				sum+=verticalSignal[y];
				nbVal++;
			}
			double arithmeticMean = sum/nbVal;

			//double arithmeticMean = MathUtil.mean( verticalSignal );


			double result = 1d- ( geometricMean / arithmeticMean );

//			System.out.println(result);


				try
				{
					for ( int y = startY+1; y< endY ; y++)
					{
						verticalSignalDenoised[y]= result;

					}
			//verticalSignalDenoised[ (int)(startY + (endY-startY)*result) ] = 1;
				}
				catch( Exception e )
				{
					System.out.println( result );
				}
//			double std = MathUtil.stddev( verticalSignal );



			/*
			for ( int y = 0; y<height ; y++)
			{
				verticalSignalDenoised[y]= verticalSignal[y] - mean ;//- std/2f; // * std *4
			}*/
		}



		/*
						// curve to bottom
						magnitudeSmoothed[x][y] +=
								magnitudeDenoised[x+smoothWindowX][y+smoothWindowY+smoothWindowX];
						nbVal++;

						// curve to top
						magnitudeSmoothed[x][y] +=
								magnitudeDenoised[x+smoothWindowX][y+smoothWindowY+smoothWindowX];
						nbVal++;
		 */

		// smooth data.
		/*
		double[][] magnitudeSmoothed = new double[magnitude.length][magnitude[0].length];

		ArrayList<OptimSteerFilter> filterList = new ArrayList<>();

		int windowX = 5;
		int windowY = 1;
		for ( double angle = -80 ; angle <= 80 ; angle+=20 )
		{
			double rad = Math.toRadians( angle );
			OptimSteerFilter osf = new OptimSteerFilter();
			for ( int xx = -windowX ; xx <= windowX ; xx++ )
			{
				for ( int thick = -windowY ; thick <= windowY ; thick++ )
				{
					double px = Math.cos( rad ) * xx;
					double py = Math.sin( rad ) * xx;

					px+= Math.cos( Math.toRadians( angle+90 ) ) * thick;
					py+= Math.sin( Math.toRadians( angle+90 ) ) * thick;

					//osf.offsetList.add( (int)px + (int)(py) * width );
					osf.offsetList.add( new Point( (int)px, (int)py ) );
					//double read = data[ (int)px + (int)(py) * width ];
				}
			}
			filterList.add( osf );
		}
		*/

		// normal version
		/*
		for ( int x=windowX; x<width-windowX ; x++ )
		{
			for ( int y = windowY+10; y < height - windowY-10; y++ )
			{
				double max = Double.MIN_VALUE;
				for ( OptimSteerFilter osf : filterList )
				{
					double val = 0;
					for ( Point offsetPoint : osf.offsetList )
					{
						val+= magnitudeDenoised[x+offsetPoint.x][y+offsetPoint.y];
					}

					if ( val>max)
					{
						max = val;
					}
				}
				double result = max / (double) filterList.get( 0 ).offsetList.size();
				magnitudeSmoothed[x][y] = result; // Math.log( result );
			}
		}
		*/
/*
		// high speed version:
		Processor processor = new Processor( Integer.MAX_VALUE , SystemUtil.getNumberOfCPUs() );
		for ( int x=windowX; x<width-windowX ; x++ )
		{
			final int xx = x;
			Runnable task = new Runnable() {

				@Override
				public void run() {
					//for ( int y = windowY+10; y < height - windowY-10; y++ )

					for ( int y = Constant.MIN_Y_IN_SPECTRUM; y < Constant.MAX_Y_IN_SPECTRUM; y++ )
					{
						double max = -Double.MAX_VALUE;
						for ( OptimSteerFilter osf : filterList )
						{
							double val = 0;
							for ( Point offsetPoint : osf.offsetList )
							{
								val+= magnitudeDenoised[xx+offsetPoint.x][y+offsetPoint.y];
							}

							if ( val>max)
							{
								max = val;
							}
						}
						double result = max / (double) filterList.get( 0 ).offsetList.size();
						magnitudeSmoothed[xx][y] = result; // Math.log( result );
					}

				}
			};
			processor.submit( task );
		}
		while ( processor.isProcessing() )
		{
			System.out.println("Processing... " + (int)(100d* processor.getCompletedTaskCount() / processor.getTaskCount()) + " %");
			try {
				Thread.sleep( 250 );
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		*/



/*
		// remove horizontals constant frequencies
		{
			for ( int y = windowY+10; y < height - windowY-10; y++ )
			{
				int subWin = 40;
				int sizeX = magnitudeSmoothed.length;
				int minX = windowX;
				int maxX = width - windowX;
				double[] result = new double[sizeX];
				for ( int x= minX ; x< maxX ; x++ )
				{
					double sum = 0;
					double nbVal = 0;
					for ( int offsetX = -subWin; offsetX < subWin; offsetX++ )
					{
						if ( x+offsetX < 0 || x+offsetX >= sizeX ) continue;
						sum+=magnitudeSmoothed[x+offsetX][y];
						nbVal++;
					}
					double mean = sum/nbVal;
					result[x] = mean;
				}

				// copy back.
				for ( int x = minX; x < maxX; x++ )
				{
					magnitudeSmoothed[x][y] -= result[x];
				}

			}
		}
		*/

		// remove verticals
/*
		for ( int x=windowX; x<width-windowX ; x++ )
		{
			double sum = 0;
			double nbVal = 0;
			for ( int y = windowY+10; y < height - windowY-10; y++ )
			{
				sum+=magnitudeSmoothed[x][y];
				nbVal++;
			}
			double mean = sum/nbVal;
			for ( int y = windowY+10; y < height - windowY-10; y++ )
			{
				magnitudeSmoothed[x][y] -= mean;
			}


		}
*/
		/*
		// remove vertical with kernel
		int minY = windowY+10;
		int maxY = height - windowY-10;
		int sizeY = magnitudeSmoothed[0].length;
		int subWin = 10;
		for ( int x=windowX; x<width-windowX ; x++ )
		{
			double[] result = new double[ sizeY ];

			for ( int y = minY; y < maxY; y++ )
			{
				double sum = 0;
				double nbVal = 0;
				for ( int offsetY = -subWin; offsetY < subWin; offsetY++ )
				{
					if ( y+offsetY < 0 || y+offsetY >= sizeY ) continue;
					sum+=magnitudeSmoothed[x][y+offsetY];
					nbVal++;
				}
				double mean = sum/nbVal;
				result[y] = mean;
			}

			// copy back.
			for ( int y = minY; y < maxY; y++ )
			{
				magnitudeSmoothed[x][y] -= result[y];
			}


		}
		*/


		/*

		 // smooth data version 1

		int windowX = 4;
		int windowY = 1;
		for ( int x=windowX; x<width-windowX ; x++ )
		{
			for ( int y = windowY+10; y < height - windowY-10; y++ )
			{

				{
					double nbVal = 0;
					for ( int smoothWindowX = -windowX ; smoothWindowX <=windowX ; smoothWindowX ++ )
					{
						for ( int smoothWindowY = -windowY ; smoothWindowY <=windowY ; smoothWindowY ++ )
						{
							magnitudeSmoothed[x][y] +=
									magnitudeDenoised[x+smoothWindowX][y+smoothWindowY];
							nbVal++;
						}
					}
					magnitudeSmoothed[x][y] /= nbVal;
				}
				{
					// check if diagonal positive (45°) is better
					double nbVal = 0;
					double val = 0;
					for ( int smoothWindowX = -windowX ; smoothWindowX <=windowX ; smoothWindowX ++ )
					{
						for ( int smoothWindowY = -windowY ; smoothWindowY <=windowY ; smoothWindowY ++ )
						{
							val += magnitudeDenoised[x+smoothWindowX][y+smoothWindowY+smoothWindowX];
							nbVal++;
						}
					}
					val /= nbVal;
					if ( val > magnitudeSmoothed[x][y] )
					{
						magnitudeSmoothed[x][y] = val;
					}
				}
				{
//					 check if diagonal positive (45°) is better *2
					double nbVal = 0;
					double val = 0;
					for ( int smoothWindowX = -windowX ; smoothWindowX <=windowX ; smoothWindowX ++ )
					{
						for ( int smoothWindowY = -windowY ; smoothWindowY <=windowY ; smoothWindowY ++ )
						{
							val += magnitudeDenoised[x+smoothWindowX][y+smoothWindowY+smoothWindowX*2];
							nbVal++;
						}
					}
					val /= nbVal;
					if ( val > magnitudeSmoothed[x][y] )
					{
						magnitudeSmoothed[x][y] = val;
					}
				}

				{
//					 check if diagonal negative (-45°) is better
					double nbVal = 0;
					double val = 0;
					for ( int smoothWindowX = -windowX ; smoothWindowX <=windowX ; smoothWindowX ++ )
					{
						for ( int smoothWindowY = -windowY ; smoothWindowY <=windowY ; smoothWindowY ++ )
						{
							val += magnitudeDenoised[x+smoothWindowX][y+smoothWindowY-smoothWindowX];
							nbVal++;
						}
					}
					val /= nbVal;
					if ( val > magnitudeSmoothed[x][y] )
					{
						magnitudeSmoothed[x][y] = val;
					}
				}

				{
					// check if diagonal negative (-45°) is better
					double nbVal = 0;
					double val = 0;
					for ( int smoothWindowX = -windowX ; smoothWindowX <=windowX ; smoothWindowX ++ )
					{
						for ( int smoothWindowY = -windowY ; smoothWindowY <=windowY ; smoothWindowY ++ )
						{
							val += magnitudeDenoised[x+smoothWindowX][y+smoothWindowY-smoothWindowX*2];
							nbVal++;
						}
					}
					val /= nbVal;
					if ( val > magnitudeSmoothed[x][y] )
					{
						magnitudeSmoothed[x][y] = val;
					}
				}
			}
		}
		*/

		/*
		for ( int x = 0 ; x < width ; x++ )
		{
			magnitudeDenoised[x] = magnitudeSmoothed[x];
		}
		*/


		/*
		// smooth vertical
		for ( int x=0; x<width ; x++ )
		{
			int windowSize = 3;
			verticalSignalDenoised = magnitudeDenoised[x];
			double smoothedVertical[] = new double[verticalSignalDenoised.length];
			for ( int y = windowSize; y<height-windowSize ; y++)
			{
				double nbVal = 0;
				for ( int smoothWindow = -windowSize ; smoothWindow <=windowSize ; smoothWindow ++ )
				{
					smoothedVertical[y]+= verticalSignalDenoised[y+smoothWindow];
					nbVal++;
				}
				smoothedVertical[y]/=nbVal;
			}
			magnitudeDenoised[x] = smoothedVertical;
		}*/



	}



}
