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
package plugins.fab.livemousetracker.histogram;

import icy.image.IcyBufferedImage;
import icy.roi.BooleanMask2D;
import icy.type.collection.array.Array1DUtil;

import java.awt.Rectangle;

import plugins.kernel.roi.roi2d.ROI2DArea;


/**
 * This class represent an histogram of a gray-level image.
 * The histogram is represented like this: y axis is i1+i2. x axis is cartesian distance.
 * The histogram is created using class to use less memory and optimize memory.
 *
 * TODO: Histogram should be compared between them using quadtrees to make it faster.
 *
 */
public class Histogram {

	/** input area used to compute the histogram */
	private ROI2DArea area;

	int yClassSize = 500;
	final int yNbClass = 64;
	int xClassSize = 2;
	final int xNbClass = 64;


	/** linear representation of the intensity histogram based on i1 + i2 */
	private float[] intensityHist = new float[xNbClass*yNbClass];

	/** linear representation of the contrast histogram based on i2 - i1 */
	private float[] contrastHist = new float[xNbClass*yNbClass];

	/** height of the histograms
	 * FIXME: should be static as they are common to all hists ?
	 * */
	private int height;

	/** width of histograms
	 * FIXME: should be static as they are common to all hists ?
	 * */
	private int width;

	/* FAUX
	 *
	int maxIntensity;
	int maxDistance;
	int maxLength;
	*/


/*
	Histogram(ROI2DArea area,IcyBufferedImage image, int pMaxLength)
	{
		this.area=area;
		this.image=image;
		this.width=image.getWidth();
		this.height=image.getHeight();
		this.maxLength= pMaxLength;
	}
*/

	public Histogram(ROI2DArea area,IcyBufferedImage image)
	{
		this.area=area;
		// FIXME : aucun sens que ce soit la taille de l'image ?
		this.width=image.getWidth();
		this.height=image.getHeight();
		computeHistograms( image );
	}


	/**
	 * Compute intensity and contrast histograms
	 * TODO: optimze in triangle to avoid twice the computation
	 * */
	public void computeHistograms( IcyBufferedImage image )
	{
		int width = image.getWidth();

		Rectangle rec=area.getBounds();

		int minX=(int)rec.getMinX();
		int maxX=(int)rec.getMaxX();
		int minY=(int)rec.getMinY();
		int maxY=(int)rec.getMaxY();

		// FIXME: SHOULD CROP FIRST !
		float[] imageData=Array1DUtil.arrayToFloatArray(image.getDataXY(0), image.isSignedDataType());

		BooleanMask2D booleanMask =area.getBooleanMask(true);

		for ( int x1 = minX ; x1 < maxX ; x1++ )
			for ( int y1 = minY ; y1 < maxY ; y1++ )
			{
				if ( !booleanMask.contains( x1 ,  y1 ) ) continue;

				for ( int x2 = minX ; x2 < maxX ; x2++ )
					for ( int y2 = minY ; y2 < maxY ; y2++ )
					{
						if ( !booleanMask.contains( x2 ,  y2 ) ) continue;

						float intensity1 = imageData[y1*width+x1];
						float intensity2 = imageData[y2*width+x2];

						float distance = getDistance( x1 ,  y1 ,  x2 ,  y2 );

						// find coordinates in histogram.

						int xH = (int)( distance / xClassSize );
						int yH = (int)( (intensity1+intensity2) / yClassSize );

						// check if the result is over the defined histogram range
						if ( xH >= xNbClass || yH >= yNbClass ) continue;

						intensityHist[xH + yH * xNbClass ]++;

					}

			}


		/*
		int beginX=minI;
		int beginY=minJ;


		intensityHist = new float[maxLength*512];
		contrastHist = new float[maxLength*512];

	    BooleanMask2D bm=area.getBooleanMask(true);

		while(beginX<maxI && beginY<maxJ)
		{

			if(bm.contains(beginX,beginY))
			{

				int j=minJ;
				int i2=beginX;


				if(beginY==maxJ-1)
				{
				j=minJ;
				i2=beginX+1;
				}

				else j=beginY+1;

				for(int x2=i2;x2<maxI;x2++)
				{

					for(int y2=j;y2<maxJ;y2++)
					{

							if(bm.contains(x2,y2))
							{

								float distance = getDistance(beginX,
										beginY, x2, y2);

								float sum = data[beginX*width+beginY] + data[x2*width+y2];

								float difference = Math.abs(data[beginX*width+beginY]-data[x2*width+y2]);


								int classCalculation = (int) (distance /5);
								int intensityCalculation = (int) (sum /5);
								int contrastCalculation = (int) (difference /5);


								intensityHist[(int) classCalculation*width+(int) intensityCalculation]++;
								contrastHist[(int)classCalculation*width+(int)contrastCalculation]++;
							}
						}
					}
		    	}


		   if(beginY==maxJ-1 && beginX<maxI)
		   {
			   beginY=minJ;
			   beginX++;

		   }

		   else beginY++;

		}
		*/


	}

	/** returns the contrast map as an IcyBufferedImage
	 * x,y size are the number of classes of the histogram */
	public IcyBufferedImage getContrastHistogramAsImage()
	{
		IcyBufferedImage imageContrastMap = null;
		System.err.println("FIXME: sdipqfh");
		// FIXME: error here.
		// IcyBufferedImage imageContrastMap=new IcyBufferedImage(pWidth, pHeigh, contrastHist);
		return imageContrastMap;
	}

	/** returns the intensity histogram as an IcyBufferedImage */
	public IcyBufferedImage getIntensityHistogramAsImage()
	{
		IcyBufferedImage intensityHistogramAsImage =new IcyBufferedImage(xNbClass, yNbClass, intensityHist);
		return intensityHistogramAsImage;
	}

	/**
	 * distance from current histogram to candidate histogram
	 * (using Intensity histograms)
	 *  */
	public float compareHistogramIntensity(Histogram candidateHistogram)
	{
		float mean=0;
		float[] candidateHist=candidateHistogram.getIntensityHistogram1D();

		for(int i=0;i< intensityHist.length;i++)
		{
			mean+=Math.abs( intensityHist[i] - candidateHist[i] );
		}

		mean/= intensityHist.length;

		return mean;
	}

	/*
	public float compareMap2(Histogram map)
	{
		float mean=0;
		float meanContrast=0;

		float[] intensityMap2=map.getIntensityMap();
		float[] contrastMap2=map.getContrastMap();

		for(int i=0;i<contrastHist.length;i++)
		{
			mean+=Math.abs(intensityHist[i]-intensityMap2[i]);
			meanContrast+=Math.abs(contrastHist[i]-contrastMap2[i]);
		}

		mean/=contrastMap2.length;
		meanContrast/=contrastMap2.length;

		//to test
		return mean+meanContrast;
	}
	*/

	/** cartesian distance between 2 points. */
	float getDistance(int x1,int y1, int x2, int y2)
	{
		return (float) Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1));
	}

	public ROI2DArea getArea() {
		return area;
	}

	public float[] getIntensityHistogram1D() {
		return intensityHist;
	}

	public float[] getContrastHistogram1D() {
		return contrastHist;
	}

	/*
	 * CLASS INTEGRITY ERROR: this SETTER DOES NOT RECOMPUTE
	public void setArea(ROI2DArea area) {
		this.area = area;
	}*/


/*
	public IcyBufferedImage getImage() {
		return image;
	}
*/

/*
	public void setImage(IcyBufferedImage image) {
		this.image = image;
	}
*/

/*
	public void setIntensityMap(float[] intensityMap) {
		this.intensityHist = intensityMap;
	}
*/


/*
	public void setContrastMap(float[] contrastMap) {
		this.contrastHist = contrastMap;
	}
*/

	/*
	public int getMaxIntensity() {
		return maxIntensity;
	}


	public void setMaxIntensity(int maxIntensity) {
		this.maxIntensity = maxIntensity;
	}


	public int getMaxDistance() {
		return maxDistance;
	}


	public void setMaxDistance(int maxDistance) {
		this.maxDistance = maxDistance;
	}


	public int getMaxLength() {
		return maxLength;
	}


	public void setMaxLength(int maxLength) {
		this.maxLength = maxLength;
	}


	public int getHeight() {
		return height;
	}


	public void setHeight(int height) {
		this.height = height;
	}


	public int getWidth() {
		return width;
	}


	public void setWidth(int width) {
		this.width = width;
	}
	*/




}
