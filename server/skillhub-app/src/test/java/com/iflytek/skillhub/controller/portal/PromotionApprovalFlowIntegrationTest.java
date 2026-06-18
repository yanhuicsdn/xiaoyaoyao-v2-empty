package com.iflytek.skillhub.controller.portal;

import com.iflytek.skillhub.SkillhubApplication;
import com.iflytek.skillhub.TestRedisConfig;
import com.iflytek.skillhub.auth.device.DeviceAuthService;
import com.iflytek.skillhub.auth.rbac.PlatformPrincipal;
import com.iflytek.skillhub.auth.rbac.RbacService;
import com.iflytek.skillhub.domain.governance.GovernanceNotificationService;
import com.iflytek.skillhub.domain.namespace.Namespace;
import com.iflytek.skillhub.domain.namespace.NamespaceType;
import com.iflytek.skillhub.domain.review.PromotionRequest;
import com.iflytek.skillhub.domain.review.ReviewTaskStatus;
import com.iflytek.skillhub.domain.skill.Skill;
import com.iflytek.skillhub.domain.skill.SkillVersion;
import com.iflytek.skillhub.domain.skill.SkillVersionStatus;
import com.iflytek.skillhub.domain.skill.SkillVisibility;
import com.iflytek.skillhub.domain.user.UserAccount;
import com.iflytek.skillhub.infra.jpa.NamespaceJpaRepository;
import com.iflytek.skillhub.infra.jpa.PromotionRequestJpaRepository;
import com.iflytek.skillhub.infra.jpa.SkillJpaRepository;
import com.iflytek.skillhub.infra.jpa.SkillVersionJpaRepository;
import com.iflytek.skillhub.infra.jpa.UserAccountJpaRepository;
import com.iflytek.skillhub.notification.service.NotificationDispatcher;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SkillhubApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
class PromotionApprovalFlowIntegrationTest {

    private static final String SUBMITTER_ID = "promotion-owner";
    private static final String REVIEWER_ID = "docker-admin";
    private static final String SELF_SUPER_ADMIN_ID = "self-super-admin";
    private static final String SELF_SKILL_ADMIN_ID = "self-skill-admin";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountJpaRepository userAccountRepository;

    @Autowired
    private NamespaceJpaRepository namespaceRepository;

    @Autowired
    private SkillJpaRepository skillRepository;

    @Autowired
    private SkillVersionJpaRepository skillVersionRepository;

    @Autowired
    private PromotionRequestJpaRepository promotionRequestRepository;

    @MockBean
    private DeviceAuthService deviceAuthService;

    @MockBean
    private RbacService rbacService;

    @MockBean
    private GovernanceNotificationService governanceNotificationService;

    @MockBean
    private NotificationDispatcher notificationDispatcher;

    @BeforeEach
    void setUp() {
        when(rbacService.getUserRoleCodes(REVIEWER_ID)).thenReturn(Set.of("SUPER_ADMIN"));
    }

    @Test
    void approvePromotion_persistsTargetSkillAndReturnsSuccessForBootstrapAdmin() throws Exception {
        PromotionGraph graph = createPromotionGraph();

        mockMvc.perform(post("/api/web/promotions/" + graph.request().getId() + "/approve")
                        .contentType("application/json")
                        .content("{\"comment\":\"ship it\"}")
                        .with(authentication(portalAuth(REVIEWER_ID, "SUPER_ADMIN")))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(graph.request().getId()))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.reviewedBy").value(REVIEWER_ID))
                .andExpect(jsonPath("$.data.reviewComment").value("ship it"));

        PromotionRequest savedRequest = promotionRequestRepository.findAllById(List.of(graph.request().getId()))
                .stream()
                .findFirst()
                .orElseThrow();
        assertThat(savedRequest.getStatus()).isEqualTo(ReviewTaskStatus.APPROVED);
        assertThat(savedRequest.getReviewedBy()).isEqualTo(REVIEWER_ID);
        assertThat(savedRequest.getTargetSkillId()).isNotNull();

        Skill targetSkill = skillRepository.findAllById(List.of(savedRequest.getTargetSkillId()))
                .stream()
                .findFirst()
                .orElseThrow();
        assertThat(targetSkill.getNamespaceId()).isEqualTo(graph.globalNamespace().getId());
        assertThat(targetSkill.getSlug()).isEqualTo(graph.sourceSkill().getSlug());
        assertThat(targetSkill.getSourceSkillId()).isEqualTo(graph.sourceSkill().getId());
        assertThat(targetSkill.getLatestVersionId()).isNotNull();

        List<SkillVersion> targetVersions = skillVersionRepository.findBySkillId(targetSkill.getId());
        assertThat(targetVersions).hasSize(1);
        assertThat(targetVersions.get(0).getStatus()).isEqualTo(SkillVersionStatus.PUBLISHED);
    }

    @Test
    void approvePromotion_allowsSuperAdminToApproveOwnPromotionThroughV1Route() throws Exception {
        when(rbacService.getUserRoleCodes(SELF_SUPER_ADMIN_ID)).thenReturn(Set.of("SUPER_ADMIN"));
        PromotionGraph graph = createPromotionGraph(SELF_SUPER_ADMIN_ID);

        mockMvc.perform(post("/api/v1/promotions/" + graph.request().getId() + "/approve")
                        .contentType("application/json")
                        .content("{\"comment\":\"self approve\"}")
                        .with(authentication(portalAuth(SELF_SUPER_ADMIN_ID, "SUPER_ADMIN")))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(graph.request().getId()))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.reviewedBy").value(SELF_SUPER_ADMIN_ID))
                .andExpect(jsonPath("$.data.submittedBy").value(SELF_SUPER_ADMIN_ID));

        PromotionRequest savedRequest = promotionRequestRepository.findAllById(List.of(graph.request().getId()))
                .stream()
                .findFirst()
                .orElseThrow();
        assertThat(savedRequest.getStatus()).isEqualTo(ReviewTaskStatus.APPROVED);
        assertThat(savedRequest.getReviewedBy()).isEqualTo(SELF_SUPER_ADMIN_ID);
        assertThat(savedRequest.getTargetSkillId()).isNotNull();
        verify(governanceNotificationService).notifyUser(
                eq(SELF_SUPER_ADMIN_ID),
                eq("PROMOTION"),
                eq("PROMOTION_REQUEST"),
                eq(graph.request().getId()),
                eq("Promotion approved"),
                any()
        );
    }

    @Test
    void approvePromotion_rejectsSkillAdminSelfApprovalThroughWebRoute() throws Exception {
        when(rbacService.getUserRoleCodes(SELF_SKILL_ADMIN_ID)).thenReturn(Set.of("SKILL_ADMIN"));
        PromotionGraph graph = createPromotionGraph(SELF_SKILL_ADMIN_ID);

        mockMvc.perform(post("/api/web/promotions/" + graph.request().getId() + "/approve")
                        .contentType("application/json")
                        .content("{\"comment\":\"self approve\"}")
                        .with(authentication(portalAuth(SELF_SKILL_ADMIN_ID, "SKILL_ADMIN")))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        PromotionRequest savedRequest = promotionRequestRepository.findAllById(List.of(graph.request().getId()))
                .stream()
                .findFirst()
                .orElseThrow();
        assertThat(savedRequest.getStatus()).isEqualTo(ReviewTaskStatus.PENDING);
        assertThat(savedRequest.getTargetSkillId()).isNull();
    }

    private PromotionGraph createPromotionGraph() {
        return createPromotionGraph(SUBMITTER_ID);
    }

    private PromotionGraph createPromotionGraph(String submitterId) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        saveUserIfAbsent(submitterId, "Promotion Owner", "owner-" + suffix + "@example.com");
        saveUserIfAbsent(REVIEWER_ID, "Admin", "admin-" + suffix + "@example.com");

        Namespace globalNamespace = new Namespace("global-" + suffix, "Global " + suffix, REVIEWER_ID);
        globalNamespace.setType(NamespaceType.GLOBAL);
        globalNamespace = namespaceRepository.saveAndFlush(globalNamespace);

        Namespace teamNamespace = new Namespace("team-" + suffix, "Team " + suffix, submitterId);
        teamNamespace = namespaceRepository.saveAndFlush(teamNamespace);

        Skill sourceSkill = new Skill(teamNamespace.getId(), "promote-skill-" + suffix, submitterId, SkillVisibility.PUBLIC);
        sourceSkill.setDisplayName("Promote Skill " + suffix);
        sourceSkill.setSummary("Used to verify promotion approval flow.");
        sourceSkill.setCreatedBy(submitterId);
        sourceSkill.setUpdatedBy(submitterId);
        sourceSkill = skillRepository.saveAndFlush(sourceSkill);

        SkillVersion sourceVersion = new SkillVersion(sourceSkill.getId(), "1.0.0", submitterId);
        sourceVersion.setStatus(SkillVersionStatus.PUBLISHED);
        sourceVersion.setPublishedAt(Instant.now());
        sourceVersion.setRequestedVisibility(SkillVisibility.PUBLIC);
        sourceVersion.setParsedMetadataJson("{\"name\":\"promotion-flow\"}");
        sourceVersion.setManifestJson("{\"version\":\"1.0.0\"}");
        sourceVersion = skillVersionRepository.saveAndFlush(sourceVersion);

        sourceSkill.setLatestVersionId(sourceVersion.getId());
        sourceSkill.setUpdatedBy(submitterId);
        sourceSkill = skillRepository.saveAndFlush(sourceSkill);

        PromotionRequest request = promotionRequestRepository.saveAndFlush(
                new PromotionRequest(sourceSkill.getId(), sourceVersion.getId(), globalNamespace.getId(), submitterId)
        );

        return new PromotionGraph(globalNamespace, sourceSkill, sourceVersion, request);
    }

    private void saveUserIfAbsent(String userId, String displayName, String email) {
        if (!userAccountRepository.existsById(userId)) {
            userAccountRepository.saveAndFlush(new UserAccount(userId, displayName, email, null));
        }
    }

    private UsernamePasswordAuthenticationToken portalAuth(String userId, String... roles) {
        PlatformPrincipal principal = new PlatformPrincipal(
                userId,
                userId,
                userId + "@example.com",
                "",
                "session",
                Set.of(roles)
        );
        List<SimpleGrantedAuthority> authorities = java.util.Arrays.stream(roles)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    private record PromotionGraph(Namespace globalNamespace,
                                  Skill sourceSkill,
                                  SkillVersion sourceVersion,
                                  PromotionRequest request) {
    }
}
