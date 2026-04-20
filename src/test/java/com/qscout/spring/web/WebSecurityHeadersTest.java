package com.qscout.spring.web;

import com.qscout.spring.web.dto.WebAnalysisResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "qscout.client-ip.trust-forwarded-headers=true",
        "qscout.client-ip.trusted-proxies=127.0.0.1,::1"
})
@AutoConfigureMockMvc
class WebSecurityHeadersTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void addsSecurityHeadersToIndex() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().exists("Content-Security-Policy"))
                .andExpect(header().exists("Permissions-Policy"));
    }

    @Test
    void addsNoStoreHeadersToPreviewAndDownload() throws Exception {
        MockMultipartFile zip = new MockMultipartFile(
                "projectZip",
                "sample-project.zip",
                "application/zip",
                zipDirectory(Path.of("samples/sample-project").toAbsolutePath().normalize())
        );

        MvcResult analyzeResult = mockMvc.perform(multipart("/analyze").file(zip).header("X-Forwarded-For", "198.51.100.200"))
                .andExpect(status().isOk())
                .andReturn();
        WebAnalysisResponse response = (WebAnalysisResponse) analyzeResult.getModelAndView().getModel().get("response");

        mockMvc.perform(get(response.humanPreviewUrl()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store, no-cache, must-revalidate"))
                .andExpect(header().string("Pragma", "no-cache"))
                .andExpect(header().string("Expires", "0"));

        mockMvc.perform(get(response.humanDownloadLink().url()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store, no-cache, must-revalidate"))
                .andExpect(header().string("Pragma", "no-cache"))
                .andExpect(header().string("Expires", "0"));
    }

    private byte[] zipDirectory(Path root) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);
                 var paths = Files.walk(root)) {
                paths.filter(Files::isRegularFile).forEach(path -> {
                    String entryName = root.relativize(path).toString().replace('\\', '/');
                    try {
                        zipOutputStream.putNextEntry(new ZipEntry(entryName));
                        zipOutputStream.write(Files.readAllBytes(path));
                        zipOutputStream.closeEntry();
                    } catch (IOException exception) {
                        throw new UncheckedIOException(exception);
                    }
                });
            }
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
