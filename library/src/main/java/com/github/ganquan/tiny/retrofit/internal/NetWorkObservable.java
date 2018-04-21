package com.github.ganquan.tiny.retrofit.internal;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import com.github.ganquan.tiny.retrofit.HttpClient;
import com.github.ganquan.tiny.retrofit.JtApiException;
import com.google.gson.Gson;
import com.google.gson.internal.$Gson$Types;
import com.google.gson.reflect.TypeToken;

import android.text.TextUtils;
import android.util.Log;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import okhttp3.Call;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * @author GanQuan
 * @since 2018/2/7.
 */

public class NetWorkObservable<T> extends Observable<T> {
    private static Gson gson = new Gson();
    private Request jtRequest;
    private Type mType;

    private NetWorkObservable(TypeToken<T> typeToken, Request request) {
        mType = typeToken.getType();
        this.jtRequest = request;
    }

    private NetWorkObservable(Class<?> typeToken, Request request, boolean array) {
        if (array) {
            mType = $Gson$Types.newParameterizedTypeWithOwner(null, List.class, typeToken);
        } else {
            mType = typeToken;
        }
        this.jtRequest = request;
    }

    private NetWorkObservable(Type typeToken, Request request) {
        mType = typeToken;
        this.jtRequest = request;
    }

    /**
     * 根据参数创建createWorkObservable
     *
     * @param typeToken
     * @param jtRequest
     * @param <T>
     *
     * @return
     */
    public static <T> NetWorkObservable<T> create(TypeToken<T> typeToken, Request jtRequest) {
        return new NetWorkObservable<>(typeToken, jtRequest);

    }

    public static <T> NetWorkObservable<T> create(Class<T> typeToken, Request jtRequest) {
        return new NetWorkObservable<>(typeToken, jtRequest, false);

    }

    public static NetWorkObservable<?> create(Type typeToken, Request jtRequest) {
        return new NetWorkObservable<>(typeToken, jtRequest);

    }

    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        final Call call = HttpClient.getHttpClient().newCall(jtRequest.buildRequest());
        observer.onSubscribe(new InnerDisposable(call));
        try {
            if (!call.isCanceled()) {
                handleResponse(call.execute(), observer, call);
            }
        } catch (IOException e) {
            observer.onError(e);
        } catch (Exception e) {
            observer.onError(e);
        }

    }

    /**
     * 用于处理取消的事件
     */
    private static class InnerDisposable implements Disposable {
        Call call;

        InnerDisposable(Call call) {
            this.call = call;
        }

        @Override
        public void dispose() {
            Log.d("NetWorkObservable", "InnerDisposable#dispose()");

            if (call != null) {//rxlife回调时清理
                call.cancel();
                call = null;
                Log.d("NetWorkObservable", "call#cancel()");
            }
        }

        @Override
        public boolean isDisposed() {
            return call == null || call.isCanceled();
        }
    }

    private void handleResponse(Response response, Observer<? super T> observer, Call call) {
        try {
            if (response == null) {
                observer.onError(new JtApiException(-1, "HTTP Response is null"));
                return;
            }
            if (call.isCanceled()) {
                observer.onError(new JtApiException(-1, "Request is canceled"));
                if (response.body() != null) {
                    response.close();
                }
                return;
            }

            if (!response.isSuccessful()) {
                observer.onError(new JtApiException(-1, response.message()));
                if (response.body() != null) {
                    response.close();
                }
                return;
            }

            ResponseBody responseBody = response.body();

            if (responseBody != null) {
                String data = responseBody.string();
                if (TextUtils.isEmpty(data)) {
                    observer.onError(new JtApiException(-1, response.message()));
                    if (response.body() != null) {
                        response.close();
                    }
                    return;
                }
                T result = gson.fromJson(data, mType);
                observer.onNext(result);

            }
        } catch (Exception e) {
            observer.onError(new JtApiException(-1, e.toString()));
        } finally {
            if (response != null) {
                response.close();
            }
        }

    }

}
