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
package plugins.fab.livemousetracker.misc;

import plugins.fab.livemousetracker.LiveMouseTracker;

public class Clock {

	private int t = 0;

	public int getT() {
		return t;
	}

	public void increaseT() {
		t++;
	}

	public void increaseAndCorrectTWithDropFrame( int dropFrame ) {
		if ( dropFrame > 0 )
		{
			System.out.println("Increasing with drop.");
			System.out.println("Starting from t = " + t );
		}
		int previousValidT = t;
		for ( int i = 0 ; i< dropFrame ; i++)
		{
			t++;
			System.out.println("Copy at " + t );
			LiveMouseTracker.trackContainer.repeatLastDetection( previousValidT );
		}

		t++;

		if ( dropFrame > 0 )
		{
			System.out.println("New t is : " + t);
		}
	}
}
