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

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import icy.image.IcyBufferedImage;
import icy.util.XMLUtil;
import plugins.fab.livemousetracker.Animal;
import plugins.fab.livemousetracker.LiveMouseTracker;
import plugins.fab.livemousetracker.detection.MouseDetection;
import plugins.fab.livemousetracker.listener.LiveTrackerListener;
import plugins.fab.livemousetracker.liveanalysis.DataResult;
import plugins.fab.livemousetracker.track.TrackSegment;

import java.io.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

public class ResultListenerSocket2 implements Runnable, LiveTrackerListener {


	Socket socket;
	String dataToSend = null;

	public ResultListenerSocket2( Socket socket ) {
		this.socket = socket;
//		System.out.println("Accepted Client Address - " + socket.getInetAddress().getHostName());

	}

	@Override
	public void run() {

		LiveMouseTracker.addTrackerListener( this );

		System.out.println("Processing result listener.");

		while( !LiveMouseTracker.performingShutDown )
		{
			try {

				Thread.sleep( 10 );

				if ( dataToSend != null )
				{

					//System.out.println("WRITING TCP DATA");
					OutputStream outputStream = socket.getOutputStream();
					DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
					dataOutputStream.writeUTF( dataToSend );
					dataOutputStream.flush(); // send the message
					// dataOutputStream.close(); // close the output stream when we're done.
					dataToSend = null;
				}
				/*
			DataAnswer dataAnswer = new DataAnswer();
			dataAnswer.toSocket( socket );
				 */

				//Element root = new Element("LMT");

				//			while( socket.getInputStream().available() == 0 )
				//			{
				//				Thread.sleep(10);
				//			}

				//			System.out.println( "Available: " + socket.getInputStream().available() );
				//			//while ( socket.is)
				//
				//			DataResult dataResult = DataResult.createFromSocket( socket );
				//			System.out.println( "The request is: " + dataResult.getPList() );

				//			DataAnswer dataAnswer = new DataAnswer();
				//			TrackSegment ts = new TrackSegment();
				//			for ( int t = 0 ; t < 50 ; t++ )
				//			{
				//				ts.addDetection( new RawMouseDetection( new ROI2DArea( new Point2D.Double( t , t ) ), t ));
				//			}
				//			dataAnswer.setTrackSegment( new TrackSegment() );
				//			dataAnswer.setPoint( 5 );
				//			dataAnswer.toSocket( socket );

				//			socket.close();


			}// catch (IOException | InterruptedException e) {
			    catch ( Exception e) {
				e.printStackTrace();
				LiveMouseTracker.log("TCP sending error: client disconnected ? - disconnecting");
				LiveMouseTracker.removeTrackerListener( this );
				break;
			}
		}

		try {
			OutputStream outputStream = socket.getOutputStream();
			DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
			dataOutputStream.writeUTF( "<root><end></root>" );
			dataOutputStream.flush(); // send the message
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void liveTrackerEndOfFrame(LiveMouseTracker liveMouseTracker) {

		// prepare data
		int currentT = liveMouseTracker.currentFrameInfo.getFrameNumber();
		long epochTimeStamp = liveMouseTracker.currentFrameInfo.getDateTime().getTime();

		try {

			Document document = XMLUtil.createDocument( true );
			Element lmt = XMLUtil.createRootElement( document, "lmt" );

			Element frame = XMLUtil.addElement( lmt, "frame");

			XMLUtil.setAttributeIntValue( frame, "number", currentT );
			XMLUtil.setAttributeLongValue( frame, "epochtimestamp", epochTimeStamp );

			for ( TrackSegment track : liveMouseTracker.trackContainer.getAllTracks( currentT ) )
			{
				MouseDetection detection = track.getDetection( currentT );

				float confidencyScore = 0;
				if ( track.getIdentityAffectedBy().equals("RFID") )
				{
					confidencyScore =1;
				}

				Element detectionElement = XMLUtil.addElement( frame , "detection" );
				XMLUtil.setAttributeFloatValue( detectionElement, "x", (float)detection.getMassCenter().getX() );
				XMLUtil.setAttributeFloatValue( detectionElement, "y", (float)detection.getMassCenter().getY() );
				XMLUtil.setAttributeFloatValue( detectionElement, "confidencyScore", confidencyScore );
				Animal animal = liveMouseTracker.trackContainer.animalTrackSegmentPool.getAnimalOwningTrack( track );
				int animalId = 0;
				String RFID = "";
				if ( animal != null )
				{
					animalId = (int) animal.getDataBaseId() ;
					if ( animal.getRfidID() != null )
					{
						RFID = animal.getRfidID();
					}
				}
				XMLUtil.setAttributeIntValue( detectionElement, "id", animalId );
				XMLUtil.setAttributeValue( detectionElement, "RFID", RFID );

			}

			DocumentBuilderFactory domFact = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = domFact.newDocumentBuilder();
			DOMSource domSource = new DOMSource(document);
			StringWriter writer = new StringWriter();
			StreamResult result = new StreamResult(writer);
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.transform(domSource, result);
			String outToTCP = writer.toString();
			// System.out.println("XML IN String format is: \n" + outToTCP );
			dataToSend = outToTCP;

		} catch ( ParserConfigurationException | TransformerException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void liveTrackerPostInitEvent(LiveMouseTracker liveMouseTracker) {
	}

	@Override
	public void liveTrackerPostProcessDetectionFiltering(ArrayList<MouseDetection> rawMouseDetectionList, int t,
			IcyBufferedImage depthImage) {
	}

}
