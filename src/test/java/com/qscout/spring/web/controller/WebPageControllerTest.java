package com.qscout.spring.web.controller;

import com.qscout.spring.i18n.MessageSources;
import com.qscout.spring.web.dto.DownloadLinkView;
import com.qscout.spring.web.dto.ErrorViewModel;
import com.qscout.spring.web.dto.WebAnalysisResponse;
import com.qscout.spring.web.exception.AnalysisTimeoutException;
import com.qscout.spring.web.exception.InvalidUploadException;
import com.qscout.spring.web.service.WebAnalysisService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.ui.ConcurrentModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebPageControllerTest {
    @Test
    void putsResponseIntoModelOnSuccess() {
        WebAnalysisService webAnalysisService = mock(WebAnalysisService.class);
        WebPageController controller = new WebPageController(webAnalysisService, MessageSources.create());
        MockMultipartFile file = new MockMultipartFile("projectZip", "sample.zip", "application/zip", new byte[]{1});
        WebAnalysisResponse response = new WebAnalysisResponse(
                "req-1", 80, 3, 1, 1, 1,
                new DownloadLinkView("human", "/download/req-1/human", "qscout-report.md"),
                new DownloadLinkView("ai", "/download/req-1/ai", "qscout-ai-input.md"),
                "ok", false, true
        );
        when(webAnalysisService.analyze(file)).thenReturn(response);

        ConcurrentModel model = new ConcurrentModel();
        String view = controller.analyze(file, model);

        assertThat(view).isEqualTo("index");
        assertThat(model.getAttribute("response")).isEqualTo(response);
        assertThat(model.getAttribute("limits")).isNotNull();
    }

    @Test
    void mapsInputErrorToModel() {
        WebAnalysisService webAnalysisService = mock(WebAnalysisService.class);
        WebPageController controller = new WebPageController(webAnalysisService, MessageSources.create());
        MockMultipartFile file = new MockMultipartFile("projectZip", "bad.zip", "application/zip", new byte[]{1});
        when(webAnalysisService.analyze(file)).thenThrow(new InvalidUploadException("bad input"));

        ConcurrentModel model = new ConcurrentModel();
        controller.analyze(file, model);

        ErrorViewModel error = (ErrorViewModel) model.getAttribute("error");
        assertThat(error.detailCode()).isEqualTo("INPUT_ERROR");
        assertThat(error.userMessage()).isEqualTo("bad input");
    }

    @Test
    void mapsTimeoutToModel() {
        WebAnalysisService webAnalysisService = mock(WebAnalysisService.class);
        WebPageController controller = new WebPageController(webAnalysisService, MessageSources.create());
        MockMultipartFile file = new MockMultipartFile("projectZip", "slow.zip", "application/zip", new byte[]{1});
        when(webAnalysisService.analyze(file)).thenThrow(new AnalysisTimeoutException("timeout", new RuntimeException("slow")));

        ConcurrentModel model = new ConcurrentModel();
        controller.analyze(file, model);

        ErrorViewModel error = (ErrorViewModel) model.getAttribute("error");
        assertThat(error.detailCode()).isEqualTo("TIMEOUT");
    }
}
