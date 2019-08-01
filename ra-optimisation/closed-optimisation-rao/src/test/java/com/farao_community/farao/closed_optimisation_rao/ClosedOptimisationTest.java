package com.farao_community.farao.closed_optimisation_rao;

import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.json.JsonCracFile;
import com.farao_community.farao.ra_optimisation.*;
import com.powsybl.commons.config.ComponentDefaultConfig;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Test;

import java.io.InputStream;

import static junit.framework.TestCase.assertEquals;

public class ClosedOptimisationTest {
    // ? : To put in this folder or in ra-optimisation/ra-optimisation-api ?
    private static double tolerance = 1e-4;

    private RaoComputationResult runRaoComputation(String networkFileName, String cracFileName) {
        CracFile cracFile = JsonCracFile.read(CracFile.class.getResourceAsStream(cracFileName));

        InputStream is = JsonClosedOptimisationRaoResultTest.class.getResourceAsStream(networkFileName);
        Network net = Importers.loadNetwork(networkFileName, is);

        RaoComputation rao = ComponentDefaultConfig.load().newFactoryImpl(RaoComputationFactory.class).create(net, cracFile, LocalComputationManager.getDefault(), 0);

        // Default config might not work on another commputer (?)
        RaoComputationParameters parameters = RaoComputationParameters.load(PlatformConfig.defaultConfig());
        return rao.run(net.getVariantManager().getWorkingVariantId(), parameters).join();
    }

    @Test
    public void simpleCaseWithPrecontingencyAndRDPreventiveRA() {

        /*
            Simple case with two nodes, precontingencies and preventive redispatching remedial actions
         */

        String testName = "/5_3nodes_preContingency_PSTandRD_N-1";
        RaoComputationResult result = runRaoComputation(testName + ".xiidm", testName + ".json");

        assertEquals(-800, result.getPreContingencyResult().getMonitoredBranchResults().get(0).getPreOptimisationFlow(), tolerance);
        assertEquals(-500, result.getPreContingencyResult().getMonitoredBranchResults().get(0).getPostOptimisationFlow(), tolerance);
        assertEquals(1300, result.getPreContingencyResult().getRemedialActionResults().stream()
                .filter(remedialActionResult -> remedialActionResult.getId().equals("BELGIAN_GENERATOR_RA")).findFirst().get().getRemedialActionElementResults()
                .stream().map(remedialActionElementResult -> (RedispatchElementResult) remedialActionElementResult).findFirst()
                .get().getPostOptimisationTargetP(), tolerance);
        assertEquals(1000, result.getPreContingencyResult().getRemedialActionResults().stream()
                .filter(remedialActionResult -> remedialActionResult.getId().equals("FRENCH_GENERATOR_RA")).findFirst().get().getRemedialActionElementResults()
                .stream().map(remedialActionElementResult -> (RedispatchElementResult) remedialActionElementResult).findFirst()
                .get().getPostOptimisationTargetP(), tolerance);
    }

}
