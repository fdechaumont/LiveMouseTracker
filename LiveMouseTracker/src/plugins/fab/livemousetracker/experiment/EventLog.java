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
package plugins.fab.livemousetracker.experiment;

import plugins.fab.livemousetracker.Animal;
import plugins.fab.livemousetracker.LiveMouseTracker;

/** Event log are store in database as log. */
public class EventLog {

	public int startFrame;
	public int endFrame;
	public Animal animalA;
	public Animal animalB;
	public Animal animalC;
	public Animal animalD;
	public String name="";
	public String description="";

	public EventLog( String name, Animal animalA ) {
		this.startFrame = LiveMouseTracker.getT();
		this.endFrame = LiveMouseTracker.getT();
		this.name = name;
		this.animalA = animalA;
	}

	public EventLog(String string, Animal animal, int startT) {
		this( string, animal );
		this.startFrame = startT;
		this.endFrame = startT;
	}

	public EventLog(String string, Animal animal, int startT, int endT, String description ) {
		this( string, animal );
		this.startFrame = startT;
		this.endFrame = endT;
		this.description = description;
		System.out.println("New event log : " + string + " / " + description );
	}

	public int getStartFrame() {
		return startFrame;
	}

	public int getEndFrame() {
		return endFrame;
	}

}
