package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.remedial_action;

import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.network_action.*;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.CsaProfileCrac;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreator;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.importer.CsaProfileCracImporter;
import com.farao_community.farao.data.crac_creation.util.FaraoImportException;
import com.farao_community.farao.data.crac_impl.OnContingencyStateImpl;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreationTestUtil.getCsaCracCreationContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AutoRemedialActionTest {

    @Test
    public void importAutoRemedialActionTC2() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/CSA_TestConfiguration_TC2_27Apr2023.zip");
        List<RemedialAction> autoRemedialActionList = cracCreationContext.getCrac().getRemedialActions().stream().filter(ra -> ra.getUsageRules().stream().anyMatch(usageRule -> usageRule.getInstant().isAuto())).collect(Collectors.toList());
        assertEquals(1, autoRemedialActionList.size());
        NetworkAction autoRa = (NetworkAction) autoRemedialActionList.get(0);
        assertEquals("31d41e36-11c8-417b-bafb-c410d4391898", autoRa.getId());
        assertEquals("CRA", autoRa.getName());
        assertEquals(1, autoRa.getUsageRules().size());
        UsageRule onStateUsageRule = autoRa.getUsageRules().iterator().next();
        assertEquals(InstantKind.AUTO, onStateUsageRule.getInstant().getInstantKind());
        assertEquals(UsageMethod.FORCED, onStateUsageRule.getUsageMethod());
        assertEquals("e05bbe20-9d4a-40da-9777-8424d216785d", ((OnContingencyStateImpl) onStateUsageRule).getContingency().getId());
        assertEquals("2db971f1-ed3d-4ea6-acf5-983c4289d51b", autoRa.getNetworkElements().iterator().next().getId());
        assertEquals(ActionType.OPEN, ((TopologicalAction) autoRa.getElementaryActions().iterator().next()).getActionType());
    }

    @Test
    public void importAutoRemedialActionSps2() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/SPS_with_shunt_compensator_and_pst.zip");

        List<RemedialAction> autoRemedialActionList = cracCreationContext.getCrac().getRemedialActions().stream().filter(ra -> ra.getUsageRules().stream().anyMatch(usageRule -> usageRule.getInstant().isAuto())).collect(Collectors.toList());
        assertEquals(4, autoRemedialActionList.size());

        NetworkAction ara2 = cracCreationContext.getCrac().getNetworkAction("auto-topological-action");
        assertEquals("ARA2", ara2.getName());
        UsageRule ur1 = ara2.getUsageRules().iterator().next();
        assertEquals(InstantKind.AUTO, ur1.getInstant().getInstantKind());
        assertEquals(UsageMethod.FORCED, ur1.getUsageMethod());
        assertEquals("contingency-2", ((OnContingencyStateImpl) ur1).getContingency().getId());
        assertEquals("BBE1AA1  BBE4AA1  1", ara2.getNetworkElements().iterator().next().getId());
        assertEquals(ActionType.OPEN, ((TopologicalAction) ara2.getElementaryActions().iterator().next()).getActionType());

        NetworkAction ara3 = cracCreationContext.getCrac().getNetworkAction("auto-rotating-machine-action");
        assertEquals("ARA3", ara3.getName());
        assertEquals(10, ara3.getSpeed().get());
        UsageRule ur3 = ara3.getUsageRules().iterator().next();
        assertEquals(InstantKind.AUTO, ur3.getInstant().getInstantKind());
        assertEquals(UsageMethod.FORCED, ur3.getUsageMethod());
        assertEquals("contingency-3", ((OnContingencyStateImpl) ur3).getContingency().getId());
        List<NetworkElement> networkElements = ara3.getNetworkElements().stream().sorted(Comparator.comparing(NetworkElement::getId)).toList();
        assertEquals("FFR1AA1 _generator", networkElements.get(0).getId());
        assertEquals("FFR2AA1 _generator", networkElements.get(1).getId());

        NetworkAction ara4 = cracCreationContext.getCrac().getNetworkAction("auto-shunt-compensator-action");
        assertEquals("ARA4", ara4.getName());
        UsageRule ur4 = ara4.getUsageRules().iterator().next();
        assertEquals(InstantKind.AUTO, ur4.getInstant().getInstantKind());
        assertEquals(UsageMethod.FORCED, ur4.getUsageMethod());
        assertEquals(3, ((InjectionSetpoint) ara4.getElementaryActions().iterator().next()).getSetpoint());

        PstRangeAction ara1 = cracCreationContext.getCrac().getPstRangeAction("auto-pst-range-action");
        assertEquals("ARA1", ara1.getName());
        assertEquals("BBE2AA1  BBE3AA1  1", ara1.getNetworkElement().getId());
        assertEquals(-3, ara1.getRanges().get(0).getMinTap());
        assertEquals(7, ara1.getRanges().get(0).getMaxTap());
        assertEquals("contingency-1", ((OnContingencyStateImpl) ara1.getUsageRules().iterator().next()).getContingency().getId());

    }

    @Test
    public void importInvalidRasProfilesSps3() {
        Network network = Mockito.mock(Network.class);
        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/CSA_SPS_3_InvalidProflies.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);
        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();
        assertThrows(FaraoImportException.class, () -> cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-04-27T12:00Z"), new CracCreationParameters()));
    }
}
