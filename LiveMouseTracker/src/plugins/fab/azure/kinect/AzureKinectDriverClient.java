package plugins.fab.azure.kinect;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import plugins.fab.azure.kinect.Dataset.Image;

public class AzureKinectDriverClient extends Thread
{
    final static String VERSION = "1.3";

    final static long SERVER_SIG_INIT = 0x1234567890864201L;
    final static long SERVER_SIG_DATASET = 0x1234567890864202L;
    final static long CLIENT_SIG = 0x9876543210123456L;

    final public static int COLOR_IMAGE = 1 << 0;
    final public static int DEPTH_IMAGE = 1 << 1;
    final public static int IR_IMAGE = 1 << 2;
    final public static int DEPTH_EXT_IMAGE = 1 << 3;
    final public static int DEPTH_CLOUD_IMAGE = 1 << 4;

    final public static int IMU_SAMPLE = 1 << 8;

    private TCPClient tcpClient;

    private final String host;
    private final int port;
    private final int config;
    private final List<DatasetReadyListener> listeners;

    public AzureKinectDriverClient(String host, int port, int config)
    {
        super("Kinect Azure driver");

        this.host = host;
        this.port = port;
        this.config = (config != 0) ? config : (COLOR_IMAGE | DEPTH_EXT_IMAGE | IR_IMAGE);

        listeners = new ArrayList<DatasetReadyListener>();

        tcpClient = null;
        start();
    }

    public void addListener(DatasetReadyListener listener)
    {
        listeners.add(listener);
    }

    public void removeListener(DatasetReadyListener listener)
    {
        listeners.remove(listener);
    }

    private void fireDatasetReceived(List<Dataset> datasets)
    {
        for (DatasetReadyListener listener : listeners)
            listener.DatasetReceived(datasets);
    }

    public int getConfig()
    {
        return config;
    }

    @Override
    public void run()
    {
        while (!isInterrupted())
        {
            try
            {
                // need to initialize TCP client first
                while (!isInterrupted() && (tcpClient == null))
                {
                    try
                    {
                        // connection
                        tcpClient = new TCPClient(host, port);

                        // wait for signature
                        tcpClient.waitLong(SERVER_SIG_INIT);
                        // set configuration (wanted data)
                        setConfig();
                    }
                    catch (Exception e)
                    {
                        System.err.println("Error while initializing TCP client");
                        System.err.println(e.getMessage());
                        Thread.sleep(5000);
                    }
                }

                try
                {
                    while (!isInterrupted() && tcpClient.hasDataReady())
                    {
                        // wait for signature
                        tcpClient.waitLong(SERVER_SIG_DATASET);

                        // read datasets back through TCP (client side)
                        final List<Dataset> datasets = readDatasets();

                        // notify listeners
                        fireDatasetReceived(datasets);

                        // log read data
                        // System.out.println("TCP read datasets, num = " + datasets.size() + ".");
                    }
                }
                catch (IOException e)
                {
                    System.err.println(e.getMessage());
                    tcpClient.close();
                }
                catch (Exception e)
                {
                    // can happen if server got interrupted, we may have received invalid data
                    System.err.println(e.getMessage());
                    tcpClient.close();
                }

                // keep CPU usage low
                Thread.sleep(1);

                // if connection was lost we reset it
                if (!tcpClient.isConnected())
                {
                    tcpClient.close();
                    tcpClient = null;
                }
            }
            catch (InterruptedException e)
            {
                // interrupted
                Thread.currentThread().interrupt();
            }
        }
    }

    public void setConfig() throws IOException
    {
        // send client signature
        tcpClient.sendLong(CLIENT_SIG);
        // wait for server acknowledge
        if (tcpClient.readInt() != 1)
            throw new IOException("SetConfig: invalid reply from server !");

        // set config
        tcpClient.sendInt(config);
        // wait for server acknowledge
        if (tcpClient.readInt() != 2)
            throw new IOException("SetConfig: invalid reply from server !");
    }

    public List<Dataset> readDatasets() throws IOException
    {
        final List<Dataset> result = new ArrayList<>();

        // important that format is *perfectly* matching the server version
        final int numDataset = tcpClient.readInt();

        for (int i = 0; i < numDataset; i++)
            result.add(readDataset());

        return result;
    }

    public Dataset readDataset() throws IOException
    {
        final Dataset result = new Dataset();

        // important that dataset format is *perfectly* matching the server version
        result.deviceId = tcpClient.readInt();
        result.timeStamp = tcpClient.readLong();

        result.imu.temperature = tcpClient.readFloat();
        result.imu.accelX = tcpClient.readFloat();
        result.imu.accelY = tcpClient.readFloat();
        result.imu.accelZ = tcpClient.readFloat();
        result.imu.accelTimeStamp = tcpClient.readLong();
        result.imu.gyroX = tcpClient.readFloat();
        result.imu.gyroY = tcpClient.readFloat();
        result.imu.gyroZ = tcpClient.readFloat();
        result.imu.gyroTimeStamp = tcpClient.readLong();

        result.colorImage = readImage();
        result.depthImage = readImage();
        result.irImage = readImage();
        result.strechedDepthImage = readImage();
        result.cloudDepthImage = readImage();

        return result;
    }

    public Image readImage() throws IOException
    {
        final Image result = new Image();

        // important that image format is *perfectly* matching the server version
        result.sizeX = tcpClient.readInt();
        result.sizeY = tcpClient.readInt();
        result.data = Utils.byteToShort(tcpClient.readDataBloc());

        return result;
    }

    public void close() throws InterruptedException
    {
        interrupt();
        join();
        if (tcpClient != null)
            tcpClient.close();
    }
}
