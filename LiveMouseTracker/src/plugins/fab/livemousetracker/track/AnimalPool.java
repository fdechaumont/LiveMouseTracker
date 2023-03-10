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
import java.awt.geom.Point2D;
import java.util.ArrayList;

import flanagan.math.PsRandom;
import plugins.fab.livemousetracker.Animal;
import plugins.fab.livemousetracker.LiveMouseTracker;
import plugins.fab.livemousetracker.detection.MouseDetection;
import plugins.kernel.roi.roi2d.ROI2DRectangle;

public class AnimalPool extends AbstractTrackPool {

	public ArrayList<Animal> animalList = new ArrayList<Animal>();
	
	

	public void addAnimal( Animal animal )
	{
		synchronized( animalList )
		{
	//		System.out.println("AnimalPool : Add Animal " + animal );
			animalList.add( animal );
	//		int index = animalList.indexOf( animal );
			float h =0;
			for ( Animal a : animalList ){
				//a.color = Color.getHSBColor( animalList.indexOf( a )/(float)animalList.size(), 0.9f, 0.9f );
				a.color = Color.getHSBColor( h, 0.9f, 0.9f );
				h+= 2.4 * 1 / (Math.PI * 2f );
			}
	//		switch( index )
	//		{
	//			case 0: animal.color= Color.red; break;
	//			case 1: animal.color= Color.green; break;
	//			case 2: animal.color= Color.blue; break;
	//			case 3: animal.color= Color.cyan; break;
	//		}
			}
	}

	public ArrayList<Animal> getAnimalList() {
		synchronized( animalList )
		{
			return new ArrayList<Animal>( animalList );
		}
	}



	public void addTrackSegment( Animal animal , TrackSegment trackSegment) {


		//int indexIdentity = trackSegment.getIdentity();
		animal.addTrackSegment( trackSegment );
		//System.out.println("should NOT be with index but animal object instead");

	}


	public ArrayList<TrackSegment> getTrackSegments() {

		ArrayList<TrackSegment> allTracks = new ArrayList<TrackSegment>();

		for ( Animal animal : animalList )
		{
			allTracks.addAll( animal.getTrackSegments() );
		}

		return allTracks;

	}

	/** Clear all track for all animals */
	public void clear() {

		for ( Animal animal : animalList )
		{
			animal.clearTracks();
		}

	}

	/** provide the animal using the track given as argument. Assumes no share of track.*/
	public Animal getAnimalOwningTrack ( TrackSegment ts )
	{
		for ( Animal animal : animalList )
		{
			if ( animal.getTrackSegments().contains( ts ) ) return animal;
		}
		return null;
	}

	/**
	 * Try to prolongate with a detection
	 *
	 */
	public TrackSegment addDetection(MouseDetection rawMouseDetection) {


		TrackSegment ts = TrackExtender.addDetection( rawMouseDetection, getTrackSegments() );
		//mouseDetection.getROI2DArea().setColor( LiveMouseTracker.getMainAnimalPool().getan );
		if ( ts != null ) // color the detection
		{
			rawMouseDetection.getROI2DArea().setColor( getAnimalOwningTrack( ts ).getColor() );
		}else
		{
//			System.out.println("Track animal prolongator fail: " + rawMouseDetection );
//			ROI2DRectangle roi = new ROI2DRectangle( rawMouseDetection.getROI2DArea().getBounds2D() );
//			roi.setColor( Color.cyan );
//			roi.setName("tmp animal prolongator fail");
//			LiveMouseTracker.addROIToInfraSequence( roi );
		}

		return ts;
	}


	/**
	 * Gets the closest track to the point given. null if nothing is found
	 * @param point
	 * @return
	 */
	public TrackSegment getClosestTrack( Point2D point )
	{
		ArrayList<TrackSegment> trackSegmentArrayList = getTrackSegments();

		double minDistance = java.lang.Double.MAX_VALUE;
		TrackSegment bestTrack = null;
		for ( TrackSegment ts : trackSegmentArrayList )
		{
			for ( MouseDetection detection : ts.getDetectionList() )
			{
				double distance = detection.getMassCenter().toPoint2D().distance( point );
				if ( distance < minDistance )
				{
					minDistance = distance;
					bestTrack = ts;
				}
			}
		}
		return bestTrack;
		//return getClosestTrack( point );
	}

//	/** ageInFrame is the maximum age of the retrieved track */
//	public TrackSegment getClosestTrack(Point2D point, int ageInFrame ) {
//
//	}

	public ArrayList<TrackSegment> getAllTrackSegmentFinishingAt(int t) {

		ArrayList<TrackSegment> trackSegmentList = new ArrayList<TrackSegment>();
		for ( Animal animal : animalList )
		{
			for ( TrackSegment ts : animal.getTrackSegments() )
			{
				if ( ts.getLastTimePoint() == t )
				{
					trackSegmentList.add( ts );
				}
			}
		}
		return trackSegmentList;
	}

	public Animal getAnimalWithDetection(MouseDetection detection) {

		for ( Animal animal : animalList )
		{
			for ( TrackSegment ts : animal.getTrackSegments() )
			{
				if ( ts.containDetection ( detection ) )
				{
					return animal;
				}
			}
		}
		return null;

	}

	public void correctTrackWithAnimal(Animal animalWithDetection,
			TrackSegment track ) {

		if ( animalWithDetection.contain( track ) )
		{
			System.out.println( "RFID: Track had aldready the good id." );
		}else
		{
			System.out.println( "TODO: Correct track with animals." );
		}



	}

	/**
	 * Set RFID. return false if a problem occurs.
	 * @param animalWithDetection
	 * @param id
	 * @return
	 */
	public boolean setRFID(Animal animalWithDetection, String id) {

		if ( animalWithDetection.hasRfidID() ) // Animal has already an id.
		{
			System.err.println("[RFID + t:"+LiveMouseTracker.getT()+" ] Affectation Error: Animal has already an RFID. Existing: " + animalWithDetection.getRfidID() + " asked : " + id );
			return false;
		}

		// check if ID is already given.
		for ( Animal animal : animalList )
		{
			if ( animal == animalWithDetection ) continue;

			String animalId = animal.getRfidID();
			if ( animalId != null )
			{
				if ( animalId.equals( id ) )
				{
					System.out.println("[RFID + t: "+LiveMouseTracker.getT()+ " ] Cannot assign RFID. Already used by another animal.");
					// id already affected to another animal.
					return false;
				}
			}
		}

		System.out.println("[RFID + t: "+LiveMouseTracker.getT()+ " ] Setting id " + id + " to animal " + animalWithDetection );
		animalWithDetection.setRfidID( id );

		return true;
	}

	public Animal getAnimalWithRFID(String id) {

		for ( Animal animal : animalList )
		{
			String animalId = animal.getRfidID();
			if ( animalId != null )
			{
				//System.out.println("------------ MATCH TEST: getAnimalWithRFID: " + animalId + " " + id + " : " + animalId.equals( id ) );
				if ( animalId.equals( id ) )
				{
					return animal;
				}
			}
		}
		return null;

	}

	/** return an animal with NO RFID and no track. */
	public Animal getFreeAnimal() {

		for ( Animal animal : animalList )
		{
			if ( animal.getRfidID() == null && animal.getTrackSegments().isEmpty() )
			{
				return animal;
			}
		}

		return null;
	}

	/** Provides all detections for t */
	public ArrayList<MouseDetection> getAllDetectionAt(int t) {

		ArrayList<MouseDetection> detectionList = new ArrayList<MouseDetection>();

		for ( Animal animal : getAnimalList() )
		{
			for ( TrackSegment ts : animal.getTracks( t, t ) )
			{
				detectionList.add( ts.getDetection( t ) );
			}
		}
		return detectionList;
	}

	@Override
	public void removeTrack(TrackSegment ts) {

		for ( Animal animal : animalList )
		{
			animal.removeTrackSegment( ts );
		}

	}

	@Override
	public TrackSegment getTrackWithId(int dataBaseId) {

		for ( Animal animal : getAnimalList() )
		{
			for ( TrackSegment ts : animal.getTrackSegments() )
			{
				if ( ts.dataBaseId == dataBaseId ) return ts;
			}
		}
		return null;

	}

	PsRandom random = new PsRandom( );
	/** try to retreive a random detection that can be used for learning.
	 *  result can be null.
	 *  */
	public MouseDetection getRandomDetection() {

		if ( animalList.size() == 0 ) return null;

		// pick a random animal
		Animal animal =
		//		animalList.get( (int) Math.random() * (animalList.size()-1) );
				animalList.get( random.nextInteger( 0 , animalList.size()-1 ) );


		MouseDetection mouseDetection = animal.getDetectionAt( (int) ( LiveMouseTracker.getT() - Math.random() * 200 ) );

		if ( mouseDetection == null ) return null;

		if ( mouseDetection.canBeUsedForLearning() )
		{
			return mouseDetection;
		}
		return null;
	}

	public ArrayList<TrackSegment> getTrackSegmentsContaining(int t) {

		ArrayList<TrackSegment> resultList = new ArrayList<TrackSegment>();
		for ( Animal animal : getAnimalList() )
		{
			TrackSegment track = animal.getTrackContaining( t );
			if ( track != null ) resultList.add( track );
		}
		return resultList;

	}

	public Animal getFirstAnimalWithoutRFID() {

		for ( Animal animal : animalList )
		{
			if ( animal.getRfidID() == null ) return animal;
			if ( animal.getRfidID().contains("RFID") ) return animal;
		}
		return null;

	}

	public int getNumberOfAnimalToSearchFor() {
		int nb = 0;
		for ( Animal animal : this.animalList )
		{
			if ( animal.enabled )
			{
				nb++;
			}
		}
		return nb;
	}

	public void addDynamicAnimal( String rfid ) {
		
		System.out.println( "--------------------------- CREATE DYNA ANIMAL " );
		System.out.println("TODO TODAY CREATE DYNAMIC ANIMAL");
		System.out.println("Creating new animal");
		int i = this.getAnimalList().size();
		Animal animal = new Animal( "DA_"+ i);
		animal.setRfidID(rfid);					
		this.addAnimal( animal );
		
		for ( Animal animal2 : this.getAnimalList() )
		{
			System.out.println( animal2 + " / " + animal2.getRfidID() );
		}
		System.out.println( "--------------------------- END CREATE DYNA ANIMAL" );
		
	}
	
	public void setRFIDAnimalEnabled(String rfid, boolean in ) {

		if ( in )
		{
			boolean animalSet = false;
			for ( Animal animal : this.animalList )
			{
				if ( animal.getRfidID().contains( rfid ) )
				{
					animal.setEnabled( true );
					animalSet = true;
				}
			}
			
			if ( animalSet == false )
			{
				// this RFID is new. Should init an anonymous animal with it or create a new one.
				//System.out.println("Animal was not set " );
				boolean setOk = false;
				for ( Animal animal : this.animalList )
				{
					System.out.println( "---------------" );
					System.out.println( animal );
					System.out.println( animal.getRfidID() );
					if ( !animal.hasRfidID() ) // means it is not set yet
					{
						animal.setRfidID(rfid);
						setOk = true;
						break;
					}
				}
				
				if ( !setOk )
				{
					// Create dynamic animal.
					System.out.println("Animal added by animalPool using remote identity UDP call: " + rfid );
					this.addDynamicAnimal(rfid);
				}
				
			}
			
		}
		
		if ( !in )
		{			
			for ( Animal animal : this.animalList )
			{
				if ( animal.getRfidID().contains( rfid ) )
				{
					animal.setEnabled( false );
				}
			}
		}

	}

}
