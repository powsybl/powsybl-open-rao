package com.powsybl.openrao.data.crac.io.nc.craccreator.contingencies;

import com.powsybl.openrao.data.crac.io.commons.api.ElementaryCreationContext;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracCreationContext;
import com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracCreationTestUtil;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ContingenciesCreationTest {

    @Test
    void importContingencies() {
        NcCracCreationContext cracCreationContext = NcCracCreationTestUtil.getNcCracCreationContext("/profiles/contingencies/Contingencies.zip", NcCracCreationTestUtil.NETWORK);

        List<Contingency> importedContingencies = cracCreationContext.getCrac().getContingencies().stream().sorted(Comparator.comparing(Contingency::getId)).toList();
        assertEquals(8, importedContingencies.size());

        NcCracCreationTestUtil.assertContingencyEquality(importedContingencies.get(0), "contingency-1", "RTE_CO1", Set.of("FFR1AA1  FFR2AA1  1"));
        NcCracCreationTestUtil.assertContingencyEquality(importedContingencies.get(2), "contingency-2", "RTE_CO2", Set.of("FFR1AA1  FFR2AA1  1"));
        NcCracCreationTestUtil.assertContingencyEquality(importedContingencies.get(3), "contingency-3", "RTE_CO3", Set.of("FFR1AA1  FFR2AA1  1"));
        NcCracCreationTestUtil.assertContingencyEquality(importedContingencies.get(4), "contingency-4", "RTE_CO4", Set.of("FFR1AA1  FFR2AA1  1", "FFR1AA1  FFR3AA1  1"));
        NcCracCreationTestUtil.assertContingencyEquality(importedContingencies.get(5), "contingency-5", "contingency-5", Set.of("FFR1AA1  FFR2AA1  1"));
        NcCracCreationTestUtil.assertContingencyEquality(importedContingencies.get(6), "contingency-6", "CO6", Set.of("FFR1AA1  FFR2AA1  1"));
        NcCracCreationTestUtil.assertContingencyEquality(importedContingencies.get(7), "contingency-7", "contingency-7", Set.of("FFR1AA1  FFR2AA1  1"));
        NcCracCreationTestUtil.assertContingencyEquality(importedContingencies.get(1), "contingency-12", "RTE_CO12", Set.of("FFR1AA1  FFR2AA1  1"));

        assertEquals(1, cracCreationContext.getContingencyCreationContexts().stream().filter(ElementaryCreationContext::isAltered).toList().size());
        assertEquals("Incorrect contingent status for equipment(s): FFR1AA1  FFR3AA1  1, FFR1AA1  FFR4AA1  1. Missing contingent equipment(s) in network: missing-generator, missing-line.", cracCreationContext.getContingencyCreationContexts().stream().filter(ElementaryCreationContext::isAltered).toList().get(0).getImportStatusDetail());

        assertEquals(4, cracCreationContext.getContingencyCreationContexts().stream().filter(context -> !context.isImported()).toList().size());

        NcCracCreationTestUtil.assertContingencyNotImported(cracCreationContext, "contingency-8", ImportStatus.INCOMPLETE_DATA, "Contingency contingency-8 will not be imported because no contingency equipment is linked to that contingency");
        NcCracCreationTestUtil.assertContingencyNotImported(cracCreationContext, "contingency-9", ImportStatus.NOT_FOR_RAO, "Contingency contingency-9 will not be imported because its field mustStudy is set to false");
        NcCracCreationTestUtil.assertContingencyNotImported(cracCreationContext, "contingency-10", ImportStatus.INCONSISTENCY_IN_DATA, "Contingency contingency-10 will not be imported because all contingency equipments have an incorrect contingent status: FFR1AA1  FFR2AA1  1");
        NcCracCreationTestUtil.assertContingencyNotImported(cracCreationContext, "contingency-11", ImportStatus.INCONSISTENCY_IN_DATA, "Contingency contingency-11 will not be imported because all contingency equipments are missing in the network: unknown-network-element");
    }
}
