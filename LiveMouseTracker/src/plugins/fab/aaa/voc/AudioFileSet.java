package plugins.fab.aaa.voc;

import java.io.File;
import java.util.ArrayList;

import icy.gui.frame.progress.ProgressFrame;
import icy.system.thread.Processor;

public class AudioFileSet {

	final ArrayList<AudioFile2> audioFileList = new ArrayList<AudioFile2>();
	String name;
	File[] files;

	public AudioFileSet( File[] files, String name ) {
		this.name = name;
		this.files = files;
		loadAndProcessVocalizationSet();
//		for ( Vocalization vocalization : vocalizationList )
//		{
//			vocalization.computeAll();
//		}
	}


	void loadAndProcessVocalizationSet( )
	{

		Processor processor = new Processor( 1 );

		final String MLName = this.name;

		for ( File file : files )
		{
			Runnable r = new Runnable() {


				@Override
				public void run() {
//					AudioFile2 audioFile = new AudioFile2 ( file );
					System.out.println("loadAndProcessVocalizationSet");
					FullVocProcessor fullProcessor = new FullVocProcessor( false , null );
					fullProcessor.saveHTML = false;
					fullProcessor.clearWavDataAfterLoad = true;
					fullProcessor.MANAGE_GROUND_TRUTH = false;
					System.out.println("STEP 01");
					AudioFile2 audioFile;
					try {
						audioFile = fullProcessor.process( file );
						System.out.println("STEP 02");
						audioFile.setMLClass( MLName );
						//voc.computeAll();
						audioFileList.add( audioFile );
						System.out.println("STEP 03");
						System.out.println("Current AudioFileSet-audioFileList size: " + audioFileList.size() );
						System.out.println("STEP 04");
					} catch (USVProcessingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
//					voc.showSequence();
				}
			};

			//r.run();
			processor.submit( r );
		}

		ProgressFrame progress = new ProgressFrame("Loading set " + name + " (" + files.length + " files)");


		while ( processor.isProcessing() )
		{
			progress.setPosition( processor.getCompletedTaskCount() / processor.getLargestPoolSize() );
			try {
				Thread.sleep( 100 );
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}


		progress.close();

	}
}
