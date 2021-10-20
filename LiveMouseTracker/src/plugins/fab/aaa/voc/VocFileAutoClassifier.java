package plugins.fab.aaa.voc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import icy.file.FileUtil;
import icy.file.Saver;
import icy.main.Icy;
import icy.painter.Overlay;
import icy.plugin.abstract_.PluginActionable;
import icy.plugin.interface_.PluginThreaded;
import icy.preferences.XMLPreferences;
import icy.sequence.Sequence;
import weka.classifiers.Classifier;
import weka.core.Instance;
/**
 *
 * @author Fab
 *
 */
public class VocFileAutoClassifier extends PluginActionable implements PluginThreaded {

//	ArrayList<Thread> threadList = new ArrayList<Thread>();

	File[] getFiles( String title )
	{
		JFileChooser fileChooser = new JFileChooser();

		XMLPreferences preferences = this.getPreferences("voc folder");
		String browserDirectory = preferences.get("path "+title, "");
		if(browserDirectory != "")
		{
			fileChooser.setCurrentDirectory(new File(browserDirectory));
		}

		fileChooser.setDialogTitle( title );
		fileChooser.setMultiSelectionEnabled( true );
		FileNameExtensionFilter filter = new FileNameExtensionFilter( "Wav (vocalization) files", "wav" );
		fileChooser.setFileFilter(filter);

		int result = fileChooser.showOpenDialog( null );
		if (result == JFileChooser.APPROVE_OPTION)
		{
			preferences.put("path "+title, fileChooser.getCurrentDirectory().getPath() );
			return fileChooser.getSelectedFiles();
		}
		return null;
	}


	@Override
	public void run() {

		System.out.println("Vocalization Auto Classifier. Noise vs Signal.");

		File classifierFileOnDisk = new File("vocNoise.model");

		Classifier classifier = null;


		try {
			System.out.println("Reading classifier from disk model...");
			System.out.println( classifierFileOnDisk.getAbsolutePath() );
			System.out.println( classifierFileOnDisk.getAbsolutePath() );
			classifier = (Classifier) weka.core.SerializationHelper.read( classifierFileOnDisk.getAbsolutePath() );

		} catch (Exception e3) {
			e3.printStackTrace();
		}

		if ( classifier != null  )
		{
			int dialogResult = JOptionPane.showConfirmDialog (null, "Classifier found on disk. Use it (yes) ? or rebuild from noise/voc files ? (no)","Load from disk ?", JOptionPane.YES_OPTION );
			if(dialogResult != JOptionPane.YES_OPTION){
				classifier = null;
			}
		}



		MachineLearningVocSetBuilder mlvst = new MachineLearningVocSetBuilder();

		if ( classifier == null )
		{


			File[] noiseFiles = getFiles("noise");
			File[] withVocFiles = getFiles("voc");

			AudioFileSet noiseSet = new AudioFileSet( noiseFiles, "noise" );
			AudioFileSet vocalizationSet = new AudioFileSet( withVocFiles, "voc" );

			System.out.println( "Noise size: " + noiseSet.audioFileList.size() );

			ArrayList<AudioFileSet> fullSet = new ArrayList<AudioFileSet>();
			fullSet.add( noiseSet );
			fullSet.add( vocalizationSet );

			System.out.println("Building machine learning voc builder...");
			//MachineLearningVocSetBuilder mlvst = new MachineLearningVocSetBuilder();
			mlvst.buildSet( fullSet );
			classifier = mlvst.buildClassifier();

			// saving classifier model.

			int dialogResult = JOptionPane.showConfirmDialog (null, "Save classification for further use ?", "Save classification ?", JOptionPane.YES_OPTION );
			if(dialogResult == JOptionPane.YES_OPTION){

				try {
					System.out.println("Save classifier (model) to disk.");
					System.out.println( classifierFileOnDisk.getAbsolutePath() );
					weka.core.SerializationHelper.write( classifierFileOnDisk.getAbsolutePath() , classifier );
				} catch (Exception e3) {
					e3.printStackTrace();
				}

			}

		}


		/*
		try {
			System.out.println("Read classifier from disk model.");
			classifier = (Classifier) weka.core.SerializationHelper.read("vocNoise.model");
		} catch (Exception e3) {
			e3.printStackTrace();
		}
		*/

		/*
		System.out.println("Evaluating...");
		mlvst.evaluate( null , true );
		 */

		File[] fileToClassify = getFiles("Voc to classify");

		/*
		fullSet = null;
		noiseSet = null;
		vocalizationSet = null;
		noiseFiles = null;
		withVocFiles = null;
		*/

//		VocalizationSet toClassifySet = new VocalizationSet( fileToClassify, null );

		File htmlFile = FileUtil.createFile( new File("c:/vocLogOut/index.html") );
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter( htmlFile ));
		} catch (IOException e2) {
			e2.printStackTrace();
		}

		FileUtil.delete( FileUtil.getDirectory( fileToClassify[0].getAbsolutePath() ) +"voc" , true );
		FileUtil.delete( FileUtil.getDirectory( fileToClassify[0].getAbsolutePath() ) +"noise" , true );


		for ( File file : fileToClassify )
		{
			try {
			FullVocProcessor fullProcessor = new FullVocProcessor( false, null );
			fullProcessor.saveHTML = false;
			AudioFile2 audioFile;
				audioFile = fullProcessor.process( file );

			String log ="";

			Instance vocalizationFeatures = mlvst.buildVocalizationFeatures( audioFile, mlvst.dataRaw );
			vocalizationFeatures.setDataset( mlvst.dataRaw );
			double vocProbability = 0;
			try {
				vocProbability = classifier.distributionForInstance( vocalizationFeatures )[1];
			} catch (Exception e) {
				e.printStackTrace();
			}

			// rename voc files.

			{


				String path = FileUtil.getDirectory( audioFile.file.getAbsolutePath(), true );
				String fileName = FileUtil.getFileName( audioFile.file.getAbsolutePath() );
				/*
				if ( fileName.startsWith("n_") || fileName.startsWith("v_") )
				{
					fileName = fileName.substring( 2 );
				}
				 */
				/*
				String textVocProba = String.format("%03d", (int)(vocProbability*100f ) );
				fileName = FileUtil.getFileName( fileName, false )
						+"_p_" + textVocProba
						+"_l_" + audioFile.lengthScore
						+"_c_" + audioFile.complexityScore
						+ ".wav";
				*/

				if ( vocProbability > 0.5 )
				{
					fileName = "/voc/" + fileName;
				}else
				{
					fileName = "/noise/" +fileName;
				}
				File copiedFile = new File( path + fileName );
				FileUtil.copy( audioFile.file, copiedFile, true, true );

			}

//			vocalization.setProba( result );

			/*
data should be organised in a file using the following format:

08/10/2018    15:22:55    Monitoring_started    file    dur_file    length    complexity
08/10/2018    15:41:24    T2018-08-10_15-41-24_    0000001.wav    13,7 s    vvv    vv
08/10/2018    15:41:37    T2018-08-10_15-41-37_    0000002.wav    4,9 s    vv    vv
08/10/2018    15:41:41    T2018-08-10_15-41-41_    0000003.wav    8,0 s    vv    vv
08/10/2018    15:41:49    T2018-08-10_15-41-49_    0000004.wav    15,9 s    vvv    vv
08/10/2018    15:42:04    T2018-08-10_15-42-04_    0000005.wav    48,9 s    vvv    vvv

'''
			 */

			log +="\t";
			log +="date";
			log +="\t";
			log +="time";
			log +="\t";
			log +="monitoring_started";
			log +="\t";
			log += audioFile.file.getName();
			log +="\t";
			log += "" + String.format("%.2f", audioFile.getDurationInSecond() ) + " s";
			log +="\t";
			log += "LengthScore:\t" + audioFile.lengthScore;
			log +="\t";
			log += "ComplexityScore:\t" + audioFile.complexityScore;
			log +="\t";
			log += "probaVoc:\t" + (int)( vocProbability * 100f )+" %";
			log +="\t";
			log += "nbVoc:\t" + audioFile.nbVoc;

			boolean SAVE_IMAGE_TO_FOLDER = false;

			if ( SAVE_IMAGE_TO_FOLDER )
			{
				File imageFileOverlay = FileUtil.createFile( new File("c:/vocLogOut/"+audioFile.file.getName()+"_Overlay.jpg") );
				File imageFile = FileUtil.createFile( new File("c:/vocLogOut/"+audioFile.file.getName()+".jpg") );
				try {
					writer.write ( "<br>"+audioFile.file.getAbsoluteFile() );
					writer.write ( "<br>"+audioFile.file.getName() );
//					writer.write ( "<br>NbVoc: "+vocalization.getNumberOfVoc() );
					writer.write ( "<br>Duration: "+audioFile.getDurationInSecond()+ " s" );
					writer.write ( "<br>Length Score: "+audioFile.lengthScore );
					writer.write ( "<br>Complexity Score: "+audioFile.complexityScore );

					writer.write ( "<br>Proba to be a voc: "+vocProbability );
					writer.write ( "<br><img src=" + imageFile.getName() + ">" );
					writer.write ( "<br><img src=" + imageFileOverlay.getName() + ">" );

					if ( vocProbability>=0.5)
					{
						writer.write ( "<br>VOC" );
					}else
					{
						writer.write ( "<br>NOISE" );
					}

					writer.write ( "<hr>" );
				} catch (IOException e1) {
					e1.printStackTrace();
				}

				//Sequence sequence = new Sequence(); //vocalization.showSequence();
				Sequence sequence = fullProcessor.sequence;
				while ( !Icy.getMainInterface().getSequences().contains( sequence ) )
				{
					try {
						Thread.sleep( 100 );
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				try {
					Thread.sleep( 250 );
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				Saver.save( new Sequence( sequence.getFirstViewer().getCanvas().getRenderedImage( 0, 0, -1 , false ) ), imageFileOverlay , false , false );
				for ( Overlay overlay : new ArrayList<Overlay>(sequence.getOverlays()) )
				{
					sequence.removeOverlay( overlay );
				}
				Saver.save( new Sequence( sequence.getFirstViewer().getCanvas().getRenderedImage( 0, 0, -1 , false ) ), imageFile , false , false );
				sequence.close();

			}

			System.out.println( log );
			} catch (USVProcessingException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}

		}

	    try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}



	}

}
