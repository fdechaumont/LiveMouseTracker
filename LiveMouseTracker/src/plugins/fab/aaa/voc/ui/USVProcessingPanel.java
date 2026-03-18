package plugins.fab.aaa.voc.ui;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.border.EmptyBorder;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JCheckBox;
import javax.swing.JTextPane;
import javax.swing.border.TitledBorder;
import javax.swing.JSplitPane;
import javax.swing.JTextField;

public class USVProcessingPanel extends JPanel {
	private JTextArea textArea;
	private JButton btnAddFilesTo;
	private JButton btnStartUsvProcessing;
	private JScrollPane scrollPane;
	private JScrollPane scrollPane_1;
	private JTextArea textArea_1;
	private JCheckBox detailedOutputCheckBox;
	private JSplitPane splitPane;
	private JLabel lblNewLabel;
	private JTextField textField;
	public USVProcessingPanel() {
		setBorder(new EmptyBorder(20, 20, 20, 20));
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0,0};
		gridBagLayout.rowHeights = new int[]{0,0, 0, 0, 0, 0, 0,0};
		gridBagLayout.columnWeights = new double[]{1.0,Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0,0.0, 1.0, 1.0, 0.0, 0.0, 0.0,Double.MIN_VALUE};
		setLayout(gridBagLayout);

		JLabel lblUsvProcessing = new JLabel("LMT USV Toolbox");
		lblUsvProcessing.setFont(new Font("Tahoma", Font.BOLD, 27));
		GridBagConstraints gbc_lblUsvProcessing = new GridBagConstraints();
		gbc_lblUsvProcessing.insets = new Insets(0, 0, 5, 0);
		gbc_lblUsvProcessing.gridx = 0;
		gbc_lblUsvProcessing.gridy = 0;
		add(lblUsvProcessing, gbc_lblUsvProcessing);

		btnAddFilesTo = new JButton("Add files to batch processing");
		GridBagConstraints gbc_btnAddFilesTo = new GridBagConstraints();
		gbc_btnAddFilesTo.insets = new Insets(0, 0, 5, 0);
		gbc_btnAddFilesTo.gridx = 0;
		gbc_btnAddFilesTo.gridy = 1;
		add(btnAddFilesTo, gbc_btnAddFilesTo);

		scrollPane = new JScrollPane();
		scrollPane.setViewportBorder(new TitledBorder(null, "Input files for batch processing", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		GridBagConstraints gbc_scrollPane = new GridBagConstraints();
		gbc_scrollPane.fill = GridBagConstraints.BOTH;
		gbc_scrollPane.insets = new Insets(0, 0, 5, 0);
		gbc_scrollPane.gridx = 0;
		gbc_scrollPane.gridy = 2;
		add(scrollPane, gbc_scrollPane);

		textArea = new JTextArea();
		scrollPane.setViewportView(textArea);

		scrollPane_1 = new JScrollPane();
		scrollPane_1.setViewportBorder(new TitledBorder(null, "Processing log", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		GridBagConstraints gbc_scrollPane_1 = new GridBagConstraints();
		gbc_scrollPane_1.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_1.insets = new Insets(0, 0, 5, 0);
		gbc_scrollPane_1.gridx = 0;
		gbc_scrollPane_1.gridy = 3;
		add(scrollPane_1, gbc_scrollPane_1);

		textArea_1 = new JTextArea();
		scrollPane_1.setViewportView(textArea_1);
		
		splitPane = new JSplitPane();
		GridBagConstraints gbc_splitPane = new GridBagConstraints();
		gbc_splitPane.insets = new Insets(0, 0, 5, 0);
		gbc_splitPane.fill = GridBagConstraints.BOTH;
		gbc_splitPane.gridx = 0;
		gbc_splitPane.gridy = 4;
		add(splitPane, gbc_splitPane);
		
		lblNewLabel = new JLabel("options: ");
		splitPane.setLeftComponent(lblNewLabel);
		
		textField = new JTextField();
		splitPane.setRightComponent(textField);
		textField.setColumns(10);
		
		detailedOutputCheckBox = new JCheckBox("detailed output (folder with all data) (else simple .txt file)");
		GridBagConstraints gbc_detailedOutputCheckBox = new GridBagConstraints();
		gbc_detailedOutputCheckBox.insets = new Insets(0, 0, 5, 0);
		gbc_detailedOutputCheckBox.gridx = 0;
		gbc_detailedOutputCheckBox.gridy = 5;
		add(detailedOutputCheckBox, gbc_detailedOutputCheckBox);

		btnStartUsvProcessing = new JButton("Start USV processing");
		btnStartUsvProcessing.setFont(new Font("Tahoma", Font.BOLD, 18));
		GridBagConstraints gbc_btnStartUsvProcessing = new GridBagConstraints();
		gbc_btnStartUsvProcessing.gridx = 0;
		gbc_btnStartUsvProcessing.gridy = 6;
		add(btnStartUsvProcessing, gbc_btnStartUsvProcessing);
	}



	public JTextArea getTextArea() {
		return textArea;
	}
	public JButton getBtnAddFilesTo() {
		return btnAddFilesTo;
	}
	public JButton getBtnStartUsvProcessing() {
		return btnStartUsvProcessing;
	}


	public JTextArea getLogTextArea() {
		return textArea_1;
	}
	public JCheckBox getDetailedOutputCheckBox() {
		return detailedOutputCheckBox;
	}
	public JTextField getOptionField() {
		return textField;
	}
}
