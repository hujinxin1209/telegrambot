package org.telegram.updateshandlers;

import java.io.BufferedReader;
import org.telegram.services.*;
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
import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.BotConfig;
import org.telegram.LogisticsEnum;
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
		ForceReplyKeyboard forceReplyKeyboard = getForceReply();
		SendMessage sendMessage = new SendMessage();
		sendMessage.enableMarkdown(true);
		sendMessage.setChatId(message.getChatId().toString());
		sendMessage.setReplyToMessageId(message.getMessageId());
		sendMessage.setReplyMarkup(forceReplyKeyboard);
		sendMessage.setText("支持china大部分快递,请输入订单号！");

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
		sendMessage.setText("输入格式：订单号");

		weatherState.put(chatId.toString(), LOGISTICS_CURRENT);
		return sendMessage;
	}

	private static ForceReplyKeyboard getForceReply() {
		ForceReplyKeyboard forceReplyKeyboard = new ForceReplyKeyboard();
		forceReplyKeyboard.setSelective(true);
		return forceReplyKeyboard;
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

	private static SendMessage onLogisticsReceived(Long chatId, Integer userId, Integer messageId, String text,
			String language){
        String response = getResponseByCode(text);
        
		SendMessage sendMessageRequest = new SendMessage();
		sendMessageRequest.enableMarkdown(true);
		sendMessageRequest.setReplyMarkup(getMainMenuKeyboard(language));
		sendMessageRequest.setReplyToMessageId(messageId);
		sendMessageRequest.setText(response);
		sendMessageRequest.setChatId(chatId.toString());

		weatherState.put(chatId.toString(), MAINMENU);
		return sendMessageRequest;
	}
	
	private static String getResponseByCode(String text) {
		String requestData = "";
		String response = "";
		Map<String, String> params = new HashMap<>();
//		long startTime = System.currentTimeMillis();
		for(LogisticsEnum logisticsEnum : LogisticsEnum.values()) {
			
			requestData = "{'OrderCode':'','ShipperCode':'" + logisticsEnum.getCode() + "','LogisticCode':'" + text + "'}";
	        try {
	        		params.put("RequestData", LogisticsService.urlEncoder(requestData, "UTF-8"));
	            params.put("EBusinessID", "1322588");
	            params.put("RequestType", "1002");
	            String dataSign = LogisticsService.encrypt(requestData, LogisticsService.LogisticsApiKey, "UTF-8");
	            params.put("DataSign", LogisticsService.urlEncoder(dataSign, "UTF-8"));
	            params.put("DataType", "2");
	            response = LogisticsService.sendPost(LogisticsService.LogisticsApiURL, params);	
	            JSONObject jsonObject = new JSONObject(response);
	            JSONArray jsonArray = (JSONArray) jsonObject.get("Traces");
	            if(jsonArray.length() > 0) {
	            		response = "快递：" + logisticsEnum.getValue() + "\n";
	            		for(int i = 0; i < jsonArray.length(); i++) {
	            			JSONObject obj = (JSONObject) jsonArray.get(i);  
	            			response += obj.getString("AcceptStation") + "\n" + "时间：" + obj.getString("AcceptTime") + "\n";
	            		}
	            		return response;
	            }
	        } catch(Exception e) {
				e.printStackTrace();
			}
//	        long endTime = System.currentTimeMillis();
//	        System.out.println("程序运行时间：" + (endTime - startTime)+ "ms");
		}
		
		return "我都查不到，快递已消失在地球！";
	}
	
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
			e.printStackTrace();
		} catch (IOException e) {
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
