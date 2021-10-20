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

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import plugins.fab.livemousetracker.morpho.Moment;
import plugins.kernel.roi.roi2d.ROI2DArea;
import icy.plugin.abstract_.PluginActionable;
import icy.roi.BooleanMask2D;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.roi.ROI2DLine;
import icy.roi.ROIUtil;
import icy.sequence.Sequence;

public class LiveMouseTestFitEllipse extends PluginActionable {

	@Override
	public void run() {

		Sequence sequence = getActiveSequence();

		for ( ROI2D roi : sequence.getROI2Ds() )
		{
			if (!( roi instanceof ROI2DArea ) ) continue;

			BooleanMask2D b2 = roi.getBooleanMask( true );
			Point2D massCenter = ROIUtil.getMassCenter( roi );
			Moment moment = new Moment( b2 , LiveMouseTracker.infraImage ); //, sequence.getFirstImage() );
			double angle = moment.aoipar.theta;

			ROI resultROI = new ROI2DLine( new Line2D.Double( massCenter.getX() , massCenter.getY(),
					massCenter.getX() + Math.cos( angle ) * 30d , massCenter.getY() + Math.sin( angle ) * 30d
					) );
			resultROI.setName("" + moment.aoipar.axialRatio );
			resultROI.setShowName( true );

			sequence.addROI( resultROI );


		}

	}

}
