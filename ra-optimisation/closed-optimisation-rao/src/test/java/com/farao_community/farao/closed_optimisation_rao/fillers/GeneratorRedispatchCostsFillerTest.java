package com.farao_community.farao.closed_optimisation_rao.fillers;

import com.farao_community.farao.closed_optimisation_rao.JsonClosedOptimisationRaoResultTest;
import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.RedispatchRemedialActionElement;
import com.farao_community.farao.data.crac_file.json.JsonCracFile;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;

public class GeneratorRedispatchCostsFillerTest {

    GeneratorRedispatchCostsFiller generatorRedispatchCostsFiller;

    CracFile cracFile;

    @Before
    public void setUp() {
        InputStream is = JsonClosedOptimisationRaoResultTest.class.getResourceAsStream("/1_2nodes_preContingency_RD_N.xiidm");
        Network net = Importers.loadNetwork("1_2nodes_preContingency_RD_N.xiidm", is);
        cracFile = JsonCracFile.read(CracFile.class.getResourceAsStream("/1_2nodes_preContingency_RD_N.json"));
        Map<String, Object> data = new HashMap<>();

        generatorRedispatchCostsFiller = new GeneratorRedispatchCostsFiller();
        generatorRedispatchCostsFiller.initFiller(net, cracFile, data);
    }

    @Test
    public void test() {
        List<String> variablesExpected = generatorRedispatchCostsFiller.variablesExpected();
        assertNotNull(variablesExpected);
        System.out.println(variablesExpected.toString());

        List<String> variablesProvided = generatorRedispatchCostsFiller.variablesProvided();
        assertNotNull(variablesExpected);

        System.out.println(cracFile.toString());

        generatorRedispatchCostsFiller.
        //TODO fillProblem()
    }
}
