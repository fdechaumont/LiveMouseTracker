package plugins.fab.azure.kinect;

import java.net.Socket;

public class TCPClient extends SocketCom
{
    @SuppressWarnings("resource")
    public TCPClient(String host, int port) throws Exception
    {
        super(new Socket(host, port));
    }
}