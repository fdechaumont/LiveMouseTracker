package plugins.fab.aaa.voc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;

import icy.common.exception.UnsupportedFormatException;
import icy.file.FileUtil;
import icy.file.Loader;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.plugin.abstract_.PluginActionable;
import icy.plugin.interface_.PluginThreaded;
import icy.system.thread.Processor;
import icy.type.DataType;
import icy.util.XLSUtil;
import jxl.Sheet;
import jxl.Workbook;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import plugins.fab.aaa.voc.ClusterVocTest.ImageInfo;


public class ClusterVoc2 extends PluginActionable implements PluginThreaded {

	public class ImageInfo
	{
		String name;
		IcyBufferedImage image;
		double data[];

		public ImageInfo(String name) {
			this.name = name;
		}

		void loadImage()
		{
			try {
				System.out.println( "loading " + name );
				image = Loader.loadImage( name );
			} catch (UnsupportedFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			image = IcyBufferedImageUtil.convertToType( image, DataType.DOUBLE, false);

			data = image.getDataXYAsDouble(0);
			/*
			for ( int i = 0 ; i < data.length ; i++ )
			{
				if ( Double.isNaN( data[i] ) )
				{
					data[i] = -10;
					continue;
				}
				if ( data[i] < 0.2 )	// remove low signal to keep high energy spectrum
				{
					data[i] = 0;
				}
			}
			*/
			// image = null;



		}

		public String getEasyName()
		{
			return FileUtil.getFileName( name , false );
		}

	}

	ArrayList<ImageInfo> imageInfoList = new ArrayList<>();

	public void run() {

		File folder = new File( "e:/vocNormalized/" );

		File[] listOfFiles = folder.listFiles();

		int nbFile = 0;
		for ( File file : listOfFiles )
		{
			if ( file.getName().contains("s.tif") ) // means it is scaled
			{
				System.out.println("registering " + file.getName() );
				imageInfoList.add( new ImageInfo( file.getAbsolutePath() ) );
				nbFile++;
			}
//			if( nbFile > 1000 )
//			{
//				break;
//			}
		}

		Processor p = new Processor( 500 );

		for ( ImageInfo imageInfo : imageInfoList )
		{
			p.submit( new Runnable() {

						@Override
						public void run() {

							imageInfo.loadImage();

						}
					});
		}

		while ( true )
		{
			int nbTask = p.getWaitingTasksCount();

			if ( !p.isProcessing() ) break;
			System.out.println("Nb File remaining (load): " + nbTask );
			try {
				Thread.sleep( 1000 );
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		System.out.println("Load finished.");
		DecimalFormat df = new DecimalFormat("0.00");


		// perform match
		File fout = new File("e:/vocNormalized/out.csv");
		try {
			FileOutputStream fos = new FileOutputStream(fout);
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
			// first line label.
			bw.write("nothing;");
			for ( ImageInfo imageInfo1 : imageInfoList )
			{
				bw.write( imageInfo1.getEasyName() +";");
			}
			bw.newLine();

			for ( ImageInfo imageInfo1 : imageInfoList )
			{
				System.out.println("Computing " + imageInfo1.getEasyName() );
				bw.write( imageInfo1.getEasyName() +";"); // write row name

				for ( ImageInfo imageInfo2 : imageInfoList )
				{
					//double corr = MathUtil.correlation( imageInfo1.data, imageInfo2.data, 0);
					double corr1 = correlationWithMax( imageInfo1, imageInfo2,0 );
					//double corr2 = correlationWithMax( imageInfo1, imageInfo2,-1 );
					//double corr3 = correlationWithMax( imageInfo1, imageInfo2,1 );

					double maxCorr = corr1;
//					if ( corr2 > maxCorr ) maxCorr = corr2;
//					if ( corr3 > maxCorr ) maxCorr = corr3;

					bw.write( ""+ df.format(maxCorr) +";");
				}
				bw.newLine();
			}
			bw.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		/* too big for xls. no support of xlsx in jxl lib
		File fout = new File("e:/vocNormalized/out.xlsx");
		try {
			WritableWorkbook workBook = XLSUtil.createWorkbook( fout );
			WritableSheet sheet = XLSUtil.createNewPage( workBook, "data");



			int y = 0;
			int x = 0;

			// write column header:
			{
				x = 1;
				for ( ImageInfo imageInfo1 : imageInfoList )
				{
					XLSUtil.setCellString(sheet, x, y, imageInfo1.getEasyName() );
					x++;
				}
			}
			y++;
			// write correlations:
			for ( ImageInfo imageInfo1 : imageInfoList )
			{
				x=0;
				System.out.println("Computing " + imageInfo1.getEasyName() );
				XLSUtil.setCellString(sheet, x, y, imageInfo1.getEasyName() );
				x++;
				for ( ImageInfo imageInfo2 : imageInfoList )
				{
					double corr = MathUtil.correlation( imageInfo1.data, imageInfo2.data, 0);
					XLSUtil.setCellNumber(sheet, x, y, corr );
					//bw.write( ""+ df.format(corr) +";");
					x++;
				}
				y++;
			}
			XLSUtil.saveAndClose(workBook);


		} catch (IOException e1) {

			e1.printStackTrace();
		} catch (WriteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/


		System.out.println("All done");


	};

	private int getSlope( ImageInfo image1, int x) {

		int s1 = getMaxY( image1, x-1);
		int s2 = getMaxY( image1, x+1);
		if ( s1 == -1|| s2 == -1 )
		{
			return -1;
		}
		return s2-s1;

	}

	private int getMaxY( ImageInfo image1, int x) {
		int maxY = -1;
		double maxValue = 0;
		double[] data = image1.data;
		int height = image1.image.getHeight();
		int width = image1.image.getWidth();
		int offset = x;
		for ( int y = 0 ; y < height ; y++ )
		{
			//double value = image1.getData( x, y, 0);
			double value = data[offset];
			if ( value > maxValue )
			{
				maxValue = value;
				maxY = y;
			}
			offset+=width;
		}
		if ( maxValue < 0.2 )
		{
			return -1;
		}
		return maxY;
	};


	private double correlationWithMax( ImageInfo i1, ImageInfo i2, int shiftX ) {

		IcyBufferedImage image1 = i1.image;

		double score = 0;
		int width = image1.getWidth();
		double nbPoint = 0;
		//for ( int x = 0 ; x < width ; x++ )
		for ( int x = 1 ; x < width-1 ; x++ )
		{
			int shiftedX = x+shiftX;
			if ( shiftedX < 0 || shiftedX > width-1 )
			{
				continue;
			}

			nbPoint ++;

			int y1 = getSlope( i1 , x );
			int y2 = getSlope( i2 , shiftedX );

			if ( y1 == -1 && y2 == -1 )
			{
				score++;
				continue;
			}

			if ( (y1 == -1) ^ (y2 == -1) )
			{
				score--;
				continue;
			}

			if ( Math.abs( y1-y2 ) < 1 ) // distance in pixels
			{
				score++;
			}

			/*

			  with max in y
			int y1 = getMaxY( i1 , x );
			int y2 = getMaxY( i2 , shiftedX );

			if ( y1 == -1 && y2 == -1 )
			{
				score++;
				continue;
			}

			if ( (y1 == -1) ^ (y2 == -1) )
			{
				score--;
				continue;
			}

			if ( Math.abs( y1-y2 ) < 2 ) // distance in pixels
			{
				score++;
			}
			// check the max in the other image.
	*/
		}
		//score/=image1.getWidth();
		score/=nbPoint;

		if ( Double.isNaN( score ) )
		{
			score = 0;
		}

		return score;
	}



}
