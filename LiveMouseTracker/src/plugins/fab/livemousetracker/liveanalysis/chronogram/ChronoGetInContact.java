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


public class ChronoGetInContact extends ChronoFeatureAbstract implements ProcessAnimalPairFeature {

	public ChronoGetInContact() {
		super ( "Get in contact" , "(2nd order) A get in contact to B. Aspeed >BSpeed no contact to contact with decreasing distance between animals" , TimeLineDataType.BOOLEAN );
	}

	ChronoContact chronoContactAB = null;
	ChronoApproach chronoAapproachB = null;

	public void setChronoAapproachB(ChronoApproach chronoAapproachB) {
		this.chronoAapproachB = chronoAapproachB;
	}

	public void setChronoContactAB(ChronoContact chronoContactAB) {
		this.chronoContactAB = chronoContactAB;
	}

	public void process( Animal animalA , Animal animalB , Integer startFrame, Integer endFrame )
	{
//		description = animalA.getName() + " gets in contact with " + animalB.getName()+".";
//		description +=" " + animalA.getName() + " is faster than " + animalB.getName() ;

		if ( chronoContactAB == null )
		{
			chronoContactAB = new ChronoContact();
			chronoContactAB.process( animalA, animalB, startFrame , endFrame );
		}
		EventTimeLine contactTimeLineA = chronoContactAB.getEventTimeLine();

//		ChronoContact chronoContactB = new ChronoContact();
//		chronoContactB.process( animalB );
//		EventTimeLine contactTimeLineB = chronoContactB.getEventTimeLine();

//		ChronoDistanceBetweenAnimal chronoDistanceA = new ChronoDistanceBetweenAnimal();
//		chronoDistanceA.process( animalA );
//		EventTimeLine distanceTimeLineA = chronoDistanceA.getEventTimeLine();
//
//		ChronoDistanceBetweenAnimal chronoDistanceB = new ChronoDistanceBetweenAnimal();
//		chronoDistanceB.process( animalB );
//		EventTimeLine distanceTimeLineB = chronoDistanceB.getEventTimeLine();

//		ChronoMouseSpeed chronoSpeedA = new ChronoMouseSpeed();
//		chronoSpeedA.process( animalA );
//		EventTimeLine speedTimeLineA = chronoSpeedA.getEventTimeLine();
//
//		ChronoMouseSpeed chronoSpeedB = new ChronoMouseSpeed();
//		chronoSpeedA.process( animalB );
//		EventTimeLine speedTimeLineB = chronoSpeedB.getEventTimeLine();

		if ( chronoAapproachB == null )
		{
			chronoAapproachB = new ChronoApproach();
			chronoAapproachB.process( animalA , animalB , startFrame, endFrame );
		}
		EventTimeLine approachAtoBTimeLine = chronoAapproachB.getEventTimeLine();

		eventTimeLine = new EventTimeLine( this.getName() , timeLineDataType );

		// Check each start of contact event and try to find event from it.
		for ( Event contactEvent : contactTimeLineA.getBooleanEventList() )
		{
			int startFrame2 = contactEvent.startFrame;
			Event event = approachAtoBTimeLine.getEventAt( startFrame2-1 );

			if ( event != null )
			{
				if ( contactEvent.contain( event.startFrame ) )
				{
					eventTimeLine.addEvent( event );
				}
			}
		}
	}



}


