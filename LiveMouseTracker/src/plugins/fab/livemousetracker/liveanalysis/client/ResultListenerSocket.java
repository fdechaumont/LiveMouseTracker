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
package plugins.fab.livemousetracker.liveanalysis.client;

import java.io.IOException;
import java.net.Socket;

import plugins.fab.livemousetracker.liveanalysis.DataResult;

public class ResultListenerSocket implements Runnable {


	Socket socket;
	public ResultListenerSocket( Socket socket ) {
		this.socket = socket;
//		System.out.println("Accepted Client Address - " + socket.getInetAddress().getHostName());

	}

	@Override
	public void run() {

		System.out.println("Processing result listener.");

		try {

//			while( socket.getInputStream().available() == 0 )
//			{
//				Thread.sleep(10);
//			}
			System.out.println( "Available: " + socket.getInputStream().available() );
			//while ( socket.is)

			DataResult dataResult = DataResult.createFromSocket( socket );
			System.out.println( "The request is: " + dataResult.getPList() );

//			DataAnswer dataAnswer = new DataAnswer();
//			TrackSegment ts = new TrackSegment();
//			for ( int t = 0 ; t < 50 ; t++ )
//			{
//				ts.addDetection( new RawMouseDetection( new ROI2DArea( new Point2D.Double( t , t ) ), t ));
//			}
//			dataAnswer.setTrackSegment( new TrackSegment() );
//			dataAnswer.setPoint( 5 );
//			dataAnswer.toSocket( socket );

			socket.close();


		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
