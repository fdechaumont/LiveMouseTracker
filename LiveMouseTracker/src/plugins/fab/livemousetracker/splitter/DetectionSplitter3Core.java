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
package plugins.fab.livemousetracker.splitter;

import java.awt.geom.Point2D;
import java.util.ArrayList;

import icy.roi.BooleanMask2D;
import plugins.fab.livemousetracker.LiveMouseTracker;
import plugins.fab.livemousetracker.Util;
import plugins.fab.livemousetracker.splitter.DetectionSplitter3Optimized.SplitObject;
import plugins.kernel.roi.roi2d.ROI2DArea;
import plugins.kernel.roi.roi2d.ROI2DPoint;

public class DetectionSplitter3Core {

	private static final boolean CONSIDER_SPEED_TO_PREDICT_SPLIT_LOCATION = false;
	byte[] roiIndexMap;
	short[] zMap;
	int height ;
	int width ;
	int offsetX;
	int offsetY;

	public DetectionSplitter3Core(ROI2DArea tooBigROI , BooleanMask2D tooBigROIMask, ArrayList<SplitObject> detectionSplitList ) {

		height = tooBigROIMask.bounds.height;
		width = tooBigROIMask.bounds.width;
		offsetX = tooBigROIMask.bounds.x;
		offsetY = tooBigROIMask.bounds.y;

//		System.out.println( "[SPLITTER CORE 3]" + tooBigROIMask.bounds );

		// init roi index map.
		boolean[] tooBigMask = tooBigROIMask.mask;
		roiIndexMap = new byte[tooBigMask.length];

		// fill with -2 for not assigned and -1 for tooBigArea surface )

		int surfaceTooBigMask = 0;
		for ( int i = 0 ; i < roiIndexMap.length ; i++ )
		{
			if ( tooBigMask[i] )
			{
				roiIndexMap[i] = -1;
				surfaceTooBigMask++;
			}else
			{
				roiIndexMap[i] = -2;
			}
			//roiIndexMap[i] = (byte) (mask[i]?-1:-2);
		}

		// init with indexes of detectionSplitListCandidates
		for ( byte i = 0 ; i < detectionSplitList.size() ; i++ )
		{
			SplitObject split = detectionSplitList.get( i );
			boolean init = false;
			Point2D splitSpeedVector = split.getSpeedVector();
//			ROI2DLine line = new ROI2DLine(
//					split.detection.getMassCenter().toPoint2D().getX(),
//					split.detection.getMassCenter().toPoint2D().getY(),
//					split.detection.getMassCenter().toPoint2D().getX() + splitSpeedVector.getX()*10d,
//					split.detection.getMassCenter().toPoint2D().getY() + splitSpeedVector.getY()*10d
//					);
//			line.setName("tmp line");
//			line.setColor( Color.yellow );
//			LiveMouseTracker.addROIToInfraSequence( line );
			try
			{
				for ( Point2D point : split.detection.getSpinePointsForSplit() )
				{
					// ajouter les points
					int x = 0;
					int y = 0;

					if ( CONSIDER_SPEED_TO_PREDICT_SPLIT_LOCATION )
					{
						x += (int)(splitSpeedVector.getX());
						y += (int)(splitSpeedVector.getY());
					}

					x += (int)point.getX(); //-offsetX;
					y += (int)point.getY(); //-offsetY;
//					ROI2DPoint p = new ROI2DPoint( x, y);
//					p.setName("tmp p");
//					LiveMouseTracker.addROIToInfraSequence( p );

					x-= offsetX;
					y-= offsetY;
					int index = x + y * width;

					if ( index >= 0 && index < roiIndexMap.length ) // check if point is in the map.
						roiIndexMap[index] = i;
				}

				init = true;
			}
			catch ( NullPointerException e )
			{
				// FIXME: this should not append.
				System.err.println("Spine point for split: null (no front and back point)");
			}

			if ( !init )
			{
				Point2D point = split.detection.getMassCenter().toPoint2D();
				int x = (int)point.getX()-offsetX;
				int y = (int)point.getY()-offsetY;
				int index = x + y * width;

				if ( index >= 0 && index < roiIndexMap.length ) // check if point is in the map.
				{
					roiIndexMap[index] = i;
					init = true;
				}
			}
		}

		// computes target surfaces, which are size limits for each new detection
		// Takes the previous detection and find its proportion in the previous set of detection.
		int targetSurface[] = new int[ detectionSplitList.size() ];
		// save target surface to check for error at the end of the process.
		int targetSurfaceHold[] = new int[ detectionSplitList.size() ];

		int totalPreviousSurface = 0;
		{
			for ( int i = 0 ; i< detectionSplitList.size() ; i++ )
			{
				SplitObject so = detectionSplitList.get( i );
				int surface = (int)so.detection.getSurface();
				targetSurface[i] = surface;
				totalPreviousSurface+= surface;
			}
		}

		float targetSurfaceRatio[] = new float[ detectionSplitList.size() ];
//		System.out.println("[SPLIT] Previous ratios:");
		for ( int i = 0 ; i < targetSurfaceRatio.length ; i++ )
		{
			targetSurfaceRatio[i] = (float)targetSurface[i] / (float)totalPreviousSurface;
			targetSurfaceRatio[i] = 1f / targetSurfaceRatio.length; // Hook to give same proportion to all animals involved.
//			System.out.println("[SPLIT] "+i+" pre targetRatio: " + targetSurfaceRatio[i] );
//			targetSurfaceRatio[i] *= (float) surfaceTooBigMask;
//			System.out.println("[SPLIT] "+i+" targetSurface(pix): " + targetSurfaceRatio[i] );

			targetSurface[i] = (int)( targetSurfaceRatio[i] * (float)surfaceTooBigMask );
			targetSurfaceHold[i] = targetSurface[i];
//			System.out.println("[SPLIT] "+i+" targetSurface(pix): " + targetSurface[i] );
		}
//		System.out.println("[SPLIT] Total target surface: " + surfaceTooBigMask );

//		init z map
		zMap = Util.getZAsBuffer( tooBigROIMask.bounds );

		int maxZ = 0;
		for ( int i = 0 ; i < zMap.length ; i++ )
		{
			if ( zMap[i]> maxZ ) maxZ = zMap[i];
		}

		if ( maxZ > 1000 )
		{
//			System.err.println("[SPLIT] MAX Z FAR TOO HIGH");
			maxZ = 1000;
		}

//		displayZMap();

//		displayMap();

		byte roiIndexMapNew[] = new byte[ roiIndexMap.length ];
		System.arraycopy( roiIndexMap, 0, roiIndexMapNew, 0, roiIndexMap.length );

		for ( int z = maxZ ; z > 1 ; z-=2 ) // fill respecting z
		{
			processOnce( z, roiIndexMapNew, targetSurfaceHold, true );
		}

		for ( int i = 0 ; i < 40 ; i++ ) // fill what's missing.
		{
			processOnce( 0, roiIndexMapNew, targetSurfaceHold, false );
		}

//		for ( int z = maxZ ; z > 1 ; z-=2 )
//		{
//
//			System.out.println( "Z = " + z );

			/*
			int index = 0;
			for ( int y = 0 ; y < height ; y++ )
			{
				for ( int x = 0 ; x < width ; x++, index++ )
				{
					byte val = roiIndexMap[index];

//					if ( val == -2 )
//					{
//						roiIndexMapNew[index] = val;
//						continue;
//					}

//					roiIndexMapNew[index] = -1;

					if ( val >= 0 ) // this is an area to fill
					{
						// check around for a -1 value to fill;

						// copy original value at index in new map.
						roiIndexMapNew[ index ] = val;

//						boolean checkSurface = true;
//						if ( z <= 30 ) checkSurface = false;

						// don't dilate area with too low z
						if ( zMap[index] < z ) continue;

						// top left
						int indexToCheck = index - width - 1;
						if ( y > 0 && x > 0 )
						{
							if ( roiIndexMap[ indexToCheck ] == -1 && ( targetSurface[val] > 0 || !checkSurface ) ) // this || remove the surface test if checkSurface==false
							{
								if ( roiIndexMapNew[ indexToCheck ] != val )
								{
									roiIndexMapNew[ indexToCheck ] = val;
									targetSurface[val]--;
								}
							}
						}

						// top
						indexToCheck++;
						if ( y > 0 )
						{
							if ( roiIndexMap[ indexToCheck ] == -1 && ( targetSurface[val] > 0 || !checkSurface ) )
							{
								if ( roiIndexMapNew[ indexToCheck ] != val )
								{
									roiIndexMapNew[ indexToCheck ] = val;
									targetSurface[val]--;
								}
							}
						}

						// top right
						indexToCheck++;
						if ( y > 0 && x < width-1 )
						{
							if ( roiIndexMap[ indexToCheck ] == -1 && ( targetSurface[val] > 0 || !checkSurface ) )
							{
								if ( roiIndexMapNew[ indexToCheck ] != val )
								{
									roiIndexMapNew[ indexToCheck ] = val;
									targetSurface[val]--;
								}
							}
						}

						// left
						indexToCheck =index - 1;
						if ( x > 0 )
						{
							if ( roiIndexMap[ indexToCheck ] == -1 && ( targetSurface[val] > 0 || !checkSurface ) )
							{
								if ( roiIndexMapNew[ indexToCheck ] != val )
								{
									roiIndexMapNew[ indexToCheck ] = val;
									targetSurface[val]--;
								}
							}
						}

						// right
						indexToCheck+=2;
						if ( x < width-1 )
						{
							if ( roiIndexMap[ indexToCheck ] == -1 && ( targetSurface[val] > 0 || !checkSurface ) )
							{
								if ( roiIndexMapNew[ indexToCheck ] != val )
								{
									roiIndexMapNew[ indexToCheck ] = val;
									targetSurface[val]--;
								}
							}
						}

						// bottom left
						indexToCheck=index + width - 1;
						if ( y < height-1 && x > 0 )
						{
							if ( roiIndexMap[ indexToCheck ] == -1 && ( targetSurface[val] > 0 || !checkSurface ) )
							{
								if ( roiIndexMapNew[ indexToCheck ] != val )
								{
									roiIndexMapNew[ indexToCheck ] = val;
									targetSurface[val]--;
								}
							}
						}

						// bottom
						indexToCheck++;
						if ( y < height-1 )
						{
							if ( roiIndexMap[ indexToCheck ] == -1 && ( targetSurface[val] > 0 || !checkSurface ) )
							{
								if ( roiIndexMapNew[ indexToCheck ] != val )
								{
									roiIndexMapNew[ indexToCheck ] = val;
									targetSurface[val]--;
								}
							}
						}

						// bottom right
						indexToCheck++;
						if ( y < height-1 && x < width-1 )
						{
							if ( roiIndexMap[ indexToCheck ] == -1 && ( targetSurface[val] > 0 || !checkSurface ) )
							{
								if ( roiIndexMapNew[ indexToCheck ] != val )
								{
									roiIndexMapNew[ indexToCheck ] = val;
									targetSurface[val]--;
								}
							}
						}
					}
				}
			}
			// copy back to roiIndexMap.
			//System.arraycopy( roiIndexMapNew, 0, roiIndexMap, 0, roiIndexMapNew.length );
			byte[] tmp = roiIndexMap;
			roiIndexMap = roiIndexMapNew;
			roiIndexMapNew = tmp;

//			for ( int i = 0 ; i < targetSurface.length ; i++ )
//			{
//				System.out.println("Target surface["+i+"] " + targetSurface[i] );
//			}
//			displayMap();*/
//		}

		// rebuild ROI2DArea in split objects.
		ROI2DArea[] rois = new ROI2DArea[detectionSplitList.size()];
		try
		{
			for ( int i = 0 ; i < rois.length; i++ )
			{
				// create ROI and set ip in update mode
				rois[i] = new ROI2DArea();
				rois[i].beginUpdate();
				detectionSplitList.get( i ).newROI2DArea = rois[i];
			}

			int index = 0;
			for ( int y = 0 ; y < height ; y++ )
			{
				for ( int x = 0 ; x < width ; x++)
				{
					int val = roiIndexMap[index++];

					if ( val >= 0 )
						rois[val].addPoint( x + offsetX , y + offsetY );
				}
			}
		}
		finally
		{
			// done
			for ( int i = 0 ; i < rois.length; i++ )
			{
				rois[i].endUpdate();
//				System.out.println("[SPLIT] Final surface["+i+"] :" + rois[i].getNumberOfPoints()
//						+ " expected: " + targetSurfaceHold[i]
//						);
			}
		}
	}

	private void processOnce(int z, byte[] roiIndexMapNew, int[] targetSurface, boolean checkSurface) {


		int index = 0;
		for ( int y = 0 ; y < height ; y++ )
		{
			for ( int x = 0 ; x < width ; x++, index++ )
			{
				byte val = roiIndexMap[index];

//				if ( val == -2 )
//				{
//					roiIndexMapNew[index] = val;
//					continue;
//				}

//				roiIndexMapNew[index] = -1;

				if ( val >= 0 ) // this is an area to fill
				{
					// check around for a -1 value to fill;

					// copy original value at index in new map.
					roiIndexMapNew[ index ] = val;

//					boolean checkSurface = true;
//					if ( z <= 30 ) checkSurface = false;

					// don't dilate area with too low z
					if ( zMap[index] < z ) continue;

					// top left
					int indexToCheck = index - width - 1;
					if ( y > 0 && x > 0 )
					{
						if ( roiIndexMap[ indexToCheck ] == -1 && ( targetSurface[val] > 0 || !checkSurface ) ) // this || remove the surface test if checkSurface==false
						{
							if ( roiIndexMapNew[ indexToCheck ] != val )
							{
								roiIndexMapNew[ indexToCheck ] = val;
								targetSurface[val]--;
							}
						}
					}

					// top
					indexToCheck++;
					if ( y > 0 )
					{
						if ( roiIndexMap[ indexToCheck ] == -1 && ( targetSurface[val] > 0 || !checkSurface ) )
						{
							if ( roiIndexMapNew[ indexToCheck ] != val )
							{
								roiIndexMapNew[ indexToCheck ] = val;
								targetSurface[val]--;
							}
						}
					}

					// top right
					indexToCheck++;
					if ( y > 0 && x < width-1 )
					{
						if ( roiIndexMap[ indexToCheck ] == -1 && ( targetSurface[val] > 0 || !checkSurface ) )
						{
							if ( roiIndexMapNew[ indexToCheck ] != val )
							{
								roiIndexMapNew[ indexToCheck ] = val;
								targetSurface[val]--;
							}
						}
					}

					// left
					indexToCheck =index - 1;
					if ( x > 0 )
					{
						if ( roiIndexMap[ indexToCheck ] == -1 && ( targetSurface[val] > 0 || !checkSurface ) )
						{
							if ( roiIndexMapNew[ indexToCheck ] != val )
							{
								roiIndexMapNew[ indexToCheck ] = val;
								targetSurface[val]--;
							}
						}
					}

					// right
					indexToCheck+=2;
					if ( x < width-1 )
					{
						if ( roiIndexMap[ indexToCheck ] == -1 && ( targetSurface[val] > 0 || !checkSurface ) )
						{
							if ( roiIndexMapNew[ indexToCheck ] != val )
							{
								roiIndexMapNew[ indexToCheck ] = val;
								targetSurface[val]--;
							}
						}
					}

					// bottom left
					indexToCheck=index + width - 1;
					if ( y < height-1 && x > 0 )
					{
						if ( roiIndexMap[ indexToCheck ] == -1 && ( targetSurface[val] > 0 || !checkSurface ) )
						{
							if ( roiIndexMapNew[ indexToCheck ] != val )
							{
								roiIndexMapNew[ indexToCheck ] = val;
								targetSurface[val]--;
							}
						}
					}

					// bottom
					indexToCheck++;
					if ( y < height-1 )
					{
						if ( roiIndexMap[ indexToCheck ] == -1 && ( targetSurface[val] > 0 || !checkSurface ) )
						{
							if ( roiIndexMapNew[ indexToCheck ] != val )
							{
								roiIndexMapNew[ indexToCheck ] = val;
								targetSurface[val]--;
							}
						}
					}

					// bottom right
					indexToCheck++;
					if ( y < height-1 && x < width-1 )
					{
						if ( roiIndexMap[ indexToCheck ] == -1 && ( targetSurface[val] > 0 || !checkSurface ) )
						{
							if ( roiIndexMapNew[ indexToCheck ] != val )
							{
								roiIndexMapNew[ indexToCheck ] = val;
								targetSurface[val]--;
							}
						}
					}
				}
			}
		}
		// copy back to roiIndexMap.
		//System.arraycopy( roiIndexMapNew, 0, roiIndexMap, 0, roiIndexMapNew.length );
		byte[] tmp = roiIndexMap;
		roiIndexMap = roiIndexMapNew;
		roiIndexMapNew = tmp;

//		for ( int i = 0 ; i < targetSurface.length ; i++ )
//		{
//			System.out.println("Target surface["+i+"] " + targetSurface[i] );
//		}
//		displayMap();


	}

	private void displayMap() {

		// display map.
		for ( int y = 0 ; y < height ; y++ )
		{
			System.out.println(" ");
			for ( int x = 0 ; x < width ; x++ )
			{
				byte val = roiIndexMap[ x + y * width ];

				String s = ""+val;
				if ( val == -2 ) s= ".";
				if ( val == -1 ) s= "X";

				System.out.print( s +" ");
			}
		}
		System.out.println(" ");
	}

	private void displayZMap() {

		// display map.
		for ( int y = 0 ; y < height ; y++ )
		{
			System.out.println(" ");
			for ( int x = 0 ; x < width ; x++ )
			{
				int val = (int)zMap[ x + y * width ];
				String s ="";
				if ( val < 10 ) s= "0";
				System.out.print( " " + s + val);
			}
		}
		System.out.println(" ");
	}


}
