package com.farao_community.farao.closed_optimisation_rao.fillers;

import com.farao_community.farao.closed_optimisation_rao.JsonClosedOptimisationRaoResultTest;
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

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;

public class BranchMarginsVariablesFillerTest {

    BranchMarginsVariablesFiller branchMarginsVariablesFiller;

    CracFile cracFile;

    @Before
    public void setUp() {
        InputStream is = JsonClosedOptimisationRaoResultTest.class.getResourceAsStream("/1_2nodes_preContingency_RD_N.xiidm");
        Network net = Importers.loadNetwork("1_2nodes_preContingency_RD_N.xiidm", is);
        cracFile = JsonCracFile.read(CracFile.class.getResourceAsStream("/1_2nodes_preContingency_RD_N.json"));
        Map<String, Object> data = new HashMap<>();

        branchMarginsVariablesFiller = new BranchMarginsVariablesFiller();
        branchMarginsVariablesFiller.initFiller(net, cracFile, data);

    }

    @Test
    public void test() {
        Map<String, Class> dataExcepted = branchMarginsVariablesFiller.dataExpected();
        assertNotNull(dataExcepted);

        List<String> constraintProvided = branchMarginsVariablesFiller.constraintsProvided();
        assertNotNull(constraintProvided);

        List<String> variablesProvided = branchMarginsVariablesFiller.variablesProvided();
        assertNotNull(variablesProvided);
        cracFile.getPreContingency().getMonitoredBranches().forEach(monitoredBranch -> {
            assertTrue(variablesProvided.contains(monitoredBranch.getId() + "_estimated_flow"));
            assertTrue(constraintProvided.contains(monitoredBranch.getId() + "_estimated_flow_equation"));
            dataExcepted.containsKey(monitoredBranch.getId() + "_estimated_flow_equation");
            // variablesProvided.contains(monitoredBranch.getId() + "_estimated_flow");
        });
        //TODO fillProblem()
    }
}
