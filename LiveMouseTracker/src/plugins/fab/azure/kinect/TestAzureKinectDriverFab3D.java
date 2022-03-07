package plugins.fab.azure.kinect;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.vecmath.Point3f;

import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.plugin.abstract_.Plugin;
import icy.plugin.abstract_.PluginActionable;
import icy.plugin.interface_.PluginDaemon;
import icy.plugin.interface_.PluginThreaded;
import icy.sequence.Sequence;
import icy.system.thread.ThreadUtil;
import plugins.fab.kinectdriver.KinectData;
import plugins.fab.kinectdriver.KinectEvent;
import plugins.fab.kinectdriver.KinectListener;

public class TestAzureKinectDriverFab3D extends PluginActionable implements DatasetReadyListener
{
    final static String host = "127.0.0.1";
    final static int port = 4444;

    private AzureKinectDriverClient client;
    private boolean stopped;

    private final List<Dataset> receivedDatasets;

    private final List<KinectListener> kinectListenerList = new ArrayList<KinectListener>();
    
    /*
    private final Sequence depth1;
    private final Sequence depth2;
    private final Sequence ir1;
    private final Sequence ir2;
    */

    public TestAzureKinectDriverFab3D()
    {
        super();

        client = null;
        stopped = true;

        receivedDatasets = new ArrayList<>();

/*
        depth1 = new Sequence("depth1");
        depth2 = new Sequence("depth2");
        ir1 = new Sequence("ir1");
        ir2 = new Sequence("ir2");
        */

    }
    
    Rendering3Dv3 rendering3D = null;
    
    public void init()
    {
    	rendering3D = new Rendering3Dv3();
    	
        receivedDatasets.clear();
        stopped = false;

        client = new AzureKinectDriverClient(host, port, 
        		AzureKinectDriverClient.DEPTH_IMAGE | AzureKinectDriverClient.DEPTH_CLOUD_IMAGE | AzureKinectDriverClient.IR_IMAGE );
//        
//        client = new AzureKinectDriverClient(host, port,
//                AzureKinectDriverClient.COLOR_IMAGE | AzureKinectDriverClient.DEPTH_IMAGE
//                        | AzureKinectDriverClient.DEPTH_EXT_IMAGE | AzureKinectDriverClient.DEPTH_CLOUD_IMAGE
//                        | AzureKinectDriverClient.IR_IMAGE | AzureKinectDriverClient.IMU_SAMPLE);
        client.addListener(this);

//        Icy.getMainInterface().addSequence(color);
        /*
        Icy.getMainInterface().addSequence(depth1);
        Icy.getMainInterface().addSequence(depth2);
        Icy.getMainInterface().addSequence(ir1);
        Icy.getMainInterface().addSequence(ir2);
        */
//        Icy.getMainInterface().addSequence(strechedDepth);
//        Icy.getMainInterface().addSequence(cloudDepth);
        
        /*
    	JFrame frame = new JFrame();
        frame.add(new JScrollPane( rendering3D ) );
        frame.setSize(1000, 1000);        
        frame.setVisible(true);
        frame.setLocationRelativeTo( null );
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        */

    }

    Thread captureThread = null;
    
    @Override
    public void run()
    {
    	init();
    	fireEvent( rendering3D.depthSequence , null, KinectEvent.NEW_DEPTH_SEQUENCE );
		fireEvent( rendering3D.infraSequence , null, KinectEvent.NEW_INFRARED_SEQUENCE );
    
    	Runnable runCode = new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				
				
				
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

							// convert and show image
							for (Dataset dataset : datasets)
							{
/*
								img = Utils.toIcyShortImage(dataset.depthImage);
								if (ind==0)
								{
									depth1.setImage(0, 0, img);
								}
								if (ind==1)
								{
									depth2.setImage(0, 0, img);
								}
								img = Utils.toIcyShortImage(dataset.irImage);
								if (ind==0)
								{
									ir1.setImage(0, 0, img);
								}
								if (ind==1)
								{
									ir2.setImage(0, 0, img);
								}
								*/
//								368640
//								1105920
								//if (ind==0)
								{
									//img = Utils.toIcyShortImage(dataset.cloudDepthImage); // todo remove
									//if (img != null)
									{
										// DRAW 3D Stuff here
										ArrayList<Point3f> pointList = new ArrayList();
										ArrayList<Integer> colorList = new ArrayList();
										if ( dataset.cloudDepthImage.data.length != 1105920 )
										{
											System.out.println("Kinect DK: not receiving all data (cloud)");
											continue;
										}
										if ( dataset.irImage.data.length != 368640 )
										{
											System.out.println("Kinect DK: not receiving all data (infrared)");
											continue;
										}

										/*
										short[] dataPoint = dataset.cloudDepthImage.data;
										//System.out.println( dataset.cloudDepthImage.data.length );
										short[] dataIR = dataset.irImage.data;
										//System.out.println( dataset.irImage.data.length );
										int imageSeeker= 0;										
										for ( int i = 0 ; i < dataPoint.length; i +=3 )									
										{
											
											pointList.add( new Point3f( dataPoint[i], dataPoint[i+1], dataPoint[i+2] ) );											
											colorList.add( dataIR[imageSeeker] & 0xFFFF );
											imageSeeker++;
										}
										rendering3D.setPoints( ind, pointList, colorList );
										 */
										rendering3D.setPoints( ind, dataset.cloudDepthImage.data, dataset.irImage.data );
										
									}
								}
								
								//                            cloudDepth.setImage(ind, 0, img);
								ind++;
							}
							
							// channel 3 cage.
							{
								ArrayList<Point3f> pointList = new ArrayList();
								for ( int x = - 250 ; x <= 250 ; x+= 10)
								{
									for ( int y = - 250 ; y <= 250 ; y+= 10)
									{
										pointList.add( new Point3f( x , y, 630 ) );
									}
								}
								for ( int z = 0 ; z <= 630 ; z+= 10)
								{
									pointList.add( new Point3f( 0 , 0, z ) );
								}
								
								short[] pointArray = new short[pointList.size()*3];
								short[] colorArray = new short[pointList.size()];
								for ( int i=0 ; i< pointList.size() ; i++ )
								{
									pointArray[i*3+0] = (short)pointList.get( i ).x;
									pointArray[i*3+1] = (short)pointList.get( i ).y;
									pointArray[i*3+2] = (short)pointList.get( i ).z;
									colorArray[i] = 4000;
								}
								
								rendering3D.setPoints(2 , pointArray , colorArray );
							}
							
							
							//System.out.println( "render");
							rendering3D.render();
							//fireEvent( rendering3D.depthSequence , null, KinectEvent.NEW_DEPTH_CAPTURE ); // mimic depth before infra as in Kinect2 behavior
							fireEvent( rendering3D.infraSequence , null, KinectEvent.NEW_INFRARED_CAPTURE );
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
		
		//ThreadUtil.bgRun(runCode);
		
		
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

    
}