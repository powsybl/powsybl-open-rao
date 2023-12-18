/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.flowbased_computation.json;

import com.powsybl.open_rao.data.flowbased_domain.DataDomain;
import com.powsybl.open_rao.flowbased_computation.FlowbasedComputationResult;
import com.powsybl.open_rao.flowbased_computation.FlowbasedComputationResultImpl;
import com.powsybl.commons.test.AbstractSerDeTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class JsonFlowbasedComputationResultTest extends AbstractSerDeTest {
    @Test
    void roundTripDefault() throws IOException {
        FlowbasedComputationResult result = new FlowbasedComputationResultImpl(FlowbasedComputationResult.Status.SUCCESS, new DataDomain("id", "name", "format", "description", null, Collections.emptyList(), Collections.emptyList()));
        roundTripTest(result, JsonFlowbasedComputationResult::write, JsonFlowbasedComputationResult::read, "/FlowbasedComputationResult.json");
    }
}
