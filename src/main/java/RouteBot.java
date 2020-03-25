
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendAudio;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVoice;
import org.telegram.telegrambots.meta.api.objects.Audio;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.Voice;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.System.out;

public class RouteBot extends Bot {

    private static final String name = "RouteTestBot";
    private static final String token = "900084418:AAEfgGDNoCUstvOCWlw3cBTGrny80h0rSK0";
    private static final String adminChatId = "3099992";

    RouteBot() {
        super(name, token);
        hmChat2Answers = new HashMap<>();
        hmChat2UserInfo = new HashMap<>();
    }

    private enum Command {
        COMMAND_LIST("/", "Список всех команд."),
        START("/start", "Начать все сначала."),
        SEND_JOKE("/send_joke", "Могу отправить тебе шутку.");

        String name;
        String desr;
        public static Map<String, Command> cmdByName = new HashMap<>();
        public static Command byName(String name) {
            return cmdByName.get(name);
        }
        Command(String name, String desr) {
            this.name = name;
            this.desr = desr;
        }
        static {
            for(Command cmd : Command.values()) {
                cmdByName.put(cmd.name, cmd);
            }
        }
    }

    private static final String[] vTrips = new String[]{"Хочу в road trip по Киргизии"};
    private static final String[] vQuestions = new String[]{
            "Расскажи, пожалуйста, в двух словах, о себе: чем ты занимаешься," +
                    " что ты любишь, почему хочешь поехать с нами?\n" +
                    "(Обещаю, дальше вопросы будут попроще)",
            "есть ли у тебя права и готов ли ты быть водителем (водительницей) одной из машин?",
            "когда в последний раз ты ночевал в палатке и как вообще к ним относишься?",
            "белое или красное?",
            "пришли, пожалуйста, ссылки на свои соц сети (например, фейсбук и инстаграм)",
            "Очень важный вопрос про любимый стикер.\n\n" +
                    "Во-первых нужно выбрать один из множества любимых. \n" +
                    "Во-вторых описать его словами, точнее голосом и прислать аудиосообщение сюда.\n\n" +
                    "Не претендуем на звание стикерных знатоков, но попборуем угадать!"};

    private Map<Long, List<String>> hmChat2Answers;
    private Map<Long, String> hmChat2UserInfo;

    public synchronized ReplyKeyboardMarkup getTripButtons() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
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

        return replyKeyboardMarkup;
    }

    private void setInline() {
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        List<InlineKeyboardButton> buttons1 = new ArrayList<>();
        buttons1.add(new InlineKeyboardButton().setText("Кнопка").setCallbackData("17"));
        buttons.add(buttons1);

        InlineKeyboardMarkup markupKeyboard = new InlineKeyboardMarkup();
        markupKeyboard.setKeyboard(buttons);
    }

    public synchronized void sendMsg(Long chatId, String s) {

        sendMsg(String.valueOf(chatId), s, null);
    }

    @Override
    public synchronized void sendMsg(String chatId, String s) {

        sendMsg(chatId, s, null);
    }

    public synchronized void sendMsg(
            String chatId, String s, ReplyKeyboardMarkup replyKeyboardMarkup) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setText(s);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            //Log.log(Level, "Exception: ", e.toString());
        }
    }

    public synchronized void sendVoice(Long chatId, String fileId) {
        SendVoice voice = new SendVoice();
        voice.setChatId(chatId);
        voice.setVoice(fileId);
        try {
            execute(voice);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            //Log.log(Level, "Exception: ", e.toString());
        }
    }

    public synchronized void sendAudio(Long chatId, String fileId) {
        SendAudio audio = new SendAudio();
        audio.setChatId(chatId);
        audio.setAudio(fileId);
        try {
            execute(audio);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            //Log.log(Level, "Exception: ", e.toString());
        }
    }

    public void onUpdateReceived(Update update) {
        if (!update.hasMessage()) {
            //TODO: Обработку ошибок

            return;
        }
        Long chatId = update.getMessage().getChatId();
        List<String> alAns =
                hmChat2Answers.getOrDefault(chatId, new ArrayList<>());
        if (alAns.size() == 5) {
            if (update.getMessage().hasAudio()) {

                out.println("LOG: onUpdateReceived: audio msg");

                Audio audio = update.getMessage().getAudio();
                String fileId = "a_" + audio.getFileId();
                alAns.add(fileId);

            } else if ( update.getMessage().hasVoice()) {

                Voice voice = update.getMessage().getVoice();
                String fileId = "v_" + voice.getFileId();
                alAns.add(fileId);
            } else {
                sendMsg(chatId, "Я очень извиняюсь, но Коля с Алиной попросили взять у вас именно аудио. Я не думаю, что это оно. Попробуйте еще раз, пожалуйста.");
                return;
            }

            String msgText = "Кайф, спасибо! Передам Коле и Алине все ответы, они свяжутся с тобой в ближайшее время. Если хочешь начать заново, нажми сюда /start";
            sendMsg(chatId, msgText);
            sendResponsesToAdmin(chatId);

        } else if (update.getMessage().hasText())  {

            handleTextMsg(update);
        } else {
            sendMsg(String.valueOf(chatId),
                    "Простите, я что-то не понял что это. А чего мне делать с этим. А вы кто? Простите, я уже старый.");
        }

    }

    private void handleTextMsg(Update update) {
        Message msg = update.getMessage();
        Long chatId = msg.getChatId();

        String message = msg.getText();
        out.println("LOG: onUpdateReceived: msg = " + message);

        if (message.startsWith("/")) {
            out.println("LOG: onUpdateReceived: cmd recieved");
            handleCmd(update);

            return;
        }

        User usr = msg.getFrom();
        String userName = "";
        if (usr != null) {
            userName = usr.getUserName();
        }

        List<String> alAns =
                hmChat2Answers.getOrDefault(chatId, new ArrayList<>());

        out.println("LOG: onUpdateReceived: alAns = [" + alAns.stream().reduce("", (s, s2) -> s + " " + s2) + "]");
        out.println("LOG: onUpdateReceived: has user = [" + hmChat2UserInfo.get(chatId) + "]");

        String msgText = "";
        String chooseOpt = "0";
        ReplyKeyboardMarkup rkM = null;
        if (alAns.size() == 0 && hmChat2UserInfo.get(chatId) == null) {
            chooseOpt = "1";
            msgText = vQuestions[0];
            hmChat2UserInfo.put(chatId, userName);
            rkM = getTripButtons();
        }  else {
            chooseOpt = "3";
            alAns.add(message);
            msgText = vQuestions[alAns.size()];
        }
        out.println("LOG: onUpdateReceived: msg text = [" + msgText + "] $ " + chooseOpt);

        sendMsg(chatId.toString(), msgText, rkM);
    }

    private void handleCmd(Update update) {
        Message msg = update.getMessage();
        String message = msg.getText();
        Long chatId = msg.getChatId();
        Command cmd = Command.byName(message.toLowerCase());
        if (cmd == Command.START) {
            out.println("LOG: onUpdateReceived: deleting answers");
            hmChat2Answers.put(chatId, new ArrayList<>());
            hmChat2UserInfo.put(chatId, null);


            sendMsg(
                    chatId.toString(),
                    "Привет! Тут можно записаться в поездку рута ⚡️", getTripButtons());

        } else if (cmd == Command.COMMAND_LIST) {

        } else if (cmd == Command.SEND_JOKE) {
            sendMsg(String.valueOf(chatId),"Шутка - хуютка!");
        } else {
            sendMsg(String.valueOf(chatId),
                    "Простите, я уже не помню такой команды, я пенс. Вот команды, которые я знаю:");

        }
    }


    private void handlePhotoMsg(Update update) {
        List<PhotoSize> photos = update.getMessage().getPhoto();

        String fileId = photos.stream()
                .sorted(Comparator.comparing(PhotoSize::getFileSize).reversed())
                .findFirst()
                .orElse(null).getFileId();

        String caption = photos.stream().map(photoSize -> photoSize.toString()).reduce("", String::concat);
        SendPhoto msg = new SendPhoto();
        msg.setPhoto(fileId);
        msg.setChatId(adminChatId);
        msg.setCaption("Вот ваш файл: " + caption);

        try {
            execute(msg);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleDocMsg(Update update) {

        Document doc = update.getMessage().getDocument();

        String fileId = doc.getFileId();

        String caption = doc.toString();
        SendDocument msg = new SendDocument();
        msg.setDocument(fileId);
        msg.setChatId(adminChatId);
        msg.setCaption("Вот ваш файл: " + caption);

        try {
            execute(msg);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendResponsesToAdmin(Long chatId) {
        String userName = hmChat2UserInfo.get(chatId);
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z\n\n");
        Date date = new Date(System.currentTimeMillis());

        sb.append("Ответ пользователя @")
                .append(userName)
                .append(". Время: ").append(formatter.format(date));
        List<String> alAns = hmChat2Answers.get(chatId);
        if (alAns.size() != vQuestions.length) {
            out.println("Пользователь еще не закончил отвечать на вопросы. ["
                    + alAns.size() + "], [" + vQuestions.length + "]");
            return;
        }
        for (int i = 0; i < vQuestions.length-2; i++) {
            sb.append("Вопрос: *").append(vQuestions[i]).append("*\n")
                    .append("Ответ: *").append(alAns.get(i)).append("*\n\n");
        }
        sb.append("Вопрос: *").append(vQuestions[vQuestions.length-1]).append("*\n");
        sendMsg(chatId, sb.toString());
        String fileId = alAns.get(5);
        if ( fileId.startsWith("v_")) {
            sendVoice(chatId, fileId.substring(2));
        } else if(fileId.startsWith("a_")) {
            sendAudio(chatId, fileId.substring(2));
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

