package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.remedial_action;

import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.network_action.*;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.CsaProfileCrac;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreator;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreatorTest;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.importer.CsaProfileCracImporter;
import com.farao_community.farao.data.crac_creation.util.FaraoImportException;
import com.farao_community.farao.data.crac_impl.OnContingencyStateImpl;
import com.google.common.base.Suppliers;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.ImportConfig;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AutoRemedialActionTest {

    @Test
    public void importAutoRemedialActionTC2() {
        Properties importParams = new Properties();
        Network network = Network.read(Paths.get(new File(CsaProfileCracCreatorTest.class.getResource("/csa-9/CSA_TestConfiguration_TC2_27Apr2023.zip").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);

        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/csa-9/CSA_TestConfiguration_TC2_27Apr2023.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);
        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();
        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-04-27T12:00Z"), new CracCreationParameters());

        List<RemedialAction> autoRemedialActionList = cracCreationContext.getCrac().getRemedialActions().stream().filter(ra -> ra.getUsageRules().stream().anyMatch(usageRule -> usageRule.getInstant().equals(Instant.AUTO))).collect(Collectors.toList());
        assertEquals(1, autoRemedialActionList.size());
        NetworkAction autoRa = (NetworkAction) autoRemedialActionList.get(0);
        assertEquals("31d41e36-11c8-417b-bafb-c410d4391898", autoRa.getId());
        assertEquals("CRA", autoRa.getName());
        assertEquals(1, autoRa.getUsageRules().size());
        UsageRule onStateUsageRule = autoRa.getUsageRules().iterator().next();
        assertEquals(Instant.AUTO, onStateUsageRule.getInstant());
        assertEquals(UsageMethod.FORCED, onStateUsageRule.getUsageMethod());
        assertEquals("e05bbe20-9d4a-40da-9777-8424d216785d", ((OnContingencyStateImpl) onStateUsageRule).getContingency().getId());
        assertEquals("2db971f1-ed3d-4ea6-acf5-983c4289d51b", autoRa.getNetworkElements().iterator().next().getId());
        assertEquals(ActionType.OPEN, ((TopologicalAction) autoRa.getElementaryActions().iterator().next()).getActionType());
    }

    @Test
    public void importAutoRemedialActionSps2() {
        Properties importParams = new Properties();
        Network network = Network.read(Paths.get(new File(CsaProfileCracCreatorTest.class.getResource("/CSA_SPS_2_ValidProflies.zip").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);

        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = getClass().getResourceAsStream("/CSA_SPS_2_ValidProflies.zip");
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);
        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();
        CsaProfileCracCreationContext cracCreationContext = cracCreator.createCrac(nativeCrac, network, OffsetDateTime.parse("2023-04-27T12:00Z"), new CracCreationParameters());

        List<RemedialAction> autoRemedialActionList = cracCreationContext.getCrac().getRemedialActions().stream().filter(ra -> ra.getUsageRules().stream().anyMatch(usageRule -> usageRule.getInstant().equals(Instant.AUTO))).collect(Collectors.toList());
        assertEquals(3, autoRemedialActionList.size());

        NetworkAction ara1 = cracCreationContext.getCrac().getNetworkAction("scheme-remedial-action");
        assertEquals("scheme-remedial-action", ara1.getName());
        UsageRule ur1 = ara1.getUsageRules().iterator().next();
        assertEquals(Instant.AUTO, ur1.getInstant());
        assertEquals(UsageMethod.FORCED, ur1.getUsageMethod());
        assertEquals("contingency-1", ((OnContingencyStateImpl) ur1).getContingency().getId());
        assertEquals("BBE1AA1  BBE4AA1  1", ara1.getNetworkElements().iterator().next().getId());
        assertEquals(ActionType.OPEN, ((TopologicalAction) ara1.getElementaryActions().iterator().next()).getActionType());

        NetworkAction ara2 = cracCreationContext.getCrac().getNetworkAction("named-scheme-remedial-action");
        assertEquals("ARA2", ara2.getName());
        UsageRule ur2 = ara2.getUsageRules().iterator().next();
        assertEquals(Instant.AUTO, ur2.getInstant());
        assertEquals(UsageMethod.FORCED, ur2.getUsageMethod());
        assertEquals("contingency-2", ((OnContingencyStateImpl) ur2).getContingency().getId());
        assertEquals("BBE1AA1 _generator", ara2.getNetworkElements().iterator().next().getId());
        assertEquals(75., ((InjectionSetpoint) ara2.getElementaryActions().iterator().next()).getSetpoint());

        NetworkAction ara3 = cracCreationContext.getCrac().getNetworkAction("named-scheme-remedial-action-with-tso-and-speed");
        assertEquals("RTE_ARA3", ara3.getName());
        assertEquals(10, ara3.getSpeed().get());
        assertEquals("RTE", ara3.getOperator());
        UsageRule ur3 = ara3.getUsageRules().iterator().next();
        assertEquals(Instant.AUTO, ur3.getInstant());
        assertEquals(UsageMethod.FORCED, ur3.getUsageMethod());
        assertEquals("contingency-3", ((OnContingencyStateImpl) ur3).getContingency().getId());
        List<NetworkElement> networkElements = ara3.getNetworkElements().stream().sorted(Comparator.comparing(NetworkElement::getId)).toList();
        List<ElementaryAction> elementaryActions = ara3.getElementaryActions().stream().sorted(Comparator.comparing(ElementaryAction::hashCode)).toList();

        assertEquals("BBE1AA1  BBE4AA1  1", networkElements.get(0).getId());
        assertEquals(ActionType.OPEN, ((TopologicalAction) elementaryActions.get(0)).getActionType());

        assertEquals("BBE1AA1 _generator", networkElements.get(1).getId());
        assertEquals(100., ((InjectionSetpoint) elementaryActions.get(1)).getSetpoint());
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
