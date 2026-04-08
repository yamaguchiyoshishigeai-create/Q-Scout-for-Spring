package com.qscout.spring.rule;

import com.qscout.spring.domain.ProjectContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionalMisuseRuleTest {
    private final TransactionalMisuseRule rule = new TransactionalMisuseRule();

    @TempDir
    Path tempDir;

    @Test
    void flagsControllerTransactionalUsage() throws IOException {
        Path file = writeSource("src/main/java/com/example/controller/SampleController.java", """
                package com.example.controller;

                import org.springframework.transaction.annotation.Transactional;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class SampleController {
                    @Transactional
                    public void handle() {
                    }
                }
                """);

        assertThat(rule.evaluate(context(file)).violations()).hasSize(1);
    }

    @Test
    void flagsServiceUsingRepositoryWithoutTransactional() throws IOException {
        Path file = writeSource("src/main/java/com/example/service/OrderService.java", """
                package com.example.service;

                import org.springframework.stereotype.Service;

                @Service
                public class OrderService {
                    private final OrderRepository orderRepository;

                    public OrderService(OrderRepository orderRepository) {
                        this.orderRepository = orderRepository;
                    }
                }
                """);

        assertThat(rule.evaluate(context(file)).violations()).hasSize(1);
    }

    @Test
    void ignoresServiceWithoutRepositoryAccess() throws IOException {
        Path file = writeSource("src/main/java/com/example/service/ViewService.java", """
                package com.example.service;

                import org.springframework.stereotype.Service;

                @Service
                public class ViewService {
                    public String render() {
                        return "ok";
                    }
                }
                """);

        assertThat(rule.evaluate(context(file)).violations()).isEmpty();
    }

    @Test
    void ignoresRulePackageFalsePositive() throws IOException {
        Path file = writeSource("src/main/java/com/qscout/spring/rule/TransactionalRuleHelper.java", """
                package com.qscout.spring.rule;

                public class TransactionalRuleHelper {
                    private Object userRepository;
                }
                """);

        assertThat(rule.evaluate(context(file)).violations()).isEmpty();
    }

    private ProjectContext context(Path mainFile) {
        return new ProjectContext(tempDir, tempDir.resolve("pom.xml"), List.of(mainFile), List.of());
    }

    private Path writeSource(String relativePath, String content) throws IOException {
        Path file = tempDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        return file;
    }
}
