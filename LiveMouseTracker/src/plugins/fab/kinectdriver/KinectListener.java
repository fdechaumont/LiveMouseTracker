package plugins.fab.kinectdriver;

import icy.sequence.Sequence;

public interface KinectListener {

	public void kinectChange( Sequence sourceSequence , KinectData kinectData, KinectEvent kinectEvent );
		
}
