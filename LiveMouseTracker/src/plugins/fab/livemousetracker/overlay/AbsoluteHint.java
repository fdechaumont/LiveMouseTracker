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

public class AbsoluteHint {

	String text;
	float x;
	float y;
	Color textColor;
	Color backGroundColor;
	boolean centerx = false;

	public AbsoluteHint( String text , float x, float y , Color textColor , Color backGroundColor , boolean centeredAlongX ) {

		this.text = text;
		this.x = x;
		this.y = y;
		this.textColor = textColor;
		this.backGroundColor = backGroundColor;
		this.centerx = centeredAlongX;

	}

	public AbsoluteHint( String text , float x, float y , Color textColor , Color backGroundColor ) {
		this(text, x, y, textColor, backGroundColor, false );
	}


}
