package plugins.fab.kinectdriver;

import java.io.File;

import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.sequence.Sequence;

/** 
 * This class performs the record of a kinect that it is listening to.
 * 
 * */
public class KinectLiveRecorder implements KinectListener {

	Sequence infraOutLive = null;
	Sequence depthOutLive = null;

	String infraBaseFileName ;
	String depthBaseFileName ;
	String recordingFolder ;
	String baseFolder = "c://kinect record//";
	
	public KinectLiveRecorder() {
		
		boolean fileNameOk = false;
		String seqId ="";
		while ( !fileNameOk )
		{
			seqId = NumberFormat.format( (int)(Math.random()*1000d) );					
			fileNameOk = !icy.file.FileUtil.exists( baseFolder + seqId );						
		}
		
		infraBaseFileName = baseFolder+seqId+"/infra/infra" ;
		depthBaseFileName = baseFolder+seqId+"/depth/depth" ;
		recordingFolder = baseFolder+seqId;
	}
	
	public KinectLiveRecorder( String folderName ) {

		this();
		// Force the name given.
		infraBaseFileName = baseFolder+folderName+"/infra/infra_t" ;
		depthBaseFileName = baseFolder+folderName+"/depth/depth_t" ;
		recordingFolder = baseFolder+folderName;		
	}
	
	int t = 0;
	
	@Override
	public void kinectChange(Sequence sourceSequence, KinectData kinectData,
			KinectEvent kinectEvent) {

		if ( kinectEvent == KinectEvent.NEW_DEPTH_CAPTURE )
		{
			// DO NOT PROCESS ON DEPTH EVENT.
			// THE DEPTH EVENT OCCURS BEFORE INFRARED MAP IS READY
			// IF YOU DO SO, IMAGE WILL BE SHIFTED IN TIME (INFRA AND DEPTH WILL BE UNSYNC)
		}
		
		if ( kinectEvent == KinectEvent.NEW_INFRARED_CAPTURE )
		{					
			if ( depthOutLive == null || infraOutLive == null ) return;
			
			IcyBufferedImage infraImage = IcyBufferedImageUtil.getCopy( infraOutLive.getFirstImage() );
			IcyBufferedImage depthImage = IcyBufferedImageUtil.getCopy( depthOutLive.getFirstImage() );
					
			
			//nbFrameLabel.setText( NumberFormat.format( t ) );

			//if ( recordCheckBox.isSelected() )
			{	
				File infraFileWithNumber = new File ( infraBaseFileName + NumberFormat.format( t ) + ".png" );
				
				//System.out.println( infraFileWithNumber );
				
				PNGEncoderThreaded petInfra = new PNGEncoderThreaded( 
						infraImage , infraFileWithNumber );

				petInfra.start();
				
				File depthFileWithNumber = new File ( depthBaseFileName + NumberFormat.format( t ) + ".png" );
				PNGEncoderThreaded petDepth = new PNGEncoderThreaded( 
						depthImage , depthFileWithNumber );

				petDepth.start();				
			}
			t++;
			
		}
		
		if ( kinectEvent == KinectEvent.NEW_DEPTH_SEQUENCE )
		{
			depthOutLive = sourceSequence;			
			depthOutLive.setName( infraBaseFileName );
			
			//if ( ! recordCheckBox.isSelected() )
			{
				// addSequence( depthOutLive );
			}
		}

		if ( kinectEvent == KinectEvent.NEW_INFRARED_SEQUENCE )
		{
			infraOutLive = sourceSequence;
			depthOutLive.setName( depthBaseFileName );
			
			//if ( ! recordCheckBox.isSelected() )
			{				
				// addSequence( infraOutLive );
			}
		}
		
	}

	public String getRecordingFolder() {
		return recordingFolder;
	}

	
	
}
