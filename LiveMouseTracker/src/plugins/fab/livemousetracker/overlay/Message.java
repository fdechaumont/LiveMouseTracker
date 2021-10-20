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
package plugins.fab.livemousetracker.overlay;

import java.awt.Color;
import java.text.DecimalFormat;

import icy.system.profile.Chronometer;

public class Message {

	String text;
	Color color;
	long startTimeInNs;

	DecimalFormat df = new DecimalFormat();

	public Message( String text )
	{
		this( text, Color.green );
	}

	public Message( String text, Color color ) {

		this.text = text;
		this.color = color;

		startTimeInNs = System.nanoTime();
		df.setMaximumFractionDigits(1);

	}

	public String getTextWithTime()
	{
		long duration = System.nanoTime() - startTimeInNs;
		duration /= 1000000000f;

		return this.text + " / " + df.format( duration ) + " s";
	}

}
