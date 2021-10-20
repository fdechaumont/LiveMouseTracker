package plugins.fab.aaa.voc;

import java.awt.Color;

public class GroundTruthVoc {

	/* start in pixel */
	int startX;
	/* in ms second */
	float start;
	float end;
	Color color = Color.BLACK;
	boolean notFound = false;

	public GroundTruthVoc( int startX, float startMs, float endMs ) {
		this.start = startMs;
		this.end = endMs;
		this.startX = startX;
	}

	public float getDurationInMs() {

		return (end-start);
	}

}
