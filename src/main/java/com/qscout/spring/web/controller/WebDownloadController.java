package com.qscout.spring.web.controller;

import com.qscout.spring.web.exception.ArtifactExpiredException;
import com.qscout.spring.web.service.DownloadArtifactService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.GONE;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
public class WebDownloadController {
    private final DownloadArtifactService downloadArtifactService;

    public WebDownloadController(DownloadArtifactService downloadArtifactService) {
        this.downloadArtifactService = downloadArtifactService;
    }

    @GetMapping("/download/{requestId}/{fileKey}")
    public ResponseEntity<Resource> download(@PathVariable String requestId, @PathVariable String fileKey) {
        try {
            DownloadArtifactService.DownloadArtifact artifact = downloadArtifactService.resolveForDownload(requestId, fileKey);
            return ResponseEntity.ok()
                    .contentType(artifact.contentType())
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment().filename(artifact.fileName()).build().toString())
                    .body(artifact.resource());
        } catch (ArtifactExpiredException exception) {
            throw new ResponseStatusException(GONE, exception.getMessage(), exception);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(NOT_FOUND, exception.getMessage(), exception);
        }
    }
}
