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
package plugins.fab.livemousetracker.morpho;

import icy.roi.BooleanMask2D;
import icy.roi.ROI;
import icy.roi.ROI2D;

import java.awt.Point;

import plugins.fab.livemousetracker.Util;
import plugins.kernel.roi.roi2d.ROI2DArea;

public class MorphoROITools {

	/**
     * Will dilate if the pointZ > Z value in the depth map.
     */
    public static ROI2DArea dilateROIWithZConstraint(ROI roi, int xRadius, int yRadius, int z )
    {

        int rx = xRadius, rrx = rx * rx;
        int ry = yRadius, rry = ry * ry;

        if (roi instanceof ROI2D)
        {
            BooleanMask2D m2 = ((ROI2D) roi).getBooleanMask(true);
            ROI2DArea r2 = new ROI2DArea(m2);
            r2.setC(((ROI2D) roi).getC());
            r2.setZ(((ROI2D) roi).getZ());
            r2.setT(((ROI2D) roi).getT());

            r2.beginUpdate();

            for (Point p : m2.getContourPoints())
            {
                // Brute force
                for (int y = -ry; y <= ry; y++)
                    for (int x = -rx; x <= rx; x++)
                    {
                        double xr2 = rrx == 0 ? 0 : x * x / rrx;
                        double yr2 = rry == 0 ? 0 : y * y / rry;

                        if (xr2 + yr2 <= 1.0)
                        {
                        	if (!m2.contains(p.x + x, p.y + y))
                        	{
                        		if ( Util.getZ( p ) > z )
                        		{
                        			r2.addPoint(p.x + x, p.y + y);
                        		}
                        	}
                        }
                    }

            }
            r2.endUpdate();

            return r2;
        }
        return null;
    }


	/**
     * MovingPower is The moving power is used
     * @return a new, dilated ROI of type "area"
     */
    public static ROI2DArea dilateROI(ROI roi, int xRadius, int yRadius, double movingPower )
    {

        int rx = xRadius, rrx = rx * rx;
        int ry = yRadius, rry = ry * ry;

        if (roi instanceof ROI2D)
        {
            BooleanMask2D m2 = ((ROI2D) roi).getBooleanMask(true);
            ROI2DArea r2 = new ROI2DArea(m2);
            r2.setC(((ROI2D) roi).getC());
            r2.setZ(((ROI2D) roi).getZ());
            r2.setT(((ROI2D) roi).getT());

            r2.beginUpdate();

            for (Point p : m2.getContourPoints())
            {
                // Brute force
                for (int y = -ry; y <= ry; y++)
                    for (int x = -rx; x <= rx; x++)
                    {
                        double xr2 = rrx == 0 ? 0 : x * x / rrx;
                        double yr2 = rry == 0 ? 0 : y * y / rry;

                        if (xr2 + yr2 <= 1.0)
                        {
                        //	if ( Math.random() <= movingPower )
                        	{
                        		if (!m2.contains(p.x + x, p.y + y))
                        		{
                        			r2.addPoint(p.x + x, p.y + y);
                        		}
                        	}
                        }
                    }

            }
            r2.endUpdate();

            return r2;
        }

        //System.out.println("[Dilate ROI] Warning: unsupported ROI: " + roi.getName());
        return null;
    }

	   public static ROI2DArea erodeROI(ROI roi, int xRadius, int yRadius , double movingPower )
	    {
	        // The basis of this erosion operator is to remove all pixels within a distance of "radius"
	        // from the border. Since we have easy access to the contour points of the ROI, we will
	        // start from there and instead use a radius of "radius - 1" when searching for pixels to
	        // erase, so as to be consistent with the dual dilation operator, such that openings
	        // (erosion + dilation) and closings (dilation + erosion) preserve the global ROI size

	        int rx = Math.max(0, xRadius - 1), rrx = rx * rx;
	        int ry = Math.max(0, yRadius - 1), rry = ry * ry;
	      //  int rz = Math.max(0, zRadius - 1), rrz = rz * rz;

	        if (roi instanceof ROI2D)
	        {
	            BooleanMask2D m2 = ((ROI2D) roi).getBooleanMask(true);
	            ROI2DArea r2 = new ROI2DArea(m2);

	            r2.beginUpdate();

	            for (Point p : m2.getContourPoints())
	            {
	                // Brute force
	                for (int y = -ry; y <= ry; y++)
	                    for (int x = -rx; x <= rx; x++)
	                    {
	                        double xr2 = rrx == 0 ? 0 : x * x / rrx;
	                        double yr2 = rry == 0 ? 0 : y * y / rry;

	                        // correct the sphere equation to include the outer rim for each pixel
	                        if (xr2 + yr2 <= 2.0)
	                        {
	                        //	if ( Math.random() <= movingPower )
	                        	{
	                        		if (m2.contains(p.x + x, p.y + y)) r2.removePoint(p.x + x, p.y + y);
	                        	}
	                        }
	                    }
	            }
	            r2.endUpdate();

	            return r2.getNumberOfPoints() > 0 ? r2 : null;
	        }

	        System.out.println("[Erode ROI] Warning: unsupported ROI: " + roi.getName());
	        return null;
	    }

}
