package plugins.fab.aaa.voc;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

import icy.common.exception.UnsupportedFormatException;
import icy.file.FileUtil;
import icy.file.Loader;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.plugin.abstract_.PluginActionable;
import icy.plugin.interface_.PluginThreaded;
import icy.sequence.Sequence;
import icy.type.DataType;


public class ClusterVocTest extends PluginActionable implements PluginThreaded {

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
			IcyBufferedImage image2 = IcyBufferedImageUtil.getCopy( image );

			// epaissir
			for ( int y = 0 ; y < image.getHeight() ; y++ )
			{
				for ( int x = 0 ; x < image.getWidth() ; x++ )
				{
					double value = image2.getData(x, y, 0);
					if ( value < 0.5 )
					{

					}
				}
			}
			*/
			/*
			int width = image.getWidth();
			double[] data2 = new double[ data.length ];
			for ( int i = 0 ; i < data.length ; i++ )
			{
				data2[i] = data[i];
			}

			for ( int i = 0 ; i < data2.length ; i++ )
			{
				if ( Double.isNaN( data2[i] ) )
				{
					data[i] = 0;
					continue;
				}
				if ( data2[i] < 0.5 )	// remove low signal to keep high energy spectrum
				{
					data[i] = 0;
				}
				else
				{
					for ( int yy = -2 ; yy < 2 ; yy++ )
					{
						for ( int xx = -2 ; xx < 2 ; xx++ )
						{
							data[i+xx+yy*width] = 1;
						}
					}
				}
			}
			*/




		}

		public String getEasyName()
		{
			return FileUtil.getFileName( name , false );
		}

	}

	ArrayList<ImageInfo> imageInfoList = new ArrayList<>();

	public void run() {

		String folder = "e:/vocNormalized/" ;

		imageInfoList.add( new ImageInfo( folder+"T2019-01-25_21-56-29_0000161_p_100_l_3_c_2 ts_9673s.tif" ) );
		imageInfoList.add( new ImageInfo( folder+"T2019-01-27_21-40-20_0000426_p_100_l_3_c_1 ts_1827s.tif" ) );
//		imageInfoList.add( new ImageInfo( folder+"T2019-01-26_00-36-00_0000215_p_085_l_2_c_1 ts_4293s.tif" ) );
//		imageInfoList.add( new ImageInfo( folder+"T2019-01-26_06-11-31_0000291_p_099_l_2_c_2 ts_3096s.tif" ) );

		for ( ImageInfo imageInfo : imageInfoList )
		{
			imageInfo.loadImage();
			Sequence sequence = new Sequence ( imageInfo.image );
			addSequence( sequence );
		}

		ImageInfo imageInfo1 = imageInfoList.get( 0 );
		ImageInfo imageInfo2 = imageInfoList.get( 1 );

		System.out.println("Load finished.");
		DecimalFormat df = new DecimalFormat("0.00");


		double corr = MathUtil.correlation( imageInfo1.data, imageInfo2.data, 0);
		System.out.println("Peaeson Correlation: "+ corr );

		{
			double corrWithMax = correlationWithMax( imageInfo1, imageInfo2 );
			System.out.println("Corr with max: "+ corrWithMax );
		}

		{
			double corrWithMax = correlationWithMax( imageInfo2, imageInfo1 );
			System.out.println("Corr with max: "+ corrWithMax );
		}


		/*

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

*/


		System.out.println("All done");


	}



	private int getMaxY(IcyBufferedImage image1, int x) {
		int maxY = -1;
		double maxValue = 0;
		for ( int y = 0 ; y < image1.getHeight() ; y++ )
		{
			double value = image1.getData( x, y, 0);
			if ( value > maxValue )
			{
				maxValue = value;
				maxY = y;
			}
		}
		if ( maxValue < 0.5 )
		{
			return -1;
		}
		return maxY;
	};


	private double correlationWithMax( ImageInfo i1, ImageInfo i2 ) {

		IcyBufferedImage image1 = i1.image;
		IcyBufferedImage image2 = i2.image;

		double score = 0;

		for ( int x = 0 ; x < image1.getWidth() ; x++ )
		{
			int y1 = getMaxY( image1 , x );
			int y2 = getMaxY( image2 , x );

			if ( y1==-1 && y2 == -1 )
			{
				score++;
				continue;
			}
			
			if ( y1==-1 || y2 == -1 )
			{				
				continue;
			}

			if ( Math.abs( y1-y2 ) < 5 ) // distance in pixels
			{
				score++;
			}
			// check the max in the other image.
		}
		score/=image1.getWidth();

		return score;
	}












}