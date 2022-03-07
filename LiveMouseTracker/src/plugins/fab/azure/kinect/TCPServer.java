package plugins.fab.azure.kinect;

import java.io.IOException;
import java.net.ServerSocket;

public class TCPServer extends Thread
{
    final ServerSocket serverSocket;
    SocketCom socketCom;
    volatile boolean started;

    @SuppressWarnings("resource")
    public TCPServer(int port) throws IOException
    {
        super("Azure Kinect Server");

        serverSocket = new ServerSocket(port);
        // not yet connected
        socketCom = null;
        started = false;

        // start the thread
        start();

        // wait thread started
        while (!started)
            Thread.yield();
    }

    @SuppressWarnings("resource")
    @Override
    public void run()
    {
        started = true;

        try
        {
            // await connection
            socketCom = new SocketCom(serverSocket.accept());
            // log
            System.out.println("Connection received on server !");
        }
        catch (IOException e)
        {
            System.err.println(e.getMessage());
        }
    }

    public boolean isConnected()
    {
        return socketCom != null;
    }

    public void sendData(byte[] data) throws IOException
    {
        if (!isConnected())
            throw new IOException("Not connected !");

        socketCom.sendData(data);
    }

    public byte[] readData() throws IOException
    {
        if (!isConnected())
            throw new IOException("Not connected !");

        return socketCom.readData();
    }

    public byte[] readData(int size) throws IOException
    {
        if (!isConnected())
            throw new IOException("Not connected !");

        return socketCom.readData(size);
    }

    public byte[] readDataBloc() throws IOException
    {
        if (!isConnected())
            throw new IOException("Not connected !");

        return socketCom.readDataBloc();
    }

    public void close()
    {
        if (socketCom != null)
            socketCom.close();

        try
        {
            serverSocket.close();
        }
        catch (IOException e)
        {
            // ignore
        }
    }
}
