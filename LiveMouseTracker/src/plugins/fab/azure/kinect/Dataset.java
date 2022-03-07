package plugins.fab.azure.kinect;

public class Dataset
{
    public static class IMU
    {
        public float temperature;
        public float accelX;
        public float accelY;
        public float accelZ;
        public long accelTimeStamp;
        public float gyroX;
        public float gyroY;
        public float gyroZ;
        public long gyroTimeStamp;
    }
    
    public static class Image
    {
        public int sizeX;
        public int sizeY;
        public short[] data;

        public boolean isEmpty()
        {
            return (data == null) || (data.length == 0);
        }
    }
    
    public Dataset()
    {
        super();
        
        imu = new IMU();
    }

    public int deviceId;
    public long timeStamp;
    public final IMU imu;
    public Image colorImage;
    public Image depthImage;
    public Image irImage;
    public Image strechedDepthImage;
    public Image cloudDepthImage;
}
