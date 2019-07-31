package com.farao_community.farao.closed_optimisation_rao.fillers;

import com.farao_community.farao.closed_optimisation_rao.JsonClosedOptimisationRaoResultTest;
import com.farao_community.farao.closed_optimisation_rao.MPSolverMock;
import com.farao_community.farao.data.crac_file.Contingency;
import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.json.JsonCracFile;
import com.google.ortools.linearsolver.MPSolver;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static groovy.util.GroovyTestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;

public class BranchMarginsPositivityConstraintFillerTest {

    BranchMarginsPositivityConstraintFiller branchMarginsPositivityConstraintFiller;

    FilersUtilsTest filersUtilsTest;

    CracFile cracFile;

    @Before
    public void setup() {
        filersUtilsTest = new FilersUtilsTest();
        InputStream is = JsonClosedOptimisationRaoResultTest.class.getResourceAsStream("/4_2nodes_preContingency_RD_N-1.xiidm");
        Network net = Importers.loadNetwork("4_2nodes_preContingency_RD_N-1.xiidm", is);
        cracFile = JsonCracFile.read(CracFile.class.getResourceAsStream("/4_2nodes_preContingency_RD_N-1.json"));
        Map<String, Object> data = filersUtilsTest.getData();

        long size = cracFile.getPreContingency().getMonitoredBranches().size() + cracFile.getContingencies().stream().map(Contingency::getMonitoredBranches).flatMap(monitoredBranches -> monitoredBranches.stream()).count();
        branchMarginsPositivityConstraintFiller = new BranchMarginsPositivityConstraintFiller();
        branchMarginsPositivityConstraintFiller.initFiller(net, cracFile, data);
    }

    @Test
    public void test() {
        MPSolver solver = new MPSolverMock();

        solver.makeVar(-266.667, 40, false, "estimated_flow");
        List<String> variablesExpected = branchMarginsPositivityConstraintFiller.variablesExpected();
        branchMarginsPositivityConstraintFiller.fillProblem(solver);
        assertNotNull(variablesExpected);

        long size = cracFile.getPreContingency().getMonitoredBranches().size() + cracFile.getContingencies().stream().map(Contingency::getMonitoredBranches).flatMap(monitoredBranches -> monitoredBranches.stream()).count();
        assertEquals(variablesExpected.size(), size);
        cracFile.getPreContingency().getMonitoredBranches().forEach(monitoredBranch -> {
            assertTrue(variablesExpected.contains(monitoredBranch.getId() + "_estimated_flow"));
        });
        //TODO fillProblem()
    }
}
