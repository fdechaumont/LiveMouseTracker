package plugins.fab.aaa.voc;

import java.util.ArrayList;

public class FileVocalizationClassifier {

	/*
		1-4 voc court
		5-15 moyen
		sup 15 long


		complexité:
		que short up flat down = 1
		voc jump + modulated = 2
		complex harmonique = 3

	 */

	public static void classify(AudioFile2 audioFile , ArrayList<Voc> vocList ) {

		setLengthScore( audioFile , vocList );
		setComplexityScore( audioFile , vocList );
		audioFile.nbVoc = vocList.size();

		float meanPower = 0;
		ArrayList<Double> lenList = new ArrayList<>();
		ArrayList<Double> freqList = new ArrayList<>();

		int currentT = 0;
		for ( Voc voc : vocList )
		{
			int newT = voc.getStartX();
			if ( newT < currentT )
			{
				System.out.println("WARNING: ERROR IN VOC TIME SORT !");
			}
			currentT = newT;
		}


		{
			double totalTV = 0;
			for ( Voc voc : vocList )
			{
				totalTV+=voc.getFrequencyTVInHz();
			}
			audioFile.totalTV = totalTV;
		}

		{
			double totalJump = 0;
			for ( Voc voc : vocList )
			{
				totalJump+=voc.jumpList.size();
			}
			audioFile.totalJump = totalJump;
		}

		{
			double totalModulation = 0;
			for ( Voc voc : vocList )
			{

				totalModulation+=voc.nbModulation;
			}
			audioFile.totalModulation = totalModulation;
		}

		{
			double totalPower = 0;
			for ( Voc voc : vocList )
			{
				totalPower+=voc.meanPower;
			}
			audioFile.totalPower = totalPower;
		}

		{
			double totalVocPartDuration = 0;
			for ( Voc voc : vocList )
			{
				totalVocPartDuration+=voc.getDurationInMs();
			}
			audioFile.totalVocPartDuration = totalVocPartDuration;
		}

		{
			ArrayList<Double> powerVocList = new ArrayList<>();
			for ( Voc voc : vocList )
			{
				powerVocList.add( (double) voc.meanPower );
			}
			double powerVocArray[] = new double[ powerVocList.size() ];
			for ( int i = 0 ; i < powerVocList.size() ; i ++ )
			{
				powerVocArray[i] = powerVocList.get( i );
			}
			audioFile.meanPower = (float) MathUtil.mean( powerVocArray );
			audioFile.STDPower = (float) MathUtil.stddev( powerVocArray );
		}

		{
			ArrayList<Double> jumpVocList = new ArrayList<>();
			for ( Voc voc : vocList )
			{
				jumpVocList.add( (double) voc.jumpList.size() );
			}
			double jumpVocArray[] = new double[ jumpVocList.size() ];
			for ( int i = 0 ; i < jumpVocList.size() ; i ++ )
			{
				jumpVocArray[i] = jumpVocList.get( i );
			}
			audioFile.meanNbJump = (float) MathUtil.mean( jumpVocArray );
			audioFile.STDNbJump = (float) MathUtil.stddev( jumpVocArray );
		}


		/*
		for ( Voc voc : vocList )
		{
			meanPower += voc.power;
			lenList.add( new Double ( voc.getDurationInMs() ) );
			freqList.add( new Double( voc.getFrequencyDynamicInHz() ) );
		}

		if ( vocList.size() > 0 )
		{
			meanPower /= vocList.size();
		}
		audioFile.meanPower = meanPower;
		*/

		for ( Voc voc : vocList )
		{
			lenList.add( new Double ( voc.getDurationInMs() ) );
			freqList.add( new Double( voc.getFrequencyDynamicInHz() ) );
		}
		double lenArray[] = new double[lenList.size()];
		for ( int i = 0 ; i < lenList.size() ; i ++ )
		{
			lenArray[i] = lenList.get( i );
		}
		double freqArray[] = new double[freqList.size()];
		for ( int i = 0 ; i < freqList.size() ; i ++ )
		{
			freqArray[i] = freqList.get( i );
		}
		audioFile.vocMeanLenght = (float) MathUtil.mean( lenArray );
		audioFile.vocSTDLenght = (float) MathUtil.stddev( lenArray );

		audioFile.vocMeanFrequency = (float) MathUtil.mean( freqArray );
		audioFile.vocSTDFrequency = (float) MathUtil.stddev( freqArray );

		audioFile.vocDensity = (float) ( vocList.size() / audioFile.getDurationInMilliSecond() );

		{
			ArrayList<Double> spaceBetweenVocList = new ArrayList<>();
			for ( int i = 0 ; i < vocList.size()-1 ; i++ )
			{
				Voc vocA = vocList.get( i );
				Voc vocB = vocList.get( i+1 );

				spaceBetweenVocList.add( (double) (vocB.getStartInMs() - vocA.getEndInMs()) );
			}
			double spaceBetweenVocArray[] = new double[ spaceBetweenVocList.size() ];
			for ( int i = 0 ; i < spaceBetweenVocList.size() ; i ++ )
			{
				spaceBetweenVocArray[i] = spaceBetweenVocList.get( i );
			}
			audioFile.meanSpaceBetweenVoc = (float) MathUtil.mean( spaceBetweenVocArray );
			audioFile.STDSpaceBetweenVoc = (float) MathUtil.stddev( spaceBetweenVocArray );
		}


		{
			// line fit ( decreasing or increasing tone along voc )

			ArrayList<Double> lineFitList = new ArrayList<>();
			for ( Voc voc : vocList )
			{
				lineFitList.add( (double) ( voc.pB.getY()-voc.pA.getY() ) );
			}
			double lineFitArray[] = new double[ lineFitList.size() ];
			for ( int i = 0 ; i < lineFitList.size() ; i ++ )
			{
				lineFitArray[i] = lineFitList.get( i );
			}
			audioFile.meanToneSlope = (float) MathUtil.mean( lineFitArray );
			audioFile.STDToneSlope = (float) MathUtil.stddev( lineFitArray );
		}

		{
			// consecutiveToneShift Start End ( shift between consecutive voc )

			ArrayList<Double> consecutiveToneList = new ArrayList<>();
			for ( int i = 0 ; i < vocList.size()-1 ; i++ )
			{
				Voc vocA = vocList.get( i );
				Voc vocB = vocList.get( i+1 );
				consecutiveToneList.add( (double) ( vocB.getStartFrequencyInHz() - vocA.getEndFrequencyInHz() ) );
			}
			double consecutiveToneArray[] = new double[ consecutiveToneList.size() ];
			for ( int i = 0 ; i < consecutiveToneList.size() ; i ++ )
			{
				consecutiveToneArray[i] = consecutiveToneList.get( i );
			}
			audioFile.meanconsecutiveToneShiftStartEnd = (float) MathUtil.mean( consecutiveToneArray );
			audioFile.STDconsecutiveToneShiftStartEnd = (float) MathUtil.stddev( consecutiveToneArray );
		}


//		System.out.println("---");
//		System.out.println( audioFile.meanPower );
//		System.out.println( audioFile.vocMeanLenght );
//		System.out.println( audioFile.vocSTDLenght );
//		System.out.println( audioFile.vocMeanFrequency );
//		System.out.println( audioFile.vocSTDFrequency );
//		System.out.println( audioFile.vocDensity );

	}

	private static void setComplexityScore(AudioFile2 audioFile, ArrayList<Voc> vocList) {

		audioFile.complexityScore = 1;

		{
			int nbVocWithJump = 0;
			for ( Voc voc : vocList )
			{
				if ( voc.jumpList.size() > 1 )
				{
					nbVocWithJump++;
					continue;
					//				audioFile.complexityScore = 2;
					//				break;
				}
			}

			if ( nbVocWithJump > vocList.size() / 3f )
			{
				audioFile.complexityScore = 2;
			}

			int nbVocModulated = 0;
			for ( Voc voc : vocList )
			{
				if ( voc.modulationList.size() > 1 )
				{
					nbVocModulated++;
					continue;
				}
			}

			if ( nbVocModulated > vocList.size() / 3f )
			{
				audioFile.complexityScore = 2;
			}
		}

		{
			int nbVocWithHarmonics = 0;
			for ( Voc voc : vocList )
			{
				if ( voc.containsHarmonics() )
				{
					nbVocWithHarmonics++;
//					audioFile.complexityScore = 3;
//					break;
				}
			}
			if ( nbVocWithHarmonics > vocList.size() / 3f )
			{
				audioFile.complexityScore = 3;
			}

		}

	}

	private static void setLengthScore(AudioFile2 audioFile, ArrayList<Voc> vocList) {

		audioFile.lengthScore = 1;
		if ( vocList.size() > 4 )
		{
			audioFile.lengthScore = 2;
		}
		if ( vocList.size() > 15 )
		{
			audioFile.lengthScore = 3;
		}
	}

}
