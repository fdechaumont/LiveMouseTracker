package plugins.fab.azure.kinect;

import java.util.Arrays;

import icy.image.IcyBufferedImage;
import icy.type.DataType;
import plugins.fab.azure.kinect.Dataset.Image;

public class Utils
{
    public static byte[] shortToByte(short[] data)
    {
        final byte[] result = new byte[data.length * 2];

        for (int i = 0; i < data.length; i++)
        {
            result[(i * 2) + 0] = (byte) (data[i] >> 0);
            result[(i * 2) + 1] = (byte) (data[i] >> 8);
        }

        return result;
    }

    public static short[] byteToShort(byte[] data)
    {
        if ((data.length & 1) != 0)
            throw new IllegalArgumentException("byteToShort: data length should be even (" + data.length + ") !");

        final short[] result = new short[data.length / 2];

        for (int i = 0; i < data.length; i += 2)
            result[i / 2] = (short) (((data[i + 0] & 0xFF) << 0) + ((data[i + 1] & 0xFF) << 8));

        return result;
    }

    public static IcyBufferedImage toIcyShortImage(Image image)
    {
        final int size = image.sizeX * image.sizeY;

        if ((size == 0) || image.isEmpty())
            return null;

        final int ch = image.data.length / size;
        final IcyBufferedImage result = new IcyBufferedImage(image.sizeX, image.sizeY, ch, DataType.USHORT);

        for (int c = 0; c < ch; c++)
            result.setDataXYAsShort(c, Arrays.copyOfRange(image.data, c * size, (c + 1) * size));

        return result;
    }

    public static IcyBufferedImage toIcyByteImage(Image image)
    {
        final int size = image.sizeX * image.sizeY;

        if ((size == 0) || image.isEmpty())
            return null;

        final byte[] data = shortToByte(image.data);
        final byte[][] dataCh = new byte[3][size];
        
        // separate RGBA channels
        for(int i = 0; i < size; i++)
        {
            dataCh[0][i] = data[(i * 4) + 0];
            dataCh[1][i] = data[(i * 4) + 1];
            dataCh[2][i] = data[(i * 4) + 2];
        }
            
        return new IcyBufferedImage(image.sizeX, image.sizeY, dataCh, false);
    }
}
