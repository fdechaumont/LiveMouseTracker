package plugins.fab.aaa.voc.ui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.prefs.Preferences;

import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileNameExtensionFilter;

import icy.file.FileUtil;
import icy.gui.frame.IcyFrame;
import icy.plugin.abstract_.PluginActionable;
import icy.plugin.interface_.PluginThreaded;
import icy.system.thread.Processor;
import icy.system.thread.ThreadUtil;
import plugins.fab.aaa.voc.FullVocProcessor;
import plugins.fab.aaa.voc.USVProcessingException;
import plugins.fab.aaa.voc.VocAnalysisListener;
import plugins.fab.aaa.voc.VocalizationOverlay.DrawMode;



// need to export all the fab.aaa.voc package
// + the morpho package from LMT
// + drawUtil class from LMT main package

public class USVProcessing extends PluginActionable implements PluginThreaded, ActionListener , VocAnalysisListener {

	public static USVProcessingPanel guiPanel = new USVProcessingPanel();
	IcyFrame mainFrame = new IcyFrame("USV Processing", true, true , true ,true);

	ArrayList<File> fileToProcess = new ArrayList<>();
	boolean processing = false;
	int maxNumberOfThread = 8;

	@Override
	public void run() {


		mainFrame.getContentPane().setLayout( new BoxLayout( mainFrame.getContentPane(), BoxLayout.PAGE_AXIS ) );
		mainFrame.getContentPane().add( guiPanel );

		mainFrame.setSize( new Dimension( 400 , 600 ) );
		mainFrame.addToDesktopPane();
		mainFrame.center();
		mainFrame.setVisible( true );

		guiPanel.getBtnStartUsvProcessing().addActionListener( this );
		guiPanel.getBtnAddFilesTo().addActionListener( this );

		updateUI();

	}


	private void checkStartCondition() {

		if ( fileToProcess.size() == 0 || processing )
		{
			guiPanel.getBtnStartUsvProcessing().setEnabled( false );
		}else
		{
			guiPanel.getBtnStartUsvProcessing().setEnabled( true );
		}

		guiPanel.getBtnAddFilesTo().setEnabled( !processing );

	}

	public void log( String log )
	{
		/*
		System.out.println( log );
		JTextArea logArea = guiPanel.getLogTextArea();
		String text = logArea.getText();
		text+= log + "\n";
		logArea.setText( text );
		logArea.setCaretPosition(logArea.getDocument().getLength());
		*/
	}

	@Override
	public void onAnalysisStatusUpdate(String status) {
		log ( status );
	}

	Processor processor = null;

	private void processBatch() {

		guiPanel.getLogTextArea().setText("starting process...\n");

		File firstFile = fileToProcess.get( 0 );

		File allResultsFile = new File ( FileUtil.getDirectory( firstFile.getAbsolutePath() ) + "/usv all results.csv" );
		log ( "Creating file with all results : " + allResultsFile.getAbsolutePath() );
		allResultsFile.delete();

		ExecutorService threadPool = ThreadUtil.createThreadPool( "Processing USVs");
				
		// max thread
		String optionString = guiPanel.getOptionField().getText();
		String[] optionArray = optionString.split(" ");
		for ( int i = 0 ; i < optionArray.length ; i++ ) 
		{
			if ( optionArray[i].equals("-maxThread") )
			{
				int maxThread = Integer.parseInt( optionArray[i+1] );
				System.out.println("Setting max number of thread to: " + maxThread );
				maxNumberOfThread = maxThread;
			}
		}
		
		processor = new Processor( 200000, maxNumberOfThread, Processor.NORM_PRIORITY );
		
		System.out.println("Loading pre processors...");
		
		for ( File file : fileToProcess )
		{

			processor.submit( new Runnable() {

				@Override
				public void run() {
					log( "--------------");
					log( "Processing file : " + file.getName() );
					String resultDir = FileUtil.getDirectory( file.getAbsolutePath() )+"/USV_result_"+ FileUtil.getFileName( file.getName(), false ) +"/";
					System.out.println("creating dir " + resultDir );
					FileUtil.ensureParentDirExist( resultDir );
					process ( file , resultDir , allResultsFile );
					
				}		
			});
			
		}
		
		System.out.println("Processor load finished.");

		/*
		Thread thread = new Thread() {
			@Override
			public void run() {
				try
				{
					for ( File file : fileToProcess )
					{
						log( "--------------");
						log( "Processing file : " + file.getName() );
						String resultDir = FileUtil.getDirectory( file.getAbsolutePath() )+"/USV_result_"+ FileUtil.getFileName( file.getName(), false ) +"/";
						System.out.println("creating dir " + resultDir );
						FileUtil.ensureParentDirExist( resultDir );
						process ( file , resultDir , allResultsFile );
					}
				}
				finally
				{
					processing = false;
					updateUI();
					onAnalysisStatusUpdate("Process finished.");
				}
			}
		};
		
		thread.start();
		*/

	}


	private void process( File inputFile, String outputFolder , File allResultsFile ) {

		System.out.println("Starting processing thread..." );
		System.out.println("File: " + inputFile );
		System.out.println("OutputFolder: " + outputFolder );

		FullVocProcessor processor = new FullVocProcessor( false , null );
		if ( inputFile.getAbsoluteFile().toString().toLowerCase().contains("pup") )
		{
			processor.setPupMode( true );
			System.out.println("PUP MODE activated");
			log("PUP MODE activated");
		}

		
		// options
		String optionString = guiPanel.getOptionField().getText();
		String[] optionArray = optionString.split(" ");
				
		if (optionString.indexOf("-pup") != -1)
		{
			processor.setPupMode( true );
			System.out.println("PUP MODE activated");
			log("PUP MODE activated");
		}
		
		if (optionString.indexOf("-noCancelFrequency") != -1)
		{
			processor.cancelFrequency = false;
			System.out.println("No cancel frequency");
		}
		
		
		
		// float detectionThreshold = 0.1f;
		for ( int i = 0 ; i < optionArray.length ; i++ ) 
		{
			if ( optionArray[i].equals("-detectionThreshold") )
			{
				float detectionThreshold = Float.parseFloat( optionArray[i+1] );
				System.out.println("Setting detection threshold to: " + detectionThreshold );
				processor.detectionThreshold = detectionThreshold;
			}
		}

		
		
		
		processor.setCloseAfterProcessing( true );
		processor.setSaveHTML( false );
		
		if ( guiPanel.getDetailedOutputCheckBox().isSelected() || (optionString.indexOf("-detailedOutput") != -1) )
		{		
			processor.setAllResultsFile( allResultsFile );
			processor.addVocAnalysisListener ( this );
			processor.setProcessAsWebProcessor( true );
			processor.setWebOutProcessorFolder( outputFolder );
			processor.setSaveHTML( true );
			processor.setDrawMode( DrawMode.WEB );
			processor.MANAGE_GROUND_TRUTH = false;
			processor.setCloseAfterProcessing( true );
			processor.SAVE_DATA_EXTRACTED = false;
		}

		
				
				
		
		try
		{
			processor.process( inputFile );
		}catch( java.lang.OutOfMemoryError e )
		{
			e.printStackTrace();
			System.out.println("Not enough memory (RAM) on usv-worker." );
		} catch (USVProcessingException e) {
			e.printStackTrace();
		}

		System.out.println("--------------- DONE");
		processing= false;
		
		updateUI();
		
	}

	private void updateUI() {

		JTextArea textArea = guiPanel.getTextArea();

		String text = "File(s) to process:\n\n";
		/*
		for ( File file : fileToProcess )
		{
			text+=" - " + file.getName()+"\n";
		}		
		if ( fileToProcess.size() == 0 )
		{
			text+=" - No file to process.";
		}
		*/
		
		{
			text+= fileToProcess.size()+  " file(s) to process.";
		}

		textArea.setText( text );
		textArea.setCaretPosition(textArea.getDocument().getLength());
		checkStartCondition();
	}



	@Override
	public void actionPerformed(ActionEvent e) {

		if ( e.getSource() == guiPanel.getBtnStartUsvProcessing() )
		{
			// start processing
			processing = true;
			guiPanel.getBtnStartUsvProcessing().setEnabled( false );
			processBatch();
		}

		if ( e.getSource() == guiPanel.getBtnAddFilesTo() )
		{
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setDialogType(JFileChooser.FILES_ONLY);
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

			Preferences preferences = Preferences.userRoot().node("icy/vocbrowser");
			String browserDirectory = preferences.get("path", "");

			if(browserDirectory != "")
				fileChooser.setCurrentDirectory(new File(browserDirectory));

			FileNameExtensionFilter filter = new FileNameExtensionFilter("Wav files", "wav", "wav files");
			fileChooser.setFileFilter(filter);
			fileChooser.setMultiSelectionEnabled( true );

			int returnValue = fileChooser.showDialog( null , "Select wav files to process.");

			if (returnValue == JFileChooser.APPROVE_OPTION)
			{
				preferences.put("path", fileChooser.getCurrentDirectory().getPath() );
				File[] files = fileChooser.getSelectedFiles();
				for ( File file : files )
				{
					if ( file.isDirectory() ) continue;
					//System.out.println( file );
					fileToProcess.add( file );
				}
				updateUI();
			}


		}



	}






}
















