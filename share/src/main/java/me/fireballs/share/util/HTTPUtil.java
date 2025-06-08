package me.fireballs.share.util;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.stream.Stream;

public class HTTPUtil {
    private static final String SERVICE = "https://api.pastes.dev/post";
    private static final URL URL;

    static {
        try {
            URL = URI.create(SERVICE).toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static Stream<String> post(String body) throws IOException {
        HttpsURLConnection conn = (HttpsURLConnection) URL.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "text/plain");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");

        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);

        try (DataOutputStream dos = new DataOutputStream(conn.getOutputStream())) {
            dos.writeBytes(body);
        }

        return new BufferedReader(new InputStreamReader(conn.getInputStream())).lines();
    }
}
