# Q-Scout for Spring

## Docker local run

This repository is a Spring Boot 3 web application packaged as a runnable jar.

### Build image

```bash
docker build -t qscout-for-spring .
```

### Run container

```bash
docker run --rm -p 8080:8080 -e PORT=8080 qscout-for-spring
```

### Open in browser

```text
http://localhost:8080/
```

### Notes

- The application listens on `0.0.0.0`.
- The HTTP port is controlled by the `PORT` environment variable and defaults to `8080`.
- Uploaded zip files are analyzed in the container's temporary directory.