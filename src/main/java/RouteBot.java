
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
import redis.clients.jedis.Jedis;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.System.out;

public class RouteBot extends Bot {

    private static final String name = "helloroute_bot";
    private static final String token = "1013761197:AAHv3uKJJzwiWMsQVxgnxUsqbpP5-PSrRy4";
    private static final Long adminChatId = 3099992L;
    private static final Set<Long> hsAdminChatId = new HashSet<>();

    static {
        hsAdminChatId.add(3099992L);
        hsAdminChatId.add(96353936L);
    }

    RouteBot() {
        super(name, token);
        hmChat2Answers = new HashMap<>();
        hmChat2UserInfo = new HashMap<>();
        hmChat2Trip = new HashMap<>();
    }

    private enum Command {
        HELP("/", "Список всех команд."),
        START("/start", "Начать все сначала."),
        SEND_RESPONSES("/send_resp", ""),
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
            for (Command cmd : Command.values()) {
                cmdByName.put(cmd.name, cmd);
            }
        }
    }

    private static final String[] vTrips =
            new String[]{"Хочу на кавказ и на парапланы! 10-12 июля",
                    "Хочу Калиниград, море, песочек! 22-26 июля"};
    private static final String[][] vQuestions = new String[vTrips.length][2];
    static {
        vQuestions[0] = new String[]{
                "Классно, что ты решил присоединиться к нашей поездке!\n\n" +
                        "Расскажи, пожалуйста, в двух словах, о себе: чем ты занимаешься," +
                        " что ты любишь, почему хочешь поехать с нами?\n" +
                        "(Обещаю, дальше вопросы будут попроще)",
                "Когда в последний раз ты ночевал в палатке и как вообще к ним относишься?\n",
                "Белое или красное?",
                "Пришли, пожалуйста, ссылки на свои соц сети (например, фейсбук и инстаграм)",
                "Я не смог получить твой ник в телеграмме, поэтому пришли, пожалуйста, свой телефон, чтобы мы точно могли с тобой связаться.",
                "Давай начнем разговор! Пожалуйста, запиши аудиосообщение про то, собираешься ли ты полетать с нами на парапланах и как ты себе это представляешь?\n" +
                        "А можешь записать, как будто ты уже на параплане?\n"};
        vQuestions[1] = new String[]{
                "Классно, что ты решил присоединиться к нашей поездке!\n" +
                        "Чтобы оставить заявку – ответь, пожалуйста, на наши вопросы. " +
                        "Тебе понадобится 5 свободных минут и не стесняться.",
                "Расскажи, пожалуйста, в двух словах, о себе: чем ты занимаешься," +
                        " что ты любишь, почему хочешь поехать с нами?\n" +
                        "(Обещаю, дальше вопросы будут попроще)",
                "Есть ли у тебя свой проект или очень серьезное хобби," +
                        " про которое хочется рассказать? Если есть – расскажи!",
                "Когда в последний раз ты ночевал в палатке и как вообще к ним относишься?",
                "Белое или красное?",
                "Есть ли у тебя права и готов ли ты быть водителем (водительницей) одной из машин?",
                "Пришли, пожалуйста, ссылки на свои соц сети (например, фейсбук и инстаграм)",
                "Я не смог получить твой ник в телеграмме, поэтому пришли," +
                        " пожалуйста, свой телефон, чтобы мы точно могли с тобой связаться.",
                "Ты когда-нибудь пробовал серфить? Как прошло?" +
                        "Хочешь ли повторить или готов ли попробовать с нами на Косе? " +
                        "*Запиши аудиосообщение* про свой опыт / ожидания / или" +
                        " как ты обычно себя подбадриваешь, попадая в очередную мясорубку из волн?\n" +
                        "Если стесняешься – запиши шепотом, мы все услышим."};

    }

    private Map<Long, List<String>> hmChat2Answers;
    private Map<Long, String> hmChat2UserInfo;
    private Map<Long, Integer> hmChat2Trip;


    public synchronized ReplyKeyboardMarkup getTripButtons() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(true);

        List<KeyboardRow> alKeyboardRows = new ArrayList<>();
        for (String trip : vTrips) {
            KeyboardRow keyboardFirstRow = new KeyboardRow();
            keyboardFirstRow.add(new KeyboardButton(trip));
            alKeyboardRows.add(keyboardFirstRow);
        }
        replyKeyboardMarkup.setKeyboard(alKeyboardRows);

        return replyKeyboardMarkup;
    }


    public synchronized ReplyKeyboardMarkup getCancelButton() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(true);

        List<KeyboardRow> alKeyboardRows = new ArrayList<>();
        KeyboardRow keyboardFirstRow = new KeyboardRow();
        keyboardFirstRow.add(new KeyboardButton("Отмена"));
        alKeyboardRows.add(keyboardFirstRow);
        replyKeyboardMarkup.setKeyboard(alKeyboardRows);

        return replyKeyboardMarkup;
    }

    public synchronized ReplyKeyboardMarkup getGoAndCancelButton() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> alKeyboardRows = new ArrayList<>();
        KeyboardRow keyboardFirstRow = new KeyboardRow();
        keyboardFirstRow.add(new KeyboardButton("Поехали"));
        alKeyboardRows.add(keyboardFirstRow);

        keyboardFirstRow = new KeyboardRow();
        keyboardFirstRow.add(new KeyboardButton("Отмена"));
        alKeyboardRows.add(keyboardFirstRow);

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

    public synchronized void sendMsgNoMarkDown(
            Long chatId, String s) {
        debi("sendMsg: ",chatId +" = " + s);
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(false);
        sendMessage.setChatId(chatId);
        sendMessage.setText(s);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            //Log.log(Level, "Exception: ", e.toString());
        }
    }


    public synchronized void sendMsg(Long chatId, String s) {

        sendMsg(String.valueOf(chatId), s, null);
    }

    public synchronized void sendMsg(String chatId, String s) {

        sendMsg(chatId, s, null);
    }

    public synchronized void sendMsg(
            Long chatId, String s, ReplyKeyboardMarkup replyKeyboardMarkup) {

        sendMsg(String.valueOf(chatId), s, replyKeyboardMarkup);
    }

    public synchronized void sendMsg(
            String chatId, String s, ReplyKeyboardMarkup replyKeyboardMarkup) {
        debi("sendMsg: ",chatId +" = " + s);
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setText(s);
        if (replyKeyboardMarkup == null) {
            replyKeyboardMarkup = getCancelButton();
        }
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            //Log.log(Level, "Exception: ", e.toString());
        }
    }

    public synchronized void sendMsgNoKeybord(
            String chatId, String s) {
        debi("sendMsg: ",chatId +" = " + s);
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setText(s);

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
        String methodLogPrefix = "onUpdateReceived: ";
        debi(update.toString());

        if (!update.hasMessage()) {
            //TODO: Обработку ошибок
            debi(methodLogPrefix, "no msg");

            return;
        }
        Message updMsg = update.getMessage();
        Long chatId = updMsg.getChatId();
        debi(methodLogPrefix, "chatId = " + chatId);

        if (updMsg.hasText() && !hsAdminChatId.contains(chatId)) {
            sendMsg2Admins(updMsg);
        }

        if (updMsg.hasText() &&
                updMsg.getText().startsWith("/")) {
            handleCmd(update);

            return;
        }

        Integer trip = getUserTrip(chatId);
        debi(methodLogPrefix, "trip = " + trip);
        if (updMsg.hasText() &&
                updMsg.getText().equals("Отмена")) {
            hmChat2Answers.put(chatId, new ArrayList<>());
            hmChat2UserInfo.put(chatId, null);
            hmChat2Trip.put(chatId, null);
            removeAllUserData(chatId, trip);

            sendMsg(
                    chatId.toString(),
                    "Все забыл️. Чтобы начать заново, выбери поездку и нажми на нее.", getTripButtons());

            return;
        }

        List<String> alAns = getUserAnswers(chatId, trip);
        if (alAns == null) {
            alAns = new ArrayList<>();
        }
        if (alAns.size() == vQuestions[trip].length - 1) {

            handleVoiceAudioMsg(update, alAns, chatId);
        } else if (updMsg.hasText())  {

            handleTextMsg(update);
        } else {
            sendMsg(String.valueOf(chatId),
                    "Простите, я что-то не понял что это. А чего мне делать с этим. А вы кто? Простите, я уже старый.");
        }
    }

    private void handleVoiceAudioMsg(Update update,  List<String> alAns, Long chatId) {
        if (update.getMessage().hasAudio()) {

            out.println("LOG: onUpdateReceived: audio msg");

            Audio audio = update.getMessage().getAudio();
            String fileId = "a_" + audio.getFileId();
            alAns.add(fileId);
            Jedis jedis = Helper.getConnection();
            jedis.set("a" + chatId + "_" + alAns.size(), fileId);

        } else if ( update.getMessage().hasVoice()) {

            Voice voice = update.getMessage().getVoice();
            String fileId = "v_" + voice.getFileId();
            alAns.add(fileId);
            Jedis jedis = Helper.getConnection();
            jedis.set("a" + chatId + "_" + alAns.size(), fileId);

        } else {
            sendMsg(chatId, "Я очень извиняюсь, но Коля с Алиной попросили взять у вас именно аудио. Я не думаю, что это оно. Попробуйте еще раз, пожалуйста.");
            return;
        }

        String msgText = "Кайф, спасибо! Передам Коле и Алине все ответы, они свяжутся с тобой в ближайшее время. Если хочешь начать заново, нажми сюда /start";
        sendMsgNoKeybord(String.valueOf(chatId), msgText);
        sendResponsesToAdmin(chatId);
        hmChat2Answers.put(chatId, new ArrayList<>());
        hmChat2UserInfo.put(chatId, null);
        removeAllUserData(chatId, getUserTrip(chatId));
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
        String userName = getUserStr(usr);

        Integer tripIdx = getUserTrip(chatId);
        List<String> alAns = getUserAnswers(chatId, tripIdx);

        out.println("LOG: onUpdateReceived: alAns = [" + alAns.stream().reduce("", (s, s2) -> s + ";" + s2) + "]");
        out.println("LOG: onUpdateReceived: has user = [" + hmChat2UserInfo.get(chatId) +  "|" + getUserName(chatId) + "]");

        String msgText = "";
        String chooseOpt = "0";

        Jedis jedis = Helper.getConnection();
        String userInfo = getUserName(chatId);
        if (alAns.size() == 0 &&
                userInfo == null && Arrays.stream(vTrips).noneMatch(message::equals)) {

            sendMsg(String.valueOf(chatId),
                    "Простите, я что-то не понял что это. А чего мне делать с этим." +
                            " А вы кто? Простите, я уже старый.", getTripButtons());

            return;
        } else if (alAns.size() == 0 && userInfo == null) {
            chooseOpt = "1";
            tripIdx = getTripIdx(message);
            hmChat2Trip.put(chatId,tripIdx);
            jedis.set("t" + chatId, String.valueOf(tripIdx));
            hmChat2UserInfo.put(chatId, userName);
            jedis.set("n" + chatId, userName);

            msgText = vQuestions[tripIdx][0];
            if (tripIdx == 1) {
                sendMsg(chatId, msgText, getGoAndCancelButton());

                return;
            }
        }  else {
            chooseOpt = "3";
            alAns.add(message);
            jedis.set("a" + chatId + "_" + alAns.size(), message);
            msgText = vQuestions[tripIdx][alAns.size()];
            if (msgText.startsWith("Я не смог получить твой ник") &&
                    usr.getUserName() != null) {
                alAns.add("");
                jedis.set("a" + chatId + "_" + alAns.size(), "");
                msgText = vQuestions[tripIdx][alAns.size()];
            }
        }
         debi("msg text = [" + msgText + "] $ " + chooseOpt);

        sendMsg(chatId, msgText);
    }

    private void handleCmd(Update update) {
        String methodLogPrefix = "handleCmd: ";
        Message msg = update.getMessage();
        String message = msg.getText();
        Long chatId = msg.getChatId();
        String fullMsg = message.toLowerCase();
        if (fullMsg.contains(" ")) {
            message = fullMsg.split(" ")[0];
        }
        Command cmd = Command.byName(message.toLowerCase());
        if (cmd == Command.START) {
            out.println("LOG: onUpdateReceived: deleting answers");
            hmChat2Answers.put(chatId, new ArrayList<>());
            hmChat2UserInfo.put(chatId, null);
            hmChat2Trip.put(chatId, null);
            debi(methodLogPrefix, "maps cleared");
            removeAllUserData(chatId, getUserTrip(chatId));
            debi(methodLogPrefix, "redis cleared");
            sendMsg(
                    chatId, "Привет! Тут можно записаться в поездку рута ⚡️",
                    getTripButtons());
            debi(methodLogPrefix, "msg send");
        } else if (cmd == Command.SEND_RESPONSES) {

            handleSendResponses(fullMsg);
        } else if (cmd == Command.HELP) {

        } else if (cmd == Command.SEND_JOKE) {
            sendMsg(String.valueOf(chatId),"Шутка - хуютка!");
        } else {
            sendMsg(String.valueOf(chatId),
                    "Простите, я уже не помню такой команды, я пенс. Вот команды, которые я знаю:");

        }
    }

    private void handleSendResponses(String fullCmdString) {
        String methodLogPrefix = "handleSendResponses: ";
        debi(methodLogPrefix, "starts");
        if (!fullCmdString.contains(" ")) {
            return;
        }
        long chatId = Helper.s2l(fullCmdString.split(" ")[1]);
        debi(methodLogPrefix, "" + chatId);
        if (chatId > 0) {
            debi(methodLogPrefix, "chatId > 0");
            sendResponsesToAdmin(chatId);
        }

    }

    private void handlePhotoMsg(Update update) {
        List<PhotoSize> photos = update.getMessage().getPhoto();

        String fileId = photos.stream()
                .sorted(Comparator.comparing(PhotoSize::getFileSize).reversed())
                .findFirst()
                .orElse(null).getFileId();

        String caption = photos.stream().map(PhotoSize::toString).reduce("", String::concat);
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
        String methodLogPrefix = "sendResponsesToAdmin: ";
        debi(methodLogPrefix, "starts");

        String userName = getUserName(chatId);
        Integer trip = getUserTrip(chatId);
        debi(methodLogPrefix, "userName: " + userName + " trip: " + trip);
        if (userName == null || userName.isEmpty()) {
            debi(methodLogPrefix, "cannot find user");

            return;
        }
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z\n\n");
        Date date = new Date(System.currentTimeMillis());

        sb.append("Ответ от:  ")
                .append(Helper.escapeChars(userName))
                .append(". Время: ").append(formatter.format(date));
        List<String> alAns = getUserAnswers(chatId, trip);
        debi(methodLogPrefix, "ans: " + alAns);
        int numAns = alAns.size() == vQuestions[trip].length ? vQuestions[trip].length-1 : alAns.size();
        for (int i = 0; i < numAns; i++) {
            sb.append("Вопрос: ").append(vQuestions[trip][i]).append("\n")
                    .append("Ответ: ").append(alAns.get(i)).append("\n\n");
        }
        if (alAns.size() == vQuestions[trip].length) {
            sb.append("Вопрос: ").append(vQuestions[trip][vQuestions[trip].length - 1]).append("\n");
        }
        String responses = sb.toString();
        hsAdminChatId.
                forEach(adminChatId -> sendMsgNoMarkDown(adminChatId, responses));

        if (alAns.size() == vQuestions[trip].length) {
            String fileId = alAns.get(vQuestions[trip].length - 1);
            hsAdminChatId.
                    forEach(adminChatId -> {
                        if (fileId.startsWith("v_")) {
                            sendVoice(adminChatId, fileId.substring(2));
                        } else if (fileId.startsWith("a_")) {
                            sendAudio(adminChatId, fileId.substring(2));
                        }
                    });
        }
    }

    private String getUserStr(User user) {
        return "Пользователь: " + user.getId() + " ник: @" + user.getUserName() +
                " имя: " + user.getFirstName() + " фамилия: " + user.getLastName();
    }

    private void  sendMsg2Admins(Message msg) {
        User usr = msg.getFrom();
        String userName = getUserStr(usr);
        String msgText = msg.getText();

        hsAdminChatId.
                forEach(adminChatId -> sendMsgNoMarkDown(adminChatId, userName + "\n" + msgText));
    }

    private String getUserName(Long chatId) {
        String userName = hmChat2UserInfo.get(chatId);
        try {
            if (userName == null) {
                Jedis jedis = Helper.getConnection();
                if (jedis != null) {
                    debi("redis len = " + jedis.llen("n" + chatId));
                    userName = jedis.get("n" + chatId);
                    if (userName != null) {
                        hmChat2UserInfo.put(chatId, userName);
                    }
                }
            }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        return userName;
    }

    private List<String> getUserAnswers(Long chatId, int trip) {
        List<String> answers = hmChat2Answers.get(chatId);
        try {
            if (answers == null) {
                Jedis jedis = Helper.getConnection();
                if (jedis != null) {
                    answers = new ArrayList<>();
                    for (int i = 1; i<= vQuestions[trip].length; i++) {
                        String answer = jedis.get("a" + chatId + "_" + i);
                        if (answer == null) {
                            break;
                        }
                        answers.add(answer);
                    }
                    hmChat2Answers.put(chatId, answers);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return answers;
    }

    private Integer getUserTrip(Long chatId) {
        Integer trip = hmChat2Trip.get(chatId);
        try {
            if (trip == null) {
                Jedis jedis = Helper.getConnection();
                if (jedis != null) {
                    trip = Helper.s2i(jedis.get("t" + chatId));
                    hmChat2Trip.put(chatId, trip);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return trip == null ? 0: trip;
    }

    private int getTripIdx(String message) {
        int idx = -1;
        for (int i = 0; i<vTrips.length; i++) {
            if (vTrips[i].equals(message)) {
                idx = i;
            }
        }

        return idx;
    }

    private void removeAllUserData(Long chatId, int trip) {
        try {
            Jedis jedis = Helper.getConnection();
            if (jedis.exists("n" + chatId)) {
                jedis.del("n" + chatId);
            }
            if (jedis.exists("t" + chatId)) {
                jedis.del("t" + chatId);
            }
            for (int i = 1; i <= vQuestions[trip].length; i++) {
                if (jedis.exists("a" + chatId + "_" + i)) {
                    jedis.del("a" + chatId + "_" + i);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void debe(String... strings) {
        StringBuilder sb = new StringBuilder();
        sb.append("LOG: ERR: ");
        for (String string : strings) {
            sb.append(string);
        }
        System.out.println(sb.toString());
    }

    private static void debi(String... strings) {
        StringBuilder sb = new StringBuilder();
        sb.append("LOG: ");
        for (String string : strings) {
            sb.append(string);
        }
        System.out.println(sb.toString());
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

