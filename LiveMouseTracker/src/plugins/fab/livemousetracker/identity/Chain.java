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
package plugins.fab.livemousetracker.identity;

import java.util.ArrayList;

import plugins.fab.livemousetracker.Animal;
import plugins.fab.livemousetracker.LiveMouseTracker;

public class Chain {

	//ArrayList<Animal> animalList = new ArrayList<Animal>( );
	ArrayList<TrackIdentityProblemToAnimal> tipToAnimalList = new ArrayList<TrackIdentityProblemToAnimal>( );
	boolean chainValid = true;

	/** Create a chain based on existing chain, copy the source content and add the given animal */
	public Chain( Chain chain, TrackIdentityProblemToAnimal candidateTipToAnimal ) {

		for ( TrackIdentityProblemToAnimal tipToAnimal : chain.getTipToAnimalList() )
		{
			if ( tipToAnimal.animal == candidateTipToAnimal.animal  )
				{
					chainValid = false;
					break;
				}
		}

		/*
		if ( chain.getTipToAnimalList().contains( animal ) )
		{
			// the animal already exist, invalid the chain.
			chainValid = false;
			return;
		}*/

		tipToAnimalList = new ArrayList<TrackIdentityProblemToAnimal>( chain.getTipToAnimalList() );
		tipToAnimalList.add( candidateTipToAnimal );

	}

	/** constructor used to init the primary chain. */
	public Chain() {

	}

	/** FIXME: rename it ! */
	public ArrayList<TrackIdentityProblemToAnimal> getTipToAnimalList() {
		return tipToAnimalList;
	}

	public boolean isChainValid()
	{
		return chainValid;
	}

	Double score = null ;

	public Double getScore() {
		return score;
	}

	public void computeScore()
	{
		// compute score for chain
		ArrayList<Double> values = new ArrayList<Double>();
		for ( TrackIdentityProblemToAnimal tipToAnimal : tipToAnimalList  )
		{
			int animalIndex = LiveMouseTracker.getMainAnimalPool().getAnimalList().indexOf( tipToAnimal.animal );
			if ( LiveMouseTracker.LOG_CHAIN )
			{
				System.out.println( "Animal: " + tipToAnimal.animal.getName() + " Index: " + animalIndex );
			}
			//double[] scoreArray = tipToAnimal.tip.scoreList.get( animalIndex );
			for ( double[] scoreArray : tipToAnimal.tip.scoreList )
			{
				values.add( scoreArray[animalIndex] );
			}
		}

		Double score = null;
		for ( double value : values )
		{
			if ( score == null )
			{
				score = value;
			}else
			{
				score *= value;
			}
		}

		this.score = score;
	}

	@Override
	public String toString() {

		String str = "";
		for ( TrackIdentityProblemToAnimal tipToAnimal : tipToAnimalList )
		{
			str+=tipToAnimal.animal.getName() + " ";
		}

		str+=" score: " +score;
		return str;

	}

}
