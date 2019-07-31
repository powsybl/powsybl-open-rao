package com.farao_community.farao.closed_optimisation_rao.fillers;

import com.farao_community.farao.closed_optimisation_rao.JsonClosedOptimisationRaoResultTest;
import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.json.JsonCracFile;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;

public class GeneratorRedispatchCostsFillerTest {

    GeneratorRedispatchCostsFiller generatorRedispatchCostsFiller;

    FilersUtilsTest filersUtilsTest;

    CracFile cracFile;

    @Before
    public void setup() {
        filersUtilsTest = new FilersUtilsTest();
        InputStream is = JsonClosedOptimisationRaoResultTest.class.getResourceAsStream("/4_2nodes_preContingency_RD_N-1.xiidm");
        Network net = Importers.loadNetwork("4_2nodes_preContingency_RD_N-1.xiidm", is);
        cracFile = JsonCracFile.read(CracFile.class.getResourceAsStream("/4_2nodes_preContingency_RD_N-1.json"));
        Map<String, Object> data = filersUtilsTest.getData();

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
        //generatorRedispatchCostsFiller.
        //TODO fillProblem()
    }
}
