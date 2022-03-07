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

public class KinectDK_Double_Display extends PluginActionable implements PluginThreaded {

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


        Icy.getMainInterface().addSequence(sequenceDepth);
        //Icy.getMainInterface().addSequence(sequenceIR);

        Icy.getMainInterface().addSequence(sequenceDepth2);
        //Icy.getMainInterface().addSequence(sequenceIR2);


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

            //byte readAllBuffer[] = new byte[ FRAME_WIDTH * FRAME_HEIGHT * BYTES_PER_PIXEL ];

            while ( read )
            {
                String echoText = "Request depth image\n";
                // write to pipe
                pipe.write ( echoText.getBytes() );
                // read response
                //String echoResponse = pipe.readLine();
                //System.out.println("Response: " + echoResponse );

//                System.out.println( "reading...");
//                System.out.println( pipe.length() );
//
//                if ( pipe.length() != 2949120 )
//                {
//                    System.out.println("Data size different from expected. Flushing.");
//                    while ( pipe.length() > 0 )
//                    {
//                    	pipe.readFully( readAllBuffer );
//                    }
//                }
//                else
                {
                	// processing buffer

                	pipe.readFully( b1 ); // depth cam 1
                	bin( b1,depth );
                	imageDepth.setDataXY( 0, depth );

                	//System.out.println( pipe.length() );

                	pipe.readFully( b2 ); // infra red cam 1
                	bin( b2,infraRed );
                	imageIR.setDataXY( 0, infraRed );

                	//System.out.println( pipe.length() );

                	pipe.readFully( b3 ); // depth cam 2
                	bin( b3,depth2 );
                	imageDepth2.setDataXY( 0, depth2 );

                	//System.out.println( pipe.length() );

                	pipe.readFully( b4 ); // infra red cam 2
                	bin( b4,infraRed2 );
                	imageIR2.setDataXY( 0, infraRed2 );

                	//System.out.println( pipe.length() );
                }

                Thread.sleep( 30 );

                //Thread.sleep( (int)(Math.random() * 100 ) );

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
