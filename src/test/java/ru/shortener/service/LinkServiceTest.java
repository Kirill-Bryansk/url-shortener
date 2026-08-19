package ru.shortener.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.shortener.model.Link;
import ru.shortener.repository.LinkRepository;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class LinkServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.liquibase.enabled", () -> "true");
    }

    @Autowired
    private LinkService linkService;

    @Autowired
    private LinkRepository linkRepository;

    @Test
    void createAndGetLink_shouldWork() {
        Link created = linkService.createShortLink("https://example.com", 1L);
        assertNotNull(created.getShortCode());

        String url = linkService.getOriginalUrl(created.getShortCode());
        assertEquals("https://example.com", url);

        Link found = linkRepository.findById(created.getId()).orElseThrow();
        assertEquals(1, found.getClickCount());
    }
}