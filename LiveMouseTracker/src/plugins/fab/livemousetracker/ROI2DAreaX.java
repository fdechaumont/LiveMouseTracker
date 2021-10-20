
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

import icy.canvas.IcyCanvas;
import icy.canvas.IcyCanvas2D;
import icy.canvas.IcyCanvas3D;
import icy.roi.BooleanMask2D;
import icy.roi.ROI;
import icy.sequence.Sequence;
import icy.util.GraphicsUtil;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import com.jogamp.common.util.ReflectionUtil;

import plugins.kernel.roi.roi2d.ROI2DArea;
import plugins.kernel.roi.roi2d.ROI2DArea.ROI2DAreaPainter;

public class ROI2DAreaX extends ROI2DArea {

	public boolean fill = false;

	public class ROI2DAreaXPainter extends ROI2DAreaPainter
	{
		@Override
		public void drawROI(Graphics2D g, Sequence sequence, IcyCanvas canvas) {

			if (canvas instanceof IcyCanvas2D)
            {
                // not supported
                if (g == null)
                    return;

                final Rectangle bounds = getBounds();
                // trivial paint optimization
                final boolean shapeVisible = GraphicsUtil.isVisible(g, bounds);

                if (shapeVisible)
                {
                    final Graphics2D g2 = (Graphics2D) g.create();

                    g2.setColor( this.color );

                    for (Point pt : getBooleanMask( false ).getContourPoints()  )
                    {
                    	g2.fillRect((int) pt.getX(), (int) pt.getY(), 2, 2);
                    }

//                      Fill draw
                    if ( fill )
                    {
                    	g2.setColor(getDisplayColor());
                    	try {
                    		g2.drawImage(
                    				(BufferedImage) icy.util.ReflectionUtil.getFieldObject( ROI2DAreaX.this, "imageMask", true ),
                    				null, bounds.x, bounds.y);
                    	} catch ( IllegalArgumentException e )
                    	{
                    		e.printStackTrace();
                    	}
                    	catch ( IllegalAccessException e )
                    	{
                    		e.printStackTrace();
                    	}
                    	catch (  SecurityException e ){
                    		e.printStackTrace();
                    	}
                    	catch( NoSuchFieldException e)
                    	{
                    		e.printStackTrace();
                    	}



                    }
                    g2.dispose();
                }

            }



			//super.drawROI(g, sequence, canvas);
		}

	}

	public ROI2DAreaX(BooleanMask2D mask) {
		super( mask );
	}

	@Override
	protected ROI2DAreaPainter createPainter() {
		return new ROI2DAreaXPainter();
	}

	public ROI2DAreaX() {
		super();
	}

}
