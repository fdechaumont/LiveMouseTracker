package plugins.fab.kinectdriver;

import java.util.ArrayList;
import java.util.List;

import edu.ufl.digitalworlds.j4k.J4KSDK;
import icy.image.IcyBufferedImage;
import icy.plugin.abstract_.Plugin;
import icy.sequence.Sequence;
import icy.system.SystemUtil;
import icy.type.DataType;

public class KinectLiveDriver extends J4KSDK
{
    // setting for Z intensity compensation
    private static final boolean COMPENSATE_Z_INTENSITY_ERROR = false;

    /**
     * static instance as we can't have 2 kinect driver at the same time...
     */
    private static KinectLiveDriver kinect = null;

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

    private KinectLiveDriver()
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

    /**
     * Get Kinect Driver instance
     *
     * @param plugin
     *        used for native library loading (should not be null)
     */
    public static synchronized KinectLiveDriver getInstance()
    {
        if (kinect == null)
        {
            // we need it just for native library loading
            final KinectDriverPlugin plugin = new KinectDriverPlugin();

            try
            {
                // load native librarie
                if (SystemUtil.is32bits())
                {
                	System.out.println("32 bits system not supported.");
                	return null;
//                    plugin.prepareLibrary("ufdw_j4k_32bit");
//                    plugin.prepareLibrary("ufdw_j4k2_32bit");
                }
                else
                {
                    // plugin.prepareLibrary("ufdw_j4k_64bit");
                    plugin.prepareLibrary("ufdw_j4k2_64bit");
                }
            }
            catch (UnsatisfiedLinkError e)
            {
                // just show a warning
                if (e.getMessage().contains("already loaded"))
                    System.out.println("Warning: " + e.getMessage());
                else
                    throw e;
            }

            // then we can instantiate the driver (after library loading !)
            kinect = new KinectLiveDriver();
        }

        return kinect;
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

    @Override // FIXME: not flipped already (should do it if we want to use HD)
    public void onColorFrameEvent(byte[] color_frame)
    {

        if (!displayColorFrame)
            return;

        if (colorOut == null)
        {
            colorOut = new Sequence();
            colorOut.setAutoUpdateChannelBounds(false);
            colorOut.addImage(new IcyBufferedImage(getColorWidth(), getColorHeight(), 3, DataType.UBYTE));
            colorOut.setName("Color frame");

            // Icy.getMainInterface().addSequence( colorOut );

            r = colorOut.getDataXYAsByte(0, 0, 0);
            g = colorOut.getDataXYAsByte(0, 0, 1);
            b = colorOut.getDataXYAsByte(0, 0, 2);

        }

        int seek = 0;
        for (int i = 0; i < 1920 * 1080 * 4; i += 4)
        {
            r[seek] = color_frame[i + 2];
            b[seek] = color_frame[i];
            g[seek] = color_frame[i + 1];
            seek++;
        }

        colorOut.dataChanged();

    }

    @Override
    public void onInfraredFrameEvent(short[] data)
    {

        flip(data);
        this.infra_frame = data;

    }

    @Override
    public void onInfraredFrameEventFromNative(short[] data)
    {

        if (!displayInfrared)
            return;

        flip(data);
        this.infra_frame = data;

        if (infraOut == null)
        {
            infraOut = new Sequence();
            infraOut.setName("Infrared");
            infraOut.setAutoUpdateChannelBounds(false);
            infraOut.addImage(new IcyBufferedImage(getInfraredWidth(), getInfraredHeight(), 1, DataType.USHORT));

            // Icy.getMainInterface().addSequence(infraOut);
            fireEvent(infraOut, null, KinectEvent.NEW_INFRARED_SEQUENCE);
        }

        infraOut.getImage(0, 0).setDataXY(0, data);

        fireEvent(infraOut, null, KinectEvent.NEW_INFRARED_CAPTURE);

    }

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

    private void fireEvent(Sequence sourceSequence, KinectData kinectData, KinectEvent kinectEvent)
    {
        for (KinectListener kl : kinectListenerList)
        {
            kl.kinectChange(sourceSequence, kinectData, kinectEvent);
        }

    }

    @Override
    public void onDepthFrameEvent(short[] depth_frame, byte[] body_index, float[] xyz, float[] uv)
    {

        if (!displayDepthMap)
            return;

        flip(depth_frame);
        this.depth_frame = depth_frame;

        if (COMPENSATE_Z_INTENSITY_ERROR)
        {
            compensateZIntensityError(depth_frame, this.infra_frame);
        }

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

    @Override
    public void onSkeletonFrameEvent(boolean[] arg0, float[] arg1, float[] arg2, byte[] arg3)
    {

    }

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

}
