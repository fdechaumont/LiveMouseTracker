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

public class MachineLearningSetBuilder {

	Instances set = null;
	boolean useCache = false;

	/** Build the Instances considering all animals in the animalPool */
	public Instances buildSet( AnimalPool animalPool )
	{
		return buildSet( animalPool , (ArrayList<Animal>) null );
	}

	/** Build the Instances considering only the animal in parameter */
	public Instances buildSet( AnimalPool animalPool, Animal animal )
	{
		ArrayList<Animal> candidateAnimalList = new ArrayList<Animal>( );
		candidateAnimalList.add( animal );
		return buildSet( animalPool , candidateAnimalList);
	}

	/** Build the Instances considering only the list of animal in parameter */
	public Instances buildSet( AnimalPool animalPool, ArrayList<Animal> candidateAnimalList )
	{
		if ( candidateAnimalList == null )
		{
			candidateAnimalList = animalPool.getAnimalList();
		}

		String animalList = "";
		for ( Animal animal : candidateAnimalList )
		{
			animalList += animal.getName() + " ";
		}
		//Chronometer buildSetChronometer = new Chronometer("Machine Learning build set " + animalList );


		// attributs
//		Attribute attribute1 = new Attribute("Surface");
//		Attribute attribute2 = new Attribute("Contour");
//		Attribute attribute3 = new Attribute("Mean intensity infra");
//		Attribute attribute4 = new Attribute("Mean depth");
//		Attribute attribute5 = new Attribute("Instant speed");
//		Attribute attribute6 = new Attribute("Max infra intensity");
//
//		Attribute attribute7 = new Attribute("Mass center z");
//		Attribute attribute8 = new Attribute("distance mass center-nose");
//		Attribute attribute9 = new Attribute("Nose z");
//		Attribute attribute10 = new Attribute("Volume");
//		Attribute attribute11 = new Attribute("Mean intensity ear/nose");



		// class

		FastVector fvClassVal = new FastVector();
//		ArrayList<Attribute> fvClassVal = new ArrayList<Attribute>();

		/*
		for ( Animal animal : candidateAnimalList ){
			fvClassVal.addElement( animal.getName() );
		}
		*/
		// Build the animal set. Even if all animals are not involved in the contact, all their names
		// must be set to make instances compatibles with the one created by the cache.
		// Then we will just not fill info for animal that cannot be involved in the problem.

		for ( Animal animal : animalPool.getAnimalList() ){
			fvClassVal.addElement( animal.getName() );
//			fvClassVal.add( new Attribute ( animal.getName() ) );
		}

		Attribute classAttribute = new Attribute("Animal Class", fvClassVal);


		// features = attributs + class
		FastVector fvWekaAttributes = new FastVector();
		fvWekaAttributes.addElement(classAttribute);

		// attributes

		for ( int i = 0 ; i < LiveMouseTracker.NB_SIGNATURE_HISTO_BIN ; i++ )
		{
			fvWekaAttributes.addElement(new Attribute( "infra hist "+i ) );
		}

		for ( int i = 0 ; i < LiveMouseTracker.NB_SIGNATURE_HISTO_BIN ; i++ )
		{
			fvWekaAttributes.addElement(new Attribute( "depth hist "+i ) );
		}

//		fvWekaAttributes.addElement(new Attribute( "infra hist 2") );
//		fvWekaAttributes.addElement(new Attribute( "infra hist 3") );
//		fvWekaAttributes.addElement(new Attribute( "infra hist 4") );
//		fvWekaAttributes.addElement(new Attribute( "infra hist 5") );
//		fvWekaAttributes.addElement(new Attribute( "infra hist 6") );
//		fvWekaAttributes.addElement(new Attribute( "infra hist 7") );

//		fvWekaAttributes.addElement(new Attribute( "depth hist 1") );
//		fvWekaAttributes.addElement(new Attribute( "depth hist 2") );
//		fvWekaAttributes.addElement(new Attribute( "depth hist 3") );
//		fvWekaAttributes.addElement(new Attribute( "depth hist 4") );
//		fvWekaAttributes.addElement(new Attribute( "depth hist 5") );
//		fvWekaAttributes.addElement(new Attribute( "depth hist 6") );
//		fvWekaAttributes.addElement(new Attribute( "depth hist 7") );

		fvWekaAttributes.addElement(new Attribute( "min infra") );
		fvWekaAttributes.addElement(new Attribute( "mean infra") );
		fvWekaAttributes.addElement(new Attribute( "max infra") );


		/*
		fvWekaAttributes.addElement(attribute1);
		fvWekaAttributes.addElement(attribute2);
		fvWekaAttributes.addElement(attribute3);
		fvWekaAttributes.addElement(attribute4);
		fvWekaAttributes.addElement(attribute5);
		fvWekaAttributes.addElement(attribute6);
		fvWekaAttributes.addElement(attribute7);
		fvWekaAttributes.addElement(attribute8);
		fvWekaAttributes.addElement(attribute9);
		fvWekaAttributes.addElement(attribute10);
		fvWekaAttributes.addElement(attribute11);
		*/

		// For test to compare with idTracker signature.
		// Note that idTracker signature did not use machine learning, it creates a distance to an histogram.
		// so this test is not a proper copy of idTracker, it's a bit better thanks to the machine learning.
		// If enable should disable other features.
		if ( LiveMouseTracker.ID_TRACKER_LIKE_HISTOGRAM_FEATURES_ENABLED )
		{
			for ( int i = 0 ; i < 64*64; i++ )
			{
				Attribute a = new Attribute("Hist "+i );
				fvWekaAttributes.addElement( a );
			}
		}


		// Create the training set
		Instances trainingSet = new Instances( "Training", fvWekaAttributes, 14 );

		// Set class index (first column here)
		trainingSet.setClassIndex(0);

		buildAnimalPoolFeatures( candidateAnimalList , trainingSet );

		// add 1 fake animal of each type to the set if not present in the set.
		// this is to avoid a dataset where only 1 animal is present, which crash the classifier.

//			for ( Animal animal : animalPool.getAnimalList() )
//			{
//				if ( candidateAnimalList.contains( animal ) ) continue; // animal is already present in the set.
////				System.out.println("Build fake animal " + animal.getName() );
//				Instance instance = buildDetectionFeatures( animal,
//						new MouseDetection( new ROI2DArea( new Point2D.Double( -1,-1) ), -1 )  , trainingSet );
//				if ( instance != null )
//				{
//					trainingSet.add( instance );
//				}
//			}

		//buildSetChronometer.displayInSeconds();

		this.set = trainingSet;
		return trainingSet;

	}



	private void buildAnimalPoolFeatures( ArrayList<Animal> candidateAnimalList, Instances set ) {

		for ( Animal animal: candidateAnimalList )
		{
			buildAnimalFeatures(animal, set);
		}

	}

	private void buildAnimalFeatures(Animal animal, Instances set ) {

//		if ( useCache )
//		{
//			//System.out.println("CACHE ANIMAL: " + animal.getName() );
//			Instances animalInstances = animal.getMachineLearningInstancesSet();
//			if ( animalInstances != null )
//			{
//				for ( int i = 0 ; i < animalInstances.numInstances() ; i++ )
//				{
//					set.add( animalInstances.instance( i ) );
//				}
//			}
//		}else
		{
			// take the last trackSegment in t, and create dictionary until NB_VALUE is reached or no more tracks on aimal
//			for ( TrackSegment ts : animal.getTrackSegmentList() )
//			{
//				// int nbTrack = 0;
//				if ( ts.getLastTimePoint() > LiveMouseTracker.getT() - LiveMouseTracker.LEARNING_WINDOW_IN_FRAME )
//				{
//					buildTrackSegmentFeatures( animal , ts , set );
//				}
//				//System.out.println("Build animal " + animal.getName() + " nb track used: " + nbTrack );
//			}

			// This will build a dictionary for the animal with a max of detection.
			ArrayList<TrackSegment> tsList = animal.getTrackSegments();
			int nbDetection = 0;

			for ( int i = tsList.size()-1 ; i >=0 ; i-- )
			{
				try
				{
					TrackSegment ts = tsList.get( i );
					nbDetection += ts.getLength();
					if ( nbDetection > LiveMouseTracker.LEARNING_NB_DETECTION_FOR_LEARNING_PER_ANIMAL )
					{
						break;
					}
					buildTrackSegmentFeatures( animal , ts , set );
				} catch( NullPointerException e )
				{
					// For the very rare problem where the track has removed detection during the process
					// I prefer to have a null pointer instead of synchronizing. Too speed critical here.
					System.err.println("MachineLearning set builder null pointer.");
				}
			}
			//System.out.println("[LEARNING][ML SET BUILDER] Animal " + animal.getName() + " built with " + nbDetection + " detections." );

		}

	}

	private void buildTrackSegmentFeatures( Animal animal, TrackSegment ts , Instances set ) {

//		System.out.println("Building animal named " + animal.getName() );
//		System.out.println("With #detection: " + ts.getDetectionList().size()  );
		for ( MouseDetection mouseDetection : ts.getDetectionList() )
		{
			Instance instance = buildDetectionFeatures( animal, mouseDetection  , set );
			if ( instance != null )
			{
				set.add( instance );
			}
		}

	}

	/** Can be call to build ground truth (animal is set)
	 * or to build a candidate (animal would be null)
	 */
	public Instance buildDetectionFeatures( Animal animal , MouseDetection mouseDetection , Instances set ) {

		if ( LiveMouseTracker.DO_NOT_LEARN_FROM_ANIMAL_IN_CONTACT )
		{
			if ( ! mouseDetection.canBeUsedForLearning() )
			{
				return null;
			}
		}

		//int numberOfInstances = 7*2+4;
		int numberOfInstances = 4 + LiveMouseTracker.NB_SIGNATURE_HISTO_BIN * 2 ;

		if ( LiveMouseTracker.ID_TRACKER_LIKE_HISTOGRAM_FEATURES_ENABLED )
		{
			numberOfInstances+=64*64;
		}

		Instance instance = new DenseInstance( numberOfInstances );


//		double surface = mouseDetection.getROI2DArea().getNumberOfPoints();
//		double contour = mouseDetection.getROI2DArea().getNumberOfContourPoints();
//		double meanIntensityInfra = mouseDetection.getMeanInfraIntensity();
//		double meanDepth = mouseDetection.getMeanDepth();
//		double instantSpeed = 0;
//		double maxIntensityInfra = mouseDetection.getMaxInfraIntensity();
//		double minIntensityInfra = mouseDetection.getMinInfraIntensity();
//		double massCenterZ = mouseDetection.getMassCenter().getZ();
//		double volume = mouseDetection.getVolume();
//		Double meanIntensityOfEars = mouseDetection.getMeanIntensityForEar();

		/*
		double distanceNoseMassCenter = 0;
		try
		{
			distanceNoseMassCenter = mouseDetection.getMassCenter().toPoint2D().distance( mouseDetection.getNose().toPoint2D() );
		}catch( NullPointerException e) {}

		double noseZ = 0;
		if ( mouseDetection.getNose() != null )
		{
			noseZ = mouseDetection.getNose().getZ();
		}
		//System.out.println( "noseZ: " + noseZ );
*/
		if ( animal !=null )
		{
			instance.setValue( set.attribute( 0 ), animal.getName() );
		}else
		{
			//iExample.setClassMissing();
		}

		int offset = 1;
		for ( int i = 0 ; i < mouseDetection.infraHisto.length; i++ )
		{
			instance.setValue( set.attribute( offset++ ), mouseDetection.infraHisto[i] );
		}
		for ( int i = 0 ; i < mouseDetection.depthHisto.length; i++ )
		{
			instance.setValue( offset++, mouseDetection.depthHisto[i] );
		}
//		instance.setValue( offset++ , minIntensityInfra );
//		instance.setValue( offset++ , meanIntensityInfra );
//		instance.setValue( offset++ , maxIntensityInfra );

		instance.setValue( offset++ , 1 );
		instance.setValue( offset++ , 5 );
		instance.setValue( offset++ , 10 );

//
//		instance.setValue( set.attribute( 1 ), surface );
//		instance.setValue( set.attribute( 2 ), contour );
//		instance.setValue( set.attribute( 3 ), meanIntensityInfra );
//		instance.setValue( set.attribute( 4 ), meanDepth );
//		instance.setValue( set.attribute( 5 ), instantSpeed );
//		instance.setValue( set.attribute( 6 ), maxIntensityInfra );
//		instance.setValue( set.attribute( 7 ), massCenterZ );
//		instance.setValue( set.attribute( 8 ), distanceNoseMassCenter );
//		instance.setValue( set.attribute( 9 ), noseZ );
//		instance.setValue( set.attribute( 10 ), volume );
//		if ( meanIntensityOfEars != null )
//		{
//			instance.setValue( set.attribute( 11 ), meanIntensityOfEars );
//		}else
//		{
//			instance.setMissing( set.attribute( 11 ) );
//		}


		if ( LiveMouseTracker.ID_TRACKER_LIKE_HISTOGRAM_FEATURES_ENABLED )
		{
			float histogram[] = mouseDetection.getHistogram();
			for ( int i = 0 ; i < histogram.length ; i++ )
			{
				instance.setValue( set.attribute( 11+i ), histogram[i] ); // 11 should be offset+ something ?
			}
		}

		return instance;
	}

	public void evaluate( Classifier randomForest )
	{
		evaluate( randomForest , true );
	}

	public String evaluate( Classifier classifier, boolean displayInConsole )
	{
		String log = "";

		if ( set == null )
		{
			return log;
		}

		if ( classifier == null )
		{
//			classifier = new RandomForest();
			classifier = new AdaBoostM1( );
			((AdaBoostM1)(classifier)).setClassifier( new RandomForest() );
			((AdaBoostM1)(classifier)).setNumIterations( LiveMouseTracker.ADA_BOOST_ITERATION );
			((AdaBoostM1)(classifier)).setUseResampling( LiveMouseTracker.ADA_BOOST_USE_RESAMPLING );

			try {
				classifier.buildClassifier( set );
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
			eval = new Evaluation( set );
			Random rand = new Random(1);  // using seed = 1
			int folds = 10;
			if ( set.numInstances() <= folds )
			{
				log+= "Can't evaluate training set (more folds than instances)\n";
			}
			eval.crossValidateModel( classifier , set , folds, rand);

			{
				int i = 0;
				for ( Animal animal : LiveMouseTracker.getMainAnimalPool().animalList )
				{
					animal.setTruePositiveRate( eval.truePositiveRate( i ) * 100 );
					i++;
				}
			}
			/*
			for ( String s : eval.getMetricsToDisplay() )
			{
				System.out.println("metric " + s );
				System.out.println( eval.getPluginMetric( s ) );
				eval.
			}
			*/


			log+= eval.toSummaryString();
			log+= eval.toClassDetailsString();
			log+= eval.toMatrixString();

			InfoGainAttributeEval igae = new InfoGainAttributeEval();
			igae.buildEvaluator( set );

			log+= "InfoGain \n";

			for (int i = 0; i < set.numAttributes(); i++) {
				Attribute t_attr = set.attribute(i);
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

//		RandomForest classifier = new RandomForest();
		Classifier classifier = new AdaBoostM1( );
		((AdaBoostM1)(classifier)).setClassifier( new RandomForest() );
		((AdaBoostM1)(classifier)).setNumIterations( LiveMouseTracker.ADA_BOOST_ITERATION );
		((AdaBoostM1)(classifier)).setUseResampling( LiveMouseTracker.ADA_BOOST_USE_RESAMPLING );


		try {
			classifier.buildClassifier( set );
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return classifier;

	}

	/** If set to true, will try to use the cache of the animal */
	public void setUseCache(boolean useCache) {
		this.useCache = useCache;
	}

}
