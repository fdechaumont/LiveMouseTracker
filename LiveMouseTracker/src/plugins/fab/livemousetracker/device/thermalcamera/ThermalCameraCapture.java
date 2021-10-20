package plugins.fab.livemousetracker.device.thermalcamera;

import icy.canvas.IcyCanvas;
import icy.gui.frame.progress.FailedAnnounceFrame;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.image.colormodel.IcyColorModel;
import icy.main.Icy;
import icy.math.FPSMeter;
import icy.painter.Overlay;
import icy.plugin.abstract_.PluginActionable;
import icy.plugin.interface_.PluginThreaded;
import icy.sequence.Sequence;
import icy.sequence.SequenceEvent;
import icy.sequence.SequenceListener;
import icy.type.DataType;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import com.github.sarxos.webcam.Webcam;


public class ThermalCameraCapture extends Thread implements SequenceListener, PluginThreaded
{

    Sequence sequence;
    Webcam webcam;
    volatile boolean running;
    IcyBufferedImage image = null;
    boolean thermalCameraPresent = false;

    public ThermalCameraCapture( Sequence sequenceToUse )
    {
    	System.out.println("Starting thermal camera capture.");

    	sequence = sequenceToUse;
    	if ( sequence == null )
        {
        	sequence = new Sequence("Thermal camera");
        	sequence.addListener(this);
        }
    	startWebCam();

    	if ( isThermalCameraPresent() )
    	{
    		Icy.addSequence(sequence);
    		image = new IcyBufferedImage( 640 , 240, 3, DataType.UBYTE );
    		sequence.setImage( 0 , 0, image);

    		this.start();
    	}
    }

    public boolean isThermalCameraPresent()
    {
    	return thermalCameraPresent;
    }

    private void startWebCam()
    {

        for ( Webcam cam : Webcam.getWebcams() )
        {
        	if ( cam.getName().contains("FLIR") )
        	{
        		webcam = cam;
        	}
        }

        if ( webcam == null )
        {
        	System.out.println("No thermal camera found.");
        	sequence = null;
        	return;
        }

//        webcam = Webcam.getDefault();
        webcam.setViewSize( new Dimension( 320,240 ) );
    }

    public Sequence getSequence() {
		return sequence;
	}

    /** As the FLIR camera acquisition freezes after 7 to 23 h of work, this
     * procedure restarts its.
     * It takes about 10 seconds to restart it.
     *  */
    boolean restartCapture = false;
    public void restartCapture() {
    	if ( isThermalCameraPresent() )
    	{
    		System.out.println("Restart thermal capture");
    		restartCapture = true;
    	}else
    	{
    		System.out.println("Can't restart thermal capture");
    	}
    }
//    public boolean isThermalCameraPresent()
//    {
//    	if ( sequence == null ) return false;
//    	return true;
//    }

    @Override
    public void run()
    {
        if (webcam != null)
        {

            if (webcam.open())
            {
                try
                {
                    running = true;
                    while (running)
                    {
                    	if  ( restartCapture )
                    	{
                    		System.out.println("THERMAL RESTART");
                    		System.out.println("THERMAL CLOSE");
                    		webcam.close();
                    		//Webcam.resetDriver();
                    		//webcam = cam.getName().contains("FLIR")
                    		System.out.println("THERMAL START");
                    		startWebCam();
                    		System.out.println("THERMAL OPEN");
                    		webcam.open();
                    		restartCapture = false;
                    	}

                    	IcyBufferedImage webImage= IcyBufferedImage.createFrom( webcam.getImage() );
                    	image.copyData( webImage );

                    	//image.getGraphics().drawImage( webcam.getImage(), 0, 0, null );
//                        sequence.setImage(0, 0, webcam.getImage());
                        try {
							Thread.sleep( 1000 / 10 );
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
                    }
                }
                finally
                {
                    webcam.close();
                }
            }
        }
        else
            new FailedAnnounceFrame("No webcam found !");
    }

    @Override
    public void sequenceChanged(SequenceEvent sequenceEvent)
    {

    }

    @Override
    public void sequenceClosed(Sequence sequence)
    {
        running = false;
        sequence.removeListener(this);
    }


}
