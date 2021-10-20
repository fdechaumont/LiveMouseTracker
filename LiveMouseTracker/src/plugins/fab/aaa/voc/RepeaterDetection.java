package plugins.fab.aaa.voc;

import java.util.ArrayList;

/** Find exact match of voc */
public class RepeaterDetection {

	ArrayList<Repeat> repeatList = new ArrayList<Repeat>();
	AudioFile2 audioFile;
	ArrayList<Voc> vocList;

	public RepeaterDetection(AudioFile2 audioFile, AudioFFTProcessing fftProcessing, ArrayList<Voc> vocList) {

		double[][] magnitude = fftProcessing.getMagnitude( 0 );
		this.audioFile = audioFile;
		this.vocList = vocList;
		this.repeatList = computeRepeatListWithMagnitude( magnitude , fftProcessing );

	}

	private ArrayList<Repeat> computeRepeatListWithMagnitude(double[][] magnitude, AudioFFTProcessing fftProcessing) {

		System.out.println("Start repeat");
		ArrayList<Repeat> repeatList = new ArrayList<Repeat>();

		int repeatWindow = 50;
		int startOffset = 100;
		int maxOffset = 200;

		int width = magnitude.length;
		int height = magnitude[0].length;

		// create linearSum
//		double[] verticalSum = new double[width];
//		{
//			for ( int x = 0 ; x < width-maxOffset ; x++ )
//			{
//				double sum = 0;
//				for ( int y = 0 ; y < height ; y++ )
//				{
//					sum+= magnitude[x][y];
//				}
//				verticalSum[x] = sum;
//			}
//		}
//
//		for ( int x = 0 ; x < width-maxOffset-repeatWindow ; x++ )
//		{
//			double[] data = getDataVertical( verticalSum , x , repeatWindow, width, height );
//
//			double bestCorrelation = -Double.MAX_VALUE;
//			int bestOffset = 0;
//			for ( int offsetX = startOffset ; offsetX < maxOffset ; offsetX++ )
//			{
//				double[] dataCandidate = getDataVertical( verticalSum , x+offsetX , repeatWindow, width, height );
//				double correlation = correlation( data, dataCandidate );
//
//				if ( correlation > bestCorrelation )
//				{
//					bestCorrelation = correlation;
//					bestOffset = offsetX;
//				}
//			}
//			if ( bestCorrelation > 0.99 )
//			{
//				repeatList.add( new Repeat( bestCorrelation, x , x+bestOffset , repeatWindow ) );
//				System.out.println("add repeat " + bestCorrelation );
//				x+=repeatWindow;
//			}
//		}


		// create linear data in a rotated 90 degrees dataframe and compress signal by factor
		int verticalCompressFactor=64;
		double[] linearData = new double[width*height/verticalCompressFactor];
		{
			int index = 0;
			for ( int x = 0 ; x < width-maxOffset ; x++ )
			{
				for ( int y = 0 ; y < height ; y+=verticalCompressFactor )
				{
					for ( int i = 0 ; i < verticalCompressFactor ; i++ )
					{
						linearData[index] += magnitude[x][y+i];
					}
					index++;
				}
			}
		}
		height /= verticalCompressFactor;

		for ( int x = 0 ; x < width-maxOffset-repeatWindow ; x++ )
		{
			// create window
			double[] data = getData( linearData , x , repeatWindow, width, height );

			double bestCorrelation = -Double.MAX_VALUE;
			int bestOffset = 0;
			for ( int offsetX = startOffset ; offsetX < maxOffset ; offsetX++ )
			{
				double[] dataCandidate = getData( linearData , x+offsetX , repeatWindow, width, height );
				double correlation = correlation( data, dataCandidate );
				if ( correlation > bestCorrelation )
				{
					bestCorrelation = correlation;
					bestOffset = offsetX;
				}
			}
			if ( bestCorrelation > 0.999999 )
			{
				// check best width.
				/*
				repeatList.add( new Repeat( bestCorrelation, x , x+bestOffset , repeatWindow ) );
				System.out.println("add repeat " + bestCorrelation );
				x+=repeatWindow;
				*/
				int bestWidth = 0;
				double bestCorrelationWidth = 0;
				for ( int widthTest = 50 ; widthTest < 300 ; widthTest++ )
				{
					double[] dataOriginal = getData( linearData , x , widthTest, width, height );
					double[] dataCandidate = getData( linearData , x+bestOffset , widthTest, width, height );
					double correlation = correlation( dataOriginal, dataCandidate );
					if ( correlation > 0.99 ) //bestCorrelationWidth )
					{
						bestCorrelationWidth = correlation;
						bestWidth = widthTest;
					}
				}
				repeatList.add( new Repeat( bestCorrelation, x , x+bestOffset , bestWidth ) );
				audioFile.nbRepeat++;
				System.out.println("add repeat " + bestCorrelationWidth + " / bestWidth: " + bestWidth);
				x+=bestWidth;
			}
		}

		for ( Repeat repeat : repeatList )
		{
			for ( Voc voc : vocList )
			{
				if ( overlapVocRepeat( voc, repeat ) )
				{
					voc.isInBadRepeat = true;
				}
			}
		}

		System.out.println("End repeat");


		return repeatList;
	}

	private boolean overlapVocRepeat(Voc voc, Repeat repeat) {

		// check original
		float rStart = repeat.xOriginal;
		float rEnd = repeat.xOriginal + repeat.repeatWindowWidth;

		if ( voc.getStartX() >= rStart && voc.getStartX() <= rEnd ) return true;
		if ( voc.getEndX() >= rStart && voc.getEndX() <= rEnd ) return true;
		if ( voc.getStartX() <= rStart && voc.getEndX() >= rEnd ) return true;

		rStart = repeat.xRepeat;
		rEnd = repeat.xRepeat + repeat.repeatWindowWidth;

		if ( voc.getStartX() >= rStart && voc.getStartX() <= rEnd ) return true;
		if ( voc.getEndX() >= rStart && voc.getEndX() <= rEnd ) return true;
		if ( voc.getStartX() <= rStart && voc.getEndX() >= rEnd ) return true;


		return false;

	}

	public static double correlation( double[] xs, double[] ys) {
		//TODO: check here that arrays are not null, of the same length etc

		double sx = 0.0;
		double sy = 0.0;
		double sxx = 0.0;
		double syy = 0.0;
		double sxy = 0.0;

		int n = xs.length;

		for(int i = 0; i < n; ++i) {
			double x = xs[i];
			double y = ys[i];

			sx += x;
			sy += y;
			sxx += x * x;
			syy += y * y;
			sxy += x * y;
		}

		// covariation
		double cov = sxy / n - sx * sy / n / n;
		// standard error of x
		double sigmax = Math.sqrt(sxx / n -  sx * sx / n / n);
		// standard error of y
		double sigmay = Math.sqrt(syy / n -  sy * sy / n / n);

		// correlation is just a normalized covariation
		return cov / sigmax / sigmay;
	}

	private double[] getDataVertical(double[] data, int x, int repeatWindow , int width, int height) {

		int startOffset = x;
		int endOffest = x+repeatWindow;

		double[] subData = new double[endOffest-startOffset+1];

		int index =0;
		for ( int i= startOffset ; i<endOffest;i++)
		{
			subData[index]
					= data[i];
			index++;
		}

		return subData;

	}


	private double[] getData(double[] data, int x, int repeatWindow , int width, int height) {

		int startOffset = height*x;
		int endOffset = height*(x+repeatWindow);

		double[] subData = new double[endOffset-startOffset+1];
		if ( endOffset > data.length-1 )
		{
			return subData;
		}

		int index =0;
		for ( int i= startOffset ; i<endOffset;i++)
		{
			subData[index]
					= data[i];
			index++;
		}

		return subData;

	}

}
