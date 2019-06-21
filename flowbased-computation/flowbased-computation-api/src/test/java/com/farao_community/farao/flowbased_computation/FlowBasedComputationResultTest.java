/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation;

import com.farao_community.farao.data.flowbased_domain.DataDomain;
import com.farao_community.farao.data.flowbased_domain.DataPreContingency;

import org.junit.Assert;
import org.junit.Test;

/**
 * FlowBased Computation Result Test
 *
 * @author Luc Di Gallo {@literal <luc.di-gallo at rte-france.com>}
 */
public class FlowBasedComputationResultTest {

    @Test
    public void run() {
        FlowBasedComputationResult result = new FlowBasedComputationResult(FlowBasedComputationResult.Status.FAILURE);
        Assert.assertTrue(result.getPtdflist().isEmpty());
        Assert.assertTrue(result.createDataPreContingency() instanceof DataPreContingency);
        Assert.assertTrue(result.createDataDomain() instanceof DataDomain);
        Assert.assertEquals(result.getStatus(), FlowBasedComputationResult.Status.FAILURE);
    }

}
