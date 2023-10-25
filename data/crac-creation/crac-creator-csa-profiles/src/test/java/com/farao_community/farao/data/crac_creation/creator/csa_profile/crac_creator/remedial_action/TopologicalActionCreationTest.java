package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.remedial_action;

import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.network_action.TopologicalAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.CsaProfileCrac;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreator;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreatorTest;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.importer.CsaProfileCracImporter;
import com.farao_community.farao.data.crac_impl.OnContingencyStateImpl;
import com.google.common.base.Suppliers;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.ImportConfig;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Switch;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static com.farao_community.farao.data.crac_api.Instant.CURATIVE;
import static com.farao_community.farao.data.crac_api.Instant.PREVENTIVE;
import static com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreationTestUtil.assertNetworkActionImported;
import static com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreationTestUtil.assertRaNotImported;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TopologicalActionCreationTest {

    @Test
    void testTC2ImportNetworkActions() {
        Properties importParams = new Properties();
        Network network = Network.read(Paths.get(new File(CsaProfileCracCreatorTest.class.getResource("/csa-9/CSA_TestConfiguration_TC2_27Apr2023.zip").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);

        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/csa-9/CSA_TestConfiguration_TC2_27Apr2023.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);

        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();
        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-04-27T12:00Z"), new CracCreationParameters());

        assertNotNull(cracCreationContext);
        assertEquals(9, cracCreationContext.getCrac().getNetworkActions().size());
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
        Network network = Mockito.mock(Network.class);
        Mockito.when(network.getSwitch("switch")).thenReturn(Mockito.mock(Switch.class));
        Branch networkElementMock = Mockito.mock(Branch.class);
        Mockito.when(networkElementMock.getId()).thenReturn("equipment-with-contingency");
        Mockito.when(network.getIdentifiable("equipment-with-contingency")).thenReturn(networkElementMock);

        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/csa-9/CSA_9_4_ValidProfiles.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);

        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();
        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-03-29T12:00Z"), new CracCreationParameters());

        // RA1 (on instant)
        NetworkAction ra1 = cracCreationContext.getCrac().getNetworkAction("on-instant-preventive-topological-action-parent-remedial-action");
        assertEquals("RA1", ra1.getName());
        assertEquals(PREVENTIVE, ra1.getUsageRules().iterator().next().getInstant());
        assertEquals(UsageMethod.AVAILABLE, ra1.getUsageRules().iterator().next().getUsageMethod());

        // RA2 (on instant)
        NetworkAction ra2 = cracCreationContext.getCrac().getNetworkAction("on-instant-curative-topological-action-parent-remedial-action");
        assertEquals("RA2", ra2.getName());
        assertEquals(CURATIVE, ra2.getUsageRules().iterator().next().getInstant());
        assertEquals(UsageMethod.AVAILABLE, ra2.getUsageRules().iterator().next().getUsageMethod());

        // RA3 (on state)
        NetworkAction ra3 = cracCreationContext.getCrac().getNetworkAction("on-state-considered-curative-topological-action-parent-remedial-action");
        assertEquals("RA3", ra3.getName());
        assertEquals(CURATIVE, ra3.getUsageRules().iterator().next().getInstant());
        assertEquals(UsageMethod.AVAILABLE, ra3.getUsageRules().iterator().next().getUsageMethod());
        assertEquals("switch", ra3.getNetworkElements().iterator().next().getId());

        // RA4 (on state)
        NetworkAction ra4 = cracCreationContext.getCrac().getNetworkAction("on-state-included-curative-topological-action-parent-remedial-action");
        assertEquals("RA4", ra4.getName());
        assertEquals(CURATIVE, ra4.getUsageRules().iterator().next().getInstant());
        assertEquals(UsageMethod.FORCED, ra4.getUsageRules().iterator().next().getUsageMethod());

        // RTE_RA7 (on instant)
        NetworkAction ra7 = cracCreationContext.getCrac().getNetworkAction("topological-action-with-tso-name-parent-remedial-action");
        assertEquals("RTE_RA7", ra7.getName());
        assertEquals("RTE", ra7.getOperator());
        assertEquals(PREVENTIVE, ra7.getUsageRules().iterator().next().getInstant());
        assertEquals(UsageMethod.AVAILABLE, ra7.getUsageRules().iterator().next().getUsageMethod());

        // nameless-topological-action-with-speed-parent-remedial-action (on instant)
        NetworkAction raNameless = cracCreationContext.getCrac().getNetworkAction("nameless-topological-action-with-speed-parent-remedial-action");
        assertEquals("nameless-topological-action-with-speed-parent-remedial-action", raNameless.getName());
        assertEquals(PREVENTIVE, raNameless.getUsageRules().iterator().next().getInstant());
        assertEquals(UsageMethod.AVAILABLE, raNameless.getUsageRules().iterator().next().getUsageMethod());
        assertEquals(137, raNameless.getSpeed().get());

        // nameless-topological-action-with-tso-name-parent-remedial-action (on instant)
        NetworkAction raNameless2 = cracCreationContext.getCrac().getNetworkAction("nameless-topological-action-with-tso-name-parent-remedial-action");
        assertEquals("nameless-topological-action-with-tso-name-parent-remedial-action", raNameless2.getName());
        assertEquals("RTE", raNameless2.getOperator());
        assertEquals(PREVENTIVE, raNameless2.getUsageRules().iterator().next().getInstant());
        assertEquals(UsageMethod.AVAILABLE, raNameless2.getUsageRules().iterator().next().getUsageMethod());
    }

    @Test
    void testIgnoreWrongRAKeyword() {
        Properties importParams = new Properties();
        Network network = Network.read(Paths.get(new File(CsaProfileCracCreatorTest.class.getResource("/csa-9/CSA_9_5_WrongKeyword.zip").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);

        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/csa-9/CSA_9_5_WrongKeyword.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);

        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();
        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-03-29T12:00Z"), new CracCreationParameters());

        assertEquals(0, cracCreationContext.getCrac().getRemedialActions().size());
    }

    @Test
    void testIgnoreUnhandledProfile() {
        Properties importParams = new Properties();
        Network network = Network.read(Paths.get(new File(CsaProfileCracCreatorTest.class.getResource("/csa-9/CSA_9_6_NotYetValidProfile.zip").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);

        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/csa-9/CSA_9_6_NotYetValidProfile.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);

        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();
        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-03-29T12:00Z"), new CracCreationParameters());

        assertEquals(0, cracCreationContext.getCrac().getRemedialActions().size());
    }

    @Test
    void testIgnoreOutdatedProfile() {
        Properties importParams = new Properties();
        Network network = Network.read(Paths.get(new File(CsaProfileCracCreatorTest.class.getResource("/csa-9/CSA_9_7_OutdatedProfile.zip").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);

        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/csa-9/CSA_9_7_OutdatedProfile.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);

        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();
        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-03-29T12:00Z"), new CracCreationParameters());

        assertEquals(0, cracCreationContext.getCrac().getRemedialActions().size());
    }

    @Test
    void testIgnoreInvalidNetworkActions() {
        Properties importParams = new Properties();
        Network network = Network.read(Paths.get(new File(CsaProfileCracCreatorTest.class.getResource("/csa-9/CSA_9_8_InvalidRemedialActions.zip").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);

        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/csa-9/CSA_9_8_InvalidRemedialActions.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);

        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();
        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-03-29T12:00Z"), new CracCreationParameters());

        assertEquals(0, cracCreationContext.getCrac().getRemedialActions().size());
    }

    @Test
    void testIgnoreInvalidTopologicalActions() {
        Properties importParams = new Properties();
        Network network = Network.read(Paths.get(new File(CsaProfileCracCreatorTest.class.getResource("/csa-9/CSA_9_9_InvalidTopologicalActions.zip").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);

        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/csa-9/CSA_9_9_InvalidTopologicalActions.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);

        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();
        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-03-29T12:00Z"), new CracCreationParameters());

        assertEquals(0, cracCreationContext.getCrac().getRemedialActions().size());
    }

    @Test
    void testIgnoreInvalidContingenciesWithNetworkActions() {
        Properties importParams = new Properties();
        Network network = Network.read(Paths.get(new File(CsaProfileCracCreatorTest.class.getResource("/csa-9/CSA_9_10_InvalidContingenciesWithRemedialActions.zip").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);

        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/csa-9/CSA_9_10_InvalidContingenciesWithRemedialActions.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);

        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();
        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-03-29T12:00Z"), new CracCreationParameters());

        assertEquals(0, cracCreationContext.getCrac().getRemedialActions().size());
    }

    @Test
    void testImportRemedialActionWithMultipleContingencies() {
        Properties importParams = new Properties();
        Network network = Network.read(Paths.get(new File(CsaProfileCracCreatorTest.class.getResource("/CSA_MultipleContingenciesForTheSameRemedialAction.zip").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);

        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/CSA_MultipleContingenciesForTheSameRemedialAction.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);

        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();
        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-03-29T12:00Z"), new CracCreationParameters());

        assertEquals(1, cracCreationContext.getCrac().getRemedialActions().size());
        assertNetworkActionImported(cracCreationContext, "ra2", Set.of("BBE1AA1  BBE4AA1  1"), false, 2);
        assertRaNotImported(cracCreationContext, "ra1", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action ra1 will not be imported because the ElementCombinationConstraintKinds that link the remedial action to the contingency http://entsoe.eu/#_co1 are different");
    }
}
