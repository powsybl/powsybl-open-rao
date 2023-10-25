package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.remedial_action;

import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
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
import com.powsybl.iidm.network.ImportConfig;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Properties;

import static com.farao_community.farao.data.crac_api.Instant.CURATIVE;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PstRangeActionCreationTest {

    @Test
    public void testTC1ImportPstRangeActions() {
        Properties importParams = new Properties();
        Network network = Network.read(Paths.get(new File(CsaProfileCracCreatorTest.class.getResource("/TestConfiguration_TC1_v29Mar2023.zip").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);

        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/TestConfiguration_TC1_v29Mar2023.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);

        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();
        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-03-29T12:00Z"), new CracCreationParameters());

        // ELIA_RA1 (on instant)
        PstRangeAction eliaRa1 = cracCreationContext.getCrac().getPstRangeAction("7fc2fc14-eea6-4e69-b8d9-a3edc218e687");
        assertEquals("ELIA_RA1", eliaRa1.getName());
        assertEquals("ELIA", eliaRa1.getOperator());
        assertEquals("36b83adb-3d45-4693-8967-96627b5f9ec9", eliaRa1.getNetworkElement().getId());
        assertEquals(10, eliaRa1.getInitialTap());
        assertEquals(1, eliaRa1.getRanges().size());
        assertEquals(5., eliaRa1.getRanges().iterator().next().getMinTap());
        assertEquals(20., eliaRa1.getRanges().iterator().next().getMaxTap());
        assertEquals(1, eliaRa1.getUsageRules().size());
        assertEquals(CURATIVE, eliaRa1.getUsageRules().iterator().next().getInstant());
        // TODO waiting for PO to check US, after implementation of CSA11 behaviour changed, assertEquals("493480ba-93c3-426e-bee5-347d8dda3749", ((OnContingencyStateImpl) eliaRa1.getUsageRules().iterator().next()).getState().getContingency().get().getId());
        Map<Integer, Double> expectedTapToAngleMap = Map.ofEntries(
                Map.entry(1, 4.926567934889113),
                Map.entry(2, 4.4625049779277965),
                Map.entry(3, 4.009142308337196),
                Map.entry(4, 3.5661689080738133),
                Map.entry(5, 3.133282879390916),
                Map.entry(6, 2.7101913084587235),
                Map.entry(7, 2.296610111393503),
                Map.entry(8, 1.892263865774221),
                Map.entry(9, 1.496885630374893),
                Map.entry(10, 1.1102167555229658),
                Map.entry(11, 0.7320066862066437),
                Map.entry(12, 0.36201275979482317),
                Map.entry(13, -0.0),
                Map.entry(14, -0.3542590914949466),
                Map.entry(15, -0.7009847445128217),
                Map.entry(16, -1.040390129895497),
                Map.entry(17, -1.3726815681386877),
                Map.entry(18, -1.698058736365395),
                Map.entry(19, -2.016714872973585),
                Map.entry(20, -2.32883697939856),
                Map.entry(21, -2.6346060185232267),
                Map.entry(22, -2.9341971093513304),
                Map.entry(23, -3.227779717630807),
                Map.entry(24, -3.515517842177712),
                Map.entry(25, -3.797570196706609)
        );
        assertEquals(expectedTapToAngleMap, eliaRa1.getTapToAngleConversionMap());
    }

    @Test
    public void testTC2ImportPstRangeActions() {
        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/CSA_TestConfiguration_TC2_Draft_v14Apr2023.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);

        Properties importParams = new Properties();
        Network network = Network.read(Paths.get(new File(CsaProfileCracCreatorTest.class.getResource("/CSA_TestConfiguration_TC2_Draft_v14Apr2023.zip").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);

        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();
        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-03-29T12:00Z"), new CracCreationParameters());

        PstRangeAction reeRa1 = cracCreationContext.getCrac().getPstRangeAction("5898c268-9b32-4ab5-9cfc-64546135a337");
        assertEquals("RA1", reeRa1.getName());
        assertEquals("f6e8823f-d431-6fc7-37cf-b7a0d80035dd", reeRa1.getNetworkElement().getId());
        assertEquals(13, reeRa1.getInitialTap());
        assertEquals(0, reeRa1.getRanges().size());
        assertEquals(1, reeRa1.getUsageRules().size());
        assertEquals(CURATIVE, reeRa1.getUsageRules().iterator().next().getInstant());
        assertEquals("8cdec4c6-10c3-40c1-9eeb-7f6ae8d9b3fe", ((OnContingencyStateImpl) reeRa1.getUsageRules().iterator().next()).getState().getContingency().get().getId());
        Map<Integer, Double> expectedTapToAngleMap = Map.ofEntries(
                Map.entry(-1, -2.0),
                Map.entry(0, 0.0),
                Map.entry(-2, -4.0),
                Map.entry(1, 2.0),
                Map.entry(-3, -6.0),
                Map.entry(2, 4.0),
                Map.entry(-4, -8.0),
                Map.entry(3, 6.0),
                Map.entry(-5, -10.0),
                Map.entry(4, 8.0),
                Map.entry(-6, -12.0),
                Map.entry(5, 10.0),
                Map.entry(-7, -14.0),
                Map.entry(6, 12.0),
                Map.entry(-8, -16.0),
                Map.entry(7, 14.0),
                Map.entry(-9, -18.0),
                Map.entry(8, 16.0),
                Map.entry(-10, -20.0),
                Map.entry(9, 18.0),
                Map.entry(-11, -22.0),
                Map.entry(10, 20.0),
                Map.entry(-12, -24.0),
                Map.entry(11, 22.0),
                Map.entry(-13, -26.0),
                Map.entry(12, 24.0),
                Map.entry(-14, -28.0),
                Map.entry(13, 26.0),
                Map.entry(-15, -30.0),
                Map.entry(14, 28.0),
                Map.entry(-16, -32.0),
                Map.entry(15, 30.0),
                Map.entry(-17, -34.0),
                Map.entry(16, 32.0),
                Map.entry(-18, -36.0),
                Map.entry(17, 34.0),
                Map.entry(-19, -38.0),
                Map.entry(18, 36.0),
                Map.entry(-20, -40.0),
                Map.entry(19, 38.0),
                Map.entry(20, 40.0)
        );
        assertEquals(expectedTapToAngleMap, reeRa1.getTapToAngleConversionMap());

        assertEquals(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, cracCreationContext.getRemedialActionCreationContexts().stream().filter(ra -> ra.getNativeId().equals("5e5ff13e-2043-4468-9351-01920d3d9504")).findAny().get().getImportStatus());
        assertEquals(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, cracCreationContext.getRemedialActionCreationContexts().stream().filter(ra -> ra.getNativeId().equals("2e4f4212-7b30-4316-9fce-ca618f2a8a05")).findAny().get().getImportStatus());
    }

}
