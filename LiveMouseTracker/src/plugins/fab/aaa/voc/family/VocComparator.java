package plugins.fab.aaa.voc.family;

import plugins.fab.aaa.voc.Voc;

public class VocComparator {

	static boolean canCompareVoc( Voc vocA, Voc vocB )
	{

		if ( ! aboutSameDuration( vocA, vocB ) ) return false;

		return true;
	}

	/** returns true if the max duration at +/-10% fit with min duration */
	static boolean aboutSameDuration(Voc vocA, Voc vocB) {

		float da = vocA.getDurationInMs();
		float db = vocB.getDurationInMs();

		float max = Math.max( da, db );
		float min = Math.min( da, db );
		float boundMin = max * 0.9f;
		float boundMax = max * 1.1f;

		if ( min > boundMin ) return true;

		return false;
	}


}
