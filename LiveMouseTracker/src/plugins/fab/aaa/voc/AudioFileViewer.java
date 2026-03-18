package plugins.fab.aaa.voc;

import java.io.File;
import java.util.ArrayList;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import icy.plugin.abstract_.PluginActionable;
import icy.plugin.interface_.PluginThreaded;
import icy.preferences.XMLPreferences;

/**
 * @author Fab
 *
 */
public class AudioFileViewer extends PluginActionable implements PluginThreaded {

	ArrayList<Thread> threadList = new ArrayList<Thread>();

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

		for ( File file : files )
		{
			threadList.add(
					new Thread( new Runnable() {

						@Override
						public void run() {

							FullVocProcessor processor = new FullVocProcessor( true , null );

//							processor.setSaveHTML( true );
							try {
								processor.process( file );
							} catch (USVProcessingException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

						}
					}));
		}
		for ( Thread thread : threadList )
		{
			thread.run();
//			thread.start();
		}
//		for ( Thread thread : threadList )
//		{
//			try {
//				thread.join();
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//		threadList.clear();


	}

}
