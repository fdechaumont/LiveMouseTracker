package plugins.fab.aaa.voc;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Scanner;

import icy.canvas.IcyCanvas;
import icy.common.exception.UnsupportedFormatException;
import icy.file.FileUtil;
import icy.file.Loader;
import icy.file.Saver;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.painter.Overlay;
import icy.plugin.abstract_.PluginActionable;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import icy.system.SystemUtil;
import icy.system.thread.Processor;
import icy.type.DataType;
import icy.type.point.Point5D.Double;
import loci.formats.FormatException;
import plugins.fab.aaa.voc.ClusterVoc2.ImageInfo;

public class ClusterViewer extends PluginActionable{

	Sequence outSequence = null;
	Sequence wavViewerSequence = new Sequence("Wav Viewer (shift move)");

	ClusterViewerOverlay clusterViewerOverlay = null;

	//ArrayList<String> fileList = new ArrayList<>();
	String reOrderedFileArray[] = null;
	String originalFileArray[] = null;

	ArrayList<Integer> breakFamillyIndexList = new ArrayList<>();

	class ClusterViewerOverlay extends Overlay
	{
		Sequence sequence;
		public ClusterViewerOverlay( Sequence sequence ) {
			super("Wav picker overlay");
			this.sequence = sequence;
			System.out.println("Overlay: Press (shift-L)oad voc in ROI2D");
		}

		@Override
		public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas) {

			g.setColor( Color.yellow );
			for ( int i : breakFamillyIndexList )
			{
				g.drawLine( 0,i, sequence.getWidth(), i);
			}
		}

		@Override
		public void keyPressed(KeyEvent e, Double imagePoint, IcyCanvas canvas) {

			if ( e.getKeyChar()=='k')
			{
				Sequence wavViewerROISequence = new Sequence();
				if ( sequence.getROI2Ds().size() != 0 )
				{
					ROI2D roi = (ROI2D)sequence.getROI2Ds().get( 0 );
					wavViewerROISequence.removeAllImages();
					int t = 0;
					for ( int y = (int) roi.getBounds().getMinY() ; y < roi.getBounds().getMaxY() ; y++ )
					{
						try
						{
							String fileName = reOrderedFileArray[ y ];
							System.out.println(fileName);
							IcyBufferedImage image = Loader.loadImage( "e:/vocNormalized/"+fileName+".tif" );
							wavViewerROISequence.setImage( t, 0, image );
							t++;
						}
						catch( IndexOutOfBoundsException e2 )
						{

						} catch (UnsupportedFormatException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}

				}
				addSequence(wavViewerROISequence);
			}

			float MATCH_THRESHOLD = 0.95f;

			if ( e.getKeyChar()=='p') // for prune-match
			{
				int height = imageReIndexed.getHeight();
				for ( int index = 0 ; index < height ; index++ )
				{
					double[] lineToMatch = getImageReIndexLineAsArray( index );
					System.out.println("--- match test: index: "+ index );

					boolean matchFound =false;
					for ( int y = 0 ; y < height ; y++ )
					{
						if ( y != index )
						{
							double[] candidate = getImageReIndexLineAsArray( y );
							double scoreMatch = MathUtil.correlation( lineToMatch, candidate );
							if ( scoreMatch > MATCH_THRESHOLD )
							{
								matchFound = true;
								break;
							}
						}
					}
					if ( !matchFound)
					{
						clearImageReIndexLine( index );
					}

				}
				imageReIndexed.dataChanged();
			}

			if ( e.getKeyChar()=='m') // for match
			{
				int index = (int)imagePoint.getY();
				System.out.println("--- match test: index: "+ index );
				Sequence matchViewer = new Sequence("match type 1");
				int t = 0;
				// find best match

				double[] lineToMatch = getImageReIndexLineAsArray( index );

				try {
					String fileName = reOrderedFileArray[ index ];
					IcyBufferedImage image;
					image = Loader.loadImage( "e:/vocNormalized/"+fileName+".tif" );
					matchViewer.setImage( t, 0, image );
					t++;
				} catch (UnsupportedFormatException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				}


				for ( int y = 0 ; y < imageReIndexed.getHeight() ; y++ )
				{
					if ( y != index )
					{
						double[] candidate = getImageReIndexLineAsArray( y );
						double scoreMatch = MathUtil.correlation( lineToMatch, candidate );
						if ( scoreMatch > MATCH_THRESHOLD )
						{
							try {
								String fileName = reOrderedFileArray[ y ];
								System.out.println(scoreMatch+" ," + fileName);
								IcyBufferedImage image;
								image = Loader.loadImage( "e:/vocNormalized/"+fileName+".tif" );
								matchViewer.setImage( t, 0, image );
								t++;
							} catch (UnsupportedFormatException e1) {
								e1.printStackTrace();
							} catch (IOException e1) {
								e1.printStackTrace();
							}
						}
					}
				}


				addSequence( matchViewer );
			}


			if ( e.getKeyChar()=='n') // for match type 2
			{
				int index = (int)imagePoint.getY();
				System.out.println("--- match test type 2: index: "+ index );

				int t = 0;
				// find best match

				double[] matchLine = getImageReIndexLineAsArray( index );

				String originalFileName = reOrderedFileArray[ index ];
				try {

					System.out.println( "original file: " + originalFileName );
					IcyBufferedImage image = Loader.loadImage( "e:/vocNormalized/"+originalFileName.subSequence(0, originalFileName.length()-1)+".tif" );
					Sequence matchViewer = new Sequence("match type 2 / original");
					matchViewer.setImage( t, 0, image );
					addSequence( matchViewer );
				} catch (UnsupportedFormatException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				}


				for ( int x = 0 ; x < imageReIndexed.getWidth() ; x++ )
				{
					if ( x != index )
					{
						double scoreMatch = matchLine[x];
						if ( scoreMatch > 0.8 )
						{
							String fileName = originalFileArray[ x ];
							if ( fileName.equals( originalFileName ) ) continue;
							try {
								System.out.println( x+ " , " + scoreMatch+" ," + fileName);
								IcyBufferedImage image = Loader.loadImage( "e:/vocNormalized/"+fileName.subSequence(0, fileName.length()-1)+".tif" );
								Sequence matchViewer = new Sequence("match type 2");
								matchViewer.setImage( 0, 0, image );
								addSequence( matchViewer );

							} catch (UnsupportedFormatException e1) {
								e1.printStackTrace();
							} catch (IOException e1) {
								e1.printStackTrace();
							}
						}
					}
				}


			}

			if ( e.getKeyChar()=='v') // for match type 2 export for D3.js graph view in json
			{
				exportInJSON();
			}

			if ( e.getKeyChar()=='x') // convert all to png
			{
				convertToPNG();
			}


		}

		private void convertToPNG() {

			File folder = new File( "e:/vocNormalized/" );

			File[] listOfFiles = folder.listFiles();
			Processor processor = new Processor( Integer.MAX_VALUE , SystemUtil.getNumberOfCPUs()*8 );
			int nbFile = 0;
			for ( File file : listOfFiles )
			{
				if ( file.getName().contains(".tif") )
				{
					processor.submit( new Runnable() {

						@Override
						public void run() {
							try {

								System.out.println( "loading " + file );
								IcyBufferedImage image = Loader.loadImage( file );
								image = IcyBufferedImageUtil.convertToType( image , DataType.BYTE, true );
								String filePNG = "e:/vocNormalized/"+ FileUtil.getFileName( file.getName(), false )+".png";
								Saver.saveImage( image , new File ( filePNG ), true );

							} catch (UnsupportedFormatException | IOException | FormatException e) {
								e.printStackTrace();
							}
						}
					});
					//break;
				}
			}

			while ( processor.isProcessing() )
			{
				System.out.println("Waiting tasks: " + processor.getWaitingTasksCount() );

				try {
					Thread.sleep( 1000 );
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			System.out.println("PNG export done.");

		}

		ArrayList<String> nodeList = null;

		private void exportInJSON() {

			nodeList = new ArrayList<String>();

/*
			System.out.println("{ \"nodes\": [ ");
			for ( int index = 0 ; index < imageOriginalIndex.getHeight() ; index++ )
			{
				// {"name": "Myriel", "group": 1},
				String fileName = originalFileArray[ index ];
				System.out.print(" {\"id:\": \""+fileName+"\""+ ",\"group\":1}" );
				if ( index != imageOriginalIndex.getHeight()-1 )
				{
					System.out.println(",");
				}
			}

			System.out.println(" ], ");
*/
			String links="";

			// links+=" \"links\": [ \n";

			String pythonNetworkX ="";
			ArrayList<String> stringToSave = new ArrayList<String>();

			for ( int index = 0 ; index < imageOriginalIndex.getHeight() ; index++ )
			{
				System.out.println( index );
				int nbMatch = 0;
				int t = 0;
				// find best match

				double[] matchLine = getImageOriginalIndexLineAsArray( index );

				String originalFileName = originalFileArray[ index ];
				if ( originalFileName == null ) continue;

				for ( int x = 0 ; x < imageOriginalIndex.getWidth() ; x++ )
				{
					if ( x != index )
					{
						double scoreMatch = matchLine[x];

						//if ( scoreMatch > 0.95 ) continue; // is most likely a doublon

						if ( scoreMatch > 0.5 )
						{
							String fileName = originalFileArray[ x ];
							if ( fileName == null ) continue;

							if ( fileName.equals( originalFileName ) ) continue;

							// file is ok.
						    // {"source": 1, "target": 0, "value": 1},
//							int source = registerNode( originalFileName );
//							int target = registerNode( fileName );
							//links+= "{\"source\": "+source+" , \"target\":"+target+", \"value\":1},\n";

							stringToSave.add( originalFileName+","+fileName+","+scoreMatch+"\n" );
							// TEST pythonNetworkX+=originalFileName+","+fileName+","+scoreMatch+"\n";
							nbMatch++;
						}
					}
				}


				System.out.println("Nb match: " + nbMatch );

			}

			//links+=" ] } \n";

			/*
			String nodes ="";
			nodes = "{ \"nodes\": [ \n";
			//for ( int index = 0 ; index < imageOriginalIndex.getHeight() ; index++ )
			for ( String name : nodeList )
			{
				nodes+=" {\"name:\": \""+name+"\""+ ",\"group\":1},\n" ;
			}

			nodes+=" ], \n";
			*/

			/*
			System.out.println( nodes );
			System.out.println( links );
*/

			//System.out.println( pythonNetworkX );
			System.out.println("Saving data...");

			BufferedWriter writer;
			try {
				writer = new BufferedWriter(new FileWriter("e:/vocNormalized/links.txt"));
				for ( String s : stringToSave )
				{
					writer.write(s);
				}
				//writer.write( pythonNetworkX );
				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Done");

		}

		private int registerNode(String fileName) {

			// search in nodeList:
			for ( int i = 0 ; i < nodeList.size() ; i++ )
			{
				if ( fileName.equals(
						nodeList.get( i ) ))
				{
					return i;
				}
			}
			// not found. register it.
			nodeList.add( fileName );
			return nodeList.size()-1;

		}

		private void clearImageReIndexLine(int index) {

			int width = imageReIndexed.getWidth();
			double data[] = imageReIndexed.getDataXYAsDouble( 0 );
			int offset = index*width;
			for ( int x = 0 ; x < imageReIndexed.getWidth() ; x++ )
			{
				data[x+offset] = 0;
				//imageReIndexed.setData(x, index, 0, 0 );
			}

		}

		private double[] getImageReIndexLineAsArray(int index) {

			int width = imageReIndexed.getWidth();
			double[] line = new double[ width ];
			double data[] = imageReIndexed.getDataXYAsDouble( 0 );

			int offset = index*width;
			for ( int x = 0 ; x < imageReIndexed.getWidth() ; x++ )
			{
				//line[x] = imageReIndexed.getData(x, index, 0); // to speed up.
				line[x] = data[x+offset];
			}
			return line;

		}

		private double[] getImageOriginalIndexLineAsArray(int index) {

			int width = imageOriginalIndex.getWidth();
			double[] line = new double[ width ];
			double data[] = imageOriginalIndex.getDataXYAsDouble( 0 );

			int offset = index*width;
			for ( int x = 0 ; x < imageOriginalIndex.getWidth() ; x++ )
			{
				//line[x] = imageReIndexed.getData(x, index, 0); // to speed up.
				line[x] = data[x+offset];
			}
			return line;

		}

		@Override
		public void mouseMove(MouseEvent e, Point2D imagePoint, IcyCanvas canvas) {

			if ( !e.isShiftDown() ) return;

			try
			{
				int index = (int)imagePoint.getY();

				String fileName = reOrderedFileArray[index];
//				System.out.println("fileName");

				try {
					IcyBufferedImage image = Loader.loadImage( "e:/vocNormalized/"+fileName.subSequence(0, fileName.length()-1)+".tif" );
					wavViewerSequence.setImage( 0, 0, image);
				} catch (UnsupportedFormatException | IOException e1) {
					e1.printStackTrace();
				}
			}
			catch( IndexOutOfBoundsException e2 )
			{

			}
		}


	}

	IcyBufferedImage imageReIndexed = null;
	IcyBufferedImage imageOriginalIndex = null;

	@Override
	public void run() {

		ArrayList<String[]> rowDataList = new ArrayList<>();
		String row;
		try {
			BufferedReader csvReader = new BufferedReader(new FileReader("e:/vocNormalized/out.csv"));
			while ( (row = csvReader.readLine() ) != null)
			{
			    String[] data = row.split(";");
			    rowDataList.add( data );
			}
			csvReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		rowDataList.remove( 0 ); // remove headers.

		int nbData = rowDataList.size();
		System.out.println( "nb data: " + nbData );

		imageOriginalIndex = new IcyBufferedImage( nbData, nbData, 1, DataType.DOUBLE );

		for ( int y = 0 ; y < nbData ; y++ )
		{
			for ( int x = 1 ; x < nbData ; x++ )
			{
				double value = Float.parseFloat( rowDataList.get( y )[x].replace(",", ".") );
				imageOriginalIndex.setData(x-1, y, 0, value );
			}
		}

		outSequence = new Sequence();
		outSequence.setImage( 0 , 0, imageOriginalIndex);
		addSequence( outSequence );

		// load re-indexer

		File file = new File("e:/vocNormalized/reIndexed.txt");
		ArrayList<Integer> reIndexedList = new ArrayList<>();

		try {
			Scanner sc;
			sc = new Scanner(file);
			while (sc.hasNextLine())
			{
				String line = sc.nextLine();
				reIndexedList.add( Integer.parseInt( line ));
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		imageReIndexed = new IcyBufferedImage( nbData, nbData, 1, DataType.DOUBLE );
		reOrderedFileArray = new String[nbData];
		originalFileArray = new String[nbData];

		System.out.println("Build file list");
		for ( int y = 0 ; y < nbData-1 ; y++ )
		{
			try
			{
				int newIndex = reIndexedList.get( y );

				//newIndex+=2;
				//fileList.add( rowDataList.get( newIndex + 2 )[0] ); // +2 because of headers
				reOrderedFileArray[y] = rowDataList.get( newIndex  )[0] ;
				originalFileArray[y] = rowDataList.get( y  )[0];

				System.out.println( y + "\t"+ newIndex+ "\t" + rowDataList.get( newIndex )[0] );
				for ( int x = 1 ; x < nbData-1 ; x++ )
				{
					//float value = Float.parseFloat( rowDataList.get( newIndex )[x].replace(",", ".") );
					double value = Float.parseFloat( rowDataList.get( newIndex )[x].replace(",", ".") );
					imageReIndexed.setData(x-1, y, 0, value );
				}
			}catch( Exception e )
			{

			}

		}

		Sequence outSequenceReIndexed = new Sequence("Cluster. Press k with roi to get all vocs selected");
		outSequenceReIndexed.setImage( 0 , 0, imageReIndexed );
		addSequence( outSequenceReIndexed );
		clusterViewerOverlay = new ClusterViewerOverlay( outSequenceReIndexed );
		outSequenceReIndexed.addOverlay( clusterViewerOverlay );

		// find families
/*
		File fileFamily = new File("e:/vocNormalized/family.txt");
		FileOutputStream fos;
		try {
			fos = new FileOutputStream( fileFamily );
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
			for ( int y = 0; y < nbData-2; y++ )
			{

				bw.write( fileArray[y] + "\n" );

				double[] line1 = new double[nbData];
				double[] line2 = new double[nbData];
				for ( int x = 0 ; x < nbData-1 ; x++ )
				{
					line1[ x ] = imageReIndexed.getData( x, y, 0 );
					line2[ x ] = imageReIndexed.getData( x, y+1, 0 );
				}

				double correlation = MathUtil.correlation( line1, line2 );
				if ( correlation < 0.8 )
				{
					breakFamillyIndexList.add( y );
					bw.write("---\n");
				}

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/

		// end find family

		addSequence( wavViewerSequence );

	}


}
