package com.github.ganquan.tiny.retrofit;

/**
 * 抛出异常时用来包裹exception
 *
 * @author GanQuan
 */
public class JtApiException extends Exception {

    public int code;

    public String msg;
    public String jsonError;

    public JtApiException(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public JtApiException(int code, String msg, String jsonError) {
        this.code = code;
        this.msg = msg;
        this.jsonError = jsonError;
    }

    @Override
    public String toString() {
        return "JTApiException{" +
                "code=" + code +
                ", msg='" + msg + '\'' +
                '}';
    }
}
