package plugins.fab.aaa.voc.family;


import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import icy.gui.frame.IcyFrame;
import icy.main.Icy;
import icy.plugin.abstract_.PluginActionable;
import icy.preferences.XMLPreferences;
import icy.sequence.Sequence;
import plugins.fab.aaa.voc.AudioFFTProcessing;
import plugins.fab.aaa.voc.AudioFile2;
import plugins.fab.aaa.voc.AudioSpectrumViewer;
import plugins.fab.aaa.voc.FullVocProcessor;
import plugins.fab.aaa.voc.Point;
import plugins.fab.aaa.voc.USVProcessingException;
import plugins.fab.aaa.voc.Voc;

/** Creates family of vocalization
 * Receives vocs and provide families
 *
 * this does not work with burst, but with intern voc.
 * TODO: manage burst
 * */
public class FamilyMaker extends PluginActionable implements ActionListener {


	FamilyVocPanel fvp = new FamilyVocPanel();

	@Override
	public void run() {

		IcyFrame frame = new IcyFrame("Family voc maker" , true );
		frame.setPreferredSize( new Dimension( 1000 ,1000 ));

		frame.getContentPane().add( fvp );


		fvp.resultPanel().setLayout( new FlowLayout() );


		fvp.loadVocButton().addActionListener( this );

		frame.pack();
		frame.setVisible( true );
		frame.addToDesktopPane();
		frame.center();


	}

	void showVoc( Voc voc ,  AudioFFTProcessing fftProcessing, String name )
	{
		AudioSpectrumViewer asv = new AudioSpectrumViewer();
		double[][][] vocMagnitude = fftProcessing.getMagnitudeDenoisedCropped( voc.getStartX() , voc.getEndX() );
		Sequence sequence = asv.showSequence( vocMagnitude , null );
		sequence.setName( name );
	}

	void showVocPair( Voc voc ,  AudioFFTProcessing fftProcessing, String name )
	{
		/*
		AudioSpectrumViewer asv = new AudioSpectrumViewer();
		double[][][] vocMagnitude = fftProcessing.getMagnitudeDenoisedCropped( voc.getStartX() , voc.getEndX() );
		Sequence sequence = asv.showSequence( vocMagnitude  );
		sequence.setName( name );
		*/

	}

	public void process(AudioFile2 audioFile, ArrayList<Voc> vocList, AudioFFTProcessing fftProcessing) {


		// show all voc in different sequences.
		/*
		for ( Voc voc : vocList )
		{
			if ( voc.getDurationInMs() < 10 ) continue;
			showVoc(voc, fftProcessing, "name");
		}*/


//		showVocInPanel( fftProcessing.getMagnitudeForAllChannels(), "name");

		for ( Voc voc : vocList )
		{
			if ( voc.getDurationInMs() < 10 ) continue;
			double[][][] vocMagnitude = fftProcessing.getMagnitudeCropped( voc.getStartX() , voc.getEndX() );
			showVocInPanel( vocMagnitude, "name");
		}


/*
		int nameIndex = 0;
		Voc bestVocMatch = null;
		double bestScore = 0;

		for ( Voc vocA : vocList )
		{
			if ( vocA.getDurationInMs() < 10 ) continue;


			for ( Voc vocB : vocList )
			{
				if ( vocA == vocB ) continue;

				if ( canCompareVoc( vocA, vocB ) )
				{
					System.out.println("-----------------------------------");
					System.out.println( "Comparing " + vocA + "\t" + vocB );


					double score = scoreVocAffinity( vocA, vocB );
					System.out.println("Score: " + score );

					if ( score > bestScore )
					{
						bestVocMatch = vocB;
						bestScore = score;
					}

					/*
					if ( score > 0.7 && score < 0.8 )
					{
						String name = ""+nameIndex;
						showVoc( vocA , fftProcessing , name+"-A-"+(int)(score*100) );
						showVoc( vocB , fftProcessing , name+"-B-"+(int)(score*100) );

						//showVocPair( vocA, vocB , fftProcessing , name+"-"+(int)(score*100) );
						nameIndex++;
					}

				}

			}


			if ( bestScore > 0.8 )
			{
				String name = ""+nameIndex;
				nameIndex++;
				showVoc( vocA , fftProcessing , name+"-A-"+(int)(bestScore*100) );
				showVoc( bestVocMatch , fftProcessing , name+"-B-"+(int)(bestScore*100) );
			}


		}
*/

	}



	private void showVocInPanel(double[][][] vocMagnitude, String string) {

		fvp.resultPanel().add( new VocPanel( vocMagnitude ) );

	}

	public static double correlation( double[] xs, double[] ys) {
		//TODO: check here that arrays are not null, of the same length etc

		double sx = 0.0;
		double sy = 0.0;
		double sxx = 0.0;
		double syy = 0.0;
		double sxy = 0.0;

		int n = xs.length;

		for(int i = 0; i < n; ++i) {
			double x = xs[i];
			double y = ys[i];

			sx += x;
			sy += y;
			sxx += x * x;
			syy += y * y;
			sxy += x * y;
		}

		// covariation
		double cov = sxy / n - sx * sy / n / n;
		// standard error of x
		double sigmax = Math.sqrt(sxx / n -  sx * sx / n / n);
		// standard error of y
		double sigmay = Math.sqrt(syy / n -  sy * sy / n / n);

		// correlation is just a normalized covariation
		return cov / sigmax / sigmay;
	}


	private double scoreVocAffinity(Voc vocA, Voc vocB) {

		// TODO: move the minor voc in the major voc to find best fit.

		// return derivative of signal.

		Voc maxVoc = getVocOfMaxDuration( vocA , vocB );
		Voc minVoc = getVocOfMinDuration( vocA , vocB );

		int maxWidth = maxVoc.getLengthX();

		double [] maxVocSpectrumForm = new double[ maxWidth ];
		double [] minVocSpectrumForm = new double[ maxWidth ];

		for ( int x = 0 ; x < maxWidth ; x++ )
		{
			{
				Point p = maxVoc.getPointAt( x + maxVoc.getStartX() );
				if ( p != null )
				{
					maxVocSpectrumForm[x] = p.y;
				}
			}
			{
				Point p = minVoc.getPointAt( x + minVoc.getStartX() );
				if ( p != null )
				{
					minVocSpectrumForm[x] = p.y;
				}
			}
		}

		return correlation( maxVocSpectrumForm, minVocSpectrumForm );
	}

	private Voc getVocOfMinDuration(Voc vocA, Voc vocB) {
		if ( vocA.getDurationInMs() < vocB.getDurationInMs() )
		{
			return vocA;
		}
		return vocB;
	}

	private Voc getVocOfMaxDuration(Voc vocA, Voc vocB) {
		if ( vocA.getDurationInMs() < vocB.getDurationInMs() )
		{
			return vocB;
		}
		return vocA;
	}


	File[] getFiles( String title )
	{
		JFileChooser fileChooser = new JFileChooser();

		XMLPreferences preferences = this.getPreferences("voc folder");
		String browserDirectory = preferences.get("path "+title, "");
		if(browserDirectory != "")
		{
			fileChooser.setCurrentDirectory(new File(browserDirectory));
		}

		fileChooser.setDialogTitle( title );
		fileChooser.setMultiSelectionEnabled( true );
		FileNameExtensionFilter filter = new FileNameExtensionFilter( "Wav (vocalization) files", "wav" );
		fileChooser.setFileFilter(filter);

		int result = fileChooser.showOpenDialog( null );
		if (result == JFileChooser.APPROVE_OPTION)
		{
			preferences.put("path "+title, fileChooser.getCurrentDirectory().getPath() );
			return fileChooser.getSelectedFiles();
		}
		return null;
	}


	@Override
	public void actionPerformed(ActionEvent e) {

		if ( e.getSource() == fvp.loadVocButton() )
		{
			File[] files = getFiles( "Load wav files.");

			for( File file : files )
			{
				load( file );
			}

		}

	}

	private void load(File file) {

		FullVocProcessor processor = new FullVocProcessor( false , null );
		try {
			processor.process( file );
		} catch (USVProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		process( processor.getAudioFile() , processor.getAudioVocDetection().getVocList(), processor.getFftProcessing() );

	}




}
