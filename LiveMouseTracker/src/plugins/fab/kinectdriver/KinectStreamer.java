package plugins.fab.kinectdriver;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JFileChooser;
import javax.swing.Timer;

import edu.ufl.digitalworlds.j4k.J4KSDK;
import icy.file.FileUtil;
import icy.file.Loader;
import icy.gui.frame.IcyFrame;
import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;

/**
 * This class allow to
 * switch between a live kinect or a recorded kinect file
 * 
 * @author Fab
 */
public class KinectStreamer implements ActionListener, KinectListener
{
    private static final int NUMBER_OF_LOAD_FRAME_CACHED = 90;

    public static enum StreamerState
    {
        STOP, PLAYFILE, LIVE
    }

    IcyFrame mainFrame = new IcyFrame("Kinect Streamer", true, true, true, true);
    // JButton playLiveButton = new JButton("Play Live");
    // JCheckBox recordCheckBox = new JCheckBox("Record");
    // JButton stopLiveButton = new JButton("Stop Live");

    // JButton playRecordedButton = new JButton("Play Recorded");
    // JButton stopPlayRecordedButton = new JButton("Stop Recorded");

    // JComboBox<String> experimentListComboBox ;

    Sequence infraOutSequence = null;
    Sequence depthOutSequence = null;

    Timer frameTimer = null;

    int currentFrame = 0;
    Sequence infraInputSequence = null;
    Sequence depthInputSequence = null;

    // JButton pauseButton = new JButton("||");
    // JButton stepButton = new JButton(">|");
    // JButton playButton = new JButton(">");

    String baseRecordDir = "c://kinect record//";
    KinectStreamerPanel mainPanel;

    String experimentRecordSelectedBaseFile = null; // the folder name of the file suech as c://kinect record//0524 (as in the combo box)

    StreamerState state = StreamerState.STOP;

    ArrayList<CacheFileLoaded> cacheFileLoadedList = new ArrayList<>();
    Thread updateCacheThread = null;

    public KinectStreamer(boolean showGUI)
    {
        super();

        mainPanel = new KinectStreamerPanel();
        mainFrame.getContentPane().setLayout(new BoxLayout(mainFrame.getContentPane(), BoxLayout.PAGE_AXIS));
        mainFrame.getContentPane().add(mainPanel);

        // mainFrame.getContentPane().add( GuiUtil.createLineBoxPanel( stopLiveButton , playLiveButton , recordCheckBox ) );
        // mainFrame.getContentPane().add( GuiUtil.createLineBoxPanel( stopPlayRecordedButton , playRecordedButton ) );
        // mainFrame.getContentPane().add( GuiUtil.createLineBoxPanel( pauseButton , stepButton, playButton ) );

        mainPanel.getButtonPausePlayBack().addActionListener(this);
        mainPanel.getButtonResumePlayBack().addActionListener(this);
        mainPanel.getButtonStepPlayBack().addActionListener(this);
        // pauseButton.addActionListener( this );
        // stepButton.addActionListener( this );
        // playButton.addActionListener( this );

        refreshFileComboBox();

        // experimentListComboBox = new JComboBox<String>( stringArray );

        // mainFrame.getContentPane().add( GuiUtil.createLineBoxPanel( experimentListComboBox ) );

        mainFrame.pack();
        if (showGUI)
        {
            mainFrame.setVisible(true);
            mainFrame.addToDesktopPane();
        }

        mainPanel.getBtnPlayLive().addActionListener(this);
        mainPanel.getChckbxRecordRawData().addActionListener(this);
        mainPanel.getBtnStopLive().addActionListener(this);
        mainPanel.getBtnPlayPlayback().addActionListener(this);

        mainPanel.getBtnSelectInputFolder().addActionListener(this);

        // playLiveButton.addActionListener( this );
        // stopLiveButton.addActionListener( this );
        // playRecordedButton.addActionListener( this );
        // stopPlayRecordedButton.addActionListener( this );

    }
    
    public void setSequenceForOverlay(Sequence seq ) {
    	// do nothing
    }

    public StreamerState getState()
    {
        return state;
    }

    private void refreshFileComboBox()
    {
        String[] stringArray = FileUtil.getFiles(baseRecordDir, null, false, true, false);
        DefaultComboBoxModel model = new DefaultComboBoxModel(stringArray);
        mainPanel.getDataSetComboBox().setModel(model);
    }

    public String getInfraFileName(int frame)
    {
        return experimentRecordSelectedBaseFile + "//infra//infra" + NumberFormat.format(currentFrame) + ".png";
    }

    public String getDepthFileName(int frame)
    {
        return experimentRecordSelectedBaseFile + "//depth//depth" + NumberFormat.format(currentFrame) + ".png";
    }

    private void updateCacheList()
    {
        for (int i = currentFrame; i < currentFrame + NUMBER_OF_LOAD_FRAME_CACHED; i++)
        {
            String infraFile = getInfraFileName(i);
            String depthFile = getDepthFileName(i);

            if (!FileUtil.exists(infraFile) || !FileUtil.exists(depthFile))
            {
                return;
            }

            if (!isFileInLoadCache(infraFile))
            {
                CacheFileLoaded cache = new CacheFileLoaded(infraFile);
                synchronized (cacheFileLoadedList)
                {
                    cacheFileLoadedList.add(cache);
                }
            }
            if (!isFileInLoadCache(depthFile))
            {
                CacheFileLoaded cache = new CacheFileLoaded(depthFile);
                synchronized (cacheFileLoadedList)
                {
                    cacheFileLoadedList.add(cache);
                }
            }
        }

    }

    /** The file in load cache. Does not mean it is loaded yet. Maybe it's just loading. */
    private boolean isFileInLoadCache(String file)
    {
        synchronized (cacheFileLoadedList)
        {
            for (CacheFileLoaded cfl : cacheFileLoadedList)
            {
                if (cfl.fileName.equals(file))
                    return true;
            }
        }
        return false;
    }

    private IcyBufferedImage getImageInLoadCache(String file)
    {
        synchronized (cacheFileLoadedList)
        {
            for (CacheFileLoaded cfl : cacheFileLoadedList)
            {
                if (cfl.fileName.equals(file))
                    return cfl.image;
            }
        }
        return null;
    }

    private void freeImageInLoadCache(String file)
    {
        CacheFileLoaded cache = null;
        synchronized (cacheFileLoadedList)
        {
            for (CacheFileLoaded cfl : cacheFileLoadedList)
            {
                if (cfl.fileName.equals(file))
                {
                    cache = cfl;
                    break;
                }
            }
            cacheFileLoadedList.remove(cache);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {

        if (e.getSource() == mainPanel.getBtnSelectInputFolder())
        {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Choose the kinect dataset root folder");
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            if (fileChooser.showOpenDialog(mainFrame.getContentPane()) == JFileChooser.APPROVE_OPTION)
            {
                String folder = FileUtil.getDirectory(fileChooser.getSelectedFile().getAbsolutePath());
                folder += "//";
                baseRecordDir = folder;
                refreshFileComboBox();
            }
        }

        if (e.getSource() == frameTimer || e.getSource() == mainPanel.getButtonStepPlayBack())
            if (state != StreamerState.LIVE)
            {

                if (e.getSource() == mainPanel.getButtonStepPlayBack()) // stepButton )
                {
                    pause();
                }

                if (updateCacheThread != null)
                {
                    if (!updateCacheThread.isAlive())
                    {
                        updateCacheThread = null;
                    }
                }

                if (updateCacheThread == null)
                {
                    updateCacheThread = new Thread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            updateCacheList();
                        }
                    });
                    updateCacheThread.start();
                }

                // In play recorded file, plays the selected sequence.
                currentFrame++;

                IcyBufferedImage infraImage = null;
                IcyBufferedImage depthImage = null;

                try
                {

                    String infraFile = getInfraFileName(currentFrame); // experimentRecordSelectedBaseFile + "//infra//infra" +
                                                                       // NumberFormat.format(currentFrame) + ".png";
                    String depthFile = getDepthFileName(currentFrame); // experimentRecordSelectedBaseFile + "//depth//depth" +
                                                                       // NumberFormat.format(currentFrame) + ".png";

                    infraImage = getImageInLoadCache(infraFile);
                    depthImage = getImageInLoadCache(depthFile);

                    if (infraImage == null || depthImage == null)
                    {
                        if (!FileUtil.exists(infraFile) || !FileUtil.exists(depthFile))
                        {
                            System.out.println("[Kinect Streamer] EOF.");
                            stopPlayRecord();
                        }
                    }

                    if (infraImage == null) // means it was not cached.
                    {
                        infraImage = Loader.loadImage(infraFile);
                    }

                    if (depthImage == null) // means it was not cached.
                    {
                        depthImage = Loader.loadImage(depthFile);
                    }

                    freeImageInLoadCache(infraFile);
                    freeImageInLoadCache(depthFile);

                }
                catch (Exception e1)
                {
                    System.out.println("[Kinect Streamer] IO ERROR while playing. Stop playing.");
                    stopPlayRecord();
                }

                if (infraImage == null || depthImage == null)
                {
                    stopPlayRecord();
                }

                if (infraOutSequence == null)
                {
                    infraOutSequence = new Sequence("Infra");
                    fireEvent(infraOutSequence, null, KinectEvent.NEW_INFRARED_SEQUENCE);
                }

                if (depthOutSequence == null)
                {
                    depthOutSequence = new Sequence("Depth");
                    fireEvent(depthOutSequence, null, KinectEvent.NEW_DEPTH_SEQUENCE);
                }

                infraOutSequence.setImage(0, 0, infraImage);
                depthOutSequence.setImage(0, 0, depthImage);

                if (infraImage != null)
                {
                    fireEvent(infraOutSequence, null, KinectEvent.NEW_INFRARED_CAPTURE);
                }
                if (depthImage != null)
                {
                    fireEvent(depthOutSequence, null, KinectEvent.NEW_DEPTH_CAPTURE);
                }

            }

        if (e.getSource() == mainPanel.getBtnPlayPlayback())
        {
            experimentRecordSelectedBaseFile = (String) (mainPanel.getDataSetComboBox().getSelectedItem());
            System.out.println("Selected record is: " + experimentRecordSelectedBaseFile);

            currentFrame = 0;
            state = StreamerState.PLAYFILE;
            frameTimer = new Timer(1000 / 30, this);
            frameTimer.start();
        }

        if (e.getSource() == mainPanel.getButtonPausePlayBack()) // pauseButton )
        {
            pause();
        }

        if (e.getSource() == mainPanel.getButtonResumePlayBack()) // playButton )
        {
            play();
        }

        if (e.getSource() == mainPanel.getBtnStopPlayback())
        {
            stopPlayRecord();
        }

        if (e.getSource() == mainPanel.getBtnStopLive())
        {
            stopLive();
        }

        if (e.getSource() == mainPanel.getBtnPlayLive())
        {
            final KinectLiveDriver kinect = KinectLiveDriver.getInstance();

            // stop kinect first if needed
            if (kinect.isInitialized())
            {
                kinect.stop();
                kinect.removeAllKinectListener();
            }

            kinect.start(// J4KSDK.COLOR| J4KSDK.UV|J4KSDK.XYZ |
                    J4KSDK.DEPTH | J4KSDK.INFRARED);
            kinect.addKinectListener(this);
            state = StreamerState.LIVE;

            // create the KinectRecorder

            if (mainPanel.getChckbxRecordRawData().isSelected())
            {
                if (mainPanel.getChckbxRecordRawData().isSelected())
                {
                    kinectLiveRecorder = new KinectLiveRecorder();
                    kinect.addKinectListener(kinectLiveRecorder);
                }
            }

        }

    }

    KinectLiveRecorder kinectLiveRecorder = null;

    public void pause()
    {

        if (state == StreamerState.PLAYFILE)
        {
            frameTimer.stop();
            // System.out.println("[Kinect streamer] playback paused.");
        }
        else
        {
            System.out.println("Can't pause in mode " + state);
        }

    }

    public void play()
    {
        if (state == StreamerState.PLAYFILE)
        {
            frameTimer = new Timer(1000 / 30, this);
            frameTimer.start();
            System.out.println("[Kinect streamer] play.");
        }
        else
        {
            System.out.println("Can't play in mode " + state);
        }
    }

    private void stopPlayRecord()
    {

        if (frameTimer != null)
        {
            frameTimer.stop();
        }
        state = StreamerState.STOP;
        fireEvent(null, null, KinectEvent.KINECT_STOPPED);
    }

    private ArrayList<KinectListener> kinectListenerList = new ArrayList<KinectListener>();

    public void addKinectListener(KinectListener kinectListener)
    {
        kinectListenerList.add(kinectListener);
    }

    public void removeKinectListener(KinectListener kinectListener)
    {
        kinectListenerList.remove(kinectListener);
    }

    @Override
    public void kinectChange(Sequence sourceSequence, KinectData kinectData, KinectEvent kinectEvent)
    {

        fireEvent(sourceSequence, kinectData, kinectEvent);

    }

    private void fireEvent(Sequence sourceSequence, KinectData kinectData, KinectEvent kinectEvent)
    {
        for (KinectListener kl : kinectListenerList)
        {
            kl.kinectChange(sourceSequence, kinectData, kinectEvent);
        }

    }

    public void stepPlay()
    {
        mainPanel.getButtonStepPlayBack().doClick();
        // stepButton.doClick();
    }

    public void stopRecordedPlay()
    {
        mainPanel.getBtnStopPlayback().doClick();
    }

    public KinectLiveRecorder getLiveRecorder()
    {
        return kinectLiveRecorder;
    }

    public boolean isRecording()
    {
        return (getLiveRecorder() != null);
    }

    public String getPlayingFolder()
    {
        return experimentRecordSelectedBaseFile;
    }

    public void startLive()
    {

        mainPanel.getBtnPlayLive().doClick();
        // playLiveButton.doClick();

    }

    public void stopLive()
    {
        final KinectLiveDriver kinect = KinectLiveDriver.getInstance();

        if (kinect != null)
            kinect.stop();

        state = StreamerState.STOP;
        fireEvent(null, null, KinectEvent.KINECT_STOPPED);
    }

}
