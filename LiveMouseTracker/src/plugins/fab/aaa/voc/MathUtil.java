package plugins.fab.aaa.voc;

/**
 * MathUtil for vocalization
 *
 * */
public class MathUtil {

    static public double maxCorrelation(double[] xs, double[] ys , int window )
    {
    	double max = 0;
    	for ( int shift =-window ; shift < window ; shift++ )
    	{
    		double score = correlation( xs, ys, shift );
    		if ( score > max )
    		{
    			max = score;
    		}
    	}
    	return max;
    }

    /**
     * Provides the correlation between 2 signals XS and YS.
     * Both are shifted by shiftXS and shiftY
     * Correlation is computed on length elements.
     */
    static public double correlation(double[] xs, int shiftXS, double[] ys, int shiftYS, int length ) {

    	double sx = 0.0;
    	double sy = 0.0;
    	double sxx = 0.0;
    	double syy = 0.0;
    	double sxy = 0.0;

    	int n = xs.length;

    	for(int i = 0; i < length; ++i) {

    		double x = xs[i+shiftXS];
    		double y = ys[i+shiftYS];

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
    static public double correlation(double[] xs, double[] ys) {
    	return correlation(xs, ys,0);
    }
    static public double correlation(double[] xs, double[] ys , int shift ) {

    	double sx = 0.0;
    	double sy = 0.0;
    	double sxx = 0.0;
    	double syy = 0.0;
    	double sxy = 0.0;

    	int n = xs.length;

    	for(int i = 0; i < n; ++i) {
    		int indexShifted = i+shift;
    		int indexShiftedCorrected= indexShifted;
    		if( indexShifted < 0 ) indexShiftedCorrected= n-1+indexShifted;
    		if( indexShifted >= n ) indexShiftedCorrected= indexShifted-n;

    		double x = xs[indexShiftedCorrected];
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

	public static double mean( double[] data )
    {
    	double sum = 0;
    	for ( int i=0 ; i<data.length ; i++ )
    	{
    		sum+=data[i];
    	}
    	return sum / data.length;
    }

	public static float mean( float[] data )
    {
    	double sum = 0;
    	for ( int i=0 ; i<data.length ; i++ )
    	{
    		sum+=data[i];
    	}
    	return (float)(sum / data.length);
    }

    public static double stddev(double numArray[])
    {
        double standardDeviation = 0.0;
        int length = numArray.length;

        double mean = mean( numArray );

        for(double num: numArray) {
            standardDeviation += Math.pow(num - mean, 2);
        }

        return Math.sqrt(standardDeviation/length);
    }

	public static double sum(double[] data) {
    	double sum = 0;
    	for ( int i=0 ; i<data.length ; i++ )
    	{
    		sum+=data[i];
    	}
    	return sum;
	}
}
