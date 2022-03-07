package plugins.fab.azure.kinect;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

import icy.type.collection.array.DynamicArray;

public class SocketCom
{
    final protected Socket socket;
    final protected BufferedInputStream in;
    final protected BufferedOutputStream out;

    public SocketCom(Socket s) throws IOException
    {
        super();

        socket = s;

        // setting
        socket.setKeepAlive(true);
        // // just to allow sending large data block without locking server as client
        socket.setReceiveBufferSize(32 * 1024 * 1024);
        socket.setSendBufferSize(32 * 1024 * 1024);

        in = new BufferedInputStream(socket.getInputStream(), 64 * 1024);
        out = new BufferedOutputStream(socket.getOutputStream(), 64 * 1024);
    }

    public boolean hasDataReady() throws IOException
    {
        return in.available() > 0;
    }

    public void waitLong(long value) throws IOException, InterruptedException
    {
        while (readLong() != value)
            ;
    }

    public void waitInt(long value) throws IOException, InterruptedException
    {
        while (readInt() != value)
            ;
    }

    public void sendData(byte[] data) throws IOException
    {
        final byte[] size = new byte[4];

        size[0] = (byte) (data.length >> 0);
        size[1] = (byte) (data.length >> 8);
        size[2] = (byte) (data.length >> 16);
        size[3] = (byte) (data.length >> 24);

        // send size first
        out.write(size);
        // then send data
        for (int off = 0; off < data.length; off += 0x10000)
        {
            final int len = Math.min(0x10000, data.length - off);

            if (len > 0)
                out.write(data, off, len);
        }

        out.flush();
    }

    public void sendLong(long value) throws IOException
    {
        final byte[] data = new byte[8];

        data[0] = (byte) (value >> 0);
        data[1] = (byte) (value >> 8);
        data[2] = (byte) (value >> 16);
        data[3] = (byte) (value >> 24);
        data[4] = (byte) (value >> 32);
        data[5] = (byte) (value >> 40);
        data[6] = (byte) (value >> 48);
        data[7] = (byte) (value >> 56);

        // then send data
        out.write(data);

        out.flush();
    }

    public void sendInt(int value) throws IOException
    {
        final byte[] data = new byte[4];

        data[0] = (byte) (value >> 0);
        data[1] = (byte) (value >> 8);
        data[2] = (byte) (value >> 16);
        data[3] = (byte) (value >> 24);

        // then send data
        out.write(data);

        out.flush();
    }

    public byte[] readData() throws IOException
    {
        final DynamicArray result = new DynamicArray.Byte();
        final byte[] bytes = new byte[0x10000];

        int len;
        do
        {
            len = in.read(bytes);
            result.add(bytes, 0, len);
        }
        while (len > 0);

        return (byte[]) result.asArray();
    }

    public byte[] readData(int size) throws IOException
    {
        final byte[] result = new byte[size];

        for (int off = 0; off < size;)
        {
            final int len = Math.min(0x10000, size - off);

            if (len > 0)
            {
                final int read = in.read(result, off, len);

                if (read == -1)
                    throw new IOException("Unexpected end of data (" + off + " bytes read, " + size + " expected !");

                off += read;
            }
        }

        return result;
    }

    public int readInt() throws IOException
    {
        final byte[] data = readData(4);
        int result = 0;

        result |= (data[0] & 0xFF) << 0;
        result |= (data[1] & 0xFF) << 8;
        result |= (data[2] & 0xFF) << 16;
        result |= (data[3] & 0xFF) << 24;

        return result;
    }

    public long readLong() throws IOException
    {
        final byte[] data = readData(8);
        long result = 0;

        result |= ((long) (data[0] & 0xFF)) << 0;
        result |= ((long) (data[1] & 0xFF)) << 8;
        result |= ((long) (data[2] & 0xFF)) << 16;
        result |= ((long) (data[3] & 0xFF)) << 24;
        result |= ((long) (data[4] & 0xFF)) << 32;
        result |= ((long) (data[5] & 0xFF)) << 40;
        result |= ((long) (data[6] & 0xFF)) << 48;
        result |= ((long) (data[7] & 0xFF)) << 56;

        return result;
    }

    public float readFloat() throws IOException
    {
        return Float.intBitsToFloat(readInt());
    }

    public double readDouble() throws IOException
    {
        return Double.longBitsToDouble(readLong());
    }

    public byte[] readDataBloc() throws IOException
    {
        return readData(readInt());
    }

    public boolean isConnected()
    {
        if (socket.isClosed())
            return false;

        if (!socket.isBound())
            return false;
        if (!socket.isConnected())
            return false;

        try
        {
            // dummy write
            out.write(0);
            out.flush();

            return true;
        }
        catch (IOException e)
        {
            return false;
        }
    }

    public void close()
    {
        try
        {
            out.close();
            in.close();
            socket.close();
        }
        catch (IOException e)
        {
            System.out.println(e.getMessage());
        }
    }
}
