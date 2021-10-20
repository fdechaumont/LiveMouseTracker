package plugins.fab.livemousetracker;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;

import icy.canvas.Canvas2D;
import icy.util.GraphicsUtil;

public class DrawUtil {



	public static void drawCenteredString(Graphics2D g, String text, int x , int y ) {
		Font font = g.getFont();
		// 		Get the FontMetrics
	    FontMetrics metrics = g.getFontMetrics(font);
	    // Determine the X coordinate for the text
	    x = x - (metrics.stringWidth(text)) / 2;
	    // Determine the Y coordinate for the text (note we add the ascent, as in java 2d 0 is top of the screen)
	    y = y - (metrics.getHeight() / 2) + metrics.getAscent();
	    // Draw the String
	    g.drawString(text, x, y);
	}


	public static void drawCenteredHint( Graphics2D g, String text, int x, int y, Color bgColor, Color textColor )
	{
		Rectangle2D textRect = GraphicsUtil.getStringBounds(g, text);
		x = (int)(x - textRect.getWidth()/2);
		drawHint( g, text, x, y, bgColor, textColor );
	}

	public static void drawCenteredHint( Graphics2D g, String text, int x, int y, Color bgColor1 , Color bgColor2 , Color textColor )
	{
		Rectangle2D textRect = GraphicsUtil.getStringBounds(g, text);
		x = (int)(x - textRect.getWidth()/2);
		drawHint( g, text, x, y, bgColor1 , bgColor2 , textColor );
	}

	public static void drawHint(Graphics2D g, String text, int x, int y, Color bgColor, Color textColor) {
		ArrayList<Color> bgColorList = new ArrayList<Color>();
		bgColorList.add( bgColor );
		drawHint( g,  text,  x,  y, bgColorList, textColor);
	}

	public static void drawHint( Graphics2D g, String text, int x, int y, Color bgColorLeft, Color bgColorRight, Color textColor )
	{
		ArrayList<Color> bgColorList = new ArrayList<>();
		bgColorList.add( bgColorLeft );
		bgColorList.add( bgColorRight );
		drawHint(g, text, x, y, bgColorList, textColor);
	}

	public static void drawHint( Graphics2D g, String text, int x, int y, ArrayList<Color> bgColorList, Color textColor )
	{
		final Graphics2D g2 = (Graphics2D) g.create();

		final Rectangle2D stringRect = GraphicsUtil.getStringBounds(g, text);
		// calculate hint rect
		final RoundRectangle2D backgroundRect = new RoundRectangle2D.Double(x, y, (int) (stringRect.getWidth() + 10),
				(int) (stringRect.getHeight() + 8), 8, 8);

//		final RoundRectangle2D backgroundRectLeft = new RoundRectangle2D.Double(x, y, (int) (stringRect.getWidth() /2 ),
//				(int) (stringRect.getHeight() + 8), 8, 8);

		g2.setStroke(new BasicStroke(1.2f));

		if ( bgColorList.size() == 0 )
		{
			g2.setColor( Color.white );
			GraphicsUtil.mixAlpha(g2, AlphaComposite.SRC_OVER, 0.9f);
			g2.fill( backgroundRect );
		}

		int shift = 0;
		for ( int i = 0 ; i< bgColorList.size() ; i++ )
		{
			g2.setColor( bgColorList.get( i ) );
			GraphicsUtil.mixAlpha(g2, AlphaComposite.SRC_OVER, 0.9f);

			final RoundRectangle2D currentRect = new RoundRectangle2D.Double(
					x+shift,
					y,
					(int) ( backgroundRect.getWidth() / bgColorList.size() ) +1,
					(int) (stringRect.getHeight() + 8),

					8, 8);

			shift+= backgroundRect.getWidth()/bgColorList.size();

			g2.fill( currentRect);
		}

		/*
		Color strokeColor = Color.black;
		if ( bgColorList.size() > 0 )
		{
			strokeColor = bgColorList.get( 0 );
		}
*/
		// draw background stroke
		g2.setColor( Color.black );
		GraphicsUtil.mixAlpha(g2, AlphaComposite.SRC_OVER, 2f / 1f);
		g2.draw(backgroundRect);

		// draw text
		g2.setColor(textColor);
		GraphicsUtil.drawString(g2, text, x + 5, y + 4, false);

		g2.dispose();
	}
}
