package plugins.fab.aaa.voc;

public class Constant {

	public static final double MIN_STD_FOR_VERTICAL_DETECTION = 0.03f; // 0.05
	public static final double STD_MULTIPLICATOR_FOR_DETECTION = 0.5f; // 1.5f; 0.3
	public static final int MIN_Y_IN_SPECTRUM = 100;
//	public static final int MIN_Y_IN_SPECTRUM = 5;
	public static int MAX_Y_IN_SPECTRUM = 512-100;

	public static boolean NORMALIZE = false;

	static int WIDTH_IN_PIXEL_OF_NOISE_CANCELLING = 3;
	public static float MAX_GAP_DURATION_BETWEEN_SEQUENCE_TO_FUSE_VOC_IN_MS = 40f;

}
