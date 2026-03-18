package plugins.fab.aaa.voc;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import icy.file.FileUtil;
import icy.gui.frame.progress.ProgressFrame;
import icy.plugin.abstract_.PluginActionable;
import icy.plugin.interface_.PluginThreaded;
import icy.system.profile.Chronometer;

public class AllVocProcessing extends PluginActionable implements PluginThreaded {


	@Override
	public void run() {

		String folderList[] =
			{
					"E:/all_data_usv_pairs/20180810_F09_F10_usv_001/voc_F09_F10/voc",
					"E:/all_data_usv_pairs/20180810_F11_F12_usv_001/F11_F12_ch1/voc",
					"E:/all_data_usv_pairs/20180813_F03_F04_usv_002/F03_F04_ch1/voc",
					"E:/all_data_usv_pairs/20180813_F07_F08_usv_002/F07_F08_ch1/voc",
					"E:/all_data_usv_pairs/20180817_F01_F02_usv/F01_F02_ch1/voc",
					"E:/all_data_usv_pairs/20180817_F05_F06_usv/F05_F06_ch1/voc",
					"E:/all_data_usv_pairs/20181204_usv_lmt_pair_F15_F16_5we_Experiment 2797/usv_F15_F16/voc",
					"E:/all_data_usv_pairs/20181204_usv_lmt_paired_F13_F14_5we_Experiment 8959/usv_F13_F14/ch1_F13_F14/voc",
					"E:/all_data_usv_pairs/20181207_usv_lmt_pair_F17_F18_5we_Experiment 8484/usv_011_F17-F18/ch1/voc",
					"E:/all_data_usv_pairs/20181211_usv_lmt_pair_M40_M41_5we/ch1/voc",
					"E:/all_data_usv_pairs/20181211_usv_lmt_pair_M42_M43_5we/usv_010/ch1/voc",
					"E:/all_data_usv_pairs/20181214_usv_lmt_pair_M44_M45_5we/ch1_M44_M45/voc",
					"E:/all_data_usv_pairs/20181214_usv_lmt_pair_M46_M47_5we/ch1_M46_M47/voc",
					"E:/all_data_usv_pairs/20181217_usv_lmt_pair_F19_F20_5we/ch1_F19_F20/voc",
					"E:/all_data_usv_pairs/20190118_usv_lmt_pair_M40-M41_2mo_1/ch1/voc",
					"E:/all_data_usv_pairs/20190122_usv_lmt_pair_M42_M43_2mo_Experiment 7259/ch1/voc",
					"E:/all_data_usv_pairs/20190125_usv_lmt_pair_F13-F14_2mo_1/usv_021/F13-F14_2mo_ch1/voc",
					"E:/all_data_usv_pairs/20190125-usv_lmt_pair_M44_M45_2mo_1/usv_lmt004/ch1/voc",
					"E:/all_data_usv_pairs/20190129_usv_lmt_pair_F15_F16_1/F15-F16_2mo_ch1/voc",
					"E:/all_data_usv_pairs/20190129_usv_lmt_pair_M46_M47_2mo_2_1/ch1/voc",
					"E:/all_data_usv_pairs/20190201_usv_lmt_pair_F17_F18_2mo/F17-F18_2mo_ch1/voc",
					"E:/all_data_usv_pairs/20190201_usv_lmt_pair_F19_F20_2mo/F19-F20_2mo_ch1/voc",
			};


		ProgressFrame pf = new ProgressFrame("All voc processing");
		Chronometer fullJob = new Chronometer("All voc processing");
		float index = 0;
		for ( String folder : folderList )
		{
			ProgressFrame pfFolder = new ProgressFrame("Folder processing...");
			Chronometer folderJob = new Chronometer("All voc processing folder " + folder);
			File folderFile = new File(folder);
			File[] listOfFiles = folderFile.listFiles();
			int currentFileIndex = 0;
			for ( File file : listOfFiles )
			{
				if( FileUtil.getFileExtension(file.getAbsolutePath(), false ).toUpperCase().equals("WAV") )
				{
					FullVocProcessor processor = new FullVocProcessor( false , null );
					processor.setSaveHTML(false);
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
			index++;
			pf.setPosition( 100f * index / folderList.length );
			folderJob.displayInSeconds();
			pfFolder.close();
		}
		pf.close();
		fullJob.displayInSeconds();

		System.out.println("All processing Finished");

	}

	private void initFile(String string) {

		File fileToInit = new File( string );
		try {
			fileToInit.createNewFile();
			PrintWriter writer = new PrintWriter(fileToInit);
			writer.print("");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
