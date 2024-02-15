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
package plugins.fab.livemousetracker;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import java.awt.GridBagLayout;
import javax.swing.JTextField;
import java.awt.GridBagConstraints;
import javax.swing.JLabel;
import java.awt.Insets;
import javax.swing.border.TitledBorder;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.border.EmptyBorder;
import javax.swing.UIManager;
import java.awt.Color;
import javax.swing.SwingConstants;
import javax.swing.JTextArea;

public class LiveMouseTrackerPanel extends JPanel {

	public LiveMouseTrackerPanel() {
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 0};
		gridBagLayout.rowHeights = new int[]{0, 321, 0};
		gridBagLayout.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		setLayout(gridBagLayout);

				JLabel label = new JLabel("Live Mouse Tracker");
				label.setHorizontalAlignment(SwingConstants.CENTER);
				label.setFont(new Font("Tahoma", Font.BOLD, 24));
				GridBagConstraints gbc_label = new GridBagConstraints();
				gbc_label.insets = new Insets(10, 10, 10, 10);
				gbc_label.gridx = 0;
				gbc_label.gridy = 0;
				add(label, gbc_label);

				tabbedPane = new JTabbedPane(JTabbedPane.TOP);
				GridBagConstraints gbc_tabbedPane = new GridBagConstraints();
				gbc_tabbedPane.fill = GridBagConstraints.BOTH;
				gbc_tabbedPane.gridx = 0;
				gbc_tabbedPane.gridy = 1;
				add(tabbedPane, gbc_tabbedPane);

				JPanel experimentPanel = new JPanel();
				experimentPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
				tabbedPane.addTab("Experiment", null, experimentPanel, null);
				GridBagLayout gbl_experimentPanel = new GridBagLayout();
				gbl_experimentPanel.columnWidths = new int[]{0, 0, 0};
				gbl_experimentPanel.rowHeights = new int[]{0, 0, 0, 0, 43, 0};
				gbl_experimentPanel.columnWeights = new double[]{1.0, 1.0, Double.MIN_VALUE};
				gbl_experimentPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
				experimentPanel.setLayout(gbl_experimentPanel);

				JPanel panel_1 = new JPanel();
				GridBagConstraints gbc_panel_1 = new GridBagConstraints();
				gbc_panel_1.fill = GridBagConstraints.BOTH;
				gbc_panel_1.insets = new Insets(0, 0, 5, 0);
				gbc_panel_1.gridx = 1;
				gbc_panel_1.gridy = 0;
				experimentPanel.add(panel_1, gbc_panel_1);

				select1AnimalButton = new JButton("1");
				panel_1.add(select1AnimalButton);

				select2AnimalButton = new JButton("2");
				panel_1.add(select2AnimalButton);

				select3AnimalButton = new JButton("3");
				panel_1.add(select3AnimalButton);

				select4AnimalButton = new JButton("4");
				select4AnimalButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
					}
				});
				panel_1.add(select4AnimalButton);

				JLabel label_1 = new JLabel("Max number of animals:");
				GridBagConstraints gbc_label_1 = new GridBagConstraints();
				gbc_label_1.anchor = GridBagConstraints.EAST;
				gbc_label_1.insets = new Insets(0, 0, 5, 5);
				gbc_label_1.gridx = 0;
				gbc_label_1.gridy = 1;
				experimentPanel.add(label_1, gbc_label_1);

				numberOfMaxAnimalTextField = new JTextField();
				numberOfMaxAnimalTextField.setColumns(10);
				GridBagConstraints gbc_numberOfMaxAnimalTextField = new GridBagConstraints();
				gbc_numberOfMaxAnimalTextField.fill = GridBagConstraints.HORIZONTAL;
				gbc_numberOfMaxAnimalTextField.insets = new Insets(0, 0, 5, 0);
				gbc_numberOfMaxAnimalTextField.gridx = 1;
				gbc_numberOfMaxAnimalTextField.gridy = 1;
				experimentPanel.add(numberOfMaxAnimalTextField, gbc_numberOfMaxAnimalTextField);

				JLabel label_3 = new JLabel("Experiment folder:");
				GridBagConstraints gbc_label_3 = new GridBagConstraints();
				gbc_label_3.anchor = GridBagConstraints.EAST;
				gbc_label_3.insets = new Insets(0, 0, 5, 5);
				gbc_label_3.gridx = 0;
				gbc_label_3.gridy = 2;
				experimentPanel.add(label_3, gbc_label_3);

				experimentFolderTextField = new JTextField();
				experimentFolderTextField.setColumns(10);
				GridBagConstraints gbc_experimentFolderTextField = new GridBagConstraints();
				gbc_experimentFolderTextField.insets = new Insets(0, 0, 5, 0);
				gbc_experimentFolderTextField.fill = GridBagConstraints.HORIZONTAL;
				gbc_experimentFolderTextField.gridx = 1;
				gbc_experimentFolderTextField.gridy = 2;
				experimentPanel.add(experimentFolderTextField, gbc_experimentFolderTextField);

				JLabel label_4 = new JLabel("Experiment name: ");
				GridBagConstraints gbc_label_4 = new GridBagConstraints();
				gbc_label_4.anchor = GridBagConstraints.EAST;
				gbc_label_4.insets = new Insets(0, 0, 5, 5);
				gbc_label_4.gridx = 0;
				gbc_label_4.gridy = 3;
				experimentPanel.add(label_4, gbc_label_4);

				experimentNameTextField = new JTextField();
				experimentNameTextField.setColumns(10);
				GridBagConstraints gbc_experimentNameTextField = new GridBagConstraints();
				gbc_experimentNameTextField.insets = new Insets(0, 0, 5, 0);
				gbc_experimentNameTextField.fill = GridBagConstraints.HORIZONTAL;
				gbc_experimentNameTextField.gridx = 1;
				gbc_experimentNameTextField.gridy = 3;
				experimentPanel.add(experimentNameTextField, gbc_experimentNameTextField);

				JPanel panel_6 = new JPanel();
				GridBagConstraints gbc_panel_6 = new GridBagConstraints();
				gbc_panel_6.gridwidth = 2;
				gbc_panel_6.fill = GridBagConstraints.VERTICAL;
				gbc_panel_6.gridx = 0;
				gbc_panel_6.gridy = 4;
				experimentPanel.add(panel_6, gbc_panel_6);

				stopButton = new JButton("Stop");
				stopButton.setFont(new Font("Tahoma", Font.BOLD, 18));
				panel_6.add(stopButton);

				pauseButton = new JButton("Pause");
				pauseButton.setFont(new Font("Tahoma", Font.BOLD, 18));
				panel_6.add(pauseButton);

				startLiveButton = new JButton("Start Live !");
				startLiveButton.setFont(new Font("Tahoma", Font.BOLD, 18));
				panel_6.add(startLiveButton);

				JPanel savePanel = new JPanel();
				savePanel.setBorder(new EmptyBorder(15, 15, 15, 15));
				tabbedPane.addTab("Save", null, savePanel, null);
				GridBagLayout gbl_savePanel = new GridBagLayout();
				gbl_savePanel.columnWidths = new int[]{0, 0};
				gbl_savePanel.rowHeights = new int[]{56, 0, 0, 93, 0};
				gbl_savePanel.columnWeights = new double[]{1.0, Double.MIN_VALUE};
				gbl_savePanel.rowWeights = new double[]{0.0, 1.0, 0.0, 0.0, Double.MIN_VALUE};
				savePanel.setLayout(gbl_savePanel);

								JPanel panel_5 = new JPanel();
								panel_5.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Streaming to SQLite", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
								GridBagConstraints gbc_panel_5 = new GridBagConstraints();
								gbc_panel_5.fill = GridBagConstraints.BOTH;
								gbc_panel_5.insets = new Insets(0, 0, 5, 0);
								gbc_panel_5.gridx = 0;
								gbc_panel_5.gridy = 0;
								savePanel.add(panel_5, gbc_panel_5);
								GridBagLayout gbl_panel_5 = new GridBagLayout();
								gbl_panel_5.columnWidths = new int[]{0, 0};
								gbl_panel_5.rowHeights = new int[]{0, 0, 0};
								gbl_panel_5.columnWeights = new double[]{1.0, Double.MIN_VALUE};
								gbl_panel_5.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
								panel_5.setLayout(gbl_panel_5);

								streamToSQLCheckBox = new JCheckBox("Stream to sqlLite dataBase");
								streamToSQLCheckBox.setHorizontalAlignment(SwingConstants.CENTER);
								GridBagConstraints gbc_streamToSQLCheckBox = new GridBagConstraints();
								gbc_streamToSQLCheckBox.anchor = GridBagConstraints.WEST;
								gbc_streamToSQLCheckBox.insets = new Insets(0, 0, 5, 0);
								gbc_streamToSQLCheckBox.gridx = 0;
								gbc_streamToSQLCheckBox.gridy = 0;
								panel_5.add(streamToSQLCheckBox, gbc_streamToSQLCheckBox);
								streamToSQLCheckBox.setSelected(true);

				panel_2 = new JPanel();
				panel_2.setBorder(new TitledBorder(null, "Saving background height map", TitledBorder.LEADING, TitledBorder.TOP, null, null));
				GridBagConstraints gbc_panel_2 = new GridBagConstraints();
				gbc_panel_2.gridwidth = 2;
				gbc_panel_2.gridheight = 2;
				gbc_panel_2.insets = new Insets(0, 0, 5, 0);
				gbc_panel_2.fill = GridBagConstraints.BOTH;
				gbc_panel_2.gridx = 0;
				gbc_panel_2.gridy = 1;
				savePanel.add(panel_2, gbc_panel_2);
				GridBagLayout gbl_panel_2 = new GridBagLayout();
				gbl_panel_2.columnWidths = new int[]{142, 131, 0, 0};
				gbl_panel_2.rowHeights = new int[]{23, 0, 0};
				gbl_panel_2.columnWeights = new double[]{0.0, 0.0, 1.0, Double.MIN_VALUE};
				gbl_panel_2.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
				panel_2.setLayout(gbl_panel_2);

				saveBackgroundMapCheckBox = new JCheckBox("Save background map");
				saveBackgroundMapCheckBox.setSelected(true);
				GridBagConstraints gbc_saveBackgroundMapCheckBox = new GridBagConstraints();
				gbc_saveBackgroundMapCheckBox.insets = new Insets(0, 0, 5, 5);
				gbc_saveBackgroundMapCheckBox.anchor = GridBagConstraints.NORTHWEST;
				gbc_saveBackgroundMapCheckBox.gridx = 0;
				gbc_saveBackgroundMapCheckBox.gridy = 0;
				panel_2.add(saveBackgroundMapCheckBox, gbc_saveBackgroundMapCheckBox);

				lblRecordEach = new JLabel("record each # frames ");
				GridBagConstraints gbc_lblRecordEach = new GridBagConstraints();
				gbc_lblRecordEach.insets = new Insets(0, 0, 0, 5);
				gbc_lblRecordEach.anchor = GridBagConstraints.EAST;
				gbc_lblRecordEach.gridx = 0;
				gbc_lblRecordEach.gridy = 1;
				panel_2.add(lblRecordEach, gbc_lblRecordEach);

				SaveBackGroundRecordEachFrame = new JTextField();
				SaveBackGroundRecordEachFrame.setText("1800");
				GridBagConstraints gbc_SaveBackGroundRecordEachFrame = new GridBagConstraints();
				gbc_SaveBackGroundRecordEachFrame.fill = GridBagConstraints.HORIZONTAL;
				gbc_SaveBackGroundRecordEachFrame.gridx = 1;
				gbc_SaveBackGroundRecordEachFrame.gridy = 1;
				panel_2.add(SaveBackGroundRecordEachFrame, gbc_SaveBackGroundRecordEachFrame);
				SaveBackGroundRecordEachFrame.setColumns(10);

				chckbxSaveMedallon = new JCheckBox("Save Medallon");
				GridBagConstraints gbc_chckbxSaveMedallon = new GridBagConstraints();
				gbc_chckbxSaveMedallon.anchor = GridBagConstraints.WEST;
				gbc_chckbxSaveMedallon.insets = new Insets(0, 0, 5, 0);
				gbc_chckbxSaveMedallon.gridx = 0;
				gbc_chckbxSaveMedallon.gridy = 2;
				savePanel.add(chckbxSaveMedallon, gbc_chckbxSaveMedallon);

				JPanel panel_3 = new JPanel();
				panel_3.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Streaming to MP4 Timelapse", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
				GridBagConstraints gbc_panel_3 = new GridBagConstraints();
				gbc_panel_3.fill = GridBagConstraints.BOTH;
				gbc_panel_3.gridx = 0;
				gbc_panel_3.gridy = 3;
				savePanel.add(panel_3, gbc_panel_3);
				GridBagLayout gbl_panel_3 = new GridBagLayout();
				gbl_panel_3.columnWidths = new int[]{159, 182, 0};
				gbl_panel_3.rowHeights = new int[]{25, 25,0, 0};
				gbl_panel_3.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
				gbl_panel_3.rowWeights = new double[]{0.0,0.0, 0.0, Double.MIN_VALUE};
				panel_3.setLayout(gbl_panel_3);

				saveToMp4CheckBox = new JCheckBox("Save timelapse to MP4");
				saveToMp4CheckBox.setSelected(true);
				GridBagConstraints gbc_saveToMp4CheckBox = new GridBagConstraints();
				gbc_saveToMp4CheckBox.anchor = GridBagConstraints.NORTH;
				gbc_saveToMp4CheckBox.insets = new Insets(0, 0, 5, 5);
				gbc_saveToMp4CheckBox.gridx = 0;
				gbc_saveToMp4CheckBox.gridy = 0;
				panel_3.add(saveToMp4CheckBox, gbc_saveToMp4CheckBox);
				
				saveToMp4WithoutOverlayCheckBox = new JCheckBox("Save timelapse to MP4 (without overlay)");
				saveToMp4WithoutOverlayCheckBox.setSelected(false);
				GridBagConstraints gbc_saveToMp4CheckBox2 = new GridBagConstraints();
				gbc_saveToMp4CheckBox.anchor = GridBagConstraints.NORTH;
				gbc_saveToMp4CheckBox.insets = new Insets(0, 0, 5, 5);
				gbc_saveToMp4CheckBox.gridx = 0;
				gbc_saveToMp4CheckBox.gridy = 0;
				panel_3.add(saveToMp4WithoutOverlayCheckBox, gbc_saveToMp4CheckBox2);
				
				

				JLabel label_5 = new JLabel("record each # frames");
				GridBagConstraints gbc_label_5 = new GridBagConstraints();
				gbc_label_5.anchor = GridBagConstraints.EAST;
				gbc_label_5.insets = new Insets(0, 0, 0, 5);
				gbc_label_5.gridx = 0;
				gbc_label_5.gridy = 1;
				panel_3.add(label_5, gbc_label_5);

				saveToMp4SkipFrame = new JTextField();
				saveToMp4SkipFrame.setText("2");
				saveToMp4SkipFrame.setColumns(10);
				GridBagConstraints gbc_saveToMp4SkipFrame = new GridBagConstraints();
				gbc_saveToMp4SkipFrame.fill = GridBagConstraints.HORIZONTAL;
				gbc_saveToMp4SkipFrame.gridx = 1;
				gbc_saveToMp4SkipFrame.gridy = 1;
				panel_3.add(saveToMp4SkipFrame, gbc_saveToMp4SkipFrame);

								JPanel testPanel = new JPanel();
								testPanel.setBorder(null);
								tabbedPane.addTab("Advanced Options", null, testPanel, null);
								GridBagLayout gbl_testPanel = new GridBagLayout();
								gbl_testPanel.columnWidths = new int[] {115, 0, 0};
								gbl_testPanel.rowHeights = new int[]{25, 25, 0, 0, 0, 0, 0};
								gbl_testPanel.columnWeights = new double[]{1.0, 1.0, Double.MIN_VALUE};
								gbl_testPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
								testPanel.setLayout(gbl_testPanel);

								logInConsole = new JCheckBox("Log in console");
								GridBagConstraints gbc_logInConsole = new GridBagConstraints();
								gbc_logInConsole.anchor = GridBagConstraints.WEST;
								gbc_logInConsole.insets = new Insets(0, 0, 5, 5);
								gbc_logInConsole.gridx = 0;
								gbc_logInConsole.gridy = 0;
								testPanel.add(logInConsole, gbc_logInConsole);

								chckbxMultiArenaMode = new JCheckBox("Multi arena mode");
								GridBagConstraints gbc_chckbxMultiArenaMode = new GridBagConstraints();
								gbc_chckbxMultiArenaMode.anchor = GridBagConstraints.WEST;
								gbc_chckbxMultiArenaMode.insets = new Insets(0, 0, 5, 5);
								gbc_chckbxMultiArenaMode.gridx = 0;
								gbc_chckbxMultiArenaMode.gridy = 1;
								testPanel.add(chckbxMultiArenaMode, gbc_chckbxMultiArenaMode);

								chckbxAnimalAreWired = new JCheckBox("Animal are wired");
								GridBagConstraints gbc_chckbxAnimalAreWired = new GridBagConstraints();
								gbc_chckbxAnimalAreWired.insets = new Insets(0, 0, 5, 5);
								gbc_chckbxAnimalAreWired.anchor = GridBagConstraints.WEST;
								gbc_chckbxAnimalAreWired.gridx = 0;
								gbc_chckbxAnimalAreWired.gridy = 2;
								testPanel.add(chckbxAnimalAreWired, gbc_chckbxAnimalAreWired);

								diadicBlackAndWhiteWithoutRFID = new JCheckBox("Diadic black and white without RFID");
								GridBagConstraints gbc_diadicBlackAndWhiteWithoutRFID = new GridBagConstraints();
								gbc_diadicBlackAndWhiteWithoutRFID.anchor = GridBagConstraints.WEST;
								gbc_diadicBlackAndWhiteWithoutRFID.insets = new Insets(0, 0, 5, 5);
								gbc_diadicBlackAndWhiteWithoutRFID.gridx = 0;
								gbc_diadicBlackAndWhiteWithoutRFID.gridy = 3;
								testPanel.add(diadicBlackAndWhiteWithoutRFID, gbc_diadicBlackAndWhiteWithoutRFID);

								chckbxPerspectiveMode = new JCheckBox("Perspective Mode");
								GridBagConstraints gbc_chckbxPerspectiveMode = new GridBagConstraints();
								gbc_chckbxPerspectiveMode.anchor = GridBagConstraints.WEST;
								gbc_chckbxPerspectiveMode.insets = new Insets(0, 0, 5, 5);
								gbc_chckbxPerspectiveMode.gridx = 0;
								gbc_chckbxPerspectiveMode.gridy = 4;
								testPanel.add(chckbxPerspectiveMode, gbc_chckbxPerspectiveMode);

								lblDevValue = new JLabel("Dev Value 01 :");
								GridBagConstraints gbc_lblDevValue = new GridBagConstraints();
								gbc_lblDevValue.insets = new Insets(0, 0, 0, 5);
								gbc_lblDevValue.anchor = GridBagConstraints.EAST;
								gbc_lblDevValue.gridx = 0;
								gbc_lblDevValue.gridy = 5;
								testPanel.add(lblDevValue, gbc_lblDevValue);

								devValue01 = new JTextField();
								devValue01.setText("0");
								GridBagConstraints gbc_devValue01 = new GridBagConstraints();
								gbc_devValue01.fill = GridBagConstraints.HORIZONTAL;
								gbc_devValue01.gridx = 1;
								gbc_devValue01.gridy = 5;
								testPanel.add(devValue01, gbc_devValue01);
								devValue01.setColumns(10);

								panel = new JPanel();
								tabbedPane.addTab("TTL", null, panel, null);
								GridBagLayout gbl_panel = new GridBagLayout();
								gbl_panel.columnWidths = new int[] {0, 115, 0, 0};
								gbl_panel.rowHeights = new int[] {0, 0, 0, 25, 25, 25, 0};
								gbl_panel.columnWeights = new double[]{0.0, Double.MIN_VALUE};
								gbl_panel.rowWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
								panel.setLayout(gbl_panel);

								timeSynchroArduinoTTLCheckBox = new JCheckBox("Time synchro to Arduino / TTL");
								timeSynchroArduinoTTLCheckBox.setSelected(false);
								GridBagConstraints gbc_timeSynchroArduinoTTLCheckBox = new GridBagConstraints();
								gbc_timeSynchroArduinoTTLCheckBox.anchor = GridBagConstraints.WEST;
								gbc_timeSynchroArduinoTTLCheckBox.insets = new Insets(0, 0, 5, 0);
								gbc_timeSynchroArduinoTTLCheckBox.gridx = 0;
								gbc_timeSynchroArduinoTTLCheckBox.gridy = 1;
								panel.add(timeSynchroArduinoTTLCheckBox, gbc_timeSynchroArduinoTTLCheckBox);

								manageEventFromArduinoTTL = new JCheckBox("Manage events from Arduino / TTL");
								manageEventFromArduinoTTL.setSelected(false);
								GridBagConstraints gbc_manageEventFromArduinoTTL = new GridBagConstraints();
								gbc_manageEventFromArduinoTTL.anchor = GridBagConstraints.WEST;
								gbc_manageEventFromArduinoTTL.gridx = 0;
								gbc_manageEventFromArduinoTTL.gridy = 2;
								panel.add(manageEventFromArduinoTTL, gbc_manageEventFromArduinoTTL);
	}

	/**
	 *
	 */
	private static final long serialVersionUID = -4290229641939066326L;
	private JTabbedPane tabbedPane;
	private JTextField numberOfMaxAnimalTextField;
	private JTextField saveToMp4SkipFrame;
	private JButton select4AnimalButton;
	private JButton select3AnimalButton;
	private JButton select1AnimalButton;
	private JButton select2AnimalButton;
	private JCheckBox saveToMp4CheckBox;
	private JCheckBox saveToMp4WithoutOverlayCheckBox;
	
	
	private JCheckBox streamToSQLCheckBox;
	private JTextField experimentFolderTextField;
	private JTextField experimentNameTextField;
	private JButton startLiveButton;
	private JButton stopButton;
	private JButton pauseButton;
	private JCheckBox logInConsole;
	private JCheckBox chckbxAnimalAreWired;
	private JTextField devValue01;
	private JLabel lblDevValue;
	private JCheckBox chckbxMultiArenaMode;
	private JCheckBox diadicBlackAndWhiteWithoutRFID;
	private JCheckBox chckbxSaveMedallon;
	private JPanel panel;
	private JCheckBox timeSynchroArduinoTTLCheckBox;
	private JCheckBox manageEventFromArduinoTTL;
	private JCheckBox chckbxPerspectiveMode;
	private JPanel panel_2;
	private JCheckBox saveBackgroundMapCheckBox;
	private JTextField SaveBackGroundRecordEachFrame;
	private JLabel lblRecordEach;



	public JTextField getNumberOfMaxAnimalTextField() {
		return numberOfMaxAnimalTextField;
	}

//	public JComboBox getColorMode() {
//		return comboBox;
//	}

//	public JTextField getExperimentFolderTextField() {
//		return experimentTextField;
//	}
//	public JTextField getExperimentNameTextField() {
//		return experimentNameTextField;
//	}
//	public JCheckBox getStreamToSqliteCheckBox() {
//		return streamToSqliteCheckBox;
//	}
//	public JTextField getMpegFrameInterval() {
//		return mpegFrameInterval;
//	}
//	public JCheckBox getSaveMP4TimeLapseCheckBox() {
//		return saveMP4TimeLapseCheckBox;
//	}
//	public JComboBox getMp4TimeLapseDataTypeComboBox() {
//		return mp4TimeLapseDataTypeComboBox;
//	}
//	public JButton getSaveAllTracksButton() {
//		return saveAllTracksButton;
//	}
//	public JButton getLoadAllTracksButton() {
//		return loadAllTracksButton;
//	}
//	public JButton getSaveTrackAsStreamButton() {
//		return saveTrackAsStreamButton;
//	}
//	public JCheckBox getAnimalHaveHatsCheckBox() {
//		return animalHaveHatsCheckBox;
//	}
//	public JButton getSpeedSelectedNumberOfAnimal2Button() {
//		return speedSelectedNumberOfAnimal2Button;
//	}
//	public JButton getSpeedSelectedNumberOfAnimal1Button() {
//		return speedSelectedNumberOfAnimal1Button;
//	}
//	public JButton getSpeedSelectedNumberOfAnimal3Button() {
//		return speedSelectedNumberOfAnimal3Button;
//	}
//	public JButton getSpeedSelectedNumberOfAnimal4Button() {
//		return speedSelectedNumberOfAnimal4Button;
//	}

	public JButton getSelect4AnimalButton() {
		return select4AnimalButton;
	}
	public JButton getSelect3AnimalButton() {
		return select3AnimalButton;
	}
	public JButton getSelect1AnimalButton() {
		return select1AnimalButton;
	}
	public JButton getSelect2AnimalButton() {
		return select2AnimalButton;
	}
	public JTextField getExperimentNameTextField() {
		return experimentNameTextField;
	}
	public JTextField getExperimentFolderTextField() {
		return experimentFolderTextField;
	}
	public JCheckBox getSaveToMp4CheckBox() {
		return saveToMp4CheckBox;
	}
	
	public JCheckBox getSaveToMp4WithoutOverlayCheckBox() {
		return saveToMp4WithoutOverlayCheckBox;
	}


	public JTextField getSaveToMp4SkipFrame() {
		return saveToMp4SkipFrame;
	}
	public JCheckBox getStreamToSQLCheckBox() {
		return streamToSQLCheckBox;
	}

	public JButton getStartLiveButton() {
		return startLiveButton;
	}
	public JButton getStopButton() {
		return stopButton;
	}
	public JButton getPauseButton() {
		return pauseButton;
	}

	public JCheckBox getLogInConsoleCheckBox() {
		return logInConsole;
	}
	public JCheckBox getChckbxAnimalAreWired() {
		return chckbxAnimalAreWired;
	}
	public JTextField getDevValue01() {
		return devValue01;
	}
	public JCheckBox getCheckBoxMultiArenaMode() {
		return chckbxMultiArenaMode;
	}
	public JCheckBox getDiadicBlackAndWhiteWithoutRFIDCheckBox() {
		return diadicBlackAndWhiteWithoutRFID;
	}
	public JCheckBox getSaveMedallonCheckBox() {
		return chckbxSaveMedallon;
	}
	public JCheckBox getTimeSynchroArduinoTTLCheckBox() {
		return timeSynchroArduinoTTLCheckBox;
	}
	public JCheckBox getManageEventFromArduinoTTL() {
		return manageEventFromArduinoTTL;
	}
	public JCheckBox getPerspectiveMode() {
		return chckbxPerspectiveMode;
	}
	public JCheckBox getSaveBackgroundMapCheckBox() {
		return saveBackgroundMapCheckBox;
	}
	public JTextField getSaveBackGroundRecordEachFrame() {
		return SaveBackGroundRecordEachFrame;
	}
}
