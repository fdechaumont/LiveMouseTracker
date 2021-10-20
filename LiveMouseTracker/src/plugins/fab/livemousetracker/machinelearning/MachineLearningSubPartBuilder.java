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
import plugins.fab.livemousetracker.detection.SubPartDescriptor;
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

/**
 * This class will build a sub part machine learning system for an animal
 * Sub part could be typicaly A B ( like tail / head ) or more complex like
 * A B C D ( quadran ) or even A B C D E with E the tail model.
 *
 * As animal might be very different from one to another. This dictionnary is unique
 * per animal.
 *
 * Attributes are not the same as for the animals (as there is no info for instance
 * about the nose position as we observe patch that can be tail only )
 *
 * @author Fab
 */
public class MachineLearningSubPartBuilder {

	Instances set = null;

	public Instances getDataSet() {
		return set;
	}

	/** Build the Instances  */
	public Instances buildSet( Animal animal )
	{

		// attributes
//		Attribute attribute1 = new Attribute("Surface");
//		Attribute attribute2 = new Attribute("Contour");
//		Attribute attribute3 = new Attribute("Mean intensity infra");
//		Attribute attribute4 = new Attribute("Mean depth");
//		Attribute attribute5 = new Attribute("Max infra intensity");
//		Attribute attribute6 = new Attribute("Volume");

		// class

		FastVector fvClassVal = new FastVector();

		/*
		for ( Animal animal : candidateAnimalList ){
			fvClassVal.addElement( animal.getName() );
		}
		*/
		// Build the animal set. Even if all animals are not involved in the contact, all their names
		// must be set to make instances compatibles with the one created by the cache.
		// Then we will just not fill info for animal that cannot be involved in the problem.
//		for ( Animal animal : animalPool.getAnimalList() ){
//			fvClassVal.addElement( animal.getName() );
//		}

		// Here we set the different pattern that we may obtain
		// (depending on the number of parts)
		// say that ABCDE is the truth and any other letter order is false.
		fvClassVal.addElement( "A" );
		fvClassVal.addElement( "B" );

		Attribute classAttribute = new Attribute("SubPattern Class", fvClassVal);

		// features = attributs + class
		FastVector fvWekaAttributes = new FastVector();
		fvWekaAttributes.addElement(classAttribute);
//		for ( int i = 0 ; i< 2 ; i++ ) // Class AB and BA.
//		{

//			fvWekaAttributes.addElement(new Attribute( "Surface") );
//			fvWekaAttributes.addElement(new Attribute( "Contour") );
//			fvWekaAttributes.addElement(new Attribute( "Mean depth") );

//			fvWekaAttributes.addElement(new Attribute( i+" Mean intensity infra"));
//			fvWekaAttributes.addElement(new Attribute( i+" Max infra intensity"));
//			fvWekaAttributes.addElement(new Attribute( i+" Volume"));

		for ( int i = 0 ; i< LiveMouseTracker.NB_SIGNATURE_HISTO_BIN ; i++ )
		{
			fvWekaAttributes.addElement(new Attribute( "infra hist " + i ) );
		}

		for ( int i = 0 ; i< LiveMouseTracker.NB_SIGNATURE_HISTO_BIN ; i++ )
		{
			fvWekaAttributes.addElement(new Attribute( "depth hist "+i ) );
		}

//			fvWekaAttributes.addElement(new Attribute( "infra hist 2") );
//			fvWekaAttributes.addElement(new Attribute( "infra hist 3") );
//			fvWekaAttributes.addElement(new Attribute( "infra hist 4") );
//			fvWekaAttributes.addElement(new Attribute( "infra hist 5") );
//			fvWekaAttributes.addElement(new Attribute( "infra hist 6") );
//			fvWekaAttributes.addElement(new Attribute( "infra hist 7") );

//			fvWekaAttributes.addElement(new Attribute( "depth hist 2") );
//			fvWekaAttributes.addElement(new Attribute( "depth hist 3") );
//			fvWekaAttributes.addElement(new Attribute( "depth hist 4") );
//			fvWekaAttributes.addElement(new Attribute( "depth hist 5") );
//			fvWekaAttributes.addElement(new Attribute( "depth hist 6") );
//			fvWekaAttributes.addElement(new Attribute( "depth hist 7") );
//		}

//			Attribute attribute1 = new Attribute("Surface");
//			Attribute attribute2 = new Attribute("Contour");
//			Attribute attribute3 = new Attribute("Mean intensity infra");
//			Attribute attribute4 = new Attribute("Mean depth");
//			Attribute attribute5 = new Attribute("Max infra intensity");
//			Attribute attribute6 = new Attribute("Volume");

//		fvWekaAttributes.addElement(attribute1);
//		fvWekaAttributes.addElement(attribute2);
//		fvWekaAttributes.addElement(attribute3);
//		fvWekaAttributes.addElement(attribute4);
//		fvWekaAttributes.addElement(attribute5);
//		fvWekaAttributes.addElement(attribute6);

//		if ( LiveMouseTracker.HISTOGRAM_FEATURES_ENABLED )
//		{
//			for ( int i = 0 ; i < 64*64; i++ )
//			{
//				Attribute a = new Attribute("Hist "+i );
//				fvWekaAttributes.addElement( a );
//			}
//		}


		// Create the training set
		Instances trainingSet = new Instances("Training", fvWekaAttributes, SubPartDescriptor.NUMBER_OF_ATTRIBUTES +1);

		// Set class index (first column here)
		trainingSet.setClassIndex(0);

		buildSubPartAnimalFeatures(animal, trainingSet );


		// add 1 fake animal of each type to the set if not present in the set.
		// this is to avoid a dataset where only 1 animal is present, which crash the classifier.
//		{
//			for ( Animal animal : animalPool.getAnimalList() )
//			{
//				if ( candidateAnimalList.contains( animal ) ) continue; // animal is already present in the set.
//				System.out.println("Build fake animal " + animal.getName() );
//				Instance instance = buildDetectionFeatures( animal,
//						new MouseDetection( new ROI2DArea( new Point2D.Double( 0,0) ), 0 )  , trainingSet );
//				if ( instance != null )
//				{
//					trainingSet.add( instance );
//				}
//			}
//		}

		this.set = trainingSet;
		return trainingSet;

	}

	private void buildSubPartAnimalFeatures(Animal animal, Instances set ) {

		// This will build a dictionary for the animal with a max of detection.
		ArrayList<TrackSegment> tsList = animal.getTrackSegments();
//		int nbDetection = 0;
		for ( int i = tsList.size()-1 ; i >=0 ; i-- ) // start from present ad go to past.
		{
			TrackSegment ts = tsList.get( i );
//			nbDetection += ts.getLength();
//			for ( MouseDetection detection: ts.getDetectionList() ) // increment number of detection that can be used for training.
//			{
//				if ( isDetectionOkForLearning( detection ) )
//				{
//					nbDetection++;
//				}
//			}
//			if ( nbDetection > LiveMouseTracker.LEARNING_NB_DETECTION_FOR_LEARNING_SUBPARTS_PER_ANIMAL )
//			{
//				break;
//			}
			buildTrackSegmentSubPartFeatures( animal , ts , set );
			if ( set.numInstances() > LiveMouseTracker.LEARNING_NB_DETECTION_FOR_LEARNING_SUBPARTS_PER_ANIMAL )
			{
				break;
			}
		}
		System.out.println("[SUB PART ML] animal: " + animal.getName() + " nb ML Instances: " + set.numInstances() );
	}

	private boolean isDetectionOkForLearning( MouseDetection mouseDetection )
	{
		// The animal must be in a learning situation
		if ( ! mouseDetection.canBeUsedForLearning() )
		{
			return false;
		}
		// Speed of the animal must be fast enough
		Double instantSpeed = mouseDetection.getInstantSpeed();
		if ( instantSpeed == null ) return false;

//		if( instantSpeed < LiveMouseTracker.MIN_INSTANT_SPEED_FOR_SUB_PART_CALCULATION )
//		{
//			return false;
//		}

		if ( mouseDetection.getSubPartDescriptorList().size() == 0 )
		{
			return false;
		}

		return true;
	}

	private void buildTrackSegmentSubPartFeatures( Animal animal, TrackSegment ts , Instances set ) {

		if ( ts == null )
		{
			System.out.println("TS is null. Can't build sub part features");
			return;
		}
		for ( MouseDetection mouseDetection : ts.getDetectionList() )
		{
//			Instance instance =
					buildSubPartDetectionFeatures( animal, mouseDetection, set );
//			if ( instance != null )
//			{
//				set.add( instance );
//			}
		}

	}

//	public Instance buildSubPartDetectionFeatures( MouseDetection mouseDetection ) // build an AB instance for prediction.
//	{
//		if ( ! isDetectionOkForLearning( mouseDetection ) )
//		{
//			return null;
//		}
//
//		int numberOfAttributes = SubPartDescriptor.NUMBER_OF_ATTRIBUTES;
//		int nbPart = mouseDetection.getSubPartDescriptorList().size();
//		Instance instance = new Instance( numberOfAttributes*nbPart +1 );
//
//		//instance.setValue( set.attribute( 0 ), "AB" );
//
//		int offset=1;
//		for ( SubPartDescriptor spd : mouseDetection.getSubPartDescriptorList() )
//		{
//			instance.setValue( offset++ , spd.surface );
//			instance.setValue( offset++ , spd.contour );
////			instance.setValue( offset++ , spd.meanIntensityArea );
//			instance.setValue( offset++ , spd.meanDepth );
////			instance.setValue( offset++ , spd.maxInfraIntensity );
////			instance.setValue( offset++ , spd.volume );
//		}
//		return instance;
//	}

	public Instance buildSubPartDetectionFeatures( SubPartDescriptor spd )
	{
		int numberOfAttributes = SubPartDescriptor.NUMBER_OF_ATTRIBUTES;

		Instance instance = new DenseInstance( numberOfAttributes+1 );
//		SubPartDescriptor spd = mouseDetection.getSubPartDescriptorList().get( 0 );

		int offset=1;
//		for ( SubPartDescriptor spd : mouseDetection.getSubPartDescriptorList() )
		{
//			instance.setValue( offset++ , spd.surface );
//			instance.setValue( offset++ , spd.contour );
//			instance.setValue( offset++ , spd.meanDepth );

//			instance.setValue( offset++ , spd.meanIntensityArea );
//			instance.setValue( offset++ , spd.maxInfraIntensity );
//			instance.setValue( offset++ , spd.volume );
			for ( int i = 0 ; i < spd.infraHisto.length; i++ )
			{
				instance.setValue( offset++, spd.infraHisto[i] );
			}
			for ( int i = 0 ; i < spd.depthHisto.length; i++ )
			{
				instance.setValue( offset++, spd.depthHisto[i] );
			}
		}
		return instance;
	}

	public Instance buildSubPartDetectionFeatures( MouseDetection mouseDetection ) // build an A instance for prediction.
	// FIXME: should also create B instance to double check p(B)=B and p(A)=A
	{
		if ( ! isDetectionOkForLearning( mouseDetection ) )
		{
			return null;
		}

		SubPartDescriptor spd = mouseDetection.getSubPartDescriptorList().get( 0 );
		return buildSubPartDetectionFeatures( spd );

//		int numberOfAttributes = SubPartDescriptor.NUMBER_OF_ATTRIBUTES;
//
//		Instance instance = new Instance( numberOfAttributes+1 );
//
//		int offset=1;
////		for ( SubPartDescriptor spd : mouseDetection.getSubPartDescriptorList() )
//		{
////			instance.setValue( offset++ , spd.surface );
////			instance.setValue( offset++ , spd.contour );
////			instance.setValue( offset++ , spd.meanDepth );
//
////			instance.setValue( offset++ , spd.meanIntensityArea );
////			instance.setValue( offset++ , spd.maxInfraIntensity );
////			instance.setValue( offset++ , spd.volume );
//			for ( int i = 0 ; i < spd.infraHisto.length; i++ )
//			{
//				instance.setValue( offset++, spd.infraHisto[i] );
//			}
//			for ( int i = 0 ; i < spd.depthHisto.length; i++ )
//			{
//				instance.setValue( offset++, spd.depthHisto[i] );
//			}
//		}
//		return instance;
	}

	/**
	 * build A and B classes
	 * Add it to the instances
	 */
	public void buildSubPartDetectionFeatures(
			Animal animal , MouseDetection mouseDetection , Instances set )
	{

		if ( set.numInstances() > LiveMouseTracker.LEARNING_NB_DETECTION_FOR_LEARNING_SUBPARTS_PER_ANIMAL )
		{
			return;
		}

		if ( ! isDetectionOkForLearning( mouseDetection ) )
		{
			return;
		}


		{ // build A
			int numberOfAttributes = SubPartDescriptor.NUMBER_OF_ATTRIBUTES;
			int nbPart = mouseDetection.getSubPartDescriptorList().size();



			// set the number of attribute: attribute per part * nb of part + the description of the class ( AB for instance )
//			System.out.println( "NUMBER OF ATTR: " + numberOfAttributes );
			Instance instance = new DenseInstance( numberOfAttributes +1 );
			instance.setValue( set.attribute( 0 ), "A" );

			int offset=1;
			SubPartDescriptor spd = mouseDetection.getSubPartDescriptorList().get( 0 );
//			for ( SubPartDescriptor spd : mouseDetection.getSubPartDescriptorList() )
//			{
//				instance.setValue( set.attribute( offset++ ), spd.surface );
//				instance.setValue( set.attribute( offset++ ), spd.contour );
//				instance.setValue( set.attribute( offset++ ), spd.meanDepth );

//				instance.setValue( set.attribute( offset++ ), spd.meanIntensityArea );
//				instance.setValue( set.attribute( offset++ ), spd.maxInfraIntensity );
//				instance.setValue( set.attribute( offset++ ), spd.volume );
				for ( int i = 0 ; i < spd.infraHisto.length; i++ )
				{
					instance.setValue( offset++,
							//set.attribute( offset++ ),
							spd.infraHisto[i] );
				}
				for ( int i = 0 ; i < spd.depthHisto.length; i++ )
				{
					instance.setValue(
							offset++,
//							set.attribute( offset++ ),
							spd.depthHisto[i] );
				}
//			}
			set.add( instance );
		}

		{ // build B
			int numberOfAttributes = SubPartDescriptor.NUMBER_OF_ATTRIBUTES;
			int nbPart = mouseDetection.getSubPartDescriptorList().size();

			// set the number of attribute: attribute per part * nb of part + the description of the class ( AB for instance )
			Instance instance = new DenseInstance( numberOfAttributes*nbPart +1 );
			instance.setValue( set.attribute( 0 ), "B" );

			int offset=1;
			SubPartDescriptor spd = mouseDetection.getSubPartDescriptorList().get( 1 );
//			for ( SubPartDescriptor spd : mouseDetection.getSubPartDescriptorList() )
//			{
//				instance.setValue( set.attribute( offset++ ), spd.surface );
//				instance.setValue( set.attribute( offset++ ), spd.contour );
//				instance.setValue( set.attribute( offset++ ), spd.meanDepth );

//				instance.setValue( set.attribute( offset++ ), spd.meanIntensityArea );
//				instance.setValue( set.attribute( offset++ ), spd.maxInfraIntensity );
//				instance.setValue( set.attribute( offset++ ), spd.volume );
				for ( int i = 0 ; i < spd.infraHisto.length; i++ )
				{
					instance.setValue( set.attribute( offset++ ), spd.infraHisto[i] );
				}
				for ( int i = 0 ; i < spd.depthHisto.length; i++ )
				{
					instance.setValue( offset++, spd.depthHisto[i] );
				}
//			}
			set.add( instance );
		}


//		{ // build AB
//			int numberOfAttributes = SubPartDescriptor.NUMBER_OF_ATTRIBUTES;
//			int nbPart = mouseDetection.getSubPartDescriptorList().size();
//
//			// set the number of attribute: attribute per part * nb of part + the description of the class ( AB for instance )
//			Instance instance = new Instance( numberOfAttributes*nbPart +1 );
//			instance.setValue( set.attribute( 0 ), "AB" );
//
//			int offset=1;
//			for ( SubPartDescriptor spd : mouseDetection.getSubPartDescriptorList() )
//			{
//				instance.setValue( set.attribute( offset++ ), spd.surface );
//				instance.setValue( set.attribute( offset++ ), spd.contour );
////				instance.setValue( set.attribute( offset++ ), spd.meanIntensityArea );
//				instance.setValue( set.attribute( offset++ ), spd.meanDepth );
////				instance.setValue( set.attribute( offset++ ), spd.maxInfraIntensity );
////				instance.setValue( set.attribute( offset++ ), spd.volume );
//			}
//			set.add( instance );
//		}
//
//		{ // build BA
//			int numberOfAttributes = SubPartDescriptor.NUMBER_OF_ATTRIBUTES;
//			int nbPart = mouseDetection.getSubPartDescriptorList().size();
//
//			// set the number of attribute: attribute per part * nb of part + the description of the class ( AB for instance )
//			Instance instance = new Instance( numberOfAttributes*nbPart +1 );
//			instance.setValue( set.attribute( 0 ), "BA" );
//
//			int offset=1;
//			{
//				{
//					SubPartDescriptor spd = mouseDetection.getSubPartDescriptorList().get( 1 );
//					instance.setValue( set.attribute( offset++ ), spd.surface );
//					instance.setValue( set.attribute( offset++ ), spd.contour );
////					instance.setValue( set.attribute( offset++ ), spd.meanIntensityArea );
//					instance.setValue( set.attribute( offset++ ), spd.meanDepth );
////					instance.setValue( set.attribute( offset++ ), spd.maxInfraIntensity );
////					instance.setValue( set.attribute( offset++ ), spd.volume );
//				}
//				{
//					SubPartDescriptor spd = mouseDetection.getSubPartDescriptorList().get( 0 );
//					instance.setValue( set.attribute( offset++ ), spd.surface );
//					instance.setValue( set.attribute( offset++ ), spd.contour );
////					instance.setValue( set.attribute( offset++ ), spd.meanIntensityArea );
//					instance.setValue( set.attribute( offset++ ), spd.meanDepth );
////					instance.setValue( set.attribute( offset++ ), spd.maxInfraIntensity );
////					instance.setValue( set.attribute( offset++ ), spd.volume );
//				}
//			}
//			set.add( instance );
//		}

	}

	public void evaluate( )
	{
		evaluate( true );
	}

	public String evaluate( boolean displayInConsole )
	{
		String log = "";

		if ( set == null )
		{
			return log;
		}

//		RandomForest classifier = new RandomForest();

		Classifier classifier = new AdaBoostM1( );
		((AdaBoostM1)(classifier)).setClassifier( new RandomForest() );
		((AdaBoostM1)(classifier)).setNumIterations( LiveMouseTracker.ADA_BOOST_ITERATION );
		((AdaBoostM1)(classifier)).setUseResampling( LiveMouseTracker.ADA_BOOST_USE_RESAMPLING );


		try {
			classifier.buildClassifier( set );
		} catch (Exception e) {
			e.printStackTrace();
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

			System.out.println("This error is handled:");
			//e.printStackTrace();
			 System.out.println("Not enough training instances with class labels");
		}

		return classifier;

	}




}
