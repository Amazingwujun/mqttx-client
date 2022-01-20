package com.jun;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * 对象工具
 *
 * @author Jun
 * @since 1.0.0
 */
public class ObjectUtils {

    /**
     * 判断对象是否为空.
     * <p>支持如下对象类型
     * <ul>
     *     <li>{@link Optional}</li>
     *     <li>{@link CharSequence}</li>
     *     <li>{@code Array}</li>
     *     <li>{@link Collection}</li>
     *     <li>{@link Map}</li>
     * </ul>
     *
     * @param obj 待检查对象
     * @return true 如果对象为空
     */
    public static boolean isEmpty(Object obj) {
        if (obj == null) {
            return true;
        } else if (obj instanceof Optional) {
            return !((Optional<?>) obj).isPresent();
        } else if (obj instanceof CharSequence) {
            return ((CharSequence) obj).length() == 0;
        } else if (obj.getClass().isArray()) {
            return Array.getLength(obj) == 0;
        } else if (obj instanceof Collection) {
            return ((Collection<?>) obj).isEmpty();
        } else {
            return obj instanceof Map && ((Map<?, ?>) obj).isEmpty();
        }
    }
}
