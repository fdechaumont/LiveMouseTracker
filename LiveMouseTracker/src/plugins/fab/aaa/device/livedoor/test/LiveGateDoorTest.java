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
package plugins.fab.aaa.device.livedoor.test;

import icy.image.IcyBufferedImage;
import icy.plugin.abstract_.PluginActionable;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import plugins.fab.aaa.device.livedoor.Door;
import plugins.fab.aaa.device.livedoor.MazeGraph;
import plugins.fab.aaa.device.livedoor.PathManager;
import plugins.fab.aaa.device.livedoor.Segment;
import plugins.fab.aaa.device.livedoor.Zone;
import plugins.fab.livemousetracker.LiveMouseTracker;
import plugins.fab.livemousetracker.detection.MouseDetection;
import plugins.fab.livemousetracker.device.control.ServoUtil;
import plugins.fab.livemousetracker.device.control.ServoUtil.SERVO_TYPE;
import plugins.fab.livemousetracker.listener.LiveTrackerListener;
import plugins.kernel.roi.roi2d.ROI2DPolygon;
import plugins.kernel.roi.roi2d.ROI2DRectangle;
import SimpleDynamixel.Servo;

public class LiveGateDoorTest extends PluginActionable implements LiveTrackerListener {

	private Servo servo;
	private Door door1;
	private Door door2;
	private Door door3;
	private Door door4;

	Zone zoneCorridorEast ;
	Zone zoneCorridorMiddle ;
	Zone zoneRoomEast ;
	Zone zoneCorridorWest ;
	Zone zoneRoomWest ;

	//Gate gateEast;
	//Gate gateWest;

	MazeGraph maze;

	boolean initDone = false;

	@Override
	public void run() {

		System.out.println("FIXME: this should call an abstract class to start the tracking.");

	}

	public LiveGateDoorTest() {

		servo = ServoUtil.initServo( SERVO_TYPE.PLASTIC, "COM3" );

		door1 = new Door( "Door 1" , 2, 952 , 714+40, 300, 300, servo ); // 2
		door1.createOverlay( new Point2D.Double( 53, 175 ) );

//		door2 = new Door( "Door 2" , 4, 359 , 110+40, 300, 300, servo ); // 4
//		door2.createOverlay( new Point2D.Double( 180, 175 ) );

		door3 = new Door( "Door 3" , 1, 654 , 420+40, 300, 300, servo ); // 1
		door3.createOverlay( new Point2D.Double( 272, 175 ) );

//		door4 = new Door( "Door 4" , 3, 962 , 700+40, 300, 300, servo ); //3
//		door4.createOverlay( new Point2D.Double( 386, 175 ) );

		door1.open();
//		door2.open();
		door3.open();
//		door4.open();

	}

	PathManager pathBooker = null;

	public void bookAPath()
	{
		System.out.println("Booking path.");

		pathBooker = new PathManager( maze , zoneCorridorEast , zoneCorridorWest );

		//pathBooker.bookPath( zoneCorridorWest , zoneCorridorEast );
		pathBooker.bookPath( zoneCorridorEast, zoneCorridorWest );
		pathBooker.start();

	}

	@Override
	public void liveTrackerEndOfFrame( LiveMouseTracker liveMouseTracker ) {

		if ( !LiveMouseTracker.getBackgroundHeightMapBuider().isReady() ) return;

		if ( !initDone )
		{
			init( liveMouseTracker );
			return;
		}

		if ( pathBooker == null )
		{
			bookAPath();
		}

	}

	private void init(LiveMouseTracker liveMouseTracker) {

		ROI2DPolygon roiCage = new ROI2DPolygon( new Point2D.Double( 11, 178 ) );
		roiCage.addNewPoint( new Point2D.Double(  55, 176 ), false);
		roiCage.addNewPoint( new Point2D.Double(  55, 137 ), false);
		roiCage.addNewPoint( new Point2D.Double( 181, 137 ), false);
		roiCage.addNewPoint( new Point2D.Double( 181, 176 ), false);
		roiCage.addNewPoint( new Point2D.Double( 181, 176 ), false);
		roiCage.addNewPoint( new Point2D.Double( 273, 176 ), false);
		roiCage.addNewPoint( new Point2D.Double( 273, 138 ), false);
		roiCage.addNewPoint( new Point2D.Double( 387, 138 ), false);
		roiCage.addNewPoint( new Point2D.Double( 387, 175 ), false);
		roiCage.addNewPoint( new Point2D.Double( 448, 175 ), false);
		roiCage.addNewPoint( new Point2D.Double( 448, 207 ), false);
		roiCage.addNewPoint( new Point2D.Double( 386, 207 ), false);
		roiCage.addNewPoint( new Point2D.Double( 386, 241 ), false);
		roiCage.addNewPoint( new Point2D.Double( 274, 240 ), false);
		roiCage.addNewPoint( new Point2D.Double( 274, 207 ), false);
		roiCage.addNewPoint( new Point2D.Double( 181, 207 ), false);
		roiCage.addNewPoint( new Point2D.Double( 181, 241 ), false);
		roiCage.addNewPoint( new Point2D.Double( 54, 241 ), false);
		roiCage.addNewPoint( new Point2D.Double( 54, 209 ), false);
		roiCage.addNewPoint( new Point2D.Double( 10, 209 ), false);
		roiCage.setCreating( false );

		// zone setup
		ROI2DRectangle ROIcorridorEast;
		ROI2DRectangle ROIroomEast;
		ROI2DRectangle ROIcorridorMiddle;
		ROI2DRectangle ROIroomWest;
		ROI2DRectangle ROIcorridorWest;

		ROIcorridorEast = new ROI2DRectangle(
				new Point2D.Double( 446,176 ), new Point2D.Double( 386, 205 ) );
		ROIcorridorEast.setColor( Color.ORANGE );
		ROIcorridorEast.setName("Corridor East");
		ROIcorridorEast.setShowName( true );


		ROIcorridorMiddle = new ROI2DRectangle(
				new Point2D.Double( 272, 177 ), new Point2D.Double( 181, 205 ) );
		ROIcorridorMiddle.setColor( Color.ORANGE );
		ROIcorridorMiddle.setName("Corridor Middle");
		ROIcorridorMiddle.setShowName( true );

		ROIroomEast = new ROI2DRectangle(
				new Point2D.Double( 382, 139 ), new Point2D.Double( 275, 237 ) );
		ROIroomEast.setColor( Color.ORANGE );
		ROIroomEast.setName("Room East");
		ROIroomEast.setShowName( true );

		ROIcorridorWest = new ROI2DRectangle(
				new Point2D.Double( 54, 179 ), new Point2D.Double( 11, 207 ) );
		ROIcorridorWest.setColor( Color.ORANGE );
		ROIcorridorWest.setName("Corridor West");
		ROIcorridorWest.setShowName( true );

		ROIroomWest = new ROI2DRectangle(
				new Point2D.Double( 180, 139 ), new Point2D.Double( 56, 238 ) );
		ROIroomWest.setColor( Color.ORANGE );
		ROIroomWest.setName("Room West");
		ROIroomWest.setShowName( true );

		LiveMouseTracker.addROIToInfraSequence( ROIcorridorEast );
		LiveMouseTracker.addROIToInfraSequence( ROIroomEast );
		LiveMouseTracker.addROIToInfraSequence( ROIcorridorMiddle );
		LiveMouseTracker.addROIToInfraSequence( ROIroomWest );
		LiveMouseTracker.addROIToInfraSequence( ROIcorridorWest );

		//LiveMouseTracker.setROICage( roiCage );

		// create maze
		zoneCorridorEast = new Zone( ROIcorridorEast );
		zoneCorridorMiddle = new Zone ( ROIcorridorMiddle );
		zoneRoomEast = new Zone( ROIroomEast );
		zoneCorridorWest = new Zone( ROIcorridorWest );
		zoneRoomWest = new Zone( ROIroomWest );

		maze = new MazeGraph();
		maze.addZone( zoneCorridorEast );
		maze.addZone( zoneCorridorMiddle );
		maze.addZone( zoneRoomEast );
		maze.addZone( zoneCorridorWest );
		maze.addZone( zoneRoomWest );

		maze.addSegment( new Segment( zoneCorridorWest , door1 , zoneRoomWest ) );
		maze.addSegment( new Segment( zoneRoomWest , door2 , zoneCorridorMiddle ) );
		maze.addSegment( new Segment( zoneCorridorMiddle , door3 , zoneRoomEast ) );
		maze.addSegment( new Segment( zoneRoomEast , door4 , zoneCorridorEast ) );

		LiveMouseTracker.addOverlayToInfraSequence( door1.getOverlay() );
		LiveMouseTracker.addOverlayToInfraSequence( door2.getOverlay() );
		LiveMouseTracker.addOverlayToInfraSequence( door3.getOverlay() );
		LiveMouseTracker.addOverlayToInfraSequence( door4.getOverlay() );

		liveMouseTracker.TRACKING_ENABLED = false;
		liveMouseTracker.TRACKING_IDENTITY_RECOVERY_ENABLED = false;

		initDone = true;
	}

	@Override
	public void liveTrackerPostInitEvent(LiveMouseTracker liveMouseTracker) {
		// TODO Auto-generated method stub

	}

	@Override
	public void liveTrackerPostProcessDetectionFiltering(
			ArrayList<MouseDetection> rawMouseDetectionList, int t,
			IcyBufferedImage depthImage) {
		// TODO Auto-generated method stub

	}

}
