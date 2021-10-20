package plugins.fab.livemousetracker;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import icy.canvas.IcyCanvas;
import icy.painter.Overlay;
import icy.sequence.Sequence;
import icy.util.GraphicsUtil;

public class MedallonOverlay extends Overlay {

	public MedallonOverlay() {

		super("Medallon overlay");
	}

	public static void drawCenteredHint( Graphics2D g, String text, int x, int y, Color bgColor, Color textColor )
	{
		Rectangle2D textRect = GraphicsUtil.getStringBounds(g, text);
		x = (int)(x - textRect.getWidth()/2);
		GraphicsUtil.drawHint( g, text, x, y, bgColor, textColor );
	}

	Font smallFont = new Font("Arial", Font.PLAIN , 8 );

	@Override
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas) {

		g.setFont( smallFont );
		drawCenteredHint(g,
				"t:"+LiveMouseTracker.getT() + " " + Util.getTimeStamp(LiveMouseTracker.getT()),
				100/2, 100-20, Color.black , Color.white);

	}

}
