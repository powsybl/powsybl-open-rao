package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.remedial_action;

import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.network_action.TopologicalAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreationContext;
import com.farao_community.farao.data.crac_impl.OnContingencyStateImpl;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.farao_community.farao.data.crac_api.Instant.CURATIVE;
import static com.farao_community.farao.data.crac_api.Instant.PREVENTIVE;
import static com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreationTestUtil.*;
import static org.junit.jupiter.api.Assertions.*;

class TopologicalActionCreationTest {

    @Test
    void testTC2ImportNetworkActions() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/CSA_TestConfiguration_TC2_27Apr2023.zip");

        assertNotNull(cracCreationContext);
        assertEquals(10, cracCreationContext.getCrac().getNetworkActions().size());
        // RA17 (on instant)
        NetworkAction ra17 = cracCreationContext.getCrac().getNetworkAction("cfabf356-c5e1-4391-b91b-3330bc24f0c9");
        assertEquals("RA17", ra17.getName());
        assertEquals("2db971f1-ed3d-4ea6-acf5-983c4289d51b", ra17.getNetworkElements().iterator().next().getId());
        assertEquals(ActionType.OPEN, ((TopologicalAction) ra17.getElementaryActions().iterator().next()).getActionType());
        assertEquals(PREVENTIVE, ra17.getUsageRules().iterator().next().getInstant());
        assertEquals(UsageMethod.AVAILABLE, ra17.getUsageRules().iterator().next().getUsageMethod());
        // RA11 (on instant)
        NetworkAction ra11 = cracCreationContext.getCrac().getNetworkAction("b2555ccc-6562-4887-8abc-19a6e51cfe36");
        assertEquals("RA11", ra11.getName());
        assertEquals("86dff3a9-afae-4122-afeb-651f2c01c795", ra11.getNetworkElements().iterator().next().getId());
        assertEquals(ActionType.OPEN, ((TopologicalAction) ra11.getElementaryActions().iterator().next()).getActionType());
        assertEquals(PREVENTIVE, ra11.getUsageRules().iterator().next().getInstant());
        assertEquals(UsageMethod.AVAILABLE, ra11.getUsageRules().iterator().next().getUsageMethod());
        // RA2 (on instant)
        NetworkAction ra2 = cracCreationContext.getCrac().getNetworkAction("d9bd3aaf-cda3-4b54-bb2e-b03dd9925817");
        assertEquals("RA2", ra2.getName());
        assertEquals(2, ra2.getNetworkElements().size());
        assertTrue(ra2.getElementaryActions().stream().allMatch(TopologicalAction.class::isInstance));
        List<TopologicalAction> topologicalActions = ra2.getElementaryActions().stream().map(TopologicalAction.class::cast).toList();
        assertTrue(topologicalActions.stream().anyMatch(action -> action.getNetworkElement().getId().equals("39428c75-098b-4366-861d-2df2a857a805")));
        assertTrue(topologicalActions.stream().anyMatch(action -> action.getNetworkElement().getId().equals("902046a4-40e9-421d-9ef1-9adab0d9d41d")));
        assertTrue(topologicalActions.stream().allMatch(action -> action.getActionType().equals(ActionType.OPEN)));
        assertEquals(PREVENTIVE, ra2.getUsageRules().iterator().next().getInstant());
        assertEquals(UsageMethod.AVAILABLE, ra2.getUsageRules().iterator().next().getUsageMethod());
        // RA13 (on state)
        NetworkAction ra13 = cracCreationContext.getCrac().getNetworkAction("1fd630a9-b9d8-414b-ac84-b47a093af936");
        assertEquals("RA13", ra13.getName());
        assertEquals(UsageMethod.FORCED, ra13.getUsageRules().iterator().next().getUsageMethod());
        assertEquals(CURATIVE, ra13.getUsageRules().iterator().next().getInstant());
        assertEquals("b6b780cb-9fe5-4c45-989d-447a927c3874", ((OnContingencyStateImpl) ra13.getUsageRules().iterator().next()).getContingency().getId());
        assertEquals("52effb0d-091b-4867-a0a2-387109cdad5c", ra13.getNetworkElements().iterator().next().getId());
        assertEquals(ActionType.OPEN, ((TopologicalAction) ra13.getElementaryActions().iterator().next()).getActionType());

        // RA22 (on state)
        NetworkAction ra22 = cracCreationContext.getCrac().getNetworkAction("d856a2a2-3de4-4a7b-aea4-d363c13d9014");
        assertEquals("RA22", ra22.getName());
        assertEquals(UsageMethod.FORCED, ra22.getUsageRules().iterator().next().getUsageMethod());
        assertEquals(CURATIVE, ra22.getUsageRules().iterator().next().getInstant());
        assertEquals("96c96ad8-844c-4f3b-8b38-c886ba2c0214", ((OnContingencyStateImpl) ra22.getUsageRules().iterator().next()).getContingency().getId());
        assertEquals("c871da6f-816f-4398-82a4-698550cbee58", ra22.getNetworkElements().iterator().next().getId());
        assertEquals(ActionType.OPEN, ((TopologicalAction) ra22.getElementaryActions().iterator().next()).getActionType());

        // RA14 (on state)
        NetworkAction ra14 = cracCreationContext.getCrac().getNetworkAction("c8bf6b19-1c3b-4ce6-a15c-99995a3c88ce");
        assertEquals("RA14", ra14.getName());
        assertEquals(UsageMethod.FORCED, ra14.getUsageRules().iterator().next().getUsageMethod());
        assertEquals(CURATIVE, ra14.getUsageRules().iterator().next().getInstant());
        assertEquals("13334fdf-9cc2-4341-adb6-1281269040b4", ((OnContingencyStateImpl) ra14.getUsageRules().iterator().next()).getContingency().getId());
        assertEquals("88e2e417-fc08-41a7-a711-4c6d0784ac4f", ra14.getNetworkElements().iterator().next().getId());
        assertEquals(ActionType.OPEN, ((TopologicalAction) ra14.getElementaryActions().iterator().next()).getActionType());

        // RA21 (on state)
        NetworkAction ra21 = cracCreationContext.getCrac().getNetworkAction("fb487cc2-0f7b-4958-8f66-1d3fabf7840d");
        assertEquals("RA21", ra21.getName());
        assertEquals(UsageMethod.FORCED, ra21.getUsageRules().iterator().next().getUsageMethod());
        assertEquals(CURATIVE, ra21.getUsageRules().iterator().next().getInstant());
        assertEquals("9d17b84c-33b5-4a68-b8b9-ed5b31038d40", ((OnContingencyStateImpl) ra21.getUsageRules().iterator().next()).getContingency().getId());
        assertEquals("65b97d2e-d749-41df-aa8f-0be4629d5e0e", ra21.getNetworkElements().iterator().next().getId());
        assertEquals(ActionType.OPEN, ((TopologicalAction) ra21.getElementaryActions().iterator().next()).getActionType());

        // RA3 (on state)
        NetworkAction ra3 = cracCreationContext.getCrac().getNetworkAction("5e401955-387e-45ce-b126-dd142b06b20c");
        assertEquals("RA3", ra3.getName());
        assertEquals(UsageMethod.FORCED, ra3.getUsageRules().iterator().next().getUsageMethod());
        assertEquals(CURATIVE, ra3.getUsageRules().iterator().next().getInstant());
        assertEquals("475ba18f-cbf5-490b-b65d-e8e03f9bcbc4", ((OnContingencyStateImpl) ra3.getUsageRules().iterator().next()).getContingency().getId());
        assertEquals("8e55fb9d-e514-4f4b-8a5d-8fd05b1dc02e", ra3.getNetworkElements().iterator().next().getId());
        assertEquals(ActionType.OPEN, ((TopologicalAction) ra3.getElementaryActions().iterator().next()).getActionType());

        // RA5 (on state)
        NetworkAction ra5 = cracCreationContext.getCrac().getNetworkAction("587cb391-ed16-4a1d-876e-f90241addce5");
        assertEquals("RA5", ra5.getName());
        assertEquals(UsageMethod.FORCED, ra5.getUsageRules().iterator().next().getUsageMethod());
        assertEquals(CURATIVE, ra5.getUsageRules().iterator().next().getInstant());
        assertEquals("5d587c7e-9ced-416a-ad17-6ef9b241a998", ((OnContingencyStateImpl) ra5.getUsageRules().iterator().next()).getContingency().getId());
        assertEquals("21f21596-302e-4e0e-8009-2b8c3c23517f", ra5.getNetworkElements().iterator().next().getId());
        assertEquals(ActionType.OPEN, ((TopologicalAction) ra5.getElementaryActions().iterator().next()).getActionType());
    }

    @Test
    void testImportNetworkActions() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/csa-9/CSA_9_4_ValidProfiles.zip");

        assertEquals(7, cracCreationContext.getCrac().getRemedialActions().size());

        // RA1 (on instant)
        assertTopologicalActionImported(cracCreationContext, "on-instant-preventive-topological-remedial-action", "RA1", "BBE1AA1  BBE4AA1  1");
        assertHasOnInstantUsageRule(cracCreationContext, "on-instant-preventive-topological-remedial-action", PREVENTIVE, UsageMethod.AVAILABLE);

        // RA2 (on instant)
        assertTopologicalActionImported(cracCreationContext, "on-instant-curative-topological-remedial-action", "RA2", "BBE1AA1  BBE4AA1  1");
        assertHasOnInstantUsageRule(cracCreationContext, "on-instant-curative-topological-remedial-action", CURATIVE, UsageMethod.AVAILABLE);

        // RA3 (on state)
        assertTopologicalActionImported(cracCreationContext, "on-contingency-state-considered-curative-topological-remedial-action", "RA3", "BBE1AA1  BBE4AA1  1");
        assertHasOnContingencyStateUsageRule(cracCreationContext, "on-contingency-state-considered-curative-topological-remedial-action", "bbda9fe0-77e0-4f8e-b9d9-4402a539f2b7", CURATIVE, UsageMethod.AVAILABLE);

        // RA4 (on state)
        assertTopologicalActionImported(cracCreationContext, "on-contingency-state-included-curative-topological-remedial-action", "RA4", "BBE1AA1  BBE4AA1  1");
        assertHasOnContingencyStateUsageRule(cracCreationContext, "on-contingency-state-included-curative-topological-remedial-action", "bbda9fe0-77e0-4f8e-b9d9-4402a539f2b7", CURATIVE, UsageMethod.FORCED);

        // nameless-topological-remedial-action-with-speed (on instant)
        assertTopologicalActionImported(cracCreationContext, "nameless-topological-remedial-action-with-speed", "nameless-topological-remedial-action-with-speed", "BBE1AA1  BBE4AA1  1", 137);

        // RTE_RA6 (on instant)
        assertTopologicalActionImported(cracCreationContext, "topological-remedial-action-with-tso-name", "RTE_RA6", "BBE1AA1  BBE4AA1  1");
        assertHasOnInstantUsageRule(cracCreationContext, "topological-remedial-action-with-tso-name", PREVENTIVE, UsageMethod.AVAILABLE);

        // nameless-topological-remedial-action-with-tso-name-parent (on instant)
        assertTopologicalActionImported(cracCreationContext, "nameless-topological-remedial-action-with-tso-name-parent", "nameless-topological-remedial-action-with-tso-name-parent", "BBE1AA1  BBE4AA1  1");
        assertHasOnInstantUsageRule(cracCreationContext, "nameless-topological-remedial-action-with-tso-name-parent", PREVENTIVE, UsageMethod.AVAILABLE);
    }

    @Test
    void testIgnoreInvalidTopologicalActions() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/csa-9/CSA_9_5_InvalidProfiles.zip");

        assertEquals(0, cracCreationContext.getCrac().getRemedialActions().size());

        assertRaNotImported(cracCreationContext, "unavailable-topological-remedial-action", ImportStatus.NOT_FOR_RAO, "Remedial action unavailable-topological-remedial-action will not be imported because RemedialAction.normalAvailable must be 'true' to be imported");
        assertRaNotImported(cracCreationContext, "undefined-topological-remedial-action", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action undefined-topological-remedial-action will not be imported because there is no topology actions, no Set point actions, nor tap position action linked to that RA");
        assertRaNotImported(cracCreationContext, "topological-remedial-action-with-not-existing-switch", ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, "Remedial action topological-remedial-action-with-not-existing-switch will not be imported because network model does not contain a switch with id: unknown-switch");
        assertRaNotImported(cracCreationContext, "topological-remedial-action-with-wrong-property-reference", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action 'topological-remedial-action-with-wrong-property-reference' will not be imported because 'TopologyAction' must have a property reference with 'http://energy.referencedata.eu/PropertyReference/Switch.open' value, but it was: 'http://energy.referencedata.eu/PropertyReference/RotatingMachine.p'");
        assertRaNotImported(cracCreationContext, "preventive-topological-remedial-action-with-contingency", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action preventive-topological-remedial-action-with-contingency will not be imported because it is linked to a contingency but it's kind is not curative");
    }

    @Test
    void testImportRemedialActionWithMultipleContingencies() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/CSA_MultipleContingenciesForTheSameRemedialAction.zip");

        assertEquals(1, cracCreationContext.getCrac().getRemedialActions().size());
        assertNetworkActionImported(cracCreationContext, "ra2", Set.of("BBE1AA1  BBE4AA1  1"), false, 2);
        assertRaNotImported(cracCreationContext, "ra1", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action ra1 will not be imported because the ElementCombinationConstraintKinds that link the remedial action to the contingency http://entsoe.eu/#_co1 are different");
    }

    @Test
    void testTopologicalActionOpenClose() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/CSA_74_TopoOpenClose.zip");
        assertNotNull(cracCreationContext);
        assertEquals(1, cracCreationContext.getCrac().getRemedialActions().size());
        assertNetworkActionImported(cracCreationContext, "topology-action", Set.of("BBE1AA1  BBE4AA1  1", "DDE3AA1  DDE4AA1  1"), false, 1);
        cracCreationContext.getCrac().getNetworkAction("topology-action").getElementaryActions();
        Iterator it = cracCreationContext.getCrac().getNetworkAction("topology-action").getElementaryActions().iterator();
        TopologicalAction ta1 = (TopologicalAction) it.next();
        TopologicalAction ta2 = (TopologicalAction) it.next();
        if ("BBE1AA1  BBE4AA1  1".equals(ta1.getNetworkElement().getName())) {
            assertEquals(ActionType.OPEN, ta1.getActionType());
            assertEquals(ActionType.CLOSE, ta2.getActionType());
        } else {
            assertEquals(ActionType.OPEN, ta2.getActionType());
            assertEquals(ActionType.CLOSE, ta1.getActionType());
        }
        assertRaNotImported(cracCreationContext, "no-static-property-range", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action no-static-property-range will not be imported because there is no StaticPropertyRange linked to that RA");
        assertRaNotImported(cracCreationContext, "wrong-value-offset-kind", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action wrong-value-offset-kind will not be imported because the ValueOffsetKind is http://entsoe.eu/ns/nc#ValueOffsetKind.incremental but should be none.");
        assertRaNotImported(cracCreationContext, "wrong-direction", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action wrong-direction will not be imported because the RelativeDirectionKind is http://entsoe.eu/ns/nc#RelativeDirectionKind.up but should be absolute.");
        assertRaNotImported(cracCreationContext, "undefined-action-type", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action undefined-action-type will not be imported because the normalValue is 2 which does not define a proper action type (open 1 / close 0)");
    }
}
