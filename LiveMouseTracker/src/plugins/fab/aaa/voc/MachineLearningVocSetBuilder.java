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
package plugins.fab.aaa.voc;

import java.util.ArrayList;
import java.util.Random;

import icy.system.profile.Chronometer;

import plugins.fab.livemousetracker.Animal;
import plugins.fab.livemousetracker.LiveMouseTracker;
import plugins.fab.livemousetracker.detection.MouseDetection;
import plugins.fab.livemousetracker.track.AnimalPool;
import plugins.fab.livemousetracker.track.TrackSegment;
import weka.attributeSelection.InfoGainAttributeEval;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.meta.AdaBoostM1;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

public class MachineLearningVocSetBuilder {

	Instances dataRaw = null;

	/** Build the Instances considering only the list of animal in parameter */
	public Instances buildSet( ArrayList<AudioFileSet> audioFileSetList )
	{
		System.out.println("Start build set");
		Chronometer buildSetChronometer = new Chronometer("Machine Learning build VOC set " );

		ArrayList<Attribute> attributes = new ArrayList();

		// class

		ArrayList classVal = new ArrayList();
		classVal.add( "noise" );
		classVal.add( "voc" );
		attributes.add( new Attribute( "class", classVal ) );

		// attributes
		attributes.add( new Attribute("Power") );
		attributes.add( new Attribute("nbVoc") );
		attributes.add( new Attribute("nbVocPerSecond") );
		attributes.add( new Attribute("VocMeanLength") );
		attributes.add( new Attribute("VocLenSTDDEV") );
		attributes.add( new Attribute("VocMeanFrequency") );
		attributes.add( new Attribute("VocSTDFrequency") );

	    dataRaw = new Instances("TestInstances", attributes, 0);

		// Create the training set
		Instances trainingSet = new Instances( "Training", attributes, 0 );

		// Set class index (first column here)
		trainingSet.setClassIndex(0);

		System.out.println("Start feature loop over voc sets");
		for ( AudioFileSet audioFileSet : audioFileSetList )
		{
			buildVocalizationSetFeatures( audioFileSet, trainingSet );
		}

		buildSetChronometer.displayInSeconds();

		this.dataRaw = trainingSet;

		return trainingSet;

	}


	private void buildVocalizationSetFeatures( AudioFileSet audioFileSet, Instances set ) {

		System.out.println( "Processing VocalizationSet " + audioFileSet );
		System.out.println( "Number of voc in set: " + audioFileSet.audioFileList.size() );
		for ( AudioFile2 audioFile : audioFileSet.audioFileList )
		{
			Instance instance = buildVocalizationFeatures( audioFile , set );
			if ( instance != null )
			{
				System.out.println("Adding instance: " + instance );
				set.add( instance );
			}
		}

	}

	/** Can be call to build ground truth (vocType is set)
	 * or to build a candidate (vocType would be null)
	 */

	public Instance buildVocalizationFeatures( AudioFile2 audioFile, Instances set )
	{
		// TODO: check why voc can be null ? could be bad load ?

//		System.out.println("Build vocalization features");
		int numberOfFeature = 8 ;

		Instance instance = new DenseInstance( numberOfFeature );

		if ( audioFile.getMLClass() !=null ) // setClass
		{
			System.out.println( "VOC ML Class: " + audioFile.getMLClass() );
			instance.setValue( set.attribute( 0 ), audioFile.getMLClass() );
		}else
		{
		}

		int offset = 1;
		/*
		instance.setValue( set.attribute( offset++ ), 0 );
		instance.setValue( set.attribute( offset++ ), 0 );
		instance.setValue( set.attribute( offset++ ), 0 );
		instance.setValue( set.attribute( offset++ ), 0 );
		instance.setValue( set.attribute( offset++ ), 0 );
		instance.setValue( set.attribute( offset++ ), 0 );
		instance.setValue( set.attribute( offset++ ), 0 );
*/
		instance.setValue( set.attribute( offset++ ), audioFile.meanPower );
		instance.setValue( set.attribute( offset++ ), audioFile.nbVoc );
		instance.setValue( set.attribute( offset++ ), audioFile.vocDensity );
		instance.setValue( set.attribute( offset++ ), audioFile.vocMeanLenght );
		instance.setValue( set.attribute( offset++ ), audioFile.vocSTDLenght );
		instance.setValue( set.attribute( offset++ ), audioFile.vocMeanFrequency );
		instance.setValue( set.attribute( offset++ ), audioFile.vocSTDFrequency );

		return instance;
	}

	public void evaluate( Classifier randomForest )
	{
		evaluate( randomForest , true );
	}

	public String evaluate( Classifier classifier, boolean displayInConsole )
	{
		String log = "";

		if ( dataRaw == null )
		{
			return log;
		}

		if ( classifier == null )
		{
			classifier = new RandomForest();
//			classifier = new AdaBoostM1( );
//			((AdaBoostM1)(classifier)).setClassifier( new RandomForest() );
//			((AdaBoostM1)(classifier)).setNumIterations( LiveMouseTracker.ADA_BOOST_ITERATION );
//			((AdaBoostM1)(classifier)).setUseResampling( LiveMouseTracker.ADA_BOOST_USE_RESAMPLING );

			try {
				classifier.buildClassifier( dataRaw );
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// Evaluation
		Chronometer chrono = new Chronometer("Machine Learning Evaluation" );
		log+= classifier.toString();

		//System.out.println( randomForest );

		Evaluation eval;
		try {
			eval = new Evaluation( dataRaw );
			Random rand = new Random(1);  // using seed = 1
			int folds = 10;
			if ( dataRaw.numInstances() <= folds )
			{
				log+= "Can't evaluate training set (more folds than instances)\n";
				log+= "Number of instances: " + dataRaw.numInstances() + "\n";
			}
			eval.crossValidateModel( classifier , dataRaw , folds, rand);

//			{
//				int i = 0;
//				for ( Animal animal : LiveMouseTracker.getMainAnimalPool().animalList )
//				{
//					animal.setTruePositiveRate( eval.truePositiveRate( i ) * 100 );
//					i++;
//				}
//			}

			log+= eval.toSummaryString();
			log+= eval.toClassDetailsString();
			log+= eval.toMatrixString();

			InfoGainAttributeEval igae = new InfoGainAttributeEval();
			igae.buildEvaluator( dataRaw );

			log+= "InfoGain \n";

			for (int i = 0; i < dataRaw.numAttributes(); i++) {
				Attribute t_attr = dataRaw.attribute(i);
				double infogain  = igae.evaluateAttribute( i );
				log+= t_attr;
				log+= "Attrib#"+i + ": " + infogain + "\n";

			}

		} catch (Exception e1) {
			e1.printStackTrace();
		}
		log+= "" + ( chrono.getNanos() / 1000000000f ) + " seconds \n";
		chrono.displayInSeconds();

		if ( displayInConsole )
		{
			System.out.println( log );
		}

		return log;

	}



	public Classifier buildClassifier() {

		RandomForest classifier = new RandomForest();
//		Classifier classifier = new AdaBoostM1( );
//		((AdaBoostM1)(classifier)).setClassifier( new RandomForest() );
//		((AdaBoostM1)(classifier)).setNumIterations( LiveMouseTracker.ADA_BOOST_ITERATION );
//		((AdaBoostM1)(classifier)).setUseResampling( LiveMouseTracker.ADA_BOOST_USE_RESAMPLING );


		try {
			classifier.buildClassifier( dataRaw );
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return classifier;

	}


}
