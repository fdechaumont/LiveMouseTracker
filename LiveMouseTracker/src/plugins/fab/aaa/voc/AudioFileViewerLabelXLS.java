package plugins.fab.aaa.voc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import icy.file.FileUtil;
import icy.plugin.abstract_.PluginActionable;
import icy.plugin.interface_.PluginThreaded;
import icy.preferences.XMLPreferences;
import icy.util.XLSUtil;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;


/**
 * @author Fab
 *
 */
public class AudioFileViewerLabelXLS extends PluginActionable implements PluginThreaded {

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

		File xlsFile = new File( FileUtil.getDirectory( files[0].getAbsolutePath() ) + "/burst-analysis.xls");


		try {
			WritableWorkbook workbook = XLSUtil.createWorkbook( xlsFile );
			WritableSheet sheet = XLSUtil.createNewPage(workbook, "Result" );

			XLSUtil.setCellString(sheet, 0, 0, "File name");
			XLSUtil.setCellString(sheet, 1, 0, "Wav number");
			XLSUtil.setCellString(sheet, 2, 0, "Mean voc duration (ms)");
			XLSUtil.setCellString(sheet, 3, 0, "Std voc duration (ms)");
			XLSUtil.setCellString(sheet, 4, 0, "Nb voc");
			XLSUtil.setCellString(sheet, 5, 0, "Burst total duration");
			XLSUtil.setCellString(sheet, 6, 0, "mean power");
			XLSUtil.setCellString(sheet, 7, 0, "std power");
			XLSUtil.setCellString(sheet, 8, 0, "mean frequency");
			XLSUtil.setCellString(sheet, 9, 0, "std frequency");
			XLSUtil.setCellString(sheet, 10, 0, "voc density");
			XLSUtil.setCellString(sheet, 11, 0, "mean space between voc(ms)");
			XLSUtil.setCellString(sheet, 12, 0, "std space between voc(ms)");
			XLSUtil.setCellString(sheet, 13, 0, "mean tone slope");
			XLSUtil.setCellString(sheet, 14, 0, "std tone slope");
			XLSUtil.setCellString(sheet, 15, 0, "mean consecutive tone (se)");
			XLSUtil.setCellString(sheet, 16, 0, "std consecutive tone (se)");
			XLSUtil.setCellString(sheet, 17, 0, "mean nb jump");
			XLSUtil.setCellString(sheet, 18, 0, "std nb jump");
			XLSUtil.setCellString(sheet, 19, 0, "total TV");
			XLSUtil.setCellString(sheet, 20, 0, "total Jump");
			XLSUtil.setCellString(sheet, 21, 0, "total Modulation");
			XLSUtil.setCellString(sheet, 22, 0, "total Power");
			XLSUtil.setCellString(sheet, 23, 0, "total Voc Duration");

			// nb jump


			int row = 1;

			for ( File file : files )
			{
				System.out.println( "# " + row + " / " + files.length + "  \t" + file );

				FullVocProcessor processor = new FullVocProcessor( false , null );
				processor.clearWavDataAfterLoad = false;
				try {
					processor.process( file );
				} catch (USVProcessingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				XLSUtil.setCellString(sheet, 0, row, ""+file );

				String extractedNumber = FileUtil.getFileName( file.getAbsolutePath(), false ).substring( 21, 21+ 7);
				System.out.println( "Extracted Number str = " + extractedNumber );

				XLSUtil.setCellNumber(sheet, 1, row, protect( Integer.parseInt( extractedNumber ) ) );

				XLSUtil.setCellNumber(sheet, 2, row, protect( processor.audioFile.vocMeanLenght ) );

				XLSUtil.setCellNumber(sheet, 3, row, protect( processor.audioFile.vocSTDLenght ) );

				XLSUtil.setCellNumber(sheet, 4, row, protect( processor.getAudioVocDetection().vocList.size() ) );

				XLSUtil.setCellNumber(sheet, 5, row, protect( processor.audioFile.getDurationInMilliSecond() ) );

				XLSUtil.setCellNumber(sheet, 6, row, protect( processor.audioFile.meanPower ) );

				XLSUtil.setCellNumber(sheet, 7, row, protect( processor.audioFile.STDPower ) );

				XLSUtil.setCellNumber(sheet, 8, row, protect( processor.audioFile.vocMeanFrequency ) );

				XLSUtil.setCellNumber(sheet, 9, row, protect( processor.audioFile.vocSTDFrequency ) );

				XLSUtil.setCellNumber(sheet, 10, row, protect( processor.audioFile.vocDensity ) );

				XLSUtil.setCellNumber(sheet, 11, row, protect( processor.audioFile.meanSpaceBetweenVoc ) );

				XLSUtil.setCellNumber(sheet, 12, row, protect( processor.audioFile.STDSpaceBetweenVoc ) );

				XLSUtil.setCellNumber(sheet, 13, row, protect( processor.audioFile.meanToneSlope ) );

				XLSUtil.setCellNumber(sheet, 14, row, protect( processor.audioFile.STDToneSlope ) );

				XLSUtil.setCellNumber(sheet, 15, row, protect( processor.audioFile.meanconsecutiveToneShiftStartEnd ) );

				XLSUtil.setCellNumber(sheet, 16, row, protect( processor.audioFile.STDconsecutiveToneShiftStartEnd ) );

				XLSUtil.setCellNumber(sheet, 17, row, protect( processor.audioFile.meanNbJump ) );

				XLSUtil.setCellNumber(sheet, 18, row, protect( processor.audioFile.STDNbJump ) );

				XLSUtil.setCellNumber(sheet, 19, row, protect( processor.audioFile.totalTV ) );

				XLSUtil.setCellNumber(sheet, 20, row, protect( processor.audioFile.totalJump ) );

				XLSUtil.setCellNumber(sheet, 21, row, protect( processor.audioFile.totalModulation ) );

				XLSUtil.setCellNumber(sheet, 22, row, protect( processor.audioFile.totalPower ) );

				XLSUtil.setCellNumber(sheet, 23, row, protect( processor.audioFile.totalVocPartDuration ) );

				row++;
			}

			XLSUtil.saveAndClose(workbook);

		} catch (IOException | WriteException e) {
			e.printStackTrace();
		}



	}

	private double protect(double value) {

		if ( Double.isNaN( value ) ) return -1;

		return value;
	}

}
