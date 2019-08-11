package easyble2.annotation;

import com.snail.commons.methodpost.ThreadMode;

import java.lang.annotation.*;

/**
 * 
 * date: 2019/8/9 12:46
 * author: zengfansheng
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Observe {
    ThreadMode value() default ThreadMode.UNSPECIFIED;
}
