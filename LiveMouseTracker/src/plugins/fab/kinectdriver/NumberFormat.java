package plugins.fab.kinectdriver;

import java.text.DecimalFormat;

/**
 * This class is just to unify the decimalformat used for file input/output name
 * @author Fab
 *
 */
public class NumberFormat {

	static DecimalFormat df = new DecimalFormat( "000000" );
	
	public static String format( double d )
	{
		return df.format( d );
	}
}
