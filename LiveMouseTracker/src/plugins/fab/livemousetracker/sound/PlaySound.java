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
package plugins.fab.livemousetracker.sound;

import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import icy.plugin.abstract_.Plugin;
import icy.system.thread.ThreadUtil;
import plugins.fab.livemousetracker.LiveMouseTracker;

public class PlaySound {

	public static void play( final String sampleFileName )
	{
		if( !LiveMouseTracker.SOUND_ENABLED ) return;

		ThreadUtil.bgRun( new Runnable() {

			@Override
			public void run() {

				Plugin plugin = LiveMouseTracker.plugin;
				try {
					URL url = plugin.getResource("plugins/fab/livebeat/"+ sampleFileName + ".wav" );

					Line.Info linfo = new Line.Info(Clip.class);
					Line line = AudioSystem.getLine(linfo);
					Clip clip = (Clip) line;

					AudioInputStream ais;
					ais = AudioSystem.getAudioInputStream( url );
					clip.open(ais);
					clip.start();


				} catch (UnsupportedAudioFileException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				} catch (LineUnavailableException e1) {
					e1.printStackTrace();
				}

			}
		});


	}




}
