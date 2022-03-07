package plugins.fab.kinectdriver;

import java.io.IOException;

import icy.common.exception.UnsupportedFormatException;
import icy.file.Loader;
import icy.image.IcyBufferedImage;

public class CacheFileLoaded {

	String fileName;
	IcyBufferedImage image;

	public CacheFileLoaded(String fileName) {

		this.fileName = fileName;
		try {
			Loader.loadImage( fileName );
		} catch (UnsupportedFormatException | IOException e) {
			System.err.println("Cache file loader: can't load image " + fileName );
			e.printStackTrace();
		}
	}

}
