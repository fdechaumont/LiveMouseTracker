package plugins.fab.livemousetracker.remotearena;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;

import icy.roi.ROI;
import icy.roi.ROI2D;
import plugins.kernel.roi.roi2d.ROI2DRectangle;

public class RemoteArenaInfo implements Serializable {

	private static final long serialVersionUID = 4745690007519683641L;

	/** 2D coordinates of this area.
	 * ( top corner of observation field )
	 * 0,0 is the master.
	 * a setup is typically 512x424, (resol of the IR sensor)
	 * */
	public Point2D localization;
	public Rectangle cropRectangle;

	public Polygon cagePolygon; // like cage floor (not used for detection crop)
	public Polygon cageFloorPolygon; // used for detection crop

	/** name of the setup */
	public String name;

	public RemoteArenaInfo( Point2D localization, Rectangle cropRectangleArena, String name,
			Polygon cagePolygon, Polygon cageFloorPolygon ) {
		this.localization = localization;
		this.cropRectangle = cropRectangleArena;
		this.name = name;


		cagePolygon.translate( (int)localization.getX(), (int)localization.getY() );
		cageFloorPolygon.translate( (int)localization.getX(), (int)localization.getY() );

		this.cagePolygon = cagePolygon;
		this.cageFloorPolygon = cageFloorPolygon;
	}

	public Polygon getUntranslatedCagePolygon() {

		Polygon cageCopy =  new Polygon( cagePolygon.xpoints , cagePolygon.ypoints, cagePolygon.npoints );
		cageCopy.translate( -(int)localization.getX() , -(int)localization.getY() );
		return cageCopy;

	}

	public Polygon getUntranslatedCageFloorPolygon() {

		Polygon cageFloorCopy = new Polygon( cageFloorPolygon.xpoints, cageFloorPolygon.ypoints, cageFloorPolygon.npoints );
		cageFloorCopy.translate( -(int)localization.getX() , -(int)localization.getY() );
		return cageFloorCopy;

	}

}
