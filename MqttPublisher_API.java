package MiniProject;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class MqttPublisher_API implements MqttCallback{ // implement callback �߰� & �ʿ��� �޼ҵ� ����
	static MqttClient sampleClient;// Mqtt Client ��ü ����
	
    public static void main(String[] args) {
    	MqttPublisher_API obj = new MqttPublisher_API();
    	obj.run();
    }
    public void run() {    	
    	connectBroker(); // ���Ŀ ������ ����
    	while(true) {
    		try {
    			String[] weather_data  = get_weather_data(); // ���� API
    	       	publish_data("tmp", "{\"tmp\": "+weather_data[0]+"}"); // �µ� ������ ����
    	       	publish_data("humi", "{\"humi\": "+weather_data[1]+"}"); // ���� ������ ����
    	       	publish_data("wind", "{\"wind\": "+weather_data[2]+"}"); // ǳ ������ ����
    	       	Thread.sleep(5000); // @@@@@@
    		}catch (Exception e) {
				// TODO: handle exception
    			try {
    				sampleClient.disconnect();
				} catch (MqttException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
    			e.printStackTrace();
    	        System.out.println("Disconnected");
    	        System.exit(0);
			}
    	}
    }
    
    public void connectBroker() {
        String broker = "tcp://127.0.0.1:1883"; // ���Ŀ ������ �ּ� 
        String clientId = "practice"; // Ŭ���̾�Ʈ�� ID
        MemoryPersistence persistence = new MemoryPersistence();
        try {
            sampleClient = new MqttClient(broker, clientId, persistence);// Mqtt Client ��ü �ʱ�ȭ
            MqttConnectOptions connOpts = new MqttConnectOptions(); // ���ӽ� ������ �ɼ��� �����ϴ� ��ü ����
            connOpts.setCleanSession(true);
            System.out.println("Connecting to broker: "+broker);
            sampleClient.connect(connOpts); // ���Ŀ������ ����
            sampleClient.setCallback(this);// Call back option �߰�
            System.out.println("Connected");
        } catch(MqttException me) {
            System.out.println("reason "+me.getReasonCode());
            System.out.println("msg "+me.getMessage());
            System.out.println("loc "+me.getLocalizedMessage());
            System.out.println("cause "+me.getCause());
            System.out.println("excep "+me);
            me.printStackTrace();
        }
    }
    
    public void publish_data(String topic_input, String data) { // @@@@@ ����ƽ ����
        String topic = topic_input; // ����
        int qos = 0; // QoS level
        try {
            System.out.println("Publishing message: "+data);
            sampleClient.publish(topic, data.getBytes(), qos, false);
            System.out.println("Message published");
        } catch(MqttException me) {
            System.out.println("reason "+me.getReasonCode());
            System.out.println("msg "+me.getMessage());
            System.out.println("loc "+me.getLocalizedMessage());
            System.out.println("cause "+me.getCause());
            System.out.println("excep "+me);
            me.printStackTrace();
        }
    }
    
    public String[] get_weather_data() { // @@@@@ ����ƽ ����
    	// ���� �ð� Ȯ���ؼ� ��¥, �ð� ����
    	Date current = new Date(System.currentTimeMillis());
    	SimpleDateFormat d_format = new SimpleDateFormat("yyyyMMddHHmmss"); 
    	//System.out.println(d_format.format(current));
    	String date = d_format.format(current).substring(0,8); // ��¥
    	String time = d_format.format(current).substring(8,10); // �ð�
       
    	
    	String url = "http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtNcst" // https�� �ƴ� http ���������� ���� �����ؾ� ��.
    			+ "?serviceKey=ADG9aBv7TlPVvXrqwIAxqfLeq5rAeIeum%2BqrsC46u2ox9t5eUF%2FiV%2BER7MnF5YaEjqAnLxrQEobO1vaLZqS9aw%3D%3D"
    			+ "&pageNo=1&numOfRows=1000"
    			+ "&dataType=XML"
    			+ "&base_date="+"20220609"
    			+ "&base_time="+"14"+"00"
    			+ "&nx=63"
    			+ "&ny=128";//�ٻ굿
    	
    	//�����͸� ������ ���� �ʱ�ȭ
		String temp = "-99";
		String humi = "-99";
		String wind = "-99";		
    	Document doc = null;
		
		// Jsoup���� API ������ ��������
		try {
			doc = Jsoup.connect(url).get();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//System.out.println(doc);
		
		Elements elements = doc.select("item");
		for (Element e : elements) {
			if (e.select("category").text().equals("T1H")) { // ��µ�����
				temp = e.select("obsrValue").text();
			}
			if (e.select("category").text().equals("REH")) { // ����
				humi = e.select("obsrValue").text();
			}
			if (e.select("category").text().equals("WSD")) { //ǳ��
				wind = e.select("obsrValue").text();
			}	
		}
		String[] out = {temp, humi, wind};
    	return out;
}
    ///@@@@@@@@@@@@@@@@@
	@Override
	public void connectionLost(Throwable arg0) {
		// TODO Auto-generated method stub
		System.out.println("Connection lost");
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void messageArrived(String topic, MqttMessage msg) throws Exception {
		// TODO Auto-generated method stub
		if (topic.equals("led")){
			System.out.println("--------------------Actuator Function--------------------");
			System.out.println("���ûȨ����������");
			System.out.println("LED: " + msg.toString());
			System.out.println("---------------------------------------------------------");
		}		
	}
}