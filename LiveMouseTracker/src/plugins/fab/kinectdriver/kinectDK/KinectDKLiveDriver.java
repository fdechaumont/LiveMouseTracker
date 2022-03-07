package plugins.fab.kinectdriver.kinectDK;

import java.util.ArrayList;
import java.util.List;

import edu.ufl.digitalworlds.j4k.J4KSDK;
import icy.image.IcyBufferedImage;
import icy.plugin.abstract_.Plugin;
import icy.sequence.Sequence;
import icy.system.SystemUtil;
import icy.type.DataType;
import plugins.fab.kinectdriver.KinectData;
import plugins.fab.kinectdriver.KinectEvent;
import plugins.fab.kinectdriver.KinectListener;

/** @deprecated test */
public class KinectDKLiveDriver
{

    int counter;
    long time;

    Sequence colorOut;
    Sequence infraOut;
    Sequence depthOut;
    Sequence uvOut;

    byte[] r; // = new byte[1920*1080];
    byte[] g; // = new byte[1920*1080];
    byte[] b; // = new byte[1920*1080];

    float u[];
    float v[];

    short depth_frame[];
    short infra_frame[];

    boolean displayInfrared;
    boolean displayColorFrame;
    boolean displayDepthMap;

    private final List<KinectListener> kinectListenerList;

    private KinectDKLiveDriver()
    {
        super();

        // default
        counter = 0;
        time = 0;
        displayInfrared = true;
        displayColorFrame = true;
        displayDepthMap = true;
        colorOut = null;
        infraOut = null;

        kinectListenerList = new ArrayList<>();


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



    /*
    private void flip(short[] buffer)
    {

        int width = 512;
        int height = 424;

        short[] tmpLineBuffer = new short[width];
        int startLineIndex = 0;
        for (int y = 0; y < height; y++)
        {
            // copy original line
            // FIXED BY STEF
            System.arraycopy(buffer, startLineIndex, tmpLineBuffer, 0, width);
            // for ( int x = 0 ; x < width ; x++ )
            // {
            // tmpLineBuffer[x] = buffer[startLineIndex + x];
            // }

            // recopy flip
            for (int x = 0, xx = width - 1; x < width; x++, xx--)
            {
                buffer[startLineIndex + x] = tmpLineBuffer[xx];
            }
            startLineIndex += width;
        }

    }
    */

    
    
    
    private void fireEvent(Sequence sourceSequence, KinectData kinectData, KinectEvent kinectEvent)
    {
        for (KinectListener kl : kinectListenerList)
        {
            kl.kinectChange(sourceSequence, kinectData, kinectEvent);
        }

    }

    /*
    @Override
    public void onDepthFrameEvent(short[] depth_frame, byte[] body_index, float[] xyz, float[] uv)
    {

        if (!displayDepthMap)
            return;

        //flip(depth_frame);
        this.depth_frame = depth_frame;


        KinectData kinectData = null; // new KinectData( xyz , uv );

        if (depthOut == null)
        {
            depthOut = new Sequence();
            depthOut.setName("Depth map");
            depthOut.setAutoUpdateChannelBounds(false);
            depthOut.addImage(new IcyBufferedImage(getDepthWidth(), getDepthHeight(), 1, DataType.USHORT));

            depthOut.getImage(0, 0).setAutoUpdateChannelBounds(false);

            fireEvent(depthOut, kinectData, KinectEvent.NEW_DEPTH_SEQUENCE);

            // Icy.getMainInterface().addSequence(depthOut);
        }

        depthOut.getImage(0, 0).setDataXY(0, depth_frame);
        fireEvent(depthOut, kinectData, KinectEvent.NEW_DEPTH_CAPTURE);
    }
    */

    /*
    private void compensateZIntensityError(short[] bufferDepth, short[] bufferInfra)
    {

        if (bufferDepth == null || bufferInfra == null)
        {
            System.out.println("[kinect Driver] Can't compensate Z");
            return;
        }

        for (int i = 0; i < bufferInfra.length; i++)
        {
            short infra = bufferInfra[i];
            // z-compensation working with black and white animal (fur)
            float correction = -(23000 - infra) / 1000f;
            // z-compensation with a paper mire (not working with fur)
            // float correction = -2.5f * ( infra / 1000f );
            bufferDepth[i] += (short) correction;
        }

    }
    */
    
    /*
    public void displayInfrared(boolean selected)
    {
        displayInfrared = selected;
    }

    public void displayColorFrame(boolean selected)
    {
        displayColorFrame = selected;
    }

    public void displayDepthMap(boolean selected)
    {
        displayDepthMap = selected;
    }
    */

}
