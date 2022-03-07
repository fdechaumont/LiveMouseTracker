package plugins.fab.aaa.kinectDKTest;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;
import plugins.fab.aaa.voc.VocAnalysisListener;

public class KinectDK_Single extends PluginActionable implements PluginThreaded {

	enum DEPTH_MODE {
		K4A_DEPTH_MODE_NFOV_UNBINNED_640x576,
		K4A_DEPTH_MODE_NFOV_BINNED_512x512,
		K4A_DEPTH_MODE_WFOV_UNBINNED_1024x1024,
		K4A_DEPTH_MODE_WFOV_BINNED_512x512
		};

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
		Sequence sequenceDepth = new Sequence("Depth");
		sequenceDepth.addImage( imageDepth );


		IcyBufferedImage imageIR = new IcyBufferedImage( FRAME_WIDTH, FRAME_HEIGHT , 1 , DataType.USHORT );
		Sequence sequenceIR = new Sequence("Infra");
		sequenceIR.addImage( imageIR );


		Icy.getMainInterface().addSequence(sequenceDepth);
		Icy.getMainInterface().addSequence(sequenceIR);



		try {
			// Connect to the pipe
			RandomAccessFile pipe = new RandomAccessFile("\\\\.\\pipe\\lmtpipe", "rw");
			boolean read = true;

			byte b1[] = new byte [ FRAME_WIDTH * FRAME_HEIGHT * BYTES_PER_PIXEL ];
			byte b2[] = new byte [ FRAME_WIDTH * FRAME_HEIGHT * BYTES_PER_PIXEL ];

			short depth[] = new short [ FRAME_WIDTH * FRAME_HEIGHT ];
			short infraRed[] = new short [ FRAME_WIDTH * FRAME_HEIGHT ];


			while ( read )
			{
				String echoText = "Request depth image\n";
				// write to pipe
				pipe.write ( echoText.getBytes() );
				// read response
				//String echoResponse = pipe.readLine();
				//System.out.println("Response: " + echoResponse );

				pipe.readFully( b1 ); // depth
				bin( b1,depth );
				imageDepth.setDataXY( 0, depth );

				pipe.readFully( b2 ); // infra red
				bin( b2,infraRed );
				imageIR.setDataXY( 0, infraRed );

				Thread.sleep( 30 );

			}
			pipe.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

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
