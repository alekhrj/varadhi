package com.flipkart.varadhi.web;

import com.flipkart.varadhi.common.exceptions.InvalidConfigException;
import com.flipkart.varadhi.common.utils.YamlLoader;
import com.flipkart.varadhi.db.VaradhiMetaStore;
import com.flipkart.varadhi.db.ZKMetaStore;
import com.flipkart.varadhi.entities.Org;
import com.flipkart.varadhi.entities.Project;
import com.flipkart.varadhi.entities.Team;
import com.flipkart.varadhi.core.OrgService;
import com.flipkart.varadhi.core.ProjectService;
import com.flipkart.varadhi.core.TeamService;
import com.flipkart.varadhi.spi.db.MetaStore;
import com.flipkart.varadhi.spi.db.MetaStoreProvider;
import com.flipkart.varadhi.spi.services.MessagingStackProvider;
import com.flipkart.varadhi.spi.services.ProducerFactory;
import com.flipkart.varadhi.web.config.WebConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Vertx;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class LeanDeploymentValidatorTest {

    private static final String TEST_ORG = "testOrg";
    private static final String TEST_TEAM = "testTeam";
    private static final String TEST_PROJECT = "testProject";
    TestingServer zkCuratorTestingServer;
    CuratorFramework zkCurator;
    MetaStore varadhiMetaStore;
    LeanDeploymentValidator deploymentValidator;
    MessagingStackProvider messagingStackProvider;
    MetaStoreProvider metaStoreProvider;
    WebConfiguration appConfiguration;
    MeterRegistry meterRegistry;
    Vertx vertx;
    private OrgService orgService;
    private TeamService teamService;
    private ProjectService projectService;

    @BeforeEach
    public void setup() throws Exception {
        vertx = Vertx.vertx();
        zkCuratorTestingServer = new TestingServer();
        zkCurator = spy(
            CuratorFrameworkFactory.newClient(
                zkCuratorTestingServer.getConnectString(),
                new ExponentialBackoffRetry(1000, 1)
            )
        );
        zkCurator.start();
        varadhiMetaStore = new VaradhiMetaStore(new ZKMetaStore(zkCurator));

        messagingStackProvider = mock(MessagingStackProvider.class);
        metaStoreProvider = mock(MetaStoreProvider.class);
        meterRegistry = mock(MeterRegistry.class);

        when(metaStoreProvider.getMetaStore()).thenReturn(varadhiMetaStore);
        when(messagingStackProvider.getProducerFactory()).thenReturn(mock(ProducerFactory.class));

        appConfiguration = YamlLoader.loadConfig("test/configuration.yml", WebConfiguration.class);


        orgService = new OrgService(varadhiMetaStore.orgs(), varadhiMetaStore.teams());
        teamService = new TeamService(varadhiMetaStore);
        projectService = new ProjectService(varadhiMetaStore);

        deploymentValidator = new LeanDeploymentValidator(orgService, teamService, projectService);
    }

    @AfterEach
    public void tearDown() throws Exception {
        zkCurator.close();
        zkCuratorTestingServer.close();
    }

    @Test
    public void testNoEntitiesPresent_Success() {
        deploymentValidator.validate(appConfiguration.getFeatureFlags());
        Org org = orgService.getOrg(appConfiguration.getFeatureFlags().getDefaultOrg());
        assertEquals(appConfiguration.getFeatureFlags().getDefaultOrg(), org.getName());

        Team team = teamService.getTeam(appConfiguration.getFeatureFlags().getDefaultTeam(), org.getName());
        assertEquals(appConfiguration.getFeatureFlags().getDefaultTeam(), team.getName());

        Project project = projectService.getProject(appConfiguration.getFeatureFlags().getDefaultProject());
        assertEquals(appConfiguration.getFeatureFlags().getDefaultProject(), project.getName());
    }

    @Test
    public void testEntitiesPresentWithDefaultName_Success() {
        Org org = Org.of(appConfiguration.getFeatureFlags().getDefaultOrg());
        orgService.createOrg(org);
        Team team = Team.of(appConfiguration.getFeatureFlags().getDefaultTeam(), org.getName());
        teamService.createTeam(team);
        Project project = Project.of(
            appConfiguration.getFeatureFlags().getDefaultProject(),

            "",
            team.getName(),
            org.getName()
        );
        projectService.createProject(project);

        deploymentValidator.validate(appConfiguration.getFeatureFlags());
        Org orgObtained = orgService.getOrg(appConfiguration.getFeatureFlags().getDefaultOrg());
        assertEquals(appConfiguration.getFeatureFlags().getDefaultOrg(), orgObtained.getName());

        Team teamObtained = teamService.getTeam(appConfiguration.getFeatureFlags().getDefaultTeam(), org.getName());
        assertEquals(appConfiguration.getFeatureFlags().getDefaultTeam(), teamObtained.getName());

        Project pObtained = projectService.getProject(appConfiguration.getFeatureFlags().getDefaultProject());
        assertEquals(appConfiguration.getFeatureFlags().getDefaultProject(), pObtained.getName());
    }

    @Test
    public void testDifferentOrgPresent_Failure() {
        Org org = Org.of(TEST_ORG);
        orgService.createOrg(org);
        validateDeployment(
            String.format("Lean deployment can not be enabled as org with %s name is present.", TEST_ORG)
        );
    }

    @Test
    public void testMultipleOrgPresent_Failure() {
        Org org1 = Org.of(TEST_ORG);
        Org org2 = Org.of(TEST_ORG + "2");
        orgService.createOrg(org1);
        orgService.createOrg(org2);
        validateDeployment("Lean deployment can not be enabled as there are more than one orgs.");
    }

    @Test
    public void testDifferentTeamPresent_Failure() {
        Org org = Org.of(appConfiguration.getFeatureFlags().getDefaultOrg());
        orgService.createOrg(org);
        Team team = Team.of(TEST_TEAM, org.getName());
        teamService.createTeam(team);
        validateDeployment(
            String.format("Lean deployment can not be enabled as team with %s name is present.", TEST_TEAM)
        );
    }

    @Test
    public void testMultipleTeamsPresent_Failure() {
        Org org = Org.of(appConfiguration.getFeatureFlags().getDefaultOrg());
        orgService.createOrg(org);
        Team team1 = Team.of(TEST_TEAM, org.getName());
        Team team2 = Team.of(TEST_TEAM + "2", org.getName());
        teamService.createTeam(team1);
        teamService.createTeam(team2);
        validateDeployment("Lean deployment can not be enabled as there are more than one teams.");
    }

    @Test
    public void testDifferentProjectPresent_Failure() {
        Org org = Org.of(appConfiguration.getFeatureFlags().getDefaultOrg());
        orgService.createOrg(org);
        Team team = Team.of(appConfiguration.getFeatureFlags().getDefaultTeam(), org.getName());
        teamService.createTeam(team);
        Project project = Project.of(TEST_PROJECT, "", team.getName(), org.getName());
        projectService.createProject(project);
        validateDeployment(
            String.format("Lean deployment can not be enabled as project with %s name is present.", TEST_PROJECT)
        );
    }

    @Test
    public void testMultipleProjectsPresent_Failure() {
        Org org = Org.of(appConfiguration.getFeatureFlags().getDefaultOrg());
        orgService.createOrg(org);
        Team team = Team.of(appConfiguration.getFeatureFlags().getDefaultTeam(), org.getName());
        teamService.createTeam(team);
        Project project1 = Project.of(TEST_PROJECT, "", team.getName(), org.getName());
        Project project2 = Project.of(TEST_PROJECT + "2", "", team.getName(), org.getName());
        projectService.createProject(project1);
        projectService.createProject(project2);
        validateDeployment("Lean deployment can not be enabled as there are more than one projects.");
    }

    private void validateDeployment(String failureMsg) {
        InvalidConfigException ie = assertThrows(
            InvalidConfigException.class,
            () -> deploymentValidator.validate(appConfiguration.getFeatureFlags())
        );
        assertEquals(failureMsg, ie.getMessage());
    }
}
