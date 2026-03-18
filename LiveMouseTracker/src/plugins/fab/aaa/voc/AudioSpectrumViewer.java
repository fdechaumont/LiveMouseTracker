package plugins.fab.aaa.voc;

import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.image.colormap.LinearColorMap;
import icy.main.Icy;
import icy.sequence.Sequence;
import icy.type.DataType;

public class AudioSpectrumViewer {

	public Sequence showSequence(double[][][] magnitude, Sequence outSequence) {
		Sequence seq = updateSequence(outSequence, magnitude);
		//if (magnitude.length == 1) // if only 1 channel is active
		{
			setupMonoSequence(seq);
		}
		if (outSequence == null) // if the sequence was delivered, don't manage
									// the show
		{
			Icy.getMainInterface().addSequence(seq);
		}
		return seq;
	}

	public static void setupMonoSequence(Sequence sequence) {
		sequence.setColormap(0, LinearColorMap.gray_inv_);

		Viewer viewer = sequence.getFirstViewer();

		if (viewer != null) {
			viewer.getLut().getLutChannel(0).setColorMap(LinearColorMap.gray_inv_, false);
		}

		// sequence.getImage( 0 , 0).setColorMap( 0 ,
		// LinearColorMap.gray_inv_,false );
		// getLUT().setColormaps( LinearColorMap.gray_inv_ );
		// Icy.getMainInterface().addSequence( sequence );
	}

	public static Sequence updateSequence(Sequence outSequence, double[][][] magnitude) {
		if (outSequence == null) {
			outSequence = new Sequence();
		}
		int width = magnitude[0].length;
		int height = magnitude[0][0].length;

		outSequence.beginUpdate();
		try {
			IcyBufferedImage image = outSequence.getImage(0, 0);

			{
				boolean resetImage = false;
				if (image == null) {
					resetImage = true;
				} else {
					if (image.getWidth() != width || image.getHeight() != height
							|| image.getSizeC() != magnitude.length) {
						resetImage = true;
					}
				}

				if (resetImage) {
					outSequence.removeAllImages();
					image = new IcyBufferedImage(width, height, magnitude.length, DataType.FLOAT);
					outSequence.addImage(image);
				}
			}

			image.beginUpdate();

			int heightImage = image.getHeight();
			//for (int channel = 0; channel < magnitude.length; channel++)
			int channel = 0;
			{
				// draw FFT magnitude
				for (int x = 0; x < width; x++) {
					for (int y = 0; y < height; y++) {
						double value = magnitude[channel][x][heightImage - y - 1]; //Math.log(magnitude[channel][x][image.getHeight() - y - 1]);
						if (value > 8)
							value = 8;
						image.setData((int) x, y, channel, value);
					}
				}
			}

			image.endUpdate();
			outSequence.updateChannelsBounds();
			// image.dataChanged();

		} finally {
			outSequence.endUpdate();
		}

		return outSequence;
	}

}
