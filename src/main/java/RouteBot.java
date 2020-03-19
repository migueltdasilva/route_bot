
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RouteBot extends Bot {

    private static final String name = "RouteTestBot";
    private static final String token = "900084418:AAEfgGDNoCUstvOCWlw3cBTGrny80h0rSK0";
    RouteBot() {
        super(name, token);
        hmChat2Answers = new HashMap<>();
    }

    private static final String[] vTrips = new String[]{"Хочу в road trip по Киргизии"};
    private static final String[] vQuestions = new String[]{
            "Расскажи, пожалуйста, в двух словах, о себе: чем ты занимаешься," +
                    " что ты любишь, почему хочешь поехать с нами?\n" +
                    " (Обещаю, дальше вопросы будут попроще)",
            "есть ли у тебя права и готов ли ты быть водителем (водительницей) одной из машин?",
            "когда в последний раз ты ночевал в палатке и как вообще к ним относишься?",
            "белое или красное?",
            "пришли, пожалуйста, ссылки на свои соц сети (например, фейсбук и инстаграм)",
            "Очень важный вопрос про любимый стикер.\n\n" +
                    "Во-первых нужно выбрать один из множества любимых. \n" +
                    "Во-вторых описать его словами, точнее голосом и прислать аудиосообщение сюда.\n\n" +
                    "Не претендуем на звание стикерных знатоков, но попборуем угадать!"};

    private Map<String, List<String>> hmChat2Answers;


    public synchronized void setTripButtons(SendMessage sendMessage) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> alKeyboardRows = new ArrayList<>();
        for (String trip : vTrips) {
            KeyboardRow keyboardFirstRow = new KeyboardRow();
            keyboardFirstRow.add(new KeyboardButton(trip));
            alKeyboardRows.add(keyboardFirstRow);
        }
        replyKeyboardMarkup.setKeyboard(alKeyboardRows);
    }

    private void setInline() {
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        List<InlineKeyboardButton> buttons1 = new ArrayList<>();
        buttons1.add(new InlineKeyboardButton().setText("Кнопка").setCallbackData("17"));
        buttons.add(buttons1);

        InlineKeyboardMarkup markupKeyboard = new InlineKeyboardMarkup();
        markupKeyboard.setKeyboard(buttons);
    }


    public synchronized void sendMsg(String chatId, String s, boolean bBtn) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setText(s);
        if (bBtn) {
            setTripButtons(sendMessage);
        }
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            //log.log(Level.SEVERE, "Exception: ", e.toString());
        }
    }


    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message msg = update.getMessage();
            String message = msg.getText();
            System.out.println("MSG REC: " + message);
            User usr = msg.getFrom();
            String userName = "";
            if (usr != null) {
                userName = usr.getUserName();
            }
            sendMsg(
                    msg.getChatId().toString(),
                    message + " " + msg.getAuthorSignature() + " " +
                            msg.getChatId() + " " + userName + " " + msg.getMessageId(),
                    message.equals("/start"));
        }
    }


    public static void main(String[] args) {
        ApiContextInitializer.init();
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        try {
            Bot routeBot = new RouteBot();
            telegramBotsApi.registerBot(routeBot);
        } catch (TelegramApiRequestException e) {

            e.printStackTrace();
        }
    }
}
