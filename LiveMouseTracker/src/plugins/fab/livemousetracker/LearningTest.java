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
package plugins.fab.livemousetracker;

import icy.plugin.abstract_.PluginActionable;

import java.util.Arrays;

import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

public class LearningTest extends PluginActionable {

	@Override
	public void run() {

		// attributs
		Attribute Attribute1 = new Attribute("firstNumeric");
		Attribute Attribute2 = new Attribute("secondNumeric");

		// class

		FastVector fvClassVal = new FastVector(2);
		fvClassVal.addElement("positive");
		fvClassVal.addElement("negative");
		Attribute classAttribute = new Attribute("theClass", fvClassVal);

		// features = attributs + class
		FastVector fvWekaAttributes = new FastVector(3);
		fvWekaAttributes.addElement(Attribute1);
		fvWekaAttributes.addElement(Attribute2);
		fvWekaAttributes.addElement(classAttribute);

		// test

		// Create an empty training set
		Instances trainingSet = new Instances("Training", fvWekaAttributes, 10);
    	// Set class index (donc le resultat correspondant)
		trainingSet.setClassIndex(2);

		// Create the instance
		/*
		Instance iExample = new Instance(3);
		iExample.setValue((Attribute)fvWekaAttributes.elementAt(0), 1.0);
		iExample.setValue((Attribute)fvWekaAttributes.elementAt(1), 0.5);
		iExample.setValue((Attribute)fvWekaAttributes.elementAt(2), "positive");
		*/
		// add the instance
//		trainingSet.add(iExample);

		for ( int i = 0 ; i < 100 ; i++ )
		{
			int a = (int)(Math.random()* 100 ) - 50 ;
			int b = (int)(Math.random()* 100 ) - 50 ;
			String result = "negative";
			if ( a > 0 && b > 0 ) result = "positive";

			Instance iExample = new DenseInstance(3);
			iExample.setValue((Attribute)fvWekaAttributes.elementAt(0), a );
			iExample.setValue((Attribute)fvWekaAttributes.elementAt(1), b );
			iExample.setValue((Attribute)fvWekaAttributes.elementAt(2), result );
			trainingSet.add(iExample);
		}





	 //Classifier cModel = (Classifier)new RandomForest();

		RandomForest randomForest = new RandomForest();

		try {
			randomForest.buildClassifier( trainingSet );
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

//		System.out.println("number of trees: " + randomForest.getNumTrees() );
		System.out.println("Max depth: " + randomForest.getMaxDepth() );


		// Test a set

for ( int i = 0 ; i<100 ; i++ )
{
		try {
			//System.out.println("-----");
			Instance iExample = new DenseInstance(2);
			int a = (int)(Math.random()* 100 ) - 50 ;
			int b = (int)(Math.random()* 100 ) - 50 ;
			iExample.setDataset( trainingSet );
			System.out.println("a:" + a + "** b:" + b );
			boolean isPositif = ( ( a >0 ) && ( b > 0 ) );

			iExample.setValue((Attribute)fvWekaAttributes.elementAt(0), a );
			iExample.setValue((Attribute)fvWekaAttributes.elementAt(1), b );

			double outcomeValue = randomForest.classifyInstance( iExample );
			//System.out.println( "outComeValue:" + outcomeValue );
			double[] percentage = randomForest.distributionForInstance( iExample );
			System.out.println(Arrays.toString( percentage ));
			//System.out.println("positif - negatif");

			boolean testOk= false;
			if ( percentage[0] > 0.95d  && isPositif )
			{
				testOk = true;
			}

			if ( percentage[1] > 0.95d  && !isPositif )
			{
				testOk = true;
			}

			if ( testOk )
			{
				System.out.println("OK");
			}else
			{
				System.out.println("BAD");
			}


		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
}

		/*

		// Declare two numeric attributes
		 Attribute Attribute1 = new Attribute("firstNumeric");
		 Attribute Attribute2 = new Attribute("secondNumeric");

		 // Declare a nominal attribute along with its values
		 FastVector fvNominalVal = new FastVector(3);
		 fvNominalVal.addElement("blue");
		 fvNominalVal.addElement("gray");
		 fvNominalVal.addElement("black");
		 Attribute Attribute3 = new Attribute("aNominal", fvNominalVal);

		 // Declare the class attribute along with its values

		 // Declare the feature vector
		 FastVector fvWekaAttributes = new FastVector(4);
		 fvWekaAttributes.addElement(Attribute1);
		 fvWekaAttributes.addElement(Attribute2);
		 fvWekaAttributes.addElement(Attribute3);
		 fvWekaAttributes.addElement(ClassAttribute);

		 // Create an empty training set
		 Instances isTrainingSet = new Instances("Rel", fvWekaAttributes, 10);
		 // Set class index
		 isTrainingSet.setClassIndex(3);

		 // Create the instance
		 Instance iExample = new Instance(4);
		 iExample.setValue((Attribute)fvWekaAttributes.elementAt(0), 1.0);
		 iExample.setValue((Attribute)fvWekaAttributes.elementAt(1), 0.5);
		 iExample.setValue((Attribute)fvWekaAttributes.elementAt(2), "gray");
		 iExample.setValue((Attribute)fvWekaAttributes.elementAt(3), "positive");

		 // add the instance
		 isTrainingSet.add(iExample);

		 //Classifier cModel = (Classifier)new RandomForest();

		 RandomForest randomForest = new RandomForest();


		 // Create a naïve bayes classifier
		 //Classifier cModel = (Classifier)new NaiveBayes();
		 try {
			 randomForest.buildClassifier(isTrainingSet);

			System.out.println( "number of trees " + randomForest.getNumTrees() );

//			Instances iUse = new Instances("Test", fvWekaAttributes, 10);
//			iUse.

			/*
			 // Test the model
			 Evaluation eTest = new Evaluation(isTrainingSet);
			 eTest.evaluateModel(cModel, isTestingSet);


			// Print the result à la Weka explorer:
			 String strSummary = eTest.toSummaryString();
			 System.out.println(strSummary);

			 // Get the confusion matrix
			 double[][] cmMatrix = eTest.confusionMatrix();

			 // Specify that the instance belong to the training set
			 // in order to inherit from the set description
			 iUse.setDataset(isTrainingSet);

			 // Get the likelihood of each classes
			 // fDistribution[0] is the probability of being “positive”
			 // fDistribution[1] is the probability of being “negative”
			 double[] fDistribution = cModel.distributionForInstance(iUse);
			 */




		 //FastRandomForest f = new FastRandomForest();

		 //f.buildClassifier( isTrainingSet );


		//Instances data = DataSource.read(Utils.getOption("t", args));

		//Instances data = new Instances( )



		//Instances instances = new Instances(dataset)

		//f.buildClassifier( trainingData );

	}



}
