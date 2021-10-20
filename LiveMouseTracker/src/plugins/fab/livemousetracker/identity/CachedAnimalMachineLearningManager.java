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
import weka.classifiers.Classifier;

public class CachedAnimalMachineLearningManager {

	static ArrayList<CachedAnimalMachineLearningClassifier> cachedList = new ArrayList<CachedAnimalMachineLearningClassifier>();

	static public CachedAnimalMachineLearningClassifier getCache( ArrayList<Animal> candidateAnimalList, boolean buildIfNotCached )
	{
		String txt ="";
		for( Animal animal: candidateAnimalList)
		{
			txt+= animal.getName()+ " ";
		}
		System.out.println("[CACHE ML] REQUEST for " + txt);

		// Remove old caches.

		synchronized ( cachedList ) {

			for ( CachedAnimalMachineLearningClassifier cachedClassifier : new ArrayList<CachedAnimalMachineLearningClassifier>( cachedList ) )
			{
				// remove cache
				if ( cachedClassifier.creationFrame < LiveMouseTracker.getT() - 30*60*2 ) // 2 minutes
				{
					System.out.println("[CACHE ML] removing cached classifier (too old):" + cachedClassifier );
					cachedList.remove( cachedClassifier );
				}
			}

			for ( CachedAnimalMachineLearningClassifier cachedClassifier : cachedList )
			{
				if ( cachedClassifier.match( candidateAnimalList ) )
				{
					return cachedClassifier;
				}
			}
		}

		if ( buildIfNotCached )
		{
			System.out.println("[CACHE ML] BUILD AS NOT CACHED");
			return createCache( candidateAnimalList, false );
		}

		return null;
	}

	public static CachedAnimalMachineLearningClassifier createCache( ArrayList<Animal> animalList, boolean evaluate )
	{
		System.out.println("[CACHE ML] CREATE CACHE");
		CachedAnimalMachineLearningClassifier cmlc = new CachedAnimalMachineLearningClassifier( animalList , evaluate );
		CachedAnimalMachineLearningClassifier previousCache = getCache( animalList , false );

		System.out.println("[CACHE ML] Cache size: " + cachedList.size() );
//azerty
//piste : enlever les caches

		synchronized ( cachedList ) {

			cachedList.remove( previousCache );
			if ( cmlc.getClassifier() != null ) // if classifier was not build, don't put it.
			{
				if ( LiveMouseTracker.USE_MACHINELEARNING_CACHE )
				{
					cachedList.add( cmlc );
				}
				return cmlc;
			}

		}

		return null;
	}





}
