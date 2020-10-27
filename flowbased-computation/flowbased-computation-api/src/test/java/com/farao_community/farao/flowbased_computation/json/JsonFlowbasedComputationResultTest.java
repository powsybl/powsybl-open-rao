/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation.json;

import com.farao_community.farao.data.flowbased_domain.DataDomain;
import com.farao_community.farao.flowbased_computation.FlowbasedComputationResult;
import com.farao_community.farao.flowbased_computation.FlowbasedComputationResultImpl;
import com.powsybl.commons.AbstractConverterTest;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class JsonFlowbasedComputationResultTest extends AbstractConverterTest {
    @Test
    public void roundTripDefault() throws IOException {
        FlowbasedComputationResult result = new FlowbasedComputationResultImpl(FlowbasedComputationResult.Status.SUCCESS, new DataDomain("id", "name", "format", "description", null, Collections.emptyList()));
        roundTripTest(result, JsonFlowbasedComputationResult::write, JsonFlowbasedComputationResult::read, "/FlowbasedComputationResult.json");
    }
}
