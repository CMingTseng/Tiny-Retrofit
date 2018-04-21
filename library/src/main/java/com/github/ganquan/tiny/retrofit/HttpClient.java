package com.github.ganquan.tiny.retrofit;

import java.util.concurrent.TimeUnit;

import com.github.ganquan.tiny.retrofit.internal.Preconditions;
import com.github.ganquan.tiny.retrofit.internal.Request;

import okhttp3.OkHttpClient;

/**
 * 配置httpClient
 *
 * @author GanQuan
 * @since 2018/2/7.
 */
public class HttpClient {
    private static boolean isInit;
    private static OkHttpClient sDefaultClient;
    private static int connectTimeOut = 20;
    private static int readTimeOut = 20;
    private static int writeTimeOut = 20;

    /**
     * set the time out for http client
     *
     * @param connect
     * @param read
     * @param write
     */
    public static void setTimeOut(int connect, int read, int write) {
        connectTimeOut = connect;
        readTimeOut = read;
        writeTimeOut = write;

    }

    /**
     * create a JtRequest#Builder which contains common api params
     *
     * @return
     */
    public static Request.Builder create() {
        init();
        return new Request.Builder();
    }

    private static void init() {
        if (!isInit) {
            initOkHttpClient();
            isInit = true;
        }
    }

    private static void initOkHttpClient() {
        sDefaultClient =
                new OkHttpClient.Builder()
                        .connectTimeout(connectTimeOut, TimeUnit.SECONDS)
                        .readTimeout(readTimeOut, TimeUnit.SECONDS)
                        .writeTimeout(writeTimeOut, TimeUnit.SECONDS).build();

    }

    /**
     * 获取sDefaultClient
     *
     * @return
     */
    public static OkHttpClient getHttpClient() {
        return Preconditions.checkNotNull(sDefaultClient, "sDefaultClient init fails");
    }

}
