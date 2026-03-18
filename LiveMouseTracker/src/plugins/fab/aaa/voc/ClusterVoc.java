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


public class ClusterVoc extends PluginActionable implements PluginThreaded {

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
			image = null;



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
					double corr = MathUtil.correlation( imageInfo1.data, imageInfo2.data, 0);

					bw.write( ""+ df.format(corr) +";");
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

}
