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
package plugins.fab.livemousetracker.postprocessdatabase;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import icy.plugin.abstract_.PluginActionable;
import icy.util.XLSUtil;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;



import icy.plugin.abstract_.PluginActionable;
import icy.plugin.interface_.PluginThreaded;
import icy.system.profile.Chronometer;

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
 * Test data with a machine learning;
 * */
public class PostProcessMLForest extends PluginActionable {


	@Override
	public void run() {

		/*
		try {

			// DATA LOAD

			Workbook workbook = XLSUtil.loadWorkbookForRead( new File( "c:/base/data.xls " ) );

			Sheet sheet;

			sheet = workbook.getSheet( 0 );

			int nbCol = sheet.getColumns();
			System.out.println("Nb col: " + nbCol );
			int nbRow = sheet.getRows();
			System.out.println("Nb row: " + nbRow );

			String[] dataHeader = new String[ nbCol ];
			ArrayList<String[]> data = new ArrayList<String[]>();

			for ( int x = 0 ; x < nbCol ; x++ )
			{
				dataHeader[x] = sheet.getCell( x , 0 ).getContents();
			}

			for ( int y = 1 ; y < nbRow ; y++ )
			{
				String d[] = new String[nbCol];
				for ( int x = 0 ; x < nbCol ; x++ )
				{
					d[x] = sheet.getCell( x , y ).getContents();
				}
				data.add( d );
			}


			// DATA PREPARE

			ArrayList<Attribute> fvClassVal = new ArrayList<Attribute>();
			fvClassVal.add( new Attribute( "WT",0 ) );
			fvClassVal.add( new Attribute( "KO", 0 ) );
			//Attribute classAttribute = new Attribute("Phenotype", fvClassVal);

			nbCol = 40;
			ArrayList<Attribute> fvWekaAttributes = new ArrayList<Attribute>();
//			FastVector fvWekaAttributes = new FastVector( nbCol );

			fvWekaAttributes.addElement(classAttribute);

			for ( int x = 5 ; x < nbCol ; x++ )
			{
				Attribute attribute = new Attribute( dataHeader[x] );
				fvWekaAttributes.addElement(attribute);
			}


			if ( true )
			{
				return;
			}

			Instances trainingSet = new Instances("Training", fvWekaAttributes, nbCol );

			trainingSet.setClassIndex(0);

			// fill with data

			for ( int y = 1 ; y < nbRow ; y++ )
			{
				String[] dataLine= data.get( y );
				for ( int x = 5 ; x < nbCol ; x++ )
				{
					Instance iExample = new DenseInstance( nbCol );
					iExample.setValue((Attribute)fvWekaAttributes.elementAt( x ),
							dataLine[x] );
				}
			}

			RandomForest randomForest = new RandomForest( );
			try {

				randomForest.buildClassifier( trainingSet );
			} catch (Exception e1) {
				e1.printStackTrace();
			}

			evaluate( randomForest, trainingSet );

		} catch (BiffException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

	private void evaluate(RandomForest classifier, Instances set ) {

		System.out.println( classifier );
		boolean full= true;
		double correct = 0;

		Evaluation eval;
		try {
			eval = new Evaluation( set );
			Random rand = new Random(1);  // using seed = 1
			int folds = 10;
			eval.crossValidateModel( classifier , set , folds, rand);

			if ( full )
			{
				System.out.println(eval.toSummaryString());
				System.out.println(eval.toClassDetailsString());
			}

			System.out.println(eval.toMatrixString());
			correct = eval.correct();
			System.out.println( "Correct:  " + eval.correct() );

			if ( full )
			{
				InfoGainAttributeEval igae = new InfoGainAttributeEval();
				igae.buildEvaluator( set );

				System.out.println("InfoGain");
				for (int i = 0; i < set.numAttributes(); i++) {

					Attribute t_attr = set.attribute(i);
					double infogain  = igae.evaluateAttribute( i );
					if ( infogain > 0 )
					{
						System.out.println( t_attr );
						System.out.println("Attribute #"+i + " : " + infogain );
					}
				}
			}
			System.out.println("---");

		} catch (Exception e1) {
			e1.printStackTrace();
		}

		System.out.println( correct );

*/
	}


}
