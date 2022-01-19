package com.jun;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * 具有该注解的类或对象为 immutable(线程安全，不可修改) 类型.
 *
 * @author Jun
 * @since 1.0.0
 */
@Target({ElementType.TYPE, ElementType.FIELD})
public @interface Immutable {
}
