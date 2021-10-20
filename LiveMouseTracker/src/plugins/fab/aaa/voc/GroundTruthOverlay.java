package plugins.fab.aaa.voc;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

import icy.canvas.IcyCanvas;
import icy.file.FileUtil;
import icy.painter.Overlay;
import icy.sequence.Sequence;

public class GroundTruthOverlay extends Overlay {

	ArrayList<GroundTruthVoc> gtVocArrayList = new ArrayList<>();
	float xTimeInMs;
	ArrayList<Voc> vocDetectedArrayList = null;

	ArrayList<Voc> vocDetectedGoodArrayList = null;
	ArrayList<Voc> vocDetectedBadArrayList = null;
	ArrayList<GroundTruthVoc> gtMissedArrayList = null;

	float ratioFalsePositive;
	float ratioMissedDetection;
	float nbMissedVoc;
	float ratioOverlapDetectionAndGT;

	public GroundTruthOverlay( AudioFile2 audioFile, AudioFFTProcessing fftProcessing, ArrayList<Voc> vocArrayList ) {

		super("Ground truth");
		this.xTimeInMs = fftProcessing.xTimeInMs;

		File groundTruthFile = getFileMatching( audioFile );
		System.out.println("Ground truth file found: " + groundTruthFile );

		this.vocDetectedArrayList = vocArrayList;

		if ( groundTruthFile != null )
		{
			loadGroundTruth( groundTruthFile );
		} else
		{
			return;
		}



		computeScore();

		// overlap score.
		{
			boolean detectedArray[]= new boolean[ fftProcessing.magnitude[0].length];
			for ( Voc voc : vocDetectedArrayList )
			{
				for ( int x = voc.getStartX() ; x <= voc.getEndX() ; x++ )
				{
					detectedArray[x] = true;
				}
			}

			boolean gtArray[]= new boolean[ fftProcessing.magnitude[0].length];
			for ( GroundTruthVoc gtVoc : gtVocArrayList )
			{
				for ( int x = (int)(gtVoc.start/xTimeInMs) ; x <= (int)(gtVoc.end/xTimeInMs) ; x++ )
				{
					gtArray[x] = true;
				}
			}

			int nbOverlapDetectionAndGT = 0;
			int nbMissDetection = 0;
			int nbFalsePositive = 0;
			int nbTotalPointWithData = 0;
			int nbTotalPointWithGT = 0;
			int nbTotalPointWithAutoDetection = 0;
			for ( int x = 0 ; x < detectedArray.length ; x++ )
			{
				if ( detectedArray[x] )
				{
					nbTotalPointWithAutoDetection++;
				}

				if ( gtArray[x] )
				{
					nbTotalPointWithGT++;
				}

				if ( detectedArray[x] || gtArray[x] )
				{
					nbTotalPointWithData++;
				}

				if ( detectedArray[x] && gtArray[x] )
				{
					nbOverlapDetectionAndGT++;
				}
				if ( detectedArray[x] == false && gtArray[x] == true )
				{
					nbMissDetection ++;
				}
				if ( detectedArray[x] == true && gtArray[x] == false )
				{
					nbFalsePositive ++;
				}
			}

			this.ratioFalsePositive = (float)nbFalsePositive / (float)nbTotalPointWithAutoDetection;
			this.ratioMissedDetection = (float)nbMissDetection / (float)nbTotalPointWithGT;

			this.ratioOverlapDetectionAndGT = (float)nbOverlapDetectionAndGT / (float)nbTotalPointWithGT;


			try {
				/*
				 g.drawString( "Matching Score: ", 40, 20 );
		g.drawString( "VocDetected vs Ground Truth Score: " + matchScore, 40, 40 );
		g.drawString( "#GOOD VocDetected vs Ground Truth: " + goodMatchDetectedVsGT, 40, 60 );
		g.drawString( "#BAD VocDetected vs Ground Truth : " + badMatchDetectedVsGT, 40, 80 );
		g.drawString( "Nb VocDetected : " + vocDetectedArrayList.size(), 40, 100 );
		g.drawString( "Nb GroundTruth : " + gtVocArrayList.size(), 40, 120 );

		g.drawString( "Overlap Score: ", 800, 20 );
		g.drawString( "Overlap Ratio Detection and GT: " + ratioOverlapDetectionAndGT, 800, 40 );
		g.drawString( "Missed Detection: " + ratioMissedDetection, 800, 60 );
		g.drawString( "False Positive: " + ratioFalsePositive, 800, 80 );
				 */

				String result = audioFile.file.getAbsoluteFile().toString()

						+"\t#GOOD VocDetected\t" + goodMatchDetectedVsGT
						+"\t#BAD VocDetected\t" + badMatchDetectedVsGT
						+"\tMissed:\t" + (int)nbMissedVoc
						+"\t#NbVoc GT\t" + gtVocArrayList.size()
						+"\n";
				String validationFile = "c:/voc/validation.txt";
				if ( FileUtil.exists( validationFile))
				{
					Files.write(Paths.get(validationFile), result.getBytes(), StandardOpenOption.APPEND);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
/*
		try
		{
			if ( groundTruthFile != null )
			{
				String experimentName = "exp:"+groundTruthFile.getAbsoluteFile().toString()+"\n";
				Files.write(Paths.get("c:/voc/validationDetailed.txt"), experimentName.getBytes(), StandardOpenOption.APPEND);

				for ( Voc voc : vocDetectedGoodArrayList )
				{
					printDetailedValidation( voc );
				}
				for ( Voc voc : vocDetectedBadArrayList )
				{
					printDetailedValidation( voc );
				}
				for ( GroundTruthVoc voc : gtMissedArrayList )
				{
					printDetailedValidation( voc );
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
*/




	}

	private void printDetailedValidation(GroundTruthVoc voc) {
		String resultDetailed = "missed\t";

		resultDetailed += "duration:\t"+(int)voc.getDurationInMs()+"\t";
		resultDetailed += "startX:\t"+(int)voc.startX+"\t";

		resultDetailed +="\n";
		try {
			Files.write(Paths.get("e:/validationDetailed.txt"), resultDetailed.getBytes(), StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void printDetailedValidation(Voc voc) {

		String resultDetailed = "";
		if ( voc.matchWithGroundTruth )
		{
			resultDetailed += "good\t";
		}else
		{
			resultDetailed += "bad\t";
		}

		resultDetailed += "duration:\t"+(int)voc.getDurationInMs()+"\t";
		resultDetailed += "startX:\t"+(int)voc.getStartX()+"\t";

		resultDetailed +="\n";
		try {
			Files.write(Paths.get("e:/validationDetailed.txt"), resultDetailed.getBytes(), StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public ArrayList<GroundTruthVoc> getGroundTruthVocArrayList() {
		return gtVocArrayList;
	}

	private void loadGroundTruth(File groundTruthFile) {

		try {

			File file = groundTruthFile;
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);

			String line;
			while ((line = bufferedReader.readLine()) != null)
			{
				String cols[] = line.split("\t");
				float start = 1000f * Float.parseFloat( cols[4].replace(",", ".") );
				float end = 1000f * Float.parseFloat( cols[5].replace(",", ".") );
				System.out.println( start +"\t"+ end );
				gtVocArrayList.add( new GroundTruthVoc( (int)(start/xTimeInMs), start, end));
			}
			fileReader.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		// Fuse groundtruth with too small intervals.
		fuseGroundTruth( gtVocArrayList );


		// colorcode groundTruth.
		for ( GroundTruthVoc gtVoc : gtVocArrayList )
		{
			if ( isGroundTruthTooClose( gtVoc  ) )
			{
				gtVoc.color = Color.red;
			}
		}

		// save duration of voc in ms.
		{
			try {
				String fileIntervalDuration = "e:/gtintervaldurationvoc.txt";
				String experimentName = "exp:"+groundTruthFile.getAbsoluteFile().toString()+"\n";
				Files.write(Paths.get( fileIntervalDuration ), experimentName.getBytes(), StandardOpenOption.APPEND );
				for ( GroundTruthVoc gtVoc : gtVocArrayList )
				{
					Integer intervalDuration = intervalToNextGTVoc(gtVoc);
					if ( intervalDuration != null )
					{
						Files.write(Paths.get( fileIntervalDuration ), (""+intervalDuration+"\n").getBytes() , StandardOpenOption.APPEND);
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		ArrayList<GroundTruthVoc> matchedGroundTruth= new ArrayList<>();
		vocDetectedGoodArrayList = new ArrayList<Voc>();
		vocDetectedBadArrayList = new ArrayList<>();
		for ( Voc voc : vocDetectedArrayList )
		{
			GroundTruthVoc matchingVoc = match ( voc );
			if ( matchingVoc != null )
			{
				voc.matchWithGroundTruth = true;
				vocDetectedGoodArrayList.add( voc );
				matchedGroundTruth.add( matchingVoc );
			}else
			{
				voc.matchWithGroundTruth = false;
				vocDetectedBadArrayList.add( voc );
			}
		}

		gtMissedArrayList = new ArrayList<GroundTruthVoc>();
		for ( GroundTruthVoc gtVoc : gtVocArrayList )
		{
			if ( ! matchedGroundTruth.contains( gtVoc ) )
			{
				gtMissedArrayList.add( gtVoc );
			}
		}


	}

	private void fuseGroundTruth(ArrayList<GroundTruthVoc> gtVocArrayList2) {

		boolean hasFused = true;
		while ( hasFused )
		{
			hasFused = false;
			for ( GroundTruthVoc gtVoc : new ArrayList<GroundTruthVoc>(gtVocArrayList2) )
			{
				GroundTruthVoc nextVoc = getNextGTVoc( gtVoc );

				if ( nextVoc == null ) continue;
				if ( nextVoc.start - gtVoc.end < Constant.MAX_GAP_DURATION_BETWEEN_SEQUENCE_TO_FUSE_VOC_IN_MS )
				{
					// fuse
					gtVocArrayList2.remove( gtVoc );
					gtVocArrayList2.remove( nextVoc );
					GroundTruthVoc fusedVoc = new GroundTruthVoc( (int)(gtVoc.start/xTimeInMs) , gtVoc.start, nextVoc.end );
					gtVocArrayList2.add( fusedVoc );
					hasFused = true;
					break;
				}

			}
		}


	}

	private GroundTruthVoc getNextGTVoc(GroundTruthVoc gtVoc) {

		int closest = Integer.MAX_VALUE;
		GroundTruthVoc nextVoc = null;
		for ( GroundTruthVoc gtVocCandidate : gtVocArrayList )
		{
			if (gtVoc != gtVocCandidate )
			{
				if ( gtVocCandidate.start > gtVoc.end ) // is in future
				{
					int interval = (int)Math.abs( gtVocCandidate.start - gtVoc.end );
					if ( interval <= closest )
					{
						closest = interval;
						nextVoc = gtVocCandidate;
					}
				}
			}
		}
		return nextVoc;
	}

	Integer intervalToNextGTVoc( GroundTruthVoc gtVoc )
	{
		int closest = Integer.MAX_VALUE;
		for ( GroundTruthVoc gtVocCandidate : gtVocArrayList )
		{
			if (gtVoc != gtVocCandidate )
			{
				if ( gtVocCandidate.start > gtVoc.end ) // is in future
				{
					int interval = (int)Math.abs( gtVocCandidate.start - gtVoc.end );
					if ( interval <= closest )
					{
						closest = interval;
					}
				}
			}
		}
		if ( closest == Integer.MAX_VALUE )
		{
			return null;
		}
		return closest;
	}

	boolean isGroundTruthTooClose( GroundTruthVoc gtVoc )
	{
		for ( GroundTruthVoc gtVocCandidate : gtVocArrayList )
		{
			if (gtVoc != gtVocCandidate )
			{
				if ( Math.abs( gtVocCandidate.start - gtVoc.end ) <= Constant.MAX_GAP_DURATION_BETWEEN_SEQUENCE_TO_FUSE_VOC_IN_MS )
				{
					return true;
				}
				if ( Math.abs( gtVocCandidate.end - gtVoc.start ) <= Constant.MAX_GAP_DURATION_BETWEEN_SEQUENCE_TO_FUSE_VOC_IN_MS )
				{
					return true;
				}
			}
		}
		return false;
	}

	private File getFileMatching(AudioFile2 audioFile) {

		// seek for the corresponding .txt file with last 5 digits.
		String fileName = FileUtil.getFileName( audioFile.file.getAbsolutePath(), false );
		String fileNameEnd = endOfString( fileName , 5 );
		System.out.println("File name end: " + fileNameEnd );


		File folder = new File( FileUtil.getDirectory( audioFile.file.getAbsolutePath() , true ) );
		File[] listOfFiles = folder.listFiles();

		for (int i = 0; i < listOfFiles.length; i++)
		{
			File gtFile = listOfFiles[i];
			if ( gtFile.isFile())
			{
//				System.out.println("File " + gtFile.getName());
				String gtFileName = FileUtil.getFileName( gtFile.getAbsoluteFile().getAbsolutePath() , true );
				if ( gtFileName.contains( fileNameEnd + ".txt" ) )
				{
					return gtFile;
				}
			}

		}
		return null;

	}

	private String endOfString(String fileName, int nbChar ) {

		if ( fileName.length() > nbChar )
		{
			return fileName.substring( fileName.length() - nbChar );
		}
		return null;
	}

	Font bigFont = new Font( "Arial", Font.BOLD , 20 );

	@Override
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas) {


		BasicStroke basicStroke = new BasicStroke( 3 );
		final float dash1[] = {10.0f};
		final BasicStroke dashed = new BasicStroke(3.0f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,10.0f, dash1, 0.0f);

		for ( GroundTruthVoc gtVoc : gtVocArrayList )
		{
			if ( gtVoc.notFound )
			{
				g.setStroke( dashed );
			}else
			{
				g.setStroke( basicStroke );
			}
			float startX = (gtVoc.start)/xTimeInMs;
			float endX = (gtVoc.end)/xTimeInMs;
			g.setColor( gtVoc.color );
			g.drawRect( (int)startX, 125, (int)(endX-startX), 300 );
		}


		for ( Voc voc : new ArrayList<Voc>(vocDetectedArrayList) )
		{
			if ( voc.matchWithGroundTruth )
//			if ( match( voc ) != null )
			{
				g.setColor( Color.GREEN );
			}else
			{
				g.setColor( Color.RED );
			}
			g.fillOval( voc.getCenterX()-10, 90, 20, 20 );
		}

		int nbGroundTruth = gtVocArrayList.size();
		g.setColor( Color.black );
		g.setFont( bigFont );
		g.drawString( "Matching Score: ", 40, 20 );
		g.drawString( "#MISSED vs groundTruth: " + (int)nbMissedVoc + " / " + nbGroundTruth + " / " + (int)((nbMissedVoc/(float)nbGroundTruth)*100) + "%", 40, 40 );
		g.drawString( "#GOOD VocDetected vs Ground Truth: " + goodMatchDetectedVsGT + " / " + nbGroundTruth + " / " + (int)((goodMatchDetectedVsGT/(float)nbGroundTruth)*100) + "%" , 40, 60 );
		g.drawString( "#BAD VocDetected vs Ground Truth : " + badMatchDetectedVsGT, 40, 80 );
		g.drawString( "Nb VocDetected : " + vocDetectedArrayList.size(), 40, 100 );
		g.drawString( "Nb GroundTruth : " + nbGroundTruth, 40, 120 );
/*
		g.drawString( "Overlap Score: ", 800, 20 );
		g.drawString( "Overlap Ratio Detection and GT: " + ratioOverlapDetectionAndGT, 800, 40 );
		g.drawString( "Missed Detection: " + ratioMissedDetection, 800, 60 );
		g.drawString( "False Positive: " + ratioFalsePositive, 800, 80 );
*/



	}

	private void computeScore() {

		goodMatchDetectedVsGT = 0;
		badMatchDetectedVsGT = 0;
		ArrayList<GroundTruthVoc> groundTruthFoundInDetectionList = new ArrayList<GroundTruthVoc>();
		for ( Voc voc : vocDetectedArrayList )
		{
			GroundTruthVoc matchingVoc = match( voc );
			if ( matchingVoc!=null )
			{
				goodMatchDetectedVsGT++;
				groundTruthFoundInDetectionList.add( matchingVoc );
			}else
			{
				badMatchDetectedVsGT++;
			}
		}
//		matchScore = goodMatchDetectedVsGT / (float) vocDetectedArrayList.size() ;
		nbMissedVoc = gtVocArrayList.size() - groundTruthFoundInDetectionList.size();
		for ( GroundTruthVoc gtVoc : gtVocArrayList )
		{
			if ( !groundTruthFoundInDetectionList.contains( gtVoc ) )
			{
				gtVoc.notFound = true;
			}
		}
		// ----


	}

	int goodMatchDetectedVsGT;
	int badMatchDetectedVsGT;
	float matchScore;

	private GroundTruthVoc match(Voc voc) {

//		float NB_MILLISECOND_OF_DIFFERENCE_ALLOWED = 40;

		for ( GroundTruthVoc gtVoc : gtVocArrayList )
		{
			if ( Math.abs( gtVoc.start - voc.getStartInMs() ) < Constant.MAX_GAP_DURATION_BETWEEN_SEQUENCE_TO_FUSE_VOC_IN_MS
					&& Math.abs( gtVoc.end - voc.getEndInMs() ) < Constant.MAX_GAP_DURATION_BETWEEN_SEQUENCE_TO_FUSE_VOC_IN_MS
					) return gtVoc;
		}
		return null;

	}

}
