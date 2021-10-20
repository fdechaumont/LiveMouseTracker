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
package plugins.fab.livemousetracker.postprocessdatabase;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.sql.Connection;
import java.util.ArrayList;

import icy.system.profile.Chronometer;
import plugins.fab.livemousetracker.dataplayer.DBAnimal;
import plugins.fab.livemousetracker.liveanalysis.chronogram.Event;
import plugins.fab.livemousetracker.liveanalysis.chronogram.EventTimeLine;

public class DBAnimalRearing extends DBAnimal {

	public DBAnimalRearing(int id, String RFID) {
		super(id, RFID , null );
	}

	public EventTimeLine rearingTimeLine = null;
	public SimpleAnimalLocation sal;

	/** add x,y to the events. */
	public void enrichEvents(Connection connection) {

		Chronometer c = new Chronometer("Loading simple animal location");
		sal = new SimpleAnimalLocation(connection, id ); //, 0 , 36000 );
		c.displayInSeconds();

		ArrayList<Event> eventList = rearingTimeLine.getBooleanEventList();

		for ( Event event : eventList )
		{
			float x=0;
			float y=0;
			float nbValue = 0;
			for ( int t=event.getStartFrame() ; t<=event.getEndFrame() ; t++ )
			{
				Point2D p = sal.getPoint( t );
				if ( p!=null )
				{
					x+= p.getX();
					y+= p.getY();
					nbValue++;
				}
			}
			event.location = new Point2D.Double( x/nbValue ,  y/nbValue );
		}

	}

	int histoGlobal[] = new int[120];


	public void seekForMimic(DBAnimalRearing animalB) {

		ArrayList<Event> eventListA = rearingTimeLine.getBooleanEventList();
		ArrayList<Event> eventListB = animalB.rearingTimeLine.getBooleanEventList();

		int histo[] = new int[120];

		for ( Event eventA : eventListA )
		{
			Event eventB = getClosestSuccessingEvent( eventA , eventListB );
			if ( eventB == null ) continue;

			/*
			int dif = eventB.getStartFrame() - eventA.getStartFrame();
			if ( dif > 194 ) dif = 194;
			histo[dif/5]++;
			histoGlobal[dif/5]++;
			*/

			float distance = (float) eventB.location.distance( eventA.location );
			distance/=5;
			if ( distance > 119 ) distance = 119;
			histo[(int)(distance)]++;
			histoGlobal[(int)(distance)]++;

		}

		System.out.println("---");
		for ( int i = 0 ; i < histo.length ; i++ )
		{
			System.out.println( histo[i] );
		}

	}

	public void displayGlobalHisto()
	{
		System.out.println("--- Global histo");
		for ( int i = 0 ; i < histoGlobal.length ; i++ )
		{
			System.out.println( histoGlobal[i] );
		}
	}

	private Event getClosestSuccessingEvent( Event sourceEvent, ArrayList<Event> eventList ) {

		Event bestEvent = null;
		float bestDeltaTime = Float.MAX_VALUE;

		for ( Event event : eventList )
		{
//			if ( sourceEvent.getStartFrame()-event.getStartFrame() >30 ) continue;
			if ( event.getStartFrame() - sourceEvent.getStartFrame() >30 ) continue;

//			if ( sourceEvent.location.distance( event.location ) > 28.5 ) continue;

			if ( event.getStartFrame() < sourceEvent.getStartFrame() ) continue;

			float delta = event.getStartFrame() - sourceEvent.getStartFrame();
			if ( delta < bestDeltaTime )
			{
				bestDeltaTime = delta;
				bestEvent = event;
			}

		}

		return bestEvent;
	}





}
