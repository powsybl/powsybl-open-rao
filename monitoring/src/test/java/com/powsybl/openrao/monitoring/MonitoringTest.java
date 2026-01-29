/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring;

import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.monitoring.results.MonitoringResult;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.castor.algorithm.Castor;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class MonitoringTest {

    @Test
    void testRunMonitoringOnMultiCurative() throws IOException {
        // Defined in the CRAC 3 curative instant, 1 contingency coL1
        // => the last curative instant optimized for contingency coL1 is curative 2 (don't have any curative 3 cnecs defined).
        // 3 voltage CNECs are defined one in preventive, one in curative 1 and one in curative 2.
        // preventive => ok, curative 2 => ok but curative 1 => fail. We only monitor final curative instant.

        Monitoring monitoring = new Monitoring("OpenLoadFlow", new LoadFlowParameters());

        Network network = Network.read("voltage_monitoring.xiidm", getClass().getResourceAsStream("/voltage_monitoring.xiidm"));
        Crac crac = Crac.read("voltage_monitoring_with_multicurative_cnec.json", getClass().getResourceAsStream("/voltage_monitoring_with_multicurative_cnec.json"), network);
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/monitoring_parameters.json"));

        RaoResult raoResult = new Castor().run(RaoInput.build(network, crac).build(), raoParameters).join();

        MonitoringInput monitoringInput = new MonitoringInput.MonitoringInputBuilder()
            .withCrac(crac)
            .withNetwork(network)
            .withRaoResult(raoResult)
            .withPhysicalParameter(PhysicalParameter.VOLTAGE)
            .build();

        MonitoringResult monitoringResult = monitoring.runMonitoring(monitoringInput, 1);
        assertEquals(3, monitoringResult.getCnecResults().size());
        assertEquals(Cnec.SecurityStatus.FAILURE, monitoringResult.getStatus());

        assertEquals(4.30, monitoringResult.getCnecResults().stream().filter(cnecResult -> cnecResult.getCnec().getId().equals("vc - preventive")).findFirst().get().getMargin(), 1e-2);
        assertEquals(1.88, monitoringResult.getCnecResults().stream().filter(cnecResult -> cnecResult.getCnec().getId().equals("vc - curative2")).findFirst().get().getMargin(), 1e-2);
        assertEquals(Double.NaN, monitoringResult.getCnecResults().stream().filter(cnecResult -> cnecResult.getCnec().getId().equals("vc - curative1")).findFirst().get().getMargin(), 1e-2);
    }
}