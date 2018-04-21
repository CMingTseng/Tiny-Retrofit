package com.github.ganquan.tiny.retrofit.internal;

import java.io.File;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import com.github.ganquan.tiny.retrofit.IHttpInterceptor;
import com.github.ganquan.tiny.retrofit.JtRetrofit;
import com.google.gson.reflect.TypeToken;

import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.SingleTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

/**
 * http request构造请求参数
 */
public class Request {

    /**
     * Request builder用于初始化请求对象的参数 eg. url, post params etc.
     */
    public static class Builder {

        private Map<String, String> formEncodeParams = new HashMap<>();
        private Map<String, File> formDataPartParams = new HashMap<>();

        private String url;

        /**
         * set url for Builder
         *
         * @param url
         *
         * @return
         */
        public Builder url(String url) {
            this.url = url;
            return this;
        }

        /**
         * get url for Builder
         *
         * @return
         */
        public String getUrl() {
            return this.url;
        }

        /**
         * add form-params for Builder
         *
         * @param key
         * @param value
         *
         * @return
         */
        public Builder addFormParams(String key, String value) {
            this.formEncodeParams.put(key, value);
            return this;
        }

        /**
         * add form-params for Builder
         *
         * @param map
         *
         * @return
         */
        public Builder addFormParams(Map<String, String> map) {
            if (map != null) {
                this.formEncodeParams.putAll(map);
            }
            return this;
        }

        /**
         * add form-data-part params for Builder
         *
         * @param fileName
         * @param file
         *
         * @return
         */
        public Builder addFormDataPart(String fileName, File file) {
            this.formDataPartParams.put(fileName, file);
            return this;
        }

        /**
         * 根据builder来构建本地Request
         *
         * @return 自定义Request
         */
        public Request build() {
            Preconditions.checkNotNull(url, "url is null");
            return new Request(this);

        }
    }

    private final MediaType MEDIA_PNG = MediaType.parse("image/png");
    private final MediaType MEDIA_JPEG = MediaType.parse("image/jpeg");
    private final MediaType MEDIA_OCTET = MediaType.parse("application/octet-stream");

    /**
     * 获取xml-url-encode表单数据
     *
     * @return
     */
    public Map<String, String> getFormEncodeParams() {
        return formEncodeParams;
    }

    /**
     * 用于提交xml-url-encode表单数据
     */
    private Map<String, String> formEncodeParams = new HashMap<>();
    /**
     * 用于提交multipart数据
     */
    private Map<String, File> formDataPartParams = new HashMap<>();

    /**
     * 获取url
     *
     * @return
     */
    public String getUrl() {
        return url;
    }

    private String url;

    private Request(Builder builder) {
        this.url = builder.url;
        this.formDataPartParams = builder.formDataPartParams;
        if (builder.formEncodeParams != null) {
            this.formEncodeParams.putAll(builder.formEncodeParams);
        }
        generateCommonParams();
    }

    /**
     * post 数据并指定线程
     *
     * @param typeToken
     * @param <T>
     *
     * @return
     */
    @Deprecated
    public <T> Single<T> post(TypeToken<T> typeToken) {
        return NetWorkObservable.create(typeToken, this)
                .singleOrError()
                .compose(new SingleTransformer<T, T>() {//线程转换
                    @Override
                    public SingleSource<T> apply(Single<T> upstream) {
                        return upstream
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread());
                    }
                });

    }

    /**
     * post 数据并指定线程
     *
     * @param clazz
     * @param <T>
     *
     * @return
     */

    public <T> Single<T> post(Class<T> clazz) {
        return NetWorkObservable.create(clazz, this)
                .singleOrError()
                .compose(new SingleTransformer<T, T>() {//线程转换
                    @Override
                    public SingleSource<T> apply(Single<T> upstream) {
                        return upstream
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread());
                    }
                });

    }

    /**
     * post 数据并指定线程
     *
     * @param type
     *
     * @return
     */

    public Single post(Type type) {
        return NetWorkObservable.create(type, this)
                .singleOrError()
                .compose(new SingleTransformer<Object, Object>() {//线程转换
                    @Override
                    public SingleSource<Object> apply(Single<Object> upstream) {
                        return upstream
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread());
                    }
                });

    }

    /**
     * 透参
     */
    private void generateCommonParams() {
        if (JtRetrofit.httpInterceptors != null && !JtRetrofit.httpInterceptors.isEmpty()) {
            for (IHttpInterceptor httpInterceptor : JtRetrofit.httpInterceptors) {
                httpInterceptor.onHandleDataParams(formEncodeParams);
            }
        }

    }

    /**
     * build okhttp request
     *
     * @return
     */
    okhttp3.Request buildRequest() {
        return new okhttp3.Request.Builder()
                .url(url)
                .post(generateRequestBody())
                .build();
    }

    private RequestBody generateRequestBody() {
        //以下参数不会参与签名
        if (formDataPartParams == null || formDataPartParams.size() <= 0) {
            FormBody.Builder textBuilder = new FormBody.Builder();
            return textBuilder.build();
        } else {
            MultipartBody.Builder multipartBuilder = new MultipartBody.Builder();
            multipartBuilder.setType(MultipartBody.FORM);
            for (String key : formDataPartParams.keySet()) {
                File file = formDataPartParams.get(key);
                MediaType type = getFileMediaType(file);
                RequestBody fileBody = RequestBody.create(type, file);
                multipartBuilder.addFormDataPart(key, file.getAbsolutePath(), fileBody);
            }
            return multipartBuilder.build();
        }
    }

    private MediaType getFileMediaType(File file) {
        String filename = file.getName();
        int index = filename.lastIndexOf(".");
        String suffix = null;
        if (index > 0) {
            suffix = filename.substring(index + 1);
        }
        if ("jpg".equalsIgnoreCase(suffix)) {
            return MEDIA_JPEG;
        } else if ("png".equalsIgnoreCase(suffix)) {
            return MEDIA_PNG;
        } else {
            return MEDIA_OCTET;
        }
    }

}


