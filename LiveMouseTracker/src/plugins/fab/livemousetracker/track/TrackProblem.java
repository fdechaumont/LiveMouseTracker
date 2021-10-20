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
package plugins.fab.livemousetracker.track;

import java.awt.Color;
import java.awt.geom.Point2D;

import plugins.fab.livemousetracker.overlay.Event;

public class TrackProblem extends Event {

	private Point2D problemLocation = null;
	private int t;

	public enum State {
		PB_SOLVED ,
		PB_UNSOLVED, // default
		PB_CANNOT_BE_SOLVED, // impossible to solve this problem.
		PB_DELAYED // delayed, will be solved later.
		};

	State state = State.PB_UNSOLVED;
	//private boolean solved = false;
	private TrackSegment trackSegment;

	public TrackProblem(String name , Point2D problemLocation , int t , TrackSegment trackSegment ) {

		super( name , Color.white , problemLocation );

		this.problemLocation = problemLocation;
		this.t = t;
		this.trackSegment = trackSegment;

		System.out.println( this );
	}

	public TrackSegment getTrackSegment() {
		return trackSegment;
	}

	/*
	@Override
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas) {

		g.setColor( Color.WHITE );
		g.drawString( name , (int)problemLocation.getX() , (int)problemLocation.getY() );

		g.drawRect( (int)problemLocation.getX()-1 , (int)problemLocation.getY()-1,3,3 );

	}*/

	public Point2D getProblemLocation() {
		return problemLocation;
	};

	public int getT()
	{
		return t;
	}

	public void solved() {
		state = State.PB_SOLVED;
	}

	public boolean isSolved() {
		return ( state == State.PB_SOLVED );
	}

	@Override
	public String toString() {

		return getText() + "trackProblem t: " + t;

	}

	public boolean isDelayed()
	{
		return ( state == State.PB_DELAYED );
	}

	public void setDelayed() {

		state = State.PB_DELAYED;
	}



}
