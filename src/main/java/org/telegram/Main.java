package org.telegram;

import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;

import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.logging.BotLogger;
import org.telegram.telegrambots.logging.BotsFileHandler;
import org.telegram.updateshandlers.FunctionHandlers;

public class Main {
	private static final String LOGTAG = "MAIN";
	
	public static void main(String[] args) {
		BotLogger.setLevel(Level.ALL);
		BotLogger.registerLogger(new ConsoleHandler());
		try {
			BotLogger.registerLogger(new BotsFileHandler());
		} catch (IOException e) {
			BotLogger.severe(LOGTAG, e);
		}

		try {
			ApiContextInitializer.init();
			TelegramBotsApi telegramBotsApi = createTelegramBotsApi();
			try {
				// Register long polling bots. They work regardless type of TelegramBotsApi we
				// are creating
				telegramBotsApi.registerBot(new FunctionHandlers());
			} catch (TelegramApiException e) {
				BotLogger.error(LOGTAG, e);
			}
		} catch (Exception e) {
			BotLogger.error(LOGTAG, e);
		}
	}

	private static TelegramBotsApi createTelegramBotsApi() throws TelegramApiException {
		TelegramBotsApi telegramBotsApi = createLongPollingTelegramBotsApi();;
		return telegramBotsApi;
	}

	/**
	 * @brief Creates a Telegram Bots Api to use Long Polling (getUpdates) bots.
	 * @return TelegramBotsApi to register the bots.
	 */
	private static TelegramBotsApi createLongPollingTelegramBotsApi() {
		return new TelegramBotsApi();
	}

}
