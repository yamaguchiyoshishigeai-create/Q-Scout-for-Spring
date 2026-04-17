package com.qscout.spring.web.controller;

import com.qscout.spring.web.exception.ArtifactExpiredException;
import com.qscout.spring.web.service.DownloadArtifactService;
import com.qscout.spring.web.service.RequestAccessTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WebDownloadControllerTest {
    @Test
    void downloadsArtifact() throws Exception {
        DownloadArtifactService downloadArtifactService = mock(DownloadArtifactService.class);
        RequestAccessTokenService requestAccessTokenService = mock(RequestAccessTokenService.class);
        when(requestAccessTokenService.isValid("req-1", "human", 123L, "ok")).thenReturn(true);
        when(downloadArtifactService.resolveForDownload("req-1", "human")).thenReturn(
                new DownloadArtifactService.DownloadArtifact(new ByteArrayResource("ok".getBytes()), "qscout-report.md", MediaType.TEXT_MARKDOWN)
        );
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WebDownloadController(downloadArtifactService, requestAccessTokenService)).build();

        mockMvc.perform(get("/download/req-1/human").param("expires", "123").param("token", "ok"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store, no-cache, must-revalidate"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("qscout-report.md")));
    }

    @Test
    void mapsExpiredArtifactToGone() throws Exception {
        DownloadArtifactService downloadArtifactService = mock(DownloadArtifactService.class);
        RequestAccessTokenService requestAccessTokenService = mock(RequestAccessTokenService.class);
        when(requestAccessTokenService.isValid("req-1", "human", 123L, "ok")).thenReturn(true);
        when(downloadArtifactService.resolveForDownload("req-1", "human")).thenThrow(new ArtifactExpiredException("expired"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WebDownloadController(downloadArtifactService, requestAccessTokenService)).build();

        mockMvc.perform(get("/download/req-1/human").param("expires", "123").param("token", "ok"))
                .andExpect(status().isGone());
    }

    @Test
    void mapsIllegalArgumentToNotFound() throws Exception {
        DownloadArtifactService downloadArtifactService = mock(DownloadArtifactService.class);
        RequestAccessTokenService requestAccessTokenService = mock(RequestAccessTokenService.class);
        when(requestAccessTokenService.isValid("req-1", "bad", 123L, "ok")).thenReturn(true);
        when(downloadArtifactService.resolveForDownload("req-1", "bad")).thenThrow(new IllegalArgumentException("bad"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WebDownloadController(downloadArtifactService, requestAccessTokenService)).build();

        mockMvc.perform(get("/download/req-1/bad").param("expires", "123").param("token", "ok"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsInvalidToken() throws Exception {
        DownloadArtifactService downloadArtifactService = mock(DownloadArtifactService.class);
        RequestAccessTokenService requestAccessTokenService = mock(RequestAccessTokenService.class);
        when(requestAccessTokenService.isValid("req-1", "human", 123L, "bad")).thenReturn(false);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WebDownloadController(downloadArtifactService, requestAccessTokenService)).build();

        mockMvc.perform(get("/download/req-1/human").param("expires", "123").param("token", "bad"))
                .andExpect(status().isForbidden());
    }
}
