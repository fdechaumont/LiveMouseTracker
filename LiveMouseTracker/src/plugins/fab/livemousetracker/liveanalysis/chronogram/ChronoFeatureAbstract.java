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
package plugins.fab.livemousetracker.liveanalysis.chronogram;

public abstract class ChronoFeatureAbstract {

	public EventTimeLine eventTimeLine;
	protected String name;
	protected TimeLineDataType timeLineDataType;
	protected String description;

	public ChronoFeatureAbstract( String name , String description , TimeLineDataType timeLineDataType ) {
		this.name = name;
		this.timeLineDataType = timeLineDataType;
		this.description = description;
	}

	public String getName()
	{
		return name;
	}

	public EventTimeLine getEventTimeLine()
	{
		return eventTimeLine;
	}

	public TimeLineDataType getTimeLineDataType() {
		return timeLineDataType;
	}

	public String getDescription() {
		return description;
	}

}
