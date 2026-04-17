package com.qscout.spring.web.controller;

import com.qscout.spring.web.exception.ArtifactExpiredException;
import com.qscout.spring.web.service.DownloadArtifactService;
import com.qscout.spring.web.service.RequestAccessTokenService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.GONE;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
public class WebDownloadController {
    private final DownloadArtifactService downloadArtifactService;
    private final RequestAccessTokenService requestAccessTokenService;

    public WebDownloadController(DownloadArtifactService downloadArtifactService, RequestAccessTokenService requestAccessTokenService) {
        this.downloadArtifactService = downloadArtifactService;
        this.requestAccessTokenService = requestAccessTokenService;
    }

    @GetMapping("/download/{requestId}/{fileKey}")
    public ResponseEntity<Resource> download(
            @PathVariable String requestId,
            @PathVariable String fileKey,
            @RequestParam long expires,
            @RequestParam String token
    ) {
        try {
            if (!requestAccessTokenService.isValid(requestId, fileKey, expires, token)) {
                throw new ResponseStatusException(FORBIDDEN, "Invalid artifact access token.");
            }
            DownloadArtifactService.DownloadArtifact artifact = downloadArtifactService.resolveForDownload(requestId, fileKey);
            return ResponseEntity.ok()
                    .contentType(artifact.contentType())
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment().filename(artifact.fileName()).build().toString())
                    .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate")
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .header(HttpHeaders.EXPIRES, "0")
                    .body(artifact.resource());
        } catch (ArtifactExpiredException exception) {
            throw new ResponseStatusException(GONE, exception.getMessage(), exception);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(NOT_FOUND, exception.getMessage(), exception);
        }
    }
}
