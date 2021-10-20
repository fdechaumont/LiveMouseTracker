package plugins.fab.livemousetracker.detection;


/**
 * MathUtil for detection
 *
 * */
public class MathUtil {

	/**
	 * Provides the correlation between 2 signals XS and YS.
	 * Both are shifted by shiftXS and shiftY
	 * flipVertical to compare images flip in vertical ( row order inversed )
	 * Correlation is computed on length elements.
	 */
	static public double correlation(short[] xs, short[] ys , int width , boolean flipVertical ) {

		double sx = 0.0;
		double sy = 0.0;
		double sxx = 0.0;
		double syy = 0.0;
		double sxy = 0.0;

		int n = xs.length;

		if ( flipVertical )
		{
			short xs2[] = new short[xs.length];
			int rowIndexReversed = xs.length-1-width;
			int rowIndex = 0;
			while ( rowIndexReversed > 0 )
			{
				for ( int w = 0 ; w<width; w++ )
				{
					xs2[rowIndexReversed+w] = xs[rowIndex+w];
				}
				rowIndexReversed-=width;
				rowIndex+=width;
			}
			xs = xs2;
		}

		for(int i = 0; i < xs.length; i++) {

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


	public static double mean( short[] data )
	{
		double sum = 0;
		for ( int i=0 ; i<data.length ; i++ )
		{
			sum+=data[i];
		}
		return sum / data.length;
	}

	public static double stddev(short numArray[])
	{
		double standardDeviation = 0.0;
		int length = numArray.length;

		double mean = mean( numArray );

		for(double num: numArray) {
			standardDeviation += Math.pow(num - mean, 2);
		}

		return Math.sqrt(standardDeviation/length);
	}
}



