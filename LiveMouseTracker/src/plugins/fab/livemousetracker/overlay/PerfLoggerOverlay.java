package plugins.fab.livemousetracker.overlay;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.ArrayList;

import icy.canvas.IcyCanvas;
import icy.painter.Overlay;
import icy.sequence.Sequence;

public class PerfLoggerOverlay extends Overlay  {

	public PerfLoggerOverlay( ) {
		super("LMT perf log");
	}

	/** List of task to display for perf debug */
	static private ArrayList<Message> messageList = new ArrayList<Message>();

	public static Message addMessage( Message message )
	{
		synchronized ( messageList ) {
			messageList.add( message );
		}
		return message;
	}

	public static void removeMessage( Message message )
	{
		synchronized ( messageList ) {
			messageList.remove( message );
		}
	}

	Font font = new Font("Arial" , Font.BOLD , 16 );

	@Override
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas) {

		if ( g==null ) return;

		int y = 15;
		int nbThreads = java.lang.Thread.activeCount();
		g.setColor( Color.black );
		g.drawString( "Nb threads: "+nbThreads , 10, y );
		y+=15;

		synchronized ( messageList ) {

			for ( Message message : messageList )
			{
				g.setColor( message.color );
				g.drawString( message.text , 10, y );
				y+=15;
			}

		}

	}

}
