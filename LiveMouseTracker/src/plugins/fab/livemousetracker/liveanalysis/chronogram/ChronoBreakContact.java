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


public class ChronoBreakContact extends ChronoFeatureAbstract implements ProcessAnimalPairFeature {

	public ChronoBreakContact() {
		super ( "Break contact" , "(2nd order) A and B are in contact."
				+ " Aspeed >BSpeed and gets to no contact with increasing distance between animals" , TimeLineDataType.BOOLEAN );
	}

	ChronoContact chronoContactAB = null;
	public void setChronoContactAB(ChronoContact chronoContactAB) {
		this.chronoContactAB = chronoContactAB;
	}

	ChronoEscape chronoEscapeAfromB = null;
	public void setChronoEscapeAfromB(ChronoEscape chronoEscapeAfromB) {
		this.chronoEscapeAfromB = chronoEscapeAfromB;
	}

	public void process( Animal animalA , Animal animalB , Integer frameStart, Integer frameEnd )
	{
//		description = animalA.getName() + " and " + animalB.getName() + " are in contact";
//		description+= " " + animalA.getName() + " is faster than " + animalB.getName();
//		description+= " gets to no contact";

		if ( chronoContactAB == null )
		{
			chronoContactAB = new ChronoContact();
			chronoContactAB.process( animalA , animalB, frameStart, frameEnd );
		}
		EventTimeLine contactTimeLineA = chronoContactAB.getEventTimeLine();

		if ( chronoEscapeAfromB == null )
		{
			chronoEscapeAfromB = new ChronoEscape();
			chronoEscapeAfromB.process( animalA , animalB , frameStart , frameEnd );
		}
		EventTimeLine escapeAfromBTimeLine = chronoEscapeAfromB.getEventTimeLine();

		eventTimeLine = new EventTimeLine( this.getName() , timeLineDataType );

		// Check each start of contact event and try to find event from it.
		for ( Event contactEvent : contactTimeLineA.getBooleanEventList() )
		{
			int startFrame = contactEvent.endFrame;
			Event event = escapeAfromBTimeLine.getEventAt( startFrame+1 );

			if ( event != null )
			{
				if ( !contactEvent.contain( event.endFrame ) )
				{
					eventTimeLine.addEvent( event );
				}
			}
		}
	}



}


