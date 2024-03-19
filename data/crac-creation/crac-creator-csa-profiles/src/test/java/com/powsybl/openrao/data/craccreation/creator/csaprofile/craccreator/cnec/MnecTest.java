package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.cnec;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import com.powsybl.openrao.data.craccreation.creator.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationContext;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileElementaryCreationContext;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.parameters.CsaCracCreationParameters;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.getCsaCracCreationContext;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.getNetworkFromResource;
import static org.junit.jupiter.api.Assertions.*;

public class MnecTest {

    @Test
    void checkOnFlowConstraintUsageRule() {
        Network network = getNetworkFromResource("/SecuredAndScannedAssessedElement.zip");
        CracCreationParameters cracCreationParameters = new CracCreationParameters();
        cracCreationParameters.addExtension(CsaCracCreationParameters.class, new CsaCracCreationParameters());
        cracCreationParameters.getExtension(CsaCracCreationParameters.class).setCapacityCalculationRegionEicCode("10Y1001C–00095L");

        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/SecuredAndScannedAssessedElement.zip", network, OffsetDateTime.parse("2023-03-29T12:00Z"), cracCreationParameters);

        assertEquals(7, cracCreationContext.getCrac().getFlowCnecs().size());
        assertTrue(cracCreationContext.getCrac().getFlowCnec("RTE_AE2 (ae-2) - preventive").isOptimized());
        assertFalse(cracCreationContext.getCrac().getFlowCnec("RTE_AE2 (ae-2) - preventive").isMonitored());

        assertFalse(cracCreationContext.getCrac().getFlowCnec("RTE_AE3 (ae-3) - preventive").isOptimized());
        assertTrue(cracCreationContext.getCrac().getFlowCnec("RTE_AE3 (ae-3) - preventive").isMonitored());

        assertTrue(cracCreationContext.getCrac().getFlowCnec("RTE_AE5 (ae-5) - preventive").isOptimized());
        assertFalse(cracCreationContext.getCrac().getFlowCnec("RTE_AE5 (ae-5) - preventive").isMonitored());

        assertFalse(cracCreationContext.getCrac().getFlowCnec("RTE_AE6 (ae-6) - preventive").isOptimized());
        assertFalse(cracCreationContext.getCrac().getFlowCnec("RTE_AE6 (ae-6) - preventive").isMonitored());

        assertFalse(cracCreationContext.getCrac().getFlowCnec("RTE_AE7 (ae-7) - preventive").isOptimized());
        assertTrue(cracCreationContext.getCrac().getFlowCnec("RTE_AE7 (ae-7) - preventive").isMonitored());

        assertFalse(cracCreationContext.getCrac().getFlowCnec("RTE_AE8 (ae-8) - preventive").isOptimized());
        assertFalse(cracCreationContext.getCrac().getFlowCnec("RTE_AE8 (ae-8) - preventive").isMonitored());

        assertFalse(cracCreationContext.getCrac().getFlowCnec("RTE_AE9 (ae-9) - preventive").isOptimized());
        assertFalse(cracCreationContext.getCrac().getFlowCnec("RTE_AE9 (ae-9) - preventive").isMonitored());


        List<CsaProfileElementaryCreationContext> notImportedCnecCreationContexts = cracCreationContext.getCnecCreationContexts().stream().filter(c -> !c.isImported())
            .sorted(Comparator.comparing(CsaProfileElementaryCreationContext::getNativeId)).toList();
        assertEquals(2, notImportedCnecCreationContexts.size());

        assertEquals("RTE_AE1 (ae-1) - preventive", notImportedCnecCreationContexts.get(0).getNativeId());
        assertEquals(ImportStatus.INCONSISTENCY_IN_DATA, notImportedCnecCreationContexts.get(0).getImportStatus());
        assertEquals("AssessedElement RTE_AE1 (ae-1) - preventive will not be imported because an AssessedElement cannot be optimized and monitored at the same time.", notImportedCnecCreationContexts.get(0).getImportStatusDetail());

        assertEquals("RTE_AE4 (ae-4) - preventive", notImportedCnecCreationContexts.get(1).getNativeId());
        assertEquals(ImportStatus.INCONSISTENCY_IN_DATA, notImportedCnecCreationContexts.get(1).getImportStatus());
        assertEquals("AssessedElement RTE_AE1 (ae-1) - preventive will not be imported because an AssessedElement cannot be optimized and monitored at the same time.", notImportedCnecCreationContexts.get(0).getImportStatusDetail());

    }
}
