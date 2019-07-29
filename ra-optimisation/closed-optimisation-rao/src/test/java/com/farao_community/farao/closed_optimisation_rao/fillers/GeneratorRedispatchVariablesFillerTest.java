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


/**
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
public class GeneratorRedispatchVariablesFillerTest {

    GeneratorRedispatchVariablesFiller generatorRedispatchVariablesFiller;

    @Before
    public void setup() {
        InputStream is = JsonClosedOptimisationRaoResultTest.class.getResourceAsStream("/1_2nodes_preContingency_RD_N.xiidm");
        Network net = Importers.loadNetwork("1_2nodes_preContingency_RD_N.xiidm", is);
        CracFile cracFile = JsonCracFile.read(CracFile.class.getResourceAsStream("/1_2nodes_preContingency_RD_N.json"));
        Map<String, Object> data = new HashMap<>();

        generatorRedispatchVariablesFiller = new GeneratorRedispatchVariablesFiller();
        generatorRedispatchVariablesFiller
                .initFiller(net, cracFile, data);
    }

    @Test
    public void test() {
        List<String> result = generatorRedispatchVariablesFiller.variablesProvided();
        //TODO fillProblem()
    }

}