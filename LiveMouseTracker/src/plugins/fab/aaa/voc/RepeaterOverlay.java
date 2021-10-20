package plugins.fab.aaa.voc;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

import icy.canvas.IcyCanvas;
import icy.painter.Overlay;
import icy.sequence.Sequence;

public class RepeaterOverlay extends Overlay {

	RepeaterDetection repeaterDetection ;
	public RepeaterOverlay( RepeaterDetection repeaterDetection ) {
		super("Repeater Overlay");
		this.repeaterDetection = repeaterDetection;
	}

	@Override
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas) {

		if ( repeaterDetection.repeatList.size() != 0 )
		{
			g.drawString("nbRepeat:"+repeaterDetection.repeatList.size(), 20, sequence.getHeight()-50);
		}

		g.setStroke( new BasicStroke( 5f ) );
		for ( Repeat repeat : repeaterDetection.repeatList )
		{
			g.setColor( repeat.color );
			g.drawRect( repeat.xOriginal, 10, repeat.repeatWindowWidth, sequence.getHeight() + 10 );
			g.drawRect( repeat.xRepeat, 10, repeat.repeatWindowWidth, sequence.getHeight() + 10 );
			g.setColor( Color.black );
			g.drawString( "repeat: "+(int)(repeat.correlation*100f)+"%" +
			"  offset:"+(repeat.xRepeat-repeat.xOriginal)+
			" w: " + repeat.repeatWindowWidth
			, repeat.xOriginal, 20 );
		}
		if ( repeaterDetection.repeatList.size() > 0 )
		{
			g.setColor( Color.red );
			g.fillOval(-80, -80, 160, 160 );
		}

	}

}
