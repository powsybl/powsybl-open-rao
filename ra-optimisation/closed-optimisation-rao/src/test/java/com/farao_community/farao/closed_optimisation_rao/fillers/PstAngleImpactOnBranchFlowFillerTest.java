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

import static org.junit.Assert.assertNotNull;

public class PstAngleImpactOnBranchFlowFillerTest {

    PstAngleImpactOnBranchFlowFiller pstAngleImpactOnBranchFlowFiller;

    @Before
    public void setUp() {
        InputStream is = JsonClosedOptimisationRaoResultTest.class.getResourceAsStream("/1_2nodes_preContingency_RD_N.xiidm");
        Network net = Importers.loadNetwork("1_2nodes_preContingency_RD_N.xiidm", is);
        CracFile cracFile = JsonCracFile.read(CracFile.class.getResourceAsStream("/1_2nodes_preContingency_RD_N.json"));
        Map<String, Object> data = new HashMap<>();

        pstAngleImpactOnBranchFlowFiller = new PstAngleImpactOnBranchFlowFiller();
        pstAngleImpactOnBranchFlowFiller.initFiller(net, cracFile, data);
    }

    @Test
    public void test() {
        List<String> constraintsExpected = pstAngleImpactOnBranchFlowFiller.constraintsExpected();
        assertNotNull(constraintsExpected);

        Map<String, Class> dataExpected = pstAngleImpactOnBranchFlowFiller.dataExpected();
        assertNotNull(dataExpected);

        List<String> variablesExpected = pstAngleImpactOnBranchFlowFiller.variablesExpected();
        assertNotNull(variablesExpected);
        //TODO fillProblem()
    }
}
