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

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

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


public class ObjectTransferTestSocketClient extends LiveAnalysisAbstract {

	public ObjectTransferTestSocketClient() {

	}

	@Override
	public void run() {

		// test
//		DataRequest dataRequest = new DataRequest(  );
//		dataRequest.setDataType( DataType.FULL_POSITION_SET );


		Socket socket;
		try {
			socket = new Socket("127.0.0.1", 7101);
			socket.getInputStream();
		//	jaxbObjectToSocket( dataRequest , socket );
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}



//		System.out.println("--- document ---");
//		outPutDoc( doc );
//		System.out.println("---");

//		DataRequest newOne = jaxbXMLToObject ( doc );
//		System.out.println( "test ok: " + newOne.getDataType() );




	}

	 private DataRequest jaxbXMLToObject( Document doc ) {
	        try {
	            JAXBContext context = JAXBContext.newInstance(DataRequest.class);
	            Unmarshaller un = context.createUnmarshaller();
	            DataRequest emp = (DataRequest) un.unmarshal( doc.getDocumentElement() );
	            return emp;
	        } catch (JAXBException e) {
	            e.printStackTrace();
	        }
	        return null;
	    }

	void outPutDoc( Document doc )
	{
		try {

			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer t;
			t = tf.newTransformer();
			DOMSource source = new DOMSource(doc );
			StreamResult result = new StreamResult(System.out);
			t.transform(source, result);

		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
	}


	private void jaxbObjectToSocket(DataRequest dataRequest , Socket socket ) {

		Document doc = XMLUtil.createDocument( false );

		try {
		JAXBContext context = JAXBContext.newInstance( DataRequest.class );
        Marshaller m = context.createMarshaller();
        //for pretty-print XML in JAXB
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        // Write to System.out for debugging
        //XMLUtil.addElement(node, name, value)
        m.marshal(dataRequest, System.out );

        m.marshal(dataRequest, socket.getOutputStream() );

//		OutputStream os = socket.getOutputStream();
		//os.write(sb.toString().getBytes());
//		os.flush();

      //  m.marshal(dataRequest, doc );

        // Write to File
//			m.marshal( dataRequest, new File("c:/test.xml") );
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
