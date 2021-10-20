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

import icy.util.GraphicsUtil;
import plugins.fab.livemousetracker.LiveMouseTracker;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;

import javax.swing.Timer;

public class Event {

	private String text;
	private Color color;
	private Point2D locationEvent;
	private Point2D locationText;

	private Point2D inertiaVector ;
	private Point2D speedVector ;
	private Point2D accelVector ;

	// Disappear
	boolean canRemove = false;
	float alpha = 2;

	boolean canDelete = false;

	public void setCanRemove(boolean canRemove) {
		this.canRemove = canRemove;
	}


	public boolean canDelete()
	{
		return canDelete;
	}

//	public Event( String text , Color color , Point2D location ) { //, float seconds ) {
//		this ( text , color, location );
//
//		if ( !LiveMouseTracker.DISPLAY_MESSAGE_EVENT )
//		{
//			canDelete = true;
//			return;
//		}
//	}

	public void startCounter() {
		Timer timer = new Timer( (int)(2*1000) , new ActionListener() { // 2 seconds
			@Override
			public void actionPerformed(ActionEvent e) {
				setCanRemove( true );
			}
		} );
		timer.start();
	}

	public Event( String text , Color color , Point2D location ) {

//		if ( !LiveMouseTracker.DISPLAY_MESSAGE_EVENT )
//		{
//			canDelete = true;
//			return;
//		}

		setText( text );
		this.color = color;
		this.locationEvent = location;

		inertiaVector = new Point2D.Double( Math.random() , Math.random() );
		speedVector = new Point2D.Double( 0 , 0 );
		accelVector = new Point2D.Double( 0 , 0 );

	}

	public Rectangle getBounds( Graphics2D g ) {

		return GraphicsUtil.getHintBounds( g, text, (int)locationText.getX(), (int)locationText.getY() );

	}

	public Point2D getInertiaVector() {
		return inertiaVector;
	}
	public void setInertiaVector(Point2D inertiaVector) {
		this.inertiaVector = inertiaVector;
	}
	public Point2D getSpeedVector() {
		return speedVector;
	}
	public void setSpeedVector(Point2D speedVector) {
		this.speedVector = speedVector;
	}

	public void setLocationText(Point2D locationText) {
		this.locationText = locationText;
	}

	public Point2D getLocationText() {
		return locationText;
	}

	public Color getColor() {
		return color;
	}

	public String getText() {
		return text;
	}

	public Point2D getLocationEvent() {
		return locationEvent;
	}

	public void setAccelVector(Point2D accelVector) {
		this.accelVector = accelVector;
	}

	public void computePhysics() {

		inertiaVector.setLocation(
				inertiaVector.getX() + accelVector.getX(),
				inertiaVector.getY() + accelVector.getY() );

		locationText.setLocation(
				inertiaVector.getX() + locationText.getX(),
				inertiaVector.getY() + locationText.getY() );

		inertiaVector.setLocation( inertiaVector.getX() / 2f , inertiaVector.getY() / 2f );

		if ( canRemove )
		{
			alpha -=0.1f;
			if ( alpha < 0 )
			{
				canDelete = true;
			}
		}

	}

	public float getAlpha()
	{
		if ( alpha >1 ) return 1;
		if ( alpha <0 ) return 0;
		return alpha;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public void setText(String text) {
		this.text = text;
		System.out.println( "[EVENT] " + text );
	}

}
