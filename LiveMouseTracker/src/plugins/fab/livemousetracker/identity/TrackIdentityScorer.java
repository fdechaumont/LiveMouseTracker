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
import java.util.Arrays;

import icy.system.profile.Chronometer;
import plugins.fab.livemousetracker.Animal;
import plugins.fab.livemousetracker.LiveMouseTracker;
import plugins.fab.livemousetracker.detection.MouseDetection;
import plugins.fab.livemousetracker.machinelearning.MachineLearningSetBuilder;
import plugins.fab.livemousetracker.track.TrackSegment;
import weka.classifiers.Classifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.WekaException;

public class TrackIdentityScorer implements Runnable {

	/** The track */
	private TrackSegment track;
	/** The animals the track can match with (pruned considering tracks existing and already identified with animals ) */
	private ArrayList<Animal> candidateAnimalList = new ArrayList<Animal>();
	/** the score array of each detection of the track */
	ArrayList<double[]> scoreList = new ArrayList<double[]>();

	public TrackIdentityScorer(TrackSegment track) {
		this.setTrack(track);
	}

	@Override
	public String toString() {

		String s = "";

		s+= "[TrackIdentityProblem]: ";
		s+= getTrack();
		s+= " ";

		for( Animal animal : getCandidateAnimalList() )
		{
			s+= animal.getName() +",";
		}

		return s;
	}

	public void computeDetectionScores()
	{

		scoreList.clear();

		if ( getCandidateAnimalList().size() == 1 )
		{
			// There is only 1 animal in the candidate set,
			// creates a fake scorelist answer without the machine learning
			//System.out.println("Only 1 animal in candidate set");
			ArrayList<Animal> animalList = LiveMouseTracker.getMainAnimalPool().animalList;
			double[] score = new double[ animalList.size() ];
			for ( int i = 0 ; i < score.length ; i++ )
			{
				score[i] = 0.01;
			}
			score[ animalList.indexOf( getCandidateAnimalList().get( 0 ) ) ] = 1;
			scoreList.add( score );
			return;
		}

		CachedAnimalMachineLearningClassifier cachedClassifier = getClassifier();

		if ( cachedClassifier == null )
		{
			//System.out.println("Can't create/get classifier.");
			return;
		}

		Animal animal = null;

		/** This detection list corresponds to the full track if the number of detection is less than a given const,
		 * else it is a random pick of n detection in the track */
		ArrayList<MouseDetection> selectedDetectionList = new ArrayList<MouseDetection>();
		if ( getTrack().getLength() <= LiveMouseTracker.LEARNING_MAX_NUMBER_OF_DETECTION_CONSIDERED_IN_TRACK ){
			selectedDetectionList = getTrack().getDetectionList(); // take all detection
		}else
		{// take only MAX_NUMBER_OF_DETECTION
			ArrayList<MouseDetection> detectionList = getTrack().getDetectionList();
			while ( selectedDetectionList.size() !=  LiveMouseTracker.LEARNING_MAX_NUMBER_OF_DETECTION_CONSIDERED_IN_TRACK )
			{
				MouseDetection detection = detectionList.get( (int) (Math.random() * detectionList.size()) );
				if ( selectedDetectionList.contains( detection ) ) continue;
				selectedDetectionList.add( detection );
			}
		}

		//System.out.println( "trackSegment: " + getTrack() + " length: " + getTrack().getLength() + "det kept: " + selectedDetectionList.size() );
		if( LiveMouseTracker.LOG_CHAIN )
		{
			//System.out.println("Track last point: " + getTrack().getDetection( getTrack().getLastTimePoint() ) );
		}

		for ( MouseDetection mouseDetection : selectedDetectionList ) //getTrack().getDetectionList() )
		{
			Instance instance = null;
//			try
//			{
					instance = cachedClassifier.
					setBuilder.
					buildDetectionFeatures(
					animal, mouseDetection, cachedClassifier.candidateSet );
//			}catch( NullPointerException e )
//			{
//				System.out.println("Can't build detection features ");
//				continue;
//			}
			if ( instance == null ) continue;
			instance.setDataset( cachedClassifier.candidateSet );

			try {

				double[] percentage = cachedClassifier.getClassifier().distributionForInstance( instance );

				// remove the 0 decision which is potentially completely killing an association option
				for ( int i = 0 ; i< percentage.length ; i++ )
				{
					if ( percentage[i] < 0.01 ) percentage[i] = 0.01;
				}

				if( LiveMouseTracker.LOG_CHAIN )
				{
					double max = -1;
					int	bestIndex = -1;
					for ( int i = 0 ; i < percentage.length ; i++ )
					{
						if ( percentage[i] > max )
						{
							bestIndex = i;
							max = percentage[i];
						}
					}
					int detectionIndex = selectedDetectionList.indexOf( mouseDetection );
					/*
					System.out.println( ""+ this.hashCode() + ":"
							+ detectionIndex + ":"
							+ Arrays.toString( percentage )
							+ " : " + bestIndex
							+ " : " + getCandidateAnimalList().get( bestIndex ).getName() );
							*/
				}

				scoreList.add( percentage );

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	private CachedAnimalMachineLearningClassifier getClassifier() {

		//System.out.println("[TrackIdentityProblem] Requesting cache.");
		CachedAnimalMachineLearningClassifier cachedClassifier = CachedAnimalMachineLearningManager.getCache( candidateAnimalList , true );
		return cachedClassifier;
		/*
		if ( classifier != null ) return classifier;
		System.out.println("[TrackIdentityProblem] Cache not available.");

		Instances candidateSet;
		MachineLearningSetBuilder setBuilder = new MachineLearningSetBuilder();
		candidateSet = setBuilder.buildSet( LiveMouseTracker.getMainAnimalPool() , getCandidateAnimalList() );

		if ( LiveMouseTracker.SHOW_MACHINE_LEARNING_EVALUATION )
		{
			setBuilder.evaluate();
		}

		RandomForest randomForest = new RandomForest();
		try {
			String animalListAsString = "";
			for ( Animal animal : candidateAnimalList )
			{
				animalListAsString+= animal.getName() + " ";
			}
			System.out.println("[LEARNING] Build classifier with animals : " + animalListAsString + " number of classes: " + candidateSet.numClasses() );


			Chronometer chrono = new Chronometer( "[LEARNING][BUILD CLASSIFIER] nbInstances:" + candidateSet.numInstances() + this );
			randomForest.buildClassifier( candidateSet );
			chrono.displayMs();

		}
		catch ( WekaException e) {
			System.err.println("[LEARNING] Track identity problem: Can't build classifier.");
			System.out.println("[LEARNING] Reason: " + e.getMessage() );
			return null;
		}
		catch ( Exception e2 )
		{
			e2.printStackTrace();
		}

		System.out.println("[TrackIdentityProblem] TODO: Setting/updating cache.");

		return randomForest;
*/
	}

	public ArrayList<Animal> getCandidateAnimalList() {
		return candidateAnimalList;
	}

	public void setCandidateAnimalList(ArrayList<Animal> candidateAnimalList) {
		this.candidateAnimalList = candidateAnimalList;
	}

	public TrackSegment getTrack() {
		return track;
	}

	public void setTrack(TrackSegment track) {
		this.track = track;
	}

	@Override
	public void run() {

		//Chronometer chrono = new Chronometer("[TrackIndentityProblem] " + this );
		computeDetectionScores();
		//chrono.displayMs();
	}

	public void applyTrackConstraints() {

		// if must animal is cannot be list. we don't apply must be.
		// should we keep this ??
		if ( track.getMustBeAnimal() != null && candidateAnimalList.contains( track.getMustBeAnimal() ) )
		{
			track.mustBe( null );
		}

		if ( track.getMustBeAnimal() != null ) // If there is a must be animal
		{
			// set this animal as only candidate.
			candidateAnimalList.clear();
			candidateAnimalList.add( track.getMustBeAnimal() );
		}
		else
		{
			// Remove all impossible animals.
			candidateAnimalList.removeAll( track.getCannotBeAnimalList() );
		}

	}

}
