package com.pulsewatch.api;

import com.pulsewatch.domain.MonitorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Full HTTP-level tests of the REST API against an in-memory database. */
@SpringBootTest
class MonitorApiTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private MonitorRepository monitors;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        monitors.deleteAll();
    }

    @Test
    void creates_a_monitor_with_defaults_and_returns_its_location() throws Exception {
        mvc.perform(post("/api/monitors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Payments\", \"url\": \"https://example.com/health\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/monitors/" + onlyMonitorId())))
                .andExpect(jsonPath("$.name").value("Payments"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.intervalSeconds").value(30))
                .andExpect(jsonPath("$.expectedStatus").value(200))
                .andExpect(jsonPath("$.failureThreshold").value(3));
    }

    @Test
    void rejects_invalid_fields_with_a_problem_detail_listing_each_one() throws Exception {
        mvc.perform(post("/api/monitors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\", \"url\": \"ftp://files.example.com\", \"intervalSeconds\": 1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$..errors.name").exists())
                .andExpect(jsonPath("$..errors.url").exists())
                .andExpect(jsonPath("$..errors.intervalSeconds").exists());
    }

    @Test
    void rejects_malformed_json() throws Exception {
        mvc.perform(post("/api/monitors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Malformed request"));
    }

    @Test
    void unknown_monitor_returns_404_problem() throws Exception {
        mvc.perform(get("/api/monitors/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Monitor 999999 does not exist"));
    }

    @Test
    void lists_monitors_sorted_by_name() throws Exception {
        create("Zeta", "https://zeta.example.com");
        create("Alpha", "https://alpha.example.com");

        mvc.perform(get("/api/monitors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Alpha"))
                .andExpect(jsonPath("$[1].name").value("Zeta"));
    }

    @Test
    void updates_a_monitor() throws Exception {
        long id = create("Old name", "https://old.example.com");

        mvc.perform(put("/api/monitors/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"New name\", \"url\": \"https://new.example.com\", \"intervalSeconds\": 60}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New name"))
                .andExpect(jsonPath("$.url").value("https://new.example.com"))
                .andExpect(jsonPath("$.intervalSeconds").value(60));
    }

    @Test
    void pauses_and_resumes_a_monitor() throws Exception {
        long id = create("Pausable", "https://pause.example.com");

        mvc.perform(post("/api/monitors/" + id + "/pause"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paused").value(true))
                .andExpect(jsonPath("$.status").value("PAUSED"));

        mvc.perform(post("/api/monitors/" + id + "/resume"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paused").value(false))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void deletes_a_monitor() throws Exception {
        long id = create("Temporary", "https://temp.example.com");

        mvc.perform(delete("/api/monitors/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/monitors/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void checks_of_an_unknown_monitor_return_404() throws Exception {
        mvc.perform(get("/api/monitors/999999/checks")).andExpect(status().isNotFound());
        mvc.perform(post("/api/monitors/999999/check")).andExpect(status().isNotFound());
    }

    @Test
    void new_monitor_has_no_stats_yet() throws Exception {
        long id = create("Fresh", "https://fresh.example.com");

        mvc.perform(get("/api/monitors/" + id))
                .andExpect(jsonPath("$.uptime24h").doesNotExist())
                .andExpect(jsonPath("$.recentLatencies", hasSize(0)));
    }

    private long create(String name, String url) throws Exception {
        MvcResult result = mvc.perform(post("/api/monitors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"" + name + "\", \"url\": \"" + url + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String location = result.getResponse().getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    private long onlyMonitorId() {
        return monitors.findAll().get(0).getId();
    }
}
