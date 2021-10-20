package plugins.fab.aaa.voc;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class VocalisationLabelExporter {



	public static void export(String fileName, AudioFile2 audioFile, ArrayList<Voc> vocList, boolean appendToFile, File allResultsFile ) {

		synchronized ( vocList ) {

			AudioVocDetection.sortVocInTime( vocList );

			String result ="";

			String eol = "\r\n";
			String tab=";";

			if ( audioFile != null )
			{
				// head
				result+= "Exp:" + tab + audioFile.file.getAbsolutePath() + eol;
			}

			int i = 0;
			String columnHeader = "";
			String result2 = "";

			for ( Voc voc : vocList )
			{
				columnHeader = "";

				columnHeader+= "Voc number"+tab;
				result2+=""+i+tab;

				columnHeader+= "nbPointDefined"+tab;
				result2+= voc.pointList.size() + tab;
//				fw.write( "nbPointDefined"+tab + voc.pointList.size() + tab);

				columnHeader+= "startOffset(ms)"+tab;
				result2+= voc.getStartInMs() + tab;
//				fw.write( "startOffset(ms)"+tab + voc.getStartInMs() + tab);

				columnHeader+= "duration(ms):"+tab;
				result2+= voc.getDurationInMs() + tab;
//				fw.write( "duration(ms):"+tab + voc.getDurationInMs() + tab);

				columnHeader+= "freq dynamic(Hz)"+tab;
				result2+= voc.getFrequencyDynamicInHz() + tab;
//				fw.write( "freq dynamic(Hz):"+tab + voc.getFrequencyDynamicInHz() + tab);

				columnHeader+= "startFrequency(Hz)"+tab;
				result2+= voc.getStartFrequencyInHz() + tab;
//				fw.write( "startFrequency(Hz):"+tab + voc.getStartFrequencyInHz() + tab);

				columnHeader+= "endFrequency(Hz)"+tab;
				result2+= voc.getEndFrequencyInHz() + tab;
//				fw.write( "endFrequency(Hz):"+tab + voc.getEndFrequencyInHz() + tab);

				columnHeader+= "meanFrequency(Hz)"+tab;
				result2+= voc.getMeanFrequencyInHz() + tab;
//				fw.write( "meanFrequency(Hz):"+tab + voc.getMeanFrequencyInHz() + tab);

				columnHeader+= "FrequencyTV(Hz)"+tab;
				result2+= voc.getFrequencyTVInHz() + tab;
//				fw.write( "FrequencyTV(Hz):"+tab + voc.getFrequencyTVInHz() + tab);

				columnHeader+= "meanFrequencyTV(Hz)"+tab;
				result2+= voc.getMeanFrequencyTVInHz() + tab;
//				fw.write( "meanFrequencyTV(Hz):"+tab + voc.getMeanFrequencyTVInHz() + tab);

				columnHeader+= "linearity index"+tab;
				result2+= voc.linearityIndex + tab;
//				fw.write( "linearity index:"+tab + voc.linearityIndex + tab);

				columnHeader+= "mean power"+tab;
				result2+= voc.meanPower + tab;
//				fw.write( "mean power:"+tab + voc.meanPower + tab);

				columnHeader+= "nb modulation"+tab;
				result2+= voc.nbModulation + tab;
//				fw.write( "nb modulation:"+tab + voc.nbModulation + tab);

				columnHeader+= "nb pt Harmonics"+tab;
				result2+= voc.pointListHarmonics.size() + tab;
//				fw.write( "nb pt Harmonics:" +tab + voc.pointListHarmonics.size() + tab );

				columnHeader+= "nb jumps"+tab;
				result2+= voc.jumpList.size() + tab;
//				fw.write( "nb jumps:"+tab + voc.jumpList.size() + tab);

				columnHeader+= "Modulated"+tab;
				result2+= voc.classificationDescription.contains("Modulated") + tab;
//				fw.write( "Modulated:" + tab + voc.classificationDescription.contains("Modulated") + tab );

				columnHeader+= "Short"+tab;
				result2+= voc.classificationDescription.contains("Short") + tab;
//				fw.write( "Short:" + tab + voc.classificationDescription.contains("Short") + tab );

				columnHeader+= "Upward"+tab;
				result2+= voc.classificationDescription.contains("Upward") + tab;
//				fw.write( "Upward:" + tab + voc.classificationDescription.contains("Upward") + tab );

				columnHeader+= "Downward"+tab;
				result2+= voc.classificationDescription.contains("Downward") + tab;
//				fw.write( "Downward:" + tab + voc.classificationDescription.contains("Downward") + tab );

				columnHeader+= "Jump"+tab;
				result2+= voc.classificationDescription.contains("Jump") + tab;
//				fw.write( "Jump:" + tab + voc.classificationDescription.contains("Jump") + tab );

				columnHeader+= "Harmonics"+tab;
				result2+= voc.classificationDescription.contains("Harmonics") + tab;
//				fw.write( "Harmonics:" + tab + voc.classificationDescription.contains("Harmonics") + tab );

				columnHeader+= "minFrequency(Hz)"+tab;
				result2+= voc.getMinFrequencyInHz() + tab;
//				fw.write( "minFrequency(Hz):"+tab + voc.getMinFrequencyInHz() + tab);

				columnHeader+= "maxFrequency(Hz)"+tab;
				result2+= voc.getMaxFrequencyInHz() + tab;
//				fw.write( "maxFrequency(Hz):"+tab + voc.getMaxFrequencyInHz() + tab);

				columnHeader+= "peak power"+tab;
				result2+= voc.peakPower + tab;
//				fw.write( "peak power:"+tab + voc.peakPower + tab);

				columnHeader+= "peak Frequency(Hz)"+tab;
				result2+= voc.peakFrequency + tab;
//				fw.write( "peak Frequency(Hz):"+tab + voc.peakFrequency + tab);

				columnHeader+= "min power"+tab;
				result2+= voc.minPower + tab;
//				fw.write( "min power:"+tab + voc.minPower + tab);

				columnHeader+= "isInBadRepeat"+tab;
				result2+= voc.isInBadRepeat + tab;
//				fw.write( "isInBadRepeat:"+tab + voc.isInBadRepeat + tab);

				result2+=eol;
				i++;
//				fw.write( eol );
			}

			String allResults = result + eol + columnHeader + eol + result2+ eol;

			try
			{
				File file = new File( fileName );
				System.out.println("writing data in " + file.getAbsolutePath() );
				FileWriter fw = new FileWriter( file, appendToFile );
				fw.write( allResults );
				fw.close();
			}
			catch(IOException ioe)
			{
				System.err.println("IOException: " + ioe.getMessage());
			}

			if ( allResultsFile != null )
			{
				try
				{

					System.out.println("writing data in " + allResultsFile.getAbsolutePath() );
					FileWriter fw = new FileWriter( allResultsFile, true );
					fw.write( allResults );
					fw.close();
				}
				catch(IOException ioe)
				{
					System.err.println("IOException: " + ioe.getMessage());
				}
			}
			/*
			try
			{
				String eol = "\r\n";
				String tab="\t";
				File file = new File( fileName );
				System.out.println("writing data in " + file.getAbsolutePath() );
				FileWriter fw = new FileWriter( file, appendToFile );

				if ( audioFile != null )
				{
					fw.write( "Exp:" + tab + audioFile.file.getAbsolutePath() + tab );
					fw.write( "Duration(ms):" + tab + audioFile.getDurationInMilliSecond() + tab );
					fw.write( "NbRepeat:" + tab + audioFile.nbRepeat + tab );
				}

				fw.write( eol );
				int i = 0;
				for ( Voc voc : vocList )
				{
					fw.write( "Voc"+tab + i + tab);
					fw.write( "nbPointDefined"+tab + voc.pointList.size() + tab);
					fw.write( "startOffset(ms)"+tab + voc.getStartInMs() + tab);
					fw.write( "duration(ms):"+tab + voc.getDurationInMs() + tab);
					fw.write( "freq dynamic(Hz):"+tab + voc.getFrequencyDynamicInHz() + tab);
					fw.write( "startFrequency(Hz):"+tab + voc.getStartFrequencyInHz() + tab);
					fw.write( "endFrequency(Hz):"+tab + voc.getEndFrequencyInHz() + tab);
					fw.write( "meanFrequency(Hz):"+tab + voc.getMeanFrequencyInHz() + tab);
					fw.write( "FrequencyTV(Hz):"+tab + voc.getFrequencyTVInHz() + tab);
					fw.write( "meanFrequencyTV(Hz):"+tab + voc.getMeanFrequencyTVInHz() + tab);
					fw.write( "linearity index:"+tab + voc.linearityIndex + tab);
					fw.write( "mean power:"+tab + voc.meanPower + tab);
					fw.write( "nb modulation:"+tab + voc.nbModulation + tab);
					fw.write( "nb pt Harmonics:" +tab + voc.pointListHarmonics.size() + tab );
					fw.write( "nb jumps:"+tab + voc.jumpList.size() + tab);

					fw.write( "Modulated:" + tab + voc.classificationDescription.contains("Modulated") + tab );
					fw.write( "Short:" + tab + voc.classificationDescription.contains("Short") + tab );
					fw.write( "Upward:" + tab + voc.classificationDescription.contains("Upward") + tab );
					fw.write( "Downward:" + tab + voc.classificationDescription.contains("Downward") + tab );
					fw.write( "Jump:" + tab + voc.classificationDescription.contains("Jump") + tab );
					fw.write( "Harmonics:" + tab + voc.classificationDescription.contains("Harmonics") + tab );
					fw.write( "minFrequency(Hz):"+tab + voc.getMinFrequencyInHz() + tab);
					fw.write( "maxFrequency(Hz):"+tab + voc.getMaxFrequencyInHz() + tab);

					fw.write( "peak power:"+tab + voc.peakPower + tab);
					fw.write( "peak Frequency(Hz):"+tab + voc.peakFrequency + tab);
					fw.write( "min power:"+tab + voc.minPower + tab);
					fw.write( "isInBadRepeat:"+tab + voc.isInBadRepeat + tab);


					//				for ( String string : voc.classificationDescription )
					//				{
					//					fw.write( string+tab);
					//				}
					i++;
					fw.write( eol );
				}
				fw.close();
			}
			catch(IOException ioe)
			{
				System.err.println("IOException: " + ioe.getMessage());
			}
			 */
		}


	}

}
