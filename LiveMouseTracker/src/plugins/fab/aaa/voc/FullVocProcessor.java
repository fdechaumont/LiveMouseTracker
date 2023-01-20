package plugins.fab.aaa.voc;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.SwingUtilities;

import icy.file.FileUtil;
import icy.file.Saver;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.main.Icy;
import icy.painter.Overlay;
import icy.sequence.Sequence;
import icy.system.profile.Chronometer;
import icy.system.thread.ThreadUtil;
import loci.formats.FormatException;
import plugins.fab.aaa.voc.VocalizationOverlay.DrawMode;

public class FullVocProcessor {

	AudioFile2 audioFile;
	Sequence sequence;
	Sequence sequenceDenoised;
	boolean showSequence = true;
	private static final boolean GENERATE_DENOISED_SPECTRUM_SEQUENCE = false; // false TODOTODAY
	public static boolean SAVE_VOC_PATCHES = false;
	public boolean MANAGE_GROUND_TRUTH = true;
	public boolean clearWavDataAfterLoad = false; // false TODOTODAY
	String htmlSaveFolder = null;
	boolean closeAfterProcessing = false;
	boolean checkRepeatInUSV = false;
	public float detectionThreshold = 0.1f;
	public float amplificationFactor = 1f;
	public boolean cancelFrequency = true;
	public float MAX_GAP_DURATION_BETWEEN_SEQUENCE_TO_FUSE_VOC_IN_MS = 40f;
	public int MIN_Y_IN_SPECTRUM = 100; // 100*300/512 = 58.57 kHz

	public void setCloseAfterProcessing(boolean closeAfterProcessing) {
		this.closeAfterProcessing = closeAfterProcessing;
	}

	public FullVocProcessor( boolean showSequence, Sequence sequenceToUse ) {
		this ( showSequence , sequenceToUse , null );
	}

	public static void deleteResultFolder( String resultFolder )
	{
		FileUtil.delete( FileUtil.getDirectory( resultFolder) , true );
	}

	public FullVocProcessor( boolean showSequence, Sequence sequenceToUse, String htmlSaveFolder ) {
		this.showSequence = showSequence;
		if ( sequenceToUse != null )
		{
			this.sequence = sequenceToUse;
		}
		this.htmlSaveFolder = htmlSaveFolder;
	}

	public AudioFile2 getAudioFile() {
		return audioFile;
	}

	AudioFFTProcessing fftProcessing;
	AudioVocDetection audioVocDetection;

	public AudioFFTProcessing getFftProcessing() {
		return fftProcessing;
	}

	public AudioVocDetection getAudioVocDetection() {
		return audioVocDetection;
	}

	DrawMode drawMode = null;

	public void setDrawMode( DrawMode drawMode )
	{
		this.drawMode = drawMode;
	}



	public AudioFile2 processPart( File file, double startSecond, double endSecond , boolean doCrop ) throws USVProcessingException
	{
		fireStatusToAnalysisListener( "Starting analysis from " + startSecond + "s to " + endSecond + "s");

		this.audioFile = new AudioFile2 ( file, this.amplificationFactor ); // FIx: should not be class member anymore		

		if( doCrop )
		{
			this.audioFile.cropWave( startSecond, endSecond );
		}

		fireStatusToAnalysisListener("Performing fast Fourier transform...");
		fftProcessing = new AudioFFTProcessing(
				audioFile.getWaveFormData(), audioFile.getSampleRate(), 0.75f , 1024 ); // AviSoft is N=1024, F=100, O=75

		fireStatusToAnalysisListener("Performing noise cancellation...");
		new NoiseCanceler( fftProcessing, MIN_Y_IN_SPECTRUM );

		fireStatusToAnalysisListener("Performing frequency canceler...");
		FrequencyCancelerAndSTD frequencyCanceler = new FrequencyCancelerAndSTD( fftProcessing, pupMode, detectionThreshold , cancelFrequency, MAX_GAP_DURATION_BETWEEN_SEQUENCE_TO_FUSE_VOC_IN_MS, MIN_Y_IN_SPECTRUM );

		if ( showSequence )
		{
			fireStatusToAnalysisListener("Performing spectrogram rendering...");
			AudioSpectrumViewer asv = new AudioSpectrumViewer();

			this.sequence = asv.showSequence( fftProcessing.getMagnitudeForAllChannels() , this.sequence );
			sequence.setName( audioFile.file.getName() );
		}

		//if ( true ) return audioFile;

		// show denoised spectrum
		if ( GENERATE_DENOISED_SPECTRUM_SEQUENCE )
		{
			AudioSpectrumViewer asv = new AudioSpectrumViewer();
			sequenceDenoised = new Sequence("Denoised");
			sequenceDenoised = asv.showSequence( fftProcessing.getMagnitudeDenoisedForAllChannels() , sequenceDenoised );
			sequenceDenoised.setName( "Denoised " + audioFile.file.getName() );
			if ( showSequence )
			{
				Icy.getMainInterface().addSequence( sequenceDenoised );
			}
		}
//		if (true )
//		{
//			return null;
//		}

		// TEST

//		double [][] denoChannel = fftProcessing.getMagnitudeDenoised( 0 );
//		double[][][] denoAll = fftProcessing.getMagnitudeDenoisedForAllChannels();
//
////		x = 56
//		System.out.println("TEST");
//		for ( int y = 0 ; y < 100 ; y++ )
//		{
//			System.out.println( denoChannel[56][y] - denoAll[0][56][y] );
//		}


		// FIN TEST

		fireStatusToAnalysisListener("Performing voc detection...");
		audioVocDetection = new AudioVocDetection( fftProcessing , frequencyCanceler );

		fireStatusToAnalysisListener("Performing voc classification...");
		for ( Voc voc : audioVocDetection.getVocList() )
		{
			System.out.println( "Working with voc" );
			System.out.println( voc );
			VocalizationClassifier vocalizationClassifier = new VocalizationClassifier();
			System.out.println( "Instance of classifier passed" );
			vocalizationClassifier.classify( voc );
			System.out.println( "Voc classified" );
		}

		System.out.println( "Sort voc" );
		audioVocDetection.sortVocInTime();
		FileVocalizationClassifier.classify( audioFile , audioVocDetection.getVocList() );

		RepeaterDetection repeaterDetection = null;
		if ( checkRepeatInUSV )
		{
			System.out.println( "Check repeat" );
			fireStatusToAnalysisListener("Performing voc repeat detector...");
			repeaterDetection = new RepeaterDetection ( audioFile, fftProcessing, audioVocDetection.getVocList() );
		}

//		FamilyMaker fm = new FamilyMaker();
//		fm.process( audioFile, audioVocDetection.getVocList(), fftProcessing );

		if ( showSequence )
		{
			fireStatusToAnalysisListener("Rendering spectrogram with overlayed data...");
			VocalizationOverlay vocalizationOverlay =
					new VocalizationOverlay( audioVocDetection.getVocList() , frequencyCanceler, audioFile ,
							fftProcessing, drawMode , startSecond, MAX_GAP_DURATION_BETWEEN_SEQUENCE_TO_FUSE_VOC_IN_MS );

			sequence.addOverlay( vocalizationOverlay );
			if ( checkRepeatInUSV )
			{
				RepeaterOverlay repeaterOverlay = new RepeaterOverlay( repeaterDetection );
				sequence.addOverlay( repeaterOverlay );
			}
		}

		// save voc as patches
		if ( SAVE_VOC_PATCHES )
		{
			for ( Voc voc : audioVocDetection.getVocList() )
			{
				if ( voc.isInBadRepeat ) continue;
				//if ( voc.getDurationInMs() < 40 ) continue;

				Rectangle2D rectangle = voc.getBoundingRectangle( sequenceDenoised.getHeight() , false , 0 ); // without harmonics
				{

//					IcyBufferedImage image = IcyBufferedImageUtil.getSubImage( sequenceDenoised.getFirstImage(),
//							(int)rectangle.getMinX(), (int)rectangle.getMinY(), (int)rectangle.getWidth(), (int)rectangle.getHeight() );
//					IcyBufferedImage scaledImage = IcyBufferedImageUtil.scale( image, 32,32 );
//
					int height = sequenceDenoised.getFirstImage().getHeight();
					IcyBufferedImage image = IcyBufferedImageUtil.getSubImage( sequenceDenoised.getFirstImage(),
							(int)rectangle.getMinX(), (int)(height*0.2), (int)rectangle.getWidth(), (int)(height*0.8) );
					IcyBufferedImage scaledImage = IcyBufferedImageUtil.scale( image, 64, 128 );

					File imageFileScaled = new File( "e:/vocNormalized/"+
							//FileUtil.getFileName( audioFile.file.getAbsolutePath(), false ).substring(21) +" ts_" + voc.getStartX() +"s.tif" );
							FileUtil.getFileName( audioFile.file.getAbsolutePath(), false ) +" ts_" + voc.getStartX() +"s.tif" );
					File imageFile = new File( "e:/vocNormalized/"+
							FileUtil.getFileName( audioFile.file.getAbsolutePath(), false ) +" ts_" + voc.getStartX() +".tif" );

					try {
						Saver.saveImage( scaledImage, imageFileScaled, true );
						Saver.saveImage( image, imageFile, true );
					} catch (FormatException | IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

		if ( MANAGE_GROUND_TRUTH )
		{
			GroundTruthOverlay groundTruthOverlay = new GroundTruthOverlay( audioFile, fftProcessing, audioVocDetection.getVocList() );
			if ( sequence != null )
			{
				sequence.addOverlay( groundTruthOverlay );
			}
//			GroundTruthScorer.score( groundTruthOverlay , audioFile , audioVocDetection.getVocList() );
		}

		for ( Voc voc : audioVocDetection.getVocList() )
		{
			voc.setTimeOffsetMs( startSecond*1000 );
			allVocDetectionList.add( voc );
		}

//		if ( clearWavDataAfterLoad )
//		{
//			audioFile.clearHeavyData();
//		}

		fireStatusToAnalysisListener("Saving spectrograms...");
		saveToHtml();

		fireStatusToAnalysisListener("Saving additional result files...");
		saveDataExtracted( audioVocDetection.vocList , audioFile );

		if ( clearWavDataAfterLoad )
		{
			audioFile.clearHeavyData();
		}

		// TODOTODAY
		if ( closeAfterProcessing )
		{
			if ( sequence != null )
			{
				sequence.close();
				this.sequence = null;
			}
		}
		

		return audioFile;
	}

	String part = "";

	public AudioFile2 process( File file ) throws USVProcessingException
	{
		if ( showSequence == false && saveHTML == true )
		{
			System.out.println("Switching show sequence to true for saveHTML purposes");
			showSequence = true;
		}
		fireStatusToAnalysisListener( "Opening file...");

		System.out.println("TEST 01");
		// cut in 50 secs part to save data.
		this.audioFile = new AudioFile2 ( file, this.amplificationFactor );
	
		double totalDuration = audioFile.getDurationInSecond();
		this.totalDurationMs = audioFile.getDurationInMilliSecond();

		System.out.println( "Total duration (s): " + totalDuration );

		int maxTimeSegment = 50; // FOR TEST
		//int maxTimeSegment = 50; // 50s for less than 65000 image width in html output.

		if ( totalDuration < maxTimeSegment )
		{
			System.out.println("Process without crop");
			System.out.println("TEST 02");
			processPart( file, 0, maxTimeSegment , false );
			System.out.println("TEST 03");
			//return audioFile;
		}else
		{
			for ( double startOffset = 0 ; startOffset < totalDuration ; startOffset+=maxTimeSegment )
			{
				int totalPart = (int)( totalDuration / maxTimeSegment ) + 1;
				int currentPart = (int)( startOffset / maxTimeSegment ) +1;
				part = "Step " + currentPart + " / " + totalPart + " - ";

				System.out.println("Processing with crop : " + startOffset );
				//this.audioFile.cropWave( 0 , 50 );
				processPart(file, startOffset, startOffset+maxTimeSegment , true );
				//Icy.getMainInterface().closeAllViewers();
				this.closeAfterProcessing = true;
			}
			System.out.println("Multiple crop: done");
		}
		part ="";
		AudioVocDetection.sortVocInTime( allVocDetectionList );


		System.out.println("htmlSaveFolder : " + htmlSaveFolder );
		if ( htmlSaveFolder != null )
		{

			String resultFileName2 = htmlSaveFolder+"acoustic data.csv";
			System.out.println( "Saving to " + resultFileName2 );
			File resultFile2 = FileUtil.createFile( new File( resultFileName2) );
			VocalisationLabelExporter.export( resultFileName2 , this.audioFile , getAllAudioVocDetectionList() , false, allResultsFile );
		}

		fireStatusToAnalysisListener( "Processing done.");

		return audioFile;
//
//		fftProcessing = new AudioFFTProcessing(
//				audioFile.getWaveFormData(), audioFile.getSampleRate(), 0.75f , 1024 ); // AviSoft is N=1024, F=100, O=75
//
//		new NoiseCanceler( fftProcessing );
//
//		FrequencyCancelerAndSTD frequencyCanceler = new FrequencyCancelerAndSTD( fftProcessing );
//
//		if ( showSequence )
//		{
//			AudioSpectrumViewer asv = new AudioSpectrumViewer();
//
//			this.sequence = asv.showSequence( fftProcessing.getMagnitudeForAllChannels() , this.sequence );
//			sequence.setName( audioFile.file.getName() );
//		}
//
//
//		//if ( true ) return audioFile;
//
//		// show denoised spectrum
//		if ( GENERATE_DENOISED_SPECTRUM_SEQUENCE )
//		{
//			AudioSpectrumViewer asv = new AudioSpectrumViewer();
//			sequenceDenoised = new Sequence("Denoised");
//			sequenceDenoised = asv.showSequence( fftProcessing.getMagnitudeDenoisedForAllChannels() , sequenceDenoised );
//			sequenceDenoised.setName( "Denoised " + audioFile.file.getName() );
//			if ( showSequence )
//			{
//				Icy.getMainInterface().addSequence( sequenceDenoised );
//			}
//		}
////		if (true )
////		{
////			return null;
////		}
//
//		// TEST
//
////		double [][] denoChannel = fftProcessing.getMagnitudeDenoised( 0 );
////		double[][][] denoAll = fftProcessing.getMagnitudeDenoisedForAllChannels();
////
//////		x = 56
////		System.out.println("TEST");
////		for ( int y = 0 ; y < 100 ; y++ )
////		{
////			System.out.println( denoChannel[56][y] - denoAll[0][56][y] );
////		}
//
//
//		// FIN TEST
//
//		audioVocDetection = new AudioVocDetection( fftProcessing , frequencyCanceler );
//
//		for ( Voc voc : audioVocDetection.getVocList() )
//		{
//			VocalizationClassifier vocalizationClassifier = new VocalizationClassifier();
//			vocalizationClassifier.classify( voc );
//		}
//
//		audioVocDetection.sortVocInTime();
//		FileVocalizationClassifier.classify( audioFile , audioVocDetection.getVocList() );
//
//		RepeaterDetection repeaterDetection = new RepeaterDetection ( audioFile, fftProcessing, audioVocDetection.getVocList() );
//
////		FamilyMaker fm = new FamilyMaker();
////		fm.process( audioFile, audioVocDetection.getVocList(), fftProcessing );
//
//		if ( showSequence )
//		{
//			VocalizationOverlay vocalizationOverlay =
//					new VocalizationOverlay( audioVocDetection.getVocList() , frequencyCanceler, audioFile ,
//							fftProcessing );
//			RepeaterOverlay repeaterOverlay = new RepeaterOverlay( repeaterDetection );
//			sequence.addOverlay( vocalizationOverlay );
//			sequence.addOverlay( repeaterOverlay );
//		}
//
//		// save voc as patches
//		if ( SAVE_VOC_PATCHES )
//		{
//			for ( Voc voc : audioVocDetection.getVocList() )
//			{
//				if ( voc.isInBadRepeat ) continue;
//				//if ( voc.getDurationInMs() < 40 ) continue;
//
//				Rectangle2D rectangle = voc.getBoundingRectangle( sequenceDenoised.getHeight() , false , 0 ); // without harmonics
//				{
//
////					IcyBufferedImage image = IcyBufferedImageUtil.getSubImage( sequenceDenoised.getFirstImage(),
////							(int)rectangle.getMinX(), (int)rectangle.getMinY(), (int)rectangle.getWidth(), (int)rectangle.getHeight() );
////					IcyBufferedImage scaledImage = IcyBufferedImageUtil.scale( image, 32,32 );
////
//					int height = sequenceDenoised.getFirstImage().getHeight();
//					IcyBufferedImage image = IcyBufferedImageUtil.getSubImage( sequenceDenoised.getFirstImage(),
//							(int)rectangle.getMinX(), (int)(height*0.2), (int)rectangle.getWidth(), (int)(height*0.8) );
//					IcyBufferedImage scaledImage = IcyBufferedImageUtil.scale( image, 64, 128 );
//
//					File imageFileScaled = new File( "e:/vocNormalized/"+
//							//FileUtil.getFileName( audioFile.file.getAbsolutePath(), false ).substring(21) +" ts_" + voc.getStartX() +"s.tif" );
//							FileUtil.getFileName( audioFile.file.getAbsolutePath(), false ) +" ts_" + voc.getStartX() +"s.tif" );
//					File imageFile = new File( "e:/vocNormalized/"+
//							FileUtil.getFileName( audioFile.file.getAbsolutePath(), false ) +" ts_" + voc.getStartX() +".tif" );
//
//					try {
//						Saver.saveImage( scaledImage, imageFileScaled, true );
//						Saver.saveImage( image, imageFile, true );
//					} catch (FormatException | IOException e) {
//						e.printStackTrace();
//					}
//				}
//			}
//		}
//
//		if ( MANAGE_GROUND_TRUTH )
//		{
//			GroundTruthOverlay groundTruthOverlay = new GroundTruthOverlay( audioFile, fftProcessing, audioVocDetection.getVocList() );
//			if ( sequence != null )
//			{
//				sequence.addOverlay( groundTruthOverlay );
//			}
////			GroundTruthScorer.score( groundTruthOverlay , audioFile , audioVocDetection.getVocList() );
//		}
//
//		if ( clearWavDataAfterLoad )
//		{
//			audioFile.clearHeavyData();
//		}
//
//		saveToHtml();
//
//		saveDataExtracted( audioVocDetection.vocList );
//
//		if ( closeAfterProcessing )
//		{
//			if ( sequence != null )
//			{
//				sequence.close();
//			}
//		}
//
//		return audioFile;
	}

	public boolean SAVE_DATA_EXTRACTED = true;

	private void saveDataExtracted(ArrayList<Voc> vocList , AudioFile2 audioFile ) {

		System.out.println( "SAVE_DATA_EXTRACTED: " + SAVE_DATA_EXTRACTED );
		if ( !SAVE_DATA_EXTRACTED ) return;

		/*
		String resultFileName = "c:/FullVocProcessorResult/FullVocProcessorResult.txt";
		System.out.println("Saving to " + resultFileName );


		File resultFile = FileUtil.createFile( new File( resultFileName) );
		VocalisationLabelExporter.export( resultFileName , audioFile , vocList, true, null );
		*/

		String resultFileName2 = audioFile.file.getAbsolutePath().toString()+".txt";
		System.out.println( "Saving to " + resultFileName2 );
		//File resultFile2 = FileUtil.createFile( new File( resultFileName2) );
		VocalisationLabelExporter.export( resultFileName2 , audioFile , vocList , false, null );


	}

	private void saveToHtml() {

		if ( !saveHTML ) return;

		System.out.println("Saving to HTML");
		if ( htmlSaveFolder == null )
		{
			htmlSaveFolder = FileUtil.getDirectory( audioFile.file.getAbsolutePath().toString() )+"/result/";
		}

		appendToHTML( "<hr>" );
		appendToHTML( "<br>full file: " + audioFile.file.getAbsolutePath().toString() );
		appendToHTML( "<br>folder: " + FileUtil.getDirectory( audioFile.file.getAbsolutePath().toString() )) ;
		appendToHTML( "<br>file: " + FileUtil.getFileName( audioFile.file.getAbsolutePath().toString() )) ;

		String fileSaveImageBase = this.htmlSaveFolder+audioFile.file.getName();

		// Save images
		File imageFileOverlay = FileUtil.createFile( new File( fileSaveImageBase +"_Overlay.jpg") );
		File imageFile = FileUtil.createFile( new File( fileSaveImageBase+"_Original.jpg") );

		spectroImageFiles.add( imageFile.getAbsolutePath() );
		spectroImageWithOverlayFiles.add( imageFileOverlay.getAbsolutePath() );

		appendToHTML ( "<br><img src=\"" + imageFile.getName() + "\">" );
		appendToHTML ( "<br><img src=\"" + imageFileOverlay.getName() + "\">" );

		//Sequence sequence = sequence;
		while ( !Icy.getMainInterface().getSequences().contains( sequence ) )
		{
			System.out.println("Waiting for sequence display...");
			try {
				Thread.sleep( 100 );
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		class Cont
		{
			Viewer viewer = null;
		}
		final Cont cont = new Cont();


		ThreadUtil.invokeNow( new Runnable() {

			@Override
			public void run() {
				cont.viewer = new Viewer( sequence );
			}
		});

		Viewer sequenceViewer = cont.viewer;
		sequenceViewer.getCanvas().setLayersVisible( true );

		try {
			Thread.sleep( 1000 );
		} catch (InterruptedException e) {
			e.printStackTrace();
		}


		/*
		while ( sequence.getFirstViewer() == null )
		{
			System.out.println("There is no first viewer yet... waiting...");
			System.out.println( "nb viewers: " + Icy.getMainInterface().getViewers().size() );
			try {
				Thread.sleep( 1000 );
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}
		*/

		System.out.println("Saving..");
		Chronometer c = new Chronometer("Save file chronometer");
		System.out.println("Image width: " + sequence.getFirstImage().getWidth() );
		fireStatusToAnalysisListener("Saving spectrograms... Overlay");
		Saver.save( new Sequence(
				sequenceViewer.getCanvas().getRenderedImage( 0, 0, -1 , false ) ),
				imageFileOverlay , false , false );
		sequenceViewer.getCanvas().setLayersVisible( false );
//		for ( Overlay overlay : new ArrayList<Overlay>(sequence.getOverlays()) )
//		{
//			sequence.removeOverlay( overlay );
//		}
		fireStatusToAnalysisListener("Saving spectrograms... Original");
		Saver.save( new Sequence( sequenceViewer.getCanvas().getRenderedImage( 0, 0, -1 , false ) ), imageFile , false , false );
		// enlevé pour gagner du temps ?
		//sequenceViewer.getCanvas().setLayersVisible( true );
		c.displayMs();
//		sequence.close();
		System.out.println("Image Saved.");

		// End save images

	}

	private void appendToHTML(String text) {

		String indexFile = this.htmlSaveFolder+"index.html";
		text+="\n";

		if ( !FileUtil.exists( indexFile ) )
		{
			FileUtil.createFile(indexFile);
		}

		try {
			Files.write(Paths.get( indexFile ), text.getBytes(), StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	boolean saveHTML = true;
	public void setSaveHTML(boolean b) {
		this.saveHTML = b;
	}

	boolean pupMode = false;
	public void setPupMode(boolean b) {

		this.pupMode = b;
		this.detectionThreshold = 0.05f;
		Constant.MAX_Y_IN_SPECTRUM = 512;
	}

	boolean processAsWebProcessor = false;

	public void setProcessAsWebProcessor(boolean b) {

		processAsWebProcessor = b;

	}

	// Needed for voc processor as a webservice

	String webOutPutFolder = null;

	public void setWebOutProcessorFolder(String outputFolder) {
		this.webOutPutFolder = outputFolder;
		this.htmlSaveFolder = outputFolder;
	}

	ArrayList<String> spectroImageFiles = new ArrayList<String>();
	ArrayList<String> spectroImageWithOverlayFiles = new ArrayList<String>();

	public ArrayList<String> getSpectroImageFiles() {

		return spectroImageFiles;
	}

	public ArrayList<String> getSpectroWithOverlayImageFiles() {

		return spectroImageWithOverlayFiles;
	}

	ArrayList<VocAnalysisListener> vocAnalysisListenerList = new ArrayList<>();

	public void addVocAnalysisListener(VocAnalysisListener vocAnalysisListener) {

		vocAnalysisListenerList.add( vocAnalysisListener );

	}

	public void fireStatusToAnalysisListener( String status )
	{
		System.out.println( status );
		for ( VocAnalysisListener val : vocAnalysisListenerList )
		{
			val.onAnalysisStatusUpdate( part + status );
		}
	}

	ArrayList<Voc> allVocDetectionList = new ArrayList<Voc>();

	public ArrayList<Voc> getAllAudioVocDetectionList() {
		return allVocDetectionList;
	}


	double totalDurationMs = 0; // total duration of voc.
	public double getTotalDurationInMilliSecond() {

		return totalDurationMs;
	}

	File allResultsFile = null;

	public void setAllResultsFile(File allResultsFile) {
		this.allResultsFile = allResultsFile;
	}

	// ------------------------------------------------ end of needed for web services












}
