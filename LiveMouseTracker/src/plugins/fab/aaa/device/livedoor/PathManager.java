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
package plugins.fab.aaa.device.livedoor;

import icy.canvas.IcyCanvas;
import icy.image.IcyBufferedImage;
import icy.painter.Overlay;
import icy.sequence.Sequence;
import icy.type.point.Point5D.Double;
import icy.util.GraphicsUtil;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;



import plugins.fab.aaa.device.livedoor.Door.DoorStatus;
import plugins.fab.livemousetracker.LiveMouseTracker;
import plugins.fab.livemousetracker.detection.MouseDetection;
import plugins.fab.livemousetracker.listener.LiveTrackerListener;
import plugins.kernel.roi.roi2d.ROI2DArea;
import plugins.kernel.roi.roi2d.ROI2DRectangle;

/**
 * Manage a set of gate
 *
 * - Book path
 * - High level API interface with other devices
 *
 * @author Fab
 *
 */
public class PathManager implements LiveTrackerListener {

//	ArrayList<Gate> gateList = new ArrayList<Gate>();
	/**
	 * This zone list describes all the path the animal need to go through.
	 * */
	ArrayList<Zone> zoneListToCross = new ArrayList<Zone>();

	Zone zoneEast;
	Zone zoneWest;

	PathManagerState state = PathManagerState.INIT;
	Process processStep = null;
	MazeGraph maze = null;

	public PathManager(MazeGraph maze , Zone zoneEast, Zone zoneWest ) {

		this.zoneEast = zoneEast;
		this.zoneWest = zoneWest;
		processStep = Process.values()[0];
		this.maze = maze;
		LiveMouseTracker.addTrackerListener( this );
		LiveMouseTracker.addOverlayToInfraSequence( pathManagerOverlay );
	}

	public PathManagerState getState() {
		return state;
	}

	private PathManagerOverlay pathManagerOverlay = new PathManagerOverlay();

	public Overlay getOverlay()
	{
		return pathManagerOverlay;
	}

	class PathManagerOverlay extends Overlay
	{
		public PathManagerOverlay() {
			super("Path Manager Overlay");
		}

		@Override
		public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas) {
			if ( detectionList == null ) return;

			g.setColor( Color.pink );
			int y = 300;
			GraphicsUtil.drawString(g, "#Detection(s): "+detectionList.size() , 50, y, false );
			y+=10;
			GraphicsUtil.drawString(g, ""+PathManager.this.processStep , 50, y, false );
			y+=10;
			for ( Zone zone : zoneListToCross )
			{
				String str = "";
				if ( zone.containsDetection( detectionList ) )
				{
					str+="X";
				}else
				{
					str+=" ";
				}
				str+=" " + zone ;
				GraphicsUtil.drawString(g, str , 50, y, false );
				y+=10;
			}

			if ( zoneListToCross.get( 0 ) == zoneWest )
			{
				g.drawString( "----> to east", 300, 300 );
			}else
			{
				g.drawString( "<---- to west", 300, 300 );
			}
			if ( LiveMouseTracker.LOCK_BACKGROUND )
			{
				g.setColor( Color.red );
				g.drawString("Background locked", 300, 320 );
			}else
			{
				g.setColor( Color.green );
				g.drawString("Background unlocked", 300, 320 );
			}

			if ( PathManager.this.simulatedDetection != null )
			{
				g.drawString( "M", (int)PathManager.this.simulatedDetection.getX(), (int)PathManager.this.simulatedDetection.getY() );
			}

		}

		@Override
		public void keyPressed(KeyEvent e, Double imagePoint, IcyCanvas canvas) {
			if ( e.getKeyChar() == 'w' )
			{
				System.out.println("GOING TO WEST !");
				bookPath( zoneEast, zoneWest );
				start();
			}
			if ( e.getKeyChar() == 'e' )
			{
				System.out.println("GOING TO EAST !");
				bookPath( zoneWest, zoneEast );
				start();
			}
			if ( e.getKeyChar() == 'L' )
			{
				LiveMouseTracker.LOCK_BACKGROUND =! LiveMouseTracker.LOCK_BACKGROUND;
				start();
			}
		}

		@Override
		public void mouseMove(MouseEvent e, Double imagePoint, IcyCanvas canvas) {

			if ( e.isControlDown() )
			{
				PathManager.this.simulatedDetection = imagePoint.toPoint2D() ;
			}else
			{
				PathManager.this.simulatedDetection = null;
			}

		}

	}

	Point2D simulatedDetection = null;

	enum PathManagerState {
		INIT,
		RUNNING,
		FINISHED,
		ERROR_PATH_ALREADY_USED
	}

	enum Process {
		OPEN_DOORS,
		CHECKING_IF_NO_ANIMAL_PRESENTS_1,
		CLOSE_DOORS,
		WAIT_FOR_DOORS_CLOSED,
		CHECKING_IF_NO_ANIMAL_PRESENTS_2,
		ANIMAL_TRAVEL,
		FINISHED
	}

	ArrayList<MouseDetection> detectionList = null;

	@Override
	public void liveTrackerEndOfFrame( LiveMouseTracker liveMouseTracker ) {

		//System.out.println( processStep );

		detectionList = liveMouseTracker.getLastDetection();

		if ( PathManager.this.simulatedDetection != null )
		{
			Point2D p = PathManager.this.simulatedDetection;
			detectionList.add( new MouseDetection(
					new ROI2DArea( p ), 0 ) );
		}

		for ( int i = detectionList.size()-1 ; i>= 0 ; i-- ) // Naive filter for bad detection as we don't have machine learning knowledge :(
		{
			MouseDetection detection = detectionList.get( i );
			if ( detection.getSurface() < 100 )
			{
				detectionList.remove( detection );
			}
		}

		if ( state == PathManagerState.RUNNING )
		{
			switch ( processStep ) {

			case OPEN_DOORS:

				openAllDoors();
				processStep = Process.CHECKING_IF_NO_ANIMAL_PRESENTS_1;
				break;

			case CHECKING_IF_NO_ANIMAL_PRESENTS_1:

				if ( !isPathFreeOfAnimal( ) ) return;
				processStep = Process.CLOSE_DOORS;
				break;

			case CLOSE_DOORS:
				// Close and lock doors

				closeAllDoors( );

				processStep = Process.WAIT_FOR_DOORS_CLOSED;

				break;

			case WAIT_FOR_DOORS_CLOSED:

				// FIXME: check and create the DOOR_LOCKED_STATUS
				if ( !allDoorClosed() ) return;

				processStep = Process.CHECKING_IF_NO_ANIMAL_PRESENTS_2;

				break;

			case CHECKING_IF_NO_ANIMAL_PRESENTS_2:

				if ( !isPathFreeOfAnimal( ) ) processStep = Process.OPEN_DOORS;
				processStep = Process.ANIMAL_TRAVEL;
				break;

			case ANIMAL_TRAVEL:

				animalTravel();

				break;

			default:
				break;
			}

		}

	}

	private void animalTravel() {

		//int zoneIndexWhereTheAnimalIs = 0;

		//if ( detectionList.size() > 1 ) return;

		Zone zoneWithAnimal = null;

		for ( int i=0; i< zoneListToCross.size() ; i++  )
		{
			//Door door = maze.getDoor( zoneListToCross.get( i ) , zoneListToCross.get( i+1 ) );
			//door.close();
			//System.out.println("[PathManager close door] " + door );
			Zone zone = zoneListToCross.get( i );
			if ( zone.containsDetection( detectionList ) )
			{
				zoneWithAnimal = zone;
				//if ( zone//)oneIndexWhereTheAnimalIs = i;
				break;

			}
		}

		//System.out.println("Zone with animal: " + zoneWithAnimal );

		if ( zoneWithAnimal == null ) // If no animal is detected, prepare to receive it as if it where at the first location.
		{
			zoneWithAnimal = zoneListToCross.get( 0 );
		}

		Zone nextZone = null;
		Zone previousZone = null;
		try {
			nextZone = zoneListToCross.get( zoneListToCross.indexOf( zoneWithAnimal ) +1 );
		}catch( IndexOutOfBoundsException e ) {
			// we are at one end of the path
		}
		try{
			previousZone = zoneListToCross.get( zoneListToCross.indexOf( zoneWithAnimal ) -1 );
		}catch( IndexOutOfBoundsException e ){
			// we are at one end of the path
		}

		Door doorNext = maze.getDoor( zoneWithAnimal , nextZone );
		Door doorPrevious = maze.getDoor( zoneWithAnimal , previousZone );

		if ( doorNext != null )
		{
			doorNext.open();
		}
		if ( doorPrevious != null )
		{
			doorPrevious.open();
		}




//		if ( doorPrevious != null )
//		{
//			doorPrevious.open();
//		}

		closeAllDoors( doorNext , doorPrevious );

	}

	/** Door excluded will not be touched
	 * @param doorPrevious */
	private void closeAllDoors( Door... doorExcluded ) {

		for ( int i=0; i< zoneListToCross.size()-1 ; i++  )
		{
			Door doorToClose = maze.getDoor( zoneListToCross.get( i ) , zoneListToCross.get( i+1 ) );
			boolean canClose = true;
			if ( doorExcluded != null )
			{
				for ( Door door : doorExcluded )
				{
					if ( doorToClose == door ) canClose = false;
				}
			}
			if ( canClose )
			{
				doorToClose.close();
			}
		}

	}

	private void openAllDoors() {

		for ( int i=0; i< zoneListToCross.size()-1 ; i++  )
		{
			Door door = maze.getDoor( zoneListToCross.get( i ) , zoneListToCross.get( i+1 ) );
			door.open();
			//System.out.println("[PathManager open door] " + door );
		}

	}

	private boolean isPathFreeOfAnimal() {

		for ( Zone zone : zoneListToCross )
		{
			if ( zone.containsDetection ( detectionList ) ) return false;
		}
		return true;
	}

	private boolean allDoorClosed() {

		for ( int i=0; i< zoneListToCross.size()-1 ; i++  )
		{
			Door door = maze.getDoor( zoneListToCross.get( i ) , zoneListToCross.get( i+1 ) );
			if ( door.getStatus() != DoorStatus.CLOSED )
			{
				return false;
			}
		}

		return true;
	}

	public void start() {

		System.out.println("Path booker starting");
		state = PathManagerState.RUNNING;

	}



	public void bookPath(Zone startZone, Zone endZone ) {

		// build the graph to link startZone to endZone (super simple algo, to complex if complex maze is created)

		zoneListToCross.clear();
		zoneListToCross.add( startZone );

		while ( zoneListToCross.get( zoneListToCross.size()-1 ) != endZone )
		{
			// find next
			Zone currentZone = zoneListToCross.get( zoneListToCross.size()-1 );
			for ( Segment segment : maze.segmentList )
			{
				if( segment.contains( currentZone ) ) // is the segment connecting our last zone to something ?
				{
					// take the segment if we never been where is brings. (work in straight very simple case)
					if ( !zoneListToCross.contains( segment.otherZone( currentZone ) ) )
					{
						zoneListToCross.add( segment.otherZone( currentZone ) );
						break;
					}
				}
			}
		}

		System.out.println("Path found from " + startZone + " to " + endZone );
		for ( Zone zone : zoneListToCross )
		{
			System.out.println( "zone: " + zone );
		}



	}

	@Override
	public void liveTrackerPostInitEvent(LiveMouseTracker liveMouseTracker) {

	}

	@Override
	public void liveTrackerPostProcessDetectionFiltering( ArrayList<MouseDetection> rawMouseDetectionList, int t, IcyBufferedImage depthImage) {

	}



}
