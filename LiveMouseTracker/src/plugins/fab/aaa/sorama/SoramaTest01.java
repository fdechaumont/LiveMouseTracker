package plugins.fab.aaa.sorama;

import icy.plugin.abstract_.PluginActionable;

import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/*
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONException;
import org.json.JSONObject;
*/

import java.net.URI;
/*
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
 */

public class SoramaTest01 implements WebsocketMessageListener {

	String ip = "169.254.39.20";
	String id = null;
	WebsocketClientEndpoint clientEndPoint = null;
	public float[] previousSoundSurfaceData = new float[48*36];
	public float[] soundSurfaceData = new float[48*36];
	long previousT = 0;

	/*
	public void whenPostJsonUsingHttpClient_thenCorrect() 
			  throws ClientProtocolException, IOException {
			    CloseableHttpClient client = HttpClients.createDefault();
			    HttpPost httpPost = new HttpPost("http://www.example.com");

			    String json = "{"id":1,"name":"John"}";
			    StringEntity entity = new StringEntity(json);
			    httpPost.setEntity(entity);
			    httpPost.setHeader("Accept", "application/json");
			    httpPost.setHeader("Content-type", "application/json");

			    CloseableHttpResponse response = client.execute(httpPost);
			    assertThat(response.getStatusLine().getStatusCode(), equalTo(200));
			    client.close();
			}
	*/
	
	public void subscribeToSurface( String api_url )
	{
		try {

			URL url = new URL( api_url );
			HttpURLConnection http = (HttpURLConnection)url.openConnection();
			http.setRequestMethod("POST");
			http.setDoOutput(true);
			http.setRequestProperty("Accept", "application/json");
			
			String auth = "admin" + ":" + "admin";
			//byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.UTF_8));
			//String authHeaderValue = "Basic " + new String(encodedAuth);
			
			
			http.setRequestProperty( "Authorization", "Basic dXNlcjp1c2Vy"); //admin:admin
			//http.setRequestProperty("Authorization", authHeaderValue);
			http.setRequestProperty("Content-Type", "application/json");

			/*
			String data = "{\"callbackChannelId\": \""+ this.id +"\",\r\n"
					+ "        \"duration\": 0,\r\n"
					+ "        \"startTime\": \"2022-01-06T16:33:06.330Z\"}";
			*/
			
			String data = "{\"callbackChannelId\": \""+ this.id +"\",\r\n"
					+ "        \"duration\": 0,\r\n"
					+ "        \"startTime\": \"now\"}";
					

			System.out.println( data );
			byte[] out = data.getBytes(StandardCharsets.UTF_8);

			OutputStream stream = http.getOutputStream();
			stream.write(out);

			System.out.println(http.getResponseCode() + " " + http.getResponseMessage());
			
			http.disconnect();

		} 
		catch (IOException e) {
			e.printStackTrace();
		}


	}
	
	public void changeSurface( String ip )
	{
	    try {
	    	System.out.println("Change surface");
	    	String api_url = "http://"+ip+":9011/soundsurfaces/1";
			URL url = new URL( api_url );
			HttpURLConnection http = (HttpURLConnection)url.openConnection();
			http.setRequestMethod("PUT");
			http.setDoOutput(true);
			http.setRequestProperty("Accept", "application/json");
			
			String auth = "admin" + ":" + "admin";
			//byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.UTF_8));
			//String authHeaderValue = "Basic " + new String(encodedAuth);
			
			
			http.setRequestProperty( "Authorization", "Basic dXNlcjp1c2Vy"); //admin:admin
			//http.setRequestProperty("Authorization", authHeaderValue);
			http.setRequestProperty("Content-Type", "application/json");
			
			//String data = "{'weighting':'A','reportingInterval':37449143,'frequencyRanges':[{'from':70000,'to':77000}],'duration':0}";
			String data = "{'weighting':'A','reportingInterval':33333333,'frequencyRanges':[{'from':50000,'to':57000}],'duration':0}";
			//String data = "{'weighting':'A','reportingInterval':37449,'frequencyRanges':[{'from':20000,'to':27000}],'duration':0}";
			data = data.replace("'", "\"");
			
			System.out.println( data );
			byte[] out = data.getBytes(StandardCharsets.UTF_8);

			OutputStream stream = http.getOutputStream();
			stream.write(out);

			System.out.println(http.getResponseCode() + " " + http.getResponseMessage());
			
			http.disconnect();

		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	SoramaWindow soramaWindow = new SoramaWindow( this );
	
	public void run() {

		
		
		try {
			this.clientEndPoint = new WebsocketClientEndpoint(new URI ("ws://"+ip+":9012"));

			// add listener
			clientEndPoint.addWebsocketMessageListener( this );

			// send message to websocket
			clientEndPoint.sendMessage("hello");
			Thread.sleep( 500 ); // wait for the camera to answer.
			System.out.println( this.id );
			
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if ( this.id == null )
		{
			System.out.println("Can't get ID from camera.");
			return;
		}
		
		System.out.println("Change the surface");
		changeSurface( this.ip );

		
		System.out.println("Subscription to surfaces");
		
		
			String api_url = "http://"+ip+":9011/data/soundsurface/1/subscription";
			
			subscribeToSurface( api_url );
			
			/*
			{
				try {
					Thread.sleep( 1000 );
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}


		
		
		this.clientEndPoint.close();
		*/
		
	}

	@Override
	public void websocketMessageReceived(String message) {
		
		if ( message.contains("id") )
		{
			System.out.println("Receive ID");		
			this.id = message.substring( 7, 43 );
			System.out.println( "ID: " + this.id );
			return;
		}
				
		long currentT = Calendar.getInstance().getTimeInMillis();
		System.out.println( currentT - previousT );
		previousT = currentT;
		
		if ( message.contains("soundSurface" ) )
		{
			try {
				JSONObject jo;
				jo = new JSONObject(message);
				JSONArray ja = jo.getJSONArray("value");
				List<String> keys = new ArrayList<>();
				
				for(int i=0;i<ja.length();i++){
					float val = Float.parseFloat( ja.getString( i ) );
					this.soundSurfaceData[i]= val;
				}
				
				/*
				for(int i=0;i<ja.length();i++){
					float val = Float.parseFloat( ja.getString( i ) );
					val = val - this.previousSoundSurfaceData[i];
					if ( val < 0 ) val = 0;
					this.soundSurfaceData[i] = val;
					
				}

				for(int i=0;i<ja.length();i++){
					float val = Float.parseFloat( ja.getString( i ) );
					this.previousSoundSurfaceData[i]= val;
				}
				*/
				
				if (this.soramaWindow.pane != null)
				{
					this.soramaWindow.pane.repaint();
				}

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			

		}
		
		
		System.out.println( message.substring( 0 , 80 ) );
				
	}

}
