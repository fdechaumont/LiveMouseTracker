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

import icy.system.profile.Chronometer;
import plugins.fab.livemousetracker.Animal;
import plugins.fab.livemousetracker.LiveMouseTracker;
import plugins.fab.livemousetracker.machinelearning.MachineLearningSetBuilder;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.meta.AdaBoostM1;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.WekaException;

public class CachedAnimalMachineLearningClassifier {

	//RandomForest classifier= null;
	Classifier classifier= null;
	private ArrayList<Animal> animalList;
	MachineLearningSetBuilder setBuilder;
	Instances candidateSet;
	int creationFrame;

	public CachedAnimalMachineLearningClassifier( ArrayList<Animal> animalList ) {
		this( animalList , false );
	}

	public CachedAnimalMachineLearningClassifier( ArrayList<Animal> animalList , boolean evaluate ) {

		creationFrame = LiveMouseTracker.getT();
		this.animalList = new ArrayList<Animal>( animalList ) ;

		setBuilder = new MachineLearningSetBuilder();
		candidateSet = setBuilder.buildSet( LiveMouseTracker.getMainAnimalPool() , animalList );

//		Classifier classifier = new RandomForest();
//		((RandomForest)(classifier)).setNumIterations( 1000 );

		Classifier classifier = new AdaBoostM1( );
		((AdaBoostM1)(classifier)).setClassifier( new RandomForest() );
		((AdaBoostM1)(classifier)).setNumIterations( LiveMouseTracker.ADA_BOOST_ITERATION );
		((AdaBoostM1)(classifier)).setUseResampling( LiveMouseTracker.ADA_BOOST_USE_RESAMPLING );

//		RandomForest randomForest = new RandomForest();


		String animalListAsString = "";
		for ( Animal animal : animalList )
		{
			animalListAsString+= animal.getName() + " ";
		}
		System.out.println("[CACHING MachineLearning] build animals: " + animalListAsString + " number of classes: " + candidateSet.numClasses() );
		Chronometer chrono = new Chronometer( "[CACHING] " + animalListAsString );
		try {
//			randomForest.setMaxDepth( 1000 );
//			randomForest.setOptions( new String[]{ "-I 1000" , "-num-slots 0", "-N 5", "-B" } );
			classifier.buildClassifier( candidateSet );
			this.classifier = classifier;
			if ( evaluate )
			{
				setBuilder.evaluate( this.classifier, true );
			}
		}
		catch ( WekaException e) {
			System.err.println("[LEARNING] Track identity problem: Can't build classifier.");
			System.out.println("[LEARNING] Reason: " + e.getMessage() );
		}
		catch ( Exception e2 )
		{
			e2.printStackTrace();
		}
		chrono.displayInSeconds();

	}

	//public RandomForest getClassifier() {
	public Classifier getClassifier() {
		return classifier;
	}

	public boolean match(ArrayList<Animal> candidateAnimalList) {

		if ( candidateAnimalList.size() != animalList.size() ) return false;

		for ( Animal animal : candidateAnimalList )
		{
			if ( !animalList.contains( animal ) ) return false;
		}

		return true;
	}





}
