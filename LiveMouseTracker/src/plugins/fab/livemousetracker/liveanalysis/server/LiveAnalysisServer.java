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
package plugins.fab.livemousetracker.liveanalysis.server;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import icy.plugin.abstract_.PluginActionable;
import icy.system.thread.Processor;
import icy.util.XMLUtil;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


import org.w3c.dom.Document;

/**
 * Server of Live data to client via sockets.
 * @author Fab
 *
 */
public class LiveAnalysisServer implements Runnable {

	private ArrayList<Client> clientList = new ArrayList<Client>();

	public LiveAnalysisServer() {

	}

	@Override
	public void run() {

		try {

			System.out.println("Live Analysis Server Started.");

			ServerSocket socket;
			socket = new ServerSocket( 7101 );
			System.out.println("Server listenning on port " + socket.getLocalPort() );

			while ( !Thread.interrupted() )
			{
//				System.out.println("server loop");
				Socket socketToUse = socket.accept();

				Processor p = new Processor( 1 );
				p.execute( new ClientSocketRequestRegisterer( socketToUse , this ) );
			}

			socket.close();

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void addClient(Client client) {

		System.out.println( "Client added to Live Analysis server.");

		synchronized ( clientList ) {
			clientList.add( client );
		}

	}


}
