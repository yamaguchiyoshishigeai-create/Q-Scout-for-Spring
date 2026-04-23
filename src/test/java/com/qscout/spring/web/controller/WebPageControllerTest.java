package com.qscout.spring.web.controller;

import com.qscout.spring.i18n.MessageSources;
import com.qscout.spring.web.dto.DownloadLinkView;
import com.qscout.spring.web.dto.ErrorViewModel;
import com.qscout.spring.web.dto.RateLimitDecision;
import com.qscout.spring.web.dto.SummaryDisplayView;
import com.qscout.spring.web.dto.UploadErrorModalView;
import com.qscout.spring.web.dto.WebAnalysisResponse;
import com.qscout.spring.web.exception.AnalysisTimeoutException;
import com.qscout.spring.web.exception.InvalidUploadException;
import com.qscout.spring.web.exception.UploadTooLargeException;
import com.qscout.spring.web.service.ClientIpResolver;
import com.qscout.spring.web.service.RequestRateLimiter;
import com.qscout.spring.web.service.TrustedProxyPolicy;
import com.qscout.spring.web.service.WebAnalysisService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.springframework.ui.ConcurrentModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WebPageControllerTest {
    @Test
    void putsResponseIntoModelOnSuccess() {
        WebAnalysisService webAnalysisService = mock(WebAnalysisService.class);
        RequestRateLimiter requestRateLimiter = mock(RequestRateLimiter.class);
        when(requestRateLimiter.evaluate("127.0.0.1")).thenReturn(RateLimitDecision.allow(4));
        WebPageController controller = controller(webAnalysisService, requestRateLimiter, defaultClientIpResolver());
        MockMultipartFile file = new MockMultipartFile("projectZip", "sample.zip", "application/zip", new byte[]{1});
        WebAnalysisResponse response = new WebAnalysisResponse(
                "req-1", "sample.zip", "2026-04-13 10:30", 80, 3, 1, 1, 1,
                "score-band-high",
                new DownloadLinkView("download", "/download/req-1/human", "qscout-report.md"),
                new DownloadLinkView("download", "/download/req-1/ai", "qscout-ai-input.md"),
                "/preview/req-1/human?lang=ja",
                "/preview/req-1/ai?lang=ja",
                "ok", "auto excluded", false, true
        );
        when(webAnalysisService.analyze(file)).thenReturn(response);

        ConcurrentModel model = new ConcurrentModel();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String view = controller.analyze(file, model, request("127.0.0.1"), new MockHttpServletResponse(), redirectAttributes);

        assertThat(view).isEqualTo("redirect:/#result-summary");
        assertThat(redirectAttributes.getFlashAttributes().get("response")).isEqualTo(response);
        assertThat(redirectAttributes.getFlashAttributes().get("resultSummary")).isEqualTo(new SummaryDisplayView(response, false, "#artifacts"));
        assertThat(model.getAttribute("sampleSummary")).isNotNull();
        assertThat(model.getAttribute("limits")).isNotNull();
    }

    @Test
    void returnsHelpPage() {
        WebAnalysisService webAnalysisService = mock(WebAnalysisService.class);
        WebPageController controller = controller(webAnalysisService, mock(RequestRateLimiter.class), defaultClientIpResolver());

        ConcurrentModel model = new ConcurrentModel();
        String view = controller.showHelp(model);

        assertThat(view).isEqualTo("help");
        assertThat(model.getAttribute("sampleSummary")).isNotNull();
        assertThat(model.getAttribute("resultSummary")).isNull();
        assertThat(model.getAttribute("limits")).isNotNull();
        SummaryDisplayView sampleSummary = (SummaryDisplayView) model.getAttribute("sampleSummary");
        assertThat(sampleSummary.response().scoreBandClass()).isEqualTo("score-band-high");
    }

    @Test
    void mapsTooLargeUploadToModalModel() {
        WebAnalysisService webAnalysisService = mock(WebAnalysisService.class);
        RequestRateLimiter requestRateLimiter = mock(RequestRateLimiter.class);
        when(requestRateLimiter.evaluate("127.0.0.1")).thenReturn(RateLimitDecision.allow(4));
        WebPageController controller = controller(webAnalysisService, requestRateLimiter, defaultClientIpResolver());
        MockMultipartFile file = new MockMultipartFile("projectZip", "large.zip", "application/zip", new byte[]{1});
        when(webAnalysisService.analyze(file)).thenThrow(new UploadTooLargeException("too large"));

        ConcurrentModel model = new ConcurrentModel();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String view = controller.analyze(file, model, request("127.0.0.1"), new MockHttpServletResponse(), redirectAttributes);

        assertThat(view).isEqualTo("redirect:/#run-analysis");
        UploadErrorModalView modal = (UploadErrorModalView) redirectAttributes.getFlashAttributes().get("uploadErrorModal");
        assertThat(modal).isNotNull();
        assertThat(modal.title()).isEqualTo("アップロードに失敗しました");
        assertThat(modal.body()).isEqualTo("アップロードしたzipファイルが上限サイズ20MBを超えています。");
        assertThat(modal.retry()).isEqualTo("20MB以下のzipファイルを選択して、再度実行してください。");
        assertThat(model.getAttribute("error")).isNull();
    }

    @Test
    void mapsInputErrorToModel() {
        WebAnalysisService webAnalysisService = mock(WebAnalysisService.class);
        RequestRateLimiter requestRateLimiter = mock(RequestRateLimiter.class);
        when(requestRateLimiter.evaluate("127.0.0.1")).thenReturn(RateLimitDecision.allow(4));
        WebPageController controller = controller(webAnalysisService, requestRateLimiter, defaultClientIpResolver());
        MockMultipartFile file = new MockMultipartFile("projectZip", "bad.zip", "application/zip", new byte[]{1});
        when(webAnalysisService.analyze(file)).thenThrow(new InvalidUploadException("bad input"));

        ConcurrentModel model = new ConcurrentModel();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String view = controller.analyze(file, model, request("127.0.0.1"), new MockHttpServletResponse(), redirectAttributes);

        assertThat(view).isEqualTo("redirect:/#run-analysis");
        ErrorViewModel error = (ErrorViewModel) redirectAttributes.getFlashAttributes().get("error");
        assertThat(error.detailCode()).isEqualTo("INPUT_ERROR");
        assertThat(error.userMessage()).isEqualTo("bad input");
    }

    @Test
    void mapsTimeoutToModel() {
        WebAnalysisService webAnalysisService = mock(WebAnalysisService.class);
        RequestRateLimiter requestRateLimiter = mock(RequestRateLimiter.class);
        when(requestRateLimiter.evaluate("127.0.0.1")).thenReturn(RateLimitDecision.allow(4));
        WebPageController controller = controller(webAnalysisService, requestRateLimiter, defaultClientIpResolver());
        MockMultipartFile file = new MockMultipartFile("projectZip", "slow.zip", "application/zip", new byte[]{1});
        when(webAnalysisService.analyze(file)).thenThrow(new AnalysisTimeoutException("timeout", new RuntimeException("slow")));

        ConcurrentModel model = new ConcurrentModel();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String view = controller.analyze(file, model, request("127.0.0.1"), new MockHttpServletResponse(), redirectAttributes);

        assertThat(view).isEqualTo("redirect:/#run-analysis");
        ErrorViewModel error = (ErrorViewModel) redirectAttributes.getFlashAttributes().get("error");
        assertThat(error.detailCode()).isEqualTo("TIMEOUT");
    }

    @Test
    void mapsRateLimitTo429Model() {
        WebAnalysisService webAnalysisService = mock(WebAnalysisService.class);
        RequestRateLimiter requestRateLimiter = mock(RequestRateLimiter.class);
        when(requestRateLimiter.evaluate("198.51.100.10")).thenReturn(RateLimitDecision.deny(120));
        WebPageController controller = controller(webAnalysisService, requestRateLimiter, defaultClientIpResolver());

        ConcurrentModel model = new ConcurrentModel();
        MockHttpServletResponse response = new MockHttpServletResponse();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.analyze(null, model, request("198.51.100.10"), response, redirectAttributes);

        assertThat(view).isEqualTo("index");
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("120");
        ErrorViewModel error = (ErrorViewModel) model.getAttribute("error");
        assertThat(error.detailCode()).isEqualTo("RATE_LIMIT_EXCEEDED");
        assertThat(model.getAttribute("postAnalyzeAnchor")).isEqualTo("run-analysis");
    }

    @Test
    void ignoresSpoofedForwardedHeaderForRateLimitingWhenRemoteAddrIsNotTrusted() {
        WebAnalysisService webAnalysisService = mock(WebAnalysisService.class);
        RequestRateLimiter requestRateLimiter = mock(RequestRateLimiter.class);
        when(requestRateLimiter.evaluate("198.51.100.10")).thenReturn(RateLimitDecision.deny(120));
        WebPageController controller = controller(webAnalysisService, requestRateLimiter, defaultClientIpResolver());

        MockHttpServletRequest request = request("198.51.100.10");
        request.addHeader("X-Forwarded-For", "203.0.113.1");
        ConcurrentModel model = new ConcurrentModel();
        MockHttpServletResponse response = new MockHttpServletResponse();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        controller.analyze(null, model, request, response, redirectAttributes);

        verify(requestRateLimiter).evaluate("198.51.100.10");
        verify(requestRateLimiter, never()).evaluate("203.0.113.1");
        verifyNoInteractions(webAnalysisService);
        assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    void usesForwardedHeaderForRateLimitingWhenRemoteAddrIsTrusted() {
        WebAnalysisService webAnalysisService = mock(WebAnalysisService.class);
        RequestRateLimiter requestRateLimiter = mock(RequestRateLimiter.class);
        when(requestRateLimiter.evaluate("203.0.113.1")).thenReturn(RateLimitDecision.deny(120));
        WebPageController controller = controller(webAnalysisService, requestRateLimiter, trustedForwardedResolver());

        MockHttpServletRequest request = request("127.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.1, 198.51.100.200");
        ConcurrentModel model = new ConcurrentModel();
        MockHttpServletResponse response = new MockHttpServletResponse();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        controller.analyze(null, model, request, response, redirectAttributes);

        verify(requestRateLimiter).evaluate("203.0.113.1");
        verify(requestRateLimiter, never()).evaluate("127.0.0.1");
        verifyNoInteractions(webAnalysisService);
        assertThat(response.getStatus()).isEqualTo(429);
    }

    private WebPageController controller(
            WebAnalysisService webAnalysisService,
            RequestRateLimiter requestRateLimiter,
            ClientIpResolver clientIpResolver
    ) {
        return new WebPageController(webAnalysisService, requestRateLimiter, clientIpResolver, MessageSources.create());
    }

    private ClientIpResolver defaultClientIpResolver() {
        return new ClientIpResolver(TrustedProxyPolicy.forTesting(false, "127.0.0.1", "::1"), false);
    }

    private ClientIpResolver trustedForwardedResolver() {
        return new ClientIpResolver(TrustedProxyPolicy.forTesting(true, "127.0.0.1", "::1"), false);
    }

    private MockHttpServletRequest request(String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        return request;
    }
}
