package plugins.fab.aaa.voc;

import java.io.File;
import java.util.ArrayList;

import icy.main.Icy;
import icy.sequence.Sequence;

public class TriangulationThread extends Thread {

	// chercher sur 14ms pour les tests.

	@Override
	public void run() {

		File file = new File( "d:/avisoft records/ring.wav" );
		AudioRingBuffer audioRingBuffer = new AudioRingBuffer( file );

		Sequence sequence = new Sequence("Triangulation view");
		Icy.getMainInterface().addSequence( sequence );

		while ( !isInterrupted() )
		{
			if ( Icy.isExiting() ) break;

			audioRingBuffer.refreshRing();

			processDelta2 ( audioRingBuffer.getSampleData() );

//			AudioFFTProcessing fft = new AudioFFTProcessing( audioRingBuffer.getSampleData() , audioRingBuffer.getSampleRate(), 0.75f , 1024 );
//			AudioSpectrumViewer.updateSequence( sequence , fft.getMagnitudeForAllChannels() );

			try {
				Thread.sleep( 1000 );
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}

	}


	private void processDelta2(double[][] sampleData ) {

		System.out.println("=============================");
		// search the potential voc to process on channel 0, base on waveform amplitude

		ArrayList<SoundPatch> soundPatchList = new ArrayList<SoundPatch>();

		double soundThreshold = 0.02;
		double searchDistanceInMeter = 0.5;
		int searchWindow = (int) (166666d * ( searchDistanceInMeter / 340));
		int jump = (int) (20 * (166666 / 1000));
		double dataChannel0[] = sampleData[0];
		{

			for ( int t=0+searchWindow ; t < dataChannel0.length-searchWindow-1 ; t++ )
			{
				double value = Math.abs( dataChannel0[t] );

				if ( value > soundThreshold )
				{
					SoundPatch soundPatch = new SoundPatch();
					soundPatch.start = t;
					soundPatchList.add( soundPatch );
					t+=jump;
				}

			}
		}

		for ( SoundPatch soundPatch : soundPatchList )
		{

			for ( int channelCandidate = 1 ; channelCandidate < sampleData.length ; channelCandidate++ )
			{
				int hitIndex = 0;
				boolean found=false;
				for ( int swt = -searchWindow ; swt < searchWindow ; swt++ )
				{
					int index = swt+soundPatch.start;
					if (
					( sampleData[channelCandidate][index] < soundThreshold )
					&&
					( sampleData[channelCandidate][index+1] > soundThreshold )
					)
					{
						hitIndex = index;
						found = true;
						break;
					}
				}

				if (  !found ) continue;

				double offset = hitIndex - soundPatch.start;
				{
					double offsetInSecond = ( 1d / 166666d ) * offset;
					double offsetInMeter = offsetInSecond * 340;
					double offsetInCentimeter = (int) ( offsetInMeter * 100 );

					System.out.println( "OFFSET In centimeter:\t" + offsetInCentimeter );
				}
			}


		}

		/*
		// process delta towards channel 0.
		for ( SoundPatch soundPatch : soundPatchList )
		{
			System.out.println("********* Patch len: " + (int)(soundPatch.getLength()* ( 1000d / 166666d )) + " ms\tStart at:"  + soundPatch.start );
			int searchWindow = 1500;
			for ( int channelCandidate = 1 ; channelCandidate < sampleData.length ; channelCandidate++ )
			{
				double maxCorrelation = Double.MIN_VALUE;
				int bestOffset = 0;
				for ( int swt = -searchWindow ; swt < searchWindow ; swt++ )
				{
					double correlation = MathUtil.correlation(
							sampleData[0], soundPatch.start, sampleData[channelCandidate], soundPatch.start+swt, soundPatch.getLength() );
					if ( correlation > maxCorrelation )
					{
						maxCorrelation = correlation;
						bestOffset = swt;
					}
				}

//				if ( bestOffset > 10 ) // just to avoid the loop click
				{
//				System.out.println(
//						"Best offset ch0-ch: "+channelCandidate +" : \t" + bestOffset * (1000d / 166666) +
//						"corr:" + (int)( 100 * maxCorrelation ) );
				double offsetInSecond = ( 1d / 166666d ) * bestOffset;
				double offsetInMeter = offsetInSecond * 340;
				double offsetInCentimeter = (int) ( offsetInMeter * 100 );
//				System.out.println("Offset in second  " + offsetInSecond );
//				System.out.println("Offset in meter  " + offsetInMeter );

				System.out.println( "OFFSET In centimeter:\t" + offsetInCentimeter + "\tcorr:" + (int)( 100 * maxCorrelation ) );

				}
			}
		}*/



	}


	/*
	private void processDelta(double[][] sampleData) {

		System.out.println("=============================");
		// search the potential voc to process on channel 0, base on waveform amplitude

		ArrayList<SoundPatch> soundPatchList = new ArrayList<SoundPatch>();

		int window = (int) (20 * (166666 / 1000));
		double dataChannel0[] = sampleData[0];
		{
			SoundPatch soundPatch = null;
			for ( int t=window ; t < dataChannel0.length-window ; t++ )
			{
				double maxPower = 0;
				for ( int w = - window ; w < window ; w++ )
				{
					double value = Math.pow( dataChannel0[w+t] , 2 );
					if ( maxPower < value )
					{
						maxPower = value;
					}
				}

				if ( maxPower > 0.005 )
				{
					if ( soundPatch == null )
					{
						soundPatch = new SoundPatch();
						soundPatch.start = t;
					}else
					{
						soundPatch.end = t;
					}
				}else
				{
					if ( soundPatch != null )
					{
						soundPatch.end = t;
						soundPatchList.add( soundPatch );
						soundPatch = null;
					}
				}

			}
			// check if a sound patch is remaining
			if ( soundPatch != null )
			{
				if ( soundPatch.end != 0 )
				{
					soundPatchList.add( soundPatch );
				}
			}
		}

		// filter patch size.
//		int nbRemoved = 0;
		for ( SoundPatch soundPatch : new ArrayList<SoundPatch>( soundPatchList ) )
		{
			if ( soundPatch.getLength() < 1700 )
			{
//				System.out.println( "Remove Patch : " + soundPatch.start + " to " + soundPatch.end );
//				nbRemoved ++;
				soundPatchList.remove( soundPatch );
			}
		}

		// process delta towards channel 0.
		for ( SoundPatch soundPatch : soundPatchList )
		{
			System.out.println("********* Patch len: " + (int)(soundPatch.getLength()* ( 1000d / 166666d )) + " ms\tStart at:"  + soundPatch.start );
			int searchWindow = 1500;
			for ( int channelCandidate = 1 ; channelCandidate < sampleData.length ; channelCandidate++ )
			{
				double maxCorrelation = Double.MIN_VALUE;
				int bestOffset = 0;
				for ( int swt = -searchWindow ; swt < searchWindow ; swt++ )
				{
					double correlation = MathUtil.correlation(
							sampleData[0], soundPatch.start, sampleData[channelCandidate], soundPatch.start+swt, soundPatch.getLength() );
					if ( correlation > maxCorrelation )
					{
						maxCorrelation = correlation;
						bestOffset = swt;
					}
				}

//				if ( bestOffset > 10 ) // just to avoid the loop click
				{
//				System.out.println(
//						"Best offset ch0-ch: "+channelCandidate +" : \t" + bestOffset * (1000d / 166666) +
//						"corr:" + (int)( 100 * maxCorrelation ) );
				double offsetInSecond = ( 1d / 166666d ) * bestOffset;
				double offsetInMeter = offsetInSecond * 340;
				double offsetInCentimeter = (int) ( offsetInMeter * 100 );
//				System.out.println("Offset in second  " + offsetInSecond );
//				System.out.println("Offset in meter  " + offsetInMeter );

				System.out.println( "OFFSET In centimeter:\t" + offsetInCentimeter + "\tcorr:" + (int)( 100 * maxCorrelation ) );

				}
			}
		}



	}
*/
}
