/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * FlowBased Computation Impl Test
 *
 * @author Luc Di Gallo {@literal <luc.di-gallo at rte-france.com>}
 */
public class FlowBasedComputationImplTest {

    private FlowBasedComputationImpl flowBasedComputationMock;

    @Before
    public void setup() {
        flowBasedComputationMock = Mockito.mock(FlowBasedComputationImpl.class);
    }

    @Test
    public void runTest() {
        FlowBasedComputationParameters parameters = Mockito.mock(FlowBasedComputationParameters.class);
        String workingStateId = "1";
        flowBasedComputationMock.run(workingStateId, parameters);
    }
}
