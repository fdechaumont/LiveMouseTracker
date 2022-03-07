package plugins.fab.azure.kinect;

import java.io.IOException;

import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.type.collection.array.ByteArrayConvert;

public class DriverServer extends Thread
{
    private TCPServer tcpServer;

    public DriverServer(int port) throws IOException
    {
        super("Kinect Azure driver - server");

        tcpServer = new TCPServer(port);
        start();
    }

    @Override
    public void run()
    {
        while (!Thread.interrupted())
        {
            final IcyBufferedImage img = Icy.getMainInterface().getActiveImage();

            if ((img != null) && (tcpServer != null))
            {
                try
                {
                    // send the active image through TCP (server side)
                    tcpServer.sendData(ByteArrayConvert.toByteArray(img.getDataXY(0), false));
                    // log
                    System.out.println("Image " + img + " send through TCP.");
                }
                catch (IOException e)
                {
                    System.err.println(e.getMessage());
                }
            }

            try
            {
                // keep CPU usage low
                Thread.sleep(500);
            }
            catch (InterruptedException e)
            {
                // ignore
            }
        }
    }

    public void close()
    {
        interrupt();
        tcpServer.close();
    }
}
