package plugins.fab.aaa.voc.test;

import java.io.*;

import icy.system.profile.Chronometer;
import plugins.fab.aaa.voc.WavFile;

// from http://www.labbookpages.co.uk/audio/javaWavFiles.html

public class ReadExample
{
	public WavFile readFile( File file )
	{
		try
		{
			// Open the wav file specified as the first argument
			WavFile wavFile = WavFile.openWavFile( file);

			// Display information about the wav file
			wavFile.display();


			// Get the number of audio channels in the wav file
			int numChannels = wavFile.getNumChannels();

			// Create a buffer of 100 frames
			double[] buffer = new double[100 * numChannels];

			int framesRead;

			double min = Double.MAX_VALUE;
			double max = -Double.MAX_VALUE;

			do
			{
				// Read frames into buffer
				framesRead = wavFile.readFrames(buffer, 100);

				// Loop through frames and look for minimum and maximum value
				for (int s=0 ; s<framesRead * numChannels ; s++)
				{
					if (buffer[s] > max) max = buffer[s];
					if (buffer[s] < min) min = buffer[s];
				}
			}
			while (framesRead != 0);

			// Close the wavFile


			//wavFile.close();

			// Output the minimum and maximum value
			System.out.printf("Min: %f, Max: %f\n", min, max);


			return wavFile;

		}
		catch (Exception e)
		{
			System.err.println(e);
		}
		return null;

	}
}
