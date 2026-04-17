package com.qscout.spring.web.controller;

import com.qscout.spring.web.exception.ArtifactExpiredException;
import com.qscout.spring.web.service.DownloadArtifactService;
import com.qscout.spring.web.service.MarkdownPreviewRenderer;
import com.qscout.spring.web.service.RequestAccessTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class WebPreviewControllerTest {
    @Test
    void showsPreviewPage() throws Exception {
        DownloadArtifactService downloadArtifactService = mock(DownloadArtifactService.class);
        MarkdownPreviewRenderer markdownPreviewRenderer = mock(MarkdownPreviewRenderer.class);
        RequestAccessTokenService requestAccessTokenService = mock(RequestAccessTokenService.class);
        when(requestAccessTokenService.isValid("req-1", "human", 123L, "ok")).thenReturn(true);
        when(requestAccessTokenService.createSignedUrl("/download/req-1/human", "req-1", "human")).thenReturn("/download/req-1/human?expires=456&token=next");
        when(requestAccessTokenService.createSignedUrl("/preview/req-1/human", "req-1", "human", java.util.Map.of("lang", "ja"))).thenReturn("/preview/req-1/human?lang=ja&expires=456&token=ja");
        when(requestAccessTokenService.createSignedUrl("/preview/req-1/human", "req-1", "human", java.util.Map.of("lang", "en"))).thenReturn("/preview/req-1/human?lang=en&expires=456&token=en");
        when(downloadArtifactService.resolveForPreview("req-1", "human")).thenReturn(
                new DownloadArtifactService.PreviewArtifact("human", "qscout-report.md", org.springframework.http.MediaType.TEXT_MARKDOWN, "# report")
        );
        when(markdownPreviewRenderer.render("# report")).thenReturn("<h1>report</h1>");
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WebPreviewController(downloadArtifactService, markdownPreviewRenderer, requestAccessTokenService)).build();

        mockMvc.perform(get("/preview/req-1/human").param("expires", "123").param("token", "ok"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store, no-cache, must-revalidate"))
                .andExpect(view().name("preview"))
                .andExpect(model().attributeExists("preview"));
    }

    @Test
    void mapsExpiredPreviewToGone() throws Exception {
        DownloadArtifactService downloadArtifactService = mock(DownloadArtifactService.class);
        MarkdownPreviewRenderer markdownPreviewRenderer = mock(MarkdownPreviewRenderer.class);
        RequestAccessTokenService requestAccessTokenService = mock(RequestAccessTokenService.class);
        when(requestAccessTokenService.isValid("req-1", "human", 123L, "ok")).thenReturn(true);
        when(downloadArtifactService.resolveForPreview("req-1", "human")).thenThrow(new ArtifactExpiredException("expired"));
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
        when(downloadArtifactService.resolveForPreview("req-1", "bad")).thenThrow(new IllegalArgumentException("bad"));
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
