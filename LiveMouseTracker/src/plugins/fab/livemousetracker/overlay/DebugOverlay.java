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

import icy.canvas.IcyCanvas;
import icy.painter.Overlay;
import icy.sequence.Sequence;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.ArrayList;

/** @deprecated */
public class DebugOverlay extends Overlay {

	ArrayList<Message> messageList = new ArrayList<Message>();

	Font font = new Font("Arial" , Font.BOLD , 16 );

	public DebugOverlay(String name) {

		super(name);

	}

	synchronized public void clear()
	{
			messageList.clear();
			painterChanged();
	}

	synchronized public Message addMessage( String txt , Color color )
	{
		Message message = new Message(txt, color);
		messageList.add( message );
		painterChanged();
		return message;
	}

	@Override
	synchronized public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas) {

		if ( g==null ) return;

		int y = 15;

		for ( Message message : messageList )
		{
			g.setColor( message.color );
			g.drawString( message.text , 10, y );
			y+=15;
		}

	}


}
