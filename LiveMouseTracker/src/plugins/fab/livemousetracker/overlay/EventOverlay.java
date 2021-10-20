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
package plugins.fab.livemousetracker.overlay;

import icy.canvas.IcyCanvas;
import icy.painter.Overlay;
import icy.sequence.Sequence;
import icy.util.GraphicsUtil;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import plugins.fab.livemousetracker.LiveMouseTracker;
import plugins.fab.livemousetracker.track.TrackProblem;


public class EventOverlay extends Overlay {

	//event list
	private ArrayList<Event> eventList = new ArrayList<Event>();
	// constraints
	Point2D center;
	float ray;

	public EventOverlay(String name) {
		super(name);
	}

	public void setConstraint( Point2D center , float ray )
	{
		this.center = center;
		this.ray = ray;
	}

	public void addEvent( Event event )
	{
		if ( !LiveMouseTracker.DISPLAY_MESSAGE_EVENT ) return;

		event.startCounter();

		Point2D locationEvent = event.getLocationEvent();

		Point2D vector = new Point2D.Double(
				locationEvent.getX() - center.getX(),
				locationEvent.getY() - center.getY() );

		// normalize
		double dist = vector.distance( 0 , 0 );
		vector.setLocation( vector.getX() / dist , vector.getY() / dist );
		// mul
		vector.setLocation( center.getX() + vector.getX() *ray , center.getY() + vector.getY() * ray );

		event.setLocationText( vector );

		synchronized (eventList ) {
			eventList.add( event );
		}


	}

	@Override
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas) {

		super.paint(g, sequence, canvas);

		g =(Graphics2D) g.create();

		computePhysics( g );

		Font font = new Font( "Arial" , Font.BOLD , 8 );
		g.setFont( font );

		BasicStroke small = new BasicStroke( 2 );
		BasicStroke big = new BasicStroke( 3 );

		for ( Event event : new ArrayList<Event>( eventList ) )
		{
			if ( event.canDelete )
			{
				synchronized ( eventList ) {
					eventList.remove( event );
				}
				continue;
			}
			try
			{

				g.setComposite(AlphaComposite.getInstance( AlphaComposite.SRC_OVER , event.getAlpha() ));

				Rectangle bounds = event.getBounds( g );
				Line2D line = new Line2D.Double(
						event.getLocationEvent(),
						new Point2D.Double( bounds.getCenterX(), bounds.getCenterY() )
						);

				Point2D p = getIntersectionPoint(line, bounds );
				if ( p != null )
				{
					line.setLine( p , event.getLocationEvent() );
				}

				g.setStroke( big );
				g.setColor( Color.black );
				g.draw( line );
				g.setStroke( small );
				g.setColor( event.getColor() );
				g.draw( line );

				g.drawRect(
						(int)event.getLocationEvent().getX()-1 ,
						(int)event.getLocationEvent().getY()-1
						, 3,3 );

				GraphicsUtil.drawHint( g, event.getText(),
						(int)event.getLocationText().getX(),
						(int)event.getLocationText().getY(),
						event.getColor().darker(), event.getColor() );

				if ( event instanceof TrackProblem )
				{
					/*
				TrackProblem tp = (TrackProblem) event;
				if ( tp.isDelayed() )
				{
					g.drawString("d",
							(int)event.getLocationText().getX() ,
							(int)event.getLocationText().getY() );
				}
					 */
				}
			}
			catch ( Exception e )
			{
				System.err.println("Error in eventOverlay paint.");
			}

		}

	}

	private void computePhysics( Graphics2D g ) {

		ArrayList<Event> eventListCopy;
		synchronized ( eventList ) {
			eventListCopy = new ArrayList<Event>( eventList );
		}
		for ( Event event : eventListCopy )
		{
			Point2D accelVector = new Point2D.Double( 0 , 0 );
			Rectangle eventBound = event.getBounds( g );
			for ( Event event2 : eventListCopy )
			{
				if ( event != event2 )
				{
					Rectangle event2Bound = event2.getBounds( g );
					if ( eventBound.intersects( event2Bound ) )
					{
						Point2D vector = new Point2D.Double(
								eventBound.getCenterX() - event2Bound.getCenterX(),
								eventBound.getCenterY() - event2Bound.getCenterY()
								);

						vector.setLocation( vector.getX() / 20d , vector.getY() / 20d );

						accelVector.setLocation(
								accelVector.getX() + vector.getX(),
								accelVector.getY() + vector.getY()
								);
					}
					event.getText();
				}
			}

			// try to be out of cage
			{
				Point2D centerEvent = new Point2D.Double( eventBound.getX() , eventBound.getY() );
				Point2D vectorToCenter = new Point2D.Double(
						centerEvent.getX() - center.getX(),
						centerEvent.getY() - center.getY() );

				double dist = vectorToCenter.distance( 0 , 0 );
				if ( dist < ray ) // push away if writing come in the field of the cage
				{
					// normalize
					vectorToCenter.setLocation( vectorToCenter.getX() / dist , vectorToCenter.getY() / dist );
					// mul
					double distance = ray - centerEvent.distance( this.center );
					vectorToCenter.setLocation(
							vectorToCenter.getX() *distance ,
							vectorToCenter.getY() * distance );

					if ( vectorToCenter.distance( 0 , 0 ) > 3 )
					{
						accelVector.setLocation(
								accelVector.getX() + vectorToCenter.getX() /20d ,
								accelVector.getY() + vectorToCenter.getY() /20d
								);
					}
				}
			}



			event.setAccelVector( accelVector );
			event.computePhysics();

		}

	}



    public Point2D getIntersectionPoint(Line2D line, Rectangle rectangle) {

    	Point2D p = null;
        // Top line

        p = getIntersectionPoint(line,
                        new Line2D.Double(
                        rectangle.getX(),
                        rectangle.getY(),
                        rectangle.getX() + rectangle.getWidth(),
                        rectangle.getY())) ;
        if ( p!=null ) return p;

        // Bottom line
        p =  getIntersectionPoint(line,
                        new Line2D.Double(
                        rectangle.getX(),
                        rectangle.getY() + rectangle.getHeight(),
                        rectangle.getX() + rectangle.getWidth(),
                        rectangle.getY() + rectangle.getHeight()));
        if ( p!=null ) return p;

        // Left side...
        p =  getIntersectionPoint(line,
                        new Line2D.Double(
                        rectangle.getX(),
                        rectangle.getY(),
                        rectangle.getX(),
                        rectangle.getY() + rectangle.getHeight()));
        if ( p!=null ) return p;

        // Right side
        p =  getIntersectionPoint(line,
                        new Line2D.Double(
                        rectangle.getX() + rectangle.getWidth(),
                        rectangle.getY(),
                        rectangle.getX() + rectangle.getWidth(),
                        rectangle.getY() + rectangle.getHeight()));

        return p;

    }

    public Point2D getIntersectionPoint(Line2D lineA, Line2D lineB) {

        double x1 = lineA.getX1();
        double y1 = lineA.getY1();
        double x2 = lineA.getX2();
        double y2 = lineA.getY2();

        double x3 = lineB.getX1();
        double y3 = lineB.getY1();
        double x4 = lineB.getX2();
        double y4 = lineB.getY2();

    //    Point2D p = null;

		double d = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
		if (d == 0)
			return null;
		double xi = ((x3 - x4) * (x1 * y2 - y1 * x2) - (x1 - x2)
				* (x3 * y4 - y3 * x4))
				/ d;
		double yi = ((y3 - y4) * (x1 * y2 - y1 * x2) - (y1 - y2)
				* (x3 * y4 - y3 * x4))
				/ d;
		if (x3 == x4) {
			if (yi < Math.min(y1, y2) || yi > Math.max(y1, y2))
				return null;
		}
		Point2D.Double p = new Point2D.Double(xi, yi);
		if (xi < Math.min(x1, x2) || xi > Math.max(x1, x2))
			return null;
		if (xi < Math.min(x3, x4) || xi > Math.max(x3, x4))
			return null;
		return p;

        /*
        double d = (int) ( (x1-x2)*(y3-y4) - (y1-y2)*(x3-x4) ); // check paral
        if (d == 0) return null;

        double xi = ((x3-x4)*(x1*y2-y1*x2)-(x1-x2)*(x3*y4-y3*x4))/d;
        double yi = ((y3-y4)*(x1*y2-y1*x2)-(y1-y2)*(x3*y4-y3*x4))/d;

        Point2D p = new Point2D.Double(xi,yi);
        if (xi < Math.min(x1,x2) || xi > Math.max(x1,x2)) return null;
        if (xi < Math.min(x3,x4) || xi > Math.max(x3,x4)) return null;
        return p;
        */

        /*
        double d = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        if (d != 0) {
            double xi = ((x3 - x4) * (x1 * y2 - y1 * x2) - (x1 - x2) * (x3 * y4 - y3 * x4)) / d;
            double yi = ((y3 - y4) * (x1 * y2 - y1 * x2) - (y1 - y2) * (x3 * y4 - y3 * x4)) / d;

            p = new Point2D.Double(xi, yi);

        }


         */

        //return p;
    }


}
