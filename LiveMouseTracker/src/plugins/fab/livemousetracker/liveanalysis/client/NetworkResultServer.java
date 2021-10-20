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

import icy.system.thread.Processor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import plugins.fab.livemousetracker.liveanalysis.server.ClientSocketRequestRegisterer;

public class NetworkResultServer implements Runnable {

	@Override
	public void run() {

		// start a server to receive info.

		try {

			System.out.println("Live Analysis Result listener server started.");

			ServerSocket socket;
			socket = new ServerSocket( 55044 ); // 7101
			System.out.println("Result server listenning on port " + socket.getLocalPort() );

			while ( !Thread.interrupted() )
			{
				Socket socketToUse = socket.accept();

				Processor p = new Processor( 1 );
				p.execute( new ResultListenerSocket2( socketToUse ) );
			}

			socket.close();

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
