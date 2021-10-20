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
package plugins.fab.livemousetracker.liveanalysis;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.StringWriter;
import java.net.Socket;
import java.util.ArrayList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;

import plugins.fab.livemousetracker.liveanalysis.server.Point;


@XmlRootElement(name = "DataAnswer")
//@XmlType(propOrder = {"Point"})
public class DataResult {

//	TrackSegment trackSegment;

	ArrayList<Point> pList = new ArrayList<Point>();

	public void setPList( ArrayList<Point> pList )
	{
		this.pList = pList;
	}

	public ArrayList<Point> getPList()
	{
		return pList;
	}

	public String toXMLString( )
	{
		try {
			JAXBContext context = JAXBContext.newInstance( DataResult.class );
			Marshaller m = context.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			m.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");

			StringWriter sw = new StringWriter();

			m.marshal( this , sw );

			return sw.toString();

		} catch (JAXBException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void toSocket( Socket socket )
	{
		try {
			JAXBContext context = JAXBContext.newInstance( DataResult.class );
			Marshaller m = context.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
//			m.marshal( this, System.out );

			m.marshal( this, socket.getOutputStream() );
			socket.getOutputStream().flush();
			socket.close();

		} catch (JAXBException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public static DataResult createFromSocket(Socket socket) {

		try {
			JAXBContext context = JAXBContext.newInstance(DataResult.class);
			Unmarshaller un = context.createUnmarshaller();
			DataResult dataAnswer = (DataResult) un.unmarshal( socket.getInputStream() );
			return dataAnswer;
		} catch (JAXBException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;

	}

}
