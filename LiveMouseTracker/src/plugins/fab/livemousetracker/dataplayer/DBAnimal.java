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
package plugins.fab.livemousetracker.dataplayer;

import java.util.ArrayList;
import java.util.HashMap;

import plugins.fab.livemousetracker.Animal;
import plugins.fab.livemousetracker.track.TrackSegment;

public class DBAnimal
{
	String RFID;
	protected int id;
	char key;
	int maxTValue;
	Animal animal;
	public String genotype;

	public DBAnimal( int id , String RFID , String genotype ) {
		this.id = id;
		this.RFID = RFID;
		this.key = (char)(id+96);
		this.genotype = genotype;
	}

	@Override
	public String toString() {

		return ""+ id + " : " + key + " - " + RFID;
	}

	public int getId() {
		return id;
	}

	public String getRFID() {
		return RFID;
	}

	HashMap<Integer, MouseDetectionX> detectionHashMap = new HashMap<Integer, MouseDetectionX>();

	public HashMap<Integer, MouseDetectionX> getDetectionHashMap() {
		return detectionHashMap;
	}

	public void addDetection(MouseDetectionX detection) {
		detectionHashMap.put( detection.mouseDetection.getT() , detection );
	}

	ArrayList<TrackSegment> trackList = new ArrayList<TrackSegment>();

	public void computeTracks() {

//		System.out.println("Compute tracks. " + toString() );
		int max = getMaxT();
		int min = getMinT();
		maxTValue = max;
//		System.out.println( "Max: " + max );

		//boolean continuity = false;
		TrackSegment currentTrack = null;

		for ( int t = min ; t <= max ; t ++ )
		{
			MouseDetectionX detection = detectionHashMap.get( t );
			if ( currentTrack == null )
			{
				if ( detection == null )
				{
					//continue;
				}else
				{
					currentTrack = new TrackSegment( );
					currentTrack.addDetection( detection.mouseDetection );
					//continue;
				}
			}else
			{
				// current track exists.
				if ( detection == null )
				{
					// close track
					trackList.add( currentTrack );
					currentTrack = null;
				}else
				{
					currentTrack.addDetection( detection.mouseDetection , false );
				}
			}
		}

		if ( currentTrack != null )
		{
			if ( currentTrack.getDetectionList().size() > 0 )
			{
				trackList.add( currentTrack );
			}
		}

		for ( TrackSegment track : trackList )
		{
			track.refresh();
//			System.out.println( track );
		}

	}

	private int getMaxT() {

		int max = 0;
		for ( Integer key : detectionHashMap.keySet() )
		{
			if ( key > max ) max = key;
		}

		return max;
	}

	private int getMinT() {

		int min = Integer.MAX_VALUE;
		for ( Integer key : detectionHashMap.keySet() )
		{
			if ( key < min ) min = key;
		}

		return min;
	}

}