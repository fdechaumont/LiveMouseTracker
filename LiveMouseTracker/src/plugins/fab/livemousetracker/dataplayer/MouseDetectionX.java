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

import java.awt.Graphics2D;
import java.awt.geom.Point2D;

import plugins.fab.livemousetracker.dataplayer.UE4Data;
import plugins.fab.livemousetracker.detection.MouseDetection;

public class MouseDetectionX
{
	int animalId ;
	Graphics2D g;
	public MouseDetection mouseDetection;
	Point2D displayInfoTarget;
	String infoText="";
	UE4Data ue4Data;
	long dataBaseId;


	public Point2D front; // tmp for postprocess data
	public Point2D back; // tmp for postprocess data

	public MouseDetectionX( long dataBaseId , int animalId , MouseDetection mouseDetection ) {
		this.animalId = animalId;
		this.dataBaseId = dataBaseId;
		this.mouseDetection = mouseDetection;
	}

	void computeUE4Data()
	{
		ue4Data = new UE4Data( this );
	}
}
