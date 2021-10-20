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

import java.util.Random;

import weka.attributeSelection.InfoGainAttributeEval;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Test avec 1 set d'apprentissage par time point
 * @author Fab
 *
 */
public class LearningTest3 extends PluginActionable {

	/*
	 * t = number of time point
	 */
	private Instances buildTrainingSet( int t ) {

		// attributs
		Attribute Attribute1 = new Attribute("Volume");
		Attribute Attribute2 = new Attribute("GrayIntensity");
		Attribute Attribute3 = new Attribute("InstantSpeed");

		// class

		FastVector fvClassVal = new FastVector(2);
		fvClassVal.addElement("Animal 0");
		fvClassVal.addElement("Animal 1");
		Attribute classAttribute = new Attribute("Animal Class", fvClassVal);

		// features = attributs + class
		FastVector fvWekaAttributes = new FastVector(4);
		fvWekaAttributes.addElement(classAttribute);
		fvWekaAttributes.addElement(Attribute1);
		fvWekaAttributes.addElement(Attribute2);
		fvWekaAttributes.addElement(Attribute3);

		// Create the training set
		Instances trainingSet = new Instances("Training", fvWekaAttributes, 10);

    	// Set class index (first column here)
		trainingSet.setClassIndex(0);

		buildAnimal( 0 , t , fvWekaAttributes , trainingSet );
		buildAnimal( 1 , t , fvWekaAttributes , trainingSet );

		return trainingSet;
	}

	private void buildAnimal(int animalIndex, int t , FastVector fvWekaAttributes, Instances trainingSet ) {

		for ( int i = 0 ; i < t ; i++ )
		{
			Instance iExample = buildAnimal( animalIndex , fvWekaAttributes );
			trainingSet.add(iExample);
		}

	}

	private Instance buildAnimal( int animalIndex , FastVector fvWekaAttributes ) {

		int a = (int)(Math.random()* 20 ) + animalIndex * 10 ;
		int b = (int)(Math.random()* 20 ) - animalIndex * 10 ;
		int c = (int)(Math.random()* 100 ) + animalIndex * 30 ;
		Instance iExample = new DenseInstance(4);

		iExample.setValue((Attribute)fvWekaAttributes.elementAt(0), "Animal " + animalIndex );
		iExample.setValue((Attribute)fvWekaAttributes.elementAt(1), a );
		iExample.setValue((Attribute)fvWekaAttributes.elementAt(2), b );
		iExample.setValue((Attribute)fvWekaAttributes.elementAt(3), c );

		return iExample;
	}

	@Override
	public void run() {

		Instances trainingSet = buildTrainingSet( 30 );

		RandomForest randomForest = new RandomForest();

		try {
			randomForest.buildClassifier( trainingSet );
		} catch (Exception e) {
			e.printStackTrace();
		}

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
		System.out.println("Max depth: " + randomForest.getMaxDepth() );

		//
		//
		//   TESTS
		//
		//



/*
		for ( int i = 0 ; i<10 ; i++ )
		{
			try {

				buildAnimal( 0 , trainingSet );

				//System.out.println("-----");
				Instance iExample = new Instance(3);
				int a = (int)(Math.random()* 100 ) - 50 ;
				int b = (int)(Math.random()* 100 ) - 50 ;
				int c = (int)(Math.random()* 100 ) - 50 ;
				iExample.setDataset( trainingSet );
				System.out.println("a:" + a + "** b:" + b + " c: " + c);
				boolean isPositif = ( ( a >0 ) || ( b > 0 ) );

				iExample.setValue((Attribute)fvWekaAttributes.elementAt(0), a );
				iExample.setValue((Attribute)fvWekaAttributes.elementAt(1), b );
				iExample.setValue((Attribute)fvWekaAttributes.elementAt(2), c );

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
		}		*/


	}




}
