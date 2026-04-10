package com.qscout.spring.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class WebConfigTest {
    private final WebConfig webConfig = new WebConfig();

    @Test
    void usesSessionLocaleResolverWithJapaneseDefault() {
        LocaleResolver resolver = webConfig.localeResolver();

        assertThat(resolver).isInstanceOf(SessionLocaleResolver.class);
        assertThat(resolver.resolveLocale(new MockHttpServletRequest())).isEqualTo(Locale.JAPANESE);
    }

    @Test
    void usesLocaleChangeInterceptorWithLangParameter() {
        LocaleChangeInterceptor interceptor = webConfig.localeChangeInterceptor();

        assertThat(interceptor.getParamName()).isEqualTo("lang");
    }

    @Test
    void createsMessageSourceBackedByMessagesBundleWithJapaneseFallback() {
        MessageSource messageSource = webConfig.messageSource();

        assertThat(messageSource.getMessage("form.upload.button", null, Locale.JAPANESE)).isEqualTo("解析を実行する");
        assertThat(messageSource.getMessage("form.upload.button", null, Locale.ENGLISH)).isEqualTo("Run Analysis");
    }
}
