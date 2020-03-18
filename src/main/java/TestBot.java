import org.telegram.abilitybots.api.bot.AbilityBot;

public class TestBot extends AbilityBot {

    protected TestBot(String botToken, String botUsername) {
        super(botToken, botUsername);
    }

    @Override
    public int creatorId() {
        return 123123;
    }
}
