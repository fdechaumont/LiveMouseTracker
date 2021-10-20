package plugins.fab.aaa.voc.family;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import javax.swing.JButton;
import javax.swing.border.LineBorder;
import java.awt.Color;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import java.awt.FlowLayout;

public class FamilyVocPanel extends JPanel {
	private JButton btnLoadVoc;
	private JScrollPane scrollPane;
	private JPanel resultPanel;
	public FamilyVocPanel() {
		setLayout(new BorderLayout(0, 0));

		JPanel controlPanel = new JPanel();
		add(controlPanel, BorderLayout.WEST);

		btnLoadVoc = new JButton("Load voc");
		controlPanel.add(btnLoadVoc);

		scrollPane = new JScrollPane();
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		add(scrollPane, BorderLayout.CENTER);

		resultPanel = new JPanel();

		scrollPane.setViewportView(resultPanel);
		resultPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
	}

	public JButton loadVocButton() {
		return btnLoadVoc;
	}
	public JPanel resultPanel() {
		return resultPanel;
	}
}
