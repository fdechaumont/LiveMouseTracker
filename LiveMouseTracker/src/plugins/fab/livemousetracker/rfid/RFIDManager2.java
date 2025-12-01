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
package plugins.fab.livemousetracker.rfid;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;

import com.mchange.v2.lang.ThreadUtils;

import icy.common.listener.AcceptListener;
import icy.main.Icy;
import icy.system.thread.Processor;
import icy.system.thread.ThreadUtil;
import icy.type.point.Point3D;

//import com.fazecast.jSerialComm.SerialPort;

import plugins.fab.kinectdriver.KinectStreamer.StreamerState;
import plugins.fab.livemousetracker.Animal;
import plugins.fab.livemousetracker.LiveMouseTracker;
import plugins.fab.livemousetracker.detection.MouseDetection;
import plugins.fab.livemousetracker.track.TrackSegment;

/**
 * Created to work with new active RFID devices
 */
public class RFIDManager2 implements AntennaReadListener, AcceptListener {

	ArrayList<Antenna> antennaList = new ArrayList<Antenna>();
	/** list of events available (same as if we were recoding data) events can be late.*/
	ArrayList<AntennaReadEvent> eventList = new ArrayList<AntennaReadEvent>();
	/** All events available loaded from file. */
	ArrayList<AntennaReadEvent> allEventList = new ArrayList<AntennaReadEvent>();

	boolean shutDownDone = false;
	
	ArrayList<RFIDPairing> rfidPairingList;
	
	public RFIDManager2()
	{
		System.out.println("Constructor for reading/writing innerSerial in antenna only.");
	}
	
	public RFIDManager2( ArrayList<RFIDPairing> rfidPairingList ) {
		this.rfidPairingList = rfidPairingList;
		//System.out.println( "RFIDManager2 : rfidPairingList : " + rfidPairingList );
		solveAntennaComPortAffectation();		
		Icy.getMainInterface().addCanExitListener( this );
	}

//	public void addRemoteSystem( LMTRemoteAreaServer lmtRemoteAreaServer )
//	{
//		// Register RFID client's antenna
//		for ( ClientSystem lmtClient : lmtRemoteAreaServer.getClientList() )
//		{
//			for ( RemoteAntenna remoteAntenna : lmtClient.getAllRemoteAntenna() )
//			{
//				addAntenna( remoteAntenna );
//			}
//		}
//
//	}

	public void shutdown()
	{
		if ( shutDownDone )
		{
			System.out.println("RFIDManager 2 already shutted down");
			return;
		}
		
		Icy.getMainInterface().removeCanExitListener( this );
		for ( Antenna antenna : antennaList )
		{
			ThreadUtil.bgRun( new Runnable() {				
				@Override
				public void run() {
					antenna.shutdown();					
				}
			});			
		}
		shutDownDone = true;
	}
	
	public void removeAntenna( Antenna rfidAntenna )
	{
		synchronized ( antennaList) {
			antennaList.remove( rfidAntenna );
		}
	}

	public void addAntenna( Antenna rfidAntenna )
	{
		synchronized ( antennaList) {

			// check if antenna already exists. If yes, remove previous one.
			for ( Antenna antennaExisting : new ArrayList<Antenna>( antennaList ) )
			{
				if ( antennaExisting.getIdentifier().equals( rfidAntenna.getIdentifier() ) )
				{
					antennaList.remove( antennaExisting );
					System.out.println("RFIDManager2:addAntenna: removing existing antenna.");
				}
			}

			antennaList.add( rfidAntenna );
		}
		rfidAntenna.addRFIDAntennaListener( this );
	}

	public ArrayList<Antenna> getAntennaList()
	{
		synchronized ( antennaList) {
			return new ArrayList<Antenna>( antennaList );
		}
	}

	public void addEventToQueue( AntennaReadEvent event )
	{
		synchronized ( eventList ) {
			eventList.add( event );
		}
	}

	public void removeEvent( AntennaReadEvent event )
	{
		synchronized ( eventList ) {
			eventList.remove( event );
		}
	}

	public void clearEvents()
	{
		synchronized ( eventList ) {
			eventList.clear();
		}
	}

	public ArrayList<AntennaReadEvent> getEventList()
	{
		synchronized ( eventList ) {
			return new ArrayList<AntennaReadEvent>( eventList );
		}
	}

	@Override
	public void antennaEvent(AntennaReadEvent rfidEvent) {

//		System.out.println("[RFID] Event read.");
//		if ( LiveMouseTracker.getKinectStreamer().getState() == StreamerState.PLAYFILE )
//		{
//			return; // don't consider antenna during replays.
//		}

//		System.out.println("[RFID] Adding event.");

		synchronized ( eventList ) {
			eventList.add( rfidEvent );
			/*
			if( LiveMouseTracker.getKinectStreamer().isRecording() )
			{
				recordRFIDEvent( rfidEvent );
			}
			*/
		}

	}

	/*
	Document saveRFIDDocument = null;
	*/

/*
	private void recordRFIDEvent(AntennaReadEvent rfidEvent) {

		if ( saveRFIDDocument == null )
		{
			saveRFIDDocument = XMLUtil.createDocument( true );
			XMLUtil.addElement( saveRFIDDocument.getDocumentElement(), "RFID" );
		}

		Element RFIDElement = XMLUtil.getElement( saveRFIDDocument.getDocumentElement() , "RFID" );
		Element RFIDRecordElement = XMLUtil.addElement( RFIDElement, "EVENT" );
		XMLUtil.setAttributeIntValue( RFIDRecordElement, "t", rfidEvent.getMeasuredT() );
		XMLUtil.setAttributeFloatValue( RFIDRecordElement, "x", (float)rfidEvent.getLocation().getX() );
		XMLUtil.setAttributeFloatValue( RFIDRecordElement, "y", (float)rfidEvent.getLocation().getY() );
		XMLUtil.setAttributeFloatValue( RFIDRecordElement, "ray", rfidEvent.getRay() );
		XMLUtil.setAttributeValue( RFIDRecordElement, "id", rfidEvent.getId() );

	}

	private void saveRFIDRecord()
	{
		if ( saveRFIDDocument == null ) return;
		System.out.println("Saving RFID record...");

		KinectLiveRecorder liveKinectRecorder = LiveMouseTracker.getKinectStreamer().getLiveRecorder();
		if ( liveKinectRecorder == null ) return;

		// will save to rfid folder an XML file
		String xmlRFIDFile = liveKinectRecorder.getRecordingFolder() + "/rfid/rfid.xml";
		FileUtil.ensureParentDirExist( xmlRFIDFile );

		XMLUtil.saveDocument( saveRFIDDocument , xmlRFIDFile );
		System.out.println("RFID saved.");
	}
*/
	public void kinectStopped() {
//		saveRFIDRecord();
//		saveRFIDDocument = null; // reset to avoid side effect in case of multiple calls.
		for ( Antenna antenna : antennaList )
		{
			antenna.shutdown();
		}
	}

	@Override
	public boolean accept(Object source) {
		kinectStopped(); // save before quit
		return true;
	}

	ArrayList<AntennaReadEvent> eventListLoaded = new ArrayList<AntennaReadEvent>();

	/** Check if the events have been loaded by file. (play mode)*/
	boolean allEventLoaded = false;

	/*
	// parse all events and select the one of current t.
	public void loadEvents ( int t ) {

		if ( !allEventLoaded )
		{
			loadAllEvents();
		}

		for ( AntennaReadEvent event : eventListLoaded )
		{
			if ( event.getMeasuredT() == t )
			{
				addEventToQueue( event );
			}
		}

		// remove picked events int eventListLoaded

		eventListLoaded.removeAll( getEventList() );

	}
	*/

	/*
	public void loadAllEvents ( ) {

		try {

			if ( !allEventLoaded )
			{
				String xmlRFIDFile = LiveMouseTracker.getKinectStreamer().getPlayingFolder() + "/rfid/rfid.xml";
				System.out.println("Loading RFID file");
				Document loadRFIDDocument = XMLUtil.loadDocument( xmlRFIDFile );

				Element rfidElement = XMLUtil.getElement( loadRFIDDocument.getDocumentElement(), "RFID" );

				ArrayList<Element> events = XMLUtil.getElements( rfidElement , "EVENT" );
				System.out.println("Loading " + events.size() + " rfid events");

				for ( Element event : events )
				{
					int tEvent = XMLUtil.getAttributeIntValue( event , "t", 0 );
					int latency = XMLUtil.getAttributeIntValue( event , "latency", LiveMouseTracker.RFID_DEFAULT_LATENCY );
					float x = XMLUtil.getAttributeFloatValue( event , "x" , 0 );
					float y = XMLUtil.getAttributeFloatValue( event , "y" , 0 );
					float ray = XMLUtil.getAttributeFloatValue( event , "ray" , 0 );
					String id = XMLUtil.getAttributeValue( event , "id" , "" );
					{
						Point2D point = new Point2D.Double( x, y ) ;
						AntennaReadEvent rfidEvent = new AntennaReadEvent( tEvent, latency, id, point, ray );
						//addEvent( rfidEvent );
						eventListLoaded.add( rfidEvent );
						System.out.println("event loaded");
					}
				}

			//	allEventLoaded = true;

			}
		}catch( NullPointerException e )
		{
			System.err.println("Can't perform the loadAllEvents");
		}finally{
			allEventLoaded = true;

		}

//		System.out.println("Checking RFID data");

		for ( AntennaReadEvent event : eventListLoaded )
		{
			//System.out.println("Adding event to live. ");
			System.out.println("Adding RFID event: " + event );
			allEventList.add( event );
			//addEvent( event );
		}

	}
	*/
	
	public Antenna getAntennaAvailable( TrackSegment ts )
	{
		MouseDetection detection = ts.getDetection( LiveMouseTracker.getT() - 1 );
		Point3D p = detection.getMassCenter();
		
		Antenna antennaToActivate = null;
		double bestDistance = Double.MAX_VALUE;
		
		for ( Antenna antenna : getAntennaList() )
		{
			if ( antenna.isFaulty() ) continue;

			double distance = p.toPoint2D().distance( antenna.getLocation() );
			if ( distance > 30 ) continue;

				if ( distance < bestDistance )
				{
					bestDistance = distance;
					antennaToActivate = antenna;
				}
		}		
		return antennaToActivate;
	}
	
	/**
	 * This method will activate antenna with specific strategy 
	 * Strategy: -
	 * */
	public void activateAntennas2() {
		
		Antenna antennaToActivate = null;
		
		// date tracks.
		// super old tracks are not priority
		// very new ones are priority
		
		ArrayList<TrackSegment> allTrackSegments = new ArrayList<TrackSegment>();
		
		// increase nbFrameSinceLastRFIDReading for each animal tracks. 		
		
		ArrayList<TrackSegment> animalTrackList = LiveMouseTracker.trackContainer.animalTrackSegmentPool.
				getTrackSegmentsContaining( LiveMouseTracker.getT() - 1 );
		for ( TrackSegment track : animalTrackList )
		{
			track.nbFrameSinceLastRFIDReading++;
			allTrackSegments.add( track );
		}
		
		// increase nbFrameSinceLastRFIDReading for each animal anonymous tracks.
		
		ArrayList<TrackSegment> anonymousTrackList = LiveMouseTracker.trackContainer.anonymousTrackSegmentPool.
				getTrackSegmentsContaining( LiveMouseTracker.getT() - 1 );

		for ( TrackSegment track : anonymousTrackList )
		{
			track.nbFrameSinceLastRFIDReading++;
			allTrackSegments.add( track );
		}
		
		// set priority TODO: REMOVE THIS
		for ( TrackSegment track : allTrackSegments )
		{
			track.priorityReading = track.nbFrameSinceLastRFIDReading % (30*5);			
		}
		
		//display priority reading on track
		
		while ( allTrackSegments.size() > 0 )
		{
			TrackSegment ts = getMaxPriorityTrackSegment( allTrackSegments );
			
			Antenna antenna = getAntennaAvailable( ts );
			if ( antenna != null )
			{
				antennaToActivate = antenna;
				break;
			}
			allTrackSegments.remove( ts );
		}
		
		if ( antennaToActivate == null || LiveMouseTracker.getTrackPoolOverlay().RFIDStopEventDisplayCounter > 0 )
		{
			disableAllAntennas();
			return;
		}

		activateOnlyAntenna( antennaToActivate );
		

	}

	/**
	 * This method will activate antenna with specific strategy 
	 * Strategy: closest antenna. anonymous first
	 * */
	/*
	public void activateAntennas() {

		// find antenna to activate.
		Antenna antennaToActivate = null;

		// get anonymous tracks.
		ArrayList<TrackSegment> anonymousTrackList = LiveMouseTracker.trackContainer.anonymousTrackSegmentPool.
				getTrackSegmentsContaining( LiveMouseTracker.getT() - 1 );

		double bestDistance = Double.MAX_VALUE;

		//
		// CHECK ANONYMOUS TRACK
		//

		// get the closest location of an anonymous animal to an antenna.
		for ( TrackSegment track : anonymousTrackList )
		{
			MouseDetection detection = track.getDetection( LiveMouseTracker.getT() - 1 );
			if ( detection.isBuiltByDetectionSplitter() ) continue;
			Point3D p = detection.getMassCenter();

			for ( Antenna antenna : getAntennaList() )
			{
				if ( antenna.isFaulty() ) continue;

				double distance = p.toPoint2D().distance( antenna.getLocation() );

					if ( distance < bestDistance )
					{
						bestDistance = distance;
						antennaToActivate = antenna;
					}
			}
		}

		// if no anonymous track is found,
		// find an animal to check against its identity.


		// CHECK ANIMALS
		if ( antennaToActivate == null )
		{
			// get tracks of animals.
			ArrayList<TrackSegment> animalTrackList = LiveMouseTracker.trackContainer.animalTrackSegmentPool.
					getTrackSegmentsContaining( LiveMouseTracker.getT() - 1 );

			for ( TrackSegment track : animalTrackList )
			{
				MouseDetection detection = track.getDetection( LiveMouseTracker.getT() - 1 );
				if ( detection.isBuiltByDetectionSplitter() ) continue;
				Point3D p = detection.getMassCenter();


				for ( Antenna antenna : getAntennaList() )
				{
					if ( antenna.isFaulty() ) continue;

					double distance = p.toPoint2D().distance( antenna.getLocation() );

						if ( distance < bestDistance )
						{
							bestDistance = distance;
							antennaToActivate = antenna;
						}
				}
			}
		}

		if( ! LiveMouseTracker.MODE_TEST_ANTENNA ) // in test mode, the painter is selecting the one that is activated by clicking with the mouse cursor
		{

			if ( antennaToActivate == null )
			{
				disableAllAntennas();
				return;
			}

			activateOnlyAntenna( antennaToActivate );
		}


	}
	*/


	private TrackSegment getMaxPriorityTrackSegment(ArrayList<TrackSegment> allTrackSegments) {

		int max = -1;
		TrackSegment mostPriorityTrackSegment = null;
		for ( TrackSegment ts : allTrackSegments )
		{
			if ( ts.priorityReading > max )
			{
				mostPriorityTrackSegment = ts;
				max = ts.priorityReading;
			}
		}
		return mostPriorityTrackSegment;
	}

	/** enable an antenna and disable all others. */
	public void activateOnlyAntenna( Antenna antennaToActivate) {

		synchronized( antennaList )
		{
			for ( Antenna antenna : antennaList )
			{
				antenna.setEnabled( antenna == antennaToActivate );
			}
		}

	}

	private void disableAllAntennas() {
		synchronized( antennaList )
		{
			for ( Antenna antenna : antennaList )
			{
				antenna.setEnabled( false );
			}
		}
	}

	public void switchOffAllAntennas()
	{
		synchronized( antennaList )
		{
			for ( Antenna antenna : antennaList )
			{
				antenna.switchOff();
			}
		}
	}

	/** returns true if all antenna are faulty. */
	public boolean areAllAntennaeFaulty() {

//		System.out.println("areAllAntennaeFaulty()");
		synchronized( antennaList )
		{
			for ( Antenna antenna : getAntennaList() )
			{
				//			System.out.println( antenna.isFaulty() );

				if ( !antenna.isFaulty() ) return false;
			}
		}

		return true;
	}

	public Antenna getClosestAntenna( Point2D point ) {

		Antenna closestAntenna = null;
		double bestDistance = java.lang.Double.MAX_VALUE;
		synchronized( antennaList )
		{
			for ( Antenna antenna : LiveMouseTracker.rfidManager.getAntennaList() )
			{
				double distance= antenna.getLocation().distance( point );
				if ( distance < bestDistance )
				{
					closestAntenna = antenna;
					bestDistance = distance;
				}
			}
		}

		return closestAntenna;
	}

	public boolean areAllAntennaUsingAnInnerSerialNumber()
	{		
		for ( Antenna a : getAntennaList() )
		{
			RFIDAntenna ra = (RFIDAntenna) a;
			
			if ( ra.getInnerSerialNumber() == "1234" ) // can't say, antenna is not programmed
			{
				return false;
			}
		}
		return true;
	}
	
	/*
	public boolean areAllAntennaCorrectlyAffected()
	{
		// TODO: manage antenne deco
		boolean allCorrect = true;
		for ( Antenna a : getAntennaList() )
		{
			RFIDAntenna ra = (RFIDAntenna) a;
			
			if ( ra.getInnerSerialNumber() == "1234" ) // can't say, antenna are not programmed
			{
				continue;
			}
			
			
		}
		return allCorrect;
	}
	*/
	
	public void solveAntennaComPortAffectation() {

		 
		System.out.println("-------- Start com tester:");
		System.out.println("-------- port name list:");
		
		/*
		SerialPort[] list = SerialPort.getCommPorts();
		*/
		ArrayList<COMTester> comTestList = new ArrayList();
		
		ArrayList<String> comList = new ArrayList();
		for ( int i=3 ; i< 100; i++ )
		{
			comList.add( "COM"+i );
		}

		ArrayList<String> portNotUsedList = new ArrayList<String>();
		/*
		portNotUsedList.add( "COM1" );
		portNotUsedList.add( "COM2" );
		portNotUsedList.add( "COM19" );
		portNotUsedList.add( "COM29" );
		*/
		
		//for ( SerialPort port : list )
		for ( String port : comList )
		{			
			boolean usePort = true;
			for ( String s : portNotUsedList )
			{
				//if ( s.equals( port.getSystemPortName() ) )
				if ( s.equals( port ) )
				{
					System.out.println("solveAntennaComPortAffectation: port ignored: " + port );
					usePort = false;
				}
			}			
			
			if ( usePort == false )
			{
				continue;
			}
			//RFIDAntenna a = new RFIDAntenna( null, 0, port, null  );
			System.out.println("Starting COM tester on port " + port );
			COMTester c = new COMTester( port );
			/*
			try { // test to see if it unlocks the serial init
				Thread.sleep( 50 );
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			*/
			comTestList.add( c );

		}
				
		
		// time for the comtester to retreive serial number
		try {
			Thread.sleep( 500 );
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		for ( COMTester comTester : comTestList )			
		{
			System.out.println("waiting for comTester on port " + comTester.comPort );
			try {
				//comTester.join( 3000 );
				comTester.join( 100 );
			} catch (InterruptedException e) {
				System.out.println("No response from some comTester in 3000ms");
				e.printStackTrace();
			}
		}

		for ( COMTester comTester : comTestList )			
		{
			System.out.println( "COMTester: " + comTester.comPort + "\tinnerSerial: " + comTester.innerSerialNumber  + "\tis and RFID reader: " + comTester.isRFIDReader() );
		}
		
		System.out.println("-------- End com tester.");
		
		// read COM port and expected location2D
		
		for ( RFIDPairing rfidPairing : rfidPairingList )
		{
			rfidPairing.submitCandidates( comTestList );
		}
		
		for ( RFIDPairing rfidPairing : rfidPairingList )
		{
			this.addAntenna( rfidPairing.createAntenna() );
		}

		System.out.println("---------- Solve port affection: done.");
		
		
		
		
		
		
		
		
		/*
		
		if ( areAllAntennaUsingAnInnerSerialNumber() == false )
		{
			System.out.println("SolverAntennaComPortAffectation: can't solve problem: all antenna are not programmed.");
			return;
		}
		System.out.println("Starting SolverAntennaComPortAffectation...");
		
		HashMap<String, Point2D> h = new HashMap<String, Point2D>();
		
		for ( Antenna a : getAntennaList() )
		{
			RFIDAntenna ra = (RFIDAntenna) a;
			
			h.put( ra.getInnerSerialNumber(), ra.location );
			System.out.println( ra.getInnerSerialNumber() + " -> " + ra.location );
			
		}
		
		
		System.out.println("-------- port name list:");
		
		SerialPort[] list = SerialPort.getCommPorts();
		for ( SerialPort port : list )
		{
			System.out.println( port.getSystemPortName() );
		}
		
		
		// affecting
		System.out.println("----- affecting antenna");
		
		for ( Antenna a : getAntennaList() )
		{
			RFIDAntenna ra = (RFIDAntenna) a;
			int comNumber = Integer.parseInt( ra.getInnerSerialNumber() );
			String comName = "COM"+comNumber;
			
			System.out.println( ra );
			
			Point2D expectedLocation = h.get( ra.getInnerSerialNumber() );
			//System.out.println( ra.comPort +" " + comName +" "+ ra.comPort.equals( comName ) );
			System.out.println( "expected location: " + ra.getInnerSerialNumber() + " : "+ expectedLocation );
			
			
			if ( ra.comPort.equals( comName ) )
			{
				System.out.println( ra.comPort + " : antenna location ok: " + ra.location + " innerSerial: "  + ra.getInnerSerialNumber() );
			}else
			{
				System.out.println("Affecting antenna location " + ra.location + " to " + expectedLocation + " (com port " + ra.comPort + " should be " + ra.getInnerSerialNumber() + ") (CORRECTED)");
				ra.location = expectedLocation;
			}
			
			
			
		}
		
		*/
		
		/**
		if ( areAllAntennaCorrectlyAffected() )
		{
			System.out.println("All antenna are correctly affected.");
			return;
		}*/
		
		
		
		
	}

}





















