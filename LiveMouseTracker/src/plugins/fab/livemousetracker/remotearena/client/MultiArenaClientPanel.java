package plugins.fab.livemousetracker.remotearena.client;

import javax.swing.JPanel;
import java.awt.GridBagLayout;
import javax.swing.JButton;
import java.awt.GridBagConstraints;
import java.awt.Font;
import javax.swing.JLabel;
import java.awt.Insets;
import javax.swing.border.EmptyBorder;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class MultiArenaClientPanel extends JPanel {
	private JTextField txtLocalhost;
	private JButton btnStart;
	private JTextField textField;
	private JTextField textField_1;
	private JLabel lblStatus;
	private JLabel lblRoiforInfo;
	private JLabel lblRoiforDetection;
	private JTextField textFieldROIInfo;
	private JTextField textFieldROIDetectionArea;
	public MultiArenaClientPanel() {
		setBorder(new EmptyBorder(5, 5, 5, 5));
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{50, 0, 0};
		gridBagLayout.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
		gridBagLayout.columnWeights = new double[]{1.0, 1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		setLayout(gridBagLayout);

		JLabel lblLiveMouseTracker = new JLabel("Live Mouse Tracker Multi-Arena Client");
		lblLiveMouseTracker.setFont(new Font("Tahoma", Font.PLAIN, 16));
		GridBagConstraints gbc_lblLiveMouseTracker = new GridBagConstraints();
		gbc_lblLiveMouseTracker.gridwidth = 2;
		gbc_lblLiveMouseTracker.insets = new Insets(0, 0, 5, 0);
		gbc_lblLiveMouseTracker.gridx = 0;
		gbc_lblLiveMouseTracker.gridy = 0;
		add(lblLiveMouseTracker, gbc_lblLiveMouseTracker);

		lblStatus = new JLabel("STATUS");
		lblStatus.setFont(new Font("Tahoma", Font.BOLD, 17));
		GridBagConstraints gbc_lblStatus = new GridBagConstraints();
		gbc_lblStatus.gridwidth = 2;
		gbc_lblStatus.insets = new Insets(0, 0, 5, 0);
		gbc_lblStatus.gridx = 0;
		gbc_lblStatus.gridy = 1;
		add(lblStatus, gbc_lblStatus);

		JLabel lblServerIp = new JLabel("Server IP : ");
		GridBagConstraints gbc_lblServerIp = new GridBagConstraints();
		gbc_lblServerIp.insets = new Insets(0, 0, 5, 5);
		gbc_lblServerIp.anchor = GridBagConstraints.EAST;
		gbc_lblServerIp.gridx = 0;
		gbc_lblServerIp.gridy = 2;
		add(lblServerIp, gbc_lblServerIp);

		txtLocalhost = new JTextField();
		txtLocalhost.setText("192.168.0.3");
		GridBagConstraints gbc_txtLocalhost = new GridBagConstraints();
		gbc_txtLocalhost.insets = new Insets(0, 0, 5, 0);
		gbc_txtLocalhost.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtLocalhost.gridx = 1;
		gbc_txtLocalhost.gridy = 2;
		add(txtLocalhost, gbc_txtLocalhost);
		txtLocalhost.setColumns(10);

		btnStart = new JButton("Start");
		btnStart.setFont(new Font("Tahoma", Font.BOLD, 16));
		GridBagConstraints gbc_btnStart = new GridBagConstraints();
		gbc_btnStart.insets = new Insets(0, 0, 5, 0);
		gbc_btnStart.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnStart.gridwidth = 2;
		gbc_btnStart.gridx = 0;
		gbc_btnStart.gridy = 3;
		add(btnStart, gbc_btnStart);

		JLabel lblXyOffsetIn = new JLabel("x,y offset in px:");
		GridBagConstraints gbc_lblXyOffsetIn = new GridBagConstraints();
		gbc_lblXyOffsetIn.anchor = GridBagConstraints.EAST;
		gbc_lblXyOffsetIn.insets = new Insets(0, 0, 5, 5);
		gbc_lblXyOffsetIn.gridx = 0;
		gbc_lblXyOffsetIn.gridy = 5;
		add(lblXyOffsetIn, gbc_lblXyOffsetIn);

		textField = new JTextField();
		textField.setText("512, 0");
		GridBagConstraints gbc_textField = new GridBagConstraints();
		gbc_textField.insets = new Insets(0, 0, 5, 0);
		gbc_textField.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField.gridx = 1;
		gbc_textField.gridy = 5;
		add(textField, gbc_textField);
		textField.setColumns(10);

		JLabel lblXyxyCropIn = new JLabel("x1,y1,x2,y2 crop in px:");
		GridBagConstraints gbc_lblXyxyCropIn = new GridBagConstraints();
		gbc_lblXyxyCropIn.anchor = GridBagConstraints.EAST;
		gbc_lblXyxyCropIn.insets = new Insets(0, 0, 5, 5);
		gbc_lblXyxyCropIn.gridx = 0;
		gbc_lblXyxyCropIn.gridy = 6;
		add(lblXyxyCropIn, gbc_lblXyxyCropIn);

		textField_1 = new JTextField();
		textField_1.setText("0,0,512,424");
		GridBagConstraints gbc_textField_1 = new GridBagConstraints();
		gbc_textField_1.insets = new Insets(0, 0, 5, 0);
		gbc_textField_1.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField_1.gridx = 1;
		gbc_textField_1.gridy = 6;
		add(textField_1, gbc_textField_1);
		textField_1.setColumns(10);

		lblRoiforInfo = new JLabel("x1,y1,x2,y2 ROI (for info)");
		GridBagConstraints gbc_lblRoiforInfo = new GridBagConstraints();
		gbc_lblRoiforInfo.anchor = GridBagConstraints.EAST;
		gbc_lblRoiforInfo.insets = new Insets(0, 0, 5, 5);
		gbc_lblRoiforInfo.gridx = 0;
		gbc_lblRoiforInfo.gridy = 8;
		add(lblRoiforInfo, gbc_lblRoiforInfo);

		textFieldROIInfo = new JTextField();

		textFieldROIInfo.setText("114,63,398,353");
		GridBagConstraints gbc_textFieldROIInfo = new GridBagConstraints();
		gbc_textFieldROIInfo.insets = new Insets(0, 0, 5, 0);
		gbc_textFieldROIInfo.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFieldROIInfo.gridx = 1;
		gbc_textFieldROIInfo.gridy = 8;
		add(textFieldROIInfo, gbc_textFieldROIInfo);
		textFieldROIInfo.setColumns(10);

		lblRoiforDetection = new JLabel("x1,y1,x2,y2 ROI (for detection crop)");
		GridBagConstraints gbc_lblRoiforDetection = new GridBagConstraints();
		gbc_lblRoiforDetection.anchor = GridBagConstraints.EAST;
		gbc_lblRoiforDetection.insets = new Insets(0, 0, 0, 5);
		gbc_lblRoiforDetection.gridx = 0;
		gbc_lblRoiforDetection.gridy = 9;
		add(lblRoiforDetection, gbc_lblRoiforDetection);

		textFieldROIDetectionArea = new JTextField();
		textFieldROIDetectionArea.setText("84,33,428,383");
		GridBagConstraints gbc_textFieldROICrop = new GridBagConstraints();
		gbc_textFieldROICrop.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFieldROICrop.gridx = 1;
		gbc_textFieldROICrop.gridy = 9;
		add(textFieldROIDetectionArea, gbc_textFieldROICrop);
		textFieldROIDetectionArea.setColumns(10);
	}



	public JButton getBtnStart() {
		return btnStart;
	}
	public JTextField getTxtServerIP() {
		return txtLocalhost;
	}
	public JLabel getStatusLabel() {
		return lblStatus;
	}
	public JTextField getOffsetTextField() {
		return textField;
	}
	public JTextField getCropTextField() {
		return textField_1;
	}
	public JTextField getTextFieldROICage() {
		return textFieldROIDetectionArea;
	}
	public JTextField getTextFieldCageFloor() {
		return textFieldROIInfo;
	}
}
