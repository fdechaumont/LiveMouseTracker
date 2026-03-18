package plugins.fab.aaa.voc;

import java.util.ArrayList;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;
import icy.image.IcyBufferedImage;
import icy.image.colormap.LinearColorMap;
import icy.main.Icy;
import icy.sequence.Sequence;
import icy.type.DataType;

/**
 * Process the FFT for a mono buffer.
 * @author Fab
 *
 */
public class AudioFFTProcessing {

	/** Channel, x, magnitude */
	public double[][][] magnitude;
	public double[][][] magnitudeDenoised;
	float overlap;
	float yFrequencyInHz;
	float xTimeInMs;
	/** This image is the std of the denoisedMagnitude.*/
	public IcyBufferedImage processedImage;

	public AudioFFTProcessing( double[][] audioBuffer , int sampleRate , float overlap , int FFTSize ) {

		overlap = 1-overlap;

		this.magnitude = new double[audioBuffer.length][][];

		this.xTimeInMs = FFTSize*overlap *1000f / ( sampleRate );
		System.out.println("xTimeInMs:" + xTimeInMs );

		int channel = 0;
		//for ( int channel = 0 ; channel < audioBuffer.length ; channel ++ )
		{
			this.overlap = overlap;

			ArrayList<double[]> magnitudeList = new ArrayList<>();

			for ( int t = 0 ; t < audioBuffer[channel].length-FFTSize ; t+=FFTSize*overlap ) // mettre l'overlap ici !
			{

				// process spectrum
				/*
			N = 1024          // size of FFT and sample window
			Fs = 44100        // sample rate = 44.1 kHz
			data[N]           // input PCM data buffer
			fft[N * 2]        // FFT complex buffer (interleaved real/imag)
			magnitude[N / 2]  // power spectrum
				 */

				double[] fftBuffer = new double[FFTSize*2];
				/*
        for i = 0 to N - 1
                fft[2*i] = data[i]
                        fft[2*i+1] = 0
				 */
				for ( int index = 0 ; index < FFTSize ; index++ )
				{
					fftBuffer[2*index] = audioBuffer[channel][t+index];
					fftBuffer[2*index+1] = 0;
				}

				//		perform in-place complex-to-complex FFT on fft[] buffer
				DoubleFFT_1D dfft1D = new DoubleFFT_1D( FFTSize );
				dfft1D.complexForward( fftBuffer );

				/*
				 * // calculate power spectrum (magnitude) values from fft[]
			for i = 0 to N / 2 - 1
			re = fft[2*i]
			im = fft[2*i+1]
			magnitude[i] = sqrt(re*re+im*im)
				 */

				double[] magnitudeForT = new double[FFTSize/2];

				for ( int index = 0 ; index < FFTSize/2 ; index++ )
				{
					double re = fftBuffer[2*index];
					double im = fftBuffer[2*index+1];
					double magnitudeCurrent = Math.sqrt( re*re + im*im );
					magnitudeForT[index] = magnitudeCurrent;
				}
				magnitudeList.add( magnitudeForT );

				/*
		//convert index of largest peak to frequency
		freq = max_index * Fs / N
				 */
			}

			// convert to array
			magnitude[channel] = new double[magnitudeList.size()][];
//			System.out.println("Magnitude list size= " + magnitudeList.size() );
			for ( int t = 0 ; t< magnitudeList.size() ; t++ )
			{
				magnitude[channel][t] = magnitudeList.get( t );
			}
		}

		magnitudeDenoised = new double[ magnitude.length ][ magnitude[0].length ][ magnitude[0][0].length ];
		yFrequencyInHz = ( sampleRate / 2f ) / magnitude[0][0].length;

	}

	/** multi channel not supported yet. */
	public double[][] getMagnitude(int channel) {
		return magnitude[channel];
	}

	public double[][][] getMagnitudeForAllChannels() {
		return magnitude;
	}

	/** multi channel not supported yet. */
	public double[][] getMagnitudeDenoised(int channel) {
		return magnitudeDenoised[channel];
	}

	public double[][][] getMagnitudeDenoisedForAllChannels() {
		return magnitudeDenoised;
	}

	/*
	public double[][][] getMagnitudeDenoisedCropped(int startX, int endX) {

		double [][][] magnitudeDenoisedCropped = new double[ magnitude.length ][ endX-startX ][ magnitude[0][0].length ];

		for ( int c= 0 ; c < magnitude.length ; c++ )
		{
			for ( int x = startX ; x< endX ; x++ )
			{
				magnitudeDenoisedCropped[c][x-startX] = magnitudeDenoised[c][x];
			}
		}

		return magnitudeDenoisedCropped;


	}

	public double[][][] getMagnitudeCropped(int startX, int endX) {

		double [][][] magnitudeCropped = new double[ magnitude.length ][ endX-startX ][ magnitude[0][0].length ];

		for ( int c= 0 ; c < magnitude.length ; c++ )
		{
			for ( int x = startX ; x< endX ; x++ )
			{
				magnitudeCropped[c][x-startX] = magnitude[c][x];
			}
		}

		return magnitudeCropped;


	}
	*/



}
