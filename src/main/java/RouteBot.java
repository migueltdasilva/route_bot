
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendAudio;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVoice;
import org.telegram.telegrambots.meta.api.objects.Audio;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.Voice;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.generics.BotSession;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import redis.clients.jedis.Jedis;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.System.out;

public class RouteBot extends Bot {

    private static final String name = "helloroute_bot";
    private static final String token = "1013761197:AAHv3uKJJzwiWMsQVxgnxUsqbpP5-PSrRy4";
    private static final String adminChatId = "3099992";
    private static final Set<Long> hsAdminChatId = new HashSet<>();
    private static final String debugChatId = "-487931131";
    private Map<Long, MailinigState> hsChatId2MailingState = new HashMap<>();
    private Map<Long, String> hsChatId2MailingMsg = new HashMap<>();
    private Map<Long, String> hsChatId2MailingFile = new HashMap<>();

    static {
        hsAdminChatId.add(3099992L);
        hsAdminChatId.add(96353936L);
        hsAdminChatId.add(489194L);
    }
    private static Map<Long, String> hmId2AdminName = new HashMap<>();
    static {
        hmId2AdminName.put(3099992L, "Никита");
        hmId2AdminName.put(96353936L, "Алина");
        hmId2AdminName.put(489194L, "Коля");
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
        ALL_CHATS("/all_chats", "Покажи все чаты"),
        SEND_MSG("/send_msg", "Покажи все чаты"),
        KAZAKHSTAN("/kazakhstan", "Покажи все чаты"),
        KAVKAZ("/kavkaz", "Покажи все чаты"),
        SAKHALIN("/sakhalin", "Покажи все чаты"),
        DAGESTAN("/dagestan", "Покажи все чаты"),
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


//    private static final String[] vTrips =
//            new String[]{"Хочу в Поляну Красную", "Хочу в Териберку", "Хочу в Байкал"};
    private static final Map<String, String> hmTrips = new LinkedHashMap<>();
    static {
//        hmTrips.put("polyana", "Хочу в Поляну Красную");
//        hmTrips.put("baikal", "Хочу в Байкал");
        //hmTrips.put("buryat", "Хочу в Бурятию");
        hmTrips.put("zakam", "Хочу в Закан");
        hmTrips.put("alt", "Хочу на Алтай");
        hmTrips.put("dag1", "Хочу в Дагестан");

        //hmTrips.put("kamchatka", "Хочу на Камчатку");
    }
    //private static final String[][] vQuestions = new String[hmTrips.size()][];
    private static final Map<String, String[]> hmQuestions = new HashMap<>();
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

        hmQuestions.put("buryat", new String[] {
            "Классно, что ты решил присоединиться к нашей поездке!\n" +
                "Чтобы оставить заявку – ответь, пожалуйста, на наши вопросы. " +
                "Тебе понадобится 5 свободных минут и не стесняться.",

            "Расскажи, пожалуйста, в двух словах, о себе: чем ты занимаешься," +
                " что ты любишь, почему хочешь поехать с нами?\n" +
                "(Обещаю, дальше вопросы будут попроще. Или нет. Но точно не надо быть серьезным)",

            "Расскажи про самое необычное место, в котором ты купался?",

            "Буддизм или гедонизм?",

            "Есть ли какие-то телесные практики, которые помогают тебе прийти в равновесие в сложные времена?",

            "Пришли, пожалуйста, ссылки на свои соц сети (например, фейсбук и инстаграм)?",

            "Я не смог получить твой ник в телеграмме, поэтому пришли," +
                " пожалуйста, свой телефон, чтобы мы точно могли с тобой связаться.",

            " Коронный вопрос нашего бота! Запиши аудиосообщение: как бурханят?"});

        hmQuestions.put("polyana", new String[] {
            "Классно, что ты решил присоединиться к нашей поездке!\n" +
                "Чтобы оставить заявку – ответь, пожалуйста, на наши вопросы. " +
                "Тебе понадобится 5 свободных минут и не стесняться.",

            "Расскажи, пожалуйста, в двух словах, о себе: чем ты занимаешься," +
                " что ты любишь, почему хочешь поехать с нами?\n" +
                "(Обещаю, дальше вопросы будут попроще. Или нет. Но точно не надо быть серьезным)",

            "Катаешься ли ты на лыжах или сноуборде? " +
                "цени свой уровень по шкале от 1 до 10, где 1 - забыл, какая нога ведующая, а 10 - уверенный рассекатель пухляка.",

            "Какая будет твоя роль в бобслейной команде?",

            "Какую вещь обязательно нужно взять с собой, чтобы создать уют в нашем коливинге? Воронка, шахматы, проектор и рецепт самодельных пельменей — уже выбрали!",

            "Пришли, пожалуйста, ссылки на свои соц сети (например, фейсбук и инстаграм)?",

            "Я не смог получить твой ник в телеграмме, поэтому пришли," +
                " пожалуйста, свой телефон, чтобы мы точно могли с тобой связаться.",

            "Коронный вопрос нашего бота — нужно записать аудиосообщение! Задача: каким звуком будет начинаться и заканчиваться день в нашем доме-коворкинге?"});

        hmQuestions.put("baikal", new String[] {
            "Классно, что ты решил присоединиться к нашей поездке!\n" +
                "Чтобы оставить заявку – ответь, пожалуйста, на наши вопросы. " +
                "Тебе понадобится 5 свободных минут и не стесняться.",

            "Расскажи, пожалуйста, в двух словах, о себе: чем ты занимаешься," +
                " что ты любишь, почему хочешь поехать с нами?\n" +
                "(Обещаю, дальше вопросы будут попроще. Или нет. Но точно не надо быть серьезным)",

            "Сколько раз ты уже заполнял (или начинал заполнять) этого бота?",

            "Какой трек включишь в буханке, несущейся по льду Байкала?",

            "Пришли, пожалуйста, ссылки на свои соц сети (например, фейсбук и инстаграм)",

            "Я не смог получить твой ник в телеграмме, поэтому пришли," +
                " пожалуйста, свой телефон, чтобы мы точно могли с тобой связаться.",

            "Коронный вопрос нашего бота — нужно записать аудиосообщение! Изобрази звук хивуса. Не гугли, если не знаешь, что это, пусть будет чистая фантазия (и ор)."
        });

        hmQuestions.put("kamchatka", new String[] {
            "Классно, что ты решил присоединиться к нашей поездке!\n" +
                "Чтобы оставить заявку – ответь, пожалуйста, на наши вопросы. " +
                "Тебе понадобится 5 свободных минут и не стесняться.",

            "Какое было самое далекое место от дома, где ты побывал?",

            "Если бы ты мог назвать вулкан на Камчатке, как бы он назывался?",

            "Заплывают в бар косатка, кит и нерпа, а бармен им говорит: _______________",

            "Расскажи, пожалуйста, в двух словах, о себе: чем ты занимаешься, что ты любишь, почему хочешь поехать с нами?",

            "Свободный день на Камчатке лучше всего потратить на серф или маунтинбайк или что еще?",

            "Какие даты для тебя были бы более предпочтительны: 21-29 июля или 29 июля - 6 августа?",

            "Пришли, пожалуйста, ссылки на свои соц сети (например, фейсбук и инстаграм)",

            "Я не смог получить твой ник в телеграмме, поэтому пришли," +
                " пожалуйста, свой телефон, чтобы мы точно могли с тобой связаться.",

            "Коронный вопрос нашего бота! Запиши аудиосообщение: каким звуком надо отпугивать медведя, чтобы он точно больше никогда не захотел прийти на эту вечеринку?"
        });

        hmQuestions.put("kash", new String[] {
            "Классно, что ты решил присоединиться к нашей поездке!\n" +
                "Чтобы оставить заявку – ответь, пожалуйста, на наши вопросы. " +
                "Тебе понадобится 5 свободных минут и не стесняться.",

            "Расскажи, пожалуйста, в двух словах, о себе: чем ты занимаешься, что ты любишь, почему хочешь поехать с нами?\n" +
                "(Обещаю, дальше вопросы будут попроще. Или нет. Но точно не надо быть серьезным)\n",

            "Расскажи, где ты сейчас, в России или уже в Турции? В какие даты планируешь присоедениться?",

            "Какое размещение? (двухместное / одноместное, раздельные / двухспальная кровать)",

            "Что ты бы хотел привнести в колливинг? Возможно устроить пробежку, " +
                "заваривать воронку, или рассказать про свой профессиональный или " +
                "личный опыт, привезти шахматы, мяч – что угодно!" +
                " Коливинг – это про людей, доверие и шеринг опыта.",

            "Пришли, пожалуйста, ссылки на свои соц сети (например, фейсбук и инстаграм)",

            "Я не смог получить твой ник в телеграмме, поэтому пришли," +
                " пожалуйста, свой телефон, чтобы мы точно могли с тобой связаться.",

            "Коронный вопрос нашего бота — нужно записать аудиосообщение! Задача: каким звуком будет начинаться и заканчиваться день в нашем доме-коворкинге?"
        });

        hmQuestions.put("krg", new String[] {
            "Классно, что ты решил присоединиться к нашей поездке!\n" +
                "Чтобы оставить заявку – ответь, пожалуйста, на наши вопросы. " +
                "Тебе понадобится 5 свободных минут и не стесняться.",

            "Расскажи, пожалуйста, в двух словах, о себе: чем ты занимаешься, что ты любишь, почему хочешь поехать с нами?\n" +
                "(Обещаю, дальше вопросы будут попроще. Или нет. Но точно не надо быть серьезным)\n",

            "Когда ты в последний раз ночевал(а) в палатке, и как к ним относишься?",

            "Есть ли у тебя опыт вождения джипа, хочешь быть водителем (водительницей) в поездке?",

            "Пришли, пожалуйста, ссылки на свои соц сети (например, фейсбук и инстаграм)",

            "Я не смог получить твой ник в телеграмме, поэтому пришли," +
                " пожалуйста, свой телефон, чтобы мы точно могли с тобой связаться.",

            "Вспомни свою встречу с самым пугающим животным, кто это? Изобрази его звуком в аудиосообщении, а мы попробуем угадать!"
        });

        hmQuestions.put("kaz1", new String[] {
            "Классно, что ты решил присоединиться к нашей поездке!\n" +
                "Чтобы оставить заявку – ответь, пожалуйста, на наши вопросы. " +
                "Тебе понадобится 5 свободных минут и не стесняться.",

            "Расскажи, пожалуйста, в двух словах, о себе: чем ты занимаешься, что ты любишь, почему хочешь поехать с нами?\n" +
                "(Обещаю, дальше вопросы будут попроще. Или нет. Но точно не надо быть серьезным)\n",

            "В поездке будет походный формат: мы ездим на джипах, но живем в палатках, " +
                "ужинаем в походной кухне, а чтобы умыться, надо попросить друга" +
                " подержать канистру с водой. Что классно добавить в такие условия, " +
                "чтобы жить сразу стало комфортнее?",

            "У нас уже несколько лет была идея сделать фестиваль в степях Казахстана," +
                " когда участники привносят свой вклад в общий опыт." +
                " Если референсом будет Burning Man — про что мог бы быть твой кэмп?",

            "Пришли, пожалуйста, ссылки на свои соц сети (например, фейсбук и инстаграм)",

            "Я не смог получить твой ник в телеграмме, поэтому пришли," +
                " пожалуйста, свой телефон, чтобы мы точно могли с тобой связаться.",

            "Коронный вопрос нашего бота — аудио. Мы будем находиться на дне древнего океана," +
                " который был здесь миллионы лет назад. Какие звуки были тогда – от океана, земли, животных, людей? " +
                "Запиши короткое аудиосообщение с одним из этих звуков, или любым другим, что рисует твое воображение!"
        });
        hmQuestions.put("kav", new String[] {
            "Классно, что ты решил присоединиться к нашей поездке!\n" +
                "Чтобы оставить заявку – ответь, пожалуйста, на наши вопросы. " +
                "Тебе понадобится 5 свободных минут и не стесняться.",

            "Расскажи, пожалуйста, в двух словах, о себе: чем ты занимаешься, что ты любишь, почему хочешь поехать с нами?\n" +
                "(Обещаю, дальше вопросы будут попроще. Или нет. Но точно не надо быть серьезным)\n",

            "Когда в последний раз ты ночевал(а) в палатке и как вообще к ним относишься?",

            "Есть ли что-то, ради чего ты готов(а) рано проснуться? Расскажи про последний такой случай.",

            "Пришли, пожалуйста, ссылки на свои соц сети (например, фейсбук и инстаграм)",

            "Я не смог получить твой ник в телеграмме, поэтому пришли," +
                " пожалуйста, свой телефон, чтобы мы точно могли с тобой связаться.",

            "Давай начнем разговор! Пожалуйста, запиши аудиосообщение про то, " +
                "собираешься ли ты полетать с нами на парапланах и как ты себе" +
                " это представляешь? А можешь записать, как будто ты уже на параплане?"
        });

        hmQuestions.put("sah", new String[] {
            "Классно, что ты решил присоединиться к нашей поездке!\n" +
                "Чтобы оставить заявку – ответь, пожалуйста, на наши вопросы. " +
                "Тебе понадобится 5 свободных минут и не стесняться.",

            "Расскажи, пожалуйста, в двух словах, о себе: чем ты занимаешься, что ты любишь, почему хочешь поехать с нами?\n" +
                "(Обещаю, дальше вопросы будут попроще. Или нет. Но точно не надо быть серьезным)\n",

            "Серфинг в России — это счастье или страдание?",

            "Каким было твое самое запоминающееся путешествие по России? К бабушке в деревню тоже считается!",

            "Оказавшись перед Тихим океаном, что ты у него спросишь?",

            "Есть три южнокурильских острова: Итуруп, Шикотан и Кунашир. Если бы был четвертый, как бы он назывался?",

            "Пришли, пожалуйста, ссылки на свои соц сети (например, фейсбук и инстаграм)",

            "Я не смог получить твой ник в телеграмме, поэтому пришли," +
                " пожалуйста, свой телефон, чтобы мы точно могли с тобой связаться.",

            "Коронный вопрос нашего бота — запиши аудиосообщение: " +
                "каким звуком ты будешь отпугивать медведя, " +
                "чтобы он больше никогда не захотел к тебе подойти?"
        });

        hmQuestions.put("dag1", new String[] {
            "Классно, что ты решил присоединиться к нашей поездке!\n" +
                "Чтобы оставить заявку – ответь, пожалуйста, на наши вопросы. " +
                "Тебе понадобится 5 свободных минут и не стесняться.",

            "Расскажи, пожалуйста, в двух словах, о себе: чем ты занимаешься, что ты любишь, почему хочешь поехать с нами?\n" +
                "(Обещаю, дальше вопросы будут попроще. Или нет. Но точно не надо быть серьезным)\n",

            "Есть ли у тебя опыт гребли (любой — каяк, байдарка, сап, лодка у бабушки на даче)?",

            "Когда ты последний раз ночевал в палатке и как к ним вообще относишься?",

            "Каким одним вопросом можно выбрать себе компаньона в каяк?",

            "Пришли, пожалуйста, ссылки на свои соц сети (например, фейсбук и инстаграм)",

            "Я не смог получить твой ник в телеграмме, поэтому пришли," +
                " пожалуйста, свой телефон, чтобы мы точно могли с тобой связаться.",

            "Коронный вопрос нашего бота — запиши аудиосообщение: " +
                "что будешь напевать себе под нос, пока гребешь?"
        });

        hmQuestions.put("alt", new String[] {
            "Классно, что ты решил присоединиться к нашей поездке!\n" +
                "Чтобы оставить заявку – ответь, пожалуйста, на наши вопросы. " +
                "Тебе понадобится 5 свободных минут и не стесняться.",

            "Ходил ли ты когда-нибудь в походы? Если да, то как это было, а если нет — то почему решил попробовать?",

            "Оказавшись глубоко в диких лесах Алтая, чтобы ты хотел съесть? Банку сгущёнки?",

            "Есть ли у тебя особенный навык, который может пригодиться в походе?",

            "После похода очень хочется глотка цивилизации — например, сделать маску для лица или выпить апероль из стеклянного бокала. А что еще будет супер приятно сделать после похода?",

            "Расскажи, пожалуйста, в двух словах, о себе: чем ты занимаешься, что ты любишь, почему хочешь поехать с нами?\n" +
                "(Обещаю, дальше вопросы будут попроще. Или нет. Но точно не надо быть серьезным)",

            "Пришли, пожалуйста, ссылки на свои соц сети (например, фейсбук и инстаграм)",

            "Я не смог получить твой ник в телеграмме, поэтому пришли," +
                " пожалуйста, свой телефон, чтобы мы точно могли с тобой связаться.",

            "Коронный вопрос нашего бота — записать аудиосообщение: " +
                "с каким звуком ты будешь заходить в холодную горную реку Кучерла?"
        });

        hmQuestions.put("zakam", new String[] {
            "Классно, что ты решил присоединиться к к путешествию!\n" +
                "Чтобы оставить заявку – ответь, пожалуйста, на наши вопросы. " +
                "Тебе понадобится 5 свободных минут и не стесняться.",

            "Ходил ли ты когда-нибудь в походы или однодневные треккинги в горы? Что было самое красивое, что встретилось по пути?",

            "4 дня без связи — это радость или боль?",

            "Что легко приготовить на ужин на 20 человек? Вообще у нас будет повар, но должен же быть и план Б\uD83D\uDE43",

            "Расскажи, пожалуйста, в двух словах, о себе: чем ты занимаешься, что ты любишь, почему хочешь поехать с нами?\n" +
                "(Обещаю, дальше вопросы будут попроще. Или нет. Но точно не надо быть серьезным)",

            "Пришли, пожалуйста, ссылки на свои соц сети (например, фейсбук и инстаграм)",

            "Я не смог получить твой ник в телеграмме, поэтому пришли," +
                " пожалуйста, свой телефон, чтобы мы точно могли с тобой связаться.",

            "Коронный вопрос нашего бота — записать аудиосообщение: какой звук раздается из домика на дереве?"
        });
    }

    private Map<Long, List<String>> hmChat2Answers;
    private Map<Long, String> hmChat2UserInfo;
    private Map<Long, String> hmChat2Trip;


    public synchronized ReplyKeyboardMarkup getTripButtons() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(true);

        List<KeyboardRow> alKeyboardRows = new ArrayList<>();
        for (String trip : hmTrips.keySet()) {
            KeyboardRow keyboardFirstRow = new KeyboardRow();
            keyboardFirstRow.add(new KeyboardButton(trip));
            alKeyboardRows.add(keyboardFirstRow);
        }
        replyKeyboardMarkup.setKeyboard(alKeyboardRows);

        return replyKeyboardMarkup;
    }

    public InlineKeyboardMarkup getInlineKeyBoardWithTrips() {
        if (hmTrips.size() == 0) {

            return null;
        }

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();

        //for (int i = 0; i < vTrips.length; i ++) {
        for (Map.Entry<String, String> kv : hmTrips.entrySet())   {
            InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
            inlineKeyboardButton.setText(kv.getValue());
            inlineKeyboardButton.setCallbackData("t_" + kv.getKey());
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


    public synchronized void sendMsgNoMarkDown(
            String chatId, String s) {
        debi("sendMsg: ",chatId +" = " + s);
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(false);
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(s);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            //e.printStackTrace();
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
        sendMessage.setText(Helper.escapeChars(s));
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
        String chatId, String s) throws Exception {
        debi("sendMsg: ",chatId +" = " + s);
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setText(s);
        ReplyKeyboardRemove replyKeyboardRemove = new ReplyKeyboardRemove(true);
        sendMessage.setReplyMarkup(replyKeyboardRemove);
        execute(sendMessage);
    }

    public synchronized void sendMsgNoKeyboard(
            String chatId, String s) {
        debi("sendMsg: ",chatId +"   = " + s);
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        ReplyKeyboardRemove replyKeyboardRemove = new ReplyKeyboardRemove();
        replyKeyboardRemove.setRemoveKeyboard(true);
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
        voice.setChatId(String.valueOf(chatId));
        voice.setVoice(new InputFile(fileId));
        try {
            execute(voice);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            //Log.log(Level, "Exception: ", e.toString());
        }
    }

    public synchronized void sendAudio(Long chatId, String fileId) {
        SendAudio audio = new SendAudio();
        audio.setChatId(String.valueOf(chatId));
        audio.setAudio(new InputFile(fileId));
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
        msg.setChatId(String.valueOf(chatId));
        msg.setPhoto(new InputFile(fileId));
        msg.setCaption(caption);
        execute(msg);
    }

    public synchronized void sendPhotos(
        Long chatId, List<String> alFileIds){
        SendMediaGroup msg = new SendMediaGroup();
        msg.setChatId(String.valueOf(chatId));
        List<InputMedia> inputMediaList =
            alFileIds.stream().map(InputMediaPhoto::new).collect(Collectors.toList());
        msg.setMedias(inputMediaList);
        try {
            execute(msg);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            //Log.log(Level, "Exception: ", e.toString());
        }
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
        String tripId = message.substring(2);
        sendMsgNoMarkDown(debugChatId, userName + "\n" + hmTrips.get(tripId));

        if (!hmTrips.containsKey(tripId)) {
            sendMsgNoKeyboard(
                String.valueOf(chatId),
                "Ой. Похоже, что эта поездка уже не доступна, чтобы начать заново нажми /start.");

            return;
        }

        hmChat2Trip.put(chatId, tripId);
        jedis.set("t" + chatId, tripId);
        hmChat2UserInfo.put(chatId, userName);
        jedis.set("n" + chatId, userName);

        String msgText = hmQuestions.get(tripId)[0];
        sendMsg(chatId, msgText, getGoAndCancelButton());
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
        if (debugChatId.equals(String.valueOf(chatId))) {

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

        String tripId = getUserTrip(chatId);
        debi(methodLogPrefix, "trip = " + tripId);
        if (updMsg.hasText() &&
                updMsg.getText().equalsIgnoreCase("отмена")) {
            hmChat2Answers.put(chatId, new ArrayList<>());
            hmChat2UserInfo.put(chatId, null);
            hmChat2Trip.put(chatId, null);
            removeAllUserData(chatId, tripId);

            sendMsgNoKeyboard(
                    chatId.toString(),
                    "Все забыл️. Чтобы начать заново нажми /start");

            return;
        }

        List<String> alAns = getUserAnswers(chatId, tripId);
        if (alAns == null) {
            alAns = new ArrayList<>();
        }
        if (alAns.size() == hmQuestions.get(tripId).length - 1) {

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

        String tripId = getUserTrip(chatId);
        List<String> alAns = getUserAnswers(chatId, tripId);

        out.println("LOG: onUpdateReceived: alAns = [" + alAns.stream().reduce("", (s, s2) -> s + ";" + s2) + "]");
        out.println("LOG: onUpdateReceived: has user = [" + hmChat2UserInfo.get(chatId) +  "|" + getUserName(chatId) + "]");

        String msgText = "";
        String chooseOpt = "0";

        Jedis jedis = Helper.getConnection();
        String userInfo = getUserName(chatId);
        if (alAns.size() == 0 &&
                userInfo == null && hmTrips.values().stream().noneMatch(message::equals)) {

            sendMsg(String.valueOf(chatId),
                    "Простите, я что-то не понял что это. А чего мне делать с этим." +
                            " А вы кто? Простите, я уже старый.", getTripButtons());

            return;
        } else if (alAns.size() == 0 && userInfo == null) {
            chooseOpt = "1";
            //TODO: Убрать,
//            tripIdx = getTripIdx(message);
//            hmChat2Trip.put(chatId, tripIdx);
//            jedis.set("t" + chatId, String.valueOf(tripIdx));
//            hmChat2UserInfo.put(chatId, userName);
//            jedis.set("n" + chatId, userName);
//
//            msgText = vQuestions[tripIdx][0];
//            sendMsg(chatId, msgText, getGoAndCancelButton());

            return;
        }  else {
            chooseOpt = "3";
            alAns.add(message);
            jedis.set("a" + chatId + "_" + alAns.size(), message);
            msgText = hmQuestions.get(tripId)[alAns.size()];
            if (msgText.startsWith("Я не смог получить твой ник") &&
                    usr.getUserName() != null) {
                alAns.add("");
                jedis.set("a" + chatId + "_" + alAns.size(), "");
                msgText = hmQuestions.get(tripId)[alAns.size()];
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
            if (hmTrips.size() == 0) {
                sendMsgNoKeyboard(
                        String.valueOf(chatId),
                        "Привет!\nСкоро тут можно будет записаться в поездку рута, а пока новых поездок нет." +
                                " Следи за обновлениеями в группе на фейсбуке и возвращайся ко мне, как что-то увидишь там!\nИли просто пиши мне, я такой, я бот, выслушаю с удовольствием!️");
            } else {
                sendMsg(
                    String.valueOf(chatId),
                    "Привет! Тут можно записаться в поездку рута ⚡️ \n" +
                        "Сейчас есть варианты:" +
                        "\n - Поход в Закан, 11-14 августа" +
                        "\n - Поход на Алтай 21-28 августа" +
                        "\n - Сплав в Дагестане 9-11 сентября /dagestan" ,
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
        } else if (cmd == Command.ALL_CHATS) {
            if (!hsAdminChatId.contains(chatId)) {

                return;
            }
            handleAllChats(chatId);
        } else if (cmd == Command.SEND_MSG) {
            if (!hsAdminChatId.contains(chatId)) {

                return;
            }

            sendMsgToChatHandle(chatId, msg.getText());
        } else if (cmd == Command.KAZAKHSTAN) {

            handleMore(chatId);
        } else if (cmd == Command.KAVKAZ) {

            handleMoreKavkaz(chatId);
        } else if (cmd == Command.SAKHALIN) {

            handleMoreSakhalin(chatId);
        } else if (cmd == Command.DAGESTAN) {

            handleMoreDagestan(chatId);
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
        msg.setPhoto(new InputFile(fileId));
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
        msg.setDocument(new InputFile(fileId));
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
        String tripId = getUserTrip(chatId);
        debi(methodLogPrefix, "userName: " + userName + " trip: " + tripId);
        if (userName == null || userName.isEmpty()) {
            debi(methodLogPrefix, "cannot find user");

            return;
        }
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());

        String[] curQuest = hmQuestions.get(tripId);

        sb.append("Ответ от:  ")
                .append(userName)
                .append(". Время: ").append(formatter.format(date))
                .append("\n Поездка: ").append(hmTrips.get(tripId)).append("\n\n");
        List<String> alAns = getUserAnswers(chatId, tripId);
        debi(methodLogPrefix, "ans: " + alAns);
        int numAns = alAns.size() == curQuest.length ? curQuest.length-1 : alAns.size();
        for (int i = 0; i < numAns; i++) {
            sb.append("Вопрос: ").append(curQuest[i]).append("\n")
                    .append("Ответ: ").append(alAns.get(i)).append("\n\n");
        }
        if (alAns.size() == curQuest.length) {
            sb.append("Вопрос: ").append(curQuest[curQuest.length - 1]).append("\n");
        }
        String responses = sb.toString();
        hsAdminChatId.
                forEach(adminChatId -> sendMsgNoMarkDown(String.valueOf(adminChatId), responses));

        if (alAns.size() == curQuest.length) {
            String fileId = alAns.get(curQuest.length - 1);
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

    private List<String> getUserAnswers(Long chatId, String tripId) {
        List<String> answers = hmChat2Answers.get(chatId);
        try {
            if (answers == null) {
                Jedis jedis = Helper.getConnection();
                if (jedis != null) {
                    answers = new ArrayList<>();
                    for (int i = 1; i<= hmQuestions.get(tripId).length; i++) {
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

    private String getUserTrip(Long chatId) {
        String trip = hmChat2Trip.get(chatId);
        try {
            if (trip == null) {
                Jedis jedis = Helper.getConnection();
                if (jedis != null) {
                    trip = jedis.get("t" + chatId);
                    hmChat2Trip.put(chatId, trip);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return trip;
    }

//    private int getTripIdx(String message) {
//        int idx = -1;
//        for (int i = 0; i<h.length; i++) {
//            if (vTrips[i].equals(message)) {
//                idx = i;
//            }
//        }
//
//        return idx;
//    }

    private void removeAllUserData(Long chatId, String tripId) {
        try {
            Jedis jedis = Helper.getConnection();
//            if (jedis.exists("n" + chatId)) {
//                jedis.del("n" + chatId);
//            }
            if (jedis.exists("t" + chatId)) {
                jedis.del("t" + chatId);
            }
            for (int i = 1; i <= hmQuestions.get(tripId).length; i++) {
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
            try {
                if (msg.equalsIgnoreCase("ДА")) {
                    String msgText = hsChatId2MailingMsg.get(chatId);
                    String fileId = hsChatId2MailingFile.get(chatId);
                    if (msgText != null && !msgText.isEmpty()) {
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
            } catch (Exception ex) {
                ex.printStackTrace();
                sendMsg(chatId, "Рассылка не получилась. Была какая-то ошибка. Оскорбите вашего разработчика. Чтобы начать сначала нажмите " + Command.MAILING.name);
                hsChatId2MailingState.put(chatId, null);
            }
        }
    }

    private void handleAllChats(Long chatId) {
        Jedis jedis = Helper.getConnection();
        if (jedis == null) {
            debe("handleAllChats:", "jedis is null");
            return;
        }

        Set<String> chats = jedis.smembers("chats_on");
        String sChats =  chats.stream().reduce((s, s2) -> s + "," + s2).orElse("");
        debi("chats: SIZE: [", chats.size() + "] \n ALL= " + sChats);
        sendMsg(chatId, "Всего чатов: " + chats.size());
    }

    private void sendMsgToChatHandle(Long admChatId, String msg) {
        String methodLogPrefix = "sendMsgToChatHandle: ";
        Jedis jedis = Helper.getConnection();
        if (jedis == null) {
            debe(methodLogPrefix, "jedis is null");
            return;
        }
        debi(methodLogPrefix, msg);
        msg = msg.substring(Command.SEND_MSG.name.length());
        debi(methodLogPrefix, msg);
        String[] msgParts = msg.split("#");
        long chatId = Helper.s2l(msgParts[0].trim());
        debi(methodLogPrefix, "" + chatId);
        if (chatId == 0 || msgParts.length < 2) {
            sendMsg(admChatId, "Не получилось отправить сообщение. Нормально делай.");

            return;
        }
        sendMsgNoMarkDown(debugChatId, "Отправляю пользователю: " + chatId +
            ", Текст [ " + msgParts[1] + "]" +
            " Отвественный: " +
            hmId2AdminName.getOrDefault(admChatId, String.valueOf(admChatId)));

        sendMsgNoKeyboard(String.valueOf(chatId), msgParts[1]);
    }

    private void handleMore(Long chatId) {
        String methodLogPrefix = "handleMore: ";
        List<String> photots = new ArrayList<>();
        photots.add("AgACAgIAAxkBAAKP3WJsI1hFTJZAgBrBVexQGJaq3GrEAAIKujEb-zdQSxoVWm8XNBS9AQADAgADbQADJAQ");
        photots.add("AgACAgIAAxkBAAKP22JsIxccr-FBcDdL6tTQcLafVUn2AAIYujEb-zdQS_TWVJF6GSRwAQADAgADeAADJAQ");
        photots.add("AgACAgIAAxkBAAKP_mJsJjD1wQI5Lp8HU8iN2x5p1sC3AAILujEb-zdQS0VOu9vrhSbJAQADAgADbQADJAQ");
        photots.add("AgACAgIAAxkBAAKP_2JsJjlPRmRqvz94Il678sBAkF2zAAIMujEb-zdQS6zUZpBZWuZTAQADAgADeAADJAQ");
        photots.add("AgACAgIAAxkBAAKQAAFibCZgVjbkq4f1jRJDYxf5Xyu4RwACDboxG_s3UEvjbvc9BarmFwEAAwIAA3kAAyQE");
        photots.add("AgACAgIAAxkBAAKQAWJsJnrcgV50zvbBkDyVlQoP4CGcAAIWujEb-zdQS6Lge5r1O2bcAQADAgADeQADJAQ");

        String text = "Западный Казахстан — это когда едешь 4 дня по бесконечному белому, и каждый день находишь в этой пустоте чудо света: гигантскую скалу из ниоткуда, поле разбросанных каменных шаров размером с человека, соляное озеро с идеальным отражением неба, облаков, тебя. Все это в полной автономии, воды в пустыне нет, перепады температур есть, так что волей не волей становишься кочующим и немного диким человеком — комфорт не нужен на фоне этой природы, только вперед, к новым чудесам. \n" +
            "\n" +
            "А еще мы устроим свой фестиваль, привезем звук, соберем творческих и музыкальных друзей, и предложить всем посозидать в формате кемпов. Покричать про важное, и почувствовать себя песчинкой на дне древнего океана, где жизнь измеряется миллионами лет.\n" +
            " \n" +
            "Даты: 12-17 июня, \n" +
            "Перелеты Аэрофлотом до Актау, с прилетом 12го и вылетом вечером 17го. Идея: остаться и провести выходные на берегу Каспийского моря. \n" +
            "\n" +
            "Бюджет: 59000₽, входят 5 дней в пустыне на джипах с гидами, вся экипировка и еда, напитки, одна ночь в городе.  \n" +
            "\n" +
            "Запись через бот. Просто жми /start.";
        sendPhotos(chatId, photots);
        sendMsgNoKeyboard(String.valueOf(chatId), text);

    }

    private void handleMoreKavkaz(Long chatId) {
        String methodLogPrefix = "handleMore: ";
        List<String> photots = new ArrayList<>();
        photots.add("AgACAgIAAxkBAAKjm2KQyBIdwOmHtQvehIZyFQXui_XMAAJbvDEbToGISNrf6W-dZAAB5wEAAwIAA3kAAyQE");
        photots.add("AgACAgIAAxkBAAKjl2KQx9eWlK7Rhs2-S63jBNdE7KgoAAJVvDEbToGISJ4hWEQZcOf8AQADAgADeQADJAQ");
        photots.add("AgACAgIAAxkBAAKjmWKQx_Zsnxa7EFvKnXYRblQb_K_VAAJXvDEbToGISB4PShyckOPzAQADAgADeQADJAQ");
        photots.add("AgACAgIAAxkBAAKjnWKQyDMCJc--nNBvZvGb6IyT4LFbAAJcvDEbToGISLWa05Q7bP9FAQADAgADeQADJAQ");
        photots.add("AgACAgIAAxkBAAKjn2KQyEjhdcrrGCVKAAHV4onH4Vvo4gACXbwxG06BiEhOHIKuF4KCtAEAAwIAA3kAAyQE");
        photots.add("AgACAgIAAxkBAAKjo2KQyHn7UbRHciaac4lGLfJ8wYAEAAJgvDEbToGISIj3p6n0a6zXAQADAgADeQADJAQ");
        photots.add("AgACAgIAAxkBAAKjoWKQyFuw_UY9TSbcT1bjNQk4MDksAAJfvDEbToGISMCTt1e_VBgDAQADAgADeQADJAQ");

        String text = "11-13 ИЮНЯ — КАВКАЗ — ПАРАПЛАНЫ — ДЖИПЫ — ПАЛАТКИ — ВОДОПАД — ХЫЧИН \n" +
            "\n" +
            "Традиционный и любимый маршрут на выходные по Кабардино-Балкарии. \n" +
            "\n" +
            "Два идеальных дня среди большой природы Кавказа, которую целиком не понять, но можно почувствовать – умчать в горы на фотогеничных джипах, парить над огромным ущельем, засыпать в палатке под шум реки, ощущать брызги от водопада, много бегать по лугам, собирая цветы и улыбки. \n" +
            "\n" +
            "Шалость работает, 5 раз проверяли.\n" +
            "\n" +
            "Бюджет 23500 с отелем, джипами, экипом, едой на маршруте, но без парпаланов.\n" +
            "\n" +
            "куратор @Slavaext \n" +
            "запись тут, жми  /start\n" +
            "план в фейсбуке \uD83E\uDE82";
        sendPhotos(chatId, photots);
        sendMsgNoKeyboard(String.valueOf(chatId), text);
    }

    private void handleMoreSakhalin(Long chatId) {
        String methodLogPrefix = "handleMore: ";
        List<String> photots = new ArrayList<>();
        photots.add("AgACAgIAAxkBAAKpFGKX1E4eY8Rg0cDZ_xl1CtLYG-0wAAKhvzEb7J7ASDyRMoGIzQY9AQADAgADeQADJAQ");
        photots.add("AgACAgIAAxkBAAKpFWKX1MEcV_pWj9YSd4Pg08m2b77QAALJvzEb7J7ASOOXRKz_R6B3AQADAgADeQADJAQ");
        photots.add("AgACAgIAAxkBAAKpFmKX1N8umd_zecFHGmRihGFFcfdCAALKvzEb7J7ASELuFrwFCCUMAQADAgADeQADJAQ");
        photots.add("AgACAgIAAxkBAAKpF2KX1PcJmxgBEfQ9zNZe8GbX5CcyAALLvzEb7J7ASIC4cTc7kY8CAQADAgADeQADJAQ");
        photots.add("AgACAgIAAxkBAAKpGGKX1Q8jimTqp7HpY7tvnLKXW3u8AALMvzEb7J7ASB2QyeBJxfSFAQADAgADeQADJAQ");
        photots.add("AgACAgIAAxkBAAKpGWKX1SBo2nhAfPQSdkXX9ERgqPNMAALOvzEb7J7ASI56qUBGrMjOAQADAgADeQADJAQ");
        photots.add("AgACAgIAAxkBAAKpGmKX1TK24j1G1LAySse-99t0Rm9UAALPvzEb7J7ASI87Y09zAiLLAQADAgADeAADJAQ");
        photots.add("AgACAgIAAxkBAAKpG2KX1Y74K9XxhaohRF_KQCWGqanoAALQvzEb7J7ASBi86d772kRdAQADAgADeQADJAQ");

        String text = "26 ИЮНЯ-6 ИЮЛЯ — САХАРИЛЫ — ТИХИЙ — ДЖИПЫ — СЕРФ — КРАБЫ \n" +
            "\n" +
            "\n" +
            "Возвращаемся на любимый остров в Тихом океане – Итуруп. Мы свозили туда уже сто человек и все сто влюбились – «мы ездим по острову и собираем мурашки». \n" +
            "\n" +
            "На этот раз программа на Итурупе стала длиннее: посмотрим все точки прошлогодных маршрутов во главе с Белыми скалами, устроим пикник на Янкито, а еще доберемся до новых мест – куда еще не ступала рут-нога. По примеру одной из групп мы добавили день на лодках, а еще попробуем устроить больше разговоров и историй от местных жителей.\n" +
            "\n" +
            "Кроме Итурупа мы еще посмотрим Сахалин – самый красивый мыс Птичий, серф и крабы в бухте Тихая, попробуем доплыть до маяка Анива, изучим, что нового появилось в Южно-Сахалинске за последний год. \n" +
            "\n" +
            "Всего 18 человек + @nemescu и @nikov\n" +
            "\n" +
            "Бюджет 109000 на 10 дней – более подробно про бюджет в комментарии. \n" +
            "\n" +
            "\uD83E\uDD80Запись в дальневосточный отпуск через /start";
        sendPhotos(chatId, photots);
        sendMsgNoKeyboard(String.valueOf(chatId), text);
    }

    private void handleMoreDagestan(Long chatId) {
        String methodLogPrefix = "handleMore: ";
        List<String> photots = new ArrayList<>();
        photots.add("AgACAgIAAxkBAAK262Kx0AOFdVn7XjKCyuXQpMdhUEm7AAL7vDEbKiyRSY9rJZ7fMCNmAQADAgADeQADKQQ");
        photots.add("AgACAgIAAxkBAAK27GKx0CCOD-8ve03DHBs-LhIq3CzBAAL9vDEbKiyRSRKMQgKeboiBAQADAgADeQADKQQ");
        photots.add("AgACAgIAAxkBAAK27WKx0DaKsewucRpESCQimk7F4cPPAAL_vDEbKiyRSUiImyuPMn6SAQADAgADeQADKQQ");
        photots.add("AgACAgIAAxkBAAK27mKx0EnDTjzj9mxHLHKbY0F2P9q0AAO9MRsqLJFJAmR39tBGb9IBAAMCAAN5AAMpBA");
        photots.add("AgACAgIAAxkBAAK272Kx0FeI3DwwuHsgeAvnw9P46RnwAAIBvTEbKiyRSVTi6uhAZjGaAQADAgADeQADKQQ");

        String text = "СЛАВОСПЛАВ – ДАГЕСТАН – СУЛАКСКИЙ КАНЬОН – 8-10 ИЮЛЯ\n" +
            "\n" +
            "Возвращаемся в гостеприимную республику с новым маршрутом — сплав по Сулакскому каньону. Вокруг высоченные скалы и бирюзовая вода, впереди 30 км гребли и закат с вином в палаточном лагере.\n" +
            "\n" +
            "\n" +
            "8/07, пт: прилет в Махачкалу. Вечером или в течение дня, чтобы погулять у моря или поработать из кафе. Вечером встретимся на ужин. \n" +
            "\n" +
            "9/07, сб: рано утром старт к Сулакскому каньону — самому глубокому в Европе!\n" +
            "\n" +
            "Сплавляться будем под присмотром уже знакомых инструкторов-гидов. Водный маршрут - это 30 км или 10 часов, разбитые на 2 дня. Будем не только активно грести, но созерцать величественную природу с воды, останавливаться на обеды и купания. К 19 часам приплывем к точке для ночлега, разобьем лагерь, раскупорим вино своими сильными руками и устроим красивый ужин. \n" +
            "\n" +
            "\n" +
            "10/07, вс: Сразу после завтрака – небольшой хайк недалеко от лагеря. Дальше лениво доплываем наш маршрут, пикнике и аэропорт. \n" +
            "\n" +
            "Бюджет 30 000 руб. Подробнее в комментарии.\n" +
            "Запись в сплав /start";
        sendPhotos(chatId, photots);
        sendMsgNoKeyboard(String.valueOf(chatId), text);
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
        if (jedis == null) {
            debe(methodLogPrefix, "Jedis is null");

            return;
        }
        Set<String> hsUsers = jedis.smembers("chats_on");
        debi(methodLogPrefix, "Chats SIZE = " + hsUsers.size());
        debi(methodLogPrefix, "Chats = " + hsUsers);

        sendMsgNoMarkDown(debugChatId, "НАЧАЛ РАССЫЛКУ");
        int i = 0;
        for (String chatId : hsUsers) {
            Long chat = Long.parseLong(chatId);
            try {
                if (fileId == null) {
                    sendMsgNotSafe(String.valueOf(chat), msgText);
                } else {
                    sendPhoto(chat, msgText, fileId);
                }
                i++;

                //Thread.sleep(1000);
            } catch (Exception ex) {
                debe(methodLogPrefix, ex.getMessage());
            }
        }
        sendMsgNoMarkDown(debugChatId, "ЗАКОНЧИЛ РАССЫЛКУ");
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



    public static void main(String[] args) throws TelegramApiException {

        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        try {
            Bot routeBot = new RouteBot();
            botsApi.registerBot(routeBot);
        } catch (TelegramApiException e) {

            e.printStackTrace();
        }
    }

}

