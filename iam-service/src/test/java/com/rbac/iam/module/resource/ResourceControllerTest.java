package com.rbac.iam.module.resource;

import jakarta.persistence.EntityManager;
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
class ResourceControllerTest {

    private static final String EMPLOYEE_SCREEN = "quan-ly-nhan-su/nhan-vien";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private EntityManager entityManager;

    @Test
    @WithAnonymousUser
    void rejectsAnonymousRequest() throws Exception {
        mockMvc.perform(get("/api/resources"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("SYS-401"));
    }

    @Test
    void pagesThroughResourcesWithoutRepeatingOrSkipping() throws Exception {
        List<String> codes = new ArrayList<>();
        String cursor = "";
        boolean hasMore = true;

        while (hasMore) {
            JsonNode page = readBody(mockMvc.perform(get("/api/resources").param("limit", "3").param("cursor", cursor))
                    .andExpect(status().isOk()));
            page.get("data").forEach(resource -> codes.add(resource.get("code").asString()));
            hasMore = page.get("meta").get("hasMore").asBoolean();
            cursor = hasMore ? page.get("meta").get("nextCursor").asString() : "";
        }

        assertThat(codes).doesNotHaveDuplicates().containsExactlyInAnyOrder(
                "quan-tri-he-thong", "danh-muc", "quan-tri-he-thong/nhom-nguoi-dung", "quan-tri-he-thong/phan-quyen",
                EMPLOYEE_SCREEN, "danh-muc/ki-nang", "dropdown");
    }

    @Test
    void returnsScopesParentAndDependencies() throws Exception {
        mockMvc.perform(get("/api/resources/{id}", seedId(EMPLOYEE_SCREEN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("MENU"))
                .andExpect(jsonPath("$.data.parentId").value(seedId("quan-tri-he-thong")))
                .andExpect(jsonPath("$.data.scopes.length()").value(4))
                .andExpect(jsonPath("$.data.dependsOnIds[0]").value(seedId("dropdown")));

        mockMvc.perform(get("/api/resources/{id}", 999999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RES-001"));
    }

    @Test
    void createsMenuUnderGroup() throws Exception {
        String json = """
                {"code":"danh-muc/chuc-vu","name":"Position","type":"MENU","parentId":%d,
                 "path":"/danh-muc/chuc-vu","scopes":["VIEW","CREATE"],"dependsOnIds":[%d]}
                """.formatted(seedId("danh-muc"), seedId("dropdown"));

        postResource(json)
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.message").value("Resource created"))
                .andExpect(jsonPath("$.data.isVisible").value(true))
                .andExpect(jsonPath("$.data.sortOrder").value(999))
                .andExpect(jsonPath("$.data.scopes[0]").value("CREATE"))
                .andExpect(jsonPath("$.data.scopes[1]").value("VIEW"));

        entityManager.flush();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from iam_resource where code = 'danh-muc/chuc-vu' and created_at is not null",
                Long.class)).isEqualTo(1);
    }

    @Test
    void hidesApiResourceByDefault() throws Exception {
        postResource("{\"code\":\"bao-cao-api\",\"name\":\"Report API\",\"type\":\"API\",\"scopes\":[\"VIEW\"]}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.isVisible").value(false));
    }

    @Test
    void rejectsBadCreateInput() throws Exception {
        postResource("{\"code\":\"dropdown\",\"name\":\"Duplicate\",\"type\":\"API\",\"scopes\":[\"VIEW\"]}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RES-002"));

        postResource("{\"code\":\"Bad Code\",\"name\":\"\",\"type\":\"GROUP\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SYS-400"))
                .andExpect(jsonPath("$.details.length()").value(2));

        postResource("{\"code\":\"x\",\"name\":\"Unknown type\",\"type\":\"PAGE\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is malformed"));

        postResource("{\"code\":\"x\",\"name\":\"X\",\"type\":\"GROUP\",\"parentId\":999999999}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("RES-003"));

        postResource("{\"code\":\"x\",\"name\":\"X\",\"type\":\"API\",\"scopes\":[\"VIEW\"],\"dependsOnIds\":[999999999]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("RES-003"));
    }

    @Test
    void enforcesRulesForEachType() throws Exception {
        String[] invalid = {
                // MENU needs a path
                "{\"code\":\"x\",\"name\":\"X\",\"type\":\"MENU\",\"scopes\":[\"VIEW\"]}",
                // MENU needs at least one scope
                "{\"code\":\"x\",\"name\":\"X\",\"type\":\"MENU\",\"path\":\"/x\"}",
                // GROUP cannot have scopes
                "{\"code\":\"x\",\"name\":\"X\",\"type\":\"GROUP\",\"scopes\":[\"VIEW\"]}",
                // GROUP cannot have a path
                "{\"code\":\"x\",\"name\":\"X\",\"type\":\"GROUP\",\"path\":\"/x\"}",
                // API cannot be visible
                "{\"code\":\"x\",\"name\":\"X\",\"type\":\"API\",\"scopes\":[\"VIEW\"],\"isVisible\":true}",
                // parent must be a GROUP
                "{\"code\":\"x\",\"name\":\"X\",\"type\":\"GROUP\",\"parentId\":" + seedId("danh-muc/ki-nang") + "}",
                // API cannot have a parent
                "{\"code\":\"x\",\"name\":\"X\",\"type\":\"API\",\"scopes\":[\"VIEW\"],\"parentId\":" + seedId("danh-muc") + "}",
                // A dependency needs VIEW on this side
                "{\"code\":\"x\",\"name\":\"X\",\"type\":\"API\",\"scopes\":[\"CREATE\"],\"dependsOnIds\":[" + seedId("dropdown") + "]}",
        };

        for (String json : invalid) {
            postResource(json)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("RES-004"));
        }
    }

    @Test
    void updatesFieldsButNotCodeOrType() throws Exception {
        Long id = createApi("cap-nhat-api", "VIEW");

        putResource(id, """
                {"code":"changed","type":"GROUP","name":"Renamed","icon":"star","sortOrder":5,"isVisible":false,
                 "scopes":["VIEW","UPDATE"],"dependsOnIds":[]}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("cap-nhat-api"))
                .andExpect(jsonPath("$.data.type").value("API"))
                .andExpect(jsonPath("$.data.name").value("Renamed"))
                .andExpect(jsonPath("$.data.icon").value("star"))
                .andExpect(jsonPath("$.data.sortOrder").value(5))
                .andExpect(jsonPath("$.data.scopes.length()").value(2));

        putResource(id, "{\"name\":\"Missing lists\",\"sortOrder\":5,\"isVisible\":false}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.length()").value(2));
    }

    @Test
    void updateKeepsPermissionsOfRetainedScopes() throws Exception {
        Long id = seedId(EMPLOYEE_SCREEN);
        Long before = countPermissions(id);
        assertThat(before).isPositive();

        putResource(id, """
                {"name":"Renamed","parentId":%d,"path":"/quan-ly-nhan-su/nhan-vien","sortOrder":3,"isVisible":true,
                 "scopes":["CREATE","VIEW","UPDATE","DELETE"],"dependsOnIds":[%d]}
                """.formatted(seedId("quan-tri-he-thong"), seedId("dropdown")))
                .andExpect(status().isOk());

        entityManager.flush();
        assertThat(countPermissions(id)).isEqualTo(before);
    }

    @Test
    void refusesToRemoveScopeGrantedToRole() throws Exception {
        // ADMIN holds every scope of the seed resources
        putResource(seedId(EMPLOYEE_SCREEN), """
                {"name":"X","parentId":%d,"path":"/x","sortOrder":3,"isVisible":true,
                 "scopes":["VIEW"],"dependsOnIds":[]}
                """.formatted(seedId("quan-tri-he-thong")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RES-006"));
    }

    @Test
    void removesUnusedScope() throws Exception {
        Long id = createApi("bo-scope-api", "VIEW\",\"UPDATE");

        putResource(id, "{\"name\":\"X\",\"sortOrder\":1,\"isVisible\":false,\"scopes\":[\"VIEW\"],\"dependsOnIds\":[]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scopes.length()").value(1));
    }

    @Test
    void refusesToRemoveViewWhileOthersDependOnIt() throws Exception {
        Long dependency = createApi("phu-thuoc-api", "VIEW\",\"UPDATE");
        postResource("{\"code\":\"can-api\",\"name\":\"Needs it\",\"type\":\"API\",\"scopes\":[\"VIEW\"],\"dependsOnIds\":[" + dependency + "]}")
                .andExpect(status().isCreated());

        putResource(dependency, "{\"name\":\"X\",\"sortOrder\":1,\"isVisible\":false,\"scopes\":[\"UPDATE\"],\"dependsOnIds\":[]}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RES-007"));

        mockMvc.perform(delete("/api/resources/{id}", dependency))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RES-007"));
    }

    @Test
    void refusesInvalidMovesAndSelfDependency() throws Exception {
        Long outer = createGroup("nhom-ngoai", null);
        Long inner = createGroup("nhom-trong", outer);

        for (Long newParent : List.of(outer, inner)) {
            putResource(outer, "{\"name\":\"X\",\"parentId\":" + newParent + ",\"sortOrder\":1,\"isVisible\":true,\"scopes\":[],\"dependsOnIds\":[]}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("RES-004"));
        }

        Long api = createApi("tu-phu-thuoc-api", "VIEW");
        putResource(api, "{\"name\":\"X\",\"sortOrder\":1,\"isVisible\":false,\"scopes\":[\"VIEW\"],\"dependsOnIds\":[" + api + "]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("RES-004"));
    }

    @Test
    void deletesUnusedResource() throws Exception {
        Long id = createApi("xoa-api", "VIEW");
        entityManager.flush();

        mockMvc.perform(delete("/api/resources/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Resource deleted"))
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(get("/api/resources/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void refusesToDeleteResourceInUse() throws Exception {
        mockMvc.perform(delete("/api/resources/{id}", seedId("danh-muc")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RES-005"));

        mockMvc.perform(delete("/api/resources/{id}", seedId("danh-muc/ki-nang")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RES-006"));
    }

    private Long createApi(String code, String scopes) throws Exception {
        String json = "{\"code\":\"" + code + "\",\"name\":\"Test\",\"type\":\"API\",\"scopes\":[\"" + scopes + "\"]}";
        return readBody(postResource(json).andExpect(status().isCreated())).get("data").get("id").asLong();
    }

    private Long createGroup(String code, Long parentId) throws Exception {
        String json = "{\"code\":\"" + code + "\",\"name\":\"Test\",\"type\":\"GROUP\",\"parentId\":" + parentId + "}";
        return readBody(postResource(json).andExpect(status().isCreated())).get("data").get("id").asLong();
    }

    private ResultActions postResource(String json) throws Exception {
        return mockMvc.perform(post("/api/resources").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions putResource(Long id, String json) throws Exception {
        return mockMvc.perform(put("/api/resources/{id}", id).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private JsonNode readBody(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }

    private Long seedId(String code) {
        return jdbcTemplate.queryForObject("select id from iam_resource where code = ?", Long.class, code);
    }

    private Long countPermissions(Long resourceId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from iam_role_permission where resource_id = ?", Long.class, resourceId);
    }
}
