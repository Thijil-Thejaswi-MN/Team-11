package com.team11.insurance.auth.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Parameter annotation to inject the resolved {@link com.team11.insurance.auth.CurrentUser}
 * into Spring MVC controller handler methods.
 * <p>
 * Example:
 * <pre>
 * &#64;GetMapping("/applications")
 * public ResponseEntity&lt;?&gt; listApplications(&#64;CurrentUser CurrentUser user) { ... }
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
