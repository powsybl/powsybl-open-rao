package com.powsybl.openrao.searchtreerao.commons;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracioapi.CracImporters;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.raoapi.Rao;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;

public class ReportsTest {
    private static ReportNode buildNewRootNode() {
        return ReportNode.newRootReportNode().withMessageTemplate("Test report node", "This is a parent report node for report tests").build();
    }

    @Test
    void checkThatProvidedReportNodeIsUsed() throws IOException {
        ReportNode reportNode = buildNewRootNode();

        Network network = Network.read("small-network-2P.uct", getClass().getResourceAsStream("/network/small-network-2P.uct"));
        Crac crac = CracImporters.importCrac("crac/small-crac-2P.json", getClass().getResourceAsStream("/crac/small-crac-2P.json"), network);
        RaoInput raoInput = RaoInput.build(network, crac).build();
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json"));

        // Run RAO
        RaoResult raoResult = Rao.run(raoInput, raoParameters, null, reportNode);
        try (StringWriter writer = new StringWriter()) {
            reportNode.print(writer);
            System.out.println(writer);
        }
    }

    @Test
    void checkOtherReport() throws IOException {
        ReportNode reportNode = ReportNode.newRootReportNode()
                .withMessageTemplate("Test report node", "This is a parent report node for report tests")
                .build();

        Network network = Network.read("small-network-2P.uct", getClass().getResourceAsStream("/network/small-network-2P.uct"));
        Crac crac = CracImporters.importCrac("crac/small-crac-2P.json", getClass().getResourceAsStream("/crac/small-crac-2P.json"), network);
        RaoInput raoInput = RaoInput.build(network, crac).build();
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json"));

        // Run RAO
        RaoResult raoResult = Rao.run(raoInput, raoParameters, null, reportNode);
        try (StringWriter writer = new StringWriter()) {
            reportNode.print(writer);
            System.out.println(writer);
        }
    }
}
