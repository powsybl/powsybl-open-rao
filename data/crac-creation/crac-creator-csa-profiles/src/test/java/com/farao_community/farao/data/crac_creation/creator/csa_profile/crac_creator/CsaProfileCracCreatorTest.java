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
import java.util.Iterator;
import java.util.Properties;

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

        Iterator<Contingency> it = cracCreationContext.getCrac().getContingencies().iterator();
        Contingency c1 = it.next();
        assertEquals("c0a25fd7-eee0-4191-98a5-71a74469d36e", c1.getId());
        assertEquals("TENNET_TSO_CO1", c1.getName());
        assertEquals(1, c1.getNetworkElements().size());
        Iterator<NetworkElement> it1 = c1.getNetworkElements().iterator();
        assertEquals("b18cd1aa-7808-49b9-a7cf-605eaf07b006 + e8acf6b6-99cb-45ad-b8dc-16c7866a4ddc", it1.next().getId());

        Contingency c2 = it.next();
        assertEquals("493480ba-93c3-426e-bee5-347d8dda3749", c2.getId());
        assertEquals("ELIA_CO1", c2.getName());
        assertEquals(1, c2.getNetworkElements().size());
        Iterator<NetworkElement> it2 = c2.getNetworkElements().iterator();
        assertEquals("17086487-56ba-4979-b8de-064025a6b4da + 8fdc7abd-3746-481a-a65e-3df56acd8b13", it2.next().getId());
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
        Iterator<Contingency> it = cracCreationContext.getCrac().getContingencies().iterator();
        Contingency c1 = it.next();
        assertEquals("e05bbe20-9d4a-40da-9777-8424d216785d", c1.getId());
        assertEquals("RTE_CO1", c1.getName());
        assertEquals(1, c1.getNetworkElements().size());
        Iterator<NetworkElement> it1 = c1.getNetworkElements().iterator();
        assertEquals("f1c13f90-6d89-4a37-a51c-94742ad2dd72", it1.next().getId());

        Contingency c2 = it.next();
        assertEquals("9d17b84c-33b5-4a68-b8b9-ed5b31038d40", c2.getId());
        assertEquals("REE_CO4", c2.getName());
        assertEquals(2, c2.getNetworkElements().size());
        Iterator<NetworkElement> it2 = c2.getNetworkElements().iterator();
        assertEquals("04566cf8-c766-11e1-8775-005056c00008", it2.next().getId());
        assertEquals("0475dbd8-c766-11e1-8775-005056c00008", it2.next().getId());

        Contingency c3 = it.next();
        assertEquals("264e9a19-ae28-4c85-a43c-6b7818ca0e6c", c3.getId());
        assertEquals("RTE_CO4", c3.getName());
        assertEquals(1, c3.getNetworkElements().size());
        Iterator<NetworkElement> it3 = c3.getNetworkElements().iterator();
        assertEquals("536f4b84-db4c-4545-96e9-bb5a87f65d13 + d9622e7f-5bf0-4e7e-b766-b8596c6fe4ae", it3.next().getId());

        Contingency c4 = it.next();
        assertEquals("13334fdf-9cc2-4341-adb6-1281269040b4", c4.getId());
        assertEquals("REE_CO3", c4.getName());
        assertEquals(2, c4.getNetworkElements().size());
        Iterator<NetworkElement> it4 = c4.getNetworkElements().iterator();
        assertEquals("04566cf8-c766-11e1-8775-005056c00008", it4.next().getId());
        assertEquals("0475dbd8-c766-11e1-8775-005056c00008", it4.next().getId());

        Contingency c5 = it.next();
        assertEquals("bd7bb012-f7b9-45e0-9e15-4e2aa3592829", c5.getId());
        assertEquals("TENNET_TSO_CO3", c5.getName());
        assertEquals(1, c5.getNetworkElements().size());
        Iterator<NetworkElement> it5 = c5.getNetworkElements().iterator();
        assertEquals("9c3b8f97-7972-477d-9dc8-87365cc0ad0e", it5.next().getId());

        Contingency c6 = it.next();
        assertEquals("96c96ad8-844c-4f3b-8b38-c886ba2c0214", c6.getId());
        assertEquals("REE_CO5", c6.getName());
        assertEquals(1, c6.getNetworkElements().size());
        Iterator<NetworkElement> it6 = c6.getNetworkElements().iterator();
        assertEquals("891e77ff-39c6-4648-8eda-d81f730271f9 + a04e4e41-c0b4-496e-9ef3-390ea089411f", it6.next().getId());

        Contingency c7 = it.next();
        assertEquals("b6b780cb-9fe5-4c45-989d-447a927c3874", c7.getId());
        assertEquals("REE_CO2", c7.getName());
        assertEquals(1, c7.getNetworkElements().size());
        Iterator<NetworkElement> it7 = c7.getNetworkElements().iterator();
        assertEquals("048481d0-c766-11e1-8775-005056c00008", it7.next().getId());

        Contingency c8 = it.next();
        assertEquals("e9eab3fe-c328-4f78-9bc1-77adb59f6ba7", c8.getId());
        assertEquals("ELIA_CO1", c8.getName());
        assertEquals(1, c8.getNetworkElements().size());
        Iterator<NetworkElement> it8 = c8.getNetworkElements().iterator();
        assertEquals("dad02278-bd25-476f-8f58-dbe44be72586 + ed0c5d75-4a54-43c8-b782-b20d7431630b", it8.next().getId());

        Contingency c9 = it.next();
        assertEquals("5d587c7e-9ced-416a-ad17-6ef9b241a998", c9.getId());
        assertEquals("RTE_CO3", c9.getName());
        assertEquals(1, c9.getNetworkElements().size());
        Iterator<NetworkElement> it9 = c9.getNetworkElements().iterator();
        assertEquals("2ab1b800-0c93-4517-86b5-8fd6a3a24ee7", it9.next().getId());

        Contingency c10 = it.next();
        assertEquals("8cdec4c6-10c3-40c1-9eeb-7f6ae8d9b3fe", c10.getId());
        assertEquals("REE_CO1", c10.getName());
        assertEquals(1, c10.getNetworkElements().size());
        Iterator<NetworkElement> it10 = c10.getNetworkElements().iterator();
        assertEquals("044bbe91-c766-11e1-8775-005056c00008", it10.next().getId());

        Contingency c11 = it.next();
        assertEquals("d9ef0d5e-732d-441e-9611-c817b0afbc41", c11.getId());
        assertEquals("RTE_CO5", c11.getName());
        assertEquals(1, c11.getNetworkElements().size());
        Iterator<NetworkElement> it11 = c11.getNetworkElements().iterator();
        assertEquals("f0dee14e-aa43-411e-a2ea-b9879c20f3be", it11.next().getId());

        Contingency c12 = it.next();
        assertEquals("7e31c67d-67ba-4592-8ac1-9e806d697c8e", c12.getId());
        assertEquals("ELIA_CO2", c12.getName());
        assertEquals(1, c12.getNetworkElements().size());
        Iterator<NetworkElement> it12 = c12.getNetworkElements().iterator();
        assertEquals("536f4b84-db4c-4545-96e9-bb5a87f65d13 + d9622e7f-5bf0-4e7e-b766-b8596c6fe4ae", it12.next().getId());

        Contingency c13 = it.next();
        assertEquals("37997e71-cb7d-4a8c-baa6-2a1594956da9", c13.getId());
        assertEquals("ELIA_CO3", c13.getName());
        assertEquals(1, c13.getNetworkElements().size());
        Iterator<NetworkElement> it13 = c13.getNetworkElements().iterator();
        assertEquals("550ebe0d-f2b2-48c1-991f-cebea43a21aa", it13.next().getId());

        Contingency c14 = it.next();
        assertEquals("ce19dd34-429e-4b72-8813-7615cc57b4a4", c14.getId());
        assertEquals("RTE_CO6", c14.getName());
        assertEquals(1, c14.getNetworkElements().size());
        Iterator<NetworkElement> it14 = c14.getNetworkElements().iterator();
        assertEquals("04839777-c766-11e1-8775-005056c00008", it14.next().getId());

        Contingency c15 = it.next();
        assertEquals("475ba18f-cbf5-490b-b65d-e8e03f9bcbc4", c15.getId());
        assertEquals("RTE_CO2", c15.getName());
        assertEquals(1, c15.getNetworkElements().size());
        Iterator<NetworkElement> it15 = c15.getNetworkElements().iterator();
        assertEquals("e02e1166-1c43-4a4d-8c5a-82298ee0c8f5", it15.next().getId());
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
        Iterator<Contingency> it = cracCreationContext.getCrac().getContingencies().iterator();
        Contingency c1 = it.next();
        assertEquals("e05bbe20-9d4a-40da-9777-8424d216785d", c1.getId());
        assertEquals("RTE_CO1", c1.getName());
        assertEquals(1, c1.getNetworkElements().size());
        Iterator<NetworkElement> it1 = c1.getNetworkElements().iterator();
        assertEquals("f1c13f90-6d89-4a37-a51c-94742ad2dd72", it1.next().getId());

        Contingency c2 = it.next();
        assertEquals("264e9a19-ae28-4c85-a43c-6b7818ca0e6c", c2.getId());
        assertEquals("RTE_CO4", c2.getName());
        assertEquals(1, c2.getNetworkElements().size());
        Iterator<NetworkElement> it2 = c2.getNetworkElements().iterator();
        assertEquals("536f4b84-db4c-4545-96e9-bb5a87f65d13 + d9622e7f-5bf0-4e7e-b766-b8596c6fe4ae", it2.next().getId());

        Contingency c3 = it.next();
        assertEquals("d9ef0d5e-732d-441e-9611-c817b0afbc41", c3.getId());
        assertEquals("RTE_CO5", c3.getName());
        assertEquals(1, c3.getNetworkElements().size());
        Iterator<NetworkElement> it3 = c3.getNetworkElements().iterator();
        assertEquals("f0dee14e-aa43-411e-a2ea-b9879c20f3be", it3.next().getId());

        Contingency c4 = it.next();
        assertEquals("bd7bb012-f7b9-45e0-9e15-4e2aa3592829", c4.getId());
        assertEquals("TENNET_TSO_CO3", c4.getName());
        assertEquals(1, c4.getNetworkElements().size());
        Iterator<NetworkElement> it4 = c4.getNetworkElements().iterator();
        assertEquals("9c3b8f97-7972-477d-9dc8-87365cc0ad0e", it4.next().getId());

        Contingency c5 = it.next();
        assertEquals("ce19dd34-429e-4b72-8813-7615cc57b4a4", c5.getId());
        assertEquals("RTE_CO6", c5.getName());
        assertEquals(1, c5.getNetworkElements().size());
        Iterator<NetworkElement> it5 = c5.getNetworkElements().iterator();
        assertEquals("04839777-c766-11e1-8775-005056c00008", it5.next().getId());

        Contingency c6 = it.next();
        assertEquals("475ba18f-cbf5-490b-b65d-e8e03f9bcbc4", c6.getId());
        assertEquals("RTE_CO2", c6.getName());
        assertEquals(1, c6.getNetworkElements().size());
        Iterator<NetworkElement> it6 = c6.getNetworkElements().iterator();
        assertEquals("e02e1166-1c43-4a4d-8c5a-82298ee0c8f5", it6.next().getId());

        Contingency c7 = it.next();
        assertEquals("5d587c7e-9ced-416a-ad17-6ef9b241a998", c7.getId());
        assertEquals("RTE_CO3", c7.getName());
        assertEquals(1, c7.getNetworkElements().size());
        Iterator<NetworkElement> it7 = c7.getNetworkElements().iterator();
        assertEquals("2ab1b800-0c93-4517-86b5-8fd6a3a24ee7", it7.next().getId());
    }
}
