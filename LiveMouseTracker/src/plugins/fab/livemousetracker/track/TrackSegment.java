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
package plugins.fab.livemousetracker.track;

import java.awt.Color;
import java.util.ArrayList;

import plugins.fab.livemousetracker.Animal;
import plugins.fab.livemousetracker.LiveMouseTracker;
import plugins.fab.livemousetracker.detection.MouseDetection;
import plugins.fab.livemousetracker.machinelearning.IdentityResult;

/** A track Segment is a contuous serie of position in a track, that is axiomatic
 *
 * Should not be broken by any other track
 * @author Fab
 *
 */
public class TrackSegment {

	Color color = Color.getHSBColor( (float)Math.random() , 0.6f, 0.7f );
	//int animalIndex = -1;
//	double identityProba ;
	ArrayList<MouseDetection> detectionList = new ArrayList<MouseDetection>();
	int firstTimePoint = Integer.MAX_VALUE;
	IdentityResult identityResult = null;
	int lastTimePoint = Integer.MIN_VALUE;
	boolean blink = false;

//	/** List of potential animal this track MUST be affect to.
//	 * @deprecated At the moment I don't know how to use this list.
//	 * */
//	ArrayList<Animal> mustBeAnimalList = new ArrayList<Animal>();

	/** The must be animal is considered in the ML framework while pruning tracks. */
	Animal mustBeAnimal = null;

	/** List of animals this track cannot be affect to. */
	ArrayList<Animal> cannotBeAnimalList = new ArrayList<Animal>();

	/** If the track contains contact events, it is more likely to have association problem.
	 * @deprecated not used yet.
	 * New scheme should create track --- that are without ambiguity and XXX with problem.
	 * should have ---XXX--- tracks.
	 * */
	boolean isContactTrack = false;

	public TrackSegment() {

	}

	public TrackSegment(MouseDetection rawMouseDetection) {
		addDetection(rawMouseDetection);
	}


	/** if a track exists at t, will create 2 tracks.
	 * left track will finish at t-1 and the right track at t.
	 *
	 * */
	public ArrayList<TrackSegment> split( int t ) {

		ArrayList<TrackSegment> tsSplitList = new ArrayList<TrackSegment>();
		if ( getDetectionList().isEmpty() )
		{
			return tsSplitList;
		}

		// PRODUCE LEFT SEGMENT
		TrackSegment leftSegment = null;
		for ( int i = getFirstTimePoint() ; i< t ; i++ )
		{
			MouseDetection detection = getDetection( i );
			if ( detection != null )
			{
				if ( leftSegment == null )
				{
					leftSegment = new TrackSegment( detection );
				}else
				{
					leftSegment.addDetection( detection );
				}
			}
		}
		if ( leftSegment != null )
		{
			System.out.println("Split produced left: " + leftSegment );
			System.out.println("left nb detection: " + leftSegment.getDetectionList().size() );
			if ( !leftSegment.getDetectionList().isEmpty() )
			{
				tsSplitList.add( leftSegment );
			}
		}else
		{
			System.out.println("Split produced left no trackSegment.");
		}
		// PRODUCE RIGHT SEGMENT (future)
		TrackSegment rightSegment = null; //new TrackSegment();
		for ( int i = t ; i<= getLastTimePoint() ; i++ )
		{
			MouseDetection detection = getDetection( i );
			if ( detection != null )
			{
				if ( rightSegment == null )
				{
					rightSegment = new TrackSegment( detection );
				}else
				{
					rightSegment.addDetection( detection );
				}
			}
		}

		if ( rightSegment!=null )
		{
			System.out.println("Split produced right: " + rightSegment );
			System.out.println("right nb detection: " + rightSegment.getDetectionList().size() );
			if ( !rightSegment.getDetectionList().isEmpty() )
			{
				tsSplitList.add( rightSegment );
			}
		}else
		{
			System.out.println("Split produced right no trackSegment.");
		}
		return tsSplitList;

	}

	/**
	 * Warning: no control on detection is performed. Should only be use by reloader or process
	 * needing this structure, but performing themselves the diverses check needed.
	 *
	 * Should not be used in realtime tracking process.
	 * */
	public void addDetection(MouseDetection mouseDetection , boolean controlDetectionValidity ) {
		if ( controlDetectionValidity )
		{
			addDetection(mouseDetection);
			return;
		}
		detectionList.add( mouseDetection );
	}

	public void addDetection(MouseDetection rawMouseDetection) {

		if ( rawMouseDetection == null )
		{
			System.out.println("track segment add detection: drop null detection.");
			Thread.dumpStack();
			return;
		}

//		if ( rawMouseDetection.getT() >= 350 )
//		{
//			System.out.println("track segment debug call : " + rawMouseDetection );
//			Thread.dumpStack();
//		}

//		MouseDetection lastdetection = getDetection( rawMouseDetection.getT()-1 );
//
//		if ( lastdetection != null )
//		{
//			if ( rawMouseDetection.getT() != lastdetection.getT() + 1 )
//			{
//				System.err.println("******** ");
//				System.err.println("******** ");
//				System.err.println("******** INCOHERENT TRACK SEGMENT T");
//				System.err.println("******** ");
//				System.err.println("******** ");
//				System.out.println("last: " + lastdetection );
//				System.out.println("candidate: " + rawMouseDetection );
//			}
//		}

		detectionList.add( rawMouseDetection );

		// the post process cannot be performed on the first detection (for instance speed computation)
		// so we don't compute it if the detection size is 0;
		// but if we already had a detection (size()==2), we compute the first,
		// and the second one we just receive in the row.
		if( detectionList.size() == 2 )
		{
			if ( LiveMouseTracker.ENABLE_DETECTION_POST_PROCESS )
			{
			detectionList.get( 0 ).postProcess( this );
			}
		}
		if( detectionList.size() > 1 )
		{
			rawMouseDetection.postProcess( this );
		}

		refresh();
	}

	public Color getColor() {

		return color;
	}

	public MouseDetection getDetection(int t) {

//		System.out.println("asking t =" + t );
//		System.out.println(this);
		// FIXME: possile error here in comodification so I copy the list. the new arraylist here Should very much be avoided

		for ( MouseDetection mouseDetection : new ArrayList<MouseDetection>(detectionList) )
		{
			//System.out.println("checking t= " + mouseDetection.getT() );
			if ( mouseDetection.getT() == t )
			{
				//System.out.println("Found !");
				return mouseDetection;
			}
		}
		return null;
	}

	public ArrayList<MouseDetection> getDetectionList() {
		return new ArrayList<MouseDetection>( detectionList );
	}

	public int getFirstTimePoint() {
		return firstTimePoint;
	}

//	public IdentityResult getIdentityResult() {
//		return identityResult;
//	}

//	public double getIdentityProbability() {
//		return identityProba;
//	}

//	public void setIdentity( double identityProba ) {
//
//		//this.animalIndex = animalIndex;
//		this.identityProba = identityProba;
//
//		//Fixme : color should be in animal structure;
//
//		/*
//		switch ( animalIndex ) {
//		case 0:
//			color = Color.RED;
//			break;
//		case 1:
//			color = Color.GREEN;
//			break;
//		case 2:
//			color = Color.YELLOW;
//			break;
//
//		default:
//			break;
//		}
//		*/
//
//	}

	/*
	public int getIdentity() {

		return animalIndex;

	}*/

	public double getInstantSpeed(MouseDetection mouseDetection) {

		int t = mouseDetection.getT();
		double speed = 0;

		MouseDetection detectionA = getDetection( t -1 );
		MouseDetection detectionB = mouseDetection ;
		MouseDetection detectionC = getDetection( t + 1 );

		if ( detectionA != null && detectionB != null && detectionC != null )
		{
			double distAB = detectionA.getMassCenter().toPoint2D().distance( detectionB.getMassCenter().toPoint2D() );
			double distBC = detectionB.getMassCenter().toPoint2D().distance( detectionC.getMassCenter().toPoint2D() );
			speed = (distAB + distBC ) /2d;
		}

		return speed;
	}

	public int getLastTimePoint() {

		return lastTimePoint;

	}

	/** Returns number of time point of the track.
	 * Note that it does not mean all the detection for all the time points are existing.
	 * */
	public int getLength() {

		return getLastTimePoint() - getFirstTimePoint() + 1;
	}

	public void refresh() {

		lastTimePoint = Integer.MIN_VALUE;
		firstTimePoint = Integer.MAX_VALUE;
//		System.out.println("***");
		for ( MouseDetection detection : detectionList )
		{
			int t = detection.getT();
//			System.out.println(t);
			if ( t > lastTimePoint )
			{
				lastTimePoint = t;
			}
			if ( t < firstTimePoint )
			{
				firstTimePoint = t;
			}
		}

	}

	public void removeDetection( MouseDetection mouseDetection ) {

		detectionList.remove( mouseDetection );
		refresh();
	}

	public void setIdentityResult(IdentityResult result) {

		this.identityResult = result;

	}

	/** returns true if the track is overlapping in time with the proposed argument track
	 * */
	public boolean overlapInT(TrackSegment ts) {

		if ( ts == null ) return false;

		return overlapInT( ts.getFirstTimePoint() , ts.getLastTimePoint() );

//		if (
//				ts.getFirstTimePoint() >= this.getFirstTimePoint()
//				&&
//				ts.getFirstTimePoint() <= this.getLastTimePoint() ) return true;
//
//		if (
//				ts.getLastTimePoint() >= this.getFirstTimePoint()
//				&&
//				ts.getLastTimePoint() <= this.getLastTimePoint() ) return true;
//
//		return false;
	}

	public boolean overlapInT( int firstTimePoint, int endTimePoint )
	{
		// algo stef:

//		if ( endTimePoint <= this.lastTimePoint && firstTimePoint >= this.firstTimePoint )
//		{
//			return true;
//		}

		// new algo stef 23 oct 2017
		if ( firstTimePoint <= this.lastTimePoint && endTimePoint >= this.firstTimePoint )
		{
			return true;
		}

//
//		if (
//				firstTimePoint >= this.getFirstTimePoint()
//				&&
//				firstTimePoint <= this.getLastTimePoint() ) return true;
//
//		if (
//				endTimePoint >= this.getFirstTimePoint()
//				&&
//				endTimePoint <= this.getLastTimePoint() ) return true;
//
//		if (
//				firstTimePoint <= this.getFirstTimePoint()
//				&&
//				endTimePoint >= this.getLastTimePoint() )
//			{
//			System.out.println("DEBUG: OVERLAP FOUND WITH NEW CONDITION");
//			return true;
//			}



		return false;
	}


	public void setBlink(boolean blink ) {

		this.blink = blink;

	}

	public boolean isBlinking() {
		return blink;
	}

	public void setColor(Color color ) {

		this.color = color;

	}

	@Override
	public String toString() {
		String cantBeList ="";
		for ( Animal a : getCannotBeAnimalList() )
		{
			cantBeList+= a.getName();
		}
		return "Track ["+getFirstTimePoint()+","+getLastTimePoint()+"] " + " mustbe:" + getMustBeAnimal() + " cantbe:"+cantBeList; //+ hashCode() ;

	}

	public boolean containDetectionAt(int t) {
		return ( t >= firstTimePoint && t <= lastTimePoint );
	}

	public boolean containDetection(MouseDetection detection) {

		return detectionList.contains( detection );

	}

	/** Set which animal should own this track. */
	public void mustBe(Animal animal ) {
		mustBeAnimal = animal;
		cannotBeAnimalList.remove( animal );
	}

	public Animal getMustBeAnimal()
	{
		return mustBeAnimal;
	}

	public void addCannotBeAnimal(Animal animal ) {
		if ( !cannotBeAnimalList.contains( animal ) )
		{
			cannotBeAnimalList.add( animal );
		}
	}

	public ArrayList<Animal> getCannotBeAnimalList() {
		
		// build a tmp list of animal in which the not-enabled animals are listed so that the machine learning can't associate them.
		ArrayList<Animal> tmpCannotBeAnimalList = new ArrayList<Animal>( this.cannotBeAnimalList );
		
		// adds the animals that are not enabled at the moment.
		for ( Animal animal : LiveMouseTracker.trackContainer.animalTrackSegmentPool.getAnimalList() )
		{
			if ( animal.enabled == false )
			{
				tmpCannotBeAnimalList.add( animal );
			}
		}
		
		return cannotBeAnimalList;
		
	}

	/**
	 * Does not process the post detection events.
	 * Designed when loading trackSegment.
	 * */
	public void addAll(ArrayList<MouseDetection> detectionList ) {

		this.detectionList.addAll( detectionList );
		System.out.println("--");
//		for ( RawMouseDetection d : detectionList )
//		{
//			System.out.println( d.getT() );
//		}
		refresh();
//		System.out.println( "minT: " + getFirstTimePoint() );
//		System.out.println( "maxT: " + getLastTimePoint() );
	}

	/** The id retreive from the database for correction. */
	int dataBaseId;

	public void setDataBaseId(int dataBaseId) {

		this.dataBaseId = dataBaseId;

	}

	public int getDataBaseId() {
		return dataBaseId;
	}

	String identityAffectedBy = "";
	String orientationAffectedBy = "";
	/** Very tricky flag. If a track is prolongated, but a split is performed at the end of the track, then the initial track
	 * keep satisfying the condition "ends at the previous frame" then further detection association could try to use that track to prolongate
	 * We use that flag to avoid this problem.
	 * Could also be an hashmap of track associated that could be reset at each frame. Then this would be in the track prolongator class.
	 * */
	public boolean canBeProlongated = true;
	public int nbFrameSinceLastRFIDReading=10000;
	public int priorityReading = 0 ;

	public void setIdentityAffectedBy(String identityAffectedBy ) {
		this.identityAffectedBy = identityAffectedBy;
	}

	public void setOrientationAffectedBy(String orientationAffectedBy ) {
		this.orientationAffectedBy = orientationAffectedBy;
	}

	public String getIdentityAffectedBy() {
		return identityAffectedBy;
	}

	public String getOrientationAffectedBy() {
		return orientationAffectedBy;
	}

	/** get mean intensity over the last frame specified. */
	public Double getMeanIntensity( int frame ) {

		double sumIntensity = 0;
		double nb = 0;

		ArrayList<MouseDetection> detectionListLocal = this.getDetectionList();
		int totalLen = detectionListLocal.size();

		for ( int i = totalLen-1 ; i>totalLen-1-frame ; i-- )
		{
			if ( i >= 0 )
			{
				sumIntensity += detectionListLocal.get( i ).getMeanInfraIntensity();
				nb++;
			}
		}

		if ( nb > 0 )
		{
			return sumIntensity / nb;
		}
		return null;
	}





}
