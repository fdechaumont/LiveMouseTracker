/**
  	@author Fabrice de Chaumont
 	copyright Fabrice de Chaumont @ Institut Pasteur

 	This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package plugins.fab.livemousetracker;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.io.StringWriter;
import java.text.DecimalFormat;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import icy.gui.util.ComponentUtil;
import icy.image.IcyBufferedImage;
import icy.image.ImageUtil;
import icy.resource.ResourceUtil;
import icy.system.profile.Chronometer;
import icy.system.thread.InstanceProcessor;
import icy.system.thread.SingleInstanceProcessor;
import icy.type.point.Point3D;
import plugins.fab.livemousetracker.detection.MouseDetection;
import plugins.kernel.roi.roi2d.ROI2DArea;

public class Util {

	/* Norm vector to length l */
	public static Point2D normVector( Point2D p , double l )
	{
		double distance = p.distance( 0, 0 );
		return new Point2D.Double( l * ( p.getX() / distance ) , ( p.getY() / distance ) * l );
	}

	/**
	 * Returns vector p1->p2
	 * */
	public static Point2D createVector(Point2D p1, Point2D p2) {

		return new Point2D.Double( p2.getX() - p1.getX() , p2.getY() - p1.getY() );

	}

	public static double getZ( Point2D p )
	{
		BackgroundHeightMapBuilder backgroundHeightMapBuilder = LiveMouseTracker.getBackgroundHeightMapBuider();
		IcyBufferedImage depthImage = LiveMouseTracker.depthImage;
		return backgroundHeightMapBuilder.getVolume( depthImage, p );
	}

	public static short[] getZAsBuffer( Rectangle r )
	{
		BackgroundHeightMapBuilder backgroundHeightMapBuilder = LiveMouseTracker.getBackgroundHeightMapBuider();
		IcyBufferedImage depthImage = LiveMouseTracker.depthImage;
		return backgroundHeightMapBuilder.getRectangleVolume( depthImage, r );
	}

	/** provide the mean volume for a given point, restricted to the mask of the detection,
	 * considering a radius.
	 * */
	public static double getMeanZ( Point2D p , int radius , ROI2DArea mask ) {

		BackgroundHeightMapBuilder backgroundHeightMapBuilder = LiveMouseTracker.getBackgroundHeightMapBuider();
		IcyBufferedImage depthImage = LiveMouseTracker.depthImage; //LiveMouseTracker.getDepthOut().getImage( 0 , 0 );

		// temp: return max value;
//		double max = 0;
		double pZ = 0;
		double nbPoint = 0;
		Point2D point = new Point2D.Double();
		if ( p != null )
		{
			for ( int x = -radius ; x<=radius ; x++ )
			{
				for ( int y = -radius ; y<=radius ; y++ )
				{
					point.setLocation( p.getX()+x , p.getY() +y );

					if ( mask.contains( point ) )
					{
						double vol = backgroundHeightMapBuilder.getVolume( depthImage, point );
						pZ += vol;
						nbPoint++;
//						if ( vol > max ) max = vol;
					}
				}
			}
		}
		if ( nbPoint > 0 )
		{
			pZ /=nbPoint;
		}

//		return max;
		return pZ;
	}

	public static void setZ( Point3D p , int radius , ROI2DArea mask ) {
		double z = getMeanZ( p.toPoint2D() , radius, mask );
		p.setZ( z );
	}

	public static ImageIcon getIcon( String name )
	{
		return ResourceUtil.getImageIcon( ImageUtil.load(
				Util.class.getClassLoader().getResourceAsStream(name) ) );
	}

	public static JLabel getIconAsLabel( String name )
	{
		ImageIcon icon = getIcon( name);
		JLabel label = new JLabel( icon );
		ComponentUtil.setFixedSize( label , new Dimension( icon.getIconWidth() , icon.getIconHeight() ) );
		label.setPreferredSize( new Dimension( 200 , 200 ) );

		return label;
	}

	public static JButton getIconAsButton( String name )
	{
		ImageIcon icon = getIcon( name);
		JButton button = new JButton( icon );

//		button.setRolloverIcon(
//		getIcon( clazz , "plugins/fab/livemousetracker/launcher/logo Live Tracking Live Capture.png" ) );
		ComponentUtil.setFixedSize( button , new Dimension( icon.getIconWidth() , icon.getIconHeight() ) );
		button.setPreferredSize( new Dimension( 200 , 200 ) );

		return button;
	}

	/** Save from string */
	public static String XMLDocumentToString( Document document )
	{
		try {
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer;
			transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			StringWriter writer = new StringWriter();
			transformer.transform(new DOMSource(document), new StreamResult(writer));
			String output = writer.getBuffer().toString(); //.replaceAll("\n|\r", "");
			return output;
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}

		return null;
	}

	/** convert in 00:00:00.00 @ 30fps */
	public static String getTimeStamp( long frame )
	{

		int hour = (int) (frame / 60 / 60 / 30);
		frame -= hour * 60 * 60 * 30;

		int minute = (int) (frame / 60 / 30);
		frame -= minute * 60 * 30;

		int second = (int) (frame / 30 );
		frame -= second * 30;

		int f = (int) frame;

		DecimalFormat df = new DecimalFormat( "00" );

		String ts = df.format( hour ) + ":" + df.format( minute ) + ":" + df.format( second ) + "." + df.format( f );

		return ts;
	}

	/**
	 * project AC on AB and find projected point D.
	 */
	static public Point2D project( Point2D a , Point2D b, Point2D c )
	{
		// project AC on AB and find projected point D.
		// ref: http://stackoverflow.com/questions/23772990/find-point-with-vector-projection
		//	CF=((B.X-A.X)*(C.X-A.X)+(B.Y-A.Y)*(C.Y-A.Y))/((B.X-A.X)^2+(B.Y-A.Y)^2)
		//			D.X=A.X+(B.X-A.X)*CF
		//			D.Y=A.Y+(B.Y-A.Y)*CF


		double cf =
				((b.getX()-a.getX())*(c.getX()-a.getX())+(b.getY()-a.getY())*(c.getY()-a.getY()))/
				( Math.pow( b.getX()-a.getX(),2) + Math.pow( b.getY()-a.getY(),2 ) );
		Point2D d = new Point2D.Double(
				a.getX()+(b.getX()-a.getX() )*cf,
				a.getY()+(b.getY()-a.getY() )*cf );

		return d;
	}

	/**
	 * find ratio of projected point
	 * considering vector AC and AB. Project C on AB on point D.
	 * Ratio will be: AB * ratio = AD;
	 **/
	public static double vectorProjectRatio(Point2D a, Point2D b, Point2D c ) {

		double cf =
				((b.getX()-a.getX())*(c.getX()-a.getX())+(b.getY()-a.getY())*(c.getY()-a.getY()))/
				( Math.pow( b.getX()-a.getX(),2) + Math.pow( b.getY()-a.getY(),2 ) );
		return cf;
	}

	public static double getScalarProduct( Point2D a, Point2D b ) {
		return a.getX() * b.getX() + a.getY() * b.getY();
	}

	public static boolean isSameSign(double a, double b ) {
		if ( a >= 0 && b >= 0 ) return true;
		if ( a < 0 && b < 0 ) return true;
		return false;
	}

	public static boolean isSameSign(double a, double b, double c ) {
		if ( a >= 0 && b >= 0 && c>=0 ) return true;
		if ( a < 0 && b < 0 && c<0 ) return true;
		return false;
	}

	public static Thread runSingle2( String name, Thread thread, Runnable runnable) {

		return runSingle2( name, thread, runnable, LiveMouseTracker.SECONDARY_THREAD_PRIORITY );


	}


	public static Thread runSingle2(String name, Thread thread, Runnable runnable,
			Integer priority ) {

		if ( thread == null || !thread.isAlive() )
		{
			Chronometer chronoSingle = new Chronometer("CHRONO SINGLE:" +name );
			thread = new Thread( runnable );
			thread.setName(name );
			thread.setPriority( priority );
			thread.start();
			System.out.println("UTIL RUN SINGLE START: " + name );
			chronoSingle.displayMs();
		}
		return thread;

	}

	public static double getMinDistance( MouseDetection detectionA , MouseDetection detectionB )
	{
		if ( detectionA != null && detectionB != null )
		{
			double minDistance = Float.MAX_VALUE;

			Point[] detectionAPointList = detectionA.getROI2DArea().getBooleanMask().getContourPoints();
			Point[] detectionBPointList = detectionB.getROI2DArea().getBooleanMask().getContourPoints();

			for ( Point pA : detectionAPointList )
			{
				for ( Point pB : detectionBPointList )
				{
					double dist = pA.distanceSq( pB );
					if ( dist < minDistance )
					{
						minDistance = dist;
					}
				}
			}
			minDistance = Math.sqrt( minDistance );
			return minDistance;
		}
		return Double.MAX_VALUE;
	}

	public static Point2D getMassCenter(ROI2DArea detectionAnext) {

		float x = 0;
		float y = 0;

		int nbPoint = detectionAnext.getBooleanMask( true ).getPoints().length;
		for ( Point point: detectionAnext.getBooleanMask( true ).getPoints() )
		{
			x+= point.x;
			y+= point.y;
		}

		x/= nbPoint;
		y/= nbPoint;

		return new Point2D.Double( x ,  y );
	}


}
