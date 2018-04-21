package com.github.ganquan.tiny.retrofit.annotate;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 替换url中指定参数
 * "users/{username}"
 *
 * @author GanQuan
 * @since 2018/3/26.
 */

@Documented
@Target(ElementType.PARAMETER)
@Retention(RUNTIME)
public @interface Path {
    String value();

}
