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

import java.awt.Color;
import java.util.ArrayList;

import plugins.fab.livemousetracker.LiveMouseTracker;
import plugins.fab.livemousetracker.detection.MouseDetection;
import weka.classifiers.Classifier;
import weka.classifiers.meta.AdaBoostM1;
import weka.classifiers.trees.RandomForest;
import weka.core.Instance;
import weka.core.Instances;


/**
 * If too much detection are found, this class will filter them with the training and keep the best detections.
 * */
public class MachineLearningDetectionFiltering {

	ArrayList<MouseDetection> rejectedDetectionList = new ArrayList<MouseDetection>();
	//RandomForest classifier;
	Classifier classifier;
	Instances errorSet;

	public MachineLearningDetectionFiltering( Instances errorSet) {

		//System.out.println("Machine learning detection filtering.");

		if ( errorSet == null )
		{
			//System.out.println("Detection Filtering > Training set of error is not ready yet. Can't process.");
			return;
		}

		this.errorSet = errorSet;

		classifier = new AdaBoostM1( );
		((AdaBoostM1)(classifier)).setClassifier( new RandomForest() );
		((AdaBoostM1)(classifier)).setNumIterations( LiveMouseTracker.ADA_BOOST_ITERATION );
		((AdaBoostM1)(classifier)).setUseResampling( LiveMouseTracker.ADA_BOOST_USE_RESAMPLING );

//		classifier = new RandomForest();

		try {
			classifier.buildClassifier( errorSet );
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}



	public void filter( ArrayList<MouseDetection> rawMouseDetectionList, float devCorrection )
	{


		rejectedDetectionList.clear();
		//System.out.println("test");
		for ( MouseDetection mouseDetection : new ArrayList<MouseDetection>( rawMouseDetectionList ) )
		{
			MachineLearningSetBuilder mlSetBuilder = new MachineLearningSetBuilder();

			Instance detectionInstance = mlSetBuilder.buildDetectionFeatures( null, mouseDetection, errorSet );
			if ( detectionInstance == null ) continue;

			detectionInstance.setDataset( errorSet );

			try {
				double[] percentage = classifier.distributionForInstance( detectionInstance );
				mouseDetection.detectionChanceWithMLFilter = percentage[1];
				boolean detectionIsOk = true;
				//System.out.println("ML rej. Detection: " + percentage[1] );
				//if ( percentage[1] > LiveMouseTracker.FILTERING_ANIMAL_VS_UNKNOWN_THRESHOLD )

				if ( mouseDetection.detectionChanceWithMLFilter > LiveMouseTracker.FILTERING_ANIMAL_VS_UNKNOWN_THRESHOLD + devCorrection )
				{
					detectionIsOk = false;
				}
				//System.out.println( "" + mouseDetection + " > " + Arrays.toString( percentage ) + " Ok:" + detectionIsOk );
				if ( detectionIsOk == false )
				{
					rawMouseDetectionList.remove( mouseDetection );
					mouseDetection.getROI2DArea().setName("seg rejected MLDF " + percentage[1]);
					mouseDetection.getROI2DArea().setColor( Color.YELLOW);
					rejectedDetectionList.add( mouseDetection );
//					System.out.println("rejected detection (ML Error set): " + percentage[1] );
				}

			} catch (Exception e) {
				//System.out.println("t:" + LiveMouseTracker.getT()+ " ML filter not working");
				//e.printStackTrace();
			}
		}

	}


	public ArrayList<MouseDetection> getRawMouseDetectionRejectedList() {

		return rejectedDetectionList;
	}



}
