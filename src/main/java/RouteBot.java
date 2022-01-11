
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendAudio;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVoice;
import org.telegram.telegrambots.meta.api.objects.Audio;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.Voice;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

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
    private static final Long debugChatId = -487931131L;
    private Map<Long, MailinigState> hsChatId2MailingState = new HashMap<>();
    private Map<Long, String> hsChatId2MailingMsg = new HashMap<>();
    private Map<Long, String> hsChatId2MailingFile = new HashMap<>();

    static {
        hsAdminChatId.add(3099992L);
        hsAdminChatId.add(96353936L);
        hsAdminChatId.add(489194L);
    }

    private enum MailinigState {
        BEGAN,
        MSG_RECIEVED,
        END;
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
        GET_USERS("/get_users", ""),
        MAILING("/mailing", "Запустить произвольную рассылку"),
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
            new String[]{"Хочу в Поляну Красную"};

    private static final String[][] vQuestions = new String[vTrips.length][];
    static {
//                vQuestions[1] = new String[] {
//                "Классно, что ты решил присоединиться к нашей поездке!\n" +
//                        "Чтобы оставить заявку – ответь, пожалуйста, на наши вопросы. " +
//                        "Тебе понадобится 5 свободных минут и не стесняться.",
//
//                "Расскажи, пожалуйста, в двух словах, о себе: чем ты занимаешься," +
//                        " что ты любишь, почему хочешь поехать с нами?\n" +
//                        "(Обещаю, дальше вопросы будут попроще)",
//
//                "Ночевка в спальниках во дворце — это курьёза или не comme il faut?",
//
//                "Ты голубчик или шельмец?",
//
//                "Пришли, пожалуйста, ссылки на свои соц сети (например, фейсбук и инстаграм)",
//
//                "Я не смог получить твой ник в телеграмме, поэтому пришли," +
//                        " пожалуйста, свой телефон, чтобы мы точно могли с тобой связаться.",
//
//                "Запиши аудиосообщение, в котором ты рассказываешь своему лакею, какой парик нужно тебе достать для бала во дворце на Английской набережной"};
//
//        vQuestions[0] = new String[]{
//                "Классно, что ты решил присоединиться к нашей поездке!\n" +
//                        "Чтобы оставить заявку – ответь, пожалуйста, на наши вопросы. " +
//                        "Тебе понадобится 5 свободных минут и не стесняться. \n" +
//                        "Первые пара вопросов для новичков, если мы уже знакомы – " +
//                        "смело ответь всякую дичь на них.",
//
//                "Расскажи, пожалуйста, в двух словах, о себе: чем ты занимаешься," +
//                        " что ты любишь, почему хочешь поехать с нами?\n" +
//                        "(Обещаю, дальше вопросы будут попроще. Или нет. Но точно не надо быть серьезным)",
//
//                "Пришли, пожалуйста, ссылки на свои соц сети (например, фейсбук и инстаграм)",
//
//                "Я не смог получить твой ник в телеграмме, поэтому пришли," +
//                " пожалуйста, свой телефон, чтобы мы точно могли с тобой связаться.",
//
//                "Когда ты в последний раз ночевал в палатке, и как относишься к такому формату отдыха?",
//
//                "Какой атрибут обязательно нужно взять для путешествия в стиле Королевства полной луны?",
//
//                "Серфинг в России - это фан или слёзы?",
//
//                "Попробуй передать в аудиосообщении звуки ретро-тачки 80х годов."};

//        vQuestions[0] = new String[]{
//                "Классно, что ты решил присоединиться к нашей поездке!\n" +
//                        "Чтобы оставить заявку – ответь, пожалуйста, на наши вопросы. " +
//                        "Тебе понадобится 5 свободных минут и не стесняться. \n" +
//                        "Первые пара вопросов для новичков, если мы уже знакомы – " +
//                        "смело ответь всякую дичь на них.",
//
//                "Расскажи, пожалуйста, в двух словах, о себе: чем ты занимаешься," +
//                        " что ты любишь, почему хочешь поехать с нами?\n" +
//                        "(Обещаю, дальше вопросы будут попроще. Или нет. Но точно не надо быть серьезным)",
//
//                "Когда в последний раз ты ночевал(а) в палатке и как вообще к ним относишься?",
//
//                "Есть ли что-то, ради чего ты готов(а) рано проснуться? Расскажи про последний такой случай.",
//
//                "Пришли, пожалуйста, ссылки на свои соц сети (например, фейсбук и инстаграм)",
//
//                "Я не смог получить твой ник в телеграмме, поэтому пришли," +
//                " пожалуйста, свой телефон, чтобы мы точно могли с тобой связаться.",
//
//                "Давай начнем разговор! Пожалуйста, запиши аудиосообщение про то, собираешься ли ты полетать с нами на парапланах и как ты себе это представляешь? А можешь записать, как будто ты уже на параплане?"};

//                vQuestions[0] = new String[]{
//                    "Классно, что ты решил присоединиться к нашей поездке!\n" +
//                        "Чтобы оставить заявку – ответь, пожалуйста, на наши вопросы. " +
//                        "Тебе понадобится 5 свободных минут и не стесняться.",
//
//                    "Расскажи, пожалуйста, в двух словах, о себе: чем ты занимаешься," +
//                        " что ты любишь, почему хочешь поехать с нами?\n" +
//                        "(Обещаю, дальше вопросы будут попроще. Или нет. Но точно не надо быть серьезным)",
//
//                    "Пришли, пожалуйста, ссылки на свои соц сети (например, фейсбук и инстаграм)",
//
//                    "Я не смог получить твой ник в телеграмме, поэтому пришли," +
//                        " пожалуйста, свой телефон, чтобы мы точно могли с тобой связаться.",
//
//                    "Какой атрибут обязательно нужно взять в Дагестан? А из Дагестана?",
//
//                    "День, в который ты прошел самое большое количество шагов — что это был за день?",
//
//                    "Запиши аудиосообщение с коротким тостом. За самый лучший чокнемся в горах на закате!"};

//        vQuestions[0] = new String[]{
//                    "Классно, что ты решил присоединиться к нашей поездке!\n" +
//                        "Чтобы оставить заявку – ответь, пожалуйста, на наши вопросы. " +
//                        "Тебе понадобится 5 свободных минут и не стесняться.",
//
//                    "Расскажи, пожалуйста, в двух словах, о себе: чем ты занимаешься," +
//                        " что ты любишь, почему хочешь поехать с нами?\n" +
//                        "(Обещаю, дальше вопросы будут попроще. Или нет. Но точно не надо быть серьезным)",
//
//                    "Гоняешь на горном велике или даже на мотоцикле?",
//
//                    "От чего в последний раз захватывало дух?",
//
//                    "Если бы ты был(а) настойкой, то какой?",
//
//                    "Пришли, пожалуйста, ссылки на свои соц сети (например, фейсбук и инстаграм)?",
//
//                    "Я не смог получить твой ник в телеграмме, поэтому пришли," +
//                        " пожалуйста, свой телефон, чтобы мы точно могли с тобой связаться.",
//
//                    "Запиши в аудиосообщении звуки-ощущения-настроения от прыжка в бодряющую купель после бани."};

//        vQuestions[0] = new String[]{
//
//            "Классно, что ты решил присоединиться к нашей поездке!\n" +
//                "Чтобы оставить заявку – ответь, пожалуйста, на наши вопросы. " +
//                "Тебе понадобится 5 свободных минут и не стесняться.",
//
//            "Расскажи, пожалуйста, в двух словах, о себе: чем ты занимаешься," +
//                " что ты любишь, почему хочешь поехать с нами?\n" +
//                "(Обещаю, дальше вопросы будут попроще. Или нет. Но точно не надо быть серьезным)",
//
//            "Есть ли у тебя опыт вождения механики или даже автодома? Хочешь ли быть водителем (водительницей) одного из домов?",
//
//            "Что обязательно нужно взять с собой, чтобы устроить уютную и комфортную van life?",
//
//            "Пришли, пожалуйста, ссылки на свои соц сети (например, фейсбук и инстаграм)?",
//
//            "Я не смог получить твой ник в телеграмме, поэтому пришли," +
//                " пожалуйста, свой телефон, чтобы мы точно могли с тобой связаться.",
//
//            "Коронный вопрос нашего бота — нужно записать аудиосообщение! Задача: изобрази звук, когда зажевало кассету (биотуалета \uD83E\uDD22)"};
//
//        vQuestions[1] = new String[]{
//            "Классно, что ты решил присоединиться к нашей поездке!\n" +
//                "Чтобы оставить заявку – ответь, пожалуйста, на наши вопросы. " +
//                "Тебе понадобится 5 свободных минут и не стесняться.",
//
//            "Расскажи, пожалуйста, в двух словах, о себе: чем ты занимаешься," +
//                " что ты любишь, почему хочешь поехать с нами?\n" +
//                "(Обещаю, дальше вопросы будут попроще. Или нет. Но точно не надо быть серьезным)",
//
//            "Катаешься ли ты на лыжах или сноуборде? " +
//                "цени свой уровень по шкале от 1 до 10, где 1 - забыл, какая нога ведующая, а 10 - уверенный рассекатель пухляка.",
//
//            "Какую вещь обязательно нужно взять с собой, чтобы создать уют в нашем коливинге? Воронка, шахматы, проектор и рецепт самодельных пельменей — уже выбрали!",
//
//            "Пришли, пожалуйста, ссылки на свои соц сети (например, фейсбук и инстаграм)?",
//
//            "Я не смог получить твой ник в телеграмме, поэтому пришли," +
//                " пожалуйста, свой телефон, чтобы мы точно могли с тобой связаться.",
//
//            "Запиши аудиосообщение: звуки или внутренний голос во время твоего первого раза на бугеле."};

        vQuestions[0] = new String[]{
            "Классно, что ты решил присоединиться к нашей поездке!\n" +
                "Чтобы оставить заявку – ответь, пожалуйста, на наши вопросы. " +
                "Тебе понадобится 5 свободных минут и не стесняться.",

            "Расскажи, пожалуйста, в двух словах, о себе: чем ты занимаешься," +
                " что ты любишь, почему хочешь поехать с нами?\n" +
                "(Обещаю, дальше вопросы будут попроще. Или нет. Но точно не надо быть серьезным)",

            "Катаешься ли ты на лыжах или сноуборде? " +
                "цени свой уровень по шкале от 1 до 10, где 1 - забыл, какая нога ведующая, а 10 - уверенный рассекатель пухляка.",

            "Какая будет твоя роль в бобслейной команде?" +

            "Какую вещь обязательно нужно взять с собой, чтобы создать уют в нашем коливинге? Воронка, шахматы, проектор и рецепт самодельных пельменей — уже выбрали!",

            "Пришли, пожалуйста, ссылки на свои соц сети (например, фейсбук и инстаграм)?",

            "Я не смог получить твой ник в телеграмме, поэтому пришли," +
                " пожалуйста, свой телефон, чтобы мы точно могли с тобой связаться.",

            "Коронный вопрос нашего бота — нужно записать аудиосообщение! Задача: каким звуком будет начинаться и заканчиваться день в нашем доме-коворкинге?"};

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

    public InlineKeyboardMarkup getInlineKeyBoardWithTrips() {
        if (vTrips.length == 0) {

            return null;
        }

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();

        for (int i = 0; i < vTrips.length; i ++) {
            InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
            inlineKeyboardButton.setText(vTrips[i]);
            inlineKeyboardButton.setCallbackData("t_" + i);
            List<InlineKeyboardButton> keyboardButtonsRow = new ArrayList<>();
            keyboardButtonsRow.add(inlineKeyboardButton);
            rowList.add(keyboardButtonsRow);
        }
        inlineKeyboardMarkup.setKeyboard(rowList);

        return inlineKeyboardMarkup;
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
        replyKeyboardMarkup.setOneTimeKeyboard(true);

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

        sendMsg(String.valueOf(chatId), s, null, null);
    }

    public synchronized void sendMsg(String chatId, String s) {

        sendMsg(chatId, s, null, null);
    }

    public synchronized void sendMsg(
            Long chatId, String s, ReplyKeyboardMarkup replyKeyboardMarkup) {

        sendMsg(String.valueOf(chatId), s, replyKeyboardMarkup);
    }

    public synchronized void sendMsg(
            String chatId, String s, ReplyKeyboardMarkup replyKeyboardMarkup,
            InlineKeyboardMarkup inlineKeyboardMarkup) {
        debi("sendMsg: ",chatId +" = " + s);
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setText(s);
        if (inlineKeyboardMarkup != null) {
            sendMessage.setReplyMarkup(inlineKeyboardMarkup);
        }  else {
            if (replyKeyboardMarkup == null) {
                replyKeyboardMarkup = getCancelButton();
            }
            sendMessage.setReplyMarkup(replyKeyboardMarkup);
        }

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            //Log.log(Level, "Exception: ", e.toString());
        }
    }

    public synchronized void sendMsg(
        String chatId, String s, ReplyKeyboardMarkup replyKeyboardMarkup) {

        sendMsg(chatId, s, replyKeyboardMarkup, null);
    }

    public synchronized void sendMsg(
        String chatId, String s, InlineKeyboardMarkup inlineKeyboardMarkup) {

        sendMsg(chatId, s, null, inlineKeyboardMarkup);
    }

    public synchronized void sendMsgNotSafe(
        String chatId, String s, ReplyKeyboardMarkup replyKeyboardMarkup) throws Exception {
        debi("sendMsg: ",chatId +" = " + s);
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setText(s);
        ReplyKeyboardRemove replyKeyboardRemove = new ReplyKeyboardRemove();
        sendMessage.setReplyMarkup(replyKeyboardRemove);
        execute(sendMessage);
    }

    public synchronized void sendMsgNoKeyboard(
            String chatId, String s) {
        debi("sendMsg: ",chatId +" = " + s);
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        ReplyKeyboardRemove replyKeyboardRemove = new ReplyKeyboardRemove();
        sendMessage.setReplyMarkup(replyKeyboardRemove);
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

    public synchronized void sendPhoto(
        Long chatId, String caption, String fileId) throws Exception {
        SendPhoto msg = new SendPhoto();
        msg.setChatId(chatId);
        msg.setPhoto(fileId);
        msg.setCaption(caption);
        execute(msg);
    }

    private synchronized void handleCallbackQuery(Update update) {
        String methodLogPrefix = "handleCallbackQuery: ";
        if (!update.hasCallbackQuery()) {
            debe(methodLogPrefix, "No callback query");

            return;
        }
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        String message = update.getCallbackQuery().getData();
        User usr = update.getCallbackQuery().getFrom();

        String userName = getUserStr(usr);

        Jedis jedis = Helper.getConnection();
        Integer tripIdx = Helper.s2i(message.substring(2));

        hmChat2Trip.put(chatId,tripIdx);
        jedis.set("t" + chatId, String.valueOf(tripIdx));
        hmChat2UserInfo.put(chatId, userName);
        jedis.set("n" + chatId, userName);

        String msgText = vQuestions[tripIdx][0];
        if (tripIdx == 0 || tripIdx == 1) {
            sendMsg(chatId, msgText, getGoAndCancelButton());

        } else {
            sendMsg(chatId, msgText);
        }
    }

    public void onUpdateReceived(Update update) {
        String methodLogPrefix = "onUpdateReceived: ";
        debi(update.toString());

        if (update.hasCallbackQuery()) {
            handleCallbackQuery(update);

            return;
        }

        if (!update.hasMessage()) {
            debi(methodLogPrefix, "no msg");

            return;
        }
        Message updMsg = update.getMessage();
        Long chatId = updMsg.getChatId();
        if (debugChatId == chatId) {

            return;
        }
        addChatToDB(chatId);
        debi(methodLogPrefix, "chatId = " + chatId);

        if (updMsg.hasText() && !hsAdminChatId.contains(chatId)) {
            sendMsg2Admins(updMsg);
        }

        if (updMsg.hasText() &&
                updMsg.getText().startsWith("/")) {
            handleCmd(update);

            return;
        }
        if (hsAdminChatId.contains(chatId)) {
            if (hsChatId2MailingState.get(chatId) != null &&
                hsChatId2MailingState.get(chatId) != MailinigState.END) {
                if (updMsg.hasPhoto()) {
                    String fileId = updMsg.getPhoto().stream()
                        .sorted(Comparator.comparing(PhotoSize::getFileSize).reversed())
                        .findFirst()
                        .orElse(null).getFileId();

                    String caption = updMsg.getCaption();
                    handleMailing(chatId, caption, fileId);
                } else {
                    handleMailing(chatId, updMsg.getText(), null);
                }
                return;
            }
        }

        Integer trip = getUserTrip(chatId);
        debi(methodLogPrefix, "trip = " + trip);
        if (updMsg.hasText() &&
                updMsg.getText().equalsIgnoreCase("отмена")) {
            hmChat2Answers.put(chatId, new ArrayList<>());
            hmChat2UserInfo.put(chatId, null);
            hmChat2Trip.put(chatId, null);
            removeAllUserData(chatId, trip);

            sendMsgNoKeyboard(
                    chatId.toString(),
                    "Все забыл️. Чтобы начать заново нажми /start");

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
            sendMsg(chatId, "Я очень извиняюсь, но Коля с Алиной попросили взять у вас именно аудио. Я не думаю, что это оно. Попробуйте еще раз, пожалуйста. Без этого я не смогу передать твою заявку.");
            return;
        }

        String msgText = "Кайф, спасибо! Передам Коле и Алине все ответы, они свяжутся с тобой в ближайшее время. Если хочешь начать заново, нажми сюда /start";
        sendMsgNoKeyboard(String.valueOf(chatId), msgText);
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
            if (tripIdx == 0 || tripIdx == 1) {
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
            //addChatToDB(chatId);
            hmChat2Answers.put(chatId, new ArrayList<>());
            hmChat2UserInfo.put(chatId, null);
            hmChat2Trip.put(chatId, null);
            debi(methodLogPrefix, "maps cleared");
            removeAllUserData(chatId, getUserTrip(chatId));
            debi(methodLogPrefix, "redis cleared");
            if (vTrips.length == 0) {
                sendMsgNoKeyboard(
                        String.valueOf(chatId),
                        "Привет!\nСкоро тут можно будет записаться в поездку рута, а пока новых поездок нет." +
                                " Следи за обновлениеями в группе на фейсбуке и возвращайся ко мне, как что-то увидишь там!\nИли просто пиши мне, я такой, я бот, выслушаю с удовольствием!️");
            } else {
                sendMsg(
                    String.valueOf(chatId),
                    "Привет! Тут можно записаться в поездку рута ⚡️ \nСейчас есть вариант: \n - Красная Поляна с 6 по 13 марта\n" ,
                    getInlineKeyBoardWithTrips());
            }
            debi(methodLogPrefix, "msg send");
        } else if (cmd == Command.SEND_RESPONSES) {

            handleSendResponses(fullMsg);
        } else if (cmd == Command.HELP) {

        } else if (cmd == Command.GET_USERS) {
            if (!hsAdminChatId.contains(chatId)) {

                return;
            }
            handleGetUsers(fullMsg);
        } else if (cmd == Command.MAILING) {
            if (!hsAdminChatId.contains(chatId)) {

                return;
            }
            handleMailing(chatId, fullMsg, null);
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

    private void handleGetUsers(String fullCmdString) {
        String methodLogPrefix = "handleSendResponses: ";
        debi(methodLogPrefix, "starts");
        Jedis jedis = Helper.getConnection();
        Set<String> hsUsers =  jedis.keys("n*");
        String users = hsUsers.stream().reduce((s, s2) -> s + "\n" + s2).orElse("");

        sendMsgNoMarkDown(adminChatId, users);
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
        SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());

        sb.append("Ответ от:  ")
                .append(Helper.escapeChars(userName))
                .append(". Время: ").append(formatter.format(date))
                .append("\n Поездка: ").append(vTrips[trip]).append("\n\n");
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
        sendMsgNoMarkDown(debugChatId, userName + "\n" + msgText);
    }

    private String getUserName(Long chatId) {
        String userName = hmChat2UserInfo.get(chatId);
        try {
            if (userName == null) {
                Jedis jedis = Helper.getConnection();
                if (jedis != null) {
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


    private void handleMailing(Long chatId, String msg, String photoId) {
        MailinigState state = hsChatId2MailingState.get(chatId);
        if (state == null || state == MailinigState.END &&
            msg.equals(Command.MAILING.name)) {

            hsChatId2MailingState.put(chatId, MailinigState.BEGAN);
            sendMsg(chatId, "Вы собиретесь запустить рассылку по всем пользователям бота. Ввести сообщение для рассылки в ответ на это сообщение.");
        } else if (state == MailinigState.BEGAN) {
            if (msg == null || msg.isEmpty()) {
                sendMsg(chatId, "Все таки нужно ввести сообщение. ");

                return;
            }
            if (photoId != null) {
                hsChatId2MailingFile.put(chatId, photoId);
            }
            hsChatId2MailingMsg.put(chatId, msg);
            hsChatId2MailingState.put(chatId, MailinigState.MSG_RECIEVED);
            String msgText = "Чтобы отправить сообщение: \n\"" + msg + "\"\n введите ДА, чтобы отменить, введите любуе другое слово";
            try {
                if (photoId == null) {
                    sendMsg(chatId, msgText);
                } else {
                    sendPhoto(chatId, msgText, photoId);
                }
            }catch (Exception ex) {
                ex.printStackTrace();
            }

        } else if(state == MailinigState.MSG_RECIEVED) {
            if (msg.equalsIgnoreCase("ДА")) {
                String msgText = hsChatId2MailingMsg.get(chatId);
                String fileId = hsChatId2MailingFile.get(chatId);
                if (msgText != null && !msg.isEmpty()) {
                    sendCustomMsgToAll(msgText, fileId);
                    hsChatId2MailingState.put(chatId, MailinigState.END);

                } else {
                    sendMsg(chatId, "Не могу найти ваше сообщение, попробуйте заново по команде " + Command.MAILING.name);
                    hsChatId2MailingState.put(chatId, null);
                }
            } else {
                sendMsg(chatId, "Рассылка отменена. Чтобы начать сначала нажмите " + Command.MAILING.name);
                hsChatId2MailingState.put(chatId, null);
            }
        }
    }

    private void addChatToDB(Long chatId) {
        Jedis jedis = Helper.getConnection();
        if (jedis == null) {

            return;
        }

        jedis.sadd("chats_on", String.valueOf(chatId));
        //чёdebi("Chats on = " + jedis.smembers("chats_on"));
    }

    private void sendCustomMsgToAll(String msgText, String fileId) {
        String methodLogPrefix = "sendCustomMsgToAll: ";
        Jedis jedis = Helper.getConnection();
        Set<String> hsUsers = jedis.keys("n*");
           debi(methodLogPrefix, "Chats = " + hsUsers);

        int i = 0;
        for (String chatId : hsUsers) {
            Long chat = Long.parseLong(chatId.substring(1));
            try {
                if (fileId == null) {
                    sendMsg(chat, msgText);
                } else {
                    sendPhoto(chat, msgText, fileId);
                }
                i++;
                //Thread.sleep(1000);
            } catch (Exception ex) {
                debe(methodLogPrefix, ex.getMessage());
            }
        }

        int qty = i;
        hsAdminChatId.
            forEach(adminChatId -> sendMsg(
                adminChatId, "Всего " + hsUsers.size() +
                    " чатов.\nCобщение из рассылки отправлено в " + qty + " чатов."));

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

