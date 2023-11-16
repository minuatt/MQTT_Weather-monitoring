# Coap_SmartFarm
한림대학교 2022학년도 1학기 IOT네트워크 미니프로젝트(시연영상 :https://www.youtube.com/watch?v=qU3jj3Ze__E)
## 개요
&nbsp;한림대학교 2021학년도 1학기 IOT네트워크 미니프로젝트로 바쁜 현대인의 삶에서 날씰를 확인하지 못하는 사람들을 위한 웹페이지로, 자신이 현재 살고있는 지역의 날씨를 바로바로 모니터링 할 수있도록 구현해 보았다.

1. 사용자가 사는 지역의 날씨데이터를 브로커로 publish
2. 웹에서 subscribe하여 현재 날시 상태와, 안내문구 출력
## 실행 요약
&nbsp;- 해당 프로젝트는 MQTT를 이용하여 구현하였고, 사용자 지역의 날씨정보시스템을 주는 기상청 API를 MQTT Broker서버에 Publish한다.</br></br>  MQTT Broker는 Publish 된 데이터를 MQTT subscriber에게 전달하고, Node.js는 데이터를 수신하여 MongoDB에 저장하고, Web UI를 제공하는 HTTP server의 역할을 수행한다. MongoDB는 수신되는 데이터를 저장하고, Node.js에 따라 데이터를 제공한다.</br>
</br>
  동작과정으로는 MQTT subscriber가 Broker에게 기온,습도,풍향의 구독을 요청하고, 구독요청을 받은 MQTT Broker는 MQTT subscriber를 토픽의 구독리스트에 추가한다. </br></br>
  그후, 공공API에서 기온 및 습도, 풍향 데이터를 Publish하면 MQTT broker는 Subscriber에게 데이터를 전송한다. 후에 web UI에서 기온에따라 LED(사진)이 바뀌는 것을 볼 수 있고, 사진을 누르면 사용자가 살고 있는 위치의 기상청 날씨정보화면이 뜨게된다.
  </br>
```java
 var server = http.createServer(app);
 
 // Connect Mongo DB 
 var mongoDB = require("mongodb").MongoClient;
 var url = "mongodb://127.0.0.1:27017/IoTDB";
 var dbObj = null;
 mongoDB.connect(url, function(err, db){
   dbObj = db;
   console.log("DB connect");
 });
 
 /**
  * MQTT subscriber (MQTT Server connection & Read resource data)
  */
 var mqtt = require("mqtt");
const { stringify } = require('querystring');
 var client = mqtt.connect("mqtt://127.0.0.1")
 
 // 접속에 성공하면, 3가지 토픽을 구독.
 client.on("connect", function(){
   client.subscribe("tmp");
   console.log("Subscribing tmp");
   client.subscribe("humi");
   console.log("Subscribing humi");
   client.subscribe("wind");
   console.log("Subscribing wind");
 })
```
- 위 코드는 MongoDB와 연결하고, MQTT subscriber는 tmp, humi, wind데이터를 구독한다.
```java
// MQTT 응답 메세지 수신시 동작
client.on("message", function(topic, message){
  console.log(topic+ ": " + message.toString()); // 수신한 메세지 Topic 출력
  var obj = JSON.parse(message); // 수신한 메세지의 데이터를 obj 저장
  obj.create_at = new Date(); // 현재 날짜 데이터를 obj에 추가함.
  console.log(obj);
   // send the received data to MongoDB
   // 수신한 메세지를 Mongo DB에 저장
  if (topic == "tmp"){ // 만약 토픽이 온도라면,
    var cursor = dbObj.db("Resources").collection("Temperature");
    cursor.insertOne(obj, function(err, result){
      if(err){console.log(err);}
      else{console.log(JSON.stringify(result));}
    });
  }	
  else if (topic == "humi"){ // 만약 토픽이 습도라면,
    var cursor = dbObj.db("Resources").collection("Humidity");
    cursor.insertOne(obj, function(err, result){
      if(err){console.log(err);}
      else{console.log(JSON.stringify(result));}
    });
  }
  else if (topic == "wind"){ // 만약 토픽이 풍속이라면,
    var cursor = dbObj.db("Resources").collection("Windspeed");
    cursor.insertOne(obj, function(err, result){
      if(err){console.log(err);}
      else{console.log(JSON.stringify(result));}
    });
  }
});
```
- 위 코드는 수신한 메시지를 MongoDB에 저장한다. 만약 토픽이 온도, 습도, 풍속이라면 db에 저장되어있는 collection에서 데이터를 불러온다.
```java  
 // Mongo DB에서 최근 데이터 불러와서, HTML 페이지에 업데이트
var io = require("socket.io")(server);
io.on("connection", function(socket){
  socket.on("socket_evt_update", function(data){
    //온도 데이터
    var cursor = dbObj.db("Resources").collection("Temperature");
    var options = {sort:{"_id":-1}, projection: {_id:0, tmp:1, creat_at:1},};
    var sending_data =cursor.find({},options).limit(1);
    sending_data.toArray(function(err,results){
      if(!err){
        socket.emit("socket_up_temp", JSON.stringify(results[0]));
      }
    });
    // 습도 데이터
    var cursor = dbObj.db("Resources").collection("Humidity");
    var options = {sort:{"_id":-1}, projection: {_id:0, humi:1, creat_at:1},};
    var sending_data =cursor.find({},options).limit(1);
    sending_data.toArray(function(err,results){
      if(!err){
        socket.emit("socket_up_humi", JSON.stringify(results[0]));
      }
    }); 
    // 풍속 데이터
    var cursor = dbObj.db("Resources").collection("Windspeed");
    var options = {sort:{"_id":-1}, projection: {_id:0, wind:1, creat_at:1},};
    var sending_data =cursor.find({},options).limit(1);
    sending_data.toArray(function(err,results){
      if(!err){
        socket.emit("socket_up_wind", JSON.stringify(results[0]));
      }
    }); 
  });
  
});
 
```
- 위 코드는 MongoDB에서 최근데이터를 불러오면 HTML 페이지에 업데이트를 한다.</br>
컬렉션에서 해당되는 데이터를 불러오면 해당 데이터를 MQTT.html로 전송하여 html 형식으로 웹페이지에 출력되게된다.</br>

</br>

- [Html]
```html
<script>
	var socket = io.connect();
	var timer = null;
	$(document).ready(function(){
		socket.on("socket_up_temp", function(data){
			data = JSON.parse(data);
			$(".mqttlist_temp").html('<li> 온도는 : '+data.tmp+'도.'+'</li>');
			if(data.tmp <= 15){
				$('#box').html("추워요 감기조심하세요!");
				$('a:first-of-type').css('display','none');
				$('a:nth-of-type(2)').css('display', 'block');
				$('a:last-of-type').css('display','none');
				$('p').css('display','block');
			}
			else if(data.tmp >15 && data.tmp <=25){
				$('#box').html('날씨가 좋아요 강아지와 산책 어떠신가요?');
				$('a:first-of-type').css('display','block');
				$('a:nth-of-type(2)').css('display', 'none');
				$('a:last-of-type').css('display','none');
				$('p').css('display','block');
			}
			else{
				$('#box').html('날씨가 덥네요, 집콕 추천합니다!')
				$('a:first-of-type').css('display','none');
				$('a:nth-of-type(2)').css('display', 'none');
				$('a:last-of-type').css('display','block');
				$('p').css('display','block');
			}
			
		});
		socket.on("socket_up_humi", function(data){
			data = JSON.parse(data);
			$(".mqttlist_humi").html('<li> 습도는 : '+data.humi+'%.'+'</li>');
		});
		socket.on("socket_up_wind", function(data){
			data = JSON.parse(data);
			$(".mqttlist_wind").html('<li> 풍속은 : '+data.wind+'m/s'+'</li>');
		});
		if(timer==null){
			timer = window.setInterval("timer_1()", 3000);
		}
		
	});
	function timer_1(){
		socket.emit("socket_evt_update", JSON.stringify({}));
	}
</script>
</head>
```
- MQTT.html에서의 script코드이다.</br>
MongoDB에서 최신데이터를 받아오면 html파일에서 web ul로 표시되게 된다. </br>
받아온 온도가 if문의 조건에따라서 그림이 바뀌고, 출력문이 바뀌게된다.</br> 이과정을 led를 이용하여 빨간색 초록색 파란색으로 표현하려고했으나, 웹 ui가 너무 단조로워져서 그림으로 바꾸게 되었다. 
```html
<body>
<h3><strong>경기도 다산동 날씨 정보입니다</strong></h3>
	<div id="msg">
		<div id="mqtt_logs">
			<ul class="mqttlist_temp"></ul>
			<ul class="mqttlist_humi"></ul>
			<ul class="mqttlist_wind"></ul><br>
			<div id = box></div>
			<a href = "https://www.weather.go.kr/w/index.do#"><img src="따뜻해요.png" width="300" height="300"></a>
			<a href = "https://www.weather.go.kr/w/index.do#"><img src="추워요.png" width="300" height="300"></a>
			<a href = "https://www.weather.go.kr/w/index.do#"><img src= "더워죽는사진.png" width="300" height="300"></a>
			<p>위 그림을 클릭하면 다산1동 날씨 홈페이지로 이동합니다!</p>
		</div>
	</div>
</body>
```
- html파일의 style과 body부분 코드이다.</br>
온도에따라 변화되는 그림을 클릭하게될시에 기상청홈페이지로 이동되어 사용자가 보다 정확한 날씨를 확인할 수 있도록 하였고, 백그라운드 이미지로 다산동의 트레이드마크 현대아울렛을 넣어 단조로움을 해소하였다.</br>
</br>

- [MQTT API]
```java
public class MqttPublisher_API implements MqttCallback{ // implement callback 추가 & 필요한 메소드 정의
	static MqttClient sampleClient;// Mqtt Client 객체 선언
	
    public static void main(String[] args) {
    	MqttPublisher_API obj = new MqttPublisher_API();
    	obj.run();
    }
    public void run() {    	
    	connectBroker(); // 브로커 서버에 접속
    	while(true) {
    		try {
    			String[] weather_data  = get_weather_data(); // 공공 API
    	       	publish_data("tmp", "{\"tmp\": "+weather_data[0]+"}"); // 온도 데이터 발행
    	       	publish_data("humi", "{\"humi\": "+weather_data[1]+"}"); // 습도 데이터 발행
    	       	publish_data("wind", "{\"wind\": "+weather_data[2]+"}"); // 풍 데이터 발행
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
```
- publisher 코드이고, broker서버에 접속하여 run함수로 while문을 통해 공용 api에서 정보를 받아와서 온도,습도,풍속 데이터를 발행한다.

```java
 public void connectBroker() {
        String broker = "tcp://127.0.0.1:1883"; // 브로커 서버의 주소 
        String clientId = "practice"; // 클라이언트의 ID
        MemoryPersistence persistence = new MemoryPersistence();
        try {
            sampleClient = new MqttClient(broker, clientId, persistence);// Mqtt Client 객체 초기화
            MqttConnectOptions connOpts = new MqttConnectOptions(); // 접속시 접속의 옵션을 정의하는 객체 생성
            connOpts.setCleanSession(true);
            System.out.println("Connecting to broker: "+broker);
            sampleClient.connect(connOpts); // 브로커서버에 접속
            sampleClient.setCallback(this);// Call back option 추가
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
    
    public void publish_data(String topic_input, String data) { // @@@@@ 스태틱 제거
        String topic = topic_input; // 토픽
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
```
- 브로커 서버 주소로 브로커를 연결하는 코드와, 공용 api로 받아온 데이터를 publish하는 코드이다.
 ```java
public String[] get_weather_data() { // @@@@@ 스태틱 제거
    	// 현재 시간 확인해서 날짜, 시간 저장
    	Date current = new Date(System.currentTimeMillis());
    	SimpleDateFormat d_format = new SimpleDateFormat("yyyyMMddHHmmss"); 
    	//System.out.println(d_format.format(current));
    	String date = d_format.format(current).substring(0,8); // 날짜
    	String time = d_format.format(current).substring(8,10); // 시간
       
    	
    	String url = "http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtNcst" // https가 아닌 http 프로토콜을 통해 접근해야 함.
    			+ "?serviceKey=ADG9aBv7TlPVvXrqwIAxqfLeq5rAeIeum%2BqrsC46u2ox9t5eUF%2FiV%2BER7MnF5YaEjqAnLxrQEobO1vaLZqS9aw%3D%3D"
    			+ "&pageNo=1&numOfRows=1000"
    			+ "&dataType=XML"
    			+ "&base_date="+"20220609"
    			+ "&base_time="+"14"+"00"
    			+ "&nx=63"
    			+ "&ny=128";//다산동
    	
    	//데이터를 저장할 변수 초기화
		String temp = "-99";
		String humi = "-99";
		String wind = "-99";		
    	Document doc = null;
		
		// Jsoup으로 API 데이터 가져오기
		try {
			doc = Jsoup.connect(url).get();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//System.out.println(doc);
		
		Elements elements = doc.select("item");
		for (Element e : elements) {
			if (e.select("category").text().equals("T1H")) { // 기온데이터
				temp = e.select("obsrValue").text();
			}
			if (e.select("category").text().equals("REH")) { // 습도
				humi = e.select("obsrValue").text();
			}
			if (e.select("category").text().equals("WSD")) { //풍속
				wind = e.select("obsrValue").text();
			}	
		}
		String[] out = {temp, humi, wind};
    	return out;
```
-위 코드는 공용 api로 날씨데이터를 받아오는 코드이며, 개인인증키를 활용하여 데이터를 받아왔고, 2022년06월7일 18시의 날씨데이터를 받아오고있고, nx ny는 다산동으로 설정하여 내가 살고있는 다산동의 날씨를 불러오고있다.</br>
공용 api의 T1H는 기온데이터, REH는 습도, WSD는 풍속데이터이며, 카테고리가 이(T1H,REH,WSD)와 같으면 배열에 넣어 데이터를 가져오고있다.
##결론
&nbsp;프로젝트의 목적으론 공용 api를 이용해 데이터를 받고, web ui 에 데이터를 출력하는것이고, 사용자가 바로바로 날씨를 확인하는것에 중점을 두었고, 웹페이지에서 바로 날씨 확인을 할 수 있기때문에 이는 이루어냈다고 할 수 있다.</br></br>

하지만, 사용자가 과거 날씨가아닌 내일 날씨도 궁금해 할 수 있기때문에 공용 api의 예보 api를 사용하여 웹페이지에 출력하면 사용자에게 더욱 도움이 될 것 같다.</br>

이를 센서와 연결하여 농장이나, 비닐하우스 같은 곳에 시간별 날씨데이터를 계속 받아와 온.습도에따라 웹페이지에 “농장에 물을부어주세요 날씨가 더워요”등등 출력을 하고, 알림을 뜨게한다면, 농장 주인이 바로바로 확인할수 있고, 농작물생산에 도움이 될수 있을것 같다.
