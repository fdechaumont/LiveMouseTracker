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

import icy.image.IcyBufferedImage;
import icy.roi.BooleanMask2D;
import icy.sequence.Sequence;

import java.awt.Point;

import javax.vecmath.Point3i;

/*
 * doc from http://www.sciencedirect.com/science/article/pii/S0191814103000932
 */
public class Moment {

	public MomentParameter aoipar = new MomentParameter();
	public MomentMesure aoimes = new MomentMesure();

	public Moment( BooleanMask2D mask, IcyBufferedImage infraImage ) { //, IcyBufferedImage seq ) {

		Point[] ptsCc = mask.getPoints();

		int volume = ptsCc.length;

		int w = 512;
		int h = 424;

		if ( infraImage != null )
		{
			w = infraImage.getWidth();
			h = infraImage.getHeight();
		}
//		int w = seq.getWidth();
//		int h = seq.getHeight();

		// Re-init
		aoimes.m00 = 0;
		aoimes.m01 = 0;
		aoimes.m02 = 0;
		aoimes.m03 = 0;
		aoimes.m10 = 0;
		aoimes.m11 = 0;
		aoimes.m12 = 0;
		aoimes.m20 = 0;
		aoimes.m21 = 0;
		aoimes.m30 = 0;

		// aoimes.m00 = cc.getVolume();

		//long st = System.currentTimeMillis();
		//System.out.print("start: ");

		int perimeter = (int) mask.getContourLength();
		for (int i = 0; i < ptsCc.length; i++) {
			int x = ptsCc[i].x;
			int y = ptsCc[i].y;

			// Compute moments to include in (cc)
			double d1 = (double) y; /* !!col=y!! */
			double d2 = (double) x; /* !!row=x!! */
			aoimes.m00 += 1;
			aoimes.m01 += d1;
			aoimes.m10 += d2;
			aoimes.m11 += d1 * d2;
			d1 *= d1; // y2
			d2 *= d2; // x2
			aoimes.m02 += d1;
			aoimes.m20 += d2;
			aoimes.m12 += (double) x * d1;
			aoimes.m21 += (double) y * d2;
			aoimes.m03 += (double) y * d1;
			aoimes.m30 += (double) x * d2;

		}
		//System.out.println(System.currentTimeMillis() - st);

		double mu11, mu20, mu02, mu12, mu21, mu30, mu03, nu11, nu20, nu02, nu12, nu21, nu30, nu03, xm, ym, xm2, ym2, theta, cos2, sin2, cos_2, sin_2, a1, a2, a3, a4, a5, a6, a7, a8, a9, sigma_x, sigma_y, sigma_MX, sigma_MY, puissance;
		double l1, l2;

		/* GEOMETRY RELATED MEASURES */
		aoipar.area = volume;
		aoipar.perimeter = perimeter;
		aoipar.x = aoimes.m10 / volume;
		aoipar.y = aoimes.m01 / volume;

		mu20 = aoimes.m20 - (volume * (aoipar.x * aoipar.x));
		mu02 = aoimes.m02 - (volume * (aoipar.y * aoipar.y));
		mu11 = aoimes.m11 - (volume * (aoipar.x) * (aoipar.y));

		aoipar.sigma_x = Math.sqrt(mu20 / (double) volume);
		aoipar.sigma_y = Math.sqrt(mu02 / (double) volume);

		if (mu20 == mu02) {
			theta = Math.PI / 4d;
		} else {
			theta = 0.5d * Math.atan2( 2d * mu11 , mu20 - mu02  );
		}
		aoipar.theta = theta;

		cos2 = Math.pow(Math.cos(theta), 2);
		sin2 = Math.pow(Math.sin(theta), 2);
		cos_2 = Math.cos((((double) 2.0) * theta));
		sin_2 = Math.sin((((double) 2.0) * theta));

		/* compute the eccentricity of the shape */
		a1 = mu02 * cos2 + mu20 * sin2 - mu11 * sin_2;
		a2 = mu02 * sin2 + mu20 * cos2 + mu11 * cos_2;
		if ((a1 == (double) 0.0) || (a2 == (double) 0.0)) {
			aoipar.eccentricity = (double) -1.0;
		} else {
			double ratio = a1 / a2;
			if ((ratio) > 0)
				aoipar.eccentricity = Math.sqrt(ratio);
			else
				aoipar.eccentricity = Math.sqrt(-ratio);
		}

		/* compute the moments variance on X axis (1st principal axis) */
		if (volume == (double) 0.0) {
			aoipar.sigma_MX = sigma_MX = (double) -1.0;
			aoipar.sigma_MY = sigma_MY = (double) -1.0;
		} else {
			aoipar.sigma_MX = sigma_MX = Math
					.sqrt((cos2 * mu20 + sin2 * mu02 + sin_2 * mu11) / volume);
			aoipar.sigma_MY = sigma_MY = Math
					.sqrt((sin2 * mu20 + cos2 * mu02 - sin_2 * mu11) / volume);
		}

		/* coeff 1.5 to make the axis bigger in the axis image */
		a1 = (double) (1.75) * Math.cos(theta) * sigma_MX;
		a2 = (double) (1.75) * Math.sin(theta) * sigma_MX;
		aoipar.axis_x1deb = aoipar.x - a1;
		if (aoipar.axis_x1deb < (double) 0.0)
			aoipar.axis_x1deb = (double) 2.0;
		aoipar.axis_x1end = aoipar.x + a1;
		if (aoipar.axis_x1end > (double) w)
			aoipar.axis_x1end = (double) (w - 2);
		aoipar.axis_y1deb = aoipar.y - a2;
		if (aoipar.axis_y1deb < (double) 0.0)
			aoipar.axis_y1deb = (double) 2.0;
		aoipar.axis_y1end = aoipar.y + a2;
		if (aoipar.axis_y1end > (double) h)
			aoipar.axis_y1end = (double) (h - 2);

		a1 = -(double) (1.75) * Math.sin(theta) * sigma_MY;
		a2 = (double) (1.75) * Math.cos(theta) * sigma_MY;
		aoipar.axis_x2deb = aoipar.x - a1;
		if (aoipar.axis_x2deb < (double) 0.0)
			aoipar.axis_x2deb = (double) 2.0;
		aoipar.axis_x2end = aoipar.x + a1;
		if (aoipar.axis_x2end > (double) w)
			aoipar.axis_x2end = (double) (w - 2);
		aoipar.axis_y2deb = aoipar.y - a2;
		if (aoipar.axis_y2deb < (double) 0.0)
			aoipar.axis_y2deb = (double) 2.0;
		aoipar.axis_y2end = aoipar.y + a2;
		if (aoipar.axis_y2end > (double) h)
			aoipar.axis_y2end = (double) (h - 2);

		/* compute the axialRatio as the ratio of the two major axis */
		l1 = Math.pow(aoipar.axis_x1deb - aoipar.axis_x1end, 2)
				+ Math.pow(aoipar.axis_y1deb - aoipar.axis_y1end, 2);
		l1 = Math.sqrt(l1);
		l2 = Math.pow(aoipar.axis_x2deb - aoipar.axis_x2end, 2)
				+ Math.pow(aoipar.axis_y2deb - aoipar.axis_y2end, 2);
		l2 = Math.sqrt(l2);
		if (l1 >= l2) {
			aoipar.longAxis = l1;
			aoipar.shorterAxis = l2;
			if (l2 != 0.0) {
				aoipar.axialRatio = l1 / l2;

			} else
				aoipar.axialRatio = 1000;
		} else {
			aoipar.longAxis = l2;
			aoipar.shorterAxis = l1;
			if (l1 != 0.0)
				aoipar.axialRatio = l2 / l1;
			else
				aoipar.axialRatio = 1000;
		}

		/* compute the compactness */
		if (aoipar.area != 0.0)
			aoipar.compactness = aoipar.perimeter * aoipar.perimeter
			/ (4d * Math.PI * aoipar.area);
		else
			aoipar.compactness = -1;

		/* compute the circularity */
		aoipar.circularity = 4d * aoipar.area
				/ (aoipar.perimeter * aoipar.longAxis);

		// ellipticity: pi*L*L/(2*A)
		if (aoipar.area != 0.0)
			aoipar.ellipticity = Math.PI * aoipar.longAxis * aoipar.longAxis
			/ (2 * aoipar.area);

		// Ferret diameter: 2*sqrt(A/pi)
		aoipar.diameterFerret = 2 * Math.sqrt(aoipar.area / Math.PI);

		/* END OF GEOMETRY RELATED MEASURES */
		/*
		 * System.out.println("x: "+aoipar.x+" y: "+aoipar.y);
		 * System.out.println("area: "+aoipar.area);
		 * System.out.println("roundness: "+aoipar.roundness);
		 * System.out.println("l1: "+l1); System.out.println("l2: "+l2);
		 */

	}

	/*
	public void compute()
	{
		delta = Math.sqrt(
				4d * m11 * m11 +
				( m20 - m02 )
				);

		phi =
				0.5d * Math.atan(
						2d * m11 / ( m20 - m02 ) );
		a = Math.sqrt( 2d * ( m20 + m02 + delta ) / m11 );
		b = Math.sqrt( 2d * ( m20 + m02 - delta ) / m11 );

		ratio = ( m20 + m02 + delta) / ( m20 + m02 - delta );
	}
	 */
}

