package com.qscout.spring.i18n;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.Locale;

public final class MessageSources {
    private static final Locale DEFAULT_LOCALE = Locale.JAPANESE;

    static {
        Locale.setDefault(DEFAULT_LOCALE);
        LocaleContextHolder.setDefaultLocale(DEFAULT_LOCALE);
        LocaleContextHolder.setLocale(DEFAULT_LOCALE);
    }

    private MessageSources() {
    }

    public static MessageSource create() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        messageSource.setUseCodeAsDefaultMessage(true);
        return messageSource;
    }

    public static Locale resolveLocale() {
        LocaleContext localeContext = LocaleContextHolder.getLocaleContext();
        Locale locale = localeContext != null ? localeContext.getLocale() : null;
        if (locale == null) {
            return DEFAULT_LOCALE;
        }
        return locale.getLanguage().isBlank() ? DEFAULT_LOCALE : locale;
    }
}
