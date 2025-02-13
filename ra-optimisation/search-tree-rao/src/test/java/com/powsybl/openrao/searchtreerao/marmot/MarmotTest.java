package com.powsybl.openrao.searchtreerao.marmot;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.intertemporalconstraint.PowerGradient;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.InterTemporalRaoInput;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;

class MarmotTest {
    // TODO: no gradients 2 TS + compare to 2 independent castor runs
    // TODO: gradient 2 TS

    @Test
    void runCaseWithTwoTimestampsAndNoGradient() throws IOException {
        // we need to import twice the network to avoid variant names conflicts on the same network object
        Network network1 = Network.read("/network/2Nodes2ParallelLinesPST.uct", MarmotTest.class.getResourceAsStream("/network/2Nodes2ParallelLinesPST.uct"));
        Network network2 = Network.read("/network/2Nodes2ParallelLinesPST.uct", MarmotTest.class.getResourceAsStream("/network/2Nodes2ParallelLinesPST.uct"));
        Crac crac1 = Crac.read("/crac/crac-20250213.json", MarmotTest.class.getResourceAsStream("/crac/crac-20250213.json"), network1);
        Crac crac2 = Crac.read("/crac/crac-20250214.json", MarmotTest.class.getResourceAsStream("/crac/crac-20250214.json"), network2);
        RaoParameters raoParameters = JsonRaoParameters.read(MarmotTest.class.getResourceAsStream("/parameters/RaoParameters_dc_minObjective_discretePst.json"));

        InterTemporalRaoInput input = new InterTemporalRaoInput(
            new TemporalDataImpl<>(Map.of(OffsetDateTime.of(2025, 2, 13, 11, 35, 0, 0, ZoneOffset.UTC), RaoInput.build(network1, crac1).build(), OffsetDateTime.of(2025, 2, 14, 11, 35, 0, 0, ZoneOffset.UTC), RaoInput.build(network2, crac2).build())),
            Set.of(new PowerGradient("FFR1AA1 _generator", -1000d, 1000d))
        );

        // first RAOs shift tap to -5 for a cost of 55 each
        // MARMOT should also move the tap to -5 for both timestamps with a total cost of 110
        TemporalData<RaoResult> results = new Marmot().run(input, raoParameters).join();
    }
}
