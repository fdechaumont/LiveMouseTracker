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
package plugins.fab.livemousetracker.liveanalysis.chronogram;

import plugins.fab.livemousetracker.Animal;
import plugins.fab.livemousetracker.detection.MouseDetection;


public class ChronoMouseSpeed extends ChronoFeatureAbstract implements ProcessAnimalSingleFeature {

	public ChronoMouseSpeed() {
		super ( "Speed" , "Animal's speed in pixel per second" , TimeLineDataType.ANALOGIC );
	}

	public void process( Animal animal , Integer frameStart, Integer frameEnd )
	{
		System.out.println("processing speed...");
//		description = "Speed of " + animal.getName();
		eventTimeLine = new EventTimeLine( this.getName() , timeLineDataType );
		int tMax = animal.getLastTimePoint();

		if( frameEnd == null ) frameEnd = tMax;
		if( frameStart == null ) frameStart = 0;

		for ( int t = frameStart ; t < frameEnd ; t++ )
		{
			MouseDetection detectionAprev = animal.getDetectionAt( t - ChronoConstant.FRAME_WINDOW );
			MouseDetection detectionAnext = animal.getDetectionAt( t + ChronoConstant.FRAME_WINDOW );
			MouseDetection detectionAcurrent = animal.getDetectionAt( t );

			if( detectionAprev !=null && detectionAnext != null && detectionAcurrent != null  )
			{
				float distance = (float)detectionAprev.getMassCenter().toPoint2D().distance( detectionAnext.getMassCenter().toPoint2D() );
				float speed = distance / (float)( ChronoConstant.FRAME_WINDOW *2 +1 );
				eventTimeLine.addValue( t , speed );
			}

		}
	}

}
