package plugins.fab.aaa.voc;

import java.awt.Color;

/**
 * Store a repeated voc
 * @author Fab
 *
 */
public class Repeat {

	double correlation ;
	int xOriginal;
	int xRepeat;
	Color color;
	int repeatWindowWidth;

	public Repeat(double correlation, int xOriginal, int xRepeat, int repeatWindowWidth ) {
		this.xOriginal = xOriginal;
		this.xRepeat = xRepeat;
		this.repeatWindowWidth = repeatWindowWidth;
		this.correlation = correlation;

		this.color = Color.getHSBColor( (float)Math.random() * 10f, 0.8f, 0.8f);
	}


}
