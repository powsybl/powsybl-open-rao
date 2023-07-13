/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator;

import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.CsaProfileCrac;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.importer.CsaProfileCracImporter;
import com.google.common.base.Suppliers;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.ImportConfig;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
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
        assertEquals(2, cracCreationContext.getCrac().getContingencies().size());
        List<Contingency> listContingencies = cracCreationContext.getCrac().getContingencies()
                .stream().sorted(Comparator.comparing(Contingency::getId)).collect(Collectors.toList());

        this.assertContingencyEquality(listContingencies.get(0),
                "493480ba-93c3-426e-bee5-347d8dda3749", "ELIA_CO1",
                1, Arrays.asList("17086487-56ba-4979-b8de-064025a6b4da + 8fdc7abd-3746-481a-a65e-3df56acd8b13"));
        this.assertContingencyEquality(listContingencies.get(1),
                "c0a25fd7-eee0-4191-98a5-71a74469d36e", "TENNET_TSO_CO1",
                1, Arrays.asList("b18cd1aa-7808-49b9-a7cf-605eaf07b006 + e8acf6b6-99cb-45ad-b8dc-16c7866a4ddc"));
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
        assertEquals(15, cracCreationContext.getCrac().getContingencies().size());

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
        assertEquals(7, cracCreationContext.getCrac().getContingencies().size());
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
        assertEquals(2, cracCreationContext.getCrac().getContingencies().size());
        List<Contingency> listContingencies = cracCreationContext.getCrac().getContingencies()
                .stream().sorted(Comparator.comparing(Contingency::getId)).collect(Collectors.toList());

        this.assertContingencyEquality(listContingencies.get(0),
                "493480ba-93c3-426e-bee5-347d8dda3749", "ELIA_CO1",
                1, Arrays.asList("17086487-56ba-4979-b8de-064025a6b4da + 8fdc7abd-3746-481a-a65e-3df56acd8b13"));
        this.assertContingencyEquality(listContingencies.get(1),
                "c0a25fd7-eee0-4191-98a5-71a74469d36e", "TENNET_TSO_CO1",
                1, Arrays.asList("b18cd1aa-7808-49b9-a7cf-605eaf07b006 + e8acf6b6-99cb-45ad-b8dc-16c7866a4ddc"));
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
}
