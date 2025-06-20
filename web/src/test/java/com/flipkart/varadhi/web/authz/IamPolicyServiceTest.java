package com.flipkart.varadhi.web.authz;

import java.util.Map;
import java.util.Set;

import com.flipkart.varadhi.common.exceptions.ResourceNotFoundException;
import com.flipkart.varadhi.core.OrgService;
import com.flipkart.varadhi.core.ProjectService;
import com.flipkart.varadhi.core.TeamService;
import com.flipkart.varadhi.db.VaradhiMetaStore;
import com.flipkart.varadhi.db.ZKMetaStore;
import com.flipkart.varadhi.entities.Org;
import com.flipkart.varadhi.entities.Project;
import com.flipkart.varadhi.entities.ResourceType;
import com.flipkart.varadhi.entities.Team;
import com.flipkart.varadhi.entities.auth.IamPolicyRecord;
import com.flipkart.varadhi.entities.auth.IamPolicyRequest;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static com.flipkart.varadhi.web.authz.IamPolicyService.getAuthResourceFQN;
import static org.junit.jupiter.api.Assertions.*;

class IamPolicyServiceTest {
    TestingServer zkCuratorTestingServer;

    IamPolicyService iamPolicyService;

    OrgService orgService;
    TeamService teamService;
    ProjectService projectService;
    CuratorFramework zkCurator;
    VaradhiMetaStore varadhiMetaStore;

    Org org1;
    Org org2;
    Team org1team1;
    Team org2team1;
    Project proj1;
    Project proj2;


    @BeforeEach
    void PreTest() throws Exception {
        zkCuratorTestingServer = new TestingServer();
        zkCurator = CuratorFrameworkFactory.newClient(
            zkCuratorTestingServer.getConnectString(),
            new ExponentialBackoffRetry(1000, 1)
        );
        zkCurator.start();
        varadhiMetaStore = Mockito.spy(new VaradhiMetaStore(new ZKMetaStore(zkCurator)));
        orgService = new OrgService(varadhiMetaStore.orgs(), varadhiMetaStore.teams());
        teamService = new TeamService(varadhiMetaStore);
        projectService = new ProjectService(varadhiMetaStore);
        iamPolicyService = new IamPolicyService(varadhiMetaStore, varadhiMetaStore.iamPolicies());
        org1 = Org.of("org1");
        org2 = Org.of("org2");
        org1team1 = Team.of("team1", org1.getName());
        org2team1 = Team.of("team1", org2.getName());
        proj1 = Project.of("proj1", "", org1team1.getName(), org1.getName());
        proj2 = Project.of("proj2", "", org2team1.getName(), org2.getName());
    }

    @Test
    void testCreateIamPolicyRecord() {
        // node on org resource
        String resourceId = org1.getName();
        orgService.createOrg(org1);
        IamPolicyRequest request = new IamPolicyRequest("usr1", Set.of("role1", "role2"));
        IamPolicyRecord expect = new IamPolicyRecord(
            getAuthResourceFQN(ResourceType.ORG, resourceId),
            0,
            Map.of(request.getSubject(), request.getRoles())
        );
        IamPolicyRecord nodeCreated = iamPolicyService.setIamPolicy(ResourceType.ORG, resourceId, request);
        assertEquals(expect, nodeCreated);

        // org not exist
        var ior = assertThrowsExactly(
            IllegalArgumentException.class,
            () -> iamPolicyService.setIamPolicy(ResourceType.ORG, org2.getName(), request)
        );
        assertEquals(
            String.format("Invalid resource id(%s) for resource type(%s).", org2.getName(), ResourceType.ORG),
            ior.getMessage()
        );

        // node on team resource
        teamService.createTeam(org1team1);
        resourceId = org1.getName() + ":" + org1team1.getName();
        IamPolicyRequest request2 = new IamPolicyRequest("usr1", Set.of("role1", "role2"));
        expect = new IamPolicyRecord(
            getAuthResourceFQN(ResourceType.TEAM, resourceId),
            0,
            Map.of(request2.getSubject(), request2.getRoles())
        );
        nodeCreated = iamPolicyService.setIamPolicy(ResourceType.TEAM, resourceId, request);
        assertEquals(expect, nodeCreated);

        // wrong team name
        ior = assertThrowsExactly(
            IllegalArgumentException.class,
            () -> iamPolicyService.setIamPolicy(ResourceType.TEAM, org2team1.getName(), request2)
        );
        assertEquals(
            String.format("Invalid resource id(%s) for resource type(%s).", org2team1.getName(), ResourceType.TEAM),
            ior.getMessage()
        );

        // node on project resource
        projectService.createProject(proj1);
        resourceId = proj1.getName();
        IamPolicyRequest request3 = new IamPolicyRequest("usr1", Set.of("role1", "role2"));
        expect = new IamPolicyRecord(
            getAuthResourceFQN(ResourceType.PROJECT, resourceId),
            0,
            Map.of(request3.getSubject(), request3.getRoles())
        );
        nodeCreated = iamPolicyService.setIamPolicy(ResourceType.PROJECT, resourceId, request3);
        assertEquals(expect, nodeCreated);

        // wrong project name
        ior = assertThrowsExactly(
            IllegalArgumentException.class,
            () -> iamPolicyService.setIamPolicy(ResourceType.PROJECT, proj2.getName(), request3)
        );
        assertEquals(
            String.format("Invalid resource id(%s) for resource type(%s).", proj2.getName(), ResourceType.PROJECT),
            ior.getMessage()
        );
    }

    @Test
    void testGetIamPolicyRecord() {
        orgService.createOrg(org1);
        String resourceId = org1.getName();
        IamPolicyRequest request = new IamPolicyRequest("usr1", Set.of("role1", "role2"));
        IamPolicyRecord expect = new IamPolicyRecord(
            getAuthResourceFQN(ResourceType.ORG, resourceId),
            0,
            Map.of(request.getSubject(), request.getRoles())
        );
        iamPolicyService.setIamPolicy(ResourceType.ORG, resourceId, request);
        IamPolicyRecord get = iamPolicyService.getIamPolicy(ResourceType.ORG, resourceId);
        assertEquals(expect, get);

        teamService.createTeam(org1team1);
        resourceId = org1.getName() + ":" + org1team1.getName();
        expect = new IamPolicyRecord(
            getAuthResourceFQN(ResourceType.TEAM, resourceId),
            0,
            Map.of(request.getSubject(), request.getRoles())
        );
        iamPolicyService.setIamPolicy(ResourceType.TEAM, resourceId, request);
        get = iamPolicyService.getIamPolicy(ResourceType.TEAM, resourceId);
        assertEquals(expect, get);

        // non existent
        assertThrowsExactly(
            ResourceNotFoundException.class,
            () -> iamPolicyService.getIamPolicy(ResourceType.ORG, org2.getName())
        );
    }

    @Test
    void testUpdateNode() {
        orgService.createOrg(org1);
        String resourceId = org1.getName();
        IamPolicyRequest request = new IamPolicyRequest("usr1", Set.of("role1", "role2"));
        iamPolicyService.setIamPolicy(ResourceType.ORG, resourceId, request);

        // update node
        IamPolicyRecord expect = new IamPolicyRecord(
            getAuthResourceFQN(ResourceType.ORG, resourceId),
            1,
            Map.of("usr1", Set.of("role1"))
        );
        IamPolicyRequest update = new IamPolicyRequest("usr1", Set.of("role1"));
        IamPolicyRecord updated = iamPolicyService.setIamPolicy(ResourceType.ORG, resourceId, update);
        assertEquals(expect, updated);
    }

    @Test
    void testDeleteNode() {
        orgService.createOrg(org1);
        String resourceId = org1.getName();
        IamPolicyRequest request = new IamPolicyRequest("usr1", Set.of("role1", "role2"));
        iamPolicyService.setIamPolicy(ResourceType.ORG, resourceId, request);
        assertNotNull(iamPolicyService.getIamPolicy(ResourceType.ORG, org1.getName()));

        iamPolicyService.deleteIamPolicy(ResourceType.ORG, org1.getName());
        assertThrowsExactly(
            ResourceNotFoundException.class,
            () -> iamPolicyService.getIamPolicy(ResourceType.ORG, org1.getName())
        );

        // delete on non existing
        assertThrowsExactly(
            ResourceNotFoundException.class,
            () -> iamPolicyService.deleteIamPolicy(ResourceType.ORG, org2.getName())
        );
    }

    @Test
    void testSetIamPolicy() {
        // create and update new node
        orgService.createOrg(org1);
        IamPolicyRecord org1NodeExpected = new IamPolicyRecord(
            getAuthResourceFQN(ResourceType.ORG, org1.getName()),
            1,
            Map.of("user1", Set.of("role1", "role2"))
        );
        IamPolicyRequest org1Upd = new IamPolicyRequest("user1", Set.of("role1", "role2"));
        // since node is not created, should throw exception on get
        assertThrows(
            ResourceNotFoundException.class,
            () -> iamPolicyService.getIamPolicy(ResourceType.ORG, org1.getName())
        );

        // now we update the role assignment
        IamPolicyRecord gotNode = iamPolicyService.setIamPolicy(ResourceType.ORG, org1.getName(), org1Upd);
        assertEquals(org1NodeExpected, gotNode);

        // now we should be able to get the node
        assertDoesNotThrow(() -> iamPolicyService.getIamPolicy(ResourceType.ORG, org1.getName()));

        // update existing node
        org1NodeExpected.setRoleAssignment("user2", Set.of("role1", "role2"));
        org1NodeExpected.setVersion(2);
        IamPolicyRequest org1Upd2 = new IamPolicyRequest("user2", Set.of("role1", "role2"));
        gotNode = iamPolicyService.setIamPolicy(ResourceType.ORG, org1.getName(), org1Upd2);
        assertEquals(org1NodeExpected, gotNode);

        // update existing subject
        org1NodeExpected.setRoleAssignment("user1", Set.of("role3"));
        org1NodeExpected.setVersion(3);
        IamPolicyRequest org1Upd3 = new IamPolicyRequest("user1", Set.of("role3"));
        gotNode = iamPolicyService.setIamPolicy(ResourceType.ORG, org1.getName(), org1Upd3);
        assertEquals(org1NodeExpected, gotNode);

        // check delete subject
        org1NodeExpected.setRoleAssignment("user1", Set.of());
        org1NodeExpected.setVersion(4);
        IamPolicyRequest org1Upd4 = new IamPolicyRequest("user1", Set.of());
        gotNode = iamPolicyService.setIamPolicy(ResourceType.ORG, org1.getName(), org1Upd4);
        assertEquals(org1NodeExpected, gotNode);

        // new node on team resource
        teamService.createTeam(org1team1);
        String resourceId = org1.getName() + ":" + org1team1.getName();
        IamPolicyRecord org1team1NodeExpected = new IamPolicyRecord(
            getAuthResourceFQN(ResourceType.TEAM, resourceId),
            1,
            Map.of()
        );
        org1team1NodeExpected.setRoleAssignment("user1", Set.of("role1", "role2"));
        IamPolicyRequest org1team1Upd = new IamPolicyRequest("user1", Set.of("role1", "role2"));
        iamPolicyService.setIamPolicy(ResourceType.TEAM, resourceId, org1team1Upd);
        gotNode = iamPolicyService.setIamPolicy(ResourceType.TEAM, resourceId, org1team1Upd);
        assertEquals(org1team1NodeExpected, gotNode);

        // should not modify org node
        IamPolicyRecord org1Node = iamPolicyService.getIamPolicy(ResourceType.ORG, org1.getName());
        assertNotEquals(org1team1NodeExpected, org1Node);

        // try to update node with invalid resourceId
        IamPolicyRequest invalidUpd = new IamPolicyRequest("user1", Set.of("role1", "role2"));
        var ior = assertThrowsExactly(
            IllegalArgumentException.class,
            () -> iamPolicyService.setIamPolicy(ResourceType.ORG, "invalid", invalidUpd)
        );
        assertEquals(
            String.format("Invalid resource id(%s) for resource type(%s).", "invalid", ResourceType.ORG),
            ior.getMessage()
        );

        IamPolicyRequest incorrectUpdate = new IamPolicyRequest("user1", Set.of("role1", "role2"));
        ior = assertThrowsExactly(
            IllegalArgumentException.class,
            () -> iamPolicyService.setIamPolicy(ResourceType.TEAM, org1.getName(), incorrectUpdate)
        );
        assertEquals(
            String.format("Invalid resource id(%s) for resource type(%s).", org1.getName(), ResourceType.TEAM),
            ior.getMessage()
        );
    }
}
