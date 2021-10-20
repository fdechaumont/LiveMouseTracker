package plugins.fab.aaa.voc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;
import icy.image.IcyBufferedImage;
import icy.image.colormap.IcyColorMap;
import icy.image.colormap.LinearColorMap;
import icy.main.Icy;
import icy.sequence.Sequence;
import icy.system.profile.Chronometer;
import icy.type.DataType;

/**
 * Handle an audioFile and its processes and visu.
 * @author Fab
 *
 */
public class AudioFile2 {

    /** The wave file */
    // WavFile wave = null;

	/** The wave file */
	File file = null;

	/** QUANTITATIVE FEATURE DATA FOR MACHINE LEARNING */
	public int lengthScore = 0;
	public int complexityScore = 0;
	public int nbVoc = 0;
	public float vocDensity = 0;
	public float vocMeanLenght = 0;
	public float vocSTDLenght = 0;
	public float vocMeanFrequency = 0;
	public float vocSTDFrequency = 0;
    double meanPower;

    /** Magnitude bin size to display all info on a schrinked vertical line of spectrum */
    private int magnitudeBinSize = 0;

    /** magnitude[x][bufferSize / 2] ( full size )*/
    double[][] magnitudeAll;

    /** magnitude binned in y */
//    private double[][] magnitudeBinnedAll;

    /** Schrink magnitude size in spectrum view */
    //private int height = 300;

    /** Width of the image representation OR number of bin in time for all signal*/
//    private int width = 0; // ( nbFrame / bufferSize)+1; (set at constructor)

    /** Number of data in wav waveform */
//    private long nbFrame;

    /** max peak frequency per x */
    double[] frequencyMax;

    /** power per x */
    double[] power;


    String MLClass = null;

    /** list of continuous frequency jamming the signal */
    ArrayList<Integer> cancelFrequencyList = new ArrayList<Integer>();

    ArrayList<Voc> vocList = new ArrayList<Voc>();

    long ringStartIndex = 0;

    // The number of millisecond that one pixel represent.
//    float xLengthInMs;

    // The number of Hz that one pixel represent in y in image domain.
    float yFrequencyInHz;

//    int numChannel = 0;

    /** Number of sample used to compute 1 magnitude */
//    private int bufferSizePerChannel = 300000 / 100; /// 100;

//    private int bufferSizePerChannel = 166666 / 100; /// 100; // TODO : MAKE THIS AUTOMATIC

    /** the data for the whole sample. */
    private double waveFormData[][] = null;

	public float meanSpaceBetweenVoc;

	public float STDSpaceBetweenVoc;

	/** Tone slope is the difference between the 2 y of the line fit for a voc*/
	public float STDToneSlope;
	public float meanToneSlope;

	public float meanconsecutiveToneShiftStartEnd;

	public float STDconsecutiveToneShiftStartEnd;

	public float STDPower;

	public float meanNbJump;

	public float STDNbJump;

	public double totalTV;

	public double totalJump;

	public double totalModulation;

	public double totalPower;

	public double totalVocPartDuration;

	/** Number of repeat error in avisoft */
	public int nbRepeat = 0;

    public void setMLClass(String mLClass) {
		MLClass = mLClass;
	}

    public String getMLClass() {
		return MLClass;
	}

    /** reload data
     * @throws USVProcessingException */
    public void reloadData() throws USVProcessingException
    {
//    	try
//    	{
    		loadAllWaveformBuffer();
//    	}
//    	catch( Exception e )
//    	{
//    		e.printStackTrace();
//    		System.out.println("CANNOT COMPUTE RING");
//    	}
    }

    public double[][] getWaveFormData() {
    	synchronized ( this ) {
    		return this.waveFormData;
		}

    }

    public AudioFile2( File file ) throws USVProcessingException {
    	this.file = file;
    	reloadData();
    }

    void loadAllWaveformBuffer() throws USVProcessingException
    {
//    	System.out.println("Processing ring " + file.getName() );
    	WavFile wave = null;

        try {
            wave = WavFile.openWavFile( file);
        } catch (IOException | WavFileException e) {
            System.err.println("Can't load WAV file " + file.getAbsolutePath() );
            e.printStackTrace();
            throw new USVProcessingException("Can't load wav file.");
            //return;
        }

//        this.wave = wave;
//        long nbFrame = wave.getNumFrames();
        int numChannel = wave.getNumChannels();

        /*
        if ( numChannel != 1 )
        {
        	throw new USVProcessingException("Can't load wav file. The file provided has " + numChannel + ". 1 channel only is supported" );
        }
        */

		try {
			wave = WavFile.openWavFile( file );
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (WavFileException e1) {
			e1.printStackTrace();
		}

//		System.out.println( "Number of channel: " + numChannel );
//		System.out.println( "Number of frames: " + wave.getNumFrames() );
		int bufferSizePerChannel = (int) wave.getNumFrames();

        //double[] sampleBuffer = new double[bufferSizePerChannel];
		double[][] sampleBuffer = new double[numChannel][bufferSizePerChannel];

		//        for ( long i=0 ; i<nbFrame; i+=bufferSizePerChannel )
		//        {
		try {
			wave.readFrames( sampleBuffer, bufferSizePerChannel );
			wave.close();

		} catch (IOException | WavFileException e) {
			e.printStackTrace();
		}




		for ( int channel = 0 ; channel < numChannel ; channel++ )
		{
//			double sum=0;
			for ( int indexBuffer = 0 ; indexBuffer < bufferSizePerChannel -1 ; indexBuffer++ )
			{
//				sum+= Math.abs( sampleBuffer[channel][indexBuffer] );
				if (
						( sampleBuffer[channel][indexBuffer] > 0.99 ) &&
						( sampleBuffer[channel][indexBuffer+1] < -0.99 ) )
				{
					System.out.println("Found ring cursor in Channel " + channel + " index: " + indexBuffer + " over " + wave.getNumFrames() );
				}

			}
//			System.out.println("*** SUM channel " + channel + "\t"+sum);

		}

		// normalize
		if ( Constant.NORMALIZE )
		{
			System.out.println("Normalizing...");
			// find ratio
			float max = 0;
			for ( int channel = 0 ; channel < numChannel ; channel++ )
			{

				for ( int indexBuffer = 0 ; indexBuffer < bufferSizePerChannel -1 ; indexBuffer++ )
				{
					float current = (float) Math.abs( sampleBuffer[channel][indexBuffer] );
					if ( current > max )
					{
						max = current;
					}
				}
			}
			// amplify
			float ratio = 1.0f/max;
			for ( int channel = 0 ; channel < numChannel ; channel++ )
			{
				for ( int indexBuffer = 0 ; indexBuffer < bufferSizePerChannel -1 ; indexBuffer++ )
				{
					sampleBuffer[channel][indexBuffer]*=ratio;
				}
			}
		}

		// downmix all channels
		for ( int indexBuffer = 0 ; indexBuffer < bufferSizePerChannel -1 ; indexBuffer++ )
		{
			double r = 0;
			for ( int channel = 0 ; channel < numChannel ; channel++ )
			{
				r +=sampleBuffer[channel][indexBuffer];
			}
			r/= numChannel;
			sampleBuffer[0][indexBuffer] = r;
		}

		this.sampleRate = (int)wave.getSampleRate();

		if ( this.sampleRate != 300000 )
		{
			throw new USVProcessingException("Sample rate must be 300kHz. The file provided is sampled at " + this.sampleRate + " Hz." );
		}

		synchronized ( this ) {
				this.waveFormData = sampleBuffer;
		}

    }

    /** only for 1 channel */
    public void cropWave( double secStart , double secEnd )
    {
    	System.out.println("Croping wave : " + secStart + " to " + secEnd + "(s)" );
    	int currentSize = this.waveFormData[0].length;
    	int startOffset = (int)( secStart * sampleRate );
    	int endOffset = (int)( secEnd * sampleRate );

    	System.out.println("CROP WAVE start offset: " + secStart+ " s,offset: " + startOffset);
    	System.out.println("CROP WAVE end offset: " + secEnd+ " s,offset: " + endOffset);
    	if ( startOffset >= currentSize ) startOffset = currentSize;
    	if ( endOffset >= currentSize ) endOffset = currentSize;

    	int size = endOffset-startOffset;

    	double[] data = new double[size];
    	for ( int i = 0 ; i < size ; i++ )
    	{
    		data[i] = this.waveFormData[0][i+startOffset];
    	}

    	this.waveFormData[0] = data;
    	// rename data
    	this.file = new File( this.file.getAbsolutePath() + "_crop_" + secStart + "_" + secEnd );
    }

	public double getDurationInSecond() {
		//return wave.getDurationInSecond();
		return (float)this.waveFormData[0].length  / sampleRate;
	}

	public double getDurationInMilliSecond() {
//		return wave.getDurationInSecond() * 1000d;
		return 1000d * ( (float)this.waveFormData[0].length  / sampleRate );
	}

	int sampleRate = 0;
	public int getSampleRate() {
//		return (int)wave.getSampleRate();
		return sampleRate;
	}

	/** Clear signal data (magnitude..) and only keep quantification results. */
	public void clearHeavyData() {

		 magnitudeAll = null;
		 frequencyMax = null;
		 power = null;
		 waveFormData = null;
		 vocList.clear();
	}

}
