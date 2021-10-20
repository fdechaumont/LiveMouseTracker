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

import javax.swing.JPanel;
import java.awt.GridLayout;
import javax.swing.JLabel;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.SwingConstants;
import java.awt.Font;
import java.awt.Component;
import javax.swing.Box;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

public class TrackCorrectorPanel extends JPanel {
	public TrackCorrectorPanel() {
		setBorder(new EmptyBorder(3, 3, 3, 3));
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{200, 0};
		gridBagLayout.rowHeights = new int[]{50, 0, 0, 0, 0, 0, 0, 40, 0, 0, 50, 0};
		gridBagLayout.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		setLayout(gridBagLayout);

						JLabel lblTrackCorrector = new JLabel("Track corrector");
						lblTrackCorrector.setFont(new Font("Tahoma", Font.BOLD, 15));
						lblTrackCorrector.setHorizontalAlignment(SwingConstants.CENTER);
						GridBagConstraints gbc_lblTrackCorrector = new GridBagConstraints();
						gbc_lblTrackCorrector.fill = GridBagConstraints.HORIZONTAL;
						gbc_lblTrackCorrector.insets = new Insets(0, 0, 5, 0);
						gbc_lblTrackCorrector.gridx = 0;
						gbc_lblTrackCorrector.gridy = 0;
						add(lblTrackCorrector, gbc_lblTrackCorrector);

								JButton cutButton = new JButton("Cut");
								cutButton.setFocusable(false);
								cutButton.addActionListener(new ActionListener() {
									public void actionPerformed(ActionEvent e) {
									}
								});
								GridBagConstraints gbc_cutButton = new GridBagConstraints();
								gbc_cutButton.fill = GridBagConstraints.BOTH;
								gbc_cutButton.insets = new Insets(0, 0, 5, 0);
								gbc_cutButton.gridx = 0;
								gbc_cutButton.gridy = 1;
								add(cutButton, gbc_cutButton);

						JButton btnSplitDetection = new JButton("Split detection");
						btnSplitDetection.setFocusable(false);
						GridBagConstraints gbc_btnSplitDetection = new GridBagConstraints();
						gbc_btnSplitDetection.fill = GridBagConstraints.BOTH;
						gbc_btnSplitDetection.insets = new Insets(0, 0, 5, 0);
						gbc_btnSplitDetection.gridx = 0;
						gbc_btnSplitDetection.gridy = 2;
						add(btnSplitDetection, gbc_btnSplitDetection);

				JButton btnSetHeadDirection = new JButton("Set head direction");
				btnSetHeadDirection.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
					}
				});
				btnSetHeadDirection.setFocusable(false);
				GridBagConstraints gbc_btnSetHeadDirection = new GridBagConstraints();
				gbc_btnSetHeadDirection.fill = GridBagConstraints.BOTH;
				gbc_btnSetHeadDirection.insets = new Insets(0, 0, 5, 0);
				gbc_btnSetHeadDirection.gridx = 0;
				gbc_btnSetHeadDirection.gridy = 3;
				add(btnSetHeadDirection, gbc_btnSetHeadDirection);

						JButton btnDelete = new JButton("Delete");
						btnDelete.setFocusable(false);
						GridBagConstraints gbc_btnDelete = new GridBagConstraints();
						gbc_btnDelete.fill = GridBagConstraints.BOTH;
						gbc_btnDelete.insets = new Insets(0, 0, 5, 0);
						gbc_btnDelete.gridx = 0;
						gbc_btnDelete.gridy = 4;
						add(btnDelete, gbc_btnDelete);

								JPanel panel = new JPanel();
								panel.setBorder(new TitledBorder(null, "Anonymous Tracks", TitledBorder.LEADING, TitledBorder.TOP, null, null));
								GridBagConstraints gbc_panel = new GridBagConstraints();
								gbc_panel.insets = new Insets(0, 0, 5, 0);
								gbc_panel.fill = GridBagConstraints.BOTH;
								gbc_panel.gridx = 0;
								gbc_panel.gridy = 5;
								add(panel, gbc_panel);
								GridBagLayout gbl_panel = new GridBagLayout();
								gbl_panel.columnWidths = new int[]{0, 0, 0};
								gbl_panel.rowHeights = new int[]{0, 0};
								gbl_panel.columnWeights = new double[]{1.0, 1.0, Double.MIN_VALUE};
								gbl_panel.rowWeights = new double[]{0.0, Double.MIN_VALUE};
								panel.setLayout(gbl_panel);

								btnPrevious = new JButton("Previous");
								btnPrevious.setFocusable(false);
								GridBagConstraints gbc_btnPrevious = new GridBagConstraints();
								gbc_btnPrevious.fill = GridBagConstraints.BOTH;
								gbc_btnPrevious.insets = new Insets(0, 0, 0, 5);
								gbc_btnPrevious.gridx = 0;
								gbc_btnPrevious.gridy = 0;
								panel.add(btnPrevious, gbc_btnPrevious);

								btnNext = new JButton("Next");
								btnNext.setFocusable(false);
								GridBagConstraints gbc_btnNext = new GridBagConstraints();
								gbc_btnNext.fill = GridBagConstraints.BOTH;
								gbc_btnNext.gridx = 1;
								gbc_btnNext.gridy = 0;
								panel.add(btnNext, gbc_btnNext);

								lblInfos = new JLabel("Infos");
								lblInfos.setFont(new Font("Tahoma", Font.BOLD, 17));
								GridBagConstraints gbc_lblInfos = new GridBagConstraints();
								gbc_lblInfos.anchor = GridBagConstraints.NORTH;
								gbc_lblInfos.insets = new Insets(0, 0, 5, 0);
								gbc_lblInfos.gridx = 0;
								gbc_lblInfos.gridy = 6;
								add(lblInfos, gbc_lblInfos);

								lblTime = new JLabel("Time");
								lblTime.setFont(new Font("Tahoma", Font.BOLD, 20));
								GridBagConstraints gbc_lblTime = new GridBagConstraints();
								gbc_lblTime.anchor = GridBagConstraints.SOUTH;
								gbc_lblTime.insets = new Insets(0, 0, 5, 0);
								gbc_lblTime.gridx = 0;
								gbc_lblTime.gridy = 7;
								add(lblTime, gbc_lblTime);

																lblTimeInFrame = new JLabel("Time in frame");
																GridBagConstraints gbc_lblTimeInFrame = new GridBagConstraints();
																gbc_lblTimeInFrame.insets = new Insets(0, 0, 5, 0);
																gbc_lblTimeInFrame.gridx = 0;
																gbc_lblTimeInFrame.gridy = 8;
																add(lblTimeInFrame, gbc_lblTimeInFrame);

								btnLoadLast = new JButton("Load Last");
								btnLoadLast.setFocusable(false);
								GridBagConstraints gbc_btnLoadLast = new GridBagConstraints();
								gbc_btnLoadLast.fill = GridBagConstraints.BOTH;
								gbc_btnLoadLast.insets = new Insets(0, 0, 5, 0);
								gbc_btnLoadLast.gridx = 0;
								gbc_btnLoadLast.gridy = 9;
								add(btnLoadLast, gbc_btnLoadLast);

								btnLoad = new JButton("Load experiment");
								btnLoad.setFocusable(false);
								GridBagConstraints gbc_btnLoad = new GridBagConstraints();
								gbc_btnLoad.fill = GridBagConstraints.BOTH;
								gbc_btnLoad.gridx = 0;
								gbc_btnLoad.gridy = 10;
								add(btnLoad, gbc_btnLoad);
	}

	/**
	 *
	 */
	private static final long serialVersionUID = -1757851891068905882L;
	private JButton btnLoad;
	private JButton btnNext;
	private JButton btnPrevious;
	private JLabel lblTime;
	private JLabel lblTimeInFrame;
	private JButton btnLoadLast;
	private JLabel lblInfos;



	public JButton getLoadButton() {
		return btnLoad;
	}
	public JButton getNextAnonymousButton() {
		return btnNext;
	}
	public JButton getPreviousAnonymousButton() {
		return btnPrevious;
	}

	public JLabel getTimeLabel() {
		return lblTime;
	}
	public JLabel getFrameLabel() {
		return lblTimeInFrame;
	}
	public JButton getLoadLastButton() {
		return btnLoadLast;
	}
	public JLabel getInfoLabel() {
		return lblInfos;
	}
}
