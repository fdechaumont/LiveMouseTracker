package plugins.fab.aaa.voc;

import java.io.File;
import java.util.ArrayList;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import icy.main.Icy;
import icy.plugin.abstract_.PluginActionable;
import icy.preferences.XMLPreferences;
import icy.sequence.Sequence;


/**
 * @author Fab
 *
 */
public class AudioFileBatchLabelEdition extends PluginActionable {

	Thread processingThread = null;

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

		File[] files = getFiles("Load voc files");
		if ( files == null )
		{
			System.out.println("No file(s) selected.");
			return;
		}

		processingThread = new Thread( new Runnable() {

			@Override
			public void run() {
				for ( File file : files )
				{
					FullVocProcessor processor = new FullVocProcessor( true ,null );
					processor.showSequence = true;
					try {
						processor.process( file );
					} catch (USVProcessingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					for ( Sequence sequence : Icy.getMainInterface().getSequences() )
					{
						sequence.getFirstViewer().close();
					}

					/*
					// wait for viewer to be created.
					try {
						Thread.sleep( 1000 );
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}


					// wait for viewer to close before loading next.
					while ( processor.sequence.getFirstViewer() != null )
					{
						try {
							Thread.sleep( 100 );
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					*/

				}
			}
		});

		processingThread.start();


	}

}
