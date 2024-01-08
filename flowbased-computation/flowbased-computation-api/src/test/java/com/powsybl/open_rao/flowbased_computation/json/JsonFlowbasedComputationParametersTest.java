/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.flowbased_computation.json;

import com.powsybl.open_rao.flowbased_computation.FlowbasedComputationParameters;
import com.powsybl.commons.test.AbstractSerDeTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class JsonFlowbasedComputationParametersTest extends AbstractSerDeTest {
    @Test
    void roundTripDefault() throws IOException {
        FlowbasedComputationParameters parameters = new FlowbasedComputationParameters();
        roundTripTest(parameters, JsonFlowbasedComputationParameters::write, JsonFlowbasedComputationParameters::read, "/FlowbasedComputationParameters.json");
    }
}
