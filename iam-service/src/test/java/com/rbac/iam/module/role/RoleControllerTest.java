package com.rbac.iam.module.role;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs against the local Oracle. @Transactional rolls every test back, so no rows are left behind.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class RoleControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private RoleRepository roleRepository;

    @Test
    @WithAnonymousUser
    void rejectsAnonymousRequestInErrorFormat() throws Exception {
        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("SYS-401"))
                .andExpect(jsonPath("$.traceId").value(not("UNKNOWN")));
    }

    @Test
    void pagesThroughRolesWithoutRepeatingOrSkipping() throws Exception {
        JsonNode first = readBody(mockMvc.perform(get("/api/roles").param("limit", "2")).andExpect(status().isOk()));
        assertThat(first.get("data").size()).isEqualTo(2);
        assertThat(first.get("meta").get("hasMore").asBoolean()).isTrue();

        String cursor = first.get("meta").get("nextCursor").asString();
        JsonNode second = readBody(mockMvc.perform(get("/api/roles").param("limit", "2").param("cursor", cursor))
                .andExpect(status().isOk()));
        assertThat(second.get("meta").get("hasMore").asBoolean()).isFalse();
        assertThat(second.get("meta").has("nextCursor")).isFalse();

        List<String> codes = new ArrayList<>();
        first.get("data").forEach(role -> codes.add(role.get("code").asString()));
        second.get("data").forEach(role -> codes.add(role.get("code").asString()));
        assertThat(codes).containsExactlyInAnyOrder("ADMIN", "HR", "EMPLOYEE");
    }

    @Test
    void rejectsInvalidPagingInput() throws Exception {
        mockMvc.perform(get("/api/roles").param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SYS-400"))
                .andExpect(jsonPath("$.details[0].field").value("limit"));

        mockMvc.perform(get("/api/roles").param("cursor", "not-a-cursor"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid cursor"));
    }

    @Test
    void findsRoleById() throws Exception {
        mockMvc.perform(get("/api/roles/{id}", seedRoleId("HR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("HR"))
                .andExpect(jsonPath("$.data.isSystem").value(false))
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    @Test
    void returnsErrorCodesForBadRequests() throws Exception {
        mockMvc.perform(get("/api/roles/{id}", 999999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ROL-001"));

        mockMvc.perform(get("/api/roles/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SYS-400"));

        mockMvc.perform(get("/api/nothing-here"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SYS-404"));

        mockMvc.perform(put("/api/roles"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.errorCode").value("SYS-405"));

        mockMvc.perform(post("/api/roles").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.errorCode").value("SYS-415"));
    }

    @Test
    void createsRoleAndFillsAuditDates() throws Exception {
        JsonNode body = readBody(postRole("{\"code\":\"AUDITOR\",\"name\":\"Auditor\"}")
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.message").value("Role created"))
                .andExpect(jsonPath("$.data.isSystem").value(false))
                .andExpect(jsonPath("$.data.isActive").value(true)));

        Role saved = roleRepository.findById(body.get("data").get("id").asLong()).orElseThrow();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void rejectsInvalidOrDuplicateCreate() throws Exception {
        postRole("{\"code\":\"ADMIN\",\"name\":\"Duplicate\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ROL-002"));

        postRole("{\"code\":\"lower-case\",\"name\":\"\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SYS-400"))
                .andExpect(jsonPath("$.details.length()").value(2));

        postRole("{broken json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is malformed"));
    }

    @Test
    void clientCannotCreateSystemRole() throws Exception {
        postRole("{\"code\":\"SNEAKY\",\"name\":\"Sneaky\",\"isSystem\":true}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.isSystem").value(false));
    }

    @Test
    void updatesRoleButNotItsCode() throws Exception {
        putRole(seedRoleId("HR"), "{\"code\":\"CHANGED\",\"name\":\"Human Resources\",\"isActive\":false}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("HR"))
                .andExpect(jsonPath("$.data.name").value("Human Resources"))
                .andExpect(jsonPath("$.data.isActive").value(false));

        putRole(seedRoleId("HR"), "{\"name\":\"Missing isActive\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("isActive"));
    }

    @Test
    void protectsSystemRole() throws Exception {
        Long adminId = seedRoleId("ADMIN");

        putRole(adminId, "{\"name\":\"Hacked\",\"isActive\":false}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ROL-003"));

        mockMvc.perform(delete("/api/roles/{id}", adminId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ROL-003"));
    }

    @Test
    void deletesUnusedRole() throws Exception {
        Long id = roleRepository.saveAndFlush(Role.builder().code("TEMP").name("Temp").build()).getId();

        mockMvc.perform(delete("/api/roles/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Role deleted"))
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(get("/api/roles/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void refusesToDeleteRoleAssignedToUser() throws Exception {
        Long id = roleRepository.saveAndFlush(Role.builder().code("TEMP").name("Temp").build()).getId();
        jdbcTemplate.update("""
                insert into iam_user (id, username, full_name, email)
                values (999999999, 'role-test-user', 'Role Test', 'role-test@example.com')
                """);
        jdbcTemplate.update("insert into iam_user_role (user_id, role_id) values (999999999, ?)", id);

        mockMvc.perform(delete("/api/roles/{id}", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ROL-004"));
    }

    private ResultActions postRole(String json) throws Exception {
        return mockMvc.perform(post("/api/roles").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions putRole(Long id, String json) throws Exception {
        return mockMvc.perform(put("/api/roles/{id}", id).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private JsonNode readBody(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }

    private Long seedRoleId(String code) {
        return jdbcTemplate.queryForObject("select id from iam_role where code = ?", Long.class, code);
    }
}
