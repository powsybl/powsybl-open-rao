package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.contingencies;

import com.powsybl.openrao.data.cracapi.Contingency;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationContext;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.getCsaCracCreationContext;
import static org.junit.jupiter.api.Assertions.*;

class ContingenciesCreationTest {

    @Test
    void testTC1Contingencies() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/TestConfiguration_TC1_v29Mar2023.zip");

        assertNotNull(cracCreationContext);
        assertTrue(cracCreationContext.isCreationSuccessful());
        assertEquals(2, cracCreationContext.getCrac().getContingencies().size());
        List<Contingency> listContingencies = cracCreationContext.getCrac().getContingencies()
            .stream().sorted(Comparator.comparing(Contingency::getId)).toList();

        CsaProfileCracCreationTestUtil.assertContingencyEquality(listContingencies.iterator().next(),
            "493480ba-93c3-426e-bee5-347d8dda3749", "ELIA_CO1",
            1, List.of("17086487-56ba-4979-b8de-064025a6b4da + 8fdc7abd-3746-481a-a65e-3df56acd8b13"));
        CsaProfileCracCreationTestUtil.assertContingencyEquality(listContingencies.get(1),
            "c0a25fd7-eee0-4191-98a5-71a74469d36e", "TENNET_TSO_CO1",
            1, List.of("b18cd1aa-7808-49b9-a7cf-605eaf07b006 + e8acf6b6-99cb-45ad-b8dc-16c7866a4ddc"));

        // csa-9-1
        assertTrue(cracCreationContext.getCrac().getNetworkActions().isEmpty());
    }

    @Test
    void testTC2Contingencies() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/CSA_TestConfiguration_TC2_Draft_v14Apr2023.zip");

        assertNotNull(cracCreationContext);
        assertTrue(cracCreationContext.isCreationSuccessful());
        assertEquals(15, cracCreationContext.getCrac().getContingencies().size());

        List<Contingency> listContingencies = cracCreationContext.getCrac().getContingencies()
            .stream().sorted(Comparator.comparing(Contingency::getId)).toList();

        CsaProfileCracCreationTestUtil.assertContingencyEquality(listContingencies.iterator().next(),
            "13334fdf-9cc2-4341-adb6-1281269040b4", "REE_CO3",
            2, List.of("04566cf8-c766-11e1-8775-005056c00008", "0475dbd8-c766-11e1-8775-005056c00008"));
        CsaProfileCracCreationTestUtil.assertContingencyEquality(listContingencies.get(1),
            "264e9a19-ae28-4c85-a43c-6b7818ca0e6c", "RTE_CO4",
            1, List.of("536f4b84-db4c-4545-96e9-bb5a87f65d13 + d9622e7f-5bf0-4e7e-b766-b8596c6fe4ae"));
        CsaProfileCracCreationTestUtil.assertContingencyEquality(listContingencies.get(2),
            "37997e71-cb7d-4a8c-baa6-2a1594956da9", "ELIA_CO3",
            1, List.of("550ebe0d-f2b2-48c1-991f-cebea43a21aa"));
        CsaProfileCracCreationTestUtil.assertContingencyEquality(listContingencies.get(3),
            "475ba18f-cbf5-490b-b65d-e8e03f9bcbc4", "RTE_CO2",
            1, List.of("e02e1166-1c43-4a4d-8c5a-82298ee0c8f5"));
        CsaProfileCracCreationTestUtil.assertContingencyEquality(listContingencies.get(4),
            "5d587c7e-9ced-416a-ad17-6ef9b241a998", "RTE_CO3",
            1, List.of("2ab1b800-0c93-4517-86b5-8fd6a3a24ee7"));
        CsaProfileCracCreationTestUtil.assertContingencyEquality(listContingencies.get(5),
            "7e31c67d-67ba-4592-8ac1-9e806d697c8e", "ELIA_CO2",
            1, List.of("536f4b84-db4c-4545-96e9-bb5a87f65d13 + d9622e7f-5bf0-4e7e-b766-b8596c6fe4ae"));
        CsaProfileCracCreationTestUtil.assertContingencyEquality(listContingencies.get(6),
            "8cdec4c6-10c3-40c1-9eeb-7f6ae8d9b3fe", "REE_CO1",
            1, List.of("044bbe91-c766-11e1-8775-005056c00008"));
        CsaProfileCracCreationTestUtil.assertContingencyEquality(listContingencies.get(7),
            "96c96ad8-844c-4f3b-8b38-c886ba2c0214", "REE_CO5",
            1, List.of("891e77ff-39c6-4648-8eda-d81f730271f9 + a04e4e41-c0b4-496e-9ef3-390ea089411f"));
        CsaProfileCracCreationTestUtil.assertContingencyEquality(listContingencies.get(8),
            "9d17b84c-33b5-4a68-b8b9-ed5b31038d40", "REE_CO4",
            2, List.of("04566cf8-c766-11e1-8775-005056c00008", "0475dbd8-c766-11e1-8775-005056c00008"));
        CsaProfileCracCreationTestUtil.assertContingencyEquality(listContingencies.get(9),
            "b6b780cb-9fe5-4c45-989d-447a927c3874", "REE_CO2",
            1, List.of("048481d0-c766-11e1-8775-005056c00008"));
        CsaProfileCracCreationTestUtil.assertContingencyEquality(listContingencies.get(10),
            "bd7bb012-f7b9-45e0-9e15-4e2aa3592829", "TENNET_TSO_CO3",
            1, List.of("9c3b8f97-7972-477d-9dc8-87365cc0ad0e"));
        CsaProfileCracCreationTestUtil.assertContingencyEquality(listContingencies.get(11),
            "ce19dd34-429e-4b72-8813-7615cc57b4a4", "RTE_CO6",
            1, List.of("04839777-c766-11e1-8775-005056c00008"));
        CsaProfileCracCreationTestUtil.assertContingencyEquality(listContingencies.get(12),
            "d9ef0d5e-732d-441e-9611-c817b0afbc41", "RTE_CO5",
            1, List.of("f0dee14e-aa43-411e-a2ea-b9879c20f3be"));
        CsaProfileCracCreationTestUtil.assertContingencyEquality(listContingencies.get(13),
            "e05bbe20-9d4a-40da-9777-8424d216785d", "RTE_CO1",
            1, List.of("f1c13f90-6d89-4a37-a51c-94742ad2dd72"));
        CsaProfileCracCreationTestUtil.assertContingencyEquality(listContingencies.get(14),
            "e9eab3fe-c328-4f78-9bc1-77adb59f6ba7", "ELIA_CO1",
            1, List.of("dad02278-bd25-476f-8f58-dbe44be72586 + ed0c5d75-4a54-43c8-b782-b20d7431630b"));
    }

    @Test
    void testCreateCracCSATestWithRejectedFiles() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/CSA_Test_With_Rejected_Files.zip");

        assertEquals(7, cracCreationContext.getCrac().getContingencies().size());

        List<Contingency> listContingencies = cracCreationContext.getCrac().getContingencies()
            .stream().sorted(Comparator.comparing(Contingency::getId)).toList();

        CsaProfileCracCreationTestUtil.assertContingencyEquality(listContingencies.iterator().next(),
            "264e9a19-ae28-4c85-a43c-6b7818ca0e6c", "RTE_CO4",
            1, List.of("536f4b84-db4c-4545-96e9-bb5a87f65d13 + d9622e7f-5bf0-4e7e-b766-b8596c6fe4ae"));
        CsaProfileCracCreationTestUtil.assertContingencyEquality(listContingencies.get(1),
            "475ba18f-cbf5-490b-b65d-e8e03f9bcbc4", "RTE_CO2",
            1, List.of("e02e1166-1c43-4a4d-8c5a-82298ee0c8f5"));
        CsaProfileCracCreationTestUtil.assertContingencyEquality(listContingencies.get(2),
            "5d587c7e-9ced-416a-ad17-6ef9b241a998", "RTE_CO3",
            1, List.of("2ab1b800-0c93-4517-86b5-8fd6a3a24ee7"));
        CsaProfileCracCreationTestUtil.assertContingencyEquality(listContingencies.get(3),
            "bd7bb012-f7b9-45e0-9e15-4e2aa3592829", "TENNET_TSO_CO3",
            1, List.of("9c3b8f97-7972-477d-9dc8-87365cc0ad0e"));
        CsaProfileCracCreationTestUtil.assertContingencyEquality(listContingencies.get(4),
            "ce19dd34-429e-4b72-8813-7615cc57b4a4", "RTE_CO6",
            1, List.of("04839777-c766-11e1-8775-005056c00008"));
        CsaProfileCracCreationTestUtil.assertContingencyEquality(listContingencies.get(5),
            "d9ef0d5e-732d-441e-9611-c817b0afbc41", "RTE_CO5",
            1, List.of("f0dee14e-aa43-411e-a2ea-b9879c20f3be"));
        CsaProfileCracCreationTestUtil.assertContingencyEquality(listContingencies.get(6),
            "e05bbe20-9d4a-40da-9777-8424d216785d", "RTE_CO1",
            1, List.of("f1c13f90-6d89-4a37-a51c-94742ad2dd72"));
    }

    @Test
    void testCreateCracCSATestWithRefusedContingencies() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/Test_With_Refused_Contingencies.zip");

        assertNotNull(cracCreationContext);
        assertTrue(cracCreationContext.isCreationSuccessful());
        assertEquals(2, cracCreationContext.getCrac().getContingencies().size());

        List<Contingency> listContingencies = cracCreationContext.getCrac().getContingencies()
            .stream().sorted(Comparator.comparing(Contingency::getId)).toList();

        CsaProfileCracCreationTestUtil.assertContingencyEquality(listContingencies.iterator().next(),
            "493480ba-93c3-426e-bee5-347d8dda3749", "ELIA_CO1",
            1, List.of("17086487-56ba-4979-b8de-064025a6b4da + 8fdc7abd-3746-481a-a65e-3df56acd8b13"));
        CsaProfileCracCreationTestUtil.assertContingencyEquality(listContingencies.get(1),
            "c0a25fd7-eee0-4191-98a5-71a74469d36e", "TENNET_TSO_CO1",
            1, List.of("b18cd1aa-7808-49b9-a7cf-605eaf07b006 + e8acf6b6-99cb-45ad-b8dc-16c7866a4ddc"));
    }

    @Test
    void testCSA76ContingenciesWithoutTsoOrName() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/CSA_76_ContingenciesWithoutTSOorName.zip");

        assertNotNull(cracCreationContext);
        assertTrue(cracCreationContext.isCreationSuccessful());
        assertEquals(2, cracCreationContext.getCrac().getContingencies().size());
        List<Contingency> listContingencies = cracCreationContext.getCrac().getContingencies()
            .stream().sorted(Comparator.comparing(Contingency::getId)).toList();

        CsaProfileCracCreationTestUtil.assertContingencyEquality(listContingencies.iterator().next(),
            "493480ba-93c3-426e-bee5-347d8dda3749", "CO1",
            1, List.of("17086487-56ba-4979-b8de-064025a6b4da + 8fdc7abd-3746-481a-a65e-3df56acd8b13"));
        CsaProfileCracCreationTestUtil.assertContingencyEquality(listContingencies.get(1),
            "c0a25fd7-eee0-4191-98a5-71a74469d36e", "c0a25fd7-eee0-4191-98a5-71a74469d36e",
            1, List.of("b18cd1aa-7808-49b9-a7cf-605eaf07b006 + e8acf6b6-99cb-45ad-b8dc-16c7866a4ddc"));
    }
}
