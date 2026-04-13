package com.qscout.spring.web.controller;

import com.qscout.spring.web.exception.ArtifactExpiredException;
import com.qscout.spring.web.service.DownloadArtifactService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class WebPreviewControllerTest {
    @Test
    void showsPreviewPage() throws Exception {
        DownloadArtifactService downloadArtifactService = mock(DownloadArtifactService.class);
        when(downloadArtifactService.resolveForPreview("req-1", "human")).thenReturn(
                new DownloadArtifactService.PreviewArtifact("human", "qscout-report.md", org.springframework.http.MediaType.TEXT_MARKDOWN, "# report")
        );
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WebPreviewController(downloadArtifactService)).build();

        mockMvc.perform(get("/preview/req-1/human"))
                .andExpect(status().isOk())
                .andExpect(view().name("preview"))
                .andExpect(model().attributeExists("preview"));
    }

    @Test
    void mapsExpiredPreviewToGone() throws Exception {
        DownloadArtifactService downloadArtifactService = mock(DownloadArtifactService.class);
        when(downloadArtifactService.resolveForPreview("req-1", "human")).thenThrow(new ArtifactExpiredException("expired"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WebPreviewController(downloadArtifactService)).build();

        mockMvc.perform(get("/preview/req-1/human"))
                .andExpect(status().isGone());
    }

    @Test
    void mapsIllegalArgumentToNotFound() throws Exception {
        DownloadArtifactService downloadArtifactService = mock(DownloadArtifactService.class);
        when(downloadArtifactService.resolveForPreview("req-1", "bad")).thenThrow(new IllegalArgumentException("bad"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WebPreviewController(downloadArtifactService)).build();

        mockMvc.perform(get("/preview/req-1/bad"))
                .andExpect(status().isNotFound());
    }
}
