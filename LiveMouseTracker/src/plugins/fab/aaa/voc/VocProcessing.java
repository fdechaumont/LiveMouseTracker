package plugins.fab.aaa.voc;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import icy.file.FileUtil;
import icy.gui.frame.progress.ProgressFrame;
import icy.plugin.abstract_.PluginActionable;
import icy.plugin.interface_.PluginThreaded;
import icy.preferences.XMLPreferences;
import icy.system.profile.Chronometer;

public class VocProcessing extends PluginActionable implements PluginThreaded {

	File getFolder( String title )
	{
		JFileChooser fileChooser = new JFileChooser();

		XMLPreferences preferences = this.getPreferences("voc folder");
		String browserDirectory = preferences.get("path "+title, "");
		if(browserDirectory != "")
		{
			fileChooser.setCurrentDirectory(new File(browserDirectory));
		}

		fileChooser.setDialogTitle( title );
		fileChooser.setMultiSelectionEnabled( false );
		fileChooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );

		int result = fileChooser.showOpenDialog( null );
		if (result == JFileChooser.APPROVE_OPTION)
		{
			preferences.put("path "+title, fileChooser.getCurrentDirectory().getPath() );
			return fileChooser.getSelectedFile();
		}
		return null;
	}

	@Override
	public void run() {

		File folderFile = getFolder("voc input folder");

		{
			ProgressFrame pfFolder = new ProgressFrame("Folder processing...");
			Chronometer folderJob = new Chronometer("All voc processing folder " + folderFile.getAbsolutePath());

			File[] listOfFiles = folderFile.listFiles();
			int currentFileIndex = 0;
			for ( File file : listOfFiles )
			{
				if( FileUtil.getFileExtension(file.getAbsolutePath(), false ).toUpperCase().equals("WAV") )
				{
					FullVocProcessor processor = new FullVocProcessor( false , null );
					processor.setSaveHTML( true );
					processor.setCloseAfterProcessing( true );
					try {
						processor.process( file );
					} catch (USVProcessingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				currentFileIndex++;
				pfFolder.setPosition( 100f * currentFileIndex / listOfFiles.length );
			}
			folderJob.displayInSeconds();
			pfFolder.close();
		}

		System.out.println("All processing Finished");

	}


}
