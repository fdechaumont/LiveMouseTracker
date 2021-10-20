package plugins.fab.aaa.voc;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.lang.RandomStringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import icy.main.Icy;
import icy.plugin.abstract_.Plugin;
import icy.plugin.abstract_.PluginActionable;
import icy.plugin.interface_.PluginThreaded;
import io.socket.client.IO;
import io.socket.client.Manager;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.Transport;
import io.socket.engineio.client.transports.Polling;
import io.socket.engineio.client.transports.WebSocket;
import javassist.tools.web.Webserver;
import okhttp3.OkHttpClient;
import plugins.fab.aaa.voc.VocalizationOverlay.DrawMode;

public class WebServiceVocProcessing extends PluginActionable implements PluginThreaded {

	Socket socket;
	boolean isBusy = false;

	Processing processingThread = null;
	String processorId = null;
	boolean localMode = false;

	class Processing extends Thread implements VocAnalysisListener
	{
		File inputFile = null;
		String outputFolder = null;
		String jobId;
		boolean errorDuringProcess = false;

		public Processing(File inputFile, String outputFolder, String jobId ) {
			WebServiceVocProcessing.this.setBusy( true );
			this.inputFile= inputFile;
			this.outputFolder = outputFolder;
			this.jobId = jobId;
		}

		@Override
		public void run() {

			try{
				System.out.println("Starting processing thread..." );
				System.out.println("File: " + inputFile );
				System.out.println("OutputFolder: " + outputFolder );

				FullVocProcessor processor = new FullVocProcessor( false , null );
				if ( inputFile.getAbsoluteFile().toString().toLowerCase().contains("pup") )
				{
					processor.setPupMode( true );
					System.out.println("PUP MODE activated");
				}
				processor.addVocAnalysisListener ( this );
				processor.setProcessAsWebProcessor( true );
				processor.setWebOutProcessorFolder( outputFolder );
				processor.setSaveHTML( true );
				processor.setDrawMode( DrawMode.WEB );
				processor.MANAGE_GROUND_TRUTH = false;
				processor.setCloseAfterProcessing( true );
				processor.SAVE_DATA_EXTRACTED = false;

				try
				{
					processor.process( inputFile );
				}catch( java.lang.OutOfMemoryError e )
				{
					e.printStackTrace();
					sendProcessingErrorToServer("Not enough memory (RAM) on usv-worker." , jobId );
				}

				sendProcessingDoneToServer("done" , processor , jobId );
				System.out.println("--------------- DONE");
			}
			catch( USVProcessingException e )
			{
				System.out.println( "******* Error " + e.getMessage() );
				sendProcessingErrorToServer( e.getMessage() , jobId );
				errorDuringProcess = true;
			}
			catch( Exception e )
			{
				sendProcessingErrorToServer("Unknown error during processing." , jobId );
				errorDuringProcess = true;
				e.printStackTrace();
			}finally {

				WebServiceVocProcessing.this.setBusy( false );
			}
		}

		@Override
		public void onAnalysisStatusUpdate(String status) {
			sendProcessingLogToServer( status, jobId );
		}
	}

	void sendBusyStateToServer(  )
	{
		System.out.println("Sending busy state: " + isBusy );

		JSONObject jsonMessage = new JSONObject();
		try {
			jsonMessage.put("busy", isBusy );
			jsonMessage.put("processorId", this.processorId );
			socket.emit("USVProcessorBusy", jsonMessage );

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public void setBusy(boolean b) {
		this.isBusy = b;
		sendBusyStateToServer();
	}

	void sendProcessingLogToServer( String log, String jobId )
	{
		System.out.println("Sending processing log: " + log );

		JSONObject jsonMessage = new JSONObject();
		try {
			jsonMessage.put("log", log );
			jsonMessage.put("jobId", jobId );
			jsonMessage.put("processorId", this.processorId );
			socket.emit("USVProcessorLog", jsonMessage );

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	String changeFilePathToStatic(String path)
	{
		System.out.println( "Path to static: " + path );
		return "static"+path.split("static")[1];
	}

	void sendProcessingErrorToServer( String error , String jobId )
	{
		JSONObject jsonMessage = new JSONObject();
		try {
			jsonMessage.put("error", error);
			jsonMessage.put("jobId", jobId );
			jsonMessage.put("processorId", this.processorId );
		} catch (JSONException e) {
			e.printStackTrace();
		}
		socket.emit("USVProcessorError", jsonMessage );
	}

	void sendProcessingDoneToServer( String log, FullVocProcessor processor, String jobId )
	{

		ArrayList<String> spectroImages = processor.spectroImageFiles;
		ArrayList<String> spectroWithOverlayImages = processor.spectroImageWithOverlayFiles;

		System.out.println("Sending processing log: " + log );

		JSONObject jsonMessage = new JSONObject();
		try {
			jsonMessage.put("log", log );

			// SPECTRO IMAGES
			JSONArray spectroImagesArray = new JSONArray();
			for ( String s : spectroImages )
			{
				spectroImagesArray.put( changeFilePathToStatic( s ) );
			}
			jsonMessage.put("spectroImages", spectroImagesArray );

			JSONArray spectroOverlayImagesArray = new JSONArray();
			for ( String s : spectroWithOverlayImages )
			{
				spectroOverlayImagesArray.put( changeFilePathToStatic( s ) );
			}
			jsonMessage.put("spectroOverlayImages", spectroOverlayImagesArray );

			// GRAPH
			JSONArray vocStart = new JSONArray();
			JSONArray vocDurations = new JSONArray();
			JSONArray vocFreqMin = new JSONArray();
			JSONArray vocFreqMean = new JSONArray();
			JSONArray vocFreqMax = new JSONArray();
			JSONArray vocStartFreq = new JSONArray();
			JSONArray vocEndFreq = new JSONArray();
			JSONArray vocPeakFreq = new JSONArray();
			JSONArray vocPeakPower = new JSONArray();
			JSONArray vocMeanPower = new JSONArray();
			JSONArray vocNbModulation = new JSONArray();
			JSONArray vocNbJump = new JSONArray();

			for ( Voc voc : processor.getAllAudioVocDetectionList() )
			{
				vocStart.put( voc.getStartInMs() );
				vocDurations.put( voc.getDurationInMs() );
				vocFreqMin.put( voc.getMinFrequencyInHz() );
				vocFreqMean.put( voc.getMeanFrequencyInHz() );
				vocFreqMax.put( voc.getMaxFrequencyInHz() );
				vocStartFreq.put( voc.getStartFrequencyInHz() );
				vocEndFreq.put( voc.getEndFrequencyInHz() );
				vocPeakFreq.put( voc.peakFrequency );
				vocPeakPower.put( voc.peakPower );
				vocMeanPower.put( voc.meanPower );

				vocNbModulation.put( voc.nbModulation );
				vocNbJump.put( voc.jumpList.size() );

			}

			jsonMessage.put("vocStart", vocStart );
			jsonMessage.put("vocDurationsMs", vocDurations );
			jsonMessage.put("vocMinFrequencies", vocFreqMin );
			jsonMessage.put("vocMeanFrequencies", vocFreqMean );
			jsonMessage.put("vocMaxFrequencies", vocFreqMax );

			jsonMessage.put("vocStartFreq", vocStartFreq );
			jsonMessage.put("vocEndFreq", vocEndFreq );
			jsonMessage.put("vocPeakFreq", vocPeakFreq );
			jsonMessage.put("vocPeakPower", vocPeakPower );
			jsonMessage.put("vocMeanPower", vocMeanPower );

			jsonMessage.put("vocNbModulation", vocNbModulation );
			jsonMessage.put("vocNbJump", vocNbJump );

			// GRAPH

			jsonMessage.put("totalNbVoc", processor.getAllAudioVocDetectionList().size() );

			int nbModulated = 0;
			int nbWithJump = 0;
			for ( Voc voc : processor.getAllAudioVocDetectionList() )
			{
				if ( voc.nbModulation > 0 )
				{
					nbModulated++;
				}
				if ( voc.jumpList.size() > 0 )
				{
					nbWithJump++;
				}
			}
			jsonMessage.put("nbModulated", nbModulated );
			jsonMessage.put("nbWithJump", nbWithJump );

			jsonMessage.put("wavDurationMs", processor.getTotalDurationInMilliSecond() );

			jsonMessage.put("jobId", jobId );
			jsonMessage.put("processorId", this.processorId );

			socket.emit("USVProcessorDone", jsonMessage );

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}



	@Override
	public void run() {
		System.out.println("Starting WebServiceVocProcessing...");

		for ( String arg : Icy.getCommandLinePluginArgs() )
		{
			if(  arg.contains("-local") )
			{
				this.localMode = true;
				System.out.println("Starting in local mode");
			}else
			{
				System.out.println("Starting in online server mode");
			}
		}

		//String test = "C:/Users/Fab/eclipse-workspace_python/USVweb/webUSV/static/result/vlTXbryb/";
		//System.out.println( changeFilePathToStatic(test) );


		// connect to server



		/*
		System.out.println("Starting HeadlessVocProcessing...");

		File fileIn = new File( Icy.getCommandLinePluginArgs()[0] );

		System.out.println( "File in : " + fileIn );

		 */

//		Socket socket;
		try {

			OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    //.hostnameVerifier(hostnameVerifier)
                    //.sslSocketFactory((SSLSocketFactory) SSLSocketFactory.getDefault(), trustManager)
                    .connectTimeout(20, TimeUnit.SECONDS)
                    .writeTimeout(1, TimeUnit.MINUTES)
                    .readTimeout(1, TimeUnit.MINUTES)
                    .build();
            IO.setDefaultOkHttpCallFactory(okHttpClient);
            IO.setDefaultOkHttpWebSocketFactory(okHttpClient);

			// Sending an object
//			JSONObject obj = new JSONObject();
//			obj.put("joinExternalProcessor", "hi");
			//obj.put("binary", new byte[42]);

            /*
            IO.Options opts = new IO.Options();
            opts.forceNew = true;
            */

            if ( localMode )
            {
            	socket = IO.socket("http://127.0.0.1:5000"  );
            }else
            {
            	// production mode
            	socket = IO.socket("http://usvdemo:5000");
            }


			socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

				@Override
				public void call(Object... args) {
					System.out.println("Joining...");

					JSONObject jsonMessage = new JSONObject();
					try {
						jsonMessage.put("name", "USV Segmenter");
						jsonMessage.put("version", "1.0");
						jsonMessage.put("API-key", "AAAAA-BBBBB-AAAAA-BBBBB");

						socket.emit("joinExternalUSVProcessor", jsonMessage );

					} catch (JSONException e) {
						e.printStackTrace();
					}

					sendBusyStateToServer();
				}

			}).on("welcome", new Emitter.Listener() {

				@Override
				public void call(Object... args) {
					System.out.println("New event:");

					for ( Object arg : args )
					{
						System.out.println( arg );
					}


					for ( Object arg : args )
					{
						JSONObject JSONparam = (JSONObject) arg;
						if ( JSONparam.has("processorId") )
						{
							try {
								WebServiceVocProcessing.this.processorId = (String)JSONparam.get("processorId");
								System.out.println("ProcessorId: " + WebServiceVocProcessing.this.processorId );
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				}

			});

			socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

				@Override
				public void call(Object... args) {

					System.out.println("Disconnected from server:");
					for ( Object arg : args )
					{
						System.out.println( arg );
					}
				}

			});

			/*
			socket.on("heartBeat", new Emitter.Listener() {
				@Override
				public void call(Object... args) {
					JSONObject jsonMessage = new JSONObject();
					try {
						System.out.println("Sending heartBeat");
						jsonMessage.put("processorId", WebServiceVocProcessing.this.processorId );
						socket.emit("USVProcessorHeartBeat", jsonMessage );
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			});
			*/

			socket.on("processJob", new Emitter.Listener() {

				@Override
				public void call(Object... args) {

					System.out.println("Received process job");
					File inputFile = null;
					String outputFolder = null;
					String jobId = null;

					for ( Object arg : args )
					{
						JSONObject JSONparam = (JSONObject) arg;
						if ( JSONparam.has("file") )
						{
							try {
								inputFile = new File( (String)JSONparam.get("file") );
								System.out.println("File received for job: " + inputFile.getAbsoluteFile() );
							} catch (JSONException e) {
								e.printStackTrace();
							}
						}
						if ( JSONparam.has("outputFolder") )
						{
							try {
								outputFolder = (String)JSONparam.get("outputFolder");
							} catch (JSONException e) {
								e.printStackTrace();
							}
						}
						if ( JSONparam.has("jobId") )
						{
							try {
								jobId = (String)JSONparam.get("jobId");
							} catch (JSONException e) {
								e.printStackTrace();
							}
						}
					}

					sendProcessingLogToServer("Start processing ..." , jobId );

					processingThread = new Processing( inputFile, outputFolder , jobId );
					processingThread.start();


				}

			});


			socket.connect(  );


		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("Waiting for jobs...");

		while ( true )
		{
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		// socket.disconnect();

	}


}
