package com.farao_community.farao.linear_rao.post_processors;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Test;

public class RaoResultPostProcessorTest {

    @Test
    public void fillResult() {

        // Arrange LinearRaoData
        // Crac crac = CracImporters.importCrac("little-crac.json", getClass().getResourceAsStream("/little-crac.json"));
        // todo : import Crac from JSON importer once it is repaired

        Network network = Importers.loadNetwork("TestCase12Nodes.uct", getClass().getResourceAsStream("/TestCase12Nodes.uct"));




    }
}
