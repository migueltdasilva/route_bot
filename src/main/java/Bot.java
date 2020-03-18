
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import java.util.List;

public class Bot extends TelegramLongPollingBot {

    private final String botName;
    private final String botToken;


    Bot(String botName, String botToken) {
        this.botName = botName;
        this.botToken = botToken;
    }

    /**
     * Метод для приема сообщений.
     * @param update Содержит сообщение от пользователя.
     */
    public void onUpdateReceived(Update update) {

        String message = update.getMessage().getText();
        System.out.println("MSG REC: " + message);
        sendMsg(update.getMessage().getChatId().toString(), message);
    }

    public synchronized void answerCallbackQuery(String callbackId, String message) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(callbackId);
        answer.setText(message);
        answer.setShowAlert(true);
        try {
            execute(answer);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    /**
     * Метод для настройки сообщения и его отправки.
     * @param chatId id чата
     * @param s Строка, которую необходимот отправить в качестве сообщения.
     */
    public synchronized void sendMsg(String chatId, String s) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);

        sendMessage.setText(s);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            //log.log(Level.SEVERE, "Exception: ", e.toString());
        }
    }

    public void onUpdatesReceived(List<Update> updates) {
        for (Update upd : updates) {
            onUpdateReceived(upd);
        }
    }

    public String getBotUsername() {
        return botName;
    }

    public String getBotToken() {
        return botToken;
    }

    public static void main(String[] args) {
        ApiContextInitializer.init();
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        try {
            Bot routeBot = new Bot("RouteTestBot", "900084418:AAEfgGDNoCUstvOCWlw3cBTGrny80h0rSK0");
            telegramBotsApi.registerBot(routeBot);
        } catch (TelegramApiRequestException e) {

            e.printStackTrace();
        }
    }

}
