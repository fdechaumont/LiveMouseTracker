package plugins.fab.kinectdriver;

import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.border.TitledBorder;
import javax.swing.JCheckBox;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.UIManager;
import java.awt.Color;
import javax.swing.JComboBox;
import javax.swing.JTabbedPane;

public class KinectStreamerPanel extends JPanel {
	private JButton btnStopLive;
	private JButton btnPlayLive;
	private JCheckBox chckbxRecordRawData;
	private JButton btnSelectInputFolder;
	private JCheckBox chckbxUseCacheLoader;
	private JComboBox comboBox;
	private JButton btnNewButton;
	private JButton btnStartPlayback;
	private JButton button_1;
	private JButton button_2;
	private JButton button;
	private JTabbedPane tabbedPane;

	public KinectStreamerPanel() {
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{421, 0};
		gridBagLayout.rowHeights = new int[]{60, 0};
		gridBagLayout.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, Double.MIN_VALUE};
		setLayout(gridBagLayout);

		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		GridBagConstraints gbc_tabbedPane = new GridBagConstraints();
		gbc_tabbedPane.fill = GridBagConstraints.BOTH;
		gbc_tabbedPane.gridx = 0;
		gbc_tabbedPane.gridy = 0;
		add(tabbedPane, gbc_tabbedPane);

		JPanel panel = new JPanel();
		tabbedPane.addTab("Live", null, panel, null);
		panel.setBorder(new TitledBorder(null, "Live Streaming", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		GridBagLayout gbl_panel = new GridBagLayout();
		gbl_panel.columnWidths = new int[]{59, 55, 0};
		gbl_panel.rowHeights = new int[]{25, 0, 0};
		gbl_panel.columnWeights = new double[]{1.0, 1.0, Double.MIN_VALUE};
		gbl_panel.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		panel.setLayout(gbl_panel);

		btnStopLive = new JButton("Stop");
		GridBagConstraints gbc_btnStopLive = new GridBagConstraints();
		gbc_btnStopLive.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnStopLive.anchor = GridBagConstraints.NORTHWEST;
		gbc_btnStopLive.insets = new Insets(0, 0, 5, 5);
		gbc_btnStopLive.gridx = 0;
		gbc_btnStopLive.gridy = 0;
		panel.add(btnStopLive, gbc_btnStopLive);

		btnPlayLive = new JButton("Start Live");
		GridBagConstraints gbc_btnPlayLive = new GridBagConstraints();
		gbc_btnPlayLive.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnPlayLive.anchor = GridBagConstraints.NORTHWEST;
		gbc_btnPlayLive.insets = new Insets(0, 0, 5, 0);
		gbc_btnPlayLive.gridx = 1;
		gbc_btnPlayLive.gridy = 0;
		panel.add(btnPlayLive, gbc_btnPlayLive);

		chckbxRecordRawData = new JCheckBox("Record RAW data");
		GridBagConstraints gbc_chckbxRecordRawData = new GridBagConstraints();
		gbc_chckbxRecordRawData.anchor = GridBagConstraints.NORTH;
		gbc_chckbxRecordRawData.fill = GridBagConstraints.HORIZONTAL;
		gbc_chckbxRecordRawData.gridwidth = 2;
		gbc_chckbxRecordRawData.gridx = 0;
		gbc_chckbxRecordRawData.gridy = 1;
		panel.add(chckbxRecordRawData, gbc_chckbxRecordRawData);

		JPanel panel_1 = new JPanel();
		tabbedPane.addTab("Playback", null, panel_1, null);
		panel_1.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Playback RAW data set", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		GridBagLayout gbl_panel_1 = new GridBagLayout();
		gbl_panel_1.columnWidths = new int[]{10, 10, 0};
		gbl_panel_1.rowHeights = new int[]{0, 0, 25, 0, 0, 0};
		gbl_panel_1.columnWeights = new double[]{1.0, 1.0, Double.MIN_VALUE};
		gbl_panel_1.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		panel_1.setLayout(gbl_panel_1);

		btnSelectInputFolder = new JButton("Select input Folder...");
		GridBagConstraints gbc_btnSelectInputFolder = new GridBagConstraints();
		gbc_btnSelectInputFolder.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnSelectInputFolder.insets = new Insets(0, 0, 5, 5);
		gbc_btnSelectInputFolder.gridx = 0;
		gbc_btnSelectInputFolder.gridy = 0;
		panel_1.add(btnSelectInputFolder, gbc_btnSelectInputFolder);

		chckbxUseCacheLoader = new JCheckBox("Use cache loader");
		GridBagConstraints gbc_chckbxUseCacheLoader = new GridBagConstraints();
		gbc_chckbxUseCacheLoader.anchor = GridBagConstraints.NORTHWEST;
		gbc_chckbxUseCacheLoader.insets = new Insets(0, 0, 5, 0);
		gbc_chckbxUseCacheLoader.gridx = 1;
		gbc_chckbxUseCacheLoader.gridy = 0;
		panel_1.add(chckbxUseCacheLoader, gbc_chckbxUseCacheLoader);
		chckbxUseCacheLoader.setSelected(true);

		comboBox = new JComboBox();
		GridBagConstraints gbc_comboBox = new GridBagConstraints();
		gbc_comboBox.gridwidth = 2;
		gbc_comboBox.insets = new Insets(0, 0, 5, 0);
		gbc_comboBox.fill = GridBagConstraints.HORIZONTAL;
		gbc_comboBox.gridx = 0;
		gbc_comboBox.gridy = 1;
		panel_1.add(comboBox, gbc_comboBox);

		btnNewButton = new JButton("Stop");
		GridBagConstraints gbc_btnNewButton = new GridBagConstraints();
		gbc_btnNewButton.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnNewButton.anchor = GridBagConstraints.NORTHWEST;
		gbc_btnNewButton.insets = new Insets(0, 0, 5, 5);
		gbc_btnNewButton.gridx = 0;
		gbc_btnNewButton.gridy = 2;
		panel_1.add(btnNewButton, gbc_btnNewButton);

		btnStartPlayback = new JButton("Play");
		GridBagConstraints gbc_btnStartPlayback = new GridBagConstraints();
		gbc_btnStartPlayback.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnStartPlayback.anchor = GridBagConstraints.NORTHWEST;
		gbc_btnStartPlayback.insets = new Insets(0, 0, 5, 0);
		gbc_btnStartPlayback.gridx = 1;
		gbc_btnStartPlayback.gridy = 2;
		panel_1.add(btnStartPlayback, gbc_btnStartPlayback);

		button_1 = new JButton("||");
		GridBagConstraints gbc_button_1 = new GridBagConstraints();
		gbc_button_1.fill = GridBagConstraints.HORIZONTAL;
		gbc_button_1.anchor = GridBagConstraints.NORTHWEST;
		gbc_button_1.insets = new Insets(0, 0, 5, 5);
		gbc_button_1.gridx = 0;
		gbc_button_1.gridy = 3;
		panel_1.add(button_1, gbc_button_1);

		button_2 = new JButton(">|");
		GridBagConstraints gbc_button_2 = new GridBagConstraints();
		gbc_button_2.fill = GridBagConstraints.HORIZONTAL;
		gbc_button_2.anchor = GridBagConstraints.NORTHWEST;
		gbc_button_2.insets = new Insets(0, 0, 5, 0);
		gbc_button_2.gridx = 1;
		gbc_button_2.gridy = 3;
		panel_1.add(button_2, gbc_button_2);

		button = new JButton(">");
		GridBagConstraints gbc_button = new GridBagConstraints();
		gbc_button.gridwidth = 2;
		gbc_button.fill = GridBagConstraints.HORIZONTAL;
		gbc_button.anchor = GridBagConstraints.NORTHWEST;
		gbc_button.gridx = 0;
		gbc_button.gridy = 4;
		panel_1.add(button, gbc_button);

	}


	public JButton getBtnStopLive() {
		return btnStopLive;
	}
	public JButton getBtnPlayLive() {
		return btnPlayLive;
	}
	public JCheckBox getChckbxRecordRawData() {
		return chckbxRecordRawData;
	}
	public JButton getBtnSelectInputFolder() {
		return btnSelectInputFolder;
	}
	public JCheckBox getChckbxUseCacheLoader() {
		return chckbxUseCacheLoader;
	}
	public JComboBox getDataSetComboBox() {
		return comboBox;
	}
	public JButton getBtnStopPlayback() {
		return btnNewButton;
	}
	public JButton getBtnPlayPlayback() {
		return btnStartPlayback;
	}
	public JButton getButtonPausePlayBack() {
		return button_1;
	}
	public JButton getButtonStepPlayBack() {
		return button_2;
	}
	public JButton getButtonResumePlayBack() {
		return button;
	}
}
