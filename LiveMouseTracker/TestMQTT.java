package plugins.fab.mqtt;

import jogamp.opengl.util.pngj.ImageLine.SampleType;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import icy.plugin.abstract_.PluginActionable;

public class TestMQTT extends PluginActionable {

	@Override
	public void run() {
		
		String topic        = "MQTT Examples";
		String content      = "Message from MqttPublishSample";
		int qos             = 2;
		//String broker       = "tcp://iot.eclipse.org:1883";
		String broker       = "tcp://localhost:1883";
		String clientId     = "JavaSample";
		MemoryPersistence persistence = new MemoryPersistence();

		System.out.println("Test with:");
		System.out.println("web: http://emqtt.io/");		
		System.out.println("install: http://emqtt.io/docs/install.html");
		System.out.println("localhost dashboard: http://localhost:18083/");
		System.out.println("this sample: https://eclipse.org/paho/clients/java/");
		System.out.println("Localhost MQTT server: localhost:8083");
		try {
			MqttClient sampleClient = new MqttClient(broker, clientId, persistence);
			MqttConnectOptions connOpts = new MqttConnectOptions();
			connOpts.setCleanSession(true); 
			System.out.println("Connecting to broker: "+broker);
			connOpts.setUserName("fabTest");
			connOpts.setPassword("pass".toCharArray());
			sampleClient.connect(connOpts);
			System.out.println("Connected");
			System.out.println("Publishing message: "+content);
			MqttMessage message = new MqttMessage(content.getBytes());
			message.setQos(qos);
			sampleClient.publish(topic, message);
			System.out.println("Message published");
			
			sampleClient.subscribe("/World/#");
			sampleClient.setCallback( new MqttCallback() {
				
				@Override
				public void messageArrived(String arg0, MqttMessage arg1) throws Exception {
					System.out.println("--" + arg0 );
					System.out.println("Message arrived: " + arg1 );
				}
				
				@Override
				public void deliveryComplete(IMqttDeliveryToken arg0) {
					System.out.println("Delivery complete: " + arg0 );
				}
				
				@Override
				public void connectionLost(Throwable arg0) {
					System.out.println("Connection lost: " + arg0 );
					
				}
			});

//			sampleClient.disconnect();			
//			System.out.println("Disconnected");
			
		} catch(MqttException me) {
			System.out.println("reason "+me.getReasonCode());
			System.out.println("msg "+me.getMessage());
			System.out.println("loc "+me.getLocalizedMessage());
			System.out.println("cause "+me.getCause());
			System.out.println("excep "+me);
			me.printStackTrace();
		}

	}
	
	
}
