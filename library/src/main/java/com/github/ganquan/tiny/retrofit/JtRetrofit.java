package com.github.ganquan.tiny.retrofit;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.github.ganquan.tiny.retrofit.annotate.Field;
import com.github.ganquan.tiny.retrofit.annotate.FieldMap;
import com.github.ganquan.tiny.retrofit.annotate.Path;
import com.github.ganquan.tiny.retrofit.annotate.Url;
import com.github.ganquan.tiny.retrofit.internal.Request;

import android.util.Log;
import io.reactivex.Single;

/**
 * a retrofit client for jt,which is a type-safe  and tiny HTTP client for java and Android
 * it offers two ways of request:
 * <p>
 * one, calling method of {@link JtRetrofit#create() }obtains create a JtRequest.Builder,then throw post send the http;
 * two,call {@link  JtRetrofit#create(Class)}obtains a api object(api-oj),then call methods of api-oj for sending http
 *
 * @author GanQuan
 * @since 2018/3/25.
 */
public class JtRetrofit {

    private final static Map<Method, MethodHandler> methodHandlerCache = new LinkedHashMap<>();
    private final static Map<Class, Object> apiServers = new LinkedHashMap<>();
    public static List<IHttpInterceptor> httpInterceptors;

    /**
     * create a JtRequest#Builder from JTHttpClient.create()
     *
     * @return
     */
    public static Request.Builder create() {
        return HttpClient.create();
    }

    /**
     * set http client time out
     *
     * @param c
     * @param r
     * @param w
     */
    public static void setTimeOut(int c, int r, int w) {
        HttpClient.setTimeOut(c, r, w);
    }

    /**
     * 增加网络拦截器，用来增加builder的通用参数
     *
     * @param httpInterceptor
     */
    public static void addHttpInterceptor(IHttpInterceptor httpInterceptor) {
        if (httpInterceptor == null) {
            return;
        }
        if (httpInterceptors == null) {
            httpInterceptors = new ArrayList<>();
        }

        httpInterceptors.add(httpInterceptor);
    }

    /**
     * 实例化api对象，通过动态代理实现，具体方法的注解请参考对应的注释
     *
     * @param service
     * @param <T>
     *
     * @return
     */
    public static <T> T create(final Class<T> service) {
        Object apiService = apiServers.get(service);
        if (apiService != null) {
            return (T) apiService;
        }

        apiService = Proxy.newProxyInstance(service.getClassLoader(),
                new Class<?>[] {service},
                new InvocationHandler() {

                    //这个invoke方法会在代理对象的方法中调用，第一个参数就是代理对象
                    //第二个参数是代理对象调用的方法
                    //第三个参数方法的参数
                    @Override
                    public Object invoke(Object proxy, Method method, Object... args)
                            throws Throwable {
                        validateServiceInterface(service);

                        // If the method is a method from Object then defer to normal invocation.
                        if (method.getDeclaringClass() == Object.class) {
                            return method.invoke(this, args);
                        }
                        //调用loadMethodHandler
                        return loadMethodHandler(method).invoke(args);
                    }
                });
        apiServers.put(service, apiService);
        return (T) apiService;

    }

    private static MethodHandler loadMethodHandler(Method method) {
        MethodHandler handler;
        synchronized(methodHandlerCache) {
            handler = methodHandlerCache.get(method);
            if (handler == null) {
                handler = MethodHandler.create(method);
                methodHandlerCache.put(method, handler);
            }
        }

        return handler;
    }

    private static <T> void validateServiceInterface(Class<T> service) {
        if (!service.isInterface()) {
            throw new IllegalArgumentException("API declarations must be interfaces.");
        }
        // Prevent API interfaces from extending other interfaces. This not only avoids a bug in
        // Android (http://b.android.com/58753) but it forces composition of API declarations which is
        // the recommended pattern.
        if (service.getInterfaces().length > 0) {
            throw new IllegalArgumentException("API interfaces must not extend other interfaces.");
        }
    }

    interface RequestAction {
        void perform(Request.Builder jtRequest, Object args);
    }

    static class FieldRequestAction implements RequestAction {
        String name;

        FieldRequestAction(String name) {
            this.name = name;
        }

        @Override
        public void perform(Request.Builder jtRequest, Object args) {
            jtRequest.addFormParams(name, (String) args);

        }
    }

    static class FieldMapRequestAction implements RequestAction {

        @Override
        public void perform(Request.Builder jtRequest, Object args) {
            jtRequest.addFormParams((Map<String, String>) args);
        }
    }

    static class PathRequestAction implements RequestAction {
        String name;

        public PathRequestAction(String name) {
            this.name = name;
        }

        @Override
        public void perform(Request.Builder jtRequest, Object args) {
            String url = jtRequest.getUrl();
            url = url.replace(String.format("{%s}", name), args.toString());
            jtRequest.url(url);
        }
    }

    private static class MethodHandler {
        private String mUrl;
        private int security;

        private List<RequestAction> requestActionList = new ArrayList<>();
        private Type mReturnType;

        Object invoke(Object... objects) {
            Request.Builder builder = HttpClient.create().url(mUrl);
            if (objects != null && objects.length > 0) {
                if (objects.length != requestActionList.size()) {
                    throw new NullPointerException("url: " + mUrl + "\nrequestActionList length is "
                            + "not equals objects length");
                }
                for (int i = 0; i < requestActionList.size(); i++) {
                    requestActionList.get(i).perform(builder, objects[i]);
                }
            }

            return builder.build().post(mReturnType);

        }

        MethodHandler(Method method) {
            Log.d("MethodHandler", "MethodHandler-method " + method.getName());
            Type returnType = method.getGenericReturnType();
            if (method.getReturnType() == Single.class) {
            } else {
                throw new IllegalArgumentException("you must return a type == Single.class");
            }
            if (returnType instanceof ParameterizedType) {
                Type[] actualTypeArguments = ((ParameterizedType) returnType)
                        .getActualTypeArguments();
                if (actualTypeArguments.length == 1) {
                    mReturnType = actualTypeArguments[0];
                } else {
                    throw new IllegalArgumentException();
                }
            }
            Annotation[] methodAnnotation = method.getAnnotations();
            for (Annotation annotation : methodAnnotation) {
                if (annotation instanceof Url) {
                    mUrl = ((Url) annotation).value();
                }
            }

            Annotation[][] parameterAnnotations = method.getParameterAnnotations();
            if (parameterAnnotations == null || parameterAnnotations.length == 0) {
                return;
            }
            for (Annotation[] annotations : parameterAnnotations) {
                if (annotations != null && annotations.length != 0) {
                    for (Annotation aMethodAnnotation : annotations) {
                        if (aMethodAnnotation instanceof Field) {
                            Field field = (Field) aMethodAnnotation;
                            requestActionList.add(new FieldRequestAction(field.value()));
                            break;

                        } else if (aMethodAnnotation instanceof FieldMap) {
                            requestActionList.add(new FieldMapRequestAction());
                        } else if (aMethodAnnotation instanceof Path) {
                            Path path = (Path) aMethodAnnotation;
                            requestActionList.add(new PathRequestAction(path.value()));
                        }
                    }
                }
            }
        }

        static MethodHandler create(Method method) {
            return new MethodHandler(method);

        }
    }
}
