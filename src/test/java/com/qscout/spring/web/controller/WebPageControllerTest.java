package com.qscout.spring.web.controller;

import com.qscout.spring.i18n.MessageSources;
import com.qscout.spring.web.dto.DownloadLinkView;
import com.qscout.spring.web.dto.ErrorViewModel;
import com.qscout.spring.web.dto.SummaryDisplayView;
import com.qscout.spring.web.dto.UploadErrorModalView;
import com.qscout.spring.web.dto.WebAnalysisResponse;
import com.qscout.spring.web.exception.AnalysisTimeoutException;
import com.qscout.spring.web.exception.InvalidUploadException;
import com.qscout.spring.web.exception.UploadTooLargeException;
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
                "req-1", "sample.zip", "2026-04-13 10:30", 80, 3, 1, 1, 1,
                new DownloadLinkView("download", "/download/req-1/human", "qscout-report.md"),
                new DownloadLinkView("download", "/download/req-1/ai", "qscout-ai-input.md"),
                "ok", false, true
        );
        when(webAnalysisService.analyze(file)).thenReturn(response);

        ConcurrentModel model = new ConcurrentModel();
        String view = controller.analyze(file, model);

        assertThat(view).isEqualTo("index");
        assertThat(model.getAttribute("response")).isEqualTo(response);
        assertThat(model.getAttribute("resultSummary")).isEqualTo(new SummaryDisplayView(response, false, "#artifacts"));
        assertThat(model.getAttribute("sampleSummary")).isNotNull();
        assertThat(model.getAttribute("limits")).isNotNull();
    }

    @Test
    void returnsHelpPage() {
        WebAnalysisService webAnalysisService = mock(WebAnalysisService.class);
        WebPageController controller = new WebPageController(webAnalysisService, MessageSources.create());

        ConcurrentModel model = new ConcurrentModel();
        String view = controller.showHelp(model);

        assertThat(view).isEqualTo("help");
        assertThat(model.getAttribute("sampleSummary")).isNotNull();
        assertThat(model.getAttribute("resultSummary")).isNull();
        assertThat(model.getAttribute("limits")).isNotNull();
    }

    @Test
    void mapsTooLargeUploadToModalModel() {
        WebAnalysisService webAnalysisService = mock(WebAnalysisService.class);
        WebPageController controller = new WebPageController(webAnalysisService, MessageSources.create());
        MockMultipartFile file = new MockMultipartFile("projectZip", "large.zip", "application/zip", new byte[]{1});
        when(webAnalysisService.analyze(file)).thenThrow(new UploadTooLargeException("too large"));

        ConcurrentModel model = new ConcurrentModel();
        controller.analyze(file, model);

        UploadErrorModalView modal = (UploadErrorModalView) model.getAttribute("uploadErrorModal");
        assertThat(modal).isNotNull();
        assertThat(modal.title()).isEqualTo("アップロードに失敗しました");
        assertThat(modal.body()).isEqualTo("アップロードしたzipファイルが上限サイズ20MBを超えています。");
        assertThat(modal.retry()).isEqualTo("20MB以下のzipファイルを選択して、再度実行してください。");
        assertThat(model.getAttribute("error")).isNull();
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
