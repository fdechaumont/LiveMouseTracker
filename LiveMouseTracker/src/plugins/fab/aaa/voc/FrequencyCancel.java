package plugins.fab.aaa.voc;

/** frequency to cancel */
public class FrequencyCancel {

	int startX;
	int endX;
	int y;
	double mean;
	double stdev;

	public FrequencyCancel( int startX ,int endX , int y, double mean, double stdev ) {

		this.startX = startX;
		this.endX = endX;
		this.y = y;
		this.mean = mean;
		this.stdev = stdev;
	}

	public boolean contain(int x, int y) {


		if ( y<=this.y+ Constant.WIDTH_IN_PIXEL_OF_NOISE_CANCELLING  && y>=this.y- Constant.WIDTH_IN_PIXEL_OF_NOISE_CANCELLING && x >= this.startX && x <=this.endX )
		{
			return true;
		}
		return false;
	}

}
