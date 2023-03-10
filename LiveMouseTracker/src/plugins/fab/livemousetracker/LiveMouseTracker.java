/**
  	@author Fabrice de Chaumont
 	copyright Fabrice de Chaumont @ Institut Pasteur

 	This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package plugins.fab.livemousetracker;

import java.awt.Color;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import javax.swing.BoxLayout;
import javax.swing.JOptionPane;

import org.joda.time.DateTime;

import icy.file.FileUtil;
import icy.file.Saver;
import icy.gui.frame.IcyFrame;
import icy.gui.frame.IcyFrameEvent;
import icy.gui.frame.IcyFrameListener;
import icy.gui.frame.progress.AnnounceFrame;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.main.Icy;
import icy.painter.Overlay;
import icy.plugin.abstract_.Plugin;
import icy.plugin.abstract_.PluginActionable;
import icy.preferences.XMLPreferences;
import icy.roi.BooleanMask2D;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.roi.ROIUtil;
//import icy.roi.BooleanMask2D;
//import icy.roi.ROI;
//import icy.roi.ROI2D;
import icy.sequence.Sequence;
import icy.system.profile.Chronometer;
import icy.system.thread.ThreadUtil;
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;
import icy.type.point.Point3D;
import loci.formats.FormatException;
import plugins.fab.azure.kinect.TestAzureKinectDriverFabMultiDoubleCam;
//import plugins.fab.azure.kinect.TestAzureKinectDriverFab3D;
//import plugins.fab.azure.kinect.TestAzureKinectDriverFabMultiDoubleCam;
import plugins.fab.kinectdriver.KinectData;
import plugins.fab.kinectdriver.KinectEvent;
import plugins.fab.kinectdriver.KinectListener;
import plugins.fab.kinectdriver.KinectStreamer;
//import plugins.fab.kinectdriver.KinectStreamer.StreamerState;
import plugins.fab.livemousetracker.MPEGRecorder.MPEGTimeLapseRecorder;
import plugins.fab.livemousetracker.detection.MouseDetection;
import plugins.fab.livemousetracker.detection.MouseDetector;
import plugins.fab.livemousetracker.device.avisoft.AviSoftEventReceiver;
import plugins.fab.livemousetracker.device.sensor.SensorMonitor;
import plugins.fab.livemousetracker.device.ttl.TTLEventListener;
import plugins.fab.livemousetracker.device.ttl.TTLSynchronizer;
import plugins.fab.livemousetracker.device.ttl.TTLSynchronizer.TTL_SIGNAL;
/* THERMAL
import plugins.fab.livemousetracker.device.thermalcamera.MPEGTimeLapseThermalRecorder;
import plugins.fab.livemousetracker.device.thermalcamera.ThermalCameraCapture;
import plugins.fab.livemousetracker.device.thermalcamera.ThermalOverlay;
*/
import plugins.fab.livemousetracker.experiment.EventLog;
import plugins.fab.livemousetracker.experiment.Experiment;
import plugins.fab.livemousetracker.identity.CachedAnimalMachineLearningManager;
import plugins.fab.livemousetracker.identity.DiadicBlackAndWhiteIdentity;
import plugins.fab.livemousetracker.identity.MultiIdentityAgentManager;
import plugins.fab.livemousetracker.listener.LiveTrackerListener;
import plugins.fab.livemousetracker.liveanalysis.chronogram.ChronoConstant;
import plugins.fab.livemousetracker.liveanalysis.client.NetworkResultServer;
import plugins.fab.livemousetracker.machinelearning.MachineLearningDetectionFiltering;
import plugins.fab.livemousetracker.machinelearning.MachineLearningMonitor;
import plugins.fab.livemousetracker.machinelearning.MachineLearningSetBuilder;
import plugins.fab.livemousetracker.machinelearning.MachineLearningSubPartBuilder;
import plugins.fab.livemousetracker.machinelearning.MachineLearningTrackIdentity;
import plugins.fab.livemousetracker.machinelearning.MachineLearningTrackIdentityThread;
import plugins.fab.livemousetracker.misc.Clock;
import plugins.fab.livemousetracker.morpho.MorphoROITools;
import plugins.fab.livemousetracker.overlay.AbsoluteHint;
import plugins.fab.livemousetracker.overlay.DebugOverlay;
import plugins.fab.livemousetracker.overlay.Event;
import plugins.fab.livemousetracker.overlay.EventOverlay;
import plugins.fab.livemousetracker.overlay.PerfLoggerOverlay;
import plugins.fab.livemousetracker.overlay.ThreadMonitorOverlay;
import plugins.fab.livemousetracker.overlay.TrackPoolOverlay;
import plugins.fab.livemousetracker.perf.PerformanceMonitor;
import plugins.fab.livemousetracker.remote.event.UDPEventReceiver;
import plugins.fab.livemousetracker.remote.remoteidentitycontrol.RFIDIdentityControl;
import plugins.fab.livemousetracker.remote.rfidstop.RFIDRemoteStop;
// UNRELEASED MULTI
import plugins.fab.livemousetracker.remotearena.server.LMTRemoteAreaServer;
import plugins.fab.livemousetracker.remotearena.server.RegisteredArenaClient;
import plugins.fab.livemousetracker.rfid.RFIDAntenna;
import plugins.fab.livemousetracker.rfid.RFIDManager2;
import plugins.fab.livemousetracker.rfid.RFIDSolver2;
import plugins.fab.livemousetracker.serial.SerialDriverPlugin;
import plugins.fab.livemousetracker.splitter.DetectionSplitter3Optimized;
import plugins.fab.livemousetracker.track.AnimalPool;
import plugins.fab.livemousetracker.track.AnonymousPool;
import plugins.fab.livemousetracker.track.TrackContainer;
import plugins.fab.livemousetracker.track.TrackSegment;
//UNRELEASED PERSPECTIVE
import plugins.fab.livemousetracker.transform.PerspectiveCompensator;
import plugins.kernel.roi.roi2d.ROI2DArea;
import plugins.kernel.roi.roi2d.ROI2DPolygon;
import plugins.kernel.roi.roi2d.ROI2DRectangle;
import weka.classifiers.Classifier;
import weka.core.Instances;


public class LiveMouseTracker extends PluginActionable
implements KinectListener, ActionListener, IcyFrameListener {

	String version = "2022 PREVIEW";
	/** Warning: This depth sequence is the one from the kinect, It's not a Z-corrected version. Use LiveMouseTracker.depthImage instead */
	public static final boolean DISPLAY_DEPTH_SEQUENCE = false;
	public static final boolean DISPLAY_DIF_INFRA_SEQUENCE = false;
	public static final boolean DISPLAY_DIF_DEPTH_SEQUENCE = false;
	public static final boolean DISPLAY_TAIL_SEQUENCE = false;
	public static final boolean DISPLAY_BACKGROUND_SEQUENCE = false;
	public static final boolean DISPLAY_SUBSTRACTED_BACKGROUND_SEQUENCE = false;
	public static final boolean DISPLAY_REVERT_3D_DEPTH_AND_BACKGROUND = false;

	/** Removes from the learning the animal that have been be split.*/
	public static final boolean DO_NOT_LEARN_FROM_ANIMAL_IN_CONTACT = true;
	public static final boolean SHOW_MACHINE_LEARNING_EVALUATION = false;
	public static final boolean SOUND_ENABLED = false;

	/** This filtering const is used in the detector of wrong detection */
	public static float FILTERING_ANIMAL_VS_UNKNOWN_THRESHOLD = 1; //0.95f; // 0.7 // 1 error in an animal track is enough to break identity. Should be fixed.
	public static boolean MACHINE_LEARNING_DETECTION_ERROR_ENABLED = true;
	/** Number of default latency frame of the RFID reading (*33ms to get the latency)*/
	public static final int RFID_DEFAULT_LATENCY = 3;
	public static final float RFID_ANTENNA_RAY_MULTIPLICATOR = 1f;
	public static String BASE_FOLDER = "c:/live mouse tracker data/";

	public static int SECONDARY_THREAD_PRIORITY = Thread.NORM_PRIORITY; //Thread.MIN_PRIORITY;

	public static boolean SAVE_MEDALLON = false;
	public static boolean PERSPECTIVE_TRANSFORM = false;

	/*
	 * MACHINE LEARNING
	 */

	public static int NB_SIGNATURE_HISTO_BIN = 16;
	// identity
	public static int MIN_TRACK_LENGTH_TO_PROCESS_ID_CHECK = 60; // 30;
	public static final boolean PROCESS_ID_CHECK_ON_OLD_TRACK_EVEN_WITH_WRONG_LENGTH = true;

	public static int ADA_BOOST_ITERATION = 1; // should be 200 // 20 // 5
	public static boolean ADA_BOOST_USE_RESAMPLING = false; // false ; //true;
	public static final boolean LOG_CHAIN = false;

	public static int LEARNING_NB_DETECTION_FOR_LEARNING_PER_ANIMAL =  180 * 30; // equivalent to 3:00 minute
	public static int LEARNING_NB_DETECTION_FOR_LEARNING_SUBPARTS_PER_ANIMAL =  2700*4; // 2700 ; //90 * 30; // equivalent to 1:30 minute
	public static int MIN_TO_START_USING_MACHINE_LEARNING_NB_DETECTION_FOR_LEARNING_SUBPARTS_PER_ANIMAL =  1500; 	/** Max sampling number of detection taken in a track to find its identity. */
	public static int LEARNING_MAX_NUMBER_OF_DETECTION_CONSIDERED_IN_TRACK = 60;
	/** Once all score are evaluated, the best association should have a proportion of XXX over the global problem */
	public static double LEARNING_ID_ASSOCIATION_PROPORTION_THRESHOLD = 95; // 99

	/** Number of sample used to build the error sets. */
	private static int NB_SAMPLE_FOR_ERROR_LEARNING = 600;
	public static final double MIN_INSTANT_SPEED_FOR_SUB_PART_CALCULATION = 1;
	public static double MIN_TIME_WINDOW_FOR_AHEAD_VECTOR_CALCULATION = 10;

	/*
	 * Validation
	 */

	public static int number_of_manual_correction = 0;
	public static int number_of_auto_correction = 0;

	/*
	 * ANIMAL WITH OPTO/WIRE OPTIONS
	 */
//	public static final boolean MANAGE_FILTER_ANIMAL_WITH_CABLE = false;
	public static boolean ANIMAL_IS_WIRED = false;

	/*
	 * LOGS
	 */
	public static final boolean LOG_SPLIT = false;
	public static final boolean LOG_IDENTIFIER = false;
	public static final int NUMBER_OF_FRAME_BEFORE_SEND_DATA_TO_STREAM = 5 * 30 * 60; // 5 minutes
	static ArrayList<EventLog> event2DataBaseLog = new ArrayList<EventLog>();

	/*
	 * Tracking options
	 */
	public static final double MAX_DISTANCE_FOR_TRACKING_DIRECT_ASSO_IN_TRACK_PROLONGATOR = 30; // 20 before 60 pour fast 2 monthes animals

	/*
	 * GENERAL MAP COMPUTATION
	 */
	public static boolean COMPUTE_DEPTH_DIF_MAP = false;
	public static boolean COMPUTE_INFRA_DIF_MAP = false;
	public static boolean BUILD_TAIL_MAP = false;
	public static boolean RFID_ENABLED = true;

	// Option that are or will be integrated in the LiveKinect Driver part

	// Public static final boolean FLIP_X_INPUT_ENABLED = true;
	public static boolean COMPENSATE_Z_WITH_INTENSITY_ENABLED = false;
	public static boolean TRACKING_ENABLED = true;

	/** This value cannot change along time as inits will not be performed*/
	//	Public static int MAX_NUMBER_OF_ANIMALS_AT_INIT = 12;
	/** This value can change along time*/
	public static int MAX_NUMBER_OF_ANIMALS = 4;

	// General options and final parameters

	public static boolean TRACKING_IDENTITY_RECOVERY_ENABLED = true; //true; //false; // true avant
//	public static boolean PROBLEM_GENERATION_ENABLED = false;
	/** Enable Computation of histogram same as the one in IdTracker */
	public static boolean ID_TRACKER_LIKE_HISTOGRAM_FEATURES_ENABLED = false;

	public static boolean TRACK_REJECTED_TRACKING_ENABLED = true; // TRUE
	private static int TRACK_REJECTED_LENGTH = 60;		// frames before stating that the track is an error track and should be integrated in depth map

	public static boolean SHOW_SMALL_SPURIOUS_DETECTION_ENABLED = false;

	public static boolean CORRECT_DEPTH_INVALID_VALUES = true;
	/** if true, the background is not updated anymore*/
	public static boolean LOCK_BACKGROUND = false;

	//static int INIT_LEARNING_TIME_POINT = 300;
	static int INIT_LEARNING_TIME_POINT = 30* 3 ; // 160;

	// detection

	public static int DEPTH_SENSITIVITY = 5;
	/** If the detection contain more pixel than the MAX_SIZE_CANDIDATE, the detection is said "too big" to be a detection
	 * and the system will try to split it.Should be learned. Should be smart to de activate if no mouse is missing,
	 * and to activate if a mouse is too big and mice are missing.
	 *  */
	public static int MAX_SIZE_OF_CANDIDATE_DETECTION = 1000; //1500; // // 600 , 1100; // 800: 4 animaux rec

	/** If this value is big, animals may enter accidently into the background map. */
	public static int MIN_SIZE_SEG_OK = 100; // 30 // normalement 130 // 50

	/** Instead of splitting, reject detection */
	public static boolean REJECT_DETECTION_IF_SPLIT = false;
	/** volume that the detection should be tried to reach during split */
	public static final double DETECTION_SPLIT_TARGET_VOLUME = 31000; // 60500; // 31000
	public static final boolean ENABLE_DETECTION_POST_PROCESS = false;
	/** if a too big detection is above 150*150 pixels then we assume that is an external big object */
	public static final int TOO_BIG_DETECTION_REJECT_SIZE = 150*150;

	public static final boolean DRAW_DETECTION_AXIS = true;
	public static final boolean DRAW_DETECTION_EYES_NOSE_EAR_ROIS = true;
	public static final int NUMBER_OF_FRAME_USED_FOR_BACKGROUND_INIT = 1; // 150
	public static final boolean DISPLAY_MESSAGE_EVENT = true;

	/** If an object is too big, but only one track is accessing this detection, we accept it. */
	public static boolean ACCEPT_TOO_BIG_DETECTION_IF_ONLY_ONE_TRACK_CONCURRENCY_IN_SPLIT = true;

	public static boolean TAIL_DETECTION_ENABLED = false;
	public static boolean TAIL_TRACKING_ASSOCIATION_TO_DETECTION_ENABLED = false;
	public static boolean TAIL_FIT_ENABLED = false;

	// FIXME: faire un enum de ces 2 strategies l�.
	public static final boolean CREATE_NEW_TRACK_AFTER_SPLIT_WITH_ID_CONTINUITY = true;
	public static final boolean USE_MACHINELEARNING_CACHE = true;
	private static final boolean MANAGE_FRAME_DROP = false;
	public static final boolean HEAD_TAIL_MACHINE_LEARNING = true;
	public static boolean USE_MULTIPLE_IDENTITY_RECOVERY_WITH_MACHINE_LEARNING = true; // false for diseapearring animals

	private static boolean SAVE_BACKGROUND = false;
	public int saveBackgroundEachNumberOfFrame = 30*60;


	public static final int MAX_IMAGES_IN_REMOTE_ARENA_IMAGELIST = 5;
	private static boolean DIADIC_BLACK_AND_WHITE_NO_RFID_EXPERIMENT = false;
	public static boolean DRAW_ANONYMOUS_TRACKS = false;

	public static float HEAD_ML_SWAP_THRESHOLD = 0.8f; //0.38f;
	private static boolean DISPLAY_LOG_IN_CONSOLE = true;
	public static boolean MODE_TEST_ANTENNA = false;
	public static boolean BREAK_TRACK_CONTINUITY_AFTER_ANIMAL_CONTACT = false;

	public static boolean SHOW_KINECT_GUI = false;


	public static boolean ASSIGN_ANIMALS_ON_INIT = false;

	public static int NBFRAME_TRACK_WINDOW_DISPLAY = 5 * 30; // s * 30 of display of previous tracks.

	// 	networking

	public static boolean ENABLE_TCP_LIVE_DATA_SERVER = true;


//	public static HeadDetectionMethod HEAD_DETECTION_METHOD = HeadDetectionMethod.AUTO;

	boolean initDone = false; // Check if the init process is finished.
	boolean lutInfraDone = false;

	static int lastMainThreadComputationTimeMs = 0;
	private boolean pauseAllProcess = false;

//	public static ROI2DArea ROICage = null;
//	public static ROI2DArea ROICageFloor = null;

	public static int getLastMainThreadComputationTimeMs() {
		return lastMainThreadComputationTimeMs;
	}

	MachineLearningMonitor machineLearningMonitor ;

	static ArrayList<AbsoluteHint> absoluteHintArrayList = new ArrayList<AbsoluteHint>();

	public static ArrayList<AbsoluteHint> getAbsoluteHintArrayList() {
		return absoluteHintArrayList;
	}

	public static TrackContainer trackContainer = null;
	
	public static ArrayList<FrameInfo> frameInfoList = new ArrayList<FrameInfo>();

//	public static ColorMode ANIMAL_COLOR_DETECTION_MODE = ColorMode.AUTO_OR_MIX;


	/** merged data */
	private static Sequence infraOut = null;
	private static Sequence depthOut = null;

	private static Sequence thermalSequence = null;

	// FIXME: put it with other sequence on top, use that sequence instead of the
	// existing one provided by the driver kinect for display (will be slower)
//	static Sequence infraMergedOut = null;
//	static Sequence depthMergedOut = null;

	static BackgroundHeightMapBuilder backgroundHeightMapBuilder = null ;

	Sequence difDepthInTimeSequence = null;
	IcyBufferedImage previousDepthImage = null;

	Sequence tailSequence = null;
	IcyBufferedImage tailImage = null;

	Sequence difInfraInTimeSequence = null;
	IcyBufferedImage previousInfraImage = null;

	/** Current time in sequence */
	public static Clock clock = new Clock();

	/** Sequence that holds the sum of differences */
	Sequence difDepthCumulatedSequence = null;

	public static RFIDManager2 rfidManager;

	public static SymetryAngleFinder symetryAngleFinder = new SymetryAngleFinder();
	public static ArrayList<BooleanMask2D> tailCandidateArrayList;


	// Monitor Light, sound, humidity, temperature
	public static SensorMonitor sensorMonitor = new SensorMonitor("COM29");
	// Send frame event to TTL via arduino + some live event info.
	public static TTLSynchronizer ttlSynchronizer = new TTLSynchronizer("COM28");
	public static boolean TTL_SYNCHRO_ENABLED = false;
	// Receive TTL event from arduino
	public static boolean TTL_EVENT_LISTENER_ENABLED = false;
	public static TTLEventListener ttlEventListener = new TTLEventListener( "COM27", false );

	/* Number of frame computed in more than 30 ms. */
	public static int nbOver = 0;

	public static Sequence getInfraOut() {
		return infraOut;
	}

	public static Sequence getDepthOut() {
		return depthOut;
	}

	public static BackgroundHeightMapBuilder getBackgroundHeightMapBuider() {
		return backgroundHeightMapBuilder;
	}
//	public static KinectStreamer getKinectStreamer() {
//		return kinectStreamer;
//	}

	public static Sequence getThermalSequence() {
		return thermalSequence;
	}

	RFIDRemoteStop rfidRemoteStop;
	RFIDIdentityControl rfidRemoteIdentityControl;	
	UDPEventReceiver udpEventReceiver;

	/** images coming from local setup */
	ArrayList<ImageKinect> imageQueueList = new ArrayList<ImageKinect>();

	/** images coming from other/remote setups */
	//ArrayList<ImageKinect> imageRemoteQueueList = new ArrayList<ImageKinect>();

	static public PerfLoggerOverlay perfLogger = null;

	public enum CAGE_MODE { MULTI_CLASSIC_16, CLASSIC_16, RATS_25, SUPER_BLOCKS, MULTI_NICO, SIMPLE_JEREMY  } // MULTI_CLASSIC_16 = Philippe

	
	//boolean rat_mode = false;
	//public static CAGE_MODE cageMode = CAGE_MODE.RATS_25;
	//public static CAGE_MODE cageMode = CAGE_MODE.CLASSIC_16;
	public static CAGE_MODE cageMode = CAGE_MODE.CLASSIC_16;

	//KinectStreamer kinectStreamer = new KinectStreamer( SHOW_KINECT_GUI );
	TestAzureKinectDriverFabMultiDoubleCam kinectStreamer = new TestAzureKinectDriverFabMultiDoubleCam( 1 );

	
	public enum CRITICAL_LOOP_STEP
	{
		s01_Start,
		s02_Correct_Z_Map,
		s03_Background_Image,
		s04_Detect_Mouse,
		s05_Spurious_Detection,
		s06_Filter_Detection,
		s07_Filter_Detection,
		s08_Thread_Tasking_Launch,
		s09_Tracking,
		s10_MultitrackIdentity,
		s11_RFID_Manager,
		s12_Record_to_MPEG,
		s13,
		s14,
		s15,
		s16,
		s17,
		s18,
		s19,
		s20
	}

	static EventOverlay eventOverlay;

	IcyFrame mainFrame = new IcyFrame("Live mouse Tracker", true, true , true ,true);

	public static LiveMouseTrackerPanel guiPanel = new LiveMouseTrackerPanel();

	private static ArrayList<LiveTrackerListener> liveTrackerListenerArrayList =
			new ArrayList<LiveTrackerListener>();

	public static int getInitLearningT() {
		return INIT_LEARNING_TIME_POINT;
	}

	public static String getExperimentName()
	{
		return guiPanel.getExperimentNameTextField().getText();
	}


	public static void addEventLogToDataBase( EventLog event )
	{
		synchronized ( event2DataBaseLog )
		{
			event2DataBaseLog.add( event );
		}
	}

	public static void removeEventLogToDataBase( EventLog event )
	{
		synchronized ( event2DataBaseLog )
		{
			event2DataBaseLog.remove( event );
		}
	}

	public static ArrayList<EventLog> getEventLogToDataBaseList()
	{
		synchronized ( event2DataBaseLog )
		{
			return new ArrayList<EventLog>( event2DataBaseLog ) ;
		}
	}

/*
	void registerUserRFID()
	{
		System.out.println("Register User RFIDs.");

		ArrayList<Animal> animalList = trackContainer.animalTrackSegmentPool.animalList;

		for ( Animal animal: animalList )
		{
			int index = trackContainer.animalTrackSegmentPool.animalList.indexOf( animal );
			animal.setRfidID( getUserDefinedRFID( index ) );
		}
	}
	*/

	public void setAnimals( int numberOfAnimals )
	{
		System.out.println("Set number of max animals to " + numberOfAnimals );
		MAX_NUMBER_OF_ANIMALS = numberOfAnimals;
		guiPanel.getNumberOfMaxAnimalTextField().setText( ""+ numberOfAnimals );

		// change the number of known animal on the fly
		if ( trackContainer != null )
		{
			for ( int i = 0 ; i < MAX_NUMBER_OF_ANIMALS ; i++ )
			{
				if ( trackContainer.animalTrackSegmentPool.getAnimalList().size()<i+1 )
				{
					Animal animal = new Animal( ""+(char)( 'A'+i ) );
					if ( SAVE_MEDALLON )
					{
						animal.createMedaillonSequence();
					}
					trackContainer.animalTrackSegmentPool.addAnimal( animal );


					/*
					switch ( i )
					{
					// cage A1:
//					animal.setRfidID("000004064618");
//					animal.setRfidID("000004064568");
//					animal.setRfidID("000004064981");
//					animal.setRfidID("000004064805");

					// Cage D4
					case 0:
						animal.setRfidID("000004065042");
//						animal.setRfidID("000004064966");
						//animal.setRfidID("000004064598");
						//animal.setRfidID("000004065036");
						//animal.setRfidID("000004064857");
						break;
					case 1:
					//	animal.setRfidID("000004064957");
						animal.setRfidID("000004065036");
						break;
					case 2:
						animal.setRfidID("000004064945");
						break;
					case 3:
						animal.setRfidID("000004064847");
						break;
					}
					*/
				}
			}
			// registerUserRFID();
		}

	}

	public void setNumberOfMaxAnimalEditable( boolean isEditable )
	{
		guiPanel.getNumberOfMaxAnimalTextField().setEditable( isEditable );
	}

	private boolean checkIfLMTHasBeenLaunchedWithTheBatchFile()
	{
		return LMTLauncher.launchOK;

		/*
		for ( String arg : Icy.getCommandLineArgs() )
		{
			//System.out.println( arg );
			if ( arg.equals( "-LMTBAT") )
			{
				return true;
			}
		}

		return false;
		*/
	}

	@Override
	public void run() {

		System.out.println("Starting Live Mouse Tracker version " + version );
		new SerialDriverPlugin();

		/*
		 * PUTBACK
		if ( !checkIfLMTHasBeenLaunchedWithTheBatchFile() )
		{
			JOptionPane.showMessageDialog(null,
					"Live Mouse Tracker\n"+
			"\nPlease launch LiveMouseTracker using the LiveMouseTracker.bat file."+
			"\nThis will launch icy with a specific real-time memory configuration."+
			"\nOnce Icy is launched, start again the liveMouseTracker plugin."+
			"\n\nIf the .bat is not working, it means you are using a 32 bit java version instead of the 64bit (provided in zip)"+
		    "\ndon't forget to uninstall java 32 bits before installing the 64 bits version." );
			return;
		}
		*/


		plugin = this;
		kinectStreamer.addKinectListener( this );

		mainFrame.getContentPane().setLayout( new BoxLayout( mainFrame.getContentPane(), BoxLayout.PAGE_AXIS ) );
		mainFrame.getContentPane().add( guiPanel );

		setAnimals( MAX_NUMBER_OF_ANIMALS );

		guiPanel.getExperimentFolderTextField().setText( BASE_FOLDER );
		guiPanel.getStartLiveButton().addActionListener( this );
		guiPanel.getSelect1AnimalButton().addActionListener( this );
		guiPanel.getSelect2AnimalButton().addActionListener( this );
		guiPanel.getSelect3AnimalButton().addActionListener( this );
		guiPanel.getSelect4AnimalButton().addActionListener( this );
		guiPanel.getStopButton().setEnabled( false );
		guiPanel.getStopButton().addActionListener( this );
		guiPanel.getPauseButton().setEnabled( false );
		guiPanel.getPauseButton().addActionListener( this );


		mainFrame.pack();
		mainFrame.setVisible( true );
		mainFrame.addToDesktopPane();
		//mainFrame.setLocation( 0 , 220 );
		mainFrame.center();

		//addTrackerListener( new LiveGateDoorTest() );
		boolean experimentNameOk = false;
		while( !experimentNameOk )
		{
			int experimentRandomNumber = (int) ( Math.random() * 10000f );
			guiPanel.getExperimentNameTextField().setText( "Experiment " + experimentRandomNumber );
			experimentNameOk = ( ! FileUtil.exists( BASE_FOLDER + guiPanel.getExperimentNameTextField() ) );
		}

		mainFrame.addFrameListener( this );

		if ( Icy.getCommandLinePluginArgs().length > 0 )
		{
			System.out.println("Starting from commande line.");
			String experimentName = Icy.getCommandLinePluginArgs()[0];
			String numberOfAnimal = Icy.getCommandLinePluginArgs()[1];
			guiPanel.getExperimentNameTextField().setText( experimentName );
			guiPanel.getNumberOfMaxAnimalTextField().setText( numberOfAnimal );
			startLive();
			Icy.getMainInterface().getMainFrame().setState( Frame.ICONIFIED );
		}

	}

	private XMLPreferences getRFIDComPortPreferences() {
		return this.getPreferences("RFID COM PORTS");
	}

	public static void addTrackerListener( LiveTrackerListener listener ) {

		System.out.println("Tracker Listener added.");
		synchronized ( liveTrackerListenerArrayList ) {
			liveTrackerListenerArrayList.add( listener );
		}

	}

	public static AnonymousPool errorDetectionTrackPool = new AnonymousPool( );

	boolean inMainProcessingLoop = false;
	volatile boolean newImage = false;

	// UNRELEASED PERSPECTIVE
	PerspectiveCompensator perspectiveCompensator = null;

	int counterSkip15fps = 0;

	void mainProcessThread()
	{
		while ( ! processThread.isInterrupted() || !performingShutDown )
		{
			try{

				// new drop frame management
				synchronized ( imageQueueList ) {

					while ( imageQueueList.size() > 10 )
					{
						imageQueueList.remove( imageQueueList.size()-1 );
						System.err.println("FRAME DROP (queue > 10)");
					}

					if ( imageQueueList.size() > 1 )
					{
						System.err.println("[ImageQUEUE] size: " +  imageQueueList.size() );
					}

				}

				/* (made by registeredClient now)
				// drop remote data

				if ( isRemoteModeEnabled() )
				{
					for ( RegisteredArenaClient registeredClient : lmtRemoteAreaServer.getRegisteredClientList() )
					{
						ArrayList<ImageKinect> imageRemoteQueueList = registeredClient.getImageRemoteQueueList();

						synchronized ( imageRemoteQueueList ) {

							while ( imageRemoteQueueList.size() > 3 )
							{
								imageRemoteQueueList.remove( imageRemoteQueueList.size()-1 );
								System.err.println("REMOTE FRAME DROP (queue > 3)");
							}

							if ( imageRemoteQueueList.size() > 1 )
							{
								System.err.println("[REMOTE ImageQUEUE] size: " +  imageRemoteQueueList.size() );
							}
						}
					}
				}
		*/

				//if ( isRemoteModeEnabled() )
				//{
					ArrayList<ImageKinect> allKinectImagesToMerge = new ArrayList<ImageKinect>();

					/*
					// TEMP: FIXME : TODO: ONLY DEAL WITH FIRST CLIENT FOR TESTS
					ArrayList<ImageKinect> imageRemoteQueueList = new ArrayList<ImageKinect>();

					if ( lmtRemoteAreaServer.getRegisteredClientList().size() > 0 )
					{
						RegisteredArenaClient registeredClient = lmtRemoteAreaServer.getRegisteredClientList().get(0 );
						imageRemoteQueueList = registeredClient.getImageRemoteQueueList();
					}*/

					if ( imageQueueList.size() > 0 )
					{
						allKinectImagesToMerge.add( imageQueueList.get( 0 ) );
						imageQueueList.remove( 0 );

						// UNRELEASED MULTI

						if ( isRemoteModeEnabled() )
						{
							for ( RegisteredArenaClient registeredClient : lmtRemoteAreaServer.getRegisteredClientList() )
							{
								ImageKinect remoteImage = registeredClient.getNextRemoteImage();
								if ( remoteImage != null )
								{
									allKinectImagesToMerge.add( remoteImage );
								}

							}
						}


						newImage = true;
					}


					// merge all images
					if ( allKinectImagesToMerge.size() > 0 )
					{
						ImageKinect mergedKinect = mergeKinectImage( allKinectImagesToMerge );
						depthImage = mergedKinect.depthImage;
						infraImage = mergedKinect.infraImage;

						// UNRELEASED PERSPECTIVE
						if ( PERSPECTIVE_TRANSFORM )
						{
							if ( perspectiveCompensator == null )
							{
								perspectiveCompensator = new PerspectiveCompensator();
							}
							infraImage = perspectiveCompensator.compensateInfra( infraImage );
							depthImage = perspectiveCompensator.compensateDepth( depthImage );

						}
					}

/*
					{
						RegisteredArenaClient registeredClient = lmtRemoteAreaServer.getRegisteredClientList().get(0 );
						imageRemoteQueueList = registeredClient.getImageRemoteQueueList();
					}
*/


					/*
					if ( imageQueueList.size() > 0 ) //&& imageRemoteQueueList.size() > 0 )
					{
						// This will copy the local image to wait for client.

						ImageKinect localImage = imageQueueList.get( 0 );
						imageQueueList.remove( 0 );
						ImageKinect remoteImage = null;

						if ( imageRemoteQueueList.size() > 0 )
						{
							remoteImage = imageRemoteQueueList.get( 0 );
							imageRemoteQueueList.remove( 0 );
						}
						if ( remoteImage == null )
						{
							// copy image
							remoteImage = localImage;
						}

						mergedImage = mergeKinectImage( );

						depthImage = mergeImage(
								localImage.depthImage ,
								remoteImage.depthImage );
						infraImage = mergeImage(
								localImage.infraImage ,
								remoteImage.infraImage );
//						imageQueueList.remove( 0 );
//						imageRemoteQueueList.remove( 0 );
						newImage = true;
					}
				}else
				{
					// just local mode.
					if ( imageQueueList.size() > 0 )
					{

						boolean TEST_MODE_FAKE_REMOTE = false;
						if ( TEST_MODE_FAKE_REMOTE )
						{

							infraImage = mergeImage( imageQueueList.get( 0 ).infraImage,
									imageQueueList.get( 0 ).infraImage );
							depthImage = mergeImage( imageQueueList.get( 0 ).depthImage,
									imageQueueList.get( 0 ).depthImage );
							infraOut.setImage( 0, 0, infraImage );
							depthOut.setImage( 0 , 0 , depthImage );
						}else

	{
							infraImage = imageQueueList.get( 0 ).infraImage;
							depthImage = imageQueueList.get( 0 ).depthImage;
						}
						imageQueueList.remove( 0 );
						newImage = true;
					}
				}*/

				if ( newImage )
				{

					infraOut.setImage( 0 , 0, infraImage );
					depthOut.setImage( 0 , 0, depthImage );


					if (!lutInfraDone && (infraOut.getFirstViewer() != null))
					{
						if ( cageMode == CAGE_MODE.RATS_25 )
						{
							infraOut.getFirstViewer().getLut().getLutChannel(0).setMinMax( 0, 12750 );
						}
						
						if ( cageMode == CAGE_MODE.CLASSIC_16 )
						{
							// TODO TODAY : camera kinect v2
							//infraOut.getFirstViewer().getLut().getLutChannel(0).setMinMax( 0, 32000 );
							// TODO TODAY : camera kinect azure
							infraOut.getFirstViewer().getLut().getLutChannel(0).setMinMax( 0, 3200 );
						}
						
						if ( cageMode == CAGE_MODE.MULTI_CLASSIC_16 )
						{
							infraOut.getFirstViewer().getLut().getLutChannel(0).setMinMax( 0, 5000 );
						}

						if ( cageMode == CAGE_MODE.MULTI_NICO )
						{
							infraOut.getFirstViewer().getLut().getLutChannel(0).setMinMax( 0, 5000 );
						}
						
						
						//infraOut.getFirstViewer().getLut().getLutChannel(0).setMinMax( 0, 32000 );
						lutInfraDone = true;
					}

					inMainProcessingLoop = true;

					boolean processFrame = true;


					if ( cageMode == CAGE_MODE.RATS_25 )
					{
						/*
						counterSkip15fps++;
						if ( counterSkip15fps %2 == 0 ) // 15FPS MODE in RAT MODE
						{
							processFrame = true;
						}else
						{
							processFrame = false;
						}
						*/
					}

					if ( processFrame )
					{
						process();
					}

					inMainProcessingLoop = false;
					newImage = false;
				}

				// relax CPU
				Thread.yield();
			}catch( Exception e )
			{
				System.err.println("****** WARNING: EXCEPTION UNCATCHED IN MAIN PROCESS");
				e.printStackTrace();
			}

		}
	}


	int maxMergeX = 0;
	int maxMergeY = 0;

	/** put the 2 images side by side */
	private ImageKinect mergeKinectImage( ArrayList<ImageKinect> kinectImageArrayList )
	{

		for ( ImageKinect imageKinect : kinectImageArrayList )
		{
			int x = imageKinect.offsetX+imageKinect.infraImage.getWidth();
			int y = imageKinect.offsetY+imageKinect.infraImage.getHeight();
			if ( x > maxMergeX )
			{
				maxMergeX = x;
			}
			if ( y > maxMergeY )
			{
				maxMergeY = y;
			}
		}

		IcyBufferedImage infraImage = new IcyBufferedImage( maxMergeX, maxMergeY, 1 , DataType.USHORT );
		IcyBufferedImage depthImage = new IcyBufferedImage( maxMergeX, maxMergeY, 1 , DataType.USHORT );


		for ( ImageKinect imageKinect : kinectImageArrayList )
		{
			infraImage.copyData( imageKinect.infraImage, null, new java.awt.Point( imageKinect.offsetX, imageKinect.offsetY ) );
			depthImage.copyData( imageKinect.depthImage, null, new java.awt.Point( imageKinect.offsetX, imageKinect.offsetY ) );
		}

		ImageKinect merged = new ImageKinect(infraImage, depthImage, 0,0 );
		return merged;
	}

	/*
	mergeImage
	{
		IcyBufferedImage mergedImage = new IcyBufferedImage(
				local.getWidth()*2,
				local.getHeight(),
				1 , DataType.USHORT );

		mergedImage.copyData( local, null, null );
		mergedImage.copyData( remote, null, new java.awt.Point( local.getWidth(), 0 ) );


		return mergedImage;

	}
	*/
			/*
			 	private IcyBufferedImage mergeImage( MergeMode mergeMode , KinectImage[] kinectImageArray )
			IcyBufferedImage local,
			IcyBufferedImage remote ) {

		IcyBufferedImage mergedImage = new IcyBufferedImage(
				local.getWidth()*2,
				local.getHeight(),
				1 , DataType.USHORT );

		mergedImage.copyData( local, null, null );
		mergedImage.copyData( remote, null, new java.awt.Point( local.getWidth(), 0 ) );


		return mergedImage;
	}
			 */

	// UNRELEASED MULTI

	private boolean isRemoteModeEnabled() {
		if ( lmtRemoteAreaServer== null ) return false;
		return true;
	}


	static DateTime startTime = null;

	public static DateTime getStartTime() {
		return startTime;
	}


	long milliCriticalLoop ;

	private void process() {

		if ( MANAGE_FRAME_DROP )
		{
			clock.increaseAndCorrectTWithDropFrame( frameDrop );
		}else
		{
			clock.increaseT();
		}
		frameDrop =0;

		if ( startTime == null ) startTime = new DateTime();

//		clock.increaseT();

		milliCriticalLoop = System.currentTimeMillis();

		if ( !performingShutDown )
		{
//			inMainProcessingLoop = true;
//			infraOut.beginUpdate();
//			Icy.getMainInterface().getInspector().getOutputConsolePanel().clearLogButton.doClick();
			processCurrentT();
//			infraOut.endUpdate();
		}
//		inMainProcessingLoop = false;

		lastMainThreadComputationTimeMs = (int) ( System.currentTimeMillis() - milliCriticalLoop );
		if ( lastMainThreadComputationTimeMs > 30 )
		{
			System.out.println("COMPUTATION WARNING: lastMainThreadComputationTimeMs > 30ms. Frame: " + getT() + " - " + lastMainThreadComputationTimeMs + " ms");
		}

		fireEndOfFrameEvent();
	}

	private void fireEndOfFrameEvent() {

		synchronized ( liveTrackerListenerArrayList ) {

			for ( LiveTrackerListener lts : new ArrayList<LiveTrackerListener>( liveTrackerListenerArrayList ) )
			{
				lts.liveTrackerEndOfFrame( this );
			}
		}

	}

	private void fireEndOfInitEvent() {

		synchronized ( liveTrackerListenerArrayList ) {

			for ( LiveTrackerListener lts : new ArrayList<LiveTrackerListener>( liveTrackerListenerArrayList ) )
			{
				lts.liveTrackerPostInitEvent( this );
			}
		}
	}

	private void firePostFilterDetection( ArrayList<MouseDetection> rawMouseDetectionList,
			int t, IcyBufferedImage depthImage )
	{
		synchronized ( liveTrackerListenerArrayList )
		{
			for ( LiveTrackerListener lts : new ArrayList<LiveTrackerListener>( liveTrackerListenerArrayList ) )
			{
				lts.liveTrackerPostProcessDetectionFiltering( rawMouseDetectionList, t, depthImage );
			}
		}

	}

	Instances trainingSet = null;

	MachineLearningDetectionFiltering mlDetectionFiltering = null;

	//public static ROI2D cageROI = null;
	public static BooleanMask2D cageROIMask = null;
	public static BooleanMask2D cageFloorMask = null;

	Sequence substractedBackgroundSequence = new Sequence("BackGround Substracted");
	private boolean learningAnimalInitDone = false;

	/** Those copy are NOT the same as the one provided in the viewer. (They are z compensated, and Z filtered)
	 * Don't access the one from the viewer as they are managed by the kinect streamer.
	 * */
	public static IcyBufferedImage infraImage;
	public static IcyBufferedImage depthImage;


//	Thread recordMPEGStreamThread = null;

	//initLearning();
	Thread initLearningThread = null;
	Runnable initLearningRunnable = new Runnable() {
		@Override
		public void run() {

			System.out.println("Init learning...");
			initLearning();

		}
	};

	Runnable clearConsoleRunnable = new Runnable() {
		@Override
		public void run() {
			Icy.getMainInterface().getInspector().getOutputConsolePanel().clearLogButton.doClick();
		}
	};

	Thread refreshEstimatorsThread = null;
	Runnable refreshEstimatorsRunnable = new Runnable() {
		@Override
		public void run() {

			try
			{
				ArrayList<TrackSegment> tracks = trackContainer.animalTrackSegmentPool.getTrackSegments();

				double total = 0;
				int nb = 0;

				for ( TrackSegment ts : tracks )
				{
					for ( MouseDetection detection : ts.getDetectionList() )
					{
						if ( !detection.isBuiltByDetectionSplitter() )
						{
							total += detection.getSurface();
							nb++;
						}
					}
				}
				if ( nb > 0 )
				{
					MAX_SIZE_OF_CANDIDATE_DETECTION = (int)( 1.4d *  total / (double)nb );
					System.out.println("NEW MAX_SIZE_OF_CANDIDATE_DETECTION = " + MAX_SIZE_OF_CANDIDATE_DETECTION );
				}
			}catch( Exception e )
			{
				System.out.println("[FAIL] Fail at creating the new max size of candidate detection.");
				e.printStackTrace();
			}
		}
	};


	Thread errorRefresherThread = null;
	Runnable errorRefresherRunnable = new Runnable() {
		@Override
		public void run() {
			if ( LiveMouseTracker.MACHINE_LEARNING_DETECTION_ERROR_ENABLED )
			{
				refreshErrorSet();
			}
		}
	};

	/*
	Thread watchdogThread = null;
	Runnable watchdogRunnable = new Runnable() {
		@Override
		public void run() {
			while (  true )
			{
				if ( frameDrop > 50 )
				{
					System.out.println("WATCHDOG STARTED - frameDrop: " + frameDrop );
					for ( Thread thread:  Thread.getAllStackTraces().keySet() )
					{
						System.out.println( "*** THREAD : " + thread.getName() );
						for ( StackTraceElement txt: thread.getStackTrace() )
						{
							System.out.println( txt );
						}
					}
				}

				try {
					Thread.sleep( 1000 );
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}
		}
	};
*/

	//public static ExecutorService threadExecutor = Executors.newSingleThreadExecutor();
	public static ThreadPoolExecutor threadExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);

	Thread saveTrackSQLThread = null;
	Runnable saveTrackSQLRunnable = new Runnable() {
		@Override
		public void run() {
			saveTracks( true );
		}
	};

	Thread computeSubPartClassifierThread = null;
	Runnable computeSubPartClassifierRunnable = new Runnable() {
		@Override
		public void run() {
			try{
				refreshSubPartClassifier();
			}
			catch ( NullPointerException e )
			{
				System.out.println("WARNING ERROR IN SUBPART CLASSIFIER");
				e.printStackTrace();
			}
		}
	};

	Thread cacheMachineLearningThread = null;
	Runnable cacheMachineLearningRunnable = new Runnable() {
		@Override
		public void run() {
			if ( USE_MACHINELEARNING_CACHE )
			{
				System.out.println("Starting cache of all animals. t=" + getT() );
				CachedAnimalMachineLearningManager.createCache( trackContainer.animalTrackSegmentPool.animalList , false ); // evaluate );
			}
		}
	};

	CRITICAL_LOOP_STEP criticalStep;

	//static public PerformanceMonitor performanceMonitor;

	private void processCurrentT() {

		int t = clock.getT();
		nbImageProcessed++;


		//performanceMonitor = new PerformanceMonitor("Main critical loop #"+t);


		criticalStep = CRITICAL_LOOP_STEP.s01_Start;



		//Chronometer chrono = new Chronometer( "CriticalSteps -------- frame:  " + t  + " --- " );


		//System.out.println("Frame:"+t);

		if ( TTL_SYNCHRO_ENABLED )
		{
			if ( t < 10 )
			{
				ttlSynchronizer.sendTTL( TTLSynchronizer.TTL_SIGNAL.SYNCHRO_START );
			}
			ttlSynchronizer.sendTTL( TTLSynchronizer.TTL_SIGNAL.SYNCHRO_FRAME );
		}
		//performanceMonitor.stepDone("TTL");

		FrameInfo frameInfo = new FrameInfo(
				t , new Date( Calendar.getInstance().getTime().getTime() ), pauseAllProcess
				, sensorMonitor.getTemperature(), sensorMonitor.getHumidity(), sensorMonitor.getSoundLevel(),
				sensorMonitor.getLightVisible(), sensorMonitor.getLightInfraredAndVisible()
				);
		currentFrameInfo = frameInfo;
		synchronized ( frameInfoList ) {
			frameInfoList.add( frameInfo );
		}
		//performanceMonitor.stepDone("Frame info");

		//if ( RFID_ENABLED )
		{
			/*
			// LOAD_RFID_EVENT_FROM_FILE
			if ( getKinectStreamer().getState() == StreamerState.PLAYFILE )
			{
				rfidManager.loadEvents( t );
				for ( RFIDEvent rfidEvent : rfidManager.getEventList() )
				{
					addEvent( new Event( "RFID:" +rfidEvent.getId(), Color.YELLOW, rfidEvent.getLocation() , 2 ));
				}
			}
			*/
		}

		debugOverlay.clear();
		synchronized ( absoluteHintArrayList ) {
			absoluteHintArrayList.clear();
		}
		//performanceMonitor.stepDone("Clear debug overlays and hints");

		cleanTemporaryROIs();

		//performanceMonitor.stepDone("Clean tmp ROIs");

		criticalStep = CRITICAL_LOOP_STEP.s02_Correct_Z_Map;


		//correctInvalidZValue( depthImage );
		//compensateZIntensityError( depthImage , infraImage );
		compensateZIntensityError2( depthImage , infraImage );

		//performanceMonitor.stepDone("Correct Z map");

		if ( pauseAllProcess ) return;

		IcyBufferedImage depthDifInTimeImage = difDepthInTimeSequence.getImage( 0 , 0 );
		IcyBufferedImage infraDifInTimeImage = difInfraInTimeSequence.getImage( 0 , 0 );
		//performanceMonitor.stepDone("Get images");

		// Computes the map Depth_t - Depth_(t-1)
		if ( COMPUTE_DEPTH_DIF_MAP )
		{
			computeDepthDifferentialImageInTime( depthImage , depthDifInTimeImage );
		}

		// Computes the map Depth_t - Depth_(t-1)
		if ( COMPUTE_INFRA_DIF_MAP )
		{
			computeInfraDifferentialImageInTime( infraImage , infraDifInTimeImage );
		}



		// Computes the dif cumulated map
		//computeDepthDifCumulated( );

		criticalStep = CRITICAL_LOOP_STEP.s03_Background_Image;
		//System.out.println( "entering critical : "+criticalStep.name() );

		// Integrate the new depth Image to build the background map.
		backgroundHeightMapBuilder.integrateNewDepthMapImage( depthImage );
		//performanceMonitor.stepDone("Background height map");

		if ( !backgroundHeightMapBuilder.isReady() ) return;

		// buil tail map and connect

		if( BUILD_TAIL_MAP )
		{
//			tailCandidateArrayList = buildTailMap( );
		}

		if ( DISPLAY_SUBSTRACTED_BACKGROUND_SEQUENCE )
		{
			substractedBackgroundSequence.setImage( 0, 0, backgroundHeightMapBuilder.getSubstractedImage() );
		}

		//chrono.displayMs();
		criticalStep = CRITICAL_LOOP_STEP.s04_Detect_Mouse;
		//System.out.println( "entering critical : "+criticalStep.name() );

		// detect mice
		ArrayList<MouseDetection> rawMouseDetectionList =
				mouseDetector.detectMice( depthImage , infraImage ,t , tailCandidateArrayList );
		//performanceMonitor.stepDone("Detection done");

		// try to break too big detection using tracking
		//chrono.displayMs();
		criticalStep = CRITICAL_LOOP_STEP.s05_Spurious_Detection;
		//System.out.println( "entering critical : "+criticalStep.name() );

		for ( ROI2DArea tooBigDetection : mouseDetector.getTooBigSpuriousMaskList() )
		{
			//System.out.println("T from LiveMouseTracker = " + t );

			// check if detection is far too big and correspond to artefact of the kinect
			// specifically the horizontal bar that can appear sometimes
			/*
			if ( tooBigDetection.getBounds2D().getWidth() > 260 )
			{
				correctBackGround( depthImage , tooBigDetection.getBooleanMask( true ) );
				System.out.println("[KINECT WARNING]: Large artefact bar found. Reset background");
				LiveMouseTracker.resetBackGround();
				break;
			}
			*/

			if ( REJECT_DETECTION_IF_SPLIT )
			{
				correctBackGround( depthImage , tooBigDetection.getBooleanMask( true ) );
			}else
			{ 	// perform split
				//Chronometer chronoSplitDetection = new Chronometer("detection splitter *** ");
				//performanceMonitor.stepDone("Detection done");
				rawMouseDetectionList.addAll(
						DetectionSplitter3Optimized.splitDetectionWithSeed( tooBigDetection ,
						null ) );
				//performanceMonitor.stepDone("Splitter done");
				//	chronoSplitDetection.displayMs();
			}

		}
		//System.out.println("Nb raw mouse detection: "+rawMouseDetectionList.size() );
		{ // Intentionally make the system blind the system for test purposes
			if ( trackPoolOverlay.getLooseTrackCounter() > 0 )
			{
				rawMouseDetectionList.clear(); // don't process detection
			}
			trackPoolOverlay.decreaseLooseTrackCounter();
		}

		trackPoolOverlay.frameTick();

		// filter detection
		//chrono.displayMs();
		criticalStep = CRITICAL_LOOP_STEP.s06_Filter_Detection;
		//System.out.println( "entering critical : "+criticalStep.name() );

		// reject very big detection (after split) that can occur when nest is tracked/melt with animals
		// or when the kinect is having problem
		for ( MouseDetection rawDetection : rawMouseDetectionList )
		{
			if ( rawDetection.getBooleanMask().bounds.getWidth() > 150
					|| rawDetection.getBooleanMask().bounds.getHeight() > 150
					)
			{
				System.out.println("[TOO BIG DETECTION (after split) WARNING]: Too large detection found. Reset background");
				LiveMouseTracker.resetBackGround();
				break;
			}
		}
		
		// reject reflexion detection
		//cage
		if ( cageROIMask != null && cageFloorMask != null )
		{
			// Roi fully in cage-cagefloor is rejected
			
			for ( MouseDetection rawDetection : new ArrayList<MouseDetection>(rawMouseDetectionList) )
			{
				
				//if ( ! cageFloorMask.contains( rawDetection.getBooleanMask() ) )			
				
				Point3D massCenter = rawDetection.getMassCenter();
				//boolean detectionInCageFloor = cageFloorMask.contains( (int) massCenter.getX() , (int) massCenter.getY() );
				boolean detectionInCageFloor = cageFloorMask.intersects( rawDetection.getBooleanMask() );
						
				if ( ! detectionInCageFloor )
				{
					rawMouseDetectionList.remove( rawDetection );
					rawDetection.getROI2DArea().setColor( Color.pink );
					correctBackGround( depthImage, rawDetection );
					//System.out.println( "time : " + t + " - Reflexion removed");
					
					Event event = new Event( "Reflexion removed", Color.PINK,							
							rawDetection.getMassCenter().toPoint2D() );
					/*
					LiveMouseTracker.addEvent( event );
					*/
				}
			}			
		}
		
		
		//performanceMonitor.stepDone("Check reset background");


		filterDetection( rawMouseDetectionList , t , depthImage );
		//performanceMonitor.stepDone("Filter detection");
		//chrono.displayMs();
		//criticalStep = CRITICAL_LOOP_STEP.s07_Filter_Detection;
		//System.out.println( "entering critical : "+criticalStep.name() );

		postFilterNumberOfAnimals( rawMouseDetectionList );
		firePostFilterDetection( rawMouseDetectionList , t, depthImage );
		//performanceMonitor.stepDone("post filter detection");

//		for ( MouseDetection md : rawMouseDetectionList )
//		{
//			System.out.println("Built by splitter: " + md.isBuiltByDetectionSplitter() );
//			System.out.println("Used for ML: " + md.canBeUsedForLearning() );
//		}

//		for ( int i = rawMouseDetectionList.size() -1 ; i >= 0 ; i-- ) // Remove detection containing a reflexion (saturated)
//		{
//			RawMouseDetection r = rawMouseDetectionList.get( i );
//			if ( r.getMaxInfraIntensity() > 65000 )
//			{
//				rawMouseDetectionList.remove( r );
//				infraOut.removeROI( r.getROI2DArea() );
////				r.getROI2DArea().setColor( Color.black );
////				r.getROI2DArea().setName("tmp Specular");
//			}
//
//		}

		//filterDetectionDueToReflexion( rawMouseDetectionList , t );

		// Set the last DetectionList for overlay display.
		// FIXME: redesign the detection display
//		synchronized( trackPoolOverlay.lastDetectionList )
//		{
//			trackPoolOverlay.lastDetectionList.clear();
//
//			lastDetectionList.clear();
//			for ( MouseDetection rawMouseDetection : rawMouseDetectionList )
//			{
//				if ( rawMouseDetection.getROI2DArea().getName().startsWith("seg ok") )
//				{
//					lastDetectionList.add( rawMouseDetection );
//				}
//			}
//	}
		//System.out.println("Nb raw mouse detection filtered: "+rawMouseDetectionList.size() );
		//chrono.displayMs();
		//criticalStep = CRITICAL_LOOP_STEP.s08_Thread_Tasking_Launch;
		//System.out.println( "entering critical : "+criticalStep.name() );

		if ( t > INIT_LEARNING_TIME_POINT ) // > and not == so that it can't be missed anymore !
		{
			if ( LiveMouseTracker.MACHINE_LEARNING_DETECTION_ERROR_ENABLED )
			{
				if ( !learningAnimalInitDone )
				{
					threadExecutor.execute( new Runnable() {
						@Override
						public void run() {
							initLearningThread = Util.runSingle2( "init learning" , initLearningThread, initLearningRunnable );
						}
					});

				}
			}

//			ThreadUtil.bgRun( new Runnable() {
//				@Override
//				public void run() {
//					initLearning();
//				}
//			});
		}

		if ( USE_MACHINELEARNING_CACHE )
		{
			if ( t % 200 == 0 )
			{
				threadExecutor.execute( new Runnable() {
					@Override
					public void run() {
						cacheMachineLearningThread = Util.runSingle2( "machine learning cache",cacheMachineLearningThread, cacheMachineLearningRunnable );
					}
				});

				//ThreadUtil.runSingle( cacheMachineLearningRunnable );
				//cacheAllAnimalMachineLearning( false );

			}
		}

		/* THERMAL
		if ( t % (30*60*60) == 0 ) // restart thermal camera every our.
		{
			if ( t != 0 )
			{
				if ( thermalCameraCapture != null )
				{
					thermalCameraCapture.restartCapture();
				}
			}
		}
		*/

		if ( t % 100 == 0 )
		{
//			for ( Animal animal : trackContainer.animalTrackSegmentPool.animalList )
//			{
//				System.out.println("NB TRACKS");
//				System.out.println( "animal " + animal.getTrackSegments().size() );
//
//				//animal.getTrackSegments().size()
//			}

			if ( LiveMouseTracker.HEAD_TAIL_MACHINE_LEARNING )
			{
				Chronometer singleLauncherChrono = new Chronometer("HEAD_TAIL_MACHINE_LEARNING");
				threadExecutor.execute( new Runnable() {
					@Override
					public void run() {
						computeSubPartClassifierThread = Util.runSingle2("Sub par classifiation",  computeSubPartClassifierThread,
								computeSubPartClassifierRunnable );
					}
				});

				singleLauncherChrono.displayMs();
			}
			//ThreadUtil.runSingle( computeSubPartClassifierRunnable );
		}
/*
		if ( t % 100 == 0 )
		{
			watchdogThread = Util.runSingle( "watchdog", watchdogThread , watchdogRunnable );
		}
*/
		if ( t % 1000 == 0 )
		{
			threadExecutor.execute( new Runnable() {
				@Override
				public void run() {
					errorRefresherThread = Util.runSingle2( "error refresher", errorRefresherThread , errorRefresherRunnable );
				}
			});
			//ThreadUtil.runSingle( errorRefresherRunnable );
		}

		if ( t % 200 == 0 )
		{
			threadExecutor.execute( new Runnable() {
				@Override
				public void run() {
					refreshEstimatorsThread = Util.runSingle2( "refresh estimators", refreshEstimatorsThread , refreshEstimatorsRunnable );
				}
			});
		}

		// Save data in streaming.
		if ( t % 500 == 0 && guiPanel.getStreamToSQLCheckBox().isSelected() )
		{
			threadExecutor.execute( new Runnable() {
				@Override
				public void run() {
					saveTrackSQLThread = Util.runSingle2( "saveSQL", saveTrackSQLThread , saveTrackSQLRunnable , Thread.MIN_PRIORITY );
				}
			});
		}

		//performanceMonitor.stepDone("all post-thread started");

		//
		// Perform tracking
		//
		//chrono.displayMs();
		criticalStep = CRITICAL_LOOP_STEP.s09_Tracking;
		//System.out.println( "entering critical : "+criticalStep.name() );

		if ( TRACKING_ENABLED )
		{
			track( t,  rawMouseDetectionList );
		}
		//performanceMonitor.stepDone("Tracking");

		//chrono.displayMs();
		//criticalStep = CRITICAL_LOOP_STEP.s10_MultitrackIdentity;
		//System.out.println( "entering critical : "+criticalStep.name() );



		if ( TRACKING_ENABLED && TRACKING_IDENTITY_RECOVERY_ENABLED )
		{
			if ( DIADIC_BLACK_AND_WHITE_NO_RFID_EXPERIMENT )
			{
				DiadicBlackAndWhiteIdentity.diadicBlackAndWhiteIdentity();
			}
			else
			{
				multiTrackIdentity();
			}
		}
		//performanceMonitor.stepDone("Multi track identity");



		criticalStep = CRITICAL_LOOP_STEP.s11_RFID_Manager;
		//System.out.println( "entering critical : "+criticalStep.name() );

		if ( RFID_ENABLED )
		{
			if ( t%3 == 0 )
			{
				rfidManager.activateAntennas2();
			}
			RFIDSolver2.rfidManagment(rfidManager);
		}
		//performanceMonitor.stepDone("RFID Solver");

		if( DISPLAY_LOG_IN_CONSOLE )
		{
			// the console has now a size limit
//			if ( t % 60 == 0 ) // clear console (bgRun as if not executed not really important )
//			{
//				ThreadUtil.invokeLater( clearConsoleRunnable );
//			}
		}

		// better to do it here to avoid multiple call
		//backgroundHeightMapBuilder.backgroundImage.dataChanged();
		//performanceMonitor.stepDone("Background dataChanged");

		//chrono.displayMs();
		criticalStep = CRITICAL_LOOP_STEP.s12_Record_to_MPEG;
		//System.out.println( "entering critical : "+criticalStep.name() );

		if ( SAVE_MEDALLON )
		{
		// Hide medaillon viewer
			for ( Animal animal : trackContainer.animalTrackSegmentPool.animalList )
			{
				try
				{
//					animal.medaillonSequence.getFirstViewer().setVisible( false );
				}catch( NullPointerException e ){}
			}

			// Set medaillon
			for ( Animal animal : trackContainer.animalTrackSegmentPool.animalList )
			{
				MouseDetection detection = animal.getDetectionAt( LiveMouseTracker.getT() );
				if ( detection != null )
				{
					animal.medaillonSequence.setImage(
							0 , 0, detection.getInfraPatchRotated( detection.angle ) );

				}else
				{
					// black image if nothing found
					//System.out.println("Animal " + animal + " set medaillon empty");
					animal.medaillonSequence.removeAllImage();
					//animal.medaillonSequence.setImage( 0 , 0 , new IcyBufferedImage( 100, 100, 1, DataType.TYPE_INT_ARGB ));
				}
			}
			for ( Animal animal : trackContainer.animalTrackSegmentPool.animalList )
			{
				animal.mpegMedaillonRecorder.recordMP4TimeLapse();
			}
		}

		mpegTimeLapseRecorder.recordMP4TimeLapse();
		//performanceMonitor.stepDone("Record MPEG");
		/* THERMAL
		if ( thermalSequence != null )
		{
			mpegTimeLapseThermalRecorder.recordMP4TimeLapseThermal();
		}
		*/

		if ( SAVE_BACKGROUND )
		{
			if ( t % ( saveBackgroundEachNumberOfFrame ) == 0 )
			{
				System.out.println("[SAVE BACKGROUND]");
				IcyBufferedImage imageCopy=null;
				try
				{
					imageCopy = IcyBufferedImageUtil.getCopy( backgroundHeightMapBuilder.getBackgroundImage() );
				}
				catch( NullPointerException e )
				{
					System.out.println("[SAVE BACKGROUND] Image is null");
				}

				if ( imageCopy != null )
				{
					final IcyBufferedImage image = imageCopy;
					recordBackgroundImageThread = new Thread( new Runnable() {

						@Override
						public void run() {

							String fileName = LiveMouseTracker.BASE_FOLDER +
									LiveMouseTracker.getExperimentName() + "/background/background_t" +LiveMouseTracker.getT()+".png" ;

							FileUtil.ensureParentDirExist( fileName );

							File file = new File( fileName );

							try {
								Saver.saveImage( image, file, true );
							} catch (FormatException e) {
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							}

						}
					},"Record Background");
					recordBackgroundImageThread.start();
				}

			}

		}

		//performanceMonitor.stepDone("Save background");

		// revert depthImage (for display and technical demo purposes)
		if ( DISPLAY_REVERT_3D_DEPTH_AND_BACKGROUND )
		{
			{
				if ( depthSequenceRevert == null )
				{
					depthSequenceRevert = new Sequence("depth image reverted");
					addSequence( depthSequenceRevert );
					depthSequenceRevert.setImage( 0,0, new IcyBufferedImage( infraOut.getWidth(),
							infraOut.getHeight(), 1, DataType.FLOAT ) );
				}
				//
				short[] bufferDepth= depthImage.getDataXYAsShort( 0 );
				float[] bufferDepthInverted= depthSequenceRevert.getImage(0, 0).getDataXYAsFloat( 0 );
				for ( int i = 0 ; i < bufferDepthInverted.length ; i++ )
				{
					bufferDepthInverted[i]= (float) -(bufferDepth[i]-630);
				}
				depthSequenceRevert.dataChanged();
			}
			{
				if ( backgroundSequenceRevert == null )
				{
					backgroundSequenceRevert = new Sequence("background image reverted");
					addSequence( backgroundSequenceRevert );
					backgroundSequenceRevert.setImage( 0,0, new IcyBufferedImage( infraOut.getWidth(),
							infraOut.getHeight(), 1, DataType.FLOAT ) );
				}
				//
				short[] bufferBackground= backgroundHeightMapBuilder.backgroundImage.getDataXYAsShort( 0 );
				float[] bufferBackgroundInverted= backgroundSequenceRevert.getImage(0, 0).getDataXYAsFloat( 0 );
				for ( int i = 0 ; i < bufferBackgroundInverted.length ; i++ )
				{
					bufferBackgroundInverted[i]= (float) -(bufferBackground[i]);
				}
				backgroundSequenceRevert.dataChanged();
			}
			//performanceMonitor.stepDone("Display 3D");
		}

		// clean past data heavy detection.

		for ( MouseDetection mouseDetection : trackContainer.getAllDetectionAt( getT()-5 ) )
		{
			mouseDetection.cleanHeavyData();
		}
		//performanceMonitor.stepDone("Clean heavy data");

		if ( TTL_SYNCHRO_ENABLED )
		{
			manageTTLSynchroEvents();
			//performanceMonitor.stepDone("TTL Synchro");
		}

		/*
		if ( ( chrono.getNanos() / 1000000f ) > 30 )
		{
			System.out.println("OVER COMPUTATION");
		}
		chrono.displayMs();
		*/
		//performanceMonitor.finish();
		//performanceMonitor.printReport();

	}



	private void manageTTLSynchroEvents() {

		AnimalPool animalPool = getMainAnimalPool();
		int t =  LiveMouseTracker.getT();

		{
			// nose nose
			boolean noseNoseEventActive = false;
			for ( MouseDetection d1 : animalPool.getAllDetectionAt( t ) )
			{
				for ( MouseDetection d2 : animalPool.getAllDetectionAt( t ) )
				{
					if ( d1 == d2 ) continue;
					if ( d1.getFrontPoint() == null ) continue;
					if ( d2.getFrontPoint() == null ) continue;


					if( d1.getFrontPoint().toPoint2D().distance( d2.getFrontPoint().toPoint2D() )
							< ChronoConstant.MAX_DISTANCE_HEAD_HEAD_GENITAL_THRESHOLD )
					{
						noseNoseEventActive = true;
					}
				}
			}
			ttlSynchronizer.updateEventState( "nose-nose", 4 , noseNoseEventActive );
		}
		{
			// head 1 tail 2
			try
			{
				boolean head1Tail2EventActive = false;
				MouseDetection d1 = animalPool.animalList.get( 0 ).getDetectionAt( t );
				MouseDetection d2 = animalPool.animalList.get( 1 ).getDetectionAt( t );
				if ( d1.getFrontPoint() != null && d2.getBackPoint() != null )
				{
					if( d1.getFrontPoint().toPoint2D().distance( d2.getBackPoint().toPoint2D() )
							< ChronoConstant.MAX_DISTANCE_HEAD_HEAD_GENITAL_THRESHOLD )
					{
						head1Tail2EventActive= true;
					}
				}
				ttlSynchronizer.updateEventState( "head1-tail2", 5 , head1Tail2EventActive );
			}catch( Exception e )
			{
				// error on animalList
			}
		}
		{
			// head 2 tail 1
			try
			{
				boolean head2Tail1EventActive = false;
				MouseDetection d1 = animalPool.animalList.get( 0 ).getDetectionAt( t );
				MouseDetection d2 = animalPool.animalList.get( 1 ).getDetectionAt( t );
				if ( d1.getBackPoint() != null && d2.getFrontPoint() != null )
				{
					if( d1.getBackPoint().toPoint2D().distance( d2.getFrontPoint().toPoint2D() )
							< ChronoConstant.MAX_DISTANCE_HEAD_HEAD_GENITAL_THRESHOLD )
					{
						head2Tail1EventActive= true;
					}
				}
				ttlSynchronizer.updateEventState( "head2-tail1", 6 , head2Tail1EventActive );
			}catch( Exception e )
			{
				// error on animalList
			}
		}
	}

	Thread recordBackgroundImageThread = null;

	Sequence depthSequenceRevert = null;
	Sequence backgroundSequenceRevert = null;

//	Thread computeSubPartClassifierThread = null;
	MPEGTimeLapseRecorder mpegTimeLapseRecorder = new MPEGTimeLapseRecorder();

	/* THERMAL
	MPEGTimeLapseThermalRecorder mpegTimeLapseThermalRecorder = null;
	 */

	Thread saveToSQLStreamingThread = null;

	static Thread cacheAllAnimalMachineLearningThread = null;

	public static void cacheAllAnimalMachineLearning( final boolean evaluate) {

		boolean createCache = false;
		if ( cacheAllAnimalMachineLearningThread == null )
		{
			createCache = true;
		}

		if ( cacheAllAnimalMachineLearningThread != null )
		{
			System.out.println( "State : " + cacheAllAnimalMachineLearningThread.getState() + " alive : " + cacheAllAnimalMachineLearningThread.isAlive() );

			if ( !cacheAllAnimalMachineLearningThread.isAlive() )
			{
				createCache = true;
			}
		}

		if ( createCache )
		{
			System.out.println("[cacheAllAnimalMachineLearning] Create Cache");
			cacheAllAnimalMachineLearningThread = new Thread( new Runnable() {
				@Override
				public void run() {
					System.out.println("Starting cache of all animals. t=" + getT() );
					CachedAnimalMachineLearningManager.createCache( trackContainer.animalTrackSegmentPool.animalList , evaluate );
//					try {
//						Thread.sleep( 1000 );
//					} catch (InterruptedException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
				}
			} , "Cache all animals");
			cacheAllAnimalMachineLearningThread.setPriority( LiveMouseTracker.SECONDARY_THREAD_PRIORITY );
			cacheAllAnimalMachineLearningThread.start();
//			ThreadUtil.bgRun( cacheAllAnimalMachineLearningThread );
		}else
		{
			System.out.println("[cacheAllAnimalMachineLearning] Does not create cache.");
		}


	}

//	private void recordMP4TimeLapse() {
//
//		if ( !guiPanel.getSaveToMp4CheckBox().isSelected() ) return;
//
//
//		//if ( getT() % 18000 == 0 ) // 10 minutes
//		if ( getT() % 900 == 0 ) // 30 s
//		{
//			ThreadUtil.bgRunSingle( new Runnable() {
//
//				@Override
//				public void run() {
//
//					System.out.println("Closing MPEG.");
//
//					MPEGRecorderFramePerFrame closingMpegRecorder = mpegRecorder; // create a copy of the reference
//					mpegRecorder = null; // set as null the main recorder to force a restart of it.
//
//					try {
//						recordMPEGStreamThread.join( 5000 ); // wait the end of the current frame record.
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
//					if ( !recordMPEGStreamThread.isAlive() )
//					{
//						// Close MPEG Recording
//						if ( closingMpegRecorder != null )
//						{
//							if( closingMpegRecorder.isOpen() )
//							{
//								closingMpegRecorder.close();
//							}
//						}
////				closeMPEGRecording(); // will force a refresh of the record. ( the file cannot be seen unless it is closed so we create 10 minutes clips )
//					}else
//					{
//						System.err.println("CANT'T SAVE MPEG");
//					}
//					System.out.println("Closing MPEG Done.");
//				}
//			});
//		}
//
//		if ( getT() % Integer.parseInt( guiPanel.getSaveToMp4SkipFrame().getText() ) == 0 )
//		{
//			if ( recordMPEGStreamThread != null ) // if 2 thread are recording the same image, then we crash.
//			{
//				if ( !recordMPEGStreamThread.isAlive() )
//				{
//					recordMPEGStreamThread = null;
//				}else
//				{
//					System.out.println("MP4 recording: skipping frame at record. (recording is too slow for this framerate on your machine)");
//				}
//			}
//
//			if ( recordMPEGStreamThread == null )
//			{
//				recordMPEGStreamThread = new Thread( new Runnable() {
//
//					@Override
//					public void run() {
//						if ( guiPanel.getSaveToMp4StreamType().getSelectedItem() == TimeLapseMP4Save.INFRA_AND_OVERLAY_INFOS )
//						{
//							manageMPEGInfraRecording( true );
//						}else
//						{
//							manageMPEGInfraRecording( false );
//						}
//					}
//				});
//				recordMPEGStreamThread.setPriority( Thread.NORM_PRIORITY );
//				recordMPEGStreamThread.start();
//			}
//
//		}
//
//
//	}

//	MPEGRecorderFramePerFrame mpegRecorder = null;

	/** If too much detection are found (and we know the max number of animal) we remove a number of detection */
	private void postFilterNumberOfAnimals(
			ArrayList<MouseDetection> rawMouseDetectionList) {

		int maxNumberOfAnimalToSearchFor = trackContainer.animalTrackSegmentPool.getNumberOfAnimalToSearchFor();
		while ( rawMouseDetectionList.size() > maxNumberOfAnimalToSearchFor )
		{

//			boolean allDetectionAreEvaluatedWithML = true;
//			for ( MouseDetection detection : rawMouseDetectionList )
//			{
//				if ( detection.detectionChanceWithMLFilter < 0 )
//				{
//					allDetectionAreEvaluatedWithML = false;
//				}
//			}
//
//			if ( allDetectionAreEvaluatedWithML )
//			{
//				double minChance = Double.MAX_VALUE;
//				MouseDetection baddestDetection = null;
//				for ( MouseDetection detection : new ArrayList<MouseDetection>( rawMouseDetectionList ) )
//				{
//					double s = detection.detectionChanceWithMLFilter;
//					if ( s < minChance )
//					{
//						baddestDetection = detection;
//						minChance = s;
//					}
//				}
//				System.out.println("REMOVING DETECTION BECAUSE OF MAX NUMBER OF ANIMALS " + baddestDetection );
//				baddestDetection.getROI2DArea().setColor( Color.cyan );
//				rawMouseDetectionList.remove( baddestDetection );
//				correctBackGround( depthImage, baddestDetection );
//			}
//			else
			{
				// remove the smallest detection

				double minSurface = Double.MAX_VALUE;
				MouseDetection smallestDetection = null;
				for ( MouseDetection detection : new ArrayList<MouseDetection>( rawMouseDetectionList ) )
				{
					double s = detection.getSurface();
					if ( s < minSurface )
					{
						smallestDetection = detection;
						minSurface = s;
					}
				}
				//System.out.println("REMOVING DETECTION BECAUSE OF MAX NUMBER OF ANIMALS");
				smallestDetection.getROI2DArea().setColor( Color.cyan );
				rawMouseDetectionList.remove( smallestDetection );
				correctBackGround( depthImage, smallestDetection );
			}
		}
	}

	public void addAbsoluteHint( AbsoluteHint absoluteHint )
	{
		synchronized (absoluteHintArrayList) {
			absoluteHintArrayList.add( absoluteHint );
		}

	}

	private void multiTrackIdentity() {

		if ( !learningAnimalInitDone )
		{
			return;
		}

		//System.out.println("multi track id: Number of anonymous tracks: " + trackContainer.anonymousTrackSegmentPool.trackSegmentArrayList.size() );
		if ( trackContainer.anonymousTrackSegmentPool.trackSegmentArrayList.isEmpty() )
		{
			return;
		}
		multiIdentityAgentManager.process( trackContainer.anonymousTrackSegmentPool );
	}

	public static MultiIdentityAgentManager multiIdentityAgentManager = new MultiIdentityAgentManager();

//	Identifier identifier = null;

	/**
	 * This method shall just affect the first animal track association.
	 * Further algo will catch up tracks that are not yet associated
	 *
	 * The init learning will search for the tracks that fits those constraints:
	 * max number of detection point.
	 * max number of animal present at the same time in different tracks.
	 */
	private void initLearning() {

		System.out.println("Init learning");
		// check max concurrent tracks => provide the max number of animals.
		int maxAnimal = 0;

		if ( LiveMouseTracker.MACHINE_LEARNING_DETECTION_ERROR_ENABLED )
		{ // Build error set
			System.out.println("Building error set");
			ArrayList<MouseDetection> correctDetectionList = new ArrayList<MouseDetection>();
			for ( TrackSegment ts : trackContainer.anonymousTrackSegmentPool.getTrackSegments() )
			{
				correctDetectionList.addAll( ts.getDetectionList() );
			}
			buildInitErrorSet( correctDetectionList );
		}


		//build init set

		for ( int t = 0 ; t < clock.getT() ; t++ )
		{
			int nbTrack = trackContainer.anonymousTrackSegmentPool.getTrackSegmentsContaining(t).size();
			if ( nbTrack > maxAnimal )
			{
				maxAnimal = nbTrack;
			}
		}
		System.out.println("Max animal: " + maxAnimal );

		// Try to find the biggest concurrent tracks.
		// each track should be as big as possible but also equilibrated in number of time point.

		ArrayList<TrackSegment> bestList = new ArrayList<TrackSegment>();

		double bestMaxLength = -Double.MAX_VALUE;
		ArrayList<TrackSegment> trackPoolSegmentList = trackContainer.anonymousTrackSegmentPool.getTrackSegments();
		for ( int i = 0 ; i < 1000 ; i++ )
		{
			ArrayList<TrackSegment> candidateList = new ArrayList<TrackSegment>();
			Random random = new java.util.Random( );

			for ( int n = 0 ; n < maxAnimal ; n++ )
			{
				// create a list of track
				TrackSegment ts = trackPoolSegmentList.get(
						random.nextInt( trackPoolSegmentList.size() ) );
				candidateList.add( ts );
			}

			// check if candidate can be used.
			boolean candidateOk = true;

			for ( int a = 0 ; a < candidateList.size() ; a++ )
			{
				TrackSegment tsA = candidateList.get( a );

				for ( int b = 0 ; b < candidateList.size() ; b++ )
				{
					TrackSegment tsB = candidateList.get( b );

					if ( tsA == tsB && a!=b )
					{
						// doublon in list.
						candidateOk = false;
					}
				}
			}
			if ( !candidateOk ) continue;

			for ( TrackSegment ts : candidateList )
			{
				for ( TrackSegment ts1 : candidateList )
				{
					if ( !ts.overlapInT( ts1 ) )
					{
						// track don't overlap !
						candidateOk = false;
					}
				}
			}

			if ( !candidateOk ) continue;

			// track set is ok.

			double trackSetLen = 0;
			for ( TrackSegment ts : candidateList )
			{
				trackSetLen+= ts.getLength();
				// compute maxLength cumulated of tracks (should be somting else... to balance track size )
			}

			if ( trackSetLen > bestMaxLength )
			{
				bestList = candidateList;
				bestMaxLength = trackSetLen;
			}


		}

		if ( bestList.size() == 0 ) // the bestList can't be created. assume random tracks for start.
		{
			System.out.println("Can't find good init set to start (may be not enough animal detected). Assigning random tracks.");
			Random random = new java.util.Random( );
			try{
			for ( int n = 0 ; n < maxAnimal ; n++ )
			{
				bestList.add( trackPoolSegmentList.get( n ) );
				// create a list of track
//				TrackSegment ts = trackPoolSegmentList.get(
//						random.nextInt( trackPoolSegmentList.size() ) );
//				bestList.add( ts );
			}}
			catch( NullPointerException e )
			{
				// not enough track to init.
			}
		}

		// affect animals with tracks.
		if ( ASSIGN_ANIMALS_ON_INIT | rfidManager.areAllAntennaeFaulty() )
		{
			System.out.println("Assigning animals on init.");
			for ( int i= 0 ; i < bestList.size() ; i++ )
			{
				TrackSegment ts = bestList.get( i );
				System.out.println("" + i + "*" + ts + "* : len: " + ts.getLength() );
				trackContainer.anonymousTrackSegmentPool.removeTrack( ts );
				trackContainer.animalTrackSegmentPool.animalList.get( i ).addTrackSegment( ts );
			}
		}
		System.out.println("Learning init done.");
		learningAnimalInitDone = true;

	}

	public static Plugin plugin;
	public static FrameInfo currentFrameInfo;


	/**
	 * @deprecated This was the single track association. Now the system should use the new multi track solver.
	 */
//	private void takeSingleTrackAssociationToAnimalDecision() {
//
//		// Pour le moement simple association sans prendre en compte les scores de toutes les tracks.
//		if ( TRACKING_IDENTITY_RECOVERY_ENABLED )
//		for ( TrackSegment ts : new ArrayList<TrackSegment>( trackSegmentPool.getTrackSegments() ) )
//		{
//			IdentityResult identityResult = ts.getIdentityResult();
//			if ( identityResult != null )
//			{
//				if( ts.getLength() >= MIN_TRACK_LENGTH_TO_PROCESS_ID_CHECK ) // don't process if a number of frame is not reached.
//				{
//					if ( ts.getIdentityResult().animalProba >= 0.9 )
//					{
//						identityResult.animalFound.addTrackSegment( ts );
//						trackSegmentPool.remove( ts );
//					}
//				}
//			}
//
//		}
//
//	}

	/**
	 * @deprecated worked with single identity scheme
	 * Now should work with multi track consideration
	 */
	private void waitForMachineLearningSingleTrackIdentity() {

		for ( MachineLearningTrackIdentityThread mltit : machineLearningTrackIdentityThreadList )
		{
			try {
				mltit.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		machineLearningTrackIdentityThreadList.clear();

	}

	private void track(int t, ArrayList<MouseDetection> rawMouseDetectionList) {

		for ( MouseDetection rawMouseDetection : rawMouseDetectionList )
		{
			if ( rawMouseDetection.getROI2DArea().getName().startsWith("seg ok") )
			{
//				TrackSegment trackContainingDetection =
				addDetection( rawMouseDetection );
			}
		}
	}

//	private void findCageROI() {
//
//		for ( ROI2D roi : infraOut.getROI2Ds() )
//		{
//			if ( roi.getName().startsWith("cage limits") )
//			{
//				cageROI = roi;
//			}
//		}
//	}

	public static TrackPoolOverlay getTrackPoolOverlay() {
		return trackPoolOverlay;
	}

	private void filterDetection(ArrayList<MouseDetection> rawMouseDetectionList, int t, IcyBufferedImage depthImage  ) {

		//
		// check detection with the DetectionFiltering if the errorSet is ready.
		//
		//System.out.println( "err set: " + errorDetectionSet );
		if (errorDetectionSet != null )
		{

			if ( mlDetectionFiltering == null )
			{
				return;
				// should be rebuild from time to time
				// mlDetectionFiltering= new MachineLearningDetectionFiltering( errorDetectionSet );
			}

			float devCorrection = 0;
			try
			{
				devCorrection = Float.parseFloat( guiPanel.getDevValue01().getText() );
			}catch( Exception e ) {}
			try
			{				
				mlDetectionFiltering.filter( rawMouseDetectionList , devCorrection );
			}catch( Exception e )
			{
				System.out.println("t:"+ t + " Cannot filter detections with ML.");
			}

			if ( TRACK_REJECTED_TRACKING_ENABLED )
			{
				for ( MouseDetection detection : mlDetectionFiltering.getRawMouseDetectionRejectedList() )
				{
					errorDetectionTrackPool.addDetection( detection );
				}
				for ( TrackSegment ts : errorDetectionTrackPool.getTrackSegments() )
				{
					if ( ts.getLength() > TRACK_REJECTED_LENGTH )
					{
//						Event event = new Event( "ErrTrInt", Color.BLACK ,
//								ts.getDetection( t-1 ).getMassCenter().toPoint2D()  );
//						event.setCanRemove( true );
//						eventOverlay.addEvent( event );

						for ( MouseDetection detection : ts.getDetectionList() )
						{
							correctBackGround( depthImage, detection );
							//backgroundHeightMapBuilder.correctBackGround( depthImage, detection.getROI2DArea().getBooleanMask( true ) );
						}
						errorDetectionTrackPool.removeTrack( ts );
						continue;
					}
					if ( ts.getDetection( t ) == null ) // clean too short tracks from past
					{
						errorDetectionTrackPool.removeTrack( ts );
					}
				}
			}
		}
	}

	/** Correct the depth map with rejected detection */
	public void correctBackGround( IcyBufferedImage depthImage, BooleanMask2D mask )
	{
		backgroundHeightMapBuilder.correctBackGround( depthImage, mask );
	}

	/** Correct the depth map with rejected detection */
	public void correctBackGround( IcyBufferedImage depthImage, MouseDetection detection )
	{
		backgroundHeightMapBuilder.correctBackGround( depthImage, detection.getBooleanMask() );
	}


	/*
	private ArrayList<BooleanMask2D> buildTailMap() {

		ArrayList<BooleanMask2D> tailCandidateArrayList = new ArrayList<BooleanMask2D>();
		if ( !TAIL_DETECTION_ENABLED ) return tailCandidateArrayList;

		if ( DISPLAY_TAIL_SEQUENCE )
		{
			tailSequence.removeAllROI( false );
			try
			{
				tailSequence.getFirstViewer().getLut().getLutChannel( 0 ).setMinMax( 0 , 1 );
			}catch ( NullPointerException e ) {}
		}

		byte[] tailBuffer = tailImage.getDataXYAsByte( 0 );

		System.err.println("THIS LINE is wrong: infraOut.getImage is not affected by "
				+ "the Z compensation for instance (as it is now a copy in the process sequence)");
		IcyBufferedImage infraImage = infraOut.getImage( 0 , 0 );
		short[] infraBuffer = infraImage.getDataXYAsShort( 0 );

		IcyBufferedImage difInfraImage = difInfraInTimeSequence.getImage( 0 , 0 );
		short[] difInfraBuffer = difInfraImage.getDataXYAsShort( 0 );

		for ( int i = 0 ; i < tailBuffer.length ; i++ )
		{
			tailBuffer[i] = 0;
			//if ( infraBuffer[i] > 10000 && infraBuffer[i] < 12500 )
				if( difInfraBuffer[i] > 1800 )
			{
				tailBuffer[i]++;
			}
		}

		// remove segmented animals.
		//
//		for ( RawMouseDetection detection : rawMouseDetectionList )
//		{
//			ROI2DArea roi = ErodeDilateTools.dilateROI( detection.getROI2DArea() , 4, 4 );
//
//			BooleanMask2D mask = roi.getBooleanMask( true );
//			for ( Point p : mask.getPoints() )
//			{
//				tailBuffer[ p.x + p.y * 512 ] = 0;
//			}
//
//		}

		// getComponents.
		{
			boolean[] tailBooleanBuffer = new boolean[ tailBuffer.length ];
			for ( int i = 0 ; i < tailBooleanBuffer.length ; i++ )
			{
				tailBooleanBuffer[i] = (tailBuffer[i] > 0);
			}
			BooleanMask2D tailMask = new BooleanMask2D( infraImage.getBounds() , tailBooleanBuffer );

			ROI2D tailROI = MorphoROITools.dilateROI( new ROI2DArea( tailMask ) , 1 , 1 , 1 );
			tailMask = tailROI.getBooleanMask( true );

			BooleanMask2D[] components = tailMask.getComponents();

			// remove what is out of the cage and build the result ArrayList
			for ( int i = 0 ; i < components.length ; i++ )
			{
				if ( cageROI.contains( new ROI2DArea( components[i]) ) )
				{
					tailCandidateArrayList.add( components[i] );
				}
			}

			// filter by size and compactness
			for ( BooleanMask2D b : new ArrayList<BooleanMask2D>( tailCandidateArrayList ) )
			{
				if( b.getNumberOfPoints() < 10 )
				{
					tailCandidateArrayList.remove( b );
					continue;
				}

				double compactness = BooleanMaskUtil.getCompactness( b );
				if ( compactness < 0.04 )
				{
					if ( DISPLAY_TAIL_SEQUENCE )
					{
						ROI2DArea tmpROI = new ROI2DArea( b );
						tmpROI.setShowName( true );
						tmpROI.setName("tail + " + (int)( compactness * 1000d ) );
						tailSequence.addROI( tmpROI );
					}
				}else
				{
					tailCandidateArrayList.remove( b );
					continue;
				}
			}

		}

		if ( DISPLAY_TAIL_SEQUENCE )
		{
			tailImage.dataChanged();
		}


		return tailCandidateArrayList;

	}*/


	public static void correctInvalidZValue( IcyBufferedImage depthImage )
	{
		if ( !CORRECT_DEPTH_INVALID_VALUES ) return;

		short[] bufferDepth= depthImage.getDataXYAsShort( 0 );

		short lastDepthValue = 0;

		for ( int i = 0 ; i < bufferDepth.length ; i++ )
		{
//			lastValue = bufferDepth[i] & 0xFFFF;
//			bufferDepth[i] = (short) ( lastValue ) ; //& 0xFFFF );

//			int infra = bufferDepth[i] & 0xFFFF;
//			if ( infra == 0 || infra > 65500 )
			short depth = bufferDepth[i];
			if ( depth == 0 || depth < -32700 )
			{
				bufferDepth[i] = lastDepthValue;
//				bufferDepth[i] = (short) ( lastValue ); // (short) infra ;
			}else
			{
				lastDepthValue = depth ;
			}
		}

		//depthImage.dataChanged();

	}

	public static void compensateZIntensityError2(IcyBufferedImage depthImage,
			IcyBufferedImage infraImage) {

		short lastDepthValue = 0;

		if ( !CORRECT_DEPTH_INVALID_VALUES ) return;
		if ( !COMPENSATE_Z_WITH_INTENSITY_ENABLED ) return;

		short[] bufferInfra= infraImage.getDataXYAsShort( 0 );
		short[] bufferDepth= depthImage.getDataXYAsShort( 0 );

		// 23000 : 0
		// 2000 : -20
		for ( int i = 0 ; i < bufferInfra.length ; i++ )
		{
			// deal with invalid data
			short depth = bufferDepth[i];
			if ( depth == 0 || depth < -32700 )
			{
				bufferDepth[i] = lastDepthValue;
			}else
			{
				lastDepthValue = depth ;
			}

			// corect with infra
			short infra = bufferInfra[i];
			float correction = -( 23000 - infra ) / 1000f;

			//float correction = -2.5f * ( infra / 1000f );
			bufferDepth[i]+= (short) correction;
		}
		//depthImage.dataChanged();


	}


	public static void compensateZIntensityError(IcyBufferedImage depthImage,
			IcyBufferedImage infraImage) {

		if ( !COMPENSATE_Z_WITH_INTENSITY_ENABLED ) return;

		short[] bufferInfra= infraImage.getDataXYAsShort( 0 );
		short[] bufferDepth= depthImage.getDataXYAsShort( 0 );

		// 23000 : 0
		// 2000 : -20
		for ( int i = 0 ; i < bufferInfra.length ; i++ )
		{
			short infra = bufferInfra[i];
			float correction = -( 23000 - infra ) / 1000f;

			//float correction = -2.5f * ( infra / 1000f );
			bufferDepth[i]+= (short) correction;
		}
		//depthImage.dataChanged();


	}
	private void computeDepthDifCumulated() {

		IcyBufferedImage difDepthCumulated = difDepthCumulatedSequence.getImage( 0 , 0 );

		if ( difDepthCumulated == null ) // first row > forget about that.
		{
			difDepthCumulated = new IcyBufferedImage( infraOut.getWidth(),
					infraOut.getHeight(), 1, DataType.SHORT );
			difDepthCumulatedSequence.setImage( 0 , 0 , difDepthCumulated );
			return;
		}

		IcyBufferedImage currentDepthDifImage= difDepthInTimeSequence.getImage( 0, 0 );
		short[] currentDepthDifBuffer = currentDepthDifImage.getDataXYAsShort( 0 );
		short[] cumulatedDepthDifBuffer = difDepthCumulated.getDataXYAsShort( 0 );

		for ( int i = 0; i < currentDepthDifBuffer.length ; i++ )
		{
			if ( currentDepthDifBuffer[i] < -7 ) // FIXME: CONST
			{
				cumulatedDepthDifBuffer[i] = -20000; // just for display. could be -1
			}
			if ( currentDepthDifBuffer[i] > 7 ) // FIXME: CONST
			{
				cumulatedDepthDifBuffer[i] = 20000;
			}
		}

		difDepthCumulated.dataChanged();

	}
	/** Flips in X the image provided */
	private void flip(IcyBufferedImage image) {

		int height = image.getHeight();
		int width = image.getWidth();

		short[] buffer = image.getDataXYAsShort( 0 );
		short[] tmpLineBuffer = new short[ width ];
		int startLineIndex = 0;
		for ( int y = 0 ; y < height ; y++ )
		{
			// copy original line
			for ( int x = 0 ; x < width ; x++ )
			{
				tmpLineBuffer[x] = buffer[startLineIndex + x];
			}

			// recopy flip
			for ( int x = 0 , xx = width -1 ; x < width ; x++ , xx-- )
			{
				buffer[startLineIndex + x]
						= tmpLineBuffer[xx];
			}
			startLineIndex+=width;
		}
		image.dataChanged();

	}
	/** remove all rois in the infraOut starting with "tmp" */
	public static void cleanTemporaryROIs() {

		infraOut.beginUpdate();
		try
		{
			for ( ROI r : infraOut.getROIs() )
			{
				if ( r.getName().startsWith("tmp"))
				{
					infraOut.removeROI( r );
				}
			}
		}
		finally
		{
			infraOut.endUpdate();
		}

	}

	/*
	 * Try to add the detection in the animalPool or in the trackSegmentPool
	 */
	private TrackSegment addDetection( MouseDetection rawMouseDetection )
	{
		TrackSegment tsContainingDetection = null;
//		System.out.println("---");
//		System.out.println( "add detection: " + rawMouseDetection );
		// try to put the detection direct in animal pool tracking set

//		System.out.println("Animal prolongator call");
		tsContainingDetection = trackContainer.animalTrackSegmentPool.addDetection( rawMouseDetection ) ;

		// can't, so put it in unknown tracksegment set for further solving
		if ( tsContainingDetection == null )
		{
//			System.out.println("Anonymous prolongator call");
			tsContainingDetection = trackContainer.anonymousTrackSegmentPool.addDetection( rawMouseDetection );
		}

		return tsContainingDetection;
	}

	ArrayList<MachineLearningTrackIdentityThread> machineLearningTrackIdentityThreadList = new ArrayList<MachineLearningTrackIdentityThread>();

	private void computeIdentityInThread(TrackSegment track ) {

		// FIXME : should not use the main animal Pool but another one.
		MachineLearningTrackIdentityThread mltit = new MachineLearningTrackIdentityThread( trackContainer.animalTrackSegmentPool , track );
		machineLearningTrackIdentityThreadList.add( mltit );
		mltit.start();

	}

	private void refreshSubPartClassifier() {
		//System.out.println("*** TEST - COMPUTE SUB PARTS ***");
		//Chronometer	computeSubPartChrono = new Chronometer("COMPUTE SUB PARTS");
//		Message message = LiveMouseTracker.perfLogger.addMessage( new Message( "Refresh sub part classifier." ));
		for ( Animal animal : getMainAnimalPool().getAnimalList() )
		{
			MachineLearningSubPartBuilder ml_spb = new MachineLearningSubPartBuilder();
			if ( animal.getTrackSegments().size() == 0 )
			{
				System.out.println("Sub part classifier: no data to build animal " + animal );
				continue;
			}

			ml_spb.buildSet( animal );


			//		ml_spb.buildClassifier();
			Classifier classifier = null;
			classifier = ml_spb.buildClassifier();
			animal.setMachineLearningSubPartsClassifier( classifier, ml_spb.getDataSet() );
			//System.out.println( "*** TEST - NUM INSTANCES ML SUB :" + animal.getMachineLearningSubPartDataSet().numInstances() );
			//ml_spb.evaluate();
		}
//		LiveMouseTracker.perfLogger.removeMessage( message );
		//computeSubPartChrono.displayMs();
		//System.out.println("*** TEST - COMPUTE SUB PARTS DONE ***");
	}


	/** TODO: SHOULD BE MERGED WITH buildInitErrorSet */
	public void refreshErrorSet()
	{
//		Message message = LiveMouseTracker.perfLogger.addMessage( new Message( "Refresh error set." ));

		System.out.println("[RefreshErrorSet] Starting");
		// try to build a 600 correct set of detection
		ArrayList<MouseDetection> correctMouseDetectionList = new ArrayList<MouseDetection>();

		for ( int i = 0; i < NB_SAMPLE_FOR_ERROR_LEARNING ; i++ )
		{
			MouseDetection detection = getMainAnimalPool().getRandomDetection();
			if ( detection != null )
			{
				correctMouseDetectionList.add( detection );
			}
		}
		System.out.println("[RefreshErrorSet] Number of true detection: " +
				correctMouseDetectionList.size() + " over " + NB_SAMPLE_FOR_ERROR_LEARNING );

		Animal animal = new Animal("Animal", Color.WHITE );
		Animal error = buildErrorAnimalSet( correctMouseDetectionList.size() );

		// ALL the following is the same as the other function. Should be merged

		for ( MouseDetection correctMouseDetection : correctMouseDetectionList )
		{
			// One single track is created each time to avoid the call to rawDetection PostProcess
			TrackSegment tsCorrect = new TrackSegment( correctMouseDetection );
//			tsCorrect.addDetection( correctMouseDetection );
			animal.addTrackSegment( tsCorrect , false );
		}

		MachineLearningSetBuilder setBuilder = new MachineLearningSetBuilder();
		AnimalPool errorPool = new AnimalPool();
		errorPool.addAnimal( animal );
		errorPool.addAnimal( error );
		errorDetectionSet = setBuilder.buildSet( errorPool );
		//System.out.println("Error detection set: " +  errorDetectionSet );

		mlDetectionFiltering= new MachineLearningDetectionFiltering( errorDetectionSet );

		System.out.println("[RefreshErrorSet] Done");
//		setBuilder.evaluate();

//		LiveMouseTracker.perfLogger.removeMessage( message );
	}

	/**
	 * Build error set. (pick random sample of false mouse patch in the cage)
	 * @param nbSample
	 * */
	private Animal buildErrorAnimalSet(int nbSample ) {

//		Message message = LiveMouseTracker.perfLogger.addMessage( new Message( "Build error animal set." ));

		// get last set of animal detection to avoir learning on them.
		ArrayList<MouseDetection> animalDetectionList = LiveMouseTracker.trackContainer.getAllDetectionAt( getInitLearningT() );
		// build error
		Animal error = new Animal("Error", Color.RED );
		for ( int i = 0 ; i < NB_SAMPLE_FOR_ERROR_LEARNING && i < nbSample ; i++ )
		{
			// One single track is created each time to avoid the call to rawDetection PostProcess
			int side = (int) Math.sqrt( LiveMouseTracker.MAX_SIZE_OF_CANDIDATE_DETECTION );
			boolean roiErrorOk=false;
			ROI2DRectangle errorROI = null;
			while ( !roiErrorOk )
			{
				errorROI = new ROI2DRectangle(
						new Rectangle2D.Double(
								(512-side) * Math.random(),
								(424-side) * Math.random(),
								side , side ));
				// before
				//roiErrorOk = ( LiveMouseTracker.getROICageFloor().contains( errorROI ) );
				// after:
				roiErrorOk = ( LiveMouseTracker.cageFloorMask.contains( errorROI.getBooleanMask( true ) ) );
				for ( MouseDetection detection : animalDetectionList )
				{
					if ( detection.getROI2DArea().intersects( errorROI ) )
					{
						roiErrorOk = false;
					}
				}
			}

			BooleanMask2D errorMask = errorROI.getBooleanMask( true );
			ROI2DArea errorROIArea = new ROI2DArea( errorMask );
			//errorROIArea.setName("error");
			//infraOut.addROI( errorROIArea );

			MouseDetection detectionError = new MouseDetection( errorROIArea, errorMask, 0  );
			TrackSegment tsError = new TrackSegment( detectionError );
//			tsError.addDetection( detectionError );
			error.addTrackSegment( tsError , false );
		}

//		LiveMouseTracker.perfLogger.removeMessage( message );

		return error;
	}

	private void buildInitErrorSet( ArrayList<MouseDetection> correctMouseDetectionList ) {

//		Message message = PerfLoggerOverlay.addMessage( new Message( "Build init error set." ));

		// build error list set

		System.out.println("BUILD INIT ERROR SET");

		if ( correctMouseDetectionList.size() == 0 )
		{
			System.err.println("Can't build error set !");
			return;
		}

		Animal animal = new Animal("Animal", Color.WHITE );
		Animal error = buildErrorAnimalSet( correctMouseDetectionList.size() );

//		// build error
//		for ( int i = 0 ; i < NB_SAMPLE_FOR_ERROR_LEARNING; i++ )
//		{
//			// One single track is created each time to avoid the call to rawDetection PostProcess
//			TrackSegment tsError = new TrackSegment( );
//			ROI2DRectangle errorROI = new ROI2DRectangle( new Rectangle2D.Double(
//					100 + Math.random()* 200 , 100+ Math.random()*200 , Math.random()*30 , Math.random()* 30 ));
//			ROI2DArea errorROIArea = new ROI2DArea( errorROI.getBooleanMask( true ) );
//			//errorROIArea.setName("error");
//			//infraOut.addROI( errorROIArea );
//
//			MouseDetection detectionError =
//					new MouseDetection( errorROIArea , 0  );
//			tsError.addDetection( detectionError );
//			error.addTrackSegment( tsError , false );
//		}

		for ( MouseDetection correctMouseDetection : correctMouseDetectionList )
		{
			// One single track is created each time to avoid the call to rawDetection PostProcess
			TrackSegment tsCorrect = new TrackSegment( correctMouseDetection );
//			tsCorrect.addDetection( correctMouseDetection );
			animal.addTrackSegment( tsCorrect , false );
		}

		MachineLearningSetBuilder setBuilder = new MachineLearningSetBuilder();
		AnimalPool errorPool = new AnimalPool();
		errorPool.addAnimal( animal );
		errorPool.addAnimal( error );
		errorDetectionSet = setBuilder.buildSet( errorPool );
		if ( SHOW_MACHINE_LEARNING_EVALUATION )
		{
			setBuilder.evaluate( null );
		}

//		LiveMouseTracker.perfLogger.removeMessage( message );

	}

	Instances errorDetectionSet;
//	TrackProblemPool trackProblemPool = null ;

	MachineLearningTrackIdentity machineLearningSolver = null;

	/** Number of frame dropped
	 * @deprecated*/
	private int frameDrop;


	private void computeDepthDifferentialImageInTime(IcyBufferedImage depthImage, IcyBufferedImage difImage) {

		if ( previousDepthImage != null )
		{
			short[] depthBuffer = depthImage.getDataXYAsShort( 0 );
			short[] depthPreviousBuffer = previousDepthImage.getDataXYAsShort( 0 );
			short[] depthDifBuffer = difImage.getDataXYAsShort( 0 );

			for ( int i = 0 ; i < depthBuffer.length ; i ++ )
			{
				depthDifBuffer[i] = (short) ( ( depthPreviousBuffer[i] - depthBuffer[i] ) );
			}

			difImage.dataChanged();
		}

		previousDepthImage = depthImage;

	}

	private void computeInfraDifferentialImageInTime(IcyBufferedImage infraImage, IcyBufferedImage difInfra) {

		if ( previousInfraImage != null )
		{
			short[] infraBuffer = infraImage.getDataXYAsShort( 0 );
			short[] infraPreviousBuffer = previousInfraImage.getDataXYAsShort( 0 );
			short[] infraDifBuffer = difInfra.getDataXYAsShort( 0 );

			for ( int i = 0 ; i < infraBuffer.length ; i ++ )
			{
				// FIXME: Warning: Absolute !
				infraDifBuffer[i] = (short) ( ( infraPreviousBuffer[i] - infraBuffer[i] ) );
			}

			difInfra.dataChanged();
		}

		previousInfraImage = infraImage;

	}



	/**
	 * Correct the diverse Look up table of the viewers.
	 */
	private void setupDisplayLUTViewers() {

//		try{
//		depthOut.getFirstViewer().getLut().getLutChannel(0).setMinMax( 600, 800 );
//		} catch( NullPointerException e ){};

		try{
		backgroundSequence.getFirstViewer().getLut().getLutChannel(0).setMinMax( 600, 800 );
		} catch( NullPointerException e ){};

		try{
		difDepthInTimeSequence.getFirstViewer().getLut().getLutChannel(0).setMinMax( -20, 20 );
		} catch( NullPointerException e ){};

		try{
		difInfraInTimeSequence.getFirstViewer().getLut().getLutChannel(0).setMinMax( -20, 20 );
		} catch( NullPointerException e ){};

		try{			
			infraOut.getFirstViewer().getLut().getLutChannel(0).setMinMax( 0, 5000 );
		} catch( NullPointerException e ){};

		try{
		substractedBackgroundSequence.getFirstViewer().getLut().getLutChannel(0).setMinMax( 0, 100 );
		} catch( NullPointerException e ){};

	}


	Sequence depthOutLocal = null;
	Sequence infraOutLocal = null;

	public static int nbImageGrabbed = 0;
	public static int nbImageProcessed = 0;

	@Override
	public void kinectChange(Sequence sourceSequence, KinectData kinectData , KinectEvent kinectEvent) {

		if ( kinectEvent == KinectEvent.NEW_DEPTH_SEQUENCE )
		{
			depthOutLocal = sourceSequence;
			if ( DISPLAY_DEPTH_SEQUENCE )
			{
				addSequence( depthOut );
			}
			System.out.println("Depth sequence registered.");
			tryToInit();
		}

		if ( kinectEvent == KinectEvent.NEW_INFRARED_SEQUENCE )
		{
			infraOutLocal = sourceSequence;

//			addSequence( infraOut );

			// Sequence that is created from the kinect driver.
//			addSequence( infraOutLocal );
			System.out.println("Infra sequence registered.");
			tryToInit();
		}

		if ( kinectEvent == KinectEvent.NEW_INFRARED_CAPTURE )
		{
//			if (!lutInfraDone && (infraOut.getFirstViewer() != null))
//			{
//				infraOutLocal.getFirstViewer().getLut().getLutChannel(0).setMinMax( 0, 32000 );
//				lutInfraDone = true;
//			}

			
			IcyBufferedImage img;

			img = infraOutLocal.getImage( 0 , 0 );
			IcyBufferedImage newInfraImage = new IcyBufferedImage(img.getWidth(), img.getHeight(), Array1DUtil.copyOf(img.getDataXY(0)));
			
			//System.out.println("Receive infra: " + img.getWidth() );
			
			img = depthOutLocal.getImage( 0 , 0 );
			IcyBufferedImage newDepthImage = new IcyBufferedImage(img.getWidth(), img.getHeight(), Array1DUtil.copyOf(img.getDataXY(0)));

			synchronized ( imageQueueList )
			{
				imageQueueList.add( new ImageKinect( newInfraImage , newDepthImage, 0, 0 ) );
				nbImageGrabbed++;
			}

			/* OLD FRAME DROP !!
			if ( !inMainProcessingLoop )
			{
				infraImage = newInfraImage;
				depthImage = newDepthImage;
				// inform we have a new image
				newImage = true;
			}else
			{
				// FRAME DROPPED
				frameDrop++;
				long lastMainThreadComputationTimeMsForDrop = (int) ( System.currentTimeMillis() - milliCriticalLoop );
				System.err.println("[FRAME DROP] at t=" + getT() +
						" (nbDrop: " +  frameDrop + ") step " + criticalStep + " / " + lastMainThreadComputationTimeMsForDrop + "ms" );

				if ( MANAGE_FRAME_DROP )
				{
					addEventLogToDataBase( new EventLog("FRAME DROP", null , getT()+ frameDrop ) );
				}
			}
			*/

//			depthImage = IcyBufferedImageUtil.getCopy( depthOut.getImage( 0 , 0 ) );
//			infraImage = IcyBufferedImageUtil.getCopy( infraOut.getImage( 0 , 0 ) );

			// FOR graphical DEBUG in processing thread:
//			Thread processThread = new Thread( new Runnable() {
//
//				@Override
//				public void run() {
//					process();
//			} } );
//			processThread.start();

			// PRODUCTION:
//			process();

		}

		if ( kinectEvent == KinectEvent.NEW_DEPTH_CAPTURE )
		{
			// WARNING: DO NOT PROCESS ON THIS EVENT, DEPTH COMES BEFORE INFRARED
			// WAIT FOR INFRARED TO BE SYNCHRONIZED
		}

		if ( kinectEvent == KinectEvent.KINECT_STOPPED )
		{
			System.out.println("Kinect stopped");
			shutDown();
		}

	}

	/** Init if the 2 sequences are ready and we did not init yet.
	 * note: In live and in record the order of the first sequence ready (infra or depth) can be different.
	 * This method is to ensure that the init is consistent in any case.
	 * */
	private void tryToInit() {

		System.out.println("Try init..." + depthOut + infraOut + initDone);
		if ( depthOut != null && infraOut != null && initDone == false )
		{
			System.out.println("Starting init...");
			init();
		}
	}

	Sequence backgroundSequence = null;

	DebugOverlay debugOverlay;

	static private TrackPoolOverlay trackPoolOverlay;

	/** contains the list of detection of the last frame */
	public ArrayList<MouseDetection> lastDetectionList = new ArrayList<MouseDetection>();

	/** returns the last detection set of tracking (of the last frame processed) */
	public ArrayList<MouseDetection> getLastDetection() {

		synchronized ( lastDetectionList) {
			return new ArrayList<MouseDetection>( lastDetectionList );
		}

	}

	AviSoftEventReceiver aviSoftEventReceiver = null;
	ThreadMonitorOverlay threadMonitor = null;

	/* THERMAL
	ThermalCameraCapture thermalCameraCapture = null;
	 */
	NetworkResultServer networkResultServer = null;
	Thread networkResultServerThread = null;

	private void init() {

		TTL_SYNCHRO_ENABLED = guiPanel.getTimeSynchroArduinoTTLCheckBox().isSelected();
		TTL_EVENT_LISTENER_ENABLED = guiPanel.getManageEventFromArduinoTTL().isSelected();

		if ( !TTL_EVENT_LISTENER_ENABLED )
		{
			ttlEventListener.setEnable( false );
		}

		if ( ! TTL_SYNCHRO_ENABLED )
		{
			ttlSynchronizer.shutdown();
		}

		DISPLAY_LOG_IN_CONSOLE= guiPanel.getLogInConsoleCheckBox().isSelected();

		if( !DISPLAY_LOG_IN_CONSOLE )
		{
			System.out.println("Shutdown display log console. Send to file. (remove console redirection)");
			System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
			System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err)));
		}

		DIADIC_BLACK_AND_WHITE_NO_RFID_EXPERIMENT = guiPanel.getDiadicBlackAndWhiteWithoutRFIDCheckBox().isSelected();
		if ( DIADIC_BLACK_AND_WHITE_NO_RFID_EXPERIMENT )
		{
			guiPanel.getNumberOfMaxAnimalTextField().setText("2");
		}

		PERSPECTIVE_TRANSFORM = guiPanel.getPerspectiveMode().isSelected();

		//SAVE_MEDALLON = guiPanel.getSaveMedallonCheckBox().isSelected();
		SAVE_MEDALLON = false;

		SAVE_BACKGROUND = guiPanel.getSaveBackgroundMapCheckBox().isSelected();
		saveBackgroundEachNumberOfFrame = Integer.parseInt( guiPanel.getSaveBackGroundRecordEachFrame().getText() );

		MAX_NUMBER_OF_ANIMALS = Integer.parseInt( guiPanel.getNumberOfMaxAnimalTextField().getText() );
		BASE_FOLDER = guiPanel.getExperimentFolderTextField().getText( );

		System.out.println("Max number of animals: " + MAX_NUMBER_OF_ANIMALS );

		ANIMAL_IS_WIRED = guiPanel.getChckbxAnimalAreWired().isSelected();

		aviSoftEventReceiver = new AviSoftEventReceiver();

		/* THERMAL
			thermalCameraCapture = new ThermalCameraCapture( thermalSequence );
			if ( thermalCameraCapture.isThermalCameraPresent() )
			{
				mpegTimeLapseThermalRecorder = new MPEGTimeLapseThermalRecorder();

				if ( thermalCameraCapture.isThermalCameraPresent() )
				{
					thermalSequence = thermalCameraCapture.getSequence();
					thermalSequence.addOverlay( new ThermalOverlay() );
				}
			}
		*/
//		registerUserRFID();

//		ANIMAL_COLOR_DETECTION_MODE = (ColorMode) guiPanel.getColorModeComboBox().getSelectedItem();

//		HEAD_DETECTION_METHOD = (HeadDetectionMethod) guiPanel.getHeadMethodDetectionComboBox().getSelectedItem();
//		System.out.println("Head detection method selected: " + HEAD_DETECTION_METHOD );




//		if ( autoColorButton.isSelected() )
//		{
//			ANIMAL_COLOR_DETECTION_MODE = ColorMode.AUTO;
//		}
//
//		if ( blackColorButton.isSelected() )
//		{
//			ANIMAL_COLOR_DETECTION_MODE = ColorMode.BLACK;
//		}
//
//		if ( whiteColorButton.isSelected() )
//		{
//			ANIMAL_COLOR_DETECTION_MODE = ColorMode.WHITE;
//		}

		if ( DISPLAY_SUBSTRACTED_BACKGROUND_SEQUENCE )
		{
			addSequence( substractedBackgroundSequence );
		}

	// put A roi on the image.
		backgroundHeightMapBuilder = new BackgroundHeightMapBuilder();
		backgroundSequence = backgroundHeightMapBuilder.getBackgroundSequence();

		if ( DISPLAY_BACKGROUND_SEQUENCE )
		{
			addSequence( backgroundSequence );
		}

	/*
		ortho = new OrthoConverter();
		addSequence( ortho.getDepthOrthoSequence() );
		addSequence( ortho.getInfraOrthoSequence() );
		*/
		/*
		ROI2DPolygon roi = new ROI2DPolygon( new Point2D.Double( 180, 98 ) );
		roi.addNewPoint( new Point2D.Double( 374, 95 ), false);
		roi.addNewPoint( new Point2D.Double( 367, 280 ), false);
		roi.addNewPoint( new Point2D.Double( 186, 285 ), false);
		*/
/*
		// experience 254
		ROI2DPolygon roi = new ROI2DPolygon( new Point2D.Double( 185, 74 ) );
		roi.addNewPoint( new Point2D.Double( 349, 68 ), false);
		roi.addNewPoint( new Point2D.Double( 345, 294 ), false);
		roi.addNewPoint( new Point2D.Double( 201, 301 ), false);
		roi.setCreating( false );
		roi.setName( "cage limits" );
*/

		// 4 impega cage !
		/*
		ROI2DPolygon roiCage = new ROI2DPolygon( new Point2D.Double( 102 , 78 ) );
		roiCage.addNewPoint( new Point2D.Double( 390, 78 ), false);
		roiCage.addNewPoint( new Point2D.Double( 390, 363 ), false);
		roiCage.addNewPoint( new Point2D.Double( 102, 363 ), false);
		roiCage.setCreating( false );
		roiCage.setName( "cage limits" );
*/


/*
		// 3 impega cage !
		ROI2DPolygon roiCage = new ROI2DPolygon( new Point2D.Double( 131, 37 ) );
		roiCage.addNewPoint( new Point2D.Double( 348, 34 ), false);
		roiCage.addNewPoint( new Point2D.Double( 355, 354 ), false);
		roiCage.addNewPoint( new Point2D.Double( 122, 358 ), false);
		roiCage.setCreating( false );
		roiCage.setName( "cage limits" );

		// Cage extended is the area where the animal can climb.
		ROI2DPolygon roiCageExtended = new ROI2DPolygon( new Point2D.Double( 86, 3 ) );
		roiCageExtended.addNewPoint( new Point2D.Double( 400, 3 ), false);
		roiCageExtended.addNewPoint( new Point2D.Double( 400, 400 ), false);
		roiCageExtended.addNewPoint( new Point2D.Double(  86, 400 ), false);
		roiCageExtended.setCreating( false );
		roiCageExtended.setName( "cage extended limits" );
		*/

	/*
		ROI2DPolygon roi = new ROI2DPolygon( new Point2D.Double( 197, 120 ) );
		roi.addNewPoint( new Point2D.Double( 348, 114 ), false);
		roi.addNewPoint( new Point2D.Double( 358, 341 ), false);
		roi.addNewPoint( new Point2D.Double( 201, 348 ), false);
		roi.setCreating( false );
		roi.setName( "cage limits" );
*/

/*
		// FULL CAGE WITH BORDER ( 1 BLACK MOUSE )
		ROI2DPolygon roi = new ROI2DPolygon( new Point2D.Double( 176, 92 ) );
		roi.addNewPoint( new Point2D.Double( 378, 88 ), false);
		roi.addNewPoint( new Point2D.Double( 388, 382 ), false);
		roi.addNewPoint( new Point2D.Double( 186, 387 ), false);
		roi.setCreating( false );
		roi.setName( "cage limits" );
*/

		depthOut.removeAllROI();
		infraOut.removeAllROI();


		// Cage extended is the area where the animal can climb.
//		ROI2DPolygon roiCage = new ROI2DPolygon( new Point2D.Double( 86-5, 55-5 ) );
//		roiCage.addNewPoint( new Point2D.Double( 420+5, 55-5 ), false);
//		roiCage.addNewPoint( new Point2D.Double( 420+5, 395+5 ), false);
//		roiCage.addNewPoint( new Point2D.Double(  86-5, 395+5 ), false);
//		roiCage.setCreating( false );

/*
		// Multi with corridor en right 50x50 cage
		int corridorThickness = 13;

		int middle = 206;

		ROI2DPolygon roiCage50x50 = new ROI2DPolygon( new Point2D.Double( 86-5+3, 55-5-17 ) );
		roiCage50x50.addNewPoint( new Point2D.Double( 420+5+3, 55-5 -17), false);

		// corridor
		roiCage50x50.addNewPoint( new Point2D.Double( 420+5+3, middle - corridorThickness ), false);
		roiCage50x50.addNewPoint( new Point2D.Double( 512, middle - corridorThickness ), false);
		roiCage50x50.addNewPoint( new Point2D.Double( 512, middle + corridorThickness ), false);
		roiCage50x50.addNewPoint( new Point2D.Double( 420+5+3, middle + corridorThickness ), false);

		roiCage50x50.addNewPoint( new Point2D.Double( 420+5+3, 395+5 -17), false);
		roiCage50x50.addNewPoint( new Point2D.Double(  86-5+3, 395+5 -17), false);
		roiCage50x50.setCreating( false );

		this.ROICage = roiCage50x50;

		ROI2DPolygon roiCage50x50Floor = new ROI2DPolygon( new Point2D.Double( 86-5+3 +30, 55-5-17 +30 ) );
		roiCage50x50Floor.addNewPoint( new Point2D.Double( 420+5+3 -30 , 55-5 -17 +30 ), false);

		// corridor
		roiCage50x50Floor.addNewPoint( new Point2D.Double( 420+5+3-30, middle - corridorThickness ), false);
		roiCage50x50Floor.addNewPoint( new Point2D.Double( 512, middle - corridorThickness ), false);
		roiCage50x50Floor.addNewPoint( new Point2D.Double( 512, middle + corridorThickness ), false);
		roiCage50x50Floor.addNewPoint( new Point2D.Double( 420+5+3-30, middle + corridorThickness ), false);

		roiCage50x50Floor.addNewPoint( new Point2D.Double( 420+5+3 -30 , 395+5 -17 -30 ), false);
		roiCage50x50Floor.addNewPoint( new Point2D.Double(  86-5+3 +30 , 395+5 -17 -30 ), false);
		roiCage50x50Floor.setCreating( false );

		this.ROICageFloor = roiCage50x50Floor;
		updateAllROICage();
 */
	
		if ( cageMode == CAGE_MODE.MULTI_CLASSIC_16 )
		{
			int shiftX = 506+23+6;
			

			ROI2DPolygon roiCage50x50_A = new ROI2DPolygon( new Point2D.Double( 86-5+3 +30, 55-5-17 +30 ) );
			roiCage50x50_A.addNewPoint( new Point2D.Double( 420+5+3 -30 , 55-5 -17 +30 ), false);
			roiCage50x50_A.addNewPoint( new Point2D.Double( 420+5+3 -30 , 395+5 -17 -30 ), false);
			roiCage50x50_A.addNewPoint( new Point2D.Double(  86-5+3 +30 , 395+5 -17 -30 ), false);
			roiCage50x50_A.setCreating( false );
						
			ROI2DPolygon roiCage50x50_B = new ROI2DPolygon( new Point2D.Double( shiftX+86-5+3 +30, 55-5-17 +30 ) );
			roiCage50x50_B.addNewPoint( new Point2D.Double( shiftX+420+5+3 -30 , 55-5 -17 +30 ), false);
			roiCage50x50_B.addNewPoint( new Point2D.Double( shiftX+420+5+3 -30 , 395+5 -17 -30 ), false);
			roiCage50x50_B.addNewPoint( new Point2D.Double(  shiftX+86-5+3 +30 , 395+5 -17 -30 ), false);
			roiCage50x50_B.setCreating( false );

			ROI2DRectangle roiGate_A = new ROI2DRectangle( 380 , 118+4, 660, 145-4 );
			ROI2DRectangle roiGate_B = new ROI2DRectangle( 380 , 267+4, 660, 294-4 );
			
			kinectStreamer.setSequenceForOverlay( infraOut );
			
			ArrayList<ROI2D> cageFloorROIList = new ArrayList<ROI2D>();
			
			cageFloorROIList.add( roiCage50x50_A );
			cageFloorROIList.add( roiCage50x50_B );
			
			cageFloorROIList.add( roiGate_A );
			cageFloorROIList.add( roiGate_B );
			
			setROICageFloor( cageFloorROIList );
			
			ROI2DArea roiCage = new ROI2DArea( cageFloorMask );
			int dilatation = 3;
			System.out.println("Dilatation of floor (nb pixels): " + dilatation );
			System.out.println( "Number of point in dilated area: " + roiCage.getAsBooleanMask().getPoints().length );
			roiCage= MorphoROITools.dilateROI( roiCage , dilatation, dilatation , 1 );			
			System.out.println( "Number of point in dilated area: " + roiCage.getAsBooleanMask().getPoints().length );
			roiCage.optimizeBounds();
			
//			Sequence testSequence = new Sequence();
//			Icy.getMainInterface().addSequence( testSequence );
//			testSequence.addROI( roiCage );
//			roiCage.setEditable( true );
			
			ArrayList<ROI2D> roiCageList = new ArrayList<ROI2D>();
			roiCageList.add( roiCage );			
			setROICage( roiCageList );
			
			//setROICage( cageFloorROIList );
			
			
//			ROI2DRectangle roi = new ROI2DRectangle(  new Point2D.Double( 186 , 153 ), new Point2D.Double( 189 , 216 ) ); 
//			roiCage50x50 = ROIUtil.getSubtraction( roiCage50x50, roi );
			

			
			//updateAllROICage();

		}
		
		
		
		
		if ( cageMode == CAGE_MODE.MULTI_NICO )
		{
			
			ROI2DPolygon roiCage50x50_A = new ROI2DPolygon( new Point2D.Double( 58, 86 ) );
			roiCage50x50_A.addNewPoint( new Point2D.Double( 391,86 ), false);
			roiCage50x50_A.addNewPoint( new Point2D.Double( 391,312 ), false);
			roiCage50x50_A.addNewPoint( new Point2D.Double( 58,312 ), false);
			roiCage50x50_A.setCreating( false );
						
			ROI2DPolygon roiCage50x50_B = new ROI2DPolygon( new Point2D.Double( 513,79 ) );
			roiCage50x50_B.addNewPoint( new Point2D.Double( 623,79 ), false);
			roiCage50x50_B.addNewPoint( new Point2D.Double( 621,189 ), false);
			roiCage50x50_B.addNewPoint( new Point2D.Double( 512,189 ), false);
			roiCage50x50_B.setCreating( false );

			ROI2DPolygon roiCage50x50_C = new ROI2DPolygon( new Point2D.Double( 513,201 ) );
			roiCage50x50_C.addNewPoint( new Point2D.Double( 623,201 ), false);
			roiCage50x50_C.addNewPoint( new Point2D.Double( 621,312 ), false);
			roiCage50x50_C.addNewPoint( new Point2D.Double( 512,312 ), false);
			roiCage50x50_C.setCreating( false );
			
			// 304

			ROI2DRectangle roiGate_A = new ROI2DRectangle( 388,119,526,148 );
			ROI2DRectangle roiGate_B = new ROI2DRectangle( 388,240,526,269 );
			
			kinectStreamer.setSequenceForOverlay( infraOut );
			
			ArrayList<ROI2D> cageFloorROIList = new ArrayList<ROI2D>();
			
			cageFloorROIList.add( roiCage50x50_A );
			cageFloorROIList.add( roiCage50x50_B );
			cageFloorROIList.add( roiCage50x50_C );
			
			cageFloorROIList.add( roiGate_A );
			cageFloorROIList.add( roiGate_B );
			
			setROICageFloor( cageFloorROIList );
			

			ROI2DArea roiCage = new ROI2DArea( cageFloorMask );
			int dilatation = 2;
			System.out.println("Dilatation of floor (nb pixels): " + dilatation );
			System.out.println( "Number of point in dilated area: " + roiCage.getAsBooleanMask().getPoints().length );
			roiCage= MorphoROITools.dilateROI( roiCage , dilatation, dilatation , 1 );			
			System.out.println( "Number of point in dilated area: " + roiCage.getAsBooleanMask().getPoints().length );
			roiCage.optimizeBounds();
			
//			Sequence testSequence = new Sequence();
//			Icy.getMainInterface().addSequence( testSequence );
//			testSequence.addROI( roiCage );
//			roiCage.setEditable( true );
			
			ArrayList<ROI2D> roiCageList = new ArrayList<ROI2D>();
			roiCageList.add( roiCage );			
			setROICage( roiCageList );
			
			//setROICage( cageFloorROIList );
			
			
//			ROI2DRectangle roi = new ROI2DRectangle(  new Point2D.Double( 186 , 153 ), new Point2D.Double( 189 , 216 ) ); 
//			roiCage50x50 = ROIUtil.getSubtraction( roiCage50x50, roi );
			

			
			//updateAllROICage();

		}
		
		
		/*
		if ( cageMode == CAGE_MODE.SUPER_BLOCKS )
		{
			
//			ROI2DPolygon roiCage50x50 = new ROI2DPolygon( new Point2D.Double( 65,97 ) );
//			roiCage50x50.addNewPoint( new Point2D.Double( 433, 101), false);
//			roiCage50x50.addNewPoint( new Point2D.Double( 432, 349), false);
//			roiCage50x50.addNewPoint( new Point2D.Double(  60, 346), false);
//			roiCage50x50.setCreating( false );
//			
			
			// copy of background to avoid walls reflextions
			ROI2DPolygon roiCage50x50 = new ROI2DPolygon( new Point2D.Double( 76-1,98-1 ) );
			roiCage50x50.addNewPoint( new Point2D.Double( 509+1, 98-1), false); // 429
			roiCage50x50.addNewPoint( new Point2D.Double( 509+1, 326+1), false);
			roiCage50x50.addNewPoint( new Point2D.Double(  76-1, 326+1), false);
			roiCage50x50.setCreating( false );
			
//			ROI2DRectangle roi = new ROI2DRectangle(  new Point2D.Double( 186 , 153 ), new Point2D.Double( 189 , 216 ) ); 
//			roiCage50x50 = ROIUtil.getSubtraction( roiCage50x50, roi );
			
			LiveMouseTracker.ROICage = roiCage50x50;

			ROI2DPolygon roiCage50x50Floor = new ROI2DPolygon( new Point2D.Double( 76,98 ) );
			roiCage50x50Floor.addNewPoint( new Point2D.Double( 509,98 ), false);
			roiCage50x50Floor.addNewPoint( new Point2D.Double( 509,326 ), false);
			roiCage50x50Floor.addNewPoint( new Point2D.Double(  76,326 ), false);
			roiCage50x50Floor.setCreating( false );

			LiveMouseTracker.ROICageFloor = roiCage50x50Floor;

			updateAllROICage();
			
		}
		*/
		if ( cageMode == CAGE_MODE.CLASSIC_16 || cageMode == CAGE_MODE.RATS_25 )
		{
			ArrayList<ROI2D> cageFloorROIList = new ArrayList<ROI2D>();
			
			/*
			ROI2DPolygon roiCage50x50 = new ROI2DPolygon( new Point2D.Double( 86-5+3, 55-5-17 ) );
			roiCage50x50.addNewPoint( new Point2D.Double( 420+5+3, 55-5 -17), false);
			roiCage50x50.addNewPoint( new Point2D.Double( 420+5+3, 395+5 -17), false);
			roiCage50x50.addNewPoint( new Point2D.Double(  86-5+3, 395+5 -17), false);
			roiCage50x50.setCreating( false );

			LiveMouseTracker.ROICage = roiCage50x50;
			*/

			ROI2DPolygon roiCage50x50Floor = new ROI2DPolygon( new Point2D.Double( 86-5+3 +30, 55-5-17 +30 ) );
			roiCage50x50Floor.addNewPoint( new Point2D.Double( 420+5+3 -30 , 55-5 -17 +30 ), false);
			roiCage50x50Floor.addNewPoint( new Point2D.Double( 420+5+3 -30 , 395+5 -17 -30 ), false);
			roiCage50x50Floor.addNewPoint( new Point2D.Double(  86-5+3 +30 , 395+5 -17 -30 ), false);
			roiCage50x50Floor.setCreating( false );

			//LiveMouseTracker.ROICageFloor = roiCage50x50Floor;
			cageFloorROIList.add( roiCage50x50Floor );			
			setROICageFloor( cageFloorROIList );

			ROI2DArea roiCage = new ROI2DArea( cageFloorMask );
			int dilatation = 20;
			System.out.println("Dilatation of floor (nb pixels): " + dilatation );
			System.out.println( "Number of point in dilated area: " + roiCage.getAsBooleanMask().getPoints().length );
			roiCage= MorphoROITools.dilateROI( roiCage , dilatation, dilatation , 1 );			
			System.out.println( "Number of point in dilated area: " + roiCage.getAsBooleanMask().getPoints().length );
			roiCage.optimizeBounds();
			
			ArrayList<ROI2D> roiCageList = new ArrayList<ROI2D>();
			roiCageList.add( roiCage );			
			setROICage( roiCageList );
			
			
			
			updateAllROICage();
		}
		 
		
		if ( cageMode == CAGE_MODE.SIMPLE_JEREMY  )
		{
			// 95,72
			// 461,196

			ROI2DRectangle roiCage50x50_A = new ROI2DRectangle( 95 , 72, 461, 196 );
			
			kinectStreamer.setSequenceForOverlay( infraOut );
			
			ArrayList<ROI2D> cageFloorROIList = new ArrayList<ROI2D>();
			
			cageFloorROIList.add( roiCage50x50_A );			
			setROICageFloor( cageFloorROIList );
			
			ROI2DArea roiCage = new ROI2DArea( cageFloorMask );
			int dilatation = 3;
			System.out.println("Dilatation of floor (nb pixels): " + dilatation );
			System.out.println( "Number of point in dilated area: " + roiCage.getAsBooleanMask().getPoints().length );
			roiCage= MorphoROITools.dilateROI( roiCage , dilatation, dilatation , 1 );			
			System.out.println( "Number of point in dilated area: " + roiCage.getAsBooleanMask().getPoints().length );
			roiCage.optimizeBounds();
			
			ArrayList<ROI2D> roiCageList = new ArrayList<ROI2D>();
			roiCageList.add( roiCage );			
			setROICage( roiCageList );

		}


		debugOverlay = new DebugOverlay("debug live tracking");
		depthOut.addOverlay( debugOverlay );
		infraOut.addOverlay( debugOverlay );
		//infraMergedOut.addOverlay( debugOverlay );

//		machineLearningMonitor = new MachineLearningMonitor();
//		depthOut.addOverlay( machineLearningMonitor );
//		infraOut.addOverlay( machineLearningMonitor );

		//depthOut.getFirstViewer().getLut().getLutChannel(0).setMinMax( 796, 1082 );

		// creates depth diff sequence and image
		IcyBufferedImage image = new IcyBufferedImage( 512, 424, 1, DataType.SHORT );
		difDepthInTimeSequence = new Sequence( "DifDepthMap" , image );
		if ( DISPLAY_DIF_DEPTH_SEQUENCE )
		{
			addSequence( difDepthInTimeSequence );
		}

		// creates infra diff sequence and image
		IcyBufferedImage image2 = new IcyBufferedImage( 512, 424, 1, DataType.SHORT );

		difInfraInTimeSequence = new Sequence( "DifInfra" , image2 );
		if( DISPLAY_DIF_INFRA_SEQUENCE )
		{
			addSequence( difInfraInTimeSequence );
		}
		// tail image
		tailSequence = new Sequence("Tail Map");
		tailImage = new IcyBufferedImage( 512, 424, 1 , DataType.BYTE );
		tailSequence.setImage( 0 , 0 , tailImage );
		if ( DISPLAY_TAIL_SEQUENCE )
		{
			addSequence( tailSequence );
		}

		trackContainer = new TrackContainer();
		setAnimals( MAX_NUMBER_OF_ANIMALS );

//		trackContainer.animalTrackSegmentPool = new AnimalPool();
		//TrackContainer.animalTrackSegmentPool = new AnimalPool();


//		for ( int i = 0 ; i < MAX_NUMBER_OF_ANIMALS ; i++ )
//		{
//			trackContainer.animalTrackSegmentPool.addAnimal( new Animal( ""+(char)( 'A'+i ) ) );
//		}

//			trackContainer.animalTrackSegmentPool.addAnimal( new Animal( "B" , Color.GREEN ) );
//			trackContainer.animalTrackSegmentPool.addAnimal( new Animal( "C" , Color.BLUE ) );
//			trackContainer.animalTrackSegmentPool.addAnimal( new Animal( "D" , Color.CYAN ) );
//trackContainer.anonymousTrackSegmentPool = new TrackSegmentPool(trackSegmentArrayList)

		/*
		 * 4 blanches:
955000004064575
955000004064669
955000004064845
955000004064670
		 */

//		TrackContainer.animalTrackSegmentPool.getAnimalList().get( 0 ).setRfidID( "955000004064575" );
//		TrackContainer.animalTrackSegmentPool.getAnimalList().get( 1 ).setRfidID( "955000004064669" );
//		TrackContainer.animalTrackSegmentPool.getAnimalList().get( 2 ).setRfidID( "955000004064845" );
//		TrackContainer.animalTrackSegmentPool.getAnimalList().get( 3 ).setRfidID( "955000004064670" );
//
//		mainAnimalPool.addAnimal( new Animal( "E" , Color.MAGENTA ) );
//		mainAnimalPool.addAnimal( new Animal( "F" , Color.ORANGE ) );
//		mainAnimalPool.addAnimal( new Animal( "G" , Color.PINK ) );
//		mainAnimalPool.addAnimal( new Animal( "H" , Color.YELLOW ) );

//		trackContainer.anonymousTrackSegmentPool =
//				new TrackSegmentPool( trackContainer.animalTrackSegmentPool );

		trackPoolOverlay = new TrackPoolOverlay( "track pool overlay", trackContainer.anonymousTrackSegmentPool , trackContainer.animalTrackSegmentPool , clock );

		depthOut.addOverlay( trackPoolOverlay );
		infraOut.addOverlay( trackPoolOverlay );

//		perfLogger = new PerfLoggerOverlay();
//		depthOut.addOverlay( perfLogger );
//		infraOut.addOverlay( perfLogger );
//		perfLogger.addMessage( new Message("Main LMT", Color.green ) );

//		threadMonitor = new ThreadMonitorOverlay();
//		infraOut.addOverlay( threadMonitor );


		//infraMergedOut.addOverlay( trackPoolOverlay );

		mouseDetector = new MouseDetector(backgroundSequence, infraOut, depthOut, backgroundHeightMapBuilder);

//		difDepthCumulatedSequence = new Sequence("Dif Inf Cum");
//		addSequence( difDepthCumulatedSequence );

		eventOverlay = new EventOverlay("Event");
		eventOverlay.setConstraint( new Point2D.Double( 512/2, 424/2 ), 150 );
		infraOut.addOverlay( eventOverlay );
		//infraMergedOut.addOverlay( eventOverlay );

//		trackProblemPool = new TrackProblemPool( infraOut , trackContainer.animalTrackSegmentPool , trackContainer.anonymousTrackSegmentPool , eventOverlay );

		if ( RFID_ENABLED )
		{
			rfidManager = new RFIDManager2();
//			rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 250, 85 ), 20 , "COM4" ) );
//			rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 250, 330 ), 20 , "COM3" ) );

			/* 4 antenna model */
			/*
			rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 392,  211 ) , 20 , "COM6" ) );
			rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 268,   75 ) , 20 , "COM7" ) );
			rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 253,  339 ) , 20 , "COM8" ) );
			rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 126,  198 ) , 20 , "COM9" ) );
			 */

			/* 16 antenna model with 5 cm diameter coils */
/*
			rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 133,   81 ) , 20 , "COM30" ) ); // 23
			rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 214,   81 ) , 20 , "COM31" ) ); // 25
			rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 296,   81 ) , 20 , "COM32" ) ); // 13
			rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 378,   81 ) , 20 , "COM33" ) ); // 12

			rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 133,  165 ) , 20 , "COM34" ) ); // 24
			rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 214,  165 ) , 20 , "COM35" ) ); // 26
			rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 296,  165 ) , 20 , "COM36" ) ); // 17
			rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 378,  165 ) , 20 , "COM37" ) ); // 11

			rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 133,  246 ) , 20 , "COM38" ) ); // 20
			rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 214,  246 ) , 20 , "COM39" ) ); // 19
			rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 296,  246 ) , 20 , "COM40" ) ); // 16
			rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 378,  246 ) , 20 , "COM41" ) ); // 14

			rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 133,  324 ) , 20 , "COM42" ) ); // 21
			rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 214,  324 ) , 20 , "COM43" ) ); // 22
			rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 296,  324 ) , 20 , "COM44" ) ); // 15
			rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 378,  324 ) , 20 , "COM45" ) ); // 18
*/
			/* PROBLEME ANTENNES MAL PLACEES
			 * 16 antenna model with 10 cm diameter coils
			 * */
/*			rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 133,   81 ) , 30 , "COM30" ) ); // 23
			rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 214,   81 ) , 30 , "COM31" ) ); // 25
			rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 296,   81 ) , 30 , "COM32" ) ); // 13
			rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 378,   81 ) , 30 , "COM33" ) ); // 12

			rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 133,  165 ) , 30 , "COM34" ) ); // 24
			rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 214,  165 ) , 30 , "COM35" ) ); // 26
			rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 296,  165 ) , 30 , "COM36" ) ); // 17
			rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 378,  165 ) , 30 , "COM37" ) ); // 11

			rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 133,  246 ) , 30 , "COM38" ) ); // 20
			rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 214,  246 ) , 30 , "COM39" ) ); // 19
			rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 296,  246 ) , 30 , "COM40" ) ); // 16
			rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 378,  246 ) , 30 , "COM41" ) ); // 14

			rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 133,  324 ) , 30 , "COM42" ) ); // 21
			rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 214,  324 ) , 30 , "COM43" ) ); // 22
			rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 296,  324 ) , 30 , "COM44" ) ); // 15
			rfidManager.addAntenna( new RFIDAntenna2( new Point2D.Double( 378,  324 ) , 30 , "COM45" ) ); // 18
*/



			/*
			 * 16 antenna model with 10 cm diameter coils
			 * CLASSIC MODEL
			 * */

			if ( cageMode == CAGE_MODE.MULTI_NICO )
			{
				/*
				100: 136,123
				101: 79,112
				103:254,112
				104: 192,167
				105:360,132
				106: 132,279
				107:80,215
				108:258,217
				109:254,280
				110:364,257
				111:302,222
				*/
				// 30
				int size = 30; // 30
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 136,   123 ) , size , "COM100" ) );	
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 79,    112 ) , size , "COM101" ) );	
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 254,   112 ) , size , "COM103" ) );	
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 192,   167 ) , size , "COM104" ) );	
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 360,   132 ) , size , "COM105" ) );	
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 132,   279 ) , size , "COM106" ) );	
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 80,    225 ) , size , "COM107" ) );	
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 258,   217 ) , size , "COM108" ) );					
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 254,   280 ) , size , "COM109" ) );	
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 364,   257 ) , size , "COM110" ) );					
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 302,   222 ) , size , "COM111" ) );
				
				
			}

			if ( cageMode == CAGE_MODE.SUPER_BLOCKS )
			{
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 158,   129 ) , 30 , "COM100" ) );	
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double(  94,   122 ) , 30 , "COM101" ) );	

				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double(  97,   252 ) , 30 , "COM107" ) );	
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 153,   295 ) , 30 , "COM106" ) );	

				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 283,   129 ) , 30 , "COM103" ) );	
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 219,   182 ) , 30 , "COM104" ) );	

				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 267,   247 ) , 30 , "COM108" ) );	
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 274,   296 ) , 30 , "COM109" ) );	
				
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 348,   253 ) , 30 , "COM111" ) );	
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 392,   274 ) , 30 , "COM110" ) );	
				
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 400,   153 ) , 30 , "COM105" ) );	
				
			}
			
			if ( cageMode == CAGE_MODE.CLASSIC_16 )
			{
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 133,   81 ) , 30 , "COM30" ) ); // 23
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 214,   81 ) , 30 , "COM31" ) ); // 25
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 296,   81 ) , 30 , "COM32" ) ); // 13
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 378,   81 ) , 30 , "COM33" ) ); // 12

				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 133,  166 ) , 30 , "COM34" ) ); // 24
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 214,  166 ) , 30 , "COM35" ) ); // 26
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 296,  166 ) , 30 , "COM36" ) ); // 17
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 378,  166 ) , 30 , "COM37" ) ); // 11

				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 133,  249 ) , 30 , "COM38" ) ); // 20
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 214,  249 ) , 30 , "COM39" ) ); // 19
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 296,  249 ) , 30 , "COM40" ) ); // 16
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 378,  249 ) , 30 , "COM41" ) ); // 14

				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 133,  336 ) , 30 , "COM42" ) ); // 21
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 214,  336 ) , 30 , "COM43" ) ); // 22
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 296,  336 ) , 30 , "COM44" ) ); // 15
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 378,  336 ) , 30 , "COM45" ) ); // 18
			}
			
			
			if ( cageMode == CAGE_MODE.MULTI_CLASSIC_16 )
			{
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 133,   81 ) , 30 , "COM30" ) ); // 23
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 214,   81 ) , 30 , "COM31" ) ); // 25
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 296,   81 ) , 30 , "COM32" ) ); // 13
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 378,   81 ) , 30 , "COM33" ) ); // 12

				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 133,  166 ) , 30 , "COM34" ) ); // 24
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 214,  166 ) , 30 , "COM35" ) ); // 26
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 296,  166 ) , 30 , "COM36" ) ); // 17
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 378,  166 ) , 30 , "COM37" ) ); // 11

				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 133,  249 ) , 30 , "COM38" ) ); // 20
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 214,  249 ) , 30 , "COM39" ) ); // 19
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 296,  249 ) , 30 , "COM40" ) ); // 16
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 378,  249 ) , 30 , "COM41" ) ); // 14

				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 133,  336 ) , 30 , "COM42" ) ); // 21
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 214,  336 ) , 30 , "COM43" ) ); // 22
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 296,  336 ) , 30 , "COM44" ) ); // 15
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 378,  336 ) , 30 , "COM45" ) ); // 18
				
				int shiftX = 506+23+6;
				
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 133+shiftX,   81 ) , 30 , "COM50" ) ); 
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 214+shiftX,   81 ) , 30 , "COM51" ) ); 
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 296+shiftX,   81 ) , 30 , "COM52" ) ); 
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 378+shiftX,   81 ) , 30 , "COM53" ) ); 

				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 133+shiftX,  166 ) , 30 , "COM54" ) ); 
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 214+shiftX,  166 ) , 30 , "COM55" ) ); 
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 296+shiftX,  166 ) , 30 , "COM56" ) ); 
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 378+shiftX,  166 ) , 30 , "COM57" ) ); 

				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 133+shiftX,  249 ) , 30 , "COM58" ) ); 
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 214+shiftX,  249 ) , 30 , "COM59" ) ); 
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 296+shiftX,  249 ) , 30 , "COM60" ) ); 
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 378+shiftX,  249 ) , 30 , "COM61" ) ); 

				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 133+shiftX,  336 ) , 30 , "COM62" ) ); 
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 214+shiftX,  336 ) , 30 , "COM63" ) ); 
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 296+shiftX,  336 ) , 30 , "COM64" ) ); 
				rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( 378+shiftX,  336 ) , 30 , "COM65" ) ); 

				
			}

			/*
			 * Rat cage.
			 * */
			if ( cageMode == CAGE_MODE.RATS_25 )
			{
				//USE_MULTIPLE_IDENTITY_RECOVERY_WITH_MACHINE_LEARNING = false;
				double minX = 147 - 15 -10 +2.5;
				double minY = 95 - 15 -10 +2.5;
				
				double maxX = 147+57.5*5.0 + 15 + 10 -2.5;
				double maxY = 95+ 57.5*5.0 + 15 + 10 -2.5;
				
				double nbRow=5;
				int comNumber = 50;
				double stepX = (maxX-minX)/nbRow;
				double stepY = (maxY-minY)/nbRow;
				
				
				
				for ( int y=0; y<nbRow; y++ )
				{
					for ( int x=0; x<nbRow; x++ )
					{
						rfidManager.addAntenna( new RFIDAntenna( new Point2D.Double( minX+ x*stepX, minY+ y*stepY ) , 25 , "COM"+comNumber ) ); // 23
						comNumber++;
					}					
				}
				
				
			}




			// get all frequencies
			{
//				for ( RFIDAntenna2 antenna : rfidManager.getAntennaList() )
//				{
//					System.out.println("Reading antenna frequency " + antenna );
//					double frequency = antenna.readFrequency();
//					System.out.println("Frequency: " + frequency );
//					double error = Math.abs( 134 - frequency );
//					System.out.println("Error: " + error );
//					if ( error < 3 )
//					{
//						System.out.println("Error ok.");
//					}else
//					{
//						System.out.println("Error is higher than 3KHz. Might affect antenna's range.");
//					}
//				}
			}

		}

		// Start networking

		if ( ENABLE_TCP_LIVE_DATA_SERVER )
		{
			networkResultServer = new NetworkResultServer();
			networkResultServerThread = new Thread( networkResultServer );
			networkResultServerThread.start();
		}

		{
			rfidRemoteStop = new RFIDRemoteStop();
			udpEventReceiver = new UDPEventReceiver();
			rfidRemoteIdentityControl = new RFIDIdentityControl();
		}
		
		// UNRELEASED MULTI

		if ( guiPanel.getCheckBoxMultiArenaMode().isSelected() )
		{
			try
			{
				lmtRemoteAreaServer = new LMTRemoteAreaServer( false , this );
			}catch( NoClassDefFoundError e )
			{
				System.out.println("No multi arena code present.");
			}
		}


		initDone = true;
		fireEndOfInitEvent();

		Field[] fieldArray = this.getClass().getDeclaredFields();
		for ( Field field : fieldArray )
		{
			try {
				System.out.println( field.getName() + " : " + field.get(this) );
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}

		processThread = new Thread(  new Runnable() {

			@Override
			public void run() {
				mainProcessThread();
			}
		});
		processThread.setName("Main process thread");
		processThread.setPriority( Thread.MAX_PRIORITY );
		processThread.start();

	}

	Thread processThread;

	MouseDetector mouseDetector ;

	@Override
	public void actionPerformed(ActionEvent e) {

		if ( e.getSource() == guiPanel.getSelect1AnimalButton() ) {
			guiPanel.getNumberOfMaxAnimalTextField().setText("1");
			setAnimals( 1 );
		}
		if ( e.getSource() == guiPanel.getSelect2AnimalButton() )
		{
			guiPanel.getNumberOfMaxAnimalTextField().setText("2");
			setAnimals( 2 );
			}
		if ( e.getSource() == guiPanel.getSelect3AnimalButton() )
		{
			guiPanel.getNumberOfMaxAnimalTextField().setText("3");
			setAnimals( 3 );
		}

		if ( e.getSource() == guiPanel.getSelect4AnimalButton() )
		{
			guiPanel.getNumberOfMaxAnimalTextField().setText("4");
			setAnimals( 4 );
		}

		if ( e.getSource() == guiPanel.getStopButton() )
		{
			guiPanel.getStopButton().setEnabled( false );
			guiPanel.getPauseButton().setEnabled( false );
//			kinectStreamer.stopLive();
			shutDown();
		}

		if ( e.getSource() == guiPanel.getPauseButton() )
		{
			System.out.println("Pause button");
			pauseAllProcess = !pauseAllProcess;
			if ( pauseAllProcess )
			{
				guiPanel.getPauseButton().setText("*PAUSED*");
			}else
			{
				guiPanel.getPauseButton().setText("Pause");
			}
		}

		if ( e.getSource() == guiPanel.getStartLiveButton() )
		{
			startLive();
		}

//		if ( e.getSource() == guiPanel.getSaveAllTracksButton() )
//		{
//			saveTracks( false );
//		}
//
//		if ( e.getSource() == guiPanel.getLoadAllTracksButton() )
//		{
//			loadTracks();
//		}
//
//		if ( e.getSource() == guiPanel.getSaveTrackAsStreamButton() )
//		{
//			saveTracks( true );
//		}

	}

	// UNRELEASED MULTI

	static LMTRemoteAreaServer lmtRemoteAreaServer = null;

	private void startLive() {

		System.out.println("Start live button");

		guiPanel.getStartLiveButton().setEnabled( false );
		guiPanel.getStopButton().setEnabled( true );
		guiPanel.getPauseButton().setEnabled( true );
		
		//kinectStreamer.startLive(); // previous init location

// OLD location of LMTRemoteAreaServer
//		if ( guiPanel.getCheckBoxMultiArenaMode().isSelected() )
//		{
//			try
//			{
//				lmtRemoteAreaServer = new LMTRemoteAreaServer( false , this );
//			}catch( NoClassDefFoundError e )
//			{
//				System.out.println("No multi arena code present.");
//			}
//		}

		
		System.out.println("Creating sequences infra and depthOut");
		infraOut = new Sequence("Infra Merged Out");
		depthOut = new Sequence("Infra Depth Out");

		Icy.getMainInterface().addSequence( infraOut );		
		//Icy.getMainInterface().addSequence( depthOut );

		kinectStreamer.startLive(); // was previously -20 lines before


		// infraMergedOut = new Sequence("Infra Merged");
		//Icy.getMainInterface().addSequence( infraMergedOut );


	}

	private void loadTracks() {
		System.out.println("Loading...");
		Experiment experiment = new Experiment( LiveMouseTracker.BASE_FOLDER + getExperimentName() );
		experiment.load( null );
	}

	private void saveTracks(boolean streamMode ) {
		System.out.println("Saving all remaining tracks...");
		Experiment experiment = new Experiment( LiveMouseTracker.BASE_FOLDER + getExperimentName() );
		System.out.println("//////////////////***************** SAVING TRACKS TO DATABASE");
		experiment.save( trackContainer , streamMode ) ; //getMainAnimalPool() , trackContainer.anonymousTrackSegmentPool );
	}
//
//	private void saveOrAddTracks()
//	{
//		System.out.println("Saving or adding track in database...");
//		Experiment experiment = new Experiment( LiveMouseTracker.BASE_FOLDER + getExperimentName() );
//		experiment.saveOrAdd( trackContainer ) ;
//	}

	/** return current T in process */
	public static int getT() {

		return clock.getT();

	}

	public static AnimalPool getMainAnimalPool() {
		return trackContainer.animalTrackSegmentPool;
	}

	public static void clearTrackSegmentList() {

		System.out.println("Clear track segment list.");
		trackContainer.anonymousTrackSegmentPool.clear();
	}

	/**
	 * Set the
	 * @param roiCage
	 */
	public static void addROIToInfraSequence( ROI roi ) {
		infraOut.addROI( roi );
//		infraMergedOut.addROI( roi );
	}

	/*
	public static ROI getROICageFloor() {
		ArrayList<ROI> roiList = infraOut.getROIs();
		for ( int i = roiList.size()-1 ; i >= 0 ; i--  )
		{
			ROI roi = roiList.get( i );
			if ( roi.getName().startsWith("cage floor limits" ) )
				return roi;
		}
		return null;
	}
	*/
	public static void removeROICageFloor()
	{
		for ( int i = infraOut.getROIs().size()-1 ; i >= 0 ; i--  )
		{
			ROI roi = infraOut.getROIs().get( i );
			if ( roi.getName().startsWith("cage floor limits" ) )
			{
				depthOut.removeROI( roi );
				infraOut.removeROI( roi );
				System.out.println("remove ROI cage floor : " + roi);
			}
		}
	}

	public static void updateAllROICage()
	{
		/*
		{
			ArrayList<ROI2D> roiCageList = new ArrayList<ROI2D>();
			roiCageList.add( ROICage );
			// UNRELEASED MULTI

			if ( lmtRemoteAreaServer != null )
			{
				for ( RegisteredArenaClient registeredClient : lmtRemoteAreaServer.getRegisteredClientList() )
				{
					roiCageList.add( new ROI2DPolygon ( registeredClient.getRemoteArenaInfo().cagePolygon ) );
				}
			}

			setROICage( roiCageList );
		}

		{
			ArrayList<ROI2D> roiCageFloorList = new ArrayList<ROI2D>();
			roiCageFloorList.add( ROICageFloor );
			// UNRELEASED MULTI

			if ( lmtRemoteAreaServer != null )
			{
				for ( RegisteredArenaClient registeredClient : lmtRemoteAreaServer.getRegisteredClientList() )
				{
					roiCageFloorList.add( new ROI2DPolygon( registeredClient.getRemoteArenaInfo().cageFloorPolygon ) );
				}
			}

			setROICageFloor( roiCageFloorList );
		}
		*/

	}

	public static void setROICageFloor( ArrayList<ROI2D> roiCageFloorList ) {

		removeROICageFloor();

//		ROI roi = getROICageFloor();
//		infraOut.removeROI( roi );
//		depthOut.removeROI( roi );

		BooleanMask2D mergedBooleanMask = new BooleanMask2D();
		for ( ROI2D roiFloor : roiCageFloorList )
		{
			roiFloor.setColor( Color.orange.darker() );
			roiFloor.setName( "cage floor limits" );
			roiFloor.setOpacity( 0 );
			infraOut.addROI( roiFloor );
			depthOut.addROI( roiFloor );
			System.out.println( "adding roi cage floor: " + roiFloor );
			//infraMergedOut.addROI( roiCageFloor );
			roiFloor.setReadOnly(true);
			mergedBooleanMask.add( roiFloor.getBooleanMask( true ));
		}

		cageFloorMask = mergedBooleanMask;

		/*
		ROI roi = getROICageFloor();
		infraOut.removeROI( roi );
		depthOut.removeROI( roi );

		roiCageFloor.setColor( Color.orange.darker() );
		roiCageFloor.setName( "cage floor limits" );
		infraOut.addROI( roiCageFloor );
		depthOut.addROI( roiCageFloor );
		//infraMergedOut.addROI( roiCageFloor );
		roiCageFloor.setReadOnly(true);
		cageFloorMask = roiCageFloor.getBooleanMask(true);
	*/
	}

	public static BooleanMask2D getCageBooleanMask()
	{
		return cageROIMask;
	}

//	public static ROI getROICage() {
//		for ( int i = infraOut.getROIs().size()-1 ; i >= 0 ; i--  )
//		{
//			ROI roi = infraOut.getROIs().get( i );
//			if ( roi.getName().startsWith("cage limits" ) )
//				return roi;
//		}
//		return null;
//	}

	public static void removeROICage()
	{
		ArrayList<ROI> roiList = new ArrayList<ROI>( infraOut.getROIs() );

		for ( ROI roi : roiList )
		{
			if ( roi.getName().startsWith("cage limits" ) )
			{
				depthOut.removeROI( roi );
				infraOut.removeROI( roi );
				System.out.println("removing ROI" + roi );
			}
		}
		/*
		for ( int i = infraOut.getROIs().size()-1 ; i >= 0 ; i--  )
		{
			ROI roi = infraOut.getROIs().get( i );
			if ( roi.getName().startsWith("cage limits" ) )
			{
				depthOut.removeROI( roi );
				infraOut.removeROI( roi );
				System.out.println("removing ROI" + roi );
			}
		}
		*/
	}

	public static void setROICage(ArrayList<ROI2D> roiCageList ) {

		removeROICage();
//		ROI roi = getROICage();
//		infraOut.removeROI( roi );
//		depthOut.removeROI( roi );

		BooleanMask2D mergedBooleanMask = new BooleanMask2D();
		for ( ROI2D roiCage : roiCageList )
		{
			roiCage.setColor( Color.orange );
			roiCage.setName( "cage limits" );
			roiCage.setOpacity( 0 );
			infraOut.addROI( roiCage );
			depthOut.addROI( roiCage );

			roiCage.setReadOnly(true);
			System.out.println("adding ROI cage " + roiCage );

			mergedBooleanMask.add( roiCage.getBooleanMask( true ));
		}

		
//		cageROI = roiCage;
		cageROIMask = mergedBooleanMask;


		/*
		ROI roi = getROICage();
		infraOut.removeROI( roi );
		depthOut.removeROI( roi );

//		for ( int i = infraOut.getROIs().size()-1 ; i >= 0 ; i--  )
//		{
//			ROI roi = infraOut.getROIs().get( i );
//			if ( roi.getName().startsWith("cage limits" ) )
//			{
//				infraOut.removeROI( roi );
//				depthOut.removeROI( roi );
//			}
//		}

		roiCage.setColor( Color.orange );
		roiCage.setName( "cage limits" );
		infraOut.addROI( roiCage );
		depthOut.addROI( roiCage );
		// infraMergedOut.addROI( roiCage );
		roiCage.setReadOnly(true);
		cageROI = roiCage;
		cageROIMask = roiCage.getBooleanMask(true);
		*/
	}

	/** reset the background with the current depth map. */
	public static void resetBackGround() {

		backgroundHeightMapBuilder.correctBackGround( depthImage,
				getCageBooleanMask() );
// before:
//		getROICage().getBooleanMask2D( 0, 0, 0, true ) );

	}

	public static void addOverlayToInfraSequence( Overlay overlay ) {
		infraOut.addOverlay( overlay );
//		infraMergedOut.addOverlay( overlay );
	}

	public static void addEvent(Event event) {
		eventOverlay.addEvent( event );
	}


	public static void removeROIToInfraSequence(ROI roi) {
		infraOut.removeROI( roi );
	}

	/** call this to disable all current machine learning running.
	 * They may continue to run but their result will not be affected to tracks.
	 * Use this if several different actors are taking decision and may interfer. ( RFID vs Machine Learning for instance )
	 * */
	public static void disableCurrentMachineLearning() {

		multiIdentityAgentManager.disableAllCurrentRunningAgent();

	}
	@Override
	public void icyFrameOpened(IcyFrameEvent e) {}
	@Override
	public void icyFrameClosing(IcyFrameEvent e) {}
	@Override
	public void icyFrameClosed(IcyFrameEvent e) {
		// close MPEG stream
		//guiPanel.getStopButton().doClick();
		shutDown();
	}

	public static boolean performingShutDown = false;

	private void shutDown() {

		if ( performingShutDown == true )
		{
			System.out.println("[ShutDown] Already shutting down.");
			return;
		}
		ttlSynchronizer.sendTTL( TTL_SIGNAL.SYNCHRO_SHUTDOWN );

		performingShutDown = true;

		ThreadUtil.bgRun( new Runnable() {

			@Override
			public void run() {

				while ( inMainProcessingLoop )
				{
					System.out.println("Waiting for the end of main processing loop...");
					try {
						Thread.sleep( 500 );
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				AnnounceFrame savingDataAnnounceFrame = new AnnounceFrame("Saving data...");

				System.out.println("[ShutDown] Enter function.");

				//System.out.println("[ShutDown] KinectStream State: " + kinectStreamer.getState() );
//				if ( kinectStreamer.getState() == StreamerState.PLAYFILE ){
//					kinectStreamer.stopRecordedPlay();
//				}

//				if ( kinectStreamer.getState() == StreamerState.LIVE ){
				
				try {					
					kinectStreamer.stopLive();
				}catch( Exception e)
				{
					System.out.println("Failed to stop kinect stream");
					e.printStackTrace();
				}
				
//				}

				if ( RFID_ENABLED )
				{
					System.out.println("[ShutDown] Shutdown RFID Manager" );
					if ( rfidManager != null )
					{
						rfidManager.kinectStopped();
					}
				}

				System.out.println("[ShutDown] Joining streaming Thread..." );
				if ( saveToSQLStreamingThread != null )
				{
					try {
						saveToSQLStreamingThread.join();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				System.out.println("[ShutDown] Join passed." );

				System.out.println("[ShutDown] Closing mpeg recorder." );

				mpegTimeLapseRecorder.shutDown();

				/* THERMAL
				if ( mpegTimeLapseThermalRecorder != null )
				{
					mpegTimeLapseThermalRecorder.shutDown();
				}
				*/

				try{
				for ( Animal animal : trackContainer.animalTrackSegmentPool.animalList )
				{
					try
					{
						animal.mpegMedaillonRecorder.shutDown();
					}catch( NullPointerException e ) {}
				}
				}
				catch(NullPointerException e2 ){};

//				try{
//					mpegRecorder.close();
//				}catch( Exception e ){
//					System.err.println("Error in MPEG Close");
//					e.printStackTrace();
//				};

				System.out.println("[ShutDown] Mpeg recorder passed." );

				System.out.println("[ShutDown] Saving final track set." );
				saveTracks( false ); // flush all tracks
				System.out.println("[ShutDown] Save final track passed." );

				System.out.println("[ShutDown] Shutdown aviSoftReceiver." );
				if ( aviSoftEventReceiver!= null )
				{
					aviSoftEventReceiver.interrupt();
				}

				System.out.println("[ShutDown] Shutdown finished." );

				savingDataAnnounceFrame.close();
			}
		});

	}

//	private void closeMPEGRecording() {
//
//		if ( mpegRecorder != null )
//		{
//			if( mpegRecorder.isOpen() )
//			{
//				mpegRecorder.close();
//				mpegRecorder = null;
//			}
//		}
//	}
	@Override
	public void icyFrameIconified(IcyFrameEvent e) {}
	@Override
	public void icyFrameDeiconified(IcyFrameEvent e) {}
	@Override
	public void icyFrameActivated(IcyFrameEvent e) {}
	@Override
	public void icyFrameDeactivated(IcyFrameEvent e) {}
	@Override
	public void icyFrameInternalized(IcyFrameEvent e) {}
	@Override
	public void icyFrameExternalized(IcyFrameEvent e) {}




}
