import redis.clients.jedis.Jedis;

import java.net.URI;
import java.net.URISyntaxException;

public class RedisCli {
    public static Jedis getConnection()   {
        try {
            URI redisURI = new URI(System.getenv("REDIS_URL"));
            Jedis jedis = new Jedis(redisURI);
            return jedis;
        } catch (URISyntaxException ex) {
            ex.printStackTrace();
        }
        return null;
    }



}
