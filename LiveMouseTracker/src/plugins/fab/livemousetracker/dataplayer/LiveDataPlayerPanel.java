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
package plugins.fab.livemousetracker.dataplayer;

import javax.swing.JPanel;
import java.awt.GridBagLayout;
import javax.swing.JButton;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.border.TitledBorder;
import java.awt.GridLayout;
import javax.swing.JTabbedPane;

public class LiveDataPlayerPanel extends JPanel {
	private JTextField frameField;
	private JButton btnSelectDatabase;
	private JTextField textFieldEventFilter;
	private JLabel lblFrameNumber;
	private JLabel lblEventFilter;
	private JButton btnPrevious;
	private JButton btnNext;
	private JTextField txtEvent1;
	private JLabel lblEventJ;
	private JLabel lblEventK;
	private JLabel lblEventL;
	private JLabel lblEventM;
	private JTextField txtEvent2;
	private JTextField txtEvent3;
	private JTextField txtEvent4;
	private JLabel lblEventB;
	private JLabel lblEventN;
	private JTextField txtEvent5;
	private JTextField txtEvent6;
	public LiveDataPlayerPanel() {
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 0, 0};
		gridBagLayout.rowHeights = new int[]{0, 0, 0, 40, 0, 0, 0, 0, 0, 0, 0, 0};
		gridBagLayout.columnWeights = new double[]{1.0, 1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		setLayout(gridBagLayout);

				btnSelectDatabase = new JButton("Select database");
				GridBagConstraints gbc_btnSelectDatabase = new GridBagConstraints();
				gbc_btnSelectDatabase.fill = GridBagConstraints.HORIZONTAL;
				gbc_btnSelectDatabase.gridwidth = 2;
				gbc_btnSelectDatabase.insets = new Insets(0, 0, 5, 0);
				gbc_btnSelectDatabase.gridx = 0;
				gbc_btnSelectDatabase.gridy = 0;
				add(btnSelectDatabase, gbc_btnSelectDatabase);

		lblFrameNumber = new JLabel("Frame Number: ");
		GridBagConstraints gbc_lblFrameNumber = new GridBagConstraints();
		gbc_lblFrameNumber.insets = new Insets(0, 0, 5, 5);
		gbc_lblFrameNumber.anchor = GridBagConstraints.EAST;
		gbc_lblFrameNumber.gridx = 0;
		gbc_lblFrameNumber.gridy = 2;
		add(lblFrameNumber, gbc_lblFrameNumber);

		frameField = new JTextField();
		frameField.setText("0");
		GridBagConstraints gbc_frameField = new GridBagConstraints();
		gbc_frameField.insets = new Insets(0, 0, 5, 0);
		gbc_frameField.fill = GridBagConstraints.HORIZONTAL;
		gbc_frameField.gridx = 1;
		gbc_frameField.gridy = 2;
		add(frameField, gbc_frameField);
		frameField.setColumns(10);

										lblEventFilter = new JLabel("Event filter :");
										GridBagConstraints gbc_lblEventFilter = new GridBagConstraints();
										gbc_lblEventFilter.insets = new Insets(0, 0, 5, 5);
										gbc_lblEventFilter.gridx = 0;
										gbc_lblEventFilter.gridy = 3;
										add(lblEventFilter, gbc_lblEventFilter);

								textFieldEventFilter = new JTextField();
								GridBagConstraints gbc_textFieldEventFilter = new GridBagConstraints();
								gbc_textFieldEventFilter.fill = GridBagConstraints.HORIZONTAL;
								gbc_textFieldEventFilter.insets = new Insets(0, 0, 5, 0);
								gbc_textFieldEventFilter.gridx = 1;
								gbc_textFieldEventFilter.gridy = 3;
								add(textFieldEventFilter, gbc_textFieldEventFilter);
								textFieldEventFilter.setColumns(10);

				btnPrevious = new JButton("Previous");
				GridBagConstraints gbc_btnPrevious = new GridBagConstraints();
				gbc_btnPrevious.insets = new Insets(0, 0, 5, 5);
				gbc_btnPrevious.gridx = 0;
				gbc_btnPrevious.gridy = 4;
				add(btnPrevious, gbc_btnPrevious);

				btnNext = new JButton("Next");
				GridBagConstraints gbc_btnNext = new GridBagConstraints();
				gbc_btnNext.insets = new Insets(0, 0, 5, 0);
				gbc_btnNext.gridx = 1;
				gbc_btnNext.gridy = 4;
				add(btnNext, gbc_btnNext);

				lblEventJ = new JLabel("event J :");
				GridBagConstraints gbc_lblEventJ = new GridBagConstraints();
				gbc_lblEventJ.anchor = GridBagConstraints.EAST;
				gbc_lblEventJ.insets = new Insets(0, 0, 5, 5);
				gbc_lblEventJ.gridx = 0;
				gbc_lblEventJ.gridy = 5;
				add(lblEventJ, gbc_lblEventJ);

				txtEvent1 = new JTextField();
				txtEvent1.setText("event_1");
				GridBagConstraints gbc_txtEvent1 = new GridBagConstraints();
				gbc_txtEvent1.insets = new Insets(0, 0, 5, 0);
				gbc_txtEvent1.fill = GridBagConstraints.HORIZONTAL;
				gbc_txtEvent1.gridx = 1;
				gbc_txtEvent1.gridy = 5;
				add(txtEvent1, gbc_txtEvent1);
				txtEvent1.setColumns(10);

				lblEventK = new JLabel("event K:");
				GridBagConstraints gbc_lblEventK = new GridBagConstraints();
				gbc_lblEventK.anchor = GridBagConstraints.EAST;
				gbc_lblEventK.insets = new Insets(0, 0, 5, 5);
				gbc_lblEventK.gridx = 0;
				gbc_lblEventK.gridy = 6;
				add(lblEventK, gbc_lblEventK);

				txtEvent2 = new JTextField();
				txtEvent2.setText("event_2");
				GridBagConstraints gbc_txtEvent2 = new GridBagConstraints();
				gbc_txtEvent2.insets = new Insets(0, 0, 5, 0);
				gbc_txtEvent2.fill = GridBagConstraints.HORIZONTAL;
				gbc_txtEvent2.gridx = 1;
				gbc_txtEvent2.gridy = 6;
				add(txtEvent2, gbc_txtEvent2);
				txtEvent2.setColumns(10);

				lblEventL = new JLabel("event L:");
				GridBagConstraints gbc_lblEventL = new GridBagConstraints();
				gbc_lblEventL.anchor = GridBagConstraints.EAST;
				gbc_lblEventL.insets = new Insets(0, 0, 5, 5);
				gbc_lblEventL.gridx = 0;
				gbc_lblEventL.gridy = 7;
				add(lblEventL, gbc_lblEventL);

				txtEvent3 = new JTextField();
				txtEvent3.setText("event_3");
				GridBagConstraints gbc_txtEvent3 = new GridBagConstraints();
				gbc_txtEvent3.insets = new Insets(0, 0, 5, 0);
				gbc_txtEvent3.fill = GridBagConstraints.HORIZONTAL;
				gbc_txtEvent3.gridx = 1;
				gbc_txtEvent3.gridy = 7;
				add(txtEvent3, gbc_txtEvent3);
				txtEvent3.setColumns(10);

				lblEventM = new JLabel("event M:");
				GridBagConstraints gbc_lblEventM = new GridBagConstraints();
				gbc_lblEventM.anchor = GridBagConstraints.EAST;
				gbc_lblEventM.insets = new Insets(0, 0, 5, 5);
				gbc_lblEventM.gridx = 0;
				gbc_lblEventM.gridy = 8;
				add(lblEventM, gbc_lblEventM);

				txtEvent4 = new JTextField();
				txtEvent4.setText("event_4");
				GridBagConstraints gbc_txtEvent4 = new GridBagConstraints();
				gbc_txtEvent4.insets = new Insets(0, 0, 5, 0);
				gbc_txtEvent4.fill = GridBagConstraints.HORIZONTAL;
				gbc_txtEvent4.gridx = 1;
				gbc_txtEvent4.gridy = 8;
				add(txtEvent4, gbc_txtEvent4);
				txtEvent4.setColumns(10);

				lblEventB = new JLabel("event B:");
				GridBagConstraints gbc_lblEventB = new GridBagConstraints();
				gbc_lblEventB.anchor = GridBagConstraints.EAST;
				gbc_lblEventB.insets = new Insets(0, 0, 5, 5);
				gbc_lblEventB.gridx = 0;
				gbc_lblEventB.gridy = 9;
				add(lblEventB, gbc_lblEventB);

				txtEvent5 = new JTextField();
				txtEvent5.setText("event_5");
				txtEvent5.setColumns(10);
				GridBagConstraints gbc_txtEvent = new GridBagConstraints();
				gbc_txtEvent.insets = new Insets(0, 0, 5, 0);
				gbc_txtEvent.fill = GridBagConstraints.HORIZONTAL;
				gbc_txtEvent.gridx = 1;
				gbc_txtEvent.gridy = 9;
				add(txtEvent5, gbc_txtEvent);

				lblEventN = new JLabel("event N:");
				GridBagConstraints gbc_lblEventN = new GridBagConstraints();
				gbc_lblEventN.anchor = GridBagConstraints.EAST;
				gbc_lblEventN.insets = new Insets(0, 0, 0, 5);
				gbc_lblEventN.gridx = 0;
				gbc_lblEventN.gridy = 10;
				add(lblEventN, gbc_lblEventN);

				txtEvent6 = new JTextField();
				txtEvent6.setText("event_6");
				txtEvent6.setColumns(10);
				GridBagConstraints gbc_txtEvent_1 = new GridBagConstraints();
				gbc_txtEvent_1.fill = GridBagConstraints.HORIZONTAL;
				gbc_txtEvent_1.gridx = 1;
				gbc_txtEvent_1.gridy = 10;
				add(txtEvent6, gbc_txtEvent_1);
	}

	public JTextField getFrameField() {
		return frameField;
	}


	public JButton getBtnSelectDatabase() {
		return btnSelectDatabase;
	}
	public JTextField getTextFieldEventFilter() {
		return textFieldEventFilter;
	}
	public JButton getBtnPrevious() {
		return btnPrevious;
	}
	public JButton getBtnNext() {
		return btnNext;
	}
	public JTextField getTxtEvent1() {
		return txtEvent1;
	}
	public JTextField getTxtEvent2() {
		return txtEvent2;
	}
	public JTextField getTxtEvent3() {
		return txtEvent3;
	}
	public JTextField getTxtEvent4() {
		return txtEvent4;
	}
	public JTextField getTxtEvent5() {
		return txtEvent5;
	}
	public JTextField getTxtEvent6() {
		return txtEvent6;
	}
}
