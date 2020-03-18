
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

public class RouteBot extends Bot {

    private static final String name = "RouteTestBot";
    private static final String token = "900084418:AAEfgGDNoCUstvOCWlw3cBTGrny80h0rSK0";
    RouteBot() {
        super(name, token);
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
