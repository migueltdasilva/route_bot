import org.apache.commons.io.FileUtils;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import redis.clients.jedis.Jedis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.zip.GZIPInputStream;

public class Helper {

    private static volatile Jedis jedis;

    public static Jedis getConnection()   {
        try {
            Jedis localJedis = jedis;
            if (localJedis == null) {
                synchronized (Jedis.class) {
                    localJedis = jedis;
                    if (localJedis == null) {
                        jedis = localJedis = getNewConn();
                    }
                }
            } else {
                try {
                    String str = jedis.ping("A");
                    if (str.equals("A")) {
                        return jedis;
                    } else {
                        jedis = localJedis = getNewConn();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    jedis = localJedis = getNewConn();
                }
            }

            return localJedis;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    private boolean testConn(Jedis jedis) {
        try {
            String str = jedis.ping("A");

            return str.equals("A");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return false;
    }

    private  static synchronized Jedis getNewConn() {
        String methodLogPrefix = "getNewConn: ";
        debi(methodLogPrefix, "starts");
        try {
            URI redisURI = new URI(System.getenv("REDIS_URL"));
            return new Jedis(redisURI);
        }  catch (URISyntaxException e) {
            e.printStackTrace();
        }
        debe(methodLogPrefix, "jedis is null");
        return null;
    }

    public static BufferedReader httpGet(
        String uri, String params, Map<String,String> headers) throws Exception {
        URLConnection urlConn;
        params = (params == null || params.isEmpty() ? "" : "&") + params;
        String str = uri + params;
        URL url = new URL(str);
        System.out.println(str);
        urlConn = url.openConnection();
        for(Map.Entry<String, String> kvHeader : headers.entrySet()) {
            urlConn.setRequestProperty(kvHeader.getKey(), kvHeader.getValue());
        }
        urlConn.setConnectTimeout(300000);
        urlConn.setReadTimeout(300000);

        BufferedReader in =
            new BufferedReader(new InputStreamReader(urlConn.getInputStream()));

        return in;
    }

    public static String sendReceive(String host, String params) throws Exception {
        String methodLogPrefix = "sendReceive:";
        StringBuilder sbAnswer = new StringBuilder();
        try {
            BufferedReader in = httpPost(
                host, params, new HashMap<String, String>());
            // Прочитаем ответ от сервера
            String line;
            while ((line = in.readLine()) != null) {
                sbAnswer.append(line);
                System.out.println(line);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            StringWriter we = new StringWriter();
            ex.printStackTrace(new PrintWriter(we));

            throw new TimeoutException(ex.getMessage());
        }


        return sbAnswer.toString();
    }

    public static BufferedReader httpPost(
        String uri, String params, Map<String,String> headers) throws Exception {

        return httpPost(uri, params, headers, 300000, false);
    }

    public static BufferedReader httpPost(
        String uri, String params, Map<String,String> headers,
        int timeout) throws Exception {

        return httpPost(uri, params, headers, timeout, false);
    }


    public static BufferedReader httpPost(
        String uri, String params, Map<String,String> headers,
        int timeout, boolean gzip) throws Exception {
        URLConnection urlConn;

        URL url = new URL(uri);
        HttpURLConnection hurlConn = (HttpURLConnection) url.openConnection();
        hurlConn.setRequestMethod("POST");
        hurlConn.setConnectTimeout(timeout);
        hurlConn.setReadTimeout(timeout);
        hurlConn.setDoOutput(true);
        hurlConn.setUseCaches(false);
        for (Map.Entry<String, String> kvHeader : headers.entrySet()) {
            hurlConn.setRequestProperty(kvHeader.getKey(), kvHeader.getValue());
        }
        OutputStream os = hurlConn.getOutputStream();
        BufferedWriter writer =
            new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        writer.write(params);
        writer.flush();
        writer.close();
        os.close();
        urlConn = hurlConn;

        BufferedReader in = null;
        InputStream is = null;
        if (hurlConn.getResponseCode() == 200 ||
            (hurlConn.getResponseCode() == 201)) {
            is = urlConn.getInputStream();
        } else {
            is = hurlConn.getErrorStream();
        }
        if (gzip) {
            is = new GZIPInputStream(hurlConn.getInputStream());
        }
        in = new BufferedReader(
            new InputStreamReader(is));

        return in;
    }


    public static InputFile getInputFileByUrl(String url) {
        try {
            java.io.File file = new java.io.File("video");

            FileUtils.copyURLToFile(new URL(url), file);
            InputFile inputFile = new InputFile();

            inputFile.setMedia(FileUtils.openInputStream(file), "video");
            //System.out.println(inputFile.getMediaName() + " " + inputFile.ge);
            return inputFile;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public static String escapeChars(String s) {
        if (s == null) {

            return "";
        }

        return s
            .replace("_", "\\_")
            .replace("*", "\\*")
            .replace("[", "\\[")
            .replace("]", "\\]")
            .replace("'", "\\'");
    }

    public static long s2l(String s) {
        try {
            return Long.parseLong(s);
        } catch (Exception var2) {
            return 0L;
        }
    }

    public static int s2i(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception var2) {
            return 0;
        }
    }

    private static void debe(String... strings) {
        StringBuilder sb = new StringBuilder();
        sb.append("RH: ERR: ");
        for (String string : strings) {
            sb.append(string);
        }
        System.out.println(sb.toString());
    }

    private static void debi(String... strings) {
        StringBuilder sb = new StringBuilder();
        sb.append("RH: ");
        for (String string : strings) {
            sb.append(string);
        }
        System.out.println(sb.toString());
    }


}
