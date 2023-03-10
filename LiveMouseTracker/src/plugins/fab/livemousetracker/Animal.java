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
import java.util.ArrayList;
import icy.main.Icy;
import icy.sequence.Sequence;
import plugins.fab.livemousetracker.MPEGRecorder.MPEGMedaillonRecorder;
import plugins.fab.livemousetracker.detection.MouseDetection;
import plugins.fab.livemousetracker.track.TrackSegment;
import weka.classifiers.Classifier;
import weka.core.Instances;

public class Animal {

	String name;
	String rfidID = "RFID";

	/** Track segment owned by the animal (sure) */
	private ArrayList<TrackSegment> trackSegmentList = new ArrayList<TrackSegment>();
	public Color color;
	boolean identityConfirmedByRFID = false;
	public Sequence medaillonSequence = null;
	public boolean enabled = true; // this animal is enable, meaning that the tracking is allowed to seek for its presence. If not the animal should not be searched

	public MPEGMedaillonRecorder mpegMedaillonRecorder;

	/**
	 * This instances is used to build the machine learning predictor.
	 * To avoid always rebuilding the Instances set, the Animal object keeps a cache of the last
	 * machine learning set learned, that should be updated reularly by another thread managed by
	 * the LiveMouseTracker
	 * */
	Instances machineLearningInstancesSet = null;

	/**
	 * This instances is used to build the machine learning predictor.
	 * used for sub part dictionary.
	 * */
	Classifier machineLearningSubPartsClassifier = null;
	Instances machineLearningSubPartsDataSet = null;

	public Classifier getMachineLearningSubPartsClassifier() {
		return machineLearningSubPartsClassifier;
	}

	public Instances getMachineLearningSubPartDataSet()
	{
		return machineLearningSubPartsDataSet;
	}

	public boolean isHeadMLReady()
	{
		int nb = 0;
		try{
			nb = getMachineLearningSubPartDataSet().numInstances();
		}catch( NullPointerException  e )
		{
			return false;
		}
		if ( nb > LiveMouseTracker.MIN_TO_START_USING_MACHINE_LEARNING_NB_DETECTION_FOR_LEARNING_SUBPARTS_PER_ANIMAL )
		{
			return true;
		}
		return false;
	}

	public void setMachineLearningSubPartsClassifier(Classifier machineLearningSubPartsClassifier, Instances instances) {
		this.machineLearningSubPartsClassifier = machineLearningSubPartsClassifier;
		this.machineLearningSubPartsDataSet = instances;
	}

	/** @deprecated no working now. Should check changes in track */
//	boolean machineLearningInstancesSetUpToDate = false;

	@Override
	public String toString() {
		return getName() ;
	}

	public Animal( String name ) {
		this.name = name;
		this.rfidID = "RFID_" + name;
		this.color = Color.black;
//		this.medaillonSequence = new Sequence( name );
//		Icy.getMainInterface().addSequence( medaillonSequence );
	}

	public void createMedaillonSequence()
	{
		this.medaillonSequence = new Sequence( name );
		this.medaillonSequence.addOverlay( new MedallonOverlay() );
		Icy.getMainInterface().addSequence( medaillonSequence );
		mpegMedaillonRecorder = new MPEGMedaillonRecorder( this );
	}

	public Animal( String name, Color color ) {
		this( name );
		this.color = color;
	}

	public boolean isIdentityConfirmedByRFID() {
		return identityConfirmedByRFID;
	}

	public void setIdentityConfirmedByRFID(boolean identityConfirmedByRFID) {
		this.identityConfirmedByRFID = identityConfirmedByRFID;
	}

	public void addTrackSegment( TrackSegment ts )
	{
		addTrackSegment( ts, true );
	}

	public boolean isTrackOverlapping( TrackSegment ts )
	{
		for ( TrackSegment ts1: getTrackSegments() )
		{
			if ( ts.overlapInT( ts1 ) ) return true;
		}
		return false;
	}

	public void addTrackSegment( TrackSegment ts , boolean securityCheck )
	{
		// Slow security that should be removed in production.
		if ( securityCheck )
		{
			for ( TrackSegment ts1: getTrackSegments() )
			{
				if ( ts.overlapInT( ts1 ) )
				{
					System.err.println("------------------------------------------");
					System.err.println("ADD TRACK SEGMENT TO ANIMAL ERROR: OVERLAP");
					System.err.println("Animal: " + getName() );
					System.err.println("Track to add: " + ts );
					System.err.println("Track to add last det: " + ts.getDetection( ts.getLastTimePoint() ) );
					System.err.println("Track conflicting: " + ts1 );
					System.err.println("Track conflicting last det: " + ts1.getDetection( ts1.getLastTimePoint() ) );
					System.err.println("------------------------------------------");
					// Print stackTrace
					Thread.dumpStack();
					// Pause to watch error.
//					LiveMouseTracker.getKinectStreamer().stepPlay();
					//return;
				}
			}
		}

		synchronized ( trackSegmentList ) {
			trackSegmentList.add( ts );
		}

	}

	public ArrayList<TrackSegment> getTrackSegments() {
		return new ArrayList<TrackSegment>( trackSegmentList );
	}

	public String getName() {
		return name;
	}

	public Color getColor() {
		return color;
	}

	/** Removes all tracks of the animal.
	 * */
	public void clearTracks() {
		synchronized ( trackSegmentList ) {
			trackSegmentList.clear();
		}
	}

	public void removeTrackSegment ( TrackSegment ts )
	{
		synchronized ( trackSegmentList ) {
			trackSegmentList.remove( ts );
		}
	}

	public void removeAllTrackSegment ( ArrayList<TrackSegment> tsList )
	{
		synchronized ( trackSegmentList ) {
			trackSegmentList.removeAll( tsList );
		}
	}

	/** get the last known time point of the animal */
	public int getLastTimePoint()
	{
		int biggestT = 0;
		synchronized ( trackSegmentList ) {
			for ( TrackSegment ts : trackSegmentList )
			{
				int t = ts.getLastTimePoint();
				if ( t > biggestT ) biggestT = t;
			}
		}
		return biggestT;
	}

	public void setMachineLearningInstancesSet(Instances machineLearningSet) {
			this.machineLearningInstancesSet = machineLearningSet;
	}

	public Instances getMachineLearningInstancesSet() {
		return machineLearningInstancesSet;
	}

//	/** @deprecated not working yet */
//	public boolean isCacheOfInstancesUpToDate() {
//		return machineLearningInstancesSetUpToDate;
//	}

	/** Get mean volume over ALL detection. Heavy. Should not be called. */
	public double getMeanVolume() {

		double sumVolume = 0;
		double nbDetection = 0;
		synchronized ( trackSegmentList ) {
			for ( TrackSegment ts : trackSegmentList )
			{
				for ( MouseDetection d : ts.getDetectionList() )
				{
					sumVolume+= d.getVolume();
					nbDetection++;
				}
			}
		}
		return sumVolume / nbDetection;
	}

	/** MEAN surface over ALL detection. Should not be called.*/
	public double getMeanSurface() {

		double sumSurface = 0;
		double nbDetection = 0;
		synchronized ( trackSegmentList ) {
			for ( TrackSegment ts : trackSegmentList )
			{
				for ( MouseDetection d : ts.getDetectionList() )
				{
					sumSurface+= d.getSurface();
					nbDetection++;
				}
			}
		}
		return sumSurface / nbDetection;

	}

	public int getNumberOfLearningDetection() {

		int nbDetection = 0;
		synchronized ( trackSegmentList ) {
			for ( TrackSegment ts : trackSegmentList )
			{
				for ( MouseDetection d : ts.getDetectionList() )
				{
					if ( d.canBeUsedForLearning() ) nbDetection++;
				}
			}
		}
		return nbDetection;
	}

	public MouseDetection getDetectionAt(int t) {
		synchronized ( trackSegmentList ) {
			for( TrackSegment ts : trackSegmentList )
			{
				if ( ts.containDetectionAt( t ) )
				{
					return ts.getDetection( t );
				}
			}
		}
		return null;
	}

	public String getRfidID() {
		return rfidID;
	}

	public void setRfidID(String rfidID) {
		this.rfidID = rfidID;
	}

	public TrackSegment getTrackContainingDetection( MouseDetection detection ) {

		for ( TrackSegment ts : getTrackSegments() )
		{
			if ( ts.containDetection( detection ) )
			{
				return ts;
			}
		}
		return null;
	}

	public boolean contain(TrackSegment track) {
		return getTrackSegments().contains( track );
	}

	/** Retreive the track at t. Should only have one !*/
	public TrackSegment getTrackContaining ( int t ) {

		for( TrackSegment ts : getTrackSegments() )
		{
			if ( ts.containDetectionAt( t ) )
			{
				return ts;
			}
		}
		return null;
	}

	public ArrayList<TrackSegment> getTracks(int firstTimePoint, int lastTimePoint) {

		ArrayList<TrackSegment> result = new ArrayList<TrackSegment>();
		try
		{
			for( TrackSegment ts : getTrackSegments() )
			{
				if ( ts.getDetectionList().size() != 0 )
					if ( ts.overlapInT( firstTimePoint, lastTimePoint ) )
					{
						result.add( ts );
					}
			}
		}catch( NullPointerException e )
		{
			System.out.println("Null pointer exception in getTracks.");
			e.printStackTrace();
		}
		return result;
	}

	public boolean hasRfidID() {
		if ( rfidID.contains("RFID") ) return false;
		return true;
	}

	public void addAll(ArrayList<TrackSegment> trackList ) {

		for ( TrackSegment trackSegment : trackList )
		{
//			System.out.println("Adding track : " + trackSegment );
			addTrackSegment( trackSegment );
		}

	}

	/** The id retrieve or assigned from the database for correction or updates. */
	long dataBaseId = -1;

	public void setDataBaseId( long dataBaseId) {

		this.dataBaseId = dataBaseId;

	}

	public long getDataBaseId() {
		return dataBaseId;
	}

	public double truePositiveRate = 0;
	
	public int nbFrameSinceLastRFIDReading = 0;
	public int priorityRFIDReading = 0;

	public void setTruePositiveRate(double value) {
			this.truePositiveRate = value;
	}

	public void setEnabled(boolean b) {
		this.enabled = b;		
		System.out.println( this + " enabled = " + b );
	}


}
