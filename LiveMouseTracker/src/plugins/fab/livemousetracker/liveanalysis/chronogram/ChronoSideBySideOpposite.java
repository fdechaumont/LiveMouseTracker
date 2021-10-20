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


public class ChronoSideBySideOpposite extends ChronoFeatureAbstract implements ProcessAnimalPairFeature {

	public ChronoSideBySideOpposite() {
		super ( "Side by side Contact, opposite way" , "Animals are side by side, but opposite way" , TimeLineDataType.BOOLEAN );
	}

	public void process( Animal animalA , Animal animalB , Integer frameStart , Integer frameEnd )
	{
//		description = animalA.getName() + " and " + animalB.getName() + " are in opposite side by side.";


		eventTimeLine = new EventTimeLine( this.getName() , timeLineDataType );

		int tMax = Math.max( animalA.getLastTimePoint() , animalB.getLastTimePoint() );

		if( frameEnd == null ) frameEnd = tMax;
		if( frameStart == null ) frameStart = 0;

		for ( int t = frameStart ; t < frameEnd ; t++ )
		{
			MouseDetection detectionA = animalA.getDetectionAt( t );
			MouseDetection detectionB = animalB.getDetectionAt( t );

			if ( detectionA != null && detectionB != null )
			{
				try
				{
					double mouseABodyToMouseBBody = detectionA.getMassCenter().toPoint2D().distance( detectionB.getMassCenter().toPoint2D() );
					double mouseAHeadToMouseBHead = detectionA.getFrontPoint().toPoint2D().distance( detectionB.getFrontPoint().toPoint2D() );

					double vectMouseAX = detectionA.getFrontPoint().getX() - detectionA.getMassCenter().getX();
					double vectMouseAY = detectionA.getFrontPoint().getY() - detectionA.getMassCenter().getY();

					double vectMouseBX = detectionB.getFrontPoint().getX() - detectionB.getMassCenter().getX();
					double vectMouseBY = detectionB.getFrontPoint().getY() - detectionB.getMassCenter().getY();

					double scalarProduct = vectMouseAX * vectMouseBX + vectMouseAY * vectMouseBY ;

					if ( scalarProduct < 0 // Mice are in the opposite way
							&& mouseAHeadToMouseBHead < ChronoConstant.SIDE_BY_SIDE_THRESHOLD
							&& mouseABodyToMouseBBody < ChronoConstant.SIDE_BY_SIDE_THRESHOLD
							)
					{
						eventTimeLine.addPunctualEvent( t );
					}
				}catch( NullPointerException e )
				{
					// FIXME: add a can't build event.
					// System.err.println("side by side opposite: no front back in detection.");
				}

			}
		}
	}

}
