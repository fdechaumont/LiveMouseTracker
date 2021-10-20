/**
  	@author Fabrice de Chaumont
 	copyright Fabrice de Chaumont @ Institut Pasteur

 	This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package plugins.fab.livemousetracker.trackCorrector;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import icy.file.FileUtil;
import icy.gui.frame.progress.ProgressFrame;
import icy.gui.viewer.Viewer;
import icy.gui.viewer.ViewerEvent;
import icy.gui.viewer.ViewerListener;
import icy.image.ImageUtil;
import icy.plugin.abstract_.PluginActionable;
import icy.sequence.Sequence;
import icy.system.thread.ThreadUtil;
import plugins.fab.livemousetracker.Util;
import plugins.fab.livemousetracker.experiment.Experiment;
import plugins.fab.livemousetracker.track.TrackSegment;

public class TrackCorrector extends PluginActionable implements ViewerListener , ActionListener , ChangeListener {

	Sequence sequence = new Sequence("No experiment loaded");
	Viewer viewer = new Viewer(sequence );

	Experiment experiment;

	TrackCorrectorPanel panelCommand = new TrackCorrectorPanel();
	SliderPanel sliderCommand = new SliderPanel();
	JSlider slider = sliderCommand.getTimeSlider();

	TrackCorrectorOverlay trackCorrectorOverlay = null;
	static int NB_FRAME_TO_DISPLAY = 256; // about 8 seconds

	public void setExperiment(Experiment experiment) {
		this.experiment = experiment;
		slider.setMinimum( 0 );
		slider.setMaximum( experiment.getNumberOfFrame() );
		slider.setValue( 0 );
		sequence.removeOverlay( trackCorrectorOverlay );
		trackCorrectorOverlay = new TrackCorrectorOverlay( experiment , this );
		sequence.addOverlay( trackCorrectorOverlay );
		sequence.setName( experiment.getBaseFolder() );
	}

	private void refreshViewer() {
		if ( experiment == null ) return;

		BufferedImage image = ImageUtil.load(
				experiment.getInfraRawImageFile( slider.getValue() ) );
		sequence.getFirstViewer().getLut().getLutChannel(0).setMinMax( 0, 32000 );
		setT( slider.getValue() );
		sequence.setImage( 0, 0, image);

	}

	int t = 0;
	private String loadLastPath;

	public void setT( int t )
	{
		this.t = t;
		if ( trackCorrectorOverlay != null )
		{
			trackCorrectorOverlay.setT( t );
		}
	}

	@Override
	public void run() {

		viewer.getContentPane().add( panelCommand , BorderLayout.EAST );
		viewer.getContentPane().add( sliderCommand , BorderLayout.SOUTH );

		slider.addChangeListener( this );

		viewer.getContentPane().remove( 0 );
		viewer.addListener( this );

		panelCommand.getLoadButton().addActionListener( this );
		panelCommand.getNextAnonymousButton().addActionListener( this );
		panelCommand.getPreviousAnonymousButton().addActionListener( this );
		panelCommand.getLoadLastButton().addActionListener( this );

		setupLoadLast();

		refreshCommandButton();
	}

	private void setupLoadLast() {

		loadLastPath = getPreferences("lastLoad").get( "path", null );
		if ( loadLastPath == null )
		{
			panelCommand.getLoadLastButton().setText( "Load Last" );
			panelCommand.getLoadLastButton().setEnabled( false );
		}else
		{
			panelCommand.getLoadLastButton().setText( "Load last ("
			+loadLastPath.substring( Math.max(0, loadLastPath.length() - 20) )
			+")" );
			panelCommand.getLoadLastButton().setEnabled( true );
		}

	}

	private void refreshCommandButton() {

		panelCommand.getFrameLabel().setText("Frame: " + t );
		panelCommand.getTimeLabel().setText( Util.getTimeStamp( t ) );

		if ( experiment !=null )
		{
			panelCommand.getNextAnonymousButton().setEnabled( experiment.getNextAnonymousTrack( t ) != null );
			panelCommand.getPreviousAnonymousButton().setEnabled( experiment.getPreviousAnonymousTrack( t ) != null  );

			String infoText = "<html><font color=black>Anonymous tracks: ";
			int nbAnonymous = experiment.getAnonymousTrackList().size();
			if ( nbAnonymous == 0 )
			{
				infoText+="<font color=green>";
			}
			else
			{
				infoText+="<font color=red>";
			}
			infoText+= nbAnonymous;
			infoText +="</html>";
			panelCommand.getInfoLabel().setText( infoText );
		}
		else
		{
			panelCommand.getNextAnonymousButton().setEnabled( false );
			panelCommand.getPreviousAnonymousButton().setEnabled( false );

			panelCommand.getInfoLabel().setText( "" );
		}


	}

	@Override
	public void viewerChanged(ViewerEvent event) {

	}

	@Override
	public void viewerClosed(Viewer viewer) {

	}

	@Override
	public void actionPerformed(ActionEvent e) {

		if ( e.getSource() == panelCommand.getPreviousAnonymousButton() )
		{
			if ( experiment == null ) return;
			TrackSegment previousTrack = experiment.getPreviousAnonymousTrack( t );

			if ( previousTrack != null )
			{
				slider.setValue( previousTrack.getFirstTimePoint() );
			}
		}

		if ( e.getSource() == panelCommand.getNextAnonymousButton() )
		{
			if ( experiment == null ) return;

			TrackSegment nextTrack = experiment.getNextAnonymousTrack( t );

			if ( nextTrack != null )
			{
				slider.setValue( nextTrack.getFirstTimePoint() );
			}
		}

		if ( e.getSource() == panelCommand.getLoadLastButton() )
		{
			if ( loadLastPath != null )
			{
				loadExperiment( loadLastPath );
			}
		}

		if ( e.getSource() == panelCommand.getLoadButton() )
		{
			 JFileChooser chooser = new JFileChooser();
			 chooser.setCurrentDirectory(new java.io.File("."));
			 chooser.setDialogTitle("Choose experiment folder");
			 chooser.setCurrentDirectory( new File ("c:/live mouse tracker data/") );
			 chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			 chooser.setAcceptAllFileFilterUsed(false);

			 if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
				 System.out.println("current file: " + chooser.getSelectedFile() );

				 String fullName = FileUtil.getDirectory( chooser.getSelectedFile().getAbsolutePath()+"/" );

				 getPreferences("lastLoad").put( "path" , fullName );
				 loadLastPath = fullName;

				 loadExperiment( fullName );
			 }
		}
		refreshCommandButton();
	}

	private void loadExperiment(String fullName) {

		 final Experiment experiment =
				 new Experiment( fullName );

		 ThreadUtil.bgRun( new Runnable() {
			@Override
			public void run() {
				ProgressFrame pf = new ProgressFrame("Loading experiment");
				experiment.load( pf );
				setExperiment( experiment );
				pf.close();
			}
		});


	}

	@Override
	public void stateChanged(ChangeEvent e) {

		if ( e.getSource() == slider )
		{
			refreshViewer();
			refreshCommandButton();
		}

	}




}
