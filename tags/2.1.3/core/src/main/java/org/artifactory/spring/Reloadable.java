package org.artifactory.spring;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate classes that can be reloaded.
 *
 * @author Tomer Cohen
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Reloadable {
    Class<? extends ReloadableBean> beanClass();

    Class<? extends ReloadableBean>[] initAfter() default {};
}
