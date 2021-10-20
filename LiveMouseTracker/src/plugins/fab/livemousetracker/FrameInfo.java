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

import java.sql.Date;

public class FrameInfo {

	int frameNumber;
	Date date;
	/** Number of particle are the number of particle detected before fitlering. */
	int nbParticle;
	boolean pauseAllProcess;
	double temperature;
	double humidity;
	double sound;
	double lightVisible;
	double lightVisibleAndInfrared;

	public FrameInfo(int frameNumber, Date date, boolean pauseAllProcess,
			double temperature, double humidity, double sound, double lightVisible,
			double lightVisibleAndInfrared )
	{
		this.frameNumber = frameNumber;
		this.date = date;
		this.pauseAllProcess = pauseAllProcess;
		this.temperature = temperature;
		this.humidity = humidity;
		this.sound = sound;
		this.lightVisible = lightVisible;
		this.lightVisibleAndInfrared = lightVisibleAndInfrared;
	}

	public void setNbParticle( int nbParticle )
	{
		this.nbParticle = nbParticle;
	}

	public Date getTime() {
		return date;
	}

	public int getFrameNumber() {
		return frameNumber;
	}

	public Date getDateTime() {
		return date;
	}

	public boolean isPauseAllProcess() {
		return pauseAllProcess;
	}

	/** return the Number of particle are the number of particle detected before fitlering. */
	public int getNbParticle() {
		return nbParticle;
	}

	public void addParticle() {
		nbParticle++;
	}

	public double getTemperature() {
		return temperature;
	}

	public double getHumidity() {
		return humidity;
	}

	public double getLightVisible() {
		return lightVisible;
	}

	public double getLightVisibleAndInfrared() {
		return lightVisibleAndInfrared;
	}

	public double getSound() {
		return sound;
	}

}
