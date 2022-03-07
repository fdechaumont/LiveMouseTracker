package plugins.fab.azure.kinect;

import java.util.ArrayList;
import java.util.List;

import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.plugin.abstract_.Plugin;
import icy.plugin.abstract_.PluginActionable;
import icy.plugin.interface_.PluginDaemon;
import icy.plugin.interface_.PluginThreaded;
import icy.sequence.Sequence;

public class TestAzureKinectDriverFab extends PluginActionable implements PluginThreaded,DatasetReadyListener
{
    final static String host = "127.0.0.1";
    final static int port = 4444;

    private AzureKinectDriverClient client;
    private boolean stopped;

    private final List<Dataset> receivedDatasets;

//    private final Sequence color;
    private final Sequence depth1;
    private final Sequence depth2;
//    private final Sequence strechedDepth;
//    private final Sequence cloudDepth;

    public TestAzureKinectDriverFab()
    {
        super();

        client = null;
        stopped = true;

        receivedDatasets = new ArrayList<>();

//        color = new Sequence("color");
        depth1 = new Sequence("depth1");
        depth2 = new Sequence("depth2");
//        strechedDepth = new Sequence("streched depth");
//        cloudDepth = new Sequence("cloud depth");
    }
    
    public void init()
    {
        receivedDatasets.clear();
        stopped = false;

        client = new AzureKinectDriverClient(host, port, AzureKinectDriverClient.DEPTH_IMAGE
                        | AzureKinectDriverClient.DEPTH_CLOUD_IMAGE
        		);
        client.addListener(this);

//        Icy.getMainInterface().addSequence(color);
        Icy.getMainInterface().addSequence(depth1);
        Icy.getMainInterface().addSequence(depth2);
//        Icy.getMainInterface().addSequence(strechedDepth);
//        Icy.getMainInterface().addSequence(cloudDepth);
    }

    @Override
    public void run()
    {
    	init();
    	
        while (!stopped && !Thread.interrupted())
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
//                        img = Utils.toIcyByteImage(dataset.color_image);
//                        if (img != null)
//                            color.setImage(ind, 0, img);
                        img = Utils.toIcyShortImage(dataset.depthImage );
                        if (ind==0)
                        {
                        	depth1.setImage(0, 0, img);
                        }
                        if (ind==1)
                        {
                        	depth2.setImage(0, 0, img);
                        }
                        		
//                        if (img != null)
//                            depth.setImage(ind, 0, img);
//                        img = Utils.toIcyShortImage(dataset.streched_depth_image);
//                        if (img != null)
//                            strechedDepth.setImage(ind, 0, img);
//                        img = Utils.toIcyShortImage(dataset.cloud_depth_image);
//                        if (img != null)
//                            cloudDepth.setImage(ind, 0, img);
                        ind++;
                    }
                }
            }
            catch (InterruptedException e)
            {
                stopped = true;
            }
        }
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
}