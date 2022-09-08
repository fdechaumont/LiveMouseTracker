package plugins.fab.azure.kinect;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.vecmath.Point3f;

import icy.canvas.IcyCanvas;
import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.painter.Overlay;
import icy.plugin.abstract_.Plugin;
import icy.plugin.abstract_.PluginActionable;
import icy.plugin.interface_.PluginDaemon;
import icy.plugin.interface_.PluginThreaded;
import icy.sequence.Sequence;
import icy.system.thread.ThreadUtil;
import icy.type.DataType;
import icy.type.point.Point5D.Double;
import plugins.fab.azure.kinect.TestAzureKinectDriverFabMultiDoubleCam.CameraLMT;
import plugins.fab.kinectdriver.KinectData;
import plugins.fab.kinectdriver.KinectEvent;
import plugins.fab.kinectdriver.KinectListener;
import plugins.fab.livemousetracker.ImageKinect;

public class TestAzureKinectDriverFabMultiDoubleCam extends PluginActionable implements DatasetReadyListener
{
    final static String host = "127.0.0.1";
    final static int port = 4444;

    private AzureKinectDriverClient client;
    private boolean stopped;

    private final List<Dataset> receivedDatasets;

    private final List<KinectListener> kinectListenerList = new ArrayList<KinectListener>();
    
    private TestAzureKinectDriverFabMultiDoubleCamOverlay overlay;
    
    /*
    private final Sequence depth1;
    private final Sequence depth2;
    private final Sequence ir1;
    private final Sequence ir2;
    */
//    private final Sequence depth1;
//    private final Sequence depth2;
//    private final Sequence ir1;
//    private final Sequence ir2;
    
    private final Sequence depthMergeSequence;
    private final Sequence infraMergeSequence;

    public CameraLMT cameraK1 = new CameraLMT("K1");
    public CameraLMT cameraK2 = new CameraLMT("K2");
    
    class CameraLMT
	{
    	String name;
    	int x = 0;
    	int y = 0;
    	int cropX = 0;
    	int cropY = 0;
    	int cropWidth = 640;
    	int cropHeight = 424;
    	
    	public CameraLMT( String name )
    	{
    		this.name = name;
    	}
    	@Override
    	public String toString() {
    		return name + " x:" + x + " y:" +y + " cropX: "+ cropX + " cropY:" + cropY + " cropW: " + cropWidth + " cropH:" + cropHeight;
    	}
    }
    
    class TestAzureKinectDriverFabMultiDoubleCamOverlay extends Overlay
    {

    	CameraLMT camera = null;
		public TestAzureKinectDriverFabMultiDoubleCamOverlay() {
			super("Overlay Kinect Streamer");
			camera = cameraK1;
			this.setReceiveKeyEventOnHidden( true );
		}
		
		
		
		@Override
		public void keyPressed(KeyEvent e, Double imagePoint, IcyCanvas canvas) {
			
			//System.out.println( e );
			
			if ( e.getKeyChar() == '1' )
			{				
				camera = cameraK1;
				System.out.println("Active camera: " + camera );
			}

			if ( e.getKeyChar() == '2' )
			{				
				camera = cameraK2;
				System.out.println("Active camera: " + camera );
			}
				
			if ( e.getKeyChar() == 'x' )
			{
				camera.x--;
			}
			if ( e.getKeyChar() == 'X' )
			{
				camera.x++;
			}
			
			
			if ( e.getKeyChar() == 'y' )
			{
				camera.y--;
			}
			if ( e.getKeyChar() == 'Y' )
			{
				camera.y++;
			}
			
			
			
			if ( e.getKeyChar() == 'b' )
			{
				camera.cropX--;
			}
			if ( e.getKeyChar() == 'B' )
			{
				camera.cropX++;
			}
			
			if ( e.getKeyChar() == 'n' )
			{
				camera.cropY--;
			}
			if ( e.getKeyChar() == 'N' )
			{
				camera.cropY++;				
			}
			
			if ( e.getKeyChar() == 'w' )
			{
				camera.cropWidth--;
			}
			if ( e.getKeyChar() == 'W' )
			{
				camera.cropWidth++;				
			}
			
			
			if ( e.getKeyChar() == 'h' )
			{
				camera.cropHeight--;
			}
			if ( e.getKeyChar() == 'H' )
			{
				camera.cropHeight++;				
			}
			
			System.out.println("---");
			System.out.println( cameraK1 );
			System.out.println( cameraK2 );
			e.consume();
			//super.keyPressed(e, imagePoint, canvas);
			
		}
    	
    }

    public TestAzureKinectDriverFabMultiDoubleCam()
    {
        super();

        client = null;
        stopped = true;

        receivedDatasets = new ArrayList<>();



//        depth1 = new Sequence("depth1");
//        depth2 = new Sequence("depth2");
//        ir1 = new Sequence("ir1");
//        ir2 = new Sequence("ir2");
        
        depthMergeSequence = new Sequence("depthMerge");
        infraMergeSequence = new Sequence("infraMerge");
        
        cameraK1.x = -60;
        cameraK1.y = -75;
        cameraK1.cropX = 0;
        cameraK1.cropY = 0;
        cameraK1.cropWidth = 587;
        cameraK1.cropHeight = 491;
        
        cameraK2.x = 487;
        cameraK2.y = -68;
        cameraK2.cropX = 44;
        cameraK2.cropY = 0;
        cameraK2.cropWidth = 511;
        cameraK2.cropHeight = 484;

        
        
//        K1 x:-60 y:-75 cropX: 0 cropY:0 cropW: 587 cropH:491
//        K2 x:487 y:-68 cropX: 44 cropY:0 cropW: 510 cropH:484


    }
    
	int maxMergeX = 0;
	int maxMergeY = 0;
	

    private ImageKinect mergeKinectImage( ArrayList<ImageKinect> kinectImageArrayList )
	{

		for ( ImageKinect imageKinect : kinectImageArrayList )
		{
//			System.out.println("---------------------------------- Merge data: ");
//			System.out.println("w infra:" + imageKinect.infraImage.getWidth() );
//			System.out.println("w depth:" + imageKinect.depthImage.getWidth() );
//			System.out.println("h infra:" + imageKinect.infraImage.getHeight() );
//			System.out.println("h depth:" + imageKinect.depthImage.getHeight() );
			
			int x = 0;
			int y = 0;
			if ( imageKinect.cropRect == null )
			{
				x = imageKinect.offsetX+imageKinect.infraImage.getWidth();
				y = imageKinect.offsetY+imageKinect.infraImage.getHeight();
			}else
			{
				x = imageKinect.offsetX+imageKinect.cropRect.width;
				y = imageKinect.offsetY+imageKinect.cropRect.height;
			}
			
			if ( x > maxMergeX )
			{
				maxMergeX = x;
			}
			if ( y > maxMergeY )
			{
				maxMergeY = y;
			}
		}

		
		IcyBufferedImage infraImage = new IcyBufferedImage( maxMergeX, maxMergeY, 1 , DataType.USHORT );
		IcyBufferedImage depthImage = new IcyBufferedImage( maxMergeX, maxMergeY, 1 , DataType.USHORT );


		for ( ImageKinect imageKinect : kinectImageArrayList )
		{
			infraImage.copyData( imageKinect.infraImage, imageKinect.cropRect, new java.awt.Point( imageKinect.offsetX, imageKinect.offsetY ) );
			depthImage.copyData( imageKinect.depthImage, imageKinect.cropRect, new java.awt.Point( imageKinect.offsetX, imageKinect.offsetY ) );
		}

		ImageKinect merged = new ImageKinect(infraImage, depthImage, 0,0 );
		return merged;
	}
    
    public void init()
    {
    	
        receivedDatasets.clear();
        stopped = false;

        client = new AzureKinectDriverClient(host, port, 
        		AzureKinectDriverClient.DEPTH_IMAGE | AzureKinectDriverClient.DEPTH_CLOUD_IMAGE | AzureKinectDriverClient.IR_IMAGE );

        client.addListener(this);
        
        this.overlay = new TestAzureKinectDriverFabMultiDoubleCamOverlay();

    }

    Thread captureThread = null;
    
    @Override
    public void run()
    {
    	init();
    	fireEvent( depthMergeSequence , null, KinectEvent.NEW_DEPTH_SEQUENCE );
		fireEvent( infraMergeSequence , null, KinectEvent.NEW_INFRARED_SEQUENCE );
    
    	Runnable runCode = new Runnable() {
			
			@Override
			public void run() {
				
				while (!stopped && !Thread.interrupted() && ! Icy.isExiting())
				{
					try
					{
						// sleep a bit
						Thread.sleep(1);
						
						final List<Dataset> datasets;
						
						synchronized (receivedDatasets)
						{
							datasets = new ArrayList<>(receivedDatasets);
							receivedDatasets.clear();
						}
						
						// we received datasets from client ?
						if (!datasets.isEmpty())
						{
							IcyBufferedImage img;
							int ind = 0;

							ArrayList<ImageKinect> kinectImageArrayList = new ArrayList<ImageKinect>();
							
							ImageKinect k1 = new ImageKinect( null, null, cameraK1.x, cameraK1.y );
							ImageKinect k2 = new ImageKinect( null, null, cameraK2.x, cameraK2.y ); // 87 : decalge en x
							//ImageKinect k2 = new ImageKinect( null, null, 512, 0 );
							
							//ImageKinect k2 = new ImageKinect( null, null, 512+32, 0 );
							
							// azure kinect resolution: 640x576
							
							//k1.cropRect = new Rectangle( (640-512)/2,(576-424)/2 ,512,424); // center a crop at the size of the previous kinect
							//k2.cropRect = new Rectangle( (640-512)/2,(576-424)/2 ,512,424);
							
							//k1.cropRect = new Rectangle( 0,0 ,560, 424 );							
//							k2.cropRect = new Rectangle( (640-512)/2,(576-424)/2 ,640, 424 );
							
//							k1.cropRect = null;
//							k2.cropRect = null;
							
							k1.cropRect = new Rectangle( cameraK1.cropX, cameraK1.cropY , cameraK1.cropWidth, cameraK1.cropHeight );
							k2.cropRect = new Rectangle( cameraK2.cropX, cameraK2.cropY , cameraK2.cropWidth, cameraK2.cropHeight );
							
							// convert and show image
							for (Dataset dataset : datasets)
							{
		                        img = Utils.toIcyShortImage(dataset.depthImage );
		                        //System.out.println("Depth: w:" + img.getWidth() +" ind: " + ind );
		                        if (ind==0)
		                        {
		                        	k1.depthImage = img;
//		                        	depth1.setImage(0, 0, img);
		                        }
		                        if (ind==1)
		                        {
		                        	k2.depthImage = img;
		                        	//depth2.setImage(0, 0, img);
		                        }
		                        
		                        //System.out.println("Infra: w:" + img.getWidth() +" ind: " + ind );
		                        img = Utils.toIcyShortImage(dataset.irImage );
		                        if (ind==0)
		                        {
		                        	k1.infraImage=img;
		                        	//ir1.setImage(0, 0, img);
		                        }
		                        if (ind==1)
		                        {
		                        	k2.infraImage = img;
		                        	//ir2.setImage(0, 0, img);
		                        }
		                        

								ind++;
							}
							
							// merging sources
							kinectImageArrayList.add( k1 );
							kinectImageArrayList.add( k2 );
							ImageKinect kinectImageMerged = mergeKinectImage( kinectImageArrayList );
							infraMergeSequence.setImage( 0 , 0, kinectImageMerged.infraImage );
							depthMergeSequence.setImage( 0 , 0, kinectImageMerged.depthImage );
							
							fireEvent( infraMergeSequence , null, KinectEvent.NEW_INFRARED_CAPTURE );
						}
					}
					catch (InterruptedException e)
					{
						stopped = true;
					}
				}
			}
		};
		
		
		
		captureThread = new Thread( runCode );
		captureThread.setPriority( Thread.MAX_PRIORITY );
		captureThread.start();
		
		
    }
    
    public void startLive()
    {
    	run();
    	// implicit
    }

    public void stopLive()
    {
    	stop();
    }

    public void stop()
    {
        stopped = true;

        //Icy.getMainInterface().closeSequence(color);
        //Icy.getMainInterface().closeSequence(depth);
        //Icy.getMainInterface().closeSequence(strechedDepth);
        //Icy.getMainInterface().closeSequence(cloudDepth);

        if (client != null)
        {
            try
            {
                client.close();
            }
            catch (InterruptedException e)
            {
                // ignore
            }

            client = null;
        }
    }

    @Override
    public void DatasetReceived(List<Dataset> datasets)
    {
        synchronized (receivedDatasets)
        {
            receivedDatasets.clear();
            receivedDatasets.addAll(datasets);
        }
    }
    
    

    public void addKinectListener(KinectListener kinectListener)
    {
        kinectListenerList.add(kinectListener);
    }

    public void removeKinectListener(KinectListener kinectListener)
    {
        kinectListenerList.remove(kinectListener);
    }

    public void removeAllKinectListener()
    {
        kinectListenerList.clear();
    }
    private void fireEvent(Sequence sourceSequence, KinectData kinectData, KinectEvent kinectEvent)
    {
        for (KinectListener kl : kinectListenerList)
        {
            kl.kinectChange(sourceSequence, kinectData, kinectEvent);
        }

    }

	public void setSequenceForOverlay(Sequence seq ) {

		seq.addOverlay( this.overlay );

	}

    
}