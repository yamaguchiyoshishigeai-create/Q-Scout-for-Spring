package com.qscout.spring.web.controller;

import com.qscout.spring.i18n.MessageSources;
import com.qscout.spring.web.dto.UploadErrorModalView;
import com.qscout.spring.web.service.UploadValidationService;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WebUploadExceptionHandlerTest {
    @Test
    void redirectsBackToHomeWithUploadErrorModalWhenMultipartLimitIsExceeded() throws Exception {
        Locale previous = LocaleContextHolder.getLocale();
        LocaleContextHolder.setLocale(Locale.JAPANESE);
        try {
            MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingUploadController())
                    .setControllerAdvice(new WebUploadExceptionHandler(MessageSources.create()))
                    .build();

            var result = mockMvc.perform(post("/analyze").locale(Locale.JAPANESE))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/"))
                    .andExpect(flash().attributeExists("uploadErrorModal"))
                    .andReturn();

            UploadErrorModalView modal = (UploadErrorModalView) result.getFlashMap().get("uploadErrorModal");
            assertThat(modal.title()).isEqualTo("アップロードに失敗しました");
            assertThat(modal.body()).isEqualTo("アップロードしたzipファイルが上限サイズ20MBを超えています。");
            assertThat(modal.retry()).isEqualTo("20MB以下のzipファイルを選択して、再度実行してください。");
        } finally {
            LocaleContextHolder.setLocale(previous);
        }
    }

    @Controller
    static class ThrowingUploadController {
        @PostMapping("/analyze")
        @ResponseBody
        String analyze() {
            throw new MaxUploadSizeExceededException(UploadValidationService.MAX_UPLOAD_SIZE_BYTES);
        }
    }
}
