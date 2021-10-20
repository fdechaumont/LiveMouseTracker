package plugins.fab.aaa.voc;

import java.awt.Color;
import java.awt.Polygon;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;

import icy.math.ArrayMath;

/** This is a single voc */
public class Voc {

	Color color = new Color ( Color.HSBtoRGB( (float)Math.random() , 0.85f, 0.95f ) );

	// FIXME: remove public here
//	public ArrayList<Integer> xList = new ArrayList<Integer>();
//	ArrayList<Integer> yList = new ArrayList<Integer>();

	ArrayList<Point> pointList = new ArrayList<>();
	ArrayList<Point> pointListHarmonics = new ArrayList<>();
	public double linearityIndex = 0;
	public float meanPower = 0;
	public float peakPower = 0;
	public float peakFrequency = 0;
	public float minPower = 0;

    ArrayList<String> classificationDescription = new ArrayList<String>();

    /** number of millisecond that 1 x represents. */
    float xLengthInMs = 0;
    /** number of Hz that 1 y represents */
    float yFrequencyInHz = 0;

	public Point2D pA;
	public Point2D pB;

	public int nbModulation = 0;

	ArrayList<Integer> jumpList = new ArrayList<Integer>();
	ArrayList<Integer> modulationList = new ArrayList<Integer>();

	public boolean matchWithGroundTruth = false;

	/** bad repeat of avisoft */
	public boolean isInBadRepeat = false;



    public Voc( float xLengthInMs , float yFrequencyInHz ) {
    	this.xLengthInMs = xLengthInMs;
    	this.yFrequencyInHz = yFrequencyInHz;
	}

    @Override
    public String toString() {

    	return "Voc start: " + getStartX() + " end: " + getEndX();
    }

	public void add(int x, int freqBinned ) {

//		this.xList.add( x );
//		this.yList.add( freqBinned );
		this.pointList.add( new Point( x, freqBinned ) );
	}

	public void add( Point point ) {

		this.pointList.add( point );
//		this.xList.add( point.x );
//		this.yList.add( point.y );

	}

	public Line2D getFitLine2D( int height )
	{
		Line2D line = new Line2D.Double( pA.getX(), height - 1 - pA.getY(), pB.getX(), height - 1 - pB.getY() );
		return line;
	}

	public ArrayList<Polygon> cutInPolygonList( int height , ArrayList<Point> pointList )
	{
//		if ( getStartX() == 562 )
//		{
//			System.out.println( this );
//			for ( Point point : pointList )
//			{
//				System.out.println( point );
//			}
//		}

		ArrayList<Polygon> polygonList = new ArrayList<Polygon>();

		Polygon currentPoly = null ; // new Polygon();
		int lastXdrawn = -1;
		for ( Point point : pointList )
		{
			if ( lastXdrawn != -1 )
			{
				if ( Math.abs( point.x - lastXdrawn ) > 1 )
				{
					if ( currentPoly != null )
					{
						//polygonList.add( currentPoly );
						currentPoly = null;
					}
				}
			}
			if ( currentPoly == null )
			{
				currentPoly = new Polygon();
				polygonList.add( currentPoly );
			}
			currentPoly.addPoint( point.x, height - 1 - point.y );
			lastXdrawn = point.x;

		}

		// check 1 - length polygon ( that would not display), and add a fake vertical point.
		for ( Polygon poly : polygonList )
		{
			if ( poly.npoints == 1 )
			{
				poly.addPoint( poly.xpoints[0], poly.ypoints[0]+1);
			}
		}

//		if ( getStartX() == 562 )
//		{
//			System.out.println( polygonList.size() );
//		}

		return polygonList;


	}

	public ArrayList<Polygon> getPolygon(int height) {

		return cutInPolygonList( height, pointList );



//		ArrayList<Polygon> polygonList = new ArrayList<Polygon>();
//
//		Polygon currentPoly = null ; // new Polygon();
//		int lastXdrawn = -1;
//		for ( Point point : pointList )
//		{
//			if ( lastXdrawn != -1 )
//			{
//				if ( Math.abs( point.x - lastXdrawn ) > 1 )
//				{
//					if ( currentPoly != null )
//					{
//						polygonList.add( currentPoly );
//						currentPoly = null;
//					}
//				}
//			}
//			if ( currentPoly == null )
//			{
//				currentPoly = new Polygon();
//			}
//			currentPoly.addPoint( point.x, height - 1 - point.y );
//			lastXdrawn = point.x;
//
//		}
//
//		return polygonList;
	}

	public ArrayList<Polygon> getPolygonHarmonics(int height) {

		return cutInPolygonList( height, pointListHarmonics );

//		Polygon poly = new Polygon();
//		for ( Point point : pointListHarmonics )
//		{
//			poly.addPoint( point.x, height - 1 - point.y );
//		}
//
//		return poly;
	}

	public ArrayList<Point> getPointList() {
		return pointList;
	}

	int min( ArrayList<Integer> array )
	{
		int min = Integer.MAX_VALUE;
		for ( int value : array )
		{
			if ( value < min ) min = value;
		}
		return min;
	}

	int max( ArrayList<Integer> array )
	{
		int max = Integer.MIN_VALUE;
		for ( int value : array )
		{
			if ( value > max ) max = value;
		}
		return max;
	}



	public int getMaxY( boolean withHarmonics )
	{
		int max = Integer.MIN_VALUE;
		for ( Point point : pointList )
		{
			if ( point.y > max )
			{
				max = point.y;
			}
		}

		if ( withHarmonics )
		{
			for ( Point point : pointListHarmonics )
			{
				if ( point.y > max )
				{
					max = point.y;
				}
			}
		}

		return max;
	}

	public int getMinY( boolean withHarmonics )
	{
		int min = Integer.MAX_VALUE;
		for ( Point point : pointList )
		{
			if ( point.y < min )
			{
				min = point.y;
			}
		}

		if ( withHarmonics )
		{
			for ( Point point : pointListHarmonics )
			{
				if ( point.y < min )
				{
					min = point.y;
				}
			}
		}

		return min;
	}

	public double getAmplitudeInPixel() {
		int min = getMinY( false );
		int max = getMaxY( false );
		return max-min;
	}

	public ArrayList<String> getClassificationDescription() {
		return classificationDescription;
	}

	public int getCenterX() {

		return ( getStartX()+getEndX() ) / 2;
	}

	public float getMeanY() {

		float sum=0;
		for ( Point point : pointList )
		{
			sum+=point.y;
		}
		float mean = sum;
		if ( pointList.size() > 0 )
		{
			mean = sum/pointList.size();
		}
		return mean;
	}

	public float getMeanFrequencyInHz()
	{
		return getFrequencyInHz( getMeanY() );
	}


    void addClassificationDescription( String description )
    {
    	classificationDescription.add( description );
    }

    void removeClassificationDescription( String description )
    {
    	classificationDescription.remove( description );
    }

	public boolean switchClassificationDescription(String string) {
		boolean found = false;
		for ( String str : classificationDescription )
		{
			if ( str.equals( string ) )
			{
				found = true;
			}
		}
		if ( found )
		{
			removeClassificationDescription( string );
			return false;
		}
		else
		{
			addClassificationDescription( string );
			return true;
		}
	}


	public float getDurationInMs() {
		return xLengthInMs * getLengthX();
	}

	public float getFrequencyDynamicInHz() {
		int minY = getMinY( false );
		int maxY = getMaxY( false );
		return (maxY-minY) * yFrequencyInHz;
	}

	public float getMinFrequencyInHz() {
		int minY = getMinY( false );
		return getFrequencyInHz( minY );
	}

	public float getMaxFrequencyInHz() {
		int maxY = getMaxY( false );
		return getFrequencyInHz( maxY );
	}

	public float getStartFrequencyInHz()
	{
//		System.out.println( "startX:" + getStartX() );
		return getFrequencyInHz( getPointAt( getStartX() ).y );
	}

	public float getEndFrequencyInHz()
	{
		return getFrequencyInHz( getPointAt( getEndX() ).y );
	}

	public Point getPointAt( int x ) {

		for ( Point p : pointList )
		{
			if ( p.x == x ) return p;
		}
		return null;
	}

	public void removePoint( Point p )
	{
		pointList.remove( p );
	}

	public void removePointAtX( int x )
	{
		pointList.remove( getPointAt( x ) );
	}


	public float getFrequencyInHz(float y) {

		return yFrequencyInHz * y ;

	}

	public int getStartX() {
		int minX = Integer.MAX_VALUE;
		for ( Point p : pointList )
		{
			if ( p.x < minX ) minX = p.x;
		}
		return minX;
	}

	double timeOffsetMs = 0; // Used when part of voc is computed, shift the voc in time.

	public void setTimeOffsetMs(double timeOffsetMs) {
		this.timeOffsetMs = timeOffsetMs;
	}

	public float getStartInMs() {
		return (float)( xLengthInMs * getStartX() + timeOffsetMs) ;
	}


	public int getEndX() {
		int maxX = Integer.MIN_VALUE;
		for ( Point p : pointList )
		{
			if ( p.x > maxX ) maxX = p.x;
		}
		return maxX;
	}

	public float getEndInMs() {
		return xLengthInMs * getEndX();
	}

	public int getLengthX() {
		return getEndX() - getStartX();
	}


	/** Assume that Vocs come from the same spectrogram. */
	public static Voc fuseVoc( Voc vocA, Voc vocB ) {

		Voc voc = new Voc( vocA.xLengthInMs, vocA.yFrequencyInHz );

		for ( Point p : vocA.getPointList() )
		{
			voc.add( p );
		}

		for ( Point p : vocB.getPointList() )
		{
			voc.add( p );
		}

		return voc;

	}

	public boolean areAllValuesTheSame() {

		if ( pointList.size() > 1 )
		{
			int val = pointList.get( 0 ).y;
			for ( Point p : pointList )
			{
				if ( p.y != val  ) return false;
			}
		}

		return true;

	}

	/** compute the sum of all consecutive freq.
	 * */
	public float getMeanFrequencyTVInHz() {

		float total = getMeanFrequencyTVInPix();
		total *= yFrequencyInHz;

		return total;
	}

	/** compute the sum of all consecutive freq.
	 * */
	public float getFrequencyTVInPix() {

		float total = 0;
		for ( int i = 0 ;i < pointList.size()-1 ; i++ )
		{
			total+= (float)Math.abs( pointList.get( i ).y - pointList.get( i+1 ).y );
		}

		return total;
	}



	/** compute the sum of all consecutive freq.
	 * */
	public float getFrequencyTVInHz() {

		float total = getFrequencyTVInPix();
		total *= yFrequencyInHz;

		return total;
	}


	/** compute the sum of all consecutive freq.
	 * return the value / numberOfValues.
	 * */
	public float getMeanFrequencyTVInPix() {

		float total = 0;
		for ( int i = 0 ;i < pointList.size()-1 ; i++ )
		{
			total+= (float)Math.abs( pointList.get( i ).y - pointList.get( i+1 ).y );
		}

		if ( pointList.size() > 1 )
		{
			total /= pointList.size();
		}

		return total;
	}

	public boolean containsHarmonics() {
		if ( pointListHarmonics.size() > 0 )
		{
			return true;
		}
		return false;
	}

	public boolean contain(int x, int y , int minYdistance ) {

		for ( Point p : pointList )
		{
			if ( Math.abs( p.x - x ) < 2 && Math.abs( p.y - y ) < minYdistance )
			{
				return true;
			}
		}
		return false;


	}

	public Rectangle2D getBoundingRectangle( float height , boolean withHarmonics , int borderSize ) {

		Rectangle2D rectangle = new Rectangle2D.Double(
				getStartX()-borderSize, height - 1 - getMaxY( withHarmonics )-borderSize,
				getEndX()-getStartX()+borderSize*2, getMaxY( withHarmonics ) -getMinY( withHarmonics ) + borderSize*2 );

		return rectangle;
	}







}
