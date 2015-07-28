package org.apxeolog.streamchat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.http.Consts;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by APXEOLOG on 08/07/2015.
 */
public class Utils {
    public static <T> T fromJson(String _data, Class<T> _class) {
        return new Gson().fromJson(_data, _class);
    }

    public static String toJson(Object obj) {
        return new GsonBuilder().setPrettyPrinting().create().toJson(obj);
    }

    public static void writeToFile(String filename, String content) {
        try {
            File file = new File(filename);
            if (!file.exists()) {
                file.createNewFile();
            }
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(content);
            writer.close();
        } catch (Exception ex) {
            // TODO: Log
        }
    }

    public static String readFileContent(String filename) {
        try {
            return Files.readAllLines(new File(filename).toPath()).stream().collect(Collectors.joining("\n"));
        } catch (Exception ex) {
            return null;
        }
    }

    private static class DefaultTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}

        @Override
        public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }

    public static SSLConnectionSocketFactory sslConnectionSocketFactory;

    static {
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(new KeyManager[0], new TrustManager[]{new DefaultTrustManager()}, new SecureRandom());
            sslConnectionSocketFactory = new SSLConnectionSocketFactory(
                    context, new String[] { "TLSv1" }, null, SSLConnectionSocketFactory.getDefaultHostnameVerifier());
        } catch (Exception ex) {
            throw new RuntimeException("Cannot setup SSL Context");
        }
    }

    public static String post(String _url, HttpContext httpContext, Object... _data) {
        CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(sslConnectionSocketFactory).build();
        try {
            // Url
            HttpPost httpPost = new HttpPost(_url);

            // Parameters
            if (_data.length > 0) {
                List<NameValuePair> parameters = new ArrayList<NameValuePair>();
                for (int i = 0; i < _data.length; i += 2) {
                    parameters.add(new BasicNameValuePair(_data[i].toString(), _data[i + 1].toString()));
                }
                httpPost.setEntity(new UrlEncodedFormEntity(parameters, Consts.UTF_8));
            }

            CloseableHttpResponse response = httpContext != null ? httpClient.execute(httpPost, httpContext) : httpClient.execute(httpPost);
            return EntityUtils.toString(response.getEntity());
        } catch (Exception ex) {
            // Bad response, return empty string
            return "";
        } finally {
            try {
                httpClient.close();
            } catch (Exception ex) {
                // Ignore this one
            }
        }
    }

    public static String post(String _url, Object... _data) {
        return post(_url, null, _data);
    }

    public static String get(String _url, Object... _data) {
        CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(sslConnectionSocketFactory).build();
        try {
            // Parameters
            if (_data.length > 0) {
                StringBuilder stringBuilder = new StringBuilder(_url);
                stringBuilder.append('?');
                for (int i = 0; i < _data.length; i += 2) {
                    if (i > 0) stringBuilder.append('&');
                    stringBuilder.append(_data[i].toString());
                    stringBuilder.append('=');
                    stringBuilder.append(_data[i + 1].toString());
                }
                _url = stringBuilder.toString();
            }

            // Url
            HttpGet httpPost = new HttpGet(_url);
            CloseableHttpResponse response = httpClient.execute(httpPost);
            return EntityUtils.toString(response.getEntity());
        } catch (Exception ex) {
            // Bad response, return empty string
            return "";
        } finally {
            try {
                httpClient.close();
            } catch (Exception ex) {
                // Ignore this one
            }
        }
    }
}
