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
package plugins.fab.livemousetracker.liveanalysis.chronogram;

import plugins.fab.livemousetracker.Animal;
import plugins.fab.livemousetracker.detection.MouseDetection;


public class ChronoStop extends ChronoFeatureAbstract implements ProcessAnimalSingleFeature {

	public ChronoStop() {
		super ( "Stop" , "Animal X is stopped" , TimeLineDataType.BOOLEAN );
	}

	ChronoMouseSpeed chronoSpeedA = null;

	public void setChronoSpeedA(ChronoMouseSpeed chronoSpeedA) {
		this.chronoSpeedA = chronoSpeedA;
	}

	public void process( Animal animal , Integer frameStart, Integer frameEnd )
	{
//		this.description = "Animal " + animal.getName() + " is stopped";

		EventTimeLine speedTimeLine = chronoSpeedA.getEventTimeLine();

		eventTimeLine = new EventTimeLine( this.getName() , timeLineDataType );
		int tMax = animal.getLastTimePoint();

		if ( chronoSpeedA == null )
		{
			chronoSpeedA = new ChronoMouseSpeed();
			chronoSpeedA.process( animal , frameStart, frameEnd );
		}

		if( frameEnd == null ) frameEnd = tMax;
		if( frameStart == null ) frameStart = 0;

		for ( int t = frameStart ; t < frameEnd ; t++ )
		{
			Double speed = speedTimeLine.getValueAt( t );
			if ( speed == null ) continue;

			if ( speed < ChronoConstant.STOP_SPEED_THRESHOLD )
			{
				eventTimeLine.addPunctualEvent( t );
			}
		}

	}

}
