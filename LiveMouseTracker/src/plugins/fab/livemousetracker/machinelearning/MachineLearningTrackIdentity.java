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
package plugins.fab.livemousetracker.machinelearning;

import java.util.ArrayList;
import java.util.Arrays;

import plugins.fab.livemousetracker.Animal;
import plugins.fab.livemousetracker.LiveMouseTracker;
import plugins.fab.livemousetracker.detection.MouseDetection;
import plugins.fab.livemousetracker.track.AnimalPool;
import plugins.fab.livemousetracker.track.TrackSegment;
import weka.classifiers.Classifier;
import weka.classifiers.meta.AdaBoostM1;
import weka.classifiers.trees.RandomForest;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.WekaException;

/**
 * @author Fab
 *
 */
public class MachineLearningTrackIdentity {

	Instances candidateSet;
	Classifier classifier;
	AnimalPool animalPool;
	MachineLearningSetBuilder setBuilder;
	TrackSegment trackSegmentToIdentify;
	ArrayList<Animal> candidateAnimalList;
	boolean useCache = false;

	public MachineLearningTrackIdentity( AnimalPool animalPool,
			boolean pruneImpossibleAnimals ,
			TrackSegment trackSegment , boolean useCache ) {

		this.animalPool = animalPool;
		this.trackSegmentToIdentify = trackSegment;
		this.useCache = useCache;

		candidateAnimalList = animalPool.getAnimalList();

		if ( pruneImpossibleAnimals )
		{
			pruneCandidateAnimalPool( candidateAnimalList );
		}

		this.setBuilder = new MachineLearningSetBuilder();
		//this.setBuilder.setUseCache( useCache );

		candidateSet = setBuilder.buildSet(animalPool , candidateAnimalList  );

		if ( candidateSet.numInstances() == 0 )
		{
			// no animal identified yet to process
			return;
		}

		//classifier = new RandomForest();

		classifier = new AdaBoostM1( );
		((AdaBoostM1)(classifier)).setClassifier( new RandomForest() );
		((AdaBoostM1)(classifier)).setNumIterations( LiveMouseTracker.ADA_BOOST_ITERATION );
		((AdaBoostM1)(classifier)).setUseResampling( LiveMouseTracker.ADA_BOOST_USE_RESAMPLING );


		try {
			classifier.buildClassifier( candidateSet );
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		//setBuilder.evaluate();



	}

/** removes all candidate animals that cannot be in the problem, because they are already identified
 * in concurrent track ( concurrent track would be that have part of their track existing at the same T
 * or sharing a subset of T.
 *
 * @param candidateAnimalList
 */
	private void pruneCandidateAnimalPool( ArrayList<Animal> candidateAnimalList ) {

		AnimalPool animalPool = LiveMouseTracker.getMainAnimalPool();

		//System.out.println("Pruning animals");
		for ( Animal animal: animalPool.getAnimalList() )
		{
			animalLoop:
			for ( TrackSegment ts : animal.getTrackSegments() )
			{
				if ( ts.overlapInT( trackSegmentToIdentify ) )
				{
					candidateAnimalList.remove( animal );
					//System.out.println("Removing animal " + animal.getName() );
					continue animalLoop;
				}
			}
		}

	}


	public IdentityResult findIdentity()
	{
		Animal animal = null;

		double[] cumulatedResult = null;

		int nbdetection = 0;

		System.out.println( "trackSegment: " + trackSegmentToIdentify );

		for ( MouseDetection mouseDetection : trackSegmentToIdentify.getDetectionList() )
		{
			Instance instance = setBuilder.buildDetectionFeatures( animal, mouseDetection, candidateSet );
			if ( instance == null ) continue;
			instance.setDataset( candidateSet );
			nbdetection++;

			try {

				double[] percentage = classifier.distributionForInstance( instance );

				System.out.println( Arrays.toString( percentage ) );

				if ( cumulatedResult == null )
				{
					cumulatedResult = percentage;
				}else
				{
					for ( int i = 0 ; i < percentage.length ; i++ )
					{
						cumulatedResult[i] += percentage[i];
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		//System.out.println("** Cumulated : " + Arrays.toString( cumulatedResult ) );

		double meanResult[] = cumulatedResult;
		if ( nbdetection > 0 )
		{
			for ( int i = 0 ; i < meanResult.length ; i++ )
			{
				meanResult[i] /= (double)nbdetection;
			}
		}

		System.out.println("** Mean proba: " + Arrays.toString( meanResult ) );

		double max = 0;
		int maxIndex = 0;
		for ( int i = 0 ; i < meanResult.length ; i++ )
		{
			if ( meanResult[i] > max )
			{
				maxIndex = i;
				max = meanResult[i];
			}
		}

		animal = animalPool.getAnimalList().get( maxIndex );
		//animal = candidateAnimalList.get( maxIndex );
		System.out.println("Identity found: " + animal.getName() );
		System.out.println("Best Mean proba result: " + meanResult[maxIndex] );

		IdentityResult result = new IdentityResult();
		result.animalFound = animal;
		result.animalProba = Math.round( meanResult[maxIndex] * 100d ) / 100d;
		result.proba = meanResult;

		trackSegmentToIdentify.setIdentityResult( result );

		return result;
	}

	private double getMax(double[] array) {

		double max = 0;
		for ( int i = 0 ; i< array.length ; i++ )
		{
			if ( max < array[i] )
			{
				max = array[i];
			}
		}

		return max;

	}

	/**
	 * Check if the classifier has been build.
	 * @return
	 */
	public boolean canBeProcessed() {
		return ( classifier != null );
	}

/*
	public void evaluate()
	{
		System.out.println( randomForest );

		// Evaluation

		Evaluation eval;
		try {
			eval = new Evaluation( trainingSet );
			Random rand = new Random(1);  // using seed = 1
			int folds = 10;
			eval.crossValidateModel( randomForest , trainingSet , folds, rand);
			System.out.println(eval.toSummaryString());
			System.out.println(eval.toClassDetailsString());
			System.out.println(eval.toMatrixString());

			InfoGainAttributeEval igae = new InfoGainAttributeEval();
			igae.buildEvaluator( trainingSet );

			System.out.println("InfoGain");
			for (int i = 0; i < trainingSet.numAttributes(); i++) {
				Attribute t_attr = trainingSet.attribute(i);
				double infogain  = igae.evaluateAttribute( i );
				System.out.println( t_attr );
				System.out.println("Attribute #"+i + " : " + infogain );
			}
			System.out.println("---");


		} catch (Exception e1) {
			e1.printStackTrace();
		}

//		System.out.println("number of trees: " + randomForest.getNumTrees() );
//		System.out.println("Max depth: " + randomForest.getMaxDepth() );

	}
	*/

}
