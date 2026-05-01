package com.qscout.spring.web.controller;

import com.qscout.spring.web.dto.PreviewArtifactView;
import com.qscout.spring.web.exception.ArtifactExpiredException;
import com.qscout.spring.web.service.DownloadArtifactService;
import com.qscout.spring.web.service.MarkdownPreviewRenderer;
import com.qscout.spring.web.service.RequestAccessTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class WebPreviewControllerTest {
    @Test
    void showsPreviewPageWithLanguageSpecificHumanMarkdown() throws Exception {
        DownloadArtifactService downloadArtifactService = mock(DownloadArtifactService.class);
        MarkdownPreviewRenderer markdownPreviewRenderer = mock(MarkdownPreviewRenderer.class);
        RequestAccessTokenService requestAccessTokenService = mock(RequestAccessTokenService.class);
        when(requestAccessTokenService.isValid("req-1", "human", 123L, "ok")).thenReturn(true);
        when(requestAccessTokenService.createSignedUrl("/download/req-1/human", "req-1", "human", Map.of("lang", "en"))).thenReturn("/download/req-1/human?lang=en&expires=456&token=next");
        when(requestAccessTokenService.createSignedUrl("/preview/req-1/human", "req-1", "human", Map.of("lang", "ja"))).thenReturn("/preview/req-1/human?lang=ja&expires=456&token=ja");
        when(requestAccessTokenService.createSignedUrl("/preview/req-1/human", "req-1", "human", Map.of("lang", "en"))).thenReturn("/preview/req-1/human?lang=en&expires=456&token=en");
        when(downloadArtifactService.resolveForPreview("req-1", "human", "en")).thenReturn(
                new DownloadArtifactService.PreviewArtifact("human", "qscout-report-en.md", org.springframework.http.MediaType.TEXT_MARKDOWN, "# English report", "score-band-high")
        );
        when(markdownPreviewRenderer.render("# English report")).thenReturn("<h1>English report</h1>");
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WebPreviewController(downloadArtifactService, markdownPreviewRenderer, requestAccessTokenService)).build();

        MvcResult result = mockMvc.perform(get("/preview/req-1/human")
                        .param("expires", "123")
                        .param("token", "ok")
                        .param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store, no-cache, must-revalidate"))
                .andExpect(view().name("preview"))
                .andExpect(model().attributeExists("preview"))
                .andReturn();

        PreviewArtifactView preview = (PreviewArtifactView) result.getModelAndView().getModel().get("preview");
        assertThat(preview.fileName()).isEqualTo("qscout-report-en.md");
        assertThat(preview.content()).contains("English report");
        assertThat(preview.downloadUrl()).contains("lang=en");
        assertThat(preview.scoreBandClass()).isEqualTo("score-band-high");
    }

    @Test
    void mapsExpiredPreviewToGone() throws Exception {
        DownloadArtifactService downloadArtifactService = mock(DownloadArtifactService.class);
        MarkdownPreviewRenderer markdownPreviewRenderer = mock(MarkdownPreviewRenderer.class);
        RequestAccessTokenService requestAccessTokenService = mock(RequestAccessTokenService.class);
        when(requestAccessTokenService.isValid("req-1", "human", 123L, "ok")).thenReturn(true);
        when(downloadArtifactService.resolveForPreview("req-1", "human", "ja")).thenThrow(new ArtifactExpiredException("expired"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WebPreviewController(downloadArtifactService, markdownPreviewRenderer, requestAccessTokenService)).build();

        mockMvc.perform(get("/preview/req-1/human").param("expires", "123").param("token", "ok"))
                .andExpect(status().isGone());
    }

    @Test
    void mapsIllegalArgumentToNotFound() throws Exception {
        DownloadArtifactService downloadArtifactService = mock(DownloadArtifactService.class);
        MarkdownPreviewRenderer markdownPreviewRenderer = mock(MarkdownPreviewRenderer.class);
        RequestAccessTokenService requestAccessTokenService = mock(RequestAccessTokenService.class);
        when(requestAccessTokenService.isValid("req-1", "bad", 123L, "ok")).thenReturn(true);
        when(downloadArtifactService.resolveForPreview("req-1", "bad", "ja")).thenThrow(new IllegalArgumentException("bad"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WebPreviewController(downloadArtifactService, markdownPreviewRenderer, requestAccessTokenService)).build();

        mockMvc.perform(get("/preview/req-1/bad").param("expires", "123").param("token", "ok"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsInvalidToken() throws Exception {
        DownloadArtifactService downloadArtifactService = mock(DownloadArtifactService.class);
        MarkdownPreviewRenderer markdownPreviewRenderer = mock(MarkdownPreviewRenderer.class);
        RequestAccessTokenService requestAccessTokenService = mock(RequestAccessTokenService.class);
        when(requestAccessTokenService.isValid("req-1", "human", 123L, "bad")).thenReturn(false);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WebPreviewController(downloadArtifactService, markdownPreviewRenderer, requestAccessTokenService)).build();

        mockMvc.perform(get("/preview/req-1/human").param("expires", "123").param("token", "bad"))
                .andExpect(status().isForbidden());
    }
}
