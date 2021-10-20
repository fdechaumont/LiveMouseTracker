package plugins.fab.livemousetracker.device.thermalcamera;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import icy.canvas.Canvas2D;
import icy.canvas.IcyCanvas;
import icy.painter.Overlay;
import icy.sequence.Sequence;
import icy.util.GraphicsUtil;
import plugins.fab.livemousetracker.LiveMouseTracker;
import plugins.fab.livemousetracker.Util;

public class ThermalOverlay extends Overlay {

	public ThermalOverlay() {

		super("Thermal overlay");
	}

	public static void drawCenteredHint( Graphics2D g, String text, int x, int y, Color bgColor, Color textColor )
	{
		Rectangle2D textRect = GraphicsUtil.getStringBounds(g, text);
		x = (int)(x - textRect.getWidth()/2);
		GraphicsUtil.drawHint( g, text, x, y, bgColor, textColor );
	}

	Font smallFont = new Font("Arial", Font.PLAIN , 12 );

	@Override
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas) {

		g.setFont( smallFont );
		String txt = "t:"+LiveMouseTracker.getT() + " - " + Util.getTimeStamp(LiveMouseTracker.getT());
		drawCenteredHint(g, txt, 320/2, 210, Color.black , Color.white);

		// draw current rendering of the main LMT window

		// TODO: if it costs too much, get the last from the

		try{
			BufferedImage renderedImage = LiveMouseTracker.getInfraOut().getFirstViewer().getCanvas().getRenderedImage( 0 , 0 , -1 , false );
			//AffineTransform transform = new AffineTransform();
			g.drawImage( renderedImage, 320, 0, 320, 240, Color.black, null );
		}catch( Exception e )
		{

		}
//		g.drawImage( renderedImage, transform, x, y);


//		g.setColor( Color.white );
//		g.drawLine( 0 , 0, 100, 100);
	}

}
