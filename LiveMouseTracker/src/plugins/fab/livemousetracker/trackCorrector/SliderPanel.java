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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JPanel;
import javax.swing.JSlider;

public class SliderPanel extends JPanel {
	private JSlider timeSlider;
	public SliderPanel() {
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{200, 0};
		gridBagLayout.rowHeights = new int[]{26, 0};
		gridBagLayout.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{1.0, Double.MIN_VALUE};
		setLayout(gridBagLayout);

		timeSlider = new JSlider();
		timeSlider.setMinorTickSpacing(30);
		timeSlider.setMajorTickSpacing(1800);
		timeSlider.setPaintTicks( true );
		timeSlider.setFocusable(false);
		GridBagConstraints gbc_timeSlider = new GridBagConstraints();
		gbc_timeSlider.fill = GridBagConstraints.HORIZONTAL;
		gbc_timeSlider.gridx = 0;
		gbc_timeSlider.gridy = 0;
		add(timeSlider, gbc_timeSlider);
	}

	public JSlider getTimeSlider() {
		return timeSlider;
	}
}
