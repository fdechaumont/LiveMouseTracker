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
package plugins.fab.livemousetracker.MPEGRecorder;

import icy.gui.dialog.SaveDialog;
import icy.plugin.abstract_.PluginActionable;
import icy.system.thread.Processor;

public class MPEGRecorderGUI extends PluginActionable {

	public MPEGRecorderGUI() {

	}

	@Override
	public void run() {

		SaveDialog save = new SaveDialog();
		final String file = save.chooseFile();

		if ( file == null ) return;

		Processor p = new Processor();
		p.setThreadName("MPEG Rec");
		p.submit( new Runnable() {

			@Override
			public void run() {
				MPEGRecorderSequence recorder = new MPEGRecorderSequence( file , getActiveSequence() );

			}
		});


	}
}
