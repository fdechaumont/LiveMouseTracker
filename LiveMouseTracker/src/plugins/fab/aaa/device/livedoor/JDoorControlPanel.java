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
package plugins.fab.aaa.device.livedoor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.DecimalFormat;

import javax.swing.JPanel;

import icy.system.profile.Chronometer;
import icy.util.GraphicsUtil;

public class JDoorControlPanel extends JPanel implements KeyListener, MouseListener {

	private static final long serialVersionUID = 5311306860377731995L;
	Door door;
	Font bigFont = new Font("Arial", Font.BOLD, 20 );
	Font smallFont = new Font("Arial", Font.BOLD, 15 );
	BasicStroke bigStroke = new BasicStroke( 3 );
	BasicStroke smallStroke = new BasicStroke( 1 );

	public JDoorControlPanel(Door door) {
		setPreferredSize( new Dimension( 200 , 200 ) );
		this.door = door;
		setFocusable( true );
		addKeyListener( this );
		addMouseListener(this);
	}


	@Override
	protected void paintComponent(Graphics g2) {

		Graphics2D g = (Graphics2D) g2;

		if ( hasFocus() )
		{
			g.setColor( Color.gray.darker() );
		}else
		{
			g.setColor( Color.BLACK );
		}
		g.fillRect( 0 , 0, getWidth(), getHeight() );
		g.setColor( Color.WHITE );

		g.setFont( bigFont );
		GraphicsUtil.drawCenteredString(g, door.getName(), 100, 15, false );
		g.setFont( smallFont );
		g.setColor( Color.yellow );
		GraphicsUtil.drawCenteredString(g, "id:"+door.getId(), 100, 30, false );

		// draw door
		//System.out.println( door.getDoorLockPercentage() );

		double percentLock = door.getCachedDoorLockPourcentage();
		int y= (int)( ( 1d - percentLock ) * 80d );
		g.setColor( Color.black );
		g.fillRect( 60 , 60, 80, 80 );
		g.setColor( Color.gray );
		g.setStroke( bigStroke );
		g.fillRect( 60 , 60+y, 80, 80-y );
		g.setColor( Color.white );
		g.drawRect( 60 , 60, 80, 80 );
		g.setStroke( smallStroke );

		DecimalFormat dc = new DecimalFormat("000");
		String percentString = dc.format( percentLock * 100 ) + " %";
		g.setColor( Color.green );
		g.setFont( bigFont );
		GraphicsUtil.drawCenteredString(g, percentString, 100, 80, false );
		g.setColor( Color.yellow );
		g.setFont( smallFont );
		GraphicsUtil.drawCenteredString(g, "Order: "+door.getOrder(), 100, 100, false );
		g.setFont( smallFont );
		GraphicsUtil.drawCenteredString(g, "Status: "+door.getStatus(), 100, 120, false );
		g.setFont( smallFont );
		GraphicsUtil.drawCenteredString(g, "Intern: "+door.getInternalOrder(), 100, 140, false );
		g.setFont( smallFont );
		GraphicsUtil.drawCenteredString(g, "Err: "+door.getErrorStatus(), 100, 160, false );


	}




	@Override
	public void keyPressed(KeyEvent e) {

		if ( e.getKeyChar() =='q' )
		{
			door.open();
			door.setSpeed(200);
			door.setLimitTorque(200);
		}
		if ( e.getKeyChar() =='a' )
		{
			door.close();
			door.setSpeed(200);
			door.setLimitTorque(200);
		}
		if ( e.getKeyChar() =='s' )
		{
			door.open();
			door.setSpeed(1000);
			door.setLimitTorque(1000);
		}
		if ( e.getKeyChar() =='z' )
		{
			door.close();
			door.setSpeed(1000);
			door.setLimitTorque(1000);
		}


	}


	@Override
	public void keyReleased(KeyEvent e) {

	}


	@Override
	public void keyTyped(KeyEvent e) {

	}


	@Override
	public void mouseClicked(MouseEvent e) {
		requestFocus();
		repaint();
	}


	@Override
	public void mousePressed(MouseEvent e) {

	}


	@Override
	public void mouseReleased(MouseEvent e) {

	}


	@Override
	public void mouseEntered(MouseEvent e) {

	}


	@Override
	public void mouseExited(MouseEvent e) {

	}



}
