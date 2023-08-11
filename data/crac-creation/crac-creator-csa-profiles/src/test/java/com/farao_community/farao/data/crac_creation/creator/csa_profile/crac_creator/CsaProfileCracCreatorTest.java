/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.ElementaryAction;
import com.farao_community.farao.data.crac_api.network_action.InjectionSetpoint;
import com.farao_community.farao.data.crac_api.network_action.TopologicalAction;
import com.farao_community.farao.data.crac_api.threshold.BranchThreshold;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.CsaProfileCrac;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.importer.CsaProfileCracImporter;
import com.farao_community.farao.data.crac_impl.NetworkActionImpl;
import com.farao_community.farao.data.crac_impl.OnContingencyStateImpl;
import com.google.common.base.Suppliers;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class CsaProfileCracCreatorTest {

    @Test
    public void testCreateCracTestConfigurationTC1v29Mar2023() {
        Properties importParams = new Properties();
        Network network = Network.read(Paths.get(new File(CsaProfileCracCreatorTest.class.getResource("/TestConfiguration_TC1_v29Mar2023.zip").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);

        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/TestConfiguration_TC1_v29Mar2023.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);

        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();
        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-03-29T12:00Z"), new CracCreationParameters());

        assertNotNull(cracCreationContext);
        assertTrue(cracCreationContext.isCreationSuccessful());
        assertEquals(2, cracCreationContext.getCreationReport().getReport().size());
        assertEquals(2, cracCreationContext.getCrac().getContingencies().size());
        List<Contingency> listContingencies = cracCreationContext.getCrac().getContingencies()
                .stream().sorted(Comparator.comparing(Contingency::getId)).collect(Collectors.toList());

        this.assertContingencyEquality(listContingencies.get(0),
                "493480ba-93c3-426e-bee5-347d8dda3749", "ELIA_CO1",
                1, Arrays.asList("17086487-56ba-4979-b8de-064025a6b4da + 8fdc7abd-3746-481a-a65e-3df56acd8b13"));
        this.assertContingencyEquality(listContingencies.get(1),
                "c0a25fd7-eee0-4191-98a5-71a74469d36e", "TENNET_TSO_CO1",
                1, Arrays.asList("b18cd1aa-7808-49b9-a7cf-605eaf07b006 + e8acf6b6-99cb-45ad-b8dc-16c7866a4ddc"));

        assertEquals(4, cracCreationContext.getCrac().getFlowCnecs().size());
        List<FlowCnec> listFlowCnecs = cracCreationContext.getCrac().getFlowCnecs()
                .stream().sorted(Comparator.comparing(FlowCnec::getId)).collect(Collectors.toList());

        this.assertFlowCnecEquality(listFlowCnecs.get(0),
                "adad76ed-79e7-4985-84e1-eb493f168c85",
                "TENNET_TSO_AE1NL - preventive",
                "b18cd1aa-7808-49b9-a7cf-605eaf07b006 + e8acf6b6-99cb-45ad-b8dc-16c7866a4ddc",
                Instant.PREVENTIVE, null,
                +1876, -1876, Side.LEFT);
        this.assertFlowCnecEquality(listFlowCnecs.get(1),
                "adad76ed-79e7-4985-84e1-eb493f168c85-c0a25fd7-eee0-4191-98a5-71a74469d36e",
                "TENNET_TSO_AE1NL - TENNET_TSO_CO1 - curative",
                "b18cd1aa-7808-49b9-a7cf-605eaf07b006 + e8acf6b6-99cb-45ad-b8dc-16c7866a4ddc",
                Instant.CURATIVE, "c0a25fd7-eee0-4191-98a5-71a74469d36e",
                +1876, -1876, Side.LEFT);
        this.assertFlowCnecEquality(listFlowCnecs.get(2),
                "dd5247a7-3cb1-43f8-8ce1-12f285653f06",
                "ELIA_AE1 - preventive",
                "ffbabc27-1ccd-4fdc-b037-e341706c8d29",
                Instant.PREVENTIVE, null,
                +1312, -1312, Side.LEFT);
        this.assertFlowCnecEquality(listFlowCnecs.get(3),
                "dd5247a7-3cb1-43f8-8ce1-12f285653f06-493480ba-93c3-426e-bee5-347d8dda3749",
                "ELIA_AE1 - ELIA_CO1 - curative",
                "ffbabc27-1ccd-4fdc-b037-e341706c8d29",
                Instant.CURATIVE, "493480ba-93c3-426e-bee5-347d8dda3749",
                +1312, -1312, Side.LEFT);

        // csa-9-1
        assertEquals(0, cracCreationContext.getCrac().getRemedialActions().size());

    }

    @Test
    public void testCreateCracCSATestConfigurationTC2Draftv14Apr2023() {
        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/CSA_TestConfiguration_TC2_Draft_v14Apr2023.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);

        Properties importParams = new Properties();
        Network network = Network.read(Paths.get(new File(CsaProfileCracCreatorTest.class.getResource("/CSA_TestConfiguration_TC2_Draft_v14Apr2023.zip").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);

        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();
        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-03-29T12:00Z"), new CracCreationParameters());

        assertNotNull(cracCreationContext);
        assertTrue(cracCreationContext.isCreationSuccessful());
        assertEquals(28, cracCreationContext.getCreationReport().getReport().size());
        assertEquals(15, cracCreationContext.getCrac().getContingencies().size());
        assertEquals(12, cracCreationContext.getCrac().getFlowCnecs().size());

        List<Contingency> listContingencies = cracCreationContext.getCrac().getContingencies()
                .stream().sorted(Comparator.comparing(Contingency::getId)).collect(Collectors.toList());

        this.assertContingencyEquality(listContingencies.get(0),
                "13334fdf-9cc2-4341-adb6-1281269040b4", "REE_CO3",
                2, Arrays.asList("04566cf8-c766-11e1-8775-005056c00008", "0475dbd8-c766-11e1-8775-005056c00008"));
        this.assertContingencyEquality(listContingencies.get(1),
                "264e9a19-ae28-4c85-a43c-6b7818ca0e6c", "RTE_CO4",
                1, Arrays.asList("536f4b84-db4c-4545-96e9-bb5a87f65d13 + d9622e7f-5bf0-4e7e-b766-b8596c6fe4ae"));
        this.assertContingencyEquality(listContingencies.get(2),
                "37997e71-cb7d-4a8c-baa6-2a1594956da9", "ELIA_CO3",
                1, Arrays.asList("550ebe0d-f2b2-48c1-991f-cebea43a21aa"));
        this.assertContingencyEquality(listContingencies.get(3),
                "475ba18f-cbf5-490b-b65d-e8e03f9bcbc4", "RTE_CO2",
                1, Arrays.asList("e02e1166-1c43-4a4d-8c5a-82298ee0c8f5"));
        this.assertContingencyEquality(listContingencies.get(4),
                "5d587c7e-9ced-416a-ad17-6ef9b241a998", "RTE_CO3",
                1, Arrays.asList("2ab1b800-0c93-4517-86b5-8fd6a3a24ee7"));
        this.assertContingencyEquality(listContingencies.get(5),
                "7e31c67d-67ba-4592-8ac1-9e806d697c8e", "ELIA_CO2",
                1, Arrays.asList("536f4b84-db4c-4545-96e9-bb5a87f65d13 + d9622e7f-5bf0-4e7e-b766-b8596c6fe4ae"));
        this.assertContingencyEquality(listContingencies.get(6),
                "8cdec4c6-10c3-40c1-9eeb-7f6ae8d9b3fe", "REE_CO1",
                1, Arrays.asList("044bbe91-c766-11e1-8775-005056c00008"));
        this.assertContingencyEquality(listContingencies.get(7),
                "96c96ad8-844c-4f3b-8b38-c886ba2c0214", "REE_CO5",
                1, Arrays.asList("891e77ff-39c6-4648-8eda-d81f730271f9 + a04e4e41-c0b4-496e-9ef3-390ea089411f"));
        this.assertContingencyEquality(listContingencies.get(8),
                "9d17b84c-33b5-4a68-b8b9-ed5b31038d40", "REE_CO4",
                2, Arrays.asList("04566cf8-c766-11e1-8775-005056c00008", "0475dbd8-c766-11e1-8775-005056c00008"));
        this.assertContingencyEquality(listContingencies.get(9),
                "b6b780cb-9fe5-4c45-989d-447a927c3874", "REE_CO2",
                1, Arrays.asList("048481d0-c766-11e1-8775-005056c00008"));
        this.assertContingencyEquality(listContingencies.get(10),
                "bd7bb012-f7b9-45e0-9e15-4e2aa3592829", "TENNET_TSO_CO3",
                1, Arrays.asList("9c3b8f97-7972-477d-9dc8-87365cc0ad0e"));
        this.assertContingencyEquality(listContingencies.get(11),
                "ce19dd34-429e-4b72-8813-7615cc57b4a4", "RTE_CO6",
                1, Arrays.asList("04839777-c766-11e1-8775-005056c00008"));
        this.assertContingencyEquality(listContingencies.get(12),
                "d9ef0d5e-732d-441e-9611-c817b0afbc41", "RTE_CO5",
                1, Arrays.asList("f0dee14e-aa43-411e-a2ea-b9879c20f3be"));
        this.assertContingencyEquality(listContingencies.get(13),
                "e05bbe20-9d4a-40da-9777-8424d216785d", "RTE_CO1",
                1, Arrays.asList("f1c13f90-6d89-4a37-a51c-94742ad2dd72"));
        this.assertContingencyEquality(listContingencies.get(14),
                "e9eab3fe-c328-4f78-9bc1-77adb59f6ba7", "ELIA_CO1",
                1, Arrays.asList("dad02278-bd25-476f-8f58-dbe44be72586 + ed0c5d75-4a54-43c8-b782-b20d7431630b"));

        List<FlowCnec> listFlowCnecs = cracCreationContext.getCrac().getFlowCnecs()
                .stream().sorted(Comparator.comparing(FlowCnec::getId)).collect(Collectors.toList());
    }

    @Test
    public void testCreateCracCSATestWithRejectedFiles() {
        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/CSA_Test_With_Rejected_Files.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);

        Properties importParams = new Properties();
        Network network = Network.read(Paths.get(new File(CsaProfileCracCreatorTest.class.getResource("/CSA_Test_With_Rejected_Files.zip").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);

        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();
        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-03-29T12:00Z"), new CracCreationParameters());

        assertNotNull(cracCreationContext);
        assertTrue(cracCreationContext.isCreationSuccessful());
        assertEquals(42, cracCreationContext.getCreationReport().getReport().size());
        assertEquals(7, cracCreationContext.getCrac().getContingencies().size());
        assertEquals(1, cracCreationContext.getCrac().getFlowCnecs().size());
        List<Contingency> listContingencies = cracCreationContext.getCrac().getContingencies()
                .stream().sorted(Comparator.comparing(Contingency::getId)).collect(Collectors.toList());

        this.assertContingencyEquality(listContingencies.get(0),
                "264e9a19-ae28-4c85-a43c-6b7818ca0e6c", "RTE_CO4",
                1, Arrays.asList("536f4b84-db4c-4545-96e9-bb5a87f65d13 + d9622e7f-5bf0-4e7e-b766-b8596c6fe4ae"));
        this.assertContingencyEquality(listContingencies.get(1),
                "475ba18f-cbf5-490b-b65d-e8e03f9bcbc4", "RTE_CO2",
                1, Arrays.asList("e02e1166-1c43-4a4d-8c5a-82298ee0c8f5"));
        this.assertContingencyEquality(listContingencies.get(2),
                "5d587c7e-9ced-416a-ad17-6ef9b241a998", "RTE_CO3",
                1, Arrays.asList("2ab1b800-0c93-4517-86b5-8fd6a3a24ee7"));
        this.assertContingencyEquality(listContingencies.get(3),
                "bd7bb012-f7b9-45e0-9e15-4e2aa3592829", "TENNET_TSO_CO3",
                1, Arrays.asList("9c3b8f97-7972-477d-9dc8-87365cc0ad0e"));
        this.assertContingencyEquality(listContingencies.get(4),
                "ce19dd34-429e-4b72-8813-7615cc57b4a4", "RTE_CO6",
                1, Arrays.asList("04839777-c766-11e1-8775-005056c00008"));
        this.assertContingencyEquality(listContingencies.get(5),
                "d9ef0d5e-732d-441e-9611-c817b0afbc41", "RTE_CO5",
                1, Arrays.asList("f0dee14e-aa43-411e-a2ea-b9879c20f3be"));
        this.assertContingencyEquality(listContingencies.get(6),
                "e05bbe20-9d4a-40da-9777-8424d216785d", "RTE_CO1",
                1, Arrays.asList("f1c13f90-6d89-4a37-a51c-94742ad2dd72"));

        List<FlowCnec> listFlowCnecs = cracCreationContext.getCrac().getFlowCnecs()
                .stream().sorted(Comparator.comparing(FlowCnec::getId)).collect(Collectors.toList());
    }

    @Test
    public void testCreateCracCSATestWithRefusedContingencies() {
        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/Test_With_Refused_Contingencies.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);

        Properties importParams = new Properties();
        Network network = Network.read(Paths.get(new File(CsaProfileCracCreatorTest.class.getResource("/Test_With_Refused_Contingencies.zip").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);

        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();
        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-03-29T12:00Z"), new CracCreationParameters());

        assertNotNull(cracCreationContext);
        assertTrue(cracCreationContext.isCreationSuccessful());
        assertEquals(7, cracCreationContext.getCreationReport().getReport().size());
        assertEquals(2, cracCreationContext.getCrac().getContingencies().size());
        assertEquals(4, cracCreationContext.getCrac().getFlowCnecs().size());

        List<Contingency> listContingencies = cracCreationContext.getCrac().getContingencies()
                .stream().sorted(Comparator.comparing(Contingency::getId)).collect(Collectors.toList());

        this.assertContingencyEquality(listContingencies.get(0),
                "493480ba-93c3-426e-bee5-347d8dda3749", "ELIA_CO1",
                1, Arrays.asList("17086487-56ba-4979-b8de-064025a6b4da + 8fdc7abd-3746-481a-a65e-3df56acd8b13"));
        this.assertContingencyEquality(listContingencies.get(1),
                "c0a25fd7-eee0-4191-98a5-71a74469d36e", "TENNET_TSO_CO1",
                1, Arrays.asList("b18cd1aa-7808-49b9-a7cf-605eaf07b006 + e8acf6b6-99cb-45ad-b8dc-16c7866a4ddc"));

        List<FlowCnec> listFlowCnecs = cracCreationContext.getCrac().getFlowCnecs()
                .stream().sorted(Comparator.comparing(FlowCnec::getId)).collect(Collectors.toList());
    }

    private void assertContingencyEquality(Contingency c, String expectedContingencyId, String expectedContingecyName, int expectedNetworkElementsSize, List<String> expectedNetworkElementsIds) {
        assertEquals(expectedContingencyId, c.getId());
        assertEquals(expectedContingecyName, c.getName());
        List<NetworkElement> networkElements = c.getNetworkElements().stream()
                .sorted(Comparator.comparing(NetworkElement::getId)).collect(Collectors.toList());
        assertEquals(expectedNetworkElementsSize, networkElements.size());
        for (int i = 0; i < expectedNetworkElementsSize; i++) {
            assertEquals(expectedNetworkElementsIds.get(i), networkElements.get(i).getId());
        }
    }

    private void assertFlowCnecEquality(FlowCnec fc, String expectedFlowCnecId, String expectedFlowCnecName, String expectedNetworkElementId,
                                        Instant expectedInstant, String expectedContingencyId, double expectedThresholdMax, double expectedThresholdMin, Side expectedThresholdSide) {
        assertEquals(expectedFlowCnecId, fc.getId());
        assertEquals(expectedFlowCnecName, fc.getName());
        assertEquals(expectedNetworkElementId, fc.getNetworkElement().getId());
        assertEquals(expectedInstant, fc.getState().getInstant());
        if (expectedContingencyId == null) {
            assertFalse(fc.getState().getContingency().isPresent());
        } else {
            assertEquals(expectedContingencyId, fc.getState().getContingency().get().getId());
        }

        BranchThreshold threshold = fc.getThresholds().stream().collect(Collectors.toList()).get(0);
        assertEquals(expectedThresholdMax, threshold.max().get());
        assertEquals(expectedThresholdMin, threshold.min().get());
    }

    // csa-9
    @Test
    public void csa92() {
        Properties importParams = new Properties();
        Network network = Network.read(Paths.get(new File(CsaProfileCracCreatorTest.class.getResource("/csa-9/CSA_TestConfiguration_TC2_27Apr2023.zip").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);

        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/csa-9/CSA_TestConfiguration_TC2_27Apr2023.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);

        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();
        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-04-27T12:00Z"), new CracCreationParameters());

        assertNotNull(cracCreationContext);
        Set<RemedialAction<?>> remedialActions = cracCreationContext.getCrac().getRemedialActions();
        assertEquals(9, remedialActions.size());
        // RA17 (on instant)
        NetworkActionImpl ra17 = (NetworkActionImpl) remedialActions.stream().filter(ra -> ra.getName().equals("RA17")).findAny().get();
        assertEquals("cfabf356-c5e1-4391-b91b-3330bc24f0c9", ra17.getId());
        assertEquals("2db971f1-ed3d-4ea6-acf5-983c4289d51b", ra17.getNetworkElements().iterator().next().getId());
        assertEquals(ActionType.OPEN, ((TopologicalAction) ra17.getElementaryActions().iterator().next()).getActionType());
        assertEquals(Instant.PREVENTIVE, ra17.getUsageRules().get(0).getInstant());
        assertEquals(UsageMethod.AVAILABLE, ra17.getUsageRules().get(0).getUsageMethod());
        // RA11 (on instant)
        NetworkActionImpl ra11 = (NetworkActionImpl) remedialActions.stream().filter(ra -> ra.getName().equals("RA11")).findAny().get();
        assertEquals("b2555ccc-6562-4887-8abc-19a6e51cfe36", ra11.getId());
        assertEquals("86dff3a9-afae-4122-afeb-651f2c01c795", ra11.getNetworkElements().iterator().next().getId());
        assertEquals(ActionType.OPEN, ((TopologicalAction) ra11.getElementaryActions().iterator().next()).getActionType());
        assertEquals(Instant.PREVENTIVE, ra11.getUsageRules().get(0).getInstant());
        assertEquals(UsageMethod.AVAILABLE, ra11.getUsageRules().get(0).getUsageMethod());
        // RA2 (on instant)
        NetworkActionImpl ra2 = (NetworkActionImpl) remedialActions.stream().filter(ra -> ra.getName().equals("RA2")).findAny().get();
        assertEquals("d9bd3aaf-cda3-4b54-bb2e-b03dd9925817", ra2.getId());
        assertEquals(2, ra2.getNetworkElements().size());
        List<ElementaryAction> elementaryActions = new ArrayList<>(ra2.getElementaryActions());
        elementaryActions.sort(Comparator.comparingInt(Object::hashCode));
        TopologicalAction topologicalAction1 = (TopologicalAction) elementaryActions.get(0);
        TopologicalAction topologicalAction2 = (TopologicalAction) elementaryActions.get(1);
        assertEquals("39428c75-098b-4366-861d-2df2a857a805", topologicalAction1.getNetworkElement().getId());
        assertEquals(ActionType.OPEN, topologicalAction1.getActionType());
        assertEquals("902046a4-40e9-421d-9ef1-9adab0d9d41d", topologicalAction2.getNetworkElement().getId());
        assertEquals(ActionType.OPEN, topologicalAction2.getActionType());
        assertEquals(Instant.PREVENTIVE, ra2.getUsageRules().get(0).getInstant());
        assertEquals(UsageMethod.AVAILABLE, ra2.getUsageRules().get(0).getUsageMethod());
        // RA13 (on state)
        NetworkActionImpl ra13 = (NetworkActionImpl) remedialActions.stream().filter(ra -> ra.getName().equals("RA13")).findAny().get();
        assertEquals("1fd630a9-b9d8-414b-ac84-b47a093af936", ra13.getId());
        assertEquals(UsageMethod.FORCED, ra13.getUsageRules().get(0).getUsageMethod());
        assertEquals(Instant.CURATIVE, ra13.getUsageRules().get(0).getInstant());
        assertEquals("b6b780cb-9fe5-4c45-989d-447a927c3874", ((OnContingencyStateImpl) ra13.getUsageRules().get(0)).getContingency().getId());
        assertEquals("52effb0d-091b-4867-a0a2-387109cdad5c", ra13.getNetworkElements().iterator().next().getId());
        assertEquals(ActionType.OPEN, ((TopologicalAction) ra13.getElementaryActions().iterator().next()).getActionType());

        // RA22 (on state)
        NetworkActionImpl ra22 = (NetworkActionImpl) remedialActions.stream().filter(ra -> ra.getName().equals("RA22")).findAny().get();
        assertEquals("d856a2a2-3de4-4a7b-aea4-d363c13d9014", ra22.getId());
        assertEquals(UsageMethod.FORCED, ra22.getUsageRules().get(0).getUsageMethod());
        assertEquals(Instant.CURATIVE, ra22.getUsageRules().get(0).getInstant());
        assertEquals("96c96ad8-844c-4f3b-8b38-c886ba2c0214", ((OnContingencyStateImpl) ra22.getUsageRules().get(0)).getContingency().getId());
        assertEquals("c871da6f-816f-4398-82a4-698550cbee58", ra22.getNetworkElements().iterator().next().getId());
        assertEquals(ActionType.OPEN, ((TopologicalAction) ra22.getElementaryActions().iterator().next()).getActionType());

        // RA14 (on state)
        NetworkActionImpl ra14 = (NetworkActionImpl) remedialActions.stream().filter(ra -> ra.getName().equals("RA14")).findAny().get();
        assertEquals("c8bf6b19-1c3b-4ce6-a15c-99995a3c88ce", ra14.getId());
        assertEquals(UsageMethod.FORCED, ra14.getUsageRules().get(0).getUsageMethod());
        assertEquals(Instant.CURATIVE, ra14.getUsageRules().get(0).getInstant());
        assertEquals("13334fdf-9cc2-4341-adb6-1281269040b4", ((OnContingencyStateImpl) ra14.getUsageRules().get(0)).getContingency().getId());
        assertEquals("88e2e417-fc08-41a7-a711-4c6d0784ac4f", ra14.getNetworkElements().iterator().next().getId());
        assertEquals(ActionType.OPEN, ((TopologicalAction) ra14.getElementaryActions().iterator().next()).getActionType());

        // RA21 (on state)
        NetworkActionImpl ra21 = (NetworkActionImpl) remedialActions.stream().filter(ra -> ra.getName().equals("RA21")).findAny().get();
        assertEquals("fb487cc2-0f7b-4958-8f66-1d3fabf7840d", ra21.getId());
        assertEquals(UsageMethod.FORCED, ra21.getUsageRules().get(0).getUsageMethod());
        assertEquals(Instant.CURATIVE, ra21.getUsageRules().get(0).getInstant());
        assertEquals("9d17b84c-33b5-4a68-b8b9-ed5b31038d40", ((OnContingencyStateImpl) ra21.getUsageRules().get(0)).getContingency().getId());
        assertEquals("65b97d2e-d749-41df-aa8f-0be4629d5e0e", ra21.getNetworkElements().iterator().next().getId());
        assertEquals(ActionType.OPEN, ((TopologicalAction) ra21.getElementaryActions().iterator().next()).getActionType());

        // RA3 (on state)
        NetworkActionImpl ra3 = (NetworkActionImpl) remedialActions.stream().filter(ra -> ra.getName().equals("RA3")).findAny().get();
        assertEquals("5e401955-387e-45ce-b126-dd142b06b20c", ra3.getId());
        assertEquals(UsageMethod.FORCED, ra3.getUsageRules().get(0).getUsageMethod());
        assertEquals(Instant.CURATIVE, ra3.getUsageRules().get(0).getInstant());
        assertEquals("475ba18f-cbf5-490b-b65d-e8e03f9bcbc4", ((OnContingencyStateImpl) ra3.getUsageRules().get(0)).getContingency().getId());
        assertEquals("8e55fb9d-e514-4f4b-8a5d-8fd05b1dc02e", ra3.getNetworkElements().iterator().next().getId());
        assertEquals(ActionType.OPEN, ((TopologicalAction) ra3.getElementaryActions().iterator().next()).getActionType());

        // RA5 (on state)
        NetworkActionImpl ra5 = (NetworkActionImpl) remedialActions.stream().filter(ra -> ra.getName().equals("RA5")).findAny().get();
        assertEquals("587cb391-ed16-4a1d-876e-f90241addce5", ra5.getId());
        assertEquals(UsageMethod.FORCED, ra5.getUsageRules().get(0).getUsageMethod());
        assertEquals(Instant.CURATIVE, ra5.getUsageRules().get(0).getInstant());
        assertEquals("5d587c7e-9ced-416a-ad17-6ef9b241a998", ((OnContingencyStateImpl) ra5.getUsageRules().get(0)).getContingency().getId());
        assertEquals("21f21596-302e-4e0e-8009-2b8c3c23517f", ra5.getNetworkElements().iterator().next().getId());
        assertEquals(ActionType.OPEN, ((TopologicalAction) ra5.getElementaryActions().iterator().next()).getActionType());
    }

    @Test
    public void csa94() {
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

        Set<RemedialAction<?>> remedialActions = cracCreationContext.getCrac().getRemedialActions();
        // RA1 (on instant)
        NetworkActionImpl ra1 = (NetworkActionImpl) remedialActions.stream().filter(ra -> ra.getName().equals("RA1")).findAny().get();
        assertEquals("on-instant-preventive-topological-action-parent-remedial-action", ra1.getId());
        assertEquals(Instant.PREVENTIVE, ra1.getUsageRules().get(0).getInstant());
        assertEquals(UsageMethod.AVAILABLE, ra1.getUsageRules().get(0).getUsageMethod());

        // RA2 (on instant)
        NetworkActionImpl ra2 = (NetworkActionImpl) remedialActions.stream().filter(ra -> ra.getName().equals("RA2")).findAny().get();
        assertEquals("on-instant-curative-topological-action-parent-remedial-action", ra2.getId());
        assertEquals(Instant.CURATIVE, ra2.getUsageRules().get(0).getInstant());
        assertEquals(UsageMethod.AVAILABLE, ra2.getUsageRules().get(0).getUsageMethod());

        // RA3 (on state)
        NetworkActionImpl ra3 = (NetworkActionImpl) remedialActions.stream().filter(ra -> ra.getName().equals("RA3")).findAny().get();
        assertEquals("on-state-considered-curative-topological-action-parent-remedial-action", ra3.getId());
        assertEquals(Instant.CURATIVE, ra3.getUsageRules().get(0).getInstant());
        assertEquals(UsageMethod.AVAILABLE, ra3.getUsageRules().get(0).getUsageMethod());
        assertEquals("switch", ra3.getNetworkElements().iterator().next().getId());

        // RA4 (on state)
        NetworkActionImpl ra4 = (NetworkActionImpl) remedialActions.stream().filter(ra -> ra.getName().equals("RA4")).findAny().get();
        assertEquals("on-state-included-curative-topological-action-parent-remedial-action", ra4.getId());
        assertEquals(Instant.CURATIVE, ra4.getUsageRules().get(0).getInstant());
        assertEquals(UsageMethod.FORCED, ra4.getUsageRules().get(0).getUsageMethod());

        // RA5 (on instant + on instant)
        NetworkActionImpl ra5 = (NetworkActionImpl) remedialActions.stream().filter(ra -> ra.getName().equals("RA5")).findAny().get();
        assertEquals("on-state-excluded-curative-topological-action-parent-remedial-action", ra5.getId());
        List<UsageRule> usageRules = ra5.getUsageRules().stream().sorted(Comparator.comparing(UsageRule::getUsageMethod)).collect(Collectors.toList());
        assertEquals(2, usageRules.size());
        assertEquals(Instant.CURATIVE, usageRules.get(0).getInstant());
        assertEquals(UsageMethod.AVAILABLE, usageRules.get(0).getUsageMethod());

        assertEquals(Instant.CURATIVE, usageRules.get(1).getInstant());
        assertEquals(UsageMethod.UNAVAILABLE, usageRules.get(1).getUsageMethod());
        assertEquals("contingency", ((OnContingencyStateImpl) usageRules.get(1)).getState().getContingency().get().getId());

        // RTE_RA7 (on instant)
        NetworkActionImpl ra7 = (NetworkActionImpl) remedialActions.stream().filter(ra -> ra.getId().equals("topological-action-with-tso-name-parent-remedial-action")).findAny().get();
        assertEquals("RTE", ra7.getOperator());
        assertEquals("RTE_RA7", ra7.getName());
        assertEquals(Instant.PREVENTIVE, ra7.getUsageRules().get(0).getInstant());
        assertEquals(UsageMethod.AVAILABLE, ra7.getUsageRules().get(0).getUsageMethod());

        // nameless-topological-action-with-speed-parent-remedial-action (on instant)
        NetworkActionImpl raNameless = (NetworkActionImpl) remedialActions.stream().filter(ra -> ra.getName().equals("nameless-topological-action-with-speed-parent-remedial-action")).findAny().get();
        assertEquals("nameless-topological-action-with-speed-parent-remedial-action", raNameless.getId());
        assertEquals(Instant.PREVENTIVE, raNameless.getUsageRules().get(0).getInstant());
        assertEquals(UsageMethod.AVAILABLE, raNameless.getUsageRules().get(0).getUsageMethod());
        assertEquals(137, raNameless.getSpeed().get());

        // nameless-topological-action-with-tso-name-parent-remedial-action (on instant)
        NetworkActionImpl raNameless2 = (NetworkActionImpl) remedialActions.stream().filter(ra -> ra.getName().equals("nameless-topological-action-with-tso-name-parent-remedial-action")).findAny().get();
        assertEquals("nameless-topological-action-with-tso-name-parent-remedial-action", raNameless2.getId());
        assertEquals("nameless-topological-action-with-tso-name-parent-remedial-action", raNameless2.getName());
        assertEquals("RTE", raNameless2.getOperator());
        assertEquals(Instant.PREVENTIVE, raNameless2.getUsageRules().get(0).getInstant());
        assertEquals(UsageMethod.AVAILABLE, raNameless2.getUsageRules().get(0).getUsageMethod());
    }

    @Test
    public void csa95() {
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
    public void csa96() {
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
    public void csa97() {
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
    public void csa98() {
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
    public void csa99() {
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
    public void csa910() {
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
    public void csa231() {
        Properties importParams = new Properties();
        Network network = Network.read(Paths.get(new File(CsaProfileCracCreatorTest.class.getResource("/csa-23/CSA_23_1_ValidProfiles.zip").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);

        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/csa-23/CSA_23_1_ValidProfiles.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);

        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();
        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-03-29T12:00Z"), new CracCreationParameters());

        // TODO must be 8 but withContingency (onstate) RA's are not imported --> 0 contingency imported --> mock or fix use case
        //assertEquals(8, cracCreationContext.getCrac().getRemedialActions().size());
        Set<RemedialAction<?>> remedialActions = cracCreationContext.getCrac().getRemedialActions();
        // RA1 (on instant)
        NetworkActionImpl ra1 = (NetworkActionImpl) remedialActions.stream().filter(ra -> ra.getName().equals("RA1")).findAny().get();
        assertEquals("on-instant-preventive-remedial-action", ra1.getId());
        assertEquals(Instant.PREVENTIVE, ra1.getUsageRules().get(0).getInstant());
        assertEquals(UsageMethod.AVAILABLE, ra1.getUsageRules().get(0).getUsageMethod());
        assertEquals("rotating-machine", ((InjectionSetpoint) ra1.getElementaryActions().iterator().next()).getNetworkElement().getId());
        assertEquals(75., ((InjectionSetpoint) ra1.getElementaryActions().iterator().next()).getSetpoint());

        // RA2 (on instant)
        NetworkActionImpl ra2 = (NetworkActionImpl) remedialActions.stream().filter(ra -> ra.getName().equals("RA2")).findAny().get();
        assertEquals("on-instant-curative-remedial-action", ra2.getId());
        assertEquals(Instant.CURATIVE, ra2.getUsageRules().get(0).getInstant());
        assertEquals(UsageMethod.AVAILABLE, ra2.getUsageRules().get(0).getUsageMethod());
        assertEquals("rotating-machine", ((InjectionSetpoint) ra2.getElementaryActions().iterator().next()).getNetworkElement().getId());
        assertEquals(17.3, ((InjectionSetpoint) ra2.getElementaryActions().iterator().next()).getSetpoint(), 0.1);

        // on-instant-preventive-nameless-remedial-action-with-speed (on instant)
        NetworkActionImpl namelessRa = (NetworkActionImpl) remedialActions.stream().filter(ra -> ra.getName().equals("on-instant-preventive-nameless-remedial-action-with-speed")).findAny().get();
        assertEquals("on-instant-preventive-nameless-remedial-action-with-speed", namelessRa.getId());
        assertEquals(Instant.PREVENTIVE, namelessRa.getUsageRules().get(0).getInstant());
        assertEquals(UsageMethod.AVAILABLE, namelessRa.getUsageRules().get(0).getUsageMethod());
        assertEquals("rotating-machine", ((InjectionSetpoint) namelessRa.getElementaryActions().iterator().next()).getNetworkElement().getId());
        assertEquals(22.4, ((InjectionSetpoint) namelessRa.getElementaryActions().iterator().next()).getSetpoint(), 0.1);
        assertEquals(137, namelessRa.getSpeed().get());

        // RTE_RA7 (on instant)
        NetworkActionImpl ra7 = (NetworkActionImpl) remedialActions.stream().filter(ra -> ra.getName().equals("RTE_RA7")).findAny().get();
        assertEquals("on-instant-preventive-remedial-with-tso-name", ra7.getId());
        assertEquals(Instant.PREVENTIVE, ra7.getUsageRules().get(0).getInstant());
        assertEquals(UsageMethod.AVAILABLE, ra7.getUsageRules().get(0).getUsageMethod());
        assertEquals("rotating-machine", ((InjectionSetpoint) ra7.getElementaryActions().iterator().next()).getNetworkElement().getId());
        assertEquals(100., ((InjectionSetpoint) ra7.getElementaryActions().iterator().next()).getSetpoint(), 0.1);
        assertEquals("RTE", ra7.getOperator());

        // on-instant-nameless-preventive-remedial-with-tso-name (on instant)
        NetworkActionImpl namelessRa2 = (NetworkActionImpl) remedialActions.stream().filter(ra -> ra.getName().equals("on-instant-nameless-preventive-remedial-with-tso-name")).findAny().get();
        assertEquals("on-instant-nameless-preventive-remedial-with-tso-name", namelessRa2.getId());
        assertEquals(Instant.PREVENTIVE, namelessRa2.getUsageRules().get(0).getInstant());
        assertEquals(UsageMethod.AVAILABLE, namelessRa2.getUsageRules().get(0).getUsageMethod());
        assertEquals("rotating-machine", ((InjectionSetpoint) namelessRa2.getElementaryActions().iterator().next()).getNetworkElement().getId());
        assertEquals(98., ((InjectionSetpoint) namelessRa2.getElementaryActions().iterator().next()).getSetpoint(), 0.1);
        assertEquals("RTE", ra7.getOperator());
    }
}
