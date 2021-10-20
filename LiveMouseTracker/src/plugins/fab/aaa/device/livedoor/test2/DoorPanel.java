package plugins.fab.aaa.device.livedoor.test2;

import javax.swing.JPanel;
import java.awt.GridBagLayout;
import javax.swing.JTextField;
import java.awt.GridBagConstraints;
import javax.swing.JLabel;
import java.awt.Insets;
import javax.swing.JButton;
import java.awt.Font;
import javax.swing.JCheckBox;
import javax.swing.JProgressBar;
import javax.swing.border.EmptyBorder;
import javax.swing.JScrollBar;
import javax.swing.JSlider;

public class DoorPanel extends JPanel {
	private JSlider slider;
	private JSlider slider_1;
	private JSlider slider_2;
	private JSlider slider_3;
	private JCheckBox chckbxRepeat;
	private JButton btnGoUp;
	private JButton btnGoDown;
	private JLabel label;
	private JProgressBar progressBar;
	private JLabel lblNewLabel;
	private JLabel title;
	public DoorPanel() {
		setBorder(new EmptyBorder(5, 5, 5, 5));
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 0, 0};
		gridBagLayout.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
		gridBagLayout.columnWeights = new double[]{1.0, 1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		setLayout(gridBagLayout);

		title = new JLabel("Door test 2");
		title.setFont(new Font("Tahoma", Font.BOLD, 21));
		GridBagConstraints gbc_title = new GridBagConstraints();
		gbc_title.gridwidth = 2;
		gbc_title.insets = new Insets(0, 0, 5, 0);
		gbc_title.gridx = 0;
		gbc_title.gridy = 0;
		add(title, gbc_title);

		JLabel lblStart = new JLabel("start");
		GridBagConstraints gbc_lblStart = new GridBagConstraints();
		gbc_lblStart.insets = new Insets(0, 0, 5, 5);
		gbc_lblStart.gridx = 0;
		gbc_lblStart.gridy = 1;
		add(lblStart, gbc_lblStart);

		slider = new JSlider();
		slider.setMajorTickSpacing(100);
		slider.setMinorTickSpacing(100);
		slider.setMaximum(1000);
		slider.setValue(0);
		slider.setPaintLabels(true);
		slider.setPaintTicks(true);
		GridBagConstraints gbc_slider = new GridBagConstraints();
		gbc_slider.fill = GridBagConstraints.HORIZONTAL;
		gbc_slider.insets = new Insets(0, 0, 5, 0);
		gbc_slider.gridx = 1;
		gbc_slider.gridy = 1;
		add(slider, gbc_slider);

		JLabel lblEnd = new JLabel("end");
		GridBagConstraints gbc_lblEnd = new GridBagConstraints();
		gbc_lblEnd.insets = new Insets(0, 0, 5, 5);
		gbc_lblEnd.gridx = 0;
		gbc_lblEnd.gridy = 2;
		add(lblEnd, gbc_lblEnd);

		slider_1 = new JSlider();
		slider_1.setMinorTickSpacing(100);
		slider_1.setMajorTickSpacing(100);
		slider_1.setValue(0);
		slider_1.setMaximum(1000);
		slider_1.setPaintLabels(true);
		slider_1.setPaintTicks(true);
		GridBagConstraints gbc_slider_1 = new GridBagConstraints();
		gbc_slider_1.fill = GridBagConstraints.HORIZONTAL;
		gbc_slider_1.insets = new Insets(0, 0, 5, 0);
		gbc_slider_1.gridx = 1;
		gbc_slider_1.gridy = 2;
		add(slider_1, gbc_slider_1);

		JLabel lblSpeed = new JLabel("speed");
		GridBagConstraints gbc_lblSpeed = new GridBagConstraints();
		gbc_lblSpeed.insets = new Insets(0, 0, 5, 5);
		gbc_lblSpeed.gridx = 0;
		gbc_lblSpeed.gridy = 3;
		add(lblSpeed, gbc_lblSpeed);

		slider_2 = new JSlider();
		slider_2.setMajorTickSpacing(100);
		slider_2.setValue(0);
		slider_2.setMaximum(1000);
		slider_2.setPaintTicks(true);
		slider_2.setPaintLabels(true);
		GridBagConstraints gbc_slider_2 = new GridBagConstraints();
		gbc_slider_2.fill = GridBagConstraints.HORIZONTAL;
		gbc_slider_2.insets = new Insets(0, 0, 5, 0);
		gbc_slider_2.gridx = 1;
		gbc_slider_2.gridy = 3;
		add(slider_2, gbc_slider_2);

		JLabel lblMaxTorque = new JLabel("max torque");
		GridBagConstraints gbc_lblMaxTorque = new GridBagConstraints();
		gbc_lblMaxTorque.insets = new Insets(0, 0, 5, 5);
		gbc_lblMaxTorque.gridx = 0;
		gbc_lblMaxTorque.gridy = 4;
		add(lblMaxTorque, gbc_lblMaxTorque);

		slider_3 = new JSlider();
		slider_3.setMajorTickSpacing(100);
		slider_3.setValue(0);
		slider_3.setMaximum(1000);
		slider_3.setPaintLabels(true);
		slider_3.setPaintTicks(true);
		GridBagConstraints gbc_slider_3 = new GridBagConstraints();
		gbc_slider_3.fill = GridBagConstraints.HORIZONTAL;
		gbc_slider_3.insets = new Insets(0, 0, 5, 0);
		gbc_slider_3.gridx = 1;
		gbc_slider_3.gridy = 4;
		add(slider_3, gbc_slider_3);

		btnGoUp = new JButton("Go Up");
		GridBagConstraints gbc_btnGoUp = new GridBagConstraints();
		gbc_btnGoUp.insets = new Insets(0, 0, 5, 0);
		gbc_btnGoUp.gridx = 1;
		gbc_btnGoUp.gridy = 5;
		add(btnGoUp, gbc_btnGoUp);

		btnGoDown = new JButton("Go Down");
		GridBagConstraints gbc_btnGoDown = new GridBagConstraints();
		gbc_btnGoDown.insets = new Insets(0, 0, 5, 0);
		gbc_btnGoDown.gridx = 1;
		gbc_btnGoDown.gridy = 6;
		add(btnGoDown, gbc_btnGoDown);

		chckbxRepeat = new JCheckBox("repeat (3 sec cycle)");
		GridBagConstraints gbc_chckbxRepeat = new GridBagConstraints();
		gbc_chckbxRepeat.insets = new Insets(0, 0, 5, 0);
		gbc_chckbxRepeat.gridx = 1;
		gbc_chckbxRepeat.gridy = 7;
		add(chckbxRepeat, gbc_chckbxRepeat);

		JLabel lblCurrentPosition = new JLabel("Current Position:");
		GridBagConstraints gbc_lblCurrentPosition = new GridBagConstraints();
		gbc_lblCurrentPosition.insets = new Insets(0, 0, 5, 5);
		gbc_lblCurrentPosition.gridx = 0;
		gbc_lblCurrentPosition.gridy = 9;
		add(lblCurrentPosition, gbc_lblCurrentPosition);

		lblNewLabel = new JLabel("0");
		lblNewLabel.setFont(new Font("Tahoma", Font.PLAIN, 26));
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.insets = new Insets(0, 0, 5, 0);
		gbc_lblNewLabel.gridx = 1;
		gbc_lblNewLabel.gridy = 9;
		add(lblNewLabel, gbc_lblNewLabel);

		JLabel lblCurrentForce = new JLabel("Current Force:");
		GridBagConstraints gbc_lblCurrentForce = new GridBagConstraints();
		gbc_lblCurrentForce.insets = new Insets(0, 0, 5, 5);
		gbc_lblCurrentForce.gridx = 0;
		gbc_lblCurrentForce.gridy = 10;
		add(lblCurrentForce, gbc_lblCurrentForce);

		label = new JLabel("0");
		label.setFont(new Font("Tahoma", Font.BOLD, 26));
		GridBagConstraints gbc_label = new GridBagConstraints();
		gbc_label.insets = new Insets(0, 0, 5, 0);
		gbc_label.gridx = 1;
		gbc_label.gridy = 10;
		add(label, gbc_label);

		JLabel lblCourse = new JLabel("Course:");
		GridBagConstraints gbc_lblCourse = new GridBagConstraints();
		gbc_lblCourse.insets = new Insets(0, 0, 0, 5);
		gbc_lblCourse.gridx = 0;
		gbc_lblCourse.gridy = 11;
		add(lblCourse, gbc_lblCourse);

		progressBar = new JProgressBar();
		GridBagConstraints gbc_progressBar = new GridBagConstraints();
		gbc_progressBar.fill = GridBagConstraints.BOTH;
		gbc_progressBar.gridx = 1;
		gbc_progressBar.gridy = 11;
		add(progressBar, gbc_progressBar);
	}




	public JSlider startSlider() {
		return slider;
	}
	public JSlider endSlider() {
		return slider_1;
	}
	public JSlider speedSlider() {
		return slider_2;
	}
	public JSlider maxTorqueSlider() {
		return slider_3;
	}
	public JCheckBox repeatChckbx() {
		return chckbxRepeat;
	}
	public JButton upButton() {
		return btnGoUp;
	}
	public JButton downButton() {
		return btnGoDown;
	}
	public JLabel forceLabel() {
		return label;
	}
	public JProgressBar courseProgressBar() {
		return progressBar;
	}
	public JLabel positionLabel() {
		return lblNewLabel;
	}
	public JLabel getTitle() {
		return title;
	}
}
