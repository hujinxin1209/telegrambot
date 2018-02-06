package org.telegram.updateshandlers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.output.ThresholdingOutputStream;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.telegram.BotConfig;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.ForceReplyKeyboard;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.logging.BotLogger;

import retrofit2.Response;


public class FunctionHandlers extends TelegramLongPollingBot {
	private static final String LOGTAG = "FUNCTIONHANDLERS";
	private static final int MAINMENU = 1;
	private static final int LOGISTICS_CURRENT = 10;
	private static final int LOGISTICS_NEW = 11;
	private static final int WEATHER_CURRENT = 20;
	private static final int WEATHER_NEW = 21;

	public static Map<String, Integer> weatherState = new HashMap<String, Integer>();
	public static Map<String, Integer> logisticsState = new HashMap<String, Integer>();

	@Override
	public String getBotUsername() {
		return BotConfig.UGIA_USER;
	}

	@Override
	public String getBotToken() {
		return BotConfig.UGIA_TOKEN;
	}

	@Override
	public void onUpdateReceived(Update update) {
		try {
			if (update.hasMessage()) {
				Message message = update.getMessage();
				if (message.hasText()) {
					handleIncomingMessage(message);
				}
			}
		} catch (Exception e) {
			BotLogger.error(LOGTAG, e);
		}
	}

	private void handleIncomingMessage(Message message) throws TelegramApiException {
		int state = 1;
		if (message.getChatId().toString() != null) {
//			if (message.getText().equals("天气")) {
//				if(weatherState.size() > 0 && weatherState.get(message.getChatId().toString())>0)
//					state = weatherState.get(message.getChatId().toString());
//			} else if(message.getText().equals("物流")){
//				if(logisticsState.size() > 0 && logisticsState.get(message.getChatId().toString())>0)
//					state = logisticsState.get(message.getChatId().toString());
//			};
			if(weatherState.size() > 0 && weatherState.get(message.getChatId().toString())>0)
				state = weatherState.get(message.getChatId().toString());
		}

		SendMessage sendMessageRequest = null;
		switch (state) {
		case MAINMENU:
			sendMessageRequest = messageOnMainMenu(message, "");
			break;
		case LOGISTICS_CURRENT:
		case LOGISTICS_NEW:
			sendMessageRequest = messageOnCurrentLogistics(message, "", state);
			// 物流
			break;
		case WEATHER_CURRENT:
		case WEATHER_NEW:
			sendMessageRequest = messageOnCurrentWeather(message, "", state);
			break;
		default:
			sendMessageRequest = sendMessageDefault(message, "");
			break;
		}
		sendMessage(sendMessageRequest);
	}

	private static SendMessage messageOnMainMenu(Message message, String language) {
		SendMessage sendMessageRequest;
		if (message.hasText()) {
			if (message.getText().equals("物流")) {
				sendMessageRequest = onLogisticsChoosen(message, language);
			} else if (message.getText().equals("天气")) {
				sendMessageRequest = onCurrentNewWeather(message, language);// onWeatherChoosen(message, language);
			} else {
				sendMessageRequest = sendChooseOptionMessage(message.getChatId(), message.getMessageId(),
						getMainMenuKeyboard(language), language);
			}
		} else {
			sendMessageRequest = sendChooseOptionMessage(message.getChatId(), message.getMessageId(),
					getMainMenuKeyboard(language), language);
		}
		return sendMessageRequest;
	}

	private static SendMessage sendChooseOptionMessage(Long chatId, Integer messageId, ReplyKeyboard replyKeyboard,
			String language) {
		SendMessage sendMessage = new SendMessage();
		sendMessage.enableMarkdown(true);
		sendMessage.setChatId(chatId.toString());
		sendMessage.setReplyToMessageId(messageId);
		sendMessage.setReplyMarkup(replyKeyboard);
		sendMessage.setText("请重新选择！");

		return sendMessage;
	}

	private static SendMessage onLogisticsChoosen(Message message, String language) {
//		SendMessage sendMessage = new SendMessage();
//		weatherState.put(message.getChatId().toString(), LOGISTICS_CURRENT);
//		sendMessage.setChatId(message.getChatId());
//		sendMessage.setText("查询物流：");
//		return sendMessage;
		
		
		ForceReplyKeyboard forceReplyKeyboard = getForceReply();

		SendMessage sendMessage = new SendMessage();
		sendMessage.enableMarkdown(true);
		sendMessage.setChatId(message.getChatId().toString());
		sendMessage.setReplyToMessageId(message.getMessageId());
		sendMessage.setReplyMarkup(forceReplyKeyboard);
		sendMessage.setText("支持商家：BTWL(百世快运) DBL(德邦) EMS(EMS) ZTO(中通快递)" + "\n"
				+ "HHTT(天天快递) JGSD(京广速递) HTKY(百世快递) JTKD(捷特快递)" + "\n"
				+ "STO(申通快递) YD(韵达快递) YTO(圆通速递) SF(顺丰快递) "+ 
		"\n" + "格式：商家字母(大写) 订单号");

		weatherState.put(message.getChatId().toString(), LOGISTICS_CURRENT);
		return sendMessage;
	}

	private static SendMessage sendMessageDefault(Message message, String language) {
		ReplyKeyboardMarkup replyKeyboardMarkup = getMainMenuKeyboard(language);
		weatherState.put(message.getChatId().toString(), MAINMENU);
		return sendHelpMessage(message.getChatId(), message.getMessageId(), replyKeyboardMarkup, language);
	}

	private static SendMessage sendHelpMessage(Long chatId, Integer messageId, ReplyKeyboardMarkup replyKeyboardMarkup,
			String language) {
		SendMessage sendMessage = new SendMessage();
		sendMessage.enableMarkdown(true);
		sendMessage.setChatId(chatId);
		sendMessage.setReplyToMessageId(messageId);
		if (replyKeyboardMarkup != null) {
			sendMessage.setReplyMarkup(replyKeyboardMarkup);
		}
		sendMessage.setText("暂时查部分物流公司！");
		return sendMessage;
	}

	private static SendMessage messageOnCurrentWeather(Message message, String language, int state) {
		SendMessage sendMessageRequest = null;
		switch (state) {
		case WEATHER_NEW:
			sendMessageRequest = onCurrentNewWeather(message, language);
			break;
		case WEATHER_CURRENT:
			sendMessageRequest = onCurrentWeather(message, language);
			break;
		}
		return sendMessageRequest;
	}

	private static SendMessage messageOnCurrentLogistics(Message message, String language, int state) {
		SendMessage sendMessageRequest = null;
		switch (state) {
		case LOGISTICS_NEW:
			sendMessageRequest = onCurrentNewLogistics(message, language);
			break;
		case LOGISTICS_CURRENT:
			sendMessageRequest = onCurrentLogistics(message, language);
			break;
		}
		return sendMessageRequest;
	}

	private static SendMessage onCurrentWeather(Message message, String language) {
		if (message.isReply()) {
			return onWeatherReceived(message.getChatId(), message.getFrom().getId(), message.getMessageId(),
					message.getText(), language);
		} else {
			return sendMessageDefault(message, language);
		}
	}

	private static SendMessage onCurrentLogistics(Message message, String language) {
		if (message.isReply()) {
			return onLogisticsReceived(message.getChatId(), message.getFrom().getId(), message.getMessageId(),
					message.getText(), language);
		} else {
			return sendMessageDefault(message, language);
		}
	}

	private static SendMessage onCurrentNewWeather(Message message, String language) {
		SendMessage sendMessageRequest = null;
		if (message.hasText()) {
			sendMessageRequest = onNewCurrentWeatherCommand(message.getChatId(), message.getFrom().getId(),
					message.getMessageId(), language);
		}
		return sendMessageRequest;
	}
	private static SendMessage onCurrentNewLogistics(Message message, String language) {
		SendMessage sendMessageRequest = null;
		if (message.hasText()) {
			sendMessageRequest = onNewCurrentLogisticsCommand(message.getChatId(), message.getFrom().getId(),
					message.getMessageId(), language);
		}
		return sendMessageRequest;
	}

	private static SendMessage onNewCurrentWeatherCommand(Long chatId, Integer userId, Integer messageId,
			String language) {
		ForceReplyKeyboard forceReplyKeyboard = getForceReply();

		SendMessage sendMessage = new SendMessage();
		sendMessage.enableMarkdown(true);
		sendMessage.setChatId(chatId.toString());
		sendMessage.setReplyToMessageId(messageId);
		sendMessage.setReplyMarkup(forceReplyKeyboard);
		sendMessage.setText("请输入具体城市或县：赣州");

		weatherState.put(chatId.toString(), WEATHER_CURRENT);
		return sendMessage;
	}
	private static SendMessage onNewCurrentLogisticsCommand(Long chatId, Integer userId, Integer messageId,
			String language) {
		ForceReplyKeyboard forceReplyKeyboard = getForceReply();

		SendMessage sendMessage = new SendMessage();
		sendMessage.enableMarkdown(true);
		sendMessage.setChatId(chatId.toString());
		sendMessage.setReplyToMessageId(messageId);
		sendMessage.setReplyMarkup(forceReplyKeyboard);
		sendMessage.setText("格式：商家字母(大写) 订单号");

		weatherState.put(chatId.toString(), LOGISTICS_CURRENT);
		return sendMessage;
	}

	private static ForceReplyKeyboard getForceReply() {
		ForceReplyKeyboard forceReplyKeyboard = new ForceReplyKeyboard();
		forceReplyKeyboard.setSelective(true);
		return forceReplyKeyboard;
	}

	private static SendMessage messageOnWeather(Message message, String language, int state) {
		if (message.isReply()) {
			return onWeatherReceived(message.getChatId(), message.getFrom().getId(), message.getMessageId(),
					message.getText(), language);
		} else {
			return onWeatherReceived(message.getChatId(), message.getFrom().getId(), message.getMessageId(),
					message.getText(), language);

			// return sendMessageDefault(message, language);
		}
	}

	private static SendMessage onWeatherReceived(Long chatId, Integer userId, Integer messageId, String text,
			String language) {
		String weather = fetchWeatherCurrent(text, language);
		SendMessage sendMessageRequest = new SendMessage();
		sendMessageRequest.enableMarkdown(true);
		sendMessageRequest.setReplyMarkup(getMainMenuKeyboard(language));
		sendMessageRequest.setReplyToMessageId(messageId);
		sendMessageRequest.setText(weather);
		sendMessageRequest.setChatId(chatId.toString());

		weatherState.put(chatId.toString(), MAINMENU);
		return sendMessageRequest;
	}

	private static String urlEncoder(String str, String charset) throws UnsupportedEncodingException {
		return URLEncoder.encode(str, charset);
	}

	private static String encrypt(String content, String keyValue, String charset) throws Exception {
		if (keyValue != null) {
			return base64(MD5(content + keyValue, charset), charset);
		}
		return base64(MD5(content, charset), charset);
	}

	private static String MD5(String str, String charset) throws Exception {
		MessageDigest md = MessageDigest.getInstance("MD5");
		md.update(str.getBytes(charset));
		byte[] result = md.digest();
		StringBuilder sb = new StringBuilder(32);
		for (int i = 0; i < result.length; i++) {
			int val = result[i] & 0xff;
			if (val <= 0xf) {
				sb.append("0");
			}
			sb.append(Integer.toHexString(val));
		}
		return sb.toString().toLowerCase();
	}

	private static String base64(String str, String charset) throws UnsupportedEncodingException {
		return base64Encode(str.getBytes(charset));
	}

	public static String base64Encode(byte[] data) {
		StringBuilder sb = new StringBuilder();
		int len = data.length;
		int i = 0;
		int b1, b2, b3;
		while (i < len) {
			b1 = data[i++] & 0xff;
			if (i == len) {
				sb.append(base64EncodeChars[b1 >>> 2]);
				sb.append(base64EncodeChars[(b1 & 0x3) << 4]);
				sb.append("==");
				break;
			}
			b2 = data[i++] & 0xff;
			if (i == len) {
				sb.append(base64EncodeChars[b1 >>> 2]);
				sb.append(base64EncodeChars[((b1 & 0x03) << 4) | ((b2 & 0xf0) >>> 4)]);
				sb.append(base64EncodeChars[(b2 & 0x0f) << 2]);
				sb.append("=");
				break;
			}
			b3 = data[i++] & 0xff;
			sb.append(base64EncodeChars[b1 >>> 2]);
			sb.append(base64EncodeChars[((b1 & 0x03) << 4) | ((b2 & 0xf0) >>> 4)]);
			sb.append(base64EncodeChars[((b2 & 0x0f) << 2) | ((b3 & 0xc0) >>> 6)]);
			sb.append(base64EncodeChars[b3 & 0x3f]);
		}
		return sb.toString();
	}

	private static char[] base64EncodeChars = new char[] { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L',
			'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g',
			'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1',
			'2', '3', '4', '5', '6', '7', '8', '9', '+', '/' };
	private static String sendPost(String url, Map<String, String> params) {
        OutputStreamWriter out = null;
        BufferedReader in = null;        
        StringBuilder result = new StringBuilder(); 
        try {
            URL realUrl = new URL(url);
            HttpURLConnection conn =(HttpURLConnection) realUrl.openConnection();
            // 发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // POST方法
            conn.setRequestMethod("POST");
            // 设置通用的请求属性
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.connect();
            // 获取URLConnection对象对应的输出流
            out = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
            // 发送请求参数            
            if (params != null) {
		          StringBuilder param = new StringBuilder(); 
		          for (Map.Entry<String, String> entry : params.entrySet()) {
		        	  if(param.length()>0){
		        		  param.append("&");
		        	  }	        	  
		        	  param.append(entry.getKey());
		        	  param.append("=");
		        	  param.append(entry.getValue());		        	  
		        	  //System.out.println(entry.getKey()+":"+entry.getValue());
		          }
		          //System.out.println("param:"+param.toString());
		          out.write(param.toString());
            }
            // flush输出流的缓冲
            out.flush();
            // 定义BufferedReader输入流来读取URL的响应
            in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8"));
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
        } catch (Exception e) {            
            e.printStackTrace();
        }
        //使用finally块来关闭输出流、输入流
        finally{
            try{
                if(out!=null){
                    out.close();
                }
                if(in!=null){
                    in.close();
                }
            }
            catch(IOException ex){
                ex.printStackTrace();
            }
        }
        return result.toString();
    }

	private static SendMessage onLogisticsReceived(Long chatId, Integer userId, Integer messageId, String text,
			String language){
		String[] strs = text.split(" ");
//		Optional<ExpressCode> expressCode = ExpressCode.byName(strs[0]);
//		String expCode = expressCode.get().name();
		String requestData = "{'OrderCode':'','ShipperCode':'" + strs[0] + "','LogisticCode':'" + strs[1] + "'}";
		
        Map<String, String> params = new HashMap<>();
        String response = null;
        try {
        	params.put("RequestData", urlEncoder(requestData, "UTF-8"));
            params.put("EBusinessID", "1322588");
            params.put("RequestType", "1002");
            String dataSign = encrypt(requestData, "b4132fd1-9d17-4098-a2b7-c62929b04fd8", "UTF-8");
            params.put("DataSign", urlEncoder(dataSign, "UTF-8"));
            params.put("DataType", "2");
            response = sendPost("http://api.kdniao.cc/Ebusiness/EbusinessOrderHandle.aspx", params);	
		} catch(Exception e) {
			e.printStackTrace();
		}
        
		
		//String logistics =  (text, language);
		SendMessage sendMessageRequest = new SendMessage();
		sendMessageRequest.enableMarkdown(true);
		sendMessageRequest.setReplyMarkup(getMainMenuKeyboard(language));
		sendMessageRequest.setReplyToMessageId(messageId);
		sendMessageRequest.setText(response);
		sendMessageRequest.setChatId(chatId.toString());

		weatherState.put(chatId.toString(), MAINMENU);
		return sendMessageRequest;
	}

	// private static String fetchLogisticsCurrent(String city, String language) {
	//
	// }

	private static String fetchWeatherCurrent(String city, String language) {
		CloseableHttpClient client = HttpClientBuilder.create().setSSLHostnameVerifier(new NoopHostnameVerifier())
				.build();

		HttpGet request = new HttpGet("http://www.sojson.com/open/api/weather/json.shtml?city=" + city);
		CloseableHttpResponse response;
		String responseString = null;
		try {
			response = client.execute(request);
			HttpEntity ht = response.getEntity();

			BufferedHttpEntity buf = new BufferedHttpEntity(ht);
			responseString = EntityUtils.toString(buf, "UTF-8");
			JSONObject jsonObject = new JSONObject(responseString);
			if (jsonObject.getInt("status") == 200) {
				JSONObject jsonObject1 = new JSONObject(
						jsonObject.getJSONObject("data").getJSONArray("forecast").get(0).toString());
				JSONObject jsonObject2 = new JSONObject(
						jsonObject.getJSONObject("data").getJSONArray("forecast").get(1).toString());
				responseString = "昨天：" + jsonObject.getJSONObject("data").getJSONObject("yesterday").getString("high")
						+ "--" + jsonObject.getJSONObject("data").getJSONObject("yesterday").getString("low") + "\n"
						+ "今天：" + jsonObject1.getString("high") + "--" + jsonObject1.getString("low") + "\n" + "明天："
						+ jsonObject2.getString("high") + "--" + jsonObject2.getString("low");
				;
			}

		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return responseString;
	}

	private static ReplyKeyboardMarkup getMainMenuKeyboard(String language) {
		ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
		replyKeyboardMarkup.setSelective(true);
		replyKeyboardMarkup.setResizeKeyboard(true);
		replyKeyboardMarkup.setOneTimeKeyboard(false);

		List<KeyboardRow> keyboard = new ArrayList<>();
		KeyboardRow keyboardFirstRow = new KeyboardRow();
		keyboardFirstRow.add("物流");
		keyboardFirstRow.add("天气");
		keyboard.add(keyboardFirstRow);
		replyKeyboardMarkup.setKeyboard(keyboard);

		return replyKeyboardMarkup;
	}

}
