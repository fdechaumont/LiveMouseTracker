package plugins.fab.kinectdriver;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

import edu.ufl.digitalworlds.j4k.J4KSDK;
import icy.gui.frame.IcyFrame;
import icy.gui.util.GuiUtil;
import icy.main.Icy;
import icy.plugin.abstract_.PluginActionable;
import icy.sequence.Sequence;

public class KinectLive extends PluginActionable implements ActionListener, KinectListener
{
    IcyFrame mainFrame = new IcyFrame("Live Kinect", true, true, true, true);
    JButton startButton = new JButton("Start");
    JButton stopButton = new JButton("Stop");
    JCheckBox recordCheckBox = new JCheckBox("record");
    JLabel nbFrameLabel = new JLabel("00000");

    JCheckBox depthMapCheckBox = new JCheckBox("Depth map", true);
    JCheckBox infraredFrameCheckBox = new JCheckBox("Infrared Frame", true);
    JCheckBox colorFrameMapCheckBox = new JCheckBox("Color Frame", false);

    Sequence infraOutLive = null;
    Sequence depthOutLive = null;

    Sequence infraOutRecord = null;
    Sequence depthOutRecord = null;
    int nbFrameRecorded = 0;

    String baseFolder = "c://kinect record//";

    long previousMs = 0;
    // int t = 0;

    @Override
    public void run()
    {
        colorFrameMapCheckBox.setEnabled(false);
        mainFrame.getContentPane().setLayout(new BoxLayout(mainFrame.getContentPane(), BoxLayout.PAGE_AXIS));
        mainFrame.getContentPane().add(GuiUtil.createLineBoxPanel(stopButton, startButton));
        mainFrame.getContentPane()
                .add(GuiUtil.createLineBoxPanel(depthMapCheckBox, infraredFrameCheckBox, colorFrameMapCheckBox));
        mainFrame.getContentPane().add(GuiUtil.createLineBoxPanel(recordCheckBox, nbFrameLabel));

        mainFrame.pack();
        mainFrame.setVisible(true);
        mainFrame.addToDesktopPane();

        startButton.addActionListener(this);
        stopButton.addActionListener(this);
        depthMapCheckBox.addActionListener(this);
        infraredFrameCheckBox.addActionListener(this);
        colorFrameMapCheckBox.addActionListener(this);

        System.out.println("TODO: Miror image");

        System.out.println("***");
        for (String param : Icy.getCommandLineArgs())
            System.out.println(param);

        System.out.println("*******");
        for (String param : Icy.getCommandLinePluginArgs())
        {
            // arg.equalsIgnoreCase
            System.out.println(param);
        }

        // timeGarbage.start();

        // System.out.println("If record enabled, recording in " + baseFolder );
        // boolean fileNameOk = false;
        // String seqId ="";
        // while ( !fileNameOk )
        // {
        // seqId = NumberFormat.format( (int)(Math.random()*1000d) );
        // fileNameOk = !icy.file.FileUtil.exists( baseFolder + seqId );
        // }

        // infraBaseFileName = baseFolder+seqId+"/infra/infra" ;
        // depthBaseFileName = baseFolder+seqId+"/depth/depth" ;

    }

    // String infraBaseFileName ;
    // String depthBaseFileName ;

    @Override
    public void actionPerformed(ActionEvent e)
    {
        final KinectLiveDriver kinect = KinectLiveDriver.getInstance();

        if (e.getSource() == startButton)
        {
            nbFrameRecorded = 0;
            setDisplayParameters(kinect);

            // stop kinect first if needed
            if (kinect.isInitialized())
            {
                kinect.stop();
                kinect.removeAllKinectListener();
            }

            kinect.start(// J4KSDK.COLOR|
                    J4KSDK.DEPTH | J4KSDK.INFRARED);
            kinect.addKinectListener(this);

            if (recordCheckBox.isSelected())
                kinect.addKinectListener(new KinectLiveRecorder());
        }

        if (e.getSource() == stopButton)
            kinect.stop();

        if (e.getSource() == depthMapCheckBox || e.getSource() == infraredFrameCheckBox
                || e.getSource() == colorFrameMapCheckBox)
            setDisplayParameters(kinect);
    }

    private void setDisplayParameters(KinectLiveDriver kinect)
    {
        if (kinect != null)
        {
            kinect.displayInfrared(infraredFrameCheckBox.isSelected());
            kinect.displayColorFrame(colorFrameMapCheckBox.isSelected());
            kinect.displayDepthMap(depthMapCheckBox.isSelected());
        }
    }

    @Override
    public void kinectChange(Sequence sourceSequence, KinectData kinectData, KinectEvent kinectEvent)
    {

        if (kinectEvent == KinectEvent.NEW_DEPTH_CAPTURE)
        {
            // DO NOT PROCESS ON DEPTH EVENT.
            // THE DEPTH EVENT OCCURS BEFORE INFRARED MAP IS READY
            // IF YOU DO SO, IMAGE WILL BE SHIFTED IN TIME (INFRA AND DEPTH WILL
            // BE UNSYNC)
        }

        if (kinectEvent == KinectEvent.NEW_INFRARED_CAPTURE)
        {
            /*
             * if ( depthOutLive == null || infraOutLive == null ) return;
             *
             * nbFrameLabel.setText( NumberFormat.format( t ) );
             *
             * if ( recordCheckBox.isSelected() ) { File infraFileWithNumber =
             * new File ( infraBaseFileName + NumberFormat.format( t ) + ".png"
             * );
             *
             * System.out.println( infraFileWithNumber );
             *
             * PNGEncoderThreaded petInfra = new PNGEncoderThreaded(
             * infraOutLive.getFirstImage() , infraFileWithNumber );
             *
             * petInfra.start();
             *
             * File depthFileWithNumber = new File ( depthBaseFileName +
             * NumberFormat.format( t ) + ".png" ); PNGEncoderThreaded petDepth
             * = new PNGEncoderThreaded( depthOutLive.getFirstImage() ,
             * depthFileWithNumber );
             *
             * petDepth.start(); } t++;
             */
        }

        if (kinectEvent == KinectEvent.NEW_DEPTH_SEQUENCE)
        {
            depthOutLive = sourceSequence;
            // depthOutLive.setName( infraBaseFileName );

            // if ( ! recordCheckBox.isSelected() )
            {
                addSequence(depthOutLive);
            }
        }

        if (kinectEvent == KinectEvent.NEW_INFRARED_SEQUENCE)
        {
            infraOutLive = sourceSequence;
            // depthOutLive.setName( depthBaseFileName );

            // if ( ! recordCheckBox.isSelected() )
            {
                addSequence(infraOutLive);
            }
        }

    }

}
