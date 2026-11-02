package com.rbac.iam.module.user;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
class UserControllerTest {

    private static final String PASSWORD = "s3cret-pass";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private EntityManager entityManager;

    @Test
    @WithAnonymousUser
    void rejectsAnonymousRequest() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("SYS-401"));
    }

    @Test
    void createsUserWithRolesAndNeverReturnsPassword() throws Exception {
        JsonNode body = readBody(postUser(createJson("alice", "Alice@Example.com", "[" + seedRoleId("HR") + "]"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.message").value("User created"))
                .andExpect(jsonPath("$.data.username").value("alice"))
                .andExpect(jsonPath("$.data.email").value("alice@example.com"))
                .andExpect(jsonPath("$.data.isActive").value(true))
                .andExpect(jsonPath("$.data.roles[0].code").value("HR"))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist()));
        Long id = body.get("data").get("id").asLong();

        flushAndClear();
        assertThat(new BCryptPasswordEncoder().matches(PASSWORD, storedHash(id))).isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from iam_user where id = ? and created_at is not null", Long.class, id)).isEqualTo(1);
    }

    @Test
    void createsUserWithoutRoles() throws Exception {
        postUser("""
                {"username":"bob","password":"%s","fullName":"Bob","email":"bob@example.com"}
                """.formatted(PASSWORD))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.roles.length()").value(0));
    }

    @Test
    void rejectsInvalidCreate() throws Exception {
        postUser("""
                {"username":"Not Valid","password":"short","fullName":"","email":"not-an-email"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SYS-400"))
                .andExpect(jsonPath("$.details.length()").value(4));

        // 30 characters but 90 bytes: passes @Size, would be cut off by BCrypt
        postUser(createJson("multibyte", "multibyte@example.com", "[]").replace(PASSWORD, "ệ".repeat(30)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password must not exceed 72 bytes"));

        postUser(createJson("noroleid", "noroleid@example.com", "[999999999]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ROL-005"));
    }

    @Test
    void rejectsDuplicateUsernameOrEmail() throws Exception {
        createUser("alice", "alice@example.com");

        postUser(createJson("alice", "other@example.com", "[]"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("USR-011"));

        postUser(createJson("other", "ALICE@example.com", "[]"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("USR-002"));
    }

    @Test
    void findsUserById() throws Exception {
        Long id = createUser("alice", "alice@example.com");
        flushAndClear();

        mockMvc.perform(get("/api/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("alice"));

        mockMvc.perform(get("/api/users/{id}", 999999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("USR-001"));
    }

    @Test
    void pagesThroughUsersWithoutRepeatingOrSkipping() throws Exception {
        createUser("page-a", "page-a@example.com");
        createUser("page-b", "page-b@example.com");
        createUser("page-c", "page-c@example.com");
        flushAndClear();

        List<String> usernames = new ArrayList<>();
        String cursor = null;
        do {
            var request = get("/api/users").param("limit", "2");
            if (cursor != null) {
                request = request.param("cursor", cursor);
            }
            JsonNode page = readBody(mockMvc.perform(request).andExpect(status().isOk()));
            page.get("data").forEach(user -> usernames.add(user.get("username").asString()));
            cursor = page.get("meta").has("nextCursor") ? page.get("meta").get("nextCursor").asString() : null;
        } while (cursor != null);

        assertThat(usernames).doesNotHaveDuplicates().contains("page-a", "page-b", "page-c");
    }

    @Test
    void rejectsInvalidPagingInput() throws Exception {
        mockMvc.perform(get("/api/users").param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("limit"));

        mockMvc.perform(get("/api/users").param("cursor", "not-a-cursor"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid cursor"));
    }

    @Test
    void updatesUserAndReplacesRolesButNotUsername() throws Exception {
        Long id = readBody(postUser(createJson("alice", "alice@example.com", "[" + seedRoleId("HR") + "]")))
                .get("data").get("id").asLong();
        flushAndClear();
        String hashBefore = storedHash(id);

        putUser(id, """
                {"username":"changed","fullName":"Alice Nguyen","email":"new@example.com",
                 "isActive":false,"roleIds":[%d]}
                """.formatted(seedRoleId("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("alice"))
                .andExpect(jsonPath("$.data.fullName").value("Alice Nguyen"))
                .andExpect(jsonPath("$.data.email").value("new@example.com"))
                .andExpect(jsonPath("$.data.isActive").value(false))
                .andExpect(jsonPath("$.data.roles.length()").value(1))
                .andExpect(jsonPath("$.data.roles[0].code").value("EMPLOYEE"));

        flushAndClear();
        assertThat(storedHash(id)).isEqualTo(hashBefore);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from iam_user_role where user_id = ?", Long.class, id)).isEqualTo(1);
    }

    @Test
    void changesPasswordOnlyWhenProvided() throws Exception {
        Long id = createUser("alice", "alice@example.com");

        putUser(id, """
                {"fullName":"Alice","email":"alice@example.com","isActive":true,"roleIds":[],
                 "password":"another-pass"}
                """)
                .andExpect(status().isOk());

        flushAndClear();
        assertThat(new BCryptPasswordEncoder().matches("another-pass", storedHash(id))).isTrue();
    }

    @Test
    void rejectsInvalidUpdate() throws Exception {
        Long id = createUser("alice", "alice@example.com");
        createUser("bob", "bob@example.com");

        putUser(id, """
                {"fullName":"Alice","email":"alice@example.com"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.length()").value(2));

        putUser(id, """
                {"fullName":"Alice","email":"bob@example.com","isActive":true,"roleIds":[]}
                """)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("USR-002"));

        // Keeping its own email is not a conflict
        putUser(id, """
                {"fullName":"Alice 2","email":"alice@example.com","isActive":true,"roleIds":[]}
                """)
                .andExpect(status().isOk());

        putUser(999999999L, """
                {"fullName":"Nobody","email":"nobody@example.com","isActive":true,"roleIds":[]}
                """)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("USR-001"));
    }

    @Test
    void deletesUserAndItsRoleLinksButNotTheRole() throws Exception {
        Long roleId = seedRoleId("HR");
        Long id = readBody(postUser(createJson("alice", "alice@example.com", "[" + roleId + "]")))
                .get("data").get("id").asLong();
        flushAndClear();

        mockMvc.perform(delete("/api/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User deleted"))
                .andExpect(jsonPath("$.data").doesNotExist());

        flushAndClear();
        mockMvc.perform(get("/api/users/{id}", id))
                .andExpect(status().isNotFound());
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from iam_user_role where user_id = ?", Long.class, id)).isZero();
        assertThat(seedRoleId("HR")).isEqualTo(roleId);

        mockMvc.perform(delete("/api/users/{id}", id))
                .andExpect(status().isNotFound());
    }

    private String createJson(String username, String email, String roleIds) {
        return """
                {"username":"%s","password":"%s","fullName":"Test User","email":"%s","roleIds":%s}
                """.formatted(username, PASSWORD, email, roleIds);
    }

    private Long createUser(String username, String email) throws Exception {
        return readBody(postUser(createJson(username, email, "[]")).andExpect(status().isCreated()))
                .get("data").get("id").asLong();
    }

    private ResultActions postUser(String json) throws Exception {
        return mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions putUser(Long id, String json) throws Exception {
        return mockMvc.perform(put("/api/users/{id}", id).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private JsonNode readBody(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }

    // Sends pending SQL so constraints run and JdbcTemplate sees it; clearing forces later reads to hit the DB
    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private String storedHash(Long id) {
        return jdbcTemplate.queryForObject("select password_hash from iam_user where id = ?", String.class, id);
    }

    private Long seedRoleId(String code) {
        return jdbcTemplate.queryForObject("select id from iam_role where code = ?", Long.class, code);
    }
}
