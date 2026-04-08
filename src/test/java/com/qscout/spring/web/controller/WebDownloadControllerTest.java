package com.qscout.spring.web.controller;

import com.qscout.spring.web.exception.ArtifactExpiredException;
import com.qscout.spring.web.service.DownloadArtifactService;
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
        when(downloadArtifactService.resolveForDownload("req-1", "human")).thenReturn(
                new DownloadArtifactService.DownloadArtifact(new ByteArrayResource("ok".getBytes()), "qscout-report.md", MediaType.TEXT_MARKDOWN)
        );
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WebDownloadController(downloadArtifactService)).build();

        mockMvc.perform(get("/download/req-1/human"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("qscout-report.md")));
    }

    @Test
    void mapsExpiredArtifactToGone() throws Exception {
        DownloadArtifactService downloadArtifactService = mock(DownloadArtifactService.class);
        when(downloadArtifactService.resolveForDownload("req-1", "human")).thenThrow(new ArtifactExpiredException("expired"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WebDownloadController(downloadArtifactService)).build();

        mockMvc.perform(get("/download/req-1/human"))
                .andExpect(status().isGone());
    }

    @Test
    void mapsIllegalArgumentToNotFound() throws Exception {
        DownloadArtifactService downloadArtifactService = mock(DownloadArtifactService.class);
        when(downloadArtifactService.resolveForDownload("req-1", "bad")).thenThrow(new IllegalArgumentException("bad"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WebDownloadController(downloadArtifactService)).build();

        mockMvc.perform(get("/download/req-1/bad"))
                .andExpect(status().isNotFound());
    }
}
