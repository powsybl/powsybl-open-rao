package com.farao_community.farao.closed_optimisation_rao.fillers;

import com.farao_community.farao.closed_optimisation_rao.JsonClosedOptimisationRaoResultTest;
import com.farao_community.farao.data.crac_file.Contingency;
import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.json.JsonCracFile;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static groovy.util.GroovyTestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;

public class BranchMarginsPositivityConstraintFillerTest {

    BranchMarginsPositivityConstraintFiller branchMarginsPositivityConstraintFiller;

    CracFile cracFile;

    @Before
    public void setup() {
        InputStream is = JsonClosedOptimisationRaoResultTest.class.getResourceAsStream("/1_2nodes_preContingency_RD_N.xiidm");
        Network net = Importers.loadNetwork("1_2nodes_preContingency_RD_N.xiidm", is);
        cracFile = JsonCracFile.read(CracFile.class.getResourceAsStream("/1_2nodes_preContingency_RD_N.json"));
        Map<String, Object> data = new HashMap<>();

        long size = cracFile.getPreContingency().getMonitoredBranches().size() + cracFile.getContingencies().stream().map(Contingency::getMonitoredBranches).flatMap(monitoredBranches -> monitoredBranches.stream()).count();
        branchMarginsPositivityConstraintFiller = new BranchMarginsPositivityConstraintFiller();
        branchMarginsPositivityConstraintFiller.initFiller(net, cracFile, data);
    }

    @Test
    public void test() {
        List<String> variablesExpected = branchMarginsPositivityConstraintFiller.variablesExpected();
        assertNotNull(variablesExpected);

        long size = cracFile.getPreContingency().getMonitoredBranches().size() + cracFile.getContingencies().stream().map(Contingency::getMonitoredBranches).flatMap(monitoredBranches -> monitoredBranches.stream()).count();
        assertEquals(variablesExpected.size(), size);
        cracFile.getPreContingency().getMonitoredBranches().forEach(monitoredBranch -> {
            assertTrue(variablesExpected.contains(monitoredBranch.getId() + "_estimated_flow"));
        });
        //TODO fillProblem()
    }
}
