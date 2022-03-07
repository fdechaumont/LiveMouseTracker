package plugins.fab.aaa.kinectDKTest;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.io.PipedInputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.plugin.abstract_.Plugin;
import icy.plugin.abstract_.PluginActionable;
import icy.plugin.interface_.PluginThreaded;
import icy.sequence.Sequence;
import icy.system.profile.Chronometer;
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;
import plugins.fab.aaa.voc.VocAnalysisListener;
import plugins.kernel.roi.roi2d.ROI2DRectangle;
import plugins.kernel.roi.roi2d.plugin.ROI2DRectanglePlugin;

public class KinectDK_Double_Merge extends PluginActionable implements PluginThreaded {

	enum DEPTH_MODE {
		K4A_DEPTH_MODE_NFOV_UNBINNED_640x576,
		K4A_DEPTH_MODE_NFOV_BINNED_512x512,
		K4A_DEPTH_MODE_WFOV_UNBINNED_1024x1024,
		K4A_DEPTH_MODE_WFOV_BINNED_512x512
		};

	ROI2DRectangle rectangle = new ROI2DRectangle( 0,0,640,576 );

	@Override
	public void run() {

		//DEPTH_MODE mode = DEPTH_MODE.K4A_DEPTH_MODE_NFOV_UNBINNED_640x576;
		//DEPTH_MODE mode = DEPTH_MODE.K4A_DEPTH_MODE_WFOV_UNBINNED_1024x1024;
		DEPTH_MODE mode = DEPTH_MODE.K4A_DEPTH_MODE_NFOV_UNBINNED_640x576;

		int FRAME_WIDTH = 0;
		int FRAME_HEIGHT = 0;
		int BYTES_PER_PIXEL = 2;

		if ( mode == DEPTH_MODE.K4A_DEPTH_MODE_NFOV_UNBINNED_640x576 )
		{
			FRAME_WIDTH = 640;
			FRAME_HEIGHT = 576;
		}

		if ( mode == DEPTH_MODE.K4A_DEPTH_MODE_WFOV_UNBINNED_1024x1024 )
		{
			FRAME_WIDTH = 1024;
			FRAME_HEIGHT = 1024;
		}

		if ( mode == DEPTH_MODE.K4A_DEPTH_MODE_WFOV_BINNED_512x512 )
		{
			FRAME_WIDTH = 512;
			FRAME_HEIGHT = 512;
		}

		IcyBufferedImage imageDepth = new IcyBufferedImage( FRAME_WIDTH, FRAME_HEIGHT , 1 , DataType.USHORT );
		Sequence sequenceDepth = new Sequence("Depth cam 1");
		sequenceDepth.addImage( imageDepth );


		IcyBufferedImage imageIR = new IcyBufferedImage( FRAME_WIDTH, FRAME_HEIGHT , 1 , DataType.USHORT );
		Sequence sequenceIR = new Sequence("Infra cam 1");
		sequenceIR.addImage( imageIR );


		IcyBufferedImage imageDepth2 = new IcyBufferedImage( FRAME_WIDTH, FRAME_HEIGHT , 1 , DataType.USHORT );
		Sequence sequenceDepth2 = new Sequence("Depth cam 2");
		sequenceDepth2.addImage( imageDepth2 );


		IcyBufferedImage imageIR2 = new IcyBufferedImage( FRAME_WIDTH, FRAME_HEIGHT , 1 , DataType.USHORT );
		Sequence sequenceIR2 = new Sequence("Infra cam 2");
		sequenceIR2.addImage( imageIR2 );

		IcyBufferedImage imageDepthMerge = new IcyBufferedImage( FRAME_WIDTH, FRAME_HEIGHT , 1 , DataType.FLOAT );
		Sequence sequenceDepthMerge = new Sequence("Depth merge");
		sequenceDepthMerge.addImage( imageDepthMerge );


		IcyBufferedImage imageIRMerge = new IcyBufferedImage( FRAME_WIDTH, FRAME_HEIGHT , 1 , DataType.FLOAT );
		Sequence sequenceIRMerge = new Sequence("Infra merge");
		sequenceIRMerge.addImage( imageIRMerge );

		
		Icy.getMainInterface().addSequence(sequenceDepth);
		Icy.getMainInterface().addSequence(sequenceIR);

		Icy.getMainInterface().addSequence(sequenceDepth2);
		Icy.getMainInterface().addSequence(sequenceIR2);
		
		sequenceDepth2.addROI( rectangle );
		sequenceIR2.addROI( rectangle );

		Icy.getMainInterface().addSequence(sequenceDepthMerge);
		Icy.getMainInterface().addSequence(sequenceIRMerge);



		try {
			// Connect to the pipe
			RandomAccessFile pipe = new RandomAccessFile("\\\\.\\pipe\\lmtpipe", "rw");
			boolean read = true;

			byte b1[] = new byte [ FRAME_WIDTH * FRAME_HEIGHT * BYTES_PER_PIXEL ];
			byte b2[] = new byte [ FRAME_WIDTH * FRAME_HEIGHT * BYTES_PER_PIXEL ];

			byte b3[] = new byte [ FRAME_WIDTH * FRAME_HEIGHT * BYTES_PER_PIXEL ];
			byte b4[] = new byte [ FRAME_WIDTH * FRAME_HEIGHT * BYTES_PER_PIXEL ];

			short depth[] = new short [ FRAME_WIDTH * FRAME_HEIGHT ];
			short infraRed[] = new short [ FRAME_WIDTH * FRAME_HEIGHT ];

			short depth2[] = new short [ FRAME_WIDTH * FRAME_HEIGHT ];
			short infraRed2[] = new short [ FRAME_WIDTH * FRAME_HEIGHT ];

			float imageIRMergeBuffer[] = imageIRMerge.getDataXYAsFloat( 0 );
			float imageDepthMergeBuffer[] = imageDepthMerge.getDataXYAsFloat( 0 );

			while ( read )
			{
				String echoText = "Request depth image\n";
				// write to pipe
				pipe.write ( echoText.getBytes() );
				// read response
				//String echoResponse = pipe.readLine();
				//System.out.println("Response: " + echoResponse );

				pipe.readFully( b1 ); // depth cam 1
				bin( b1,depth );
				imageDepth.setDataXY( 0, depth );

				pipe.readFully( b2 ); // infra red cam 1
				bin( b2,infraRed );
				imageIR.setDataXY( 0, infraRed );

				pipe.readFully( b3 ); // depth cam 2
				bin( b3,depth2 );
				imageDepth2.setDataXY( 0, depth2 );

				pipe.readFully( b4 ); // infra red cam 2
				bin( b4,infraRed2 );
				imageIR2.setDataXY( 0, infraRed2 );


				//performMerge( infraRed, infraRed2, imageIRMergeBuffer );
				//imageIRMerge.dataChanged();

				//Chronometer chrono = new Chronometer("Merge");
				performMerge( depth, depth2, imageDepthMergeBuffer,
						infraRed, infraRed2, imageIRMergeBuffer
						);
				//chrono.displayMs();

				imageDepthMerge.dataChanged();
				imageIRMerge.dataChanged();

//				return;
				Thread.sleep( 30 );
//				Thread.sleep( 1000 );
				frame++;
			}
			pipe.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	int frame = 0;
	int bestXOffset = 0;
	int bestYOffset = 0;

	private void performMerge( short[] depth1, short[] depth2 , float[] depthBuffer,
			short[] infra1, short[] infra2 , float[] infraBuffer
			) {

		// find horizontal and vertical shift.
		// consider no distortion.
		// use 1 as reference, and seek in second buffer to find match.

		System.out.println("Frame: " + frame );

		if ( frame % 1000 == 0 )
		{
			System.out.println("Compute correlation");
			Point2D bestOffset = null;
			int bestScore = Integer.MAX_VALUE;

			int windowHorizontal = 200; // number of pixel to seek for from center.
			int windowVertical = 20; // number of pixel to seek for from center.

			/*
			for ( int xOffset = -windowHorizontal ; xOffset <= windowHorizontal ; xOffset +=1 )
			{
				for ( int yOffset = -windowVertical ; yOffset <= windowVertical ; yOffset +=1 )
				{
					int score = correlationScore( depth1 , depth2, xOffset, yOffset );
					if ( score < bestScore )
					{
						bestScore = score;
						bestOffset = new Point2D.Double( xOffset , yOffset );
					}
				}
			}
			*/

			/*

			for ( int xOffset = -250 ; xOffset <= -100 ; xOffset +=1 )
			{
				for ( int yOffset = -windowVertical ; yOffset <= windowVertical ; yOffset +=1 )
				{
					int score = correlationScore( depth1 , depth2, xOffset, yOffset );
					if ( score < bestScore )
					{
						bestScore = score;
						bestOffset = new Point2D.Double( xOffset , yOffset );
					}
				}
			}

			rectangle.setPosition2D( bestOffset );
			bestXOffset = (int) bestOffset.getX();
			bestYOffset = (int) bestOffset.getY();
			 */

			bestXOffset = -184;
			bestYOffset = -18;


//			int bestXOffset = (int) bestOffset.getX();
//			int bestYOffset = (int) bestOffset.getY();

			/*
			System.out.println("Best offset (x,y): " + bestXOffset + " , " + bestYOffset );
			System.out.println("Best score: " + bestScore );
			*/
		}


		// merge data

		for ( int x =0 ; x < 640 ; x++ )
		{
			for ( int y=0 ; y < 576 ; y++ )
			{
				int xx = x+bestXOffset;
				int yy = y+bestYOffset;
				if ( xx >= 0 && x< 640 && yy >= 0 && yy < 575 )
				{
					int depthVal = 0;
					int depthVal1 = depth1[y*640+x] & 0xFFFF;
					int depthVal2 = depth2[yy*640+xx] & 0xFFFF;

					int infraVal = 0;
					int infraVal1 = infra1[y*640+x] & 0xFFFF;
					int infraVal2 = infra2[yy*640+xx] & 0xFFFF;


					/*
					if ( depthVal1 > depthVal2 )
					{
						depthVal = depthVal1;
					}else
					{
						depthVal = depthVal2;
					}
					*/

					depthVal = ( depthVal1 + depthVal2 ) /2;
					infraVal = ( infraVal1 + infraVal2 ) /2;

					/*
					if ( x > 320 )
					{
						depthVal = depthVal1;
					}else
					{
						depthVal = depthVal2;
					}
					*/

					/*
					{
						if ( infraVal1 > infraVal2 )
						{
							infraVal = infraVal1;
						}else
						{
							infraVal = infraVal2;
						}
					}

					if ( depthVal1 < 1000 )
					{
						infraVal = infraVal2;
					}

					if ( depthVal2 < 1000 )
					{
						infraVal = infraVal1;
					}

					// saturation in infra.
					if ( infraVal1 > 13000 )
					{
						infraVal = infraVal2;
					}
					if ( infraVal2 > 13000 )
					{
						infraVal = infraVal1;
					}
					*/

					//val = (val1+val2)/2;

					depthBuffer[y*640+x] = depthVal;
					infraBuffer[y*640+x] = infraVal;
				}
			}
		}

	}

	/*
	private void performMerge( short[] image1, short[] image2 , float[] buffer ) {

		// find horizontal and vertical shift.
		// consider no distortion.
		// use 1 as reference, and seek in second buffer to find match.

		Point2D bestOffset = null;
		int bestScore = Integer.MAX_VALUE;

		int window = 100; // number of pixel to seek for from center.

		for ( int xOffset = -window ; xOffset <= window ; xOffset +=1 )
		{
			for ( int yOffset = -window ; yOffset <= window ; yOffset +=1 )
			{
				int score = correlationScore( image1 , image2, xOffset, yOffset );
				if ( score < bestScore )
				{
					bestScore = score;
					bestOffset = new Point2D.Double( xOffset , yOffset );
				}
			}
		}
		rectangle.setPosition2D( bestOffset );
		int bestXOffset = (int) bestOffset.getX();
		int bestYOffset = (int) bestOffset.getY();

		// merge data

		for ( int x =0 ; x < 640 ; x++ )
		{
			for ( int y=0 ; y < 576 ; y++ )
			{
				int xx = x+bestXOffset;
				int yy = y+bestYOffset;
				if ( xx >= 0 && x< 640 && yy >= 0 && yy < 575 )
				{
					int val = 0;
					int val1 = image1[y*640+x] & 0xFFFF;
					int val2 = image2[yy*640+xx] & 0xFFFF;
					val = (val1+val2)/2;
					buffer[y*640+x] = val;
				}
			}
		}

	}
	*/

	private int correlationScore(short[] depth1, short[] depth2, int xOffset, int yOffset) {

		int score = 0;
		int sizeOfWindow= 100;
		int centerX = 640/2;
		int centerY = 576/2;
		for ( int y = centerY-sizeOfWindow ; y < centerY+sizeOfWindow ; y+=1 )
		{
			for ( int x = centerX-sizeOfWindow ; x < centerX+sizeOfWindow ; x+=1 )
			{
				int depth1Val= depth1[ y *640 + x ] & 0xFFFF;
				int depth2Val= depth2[( y + yOffset )* 640 + ( x + xOffset ) ] & 0xFFFF;
				score+= Math.abs( depth1Val-depth2Val );
			}
		}
		return score;
	}

	private void bin(byte[] in, short[] out) {

		int c=0;
		for ( int i = 0 ; i < in.length ; i+=2  )
		{
			out[c] = (short)(( in[i+1] & 0xFF ) *255 + ( in[i] & 0xFF ));
			c++;
		}

	}




}
