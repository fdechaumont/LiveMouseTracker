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
package plugins.fab.livemousetracker.liveanalysis.client;

import icy.plugin.abstract_.PluginActionable;
import icy.plugin.interface_.PluginThreaded;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import plugins.fab.livemousetracker.liveanalysis.DataRequest;
import plugins.fab.livemousetracker.liveanalysis.DataRequestType;

public class LiveAnalysisClientTest extends PluginActionable implements PluginThreaded {

	public LiveAnalysisClientTest() {

	}

	@Override
	public void run() {

		System.out.println("Live Analysis Client launched.");
		try {
			Socket clientSocket = new Socket("127.0.0.1", 7101 );

			DataRequest request = new DataRequest();
			request.setDataType( DataRequestType.FULL_POSITION_SET );
			request.toSocket( clientSocket );

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}


	}

}
