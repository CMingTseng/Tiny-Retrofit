package com.github.ganquan.tiny.retrofit;

import java.util.Map;

/**
 * 公共参数相关的接口
 *
 * @author GanQuan
 * @since 2018/4/9.
 */

public interface IHttpInterceptor {
    /**
     * 获取api透传的参数
     *
     * @return
     */
    void onHandleDataParams(Map<String, String> apiParams);

}
