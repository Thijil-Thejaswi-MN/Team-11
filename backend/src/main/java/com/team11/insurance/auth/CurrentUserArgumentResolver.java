package com.team11.insurance.auth;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Spring MVC Argument Resolver that injects {@link CurrentUser} into controller method parameters.
 * Supports parameters of type {@link CurrentUser} directly or annotated with {@link com.team11.insurance.auth.annotation.CurrentUser}.
 */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final ICurrentUserProvider currentUserProvider;

    public CurrentUserArgumentResolver(ICurrentUserProvider currentUserProvider) {
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(CurrentUser.class) ||
                parameter.hasParameterAnnotation(com.team11.insurance.auth.annotation.CurrentUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        return currentUserProvider.getCurrentUser();
    }
}
