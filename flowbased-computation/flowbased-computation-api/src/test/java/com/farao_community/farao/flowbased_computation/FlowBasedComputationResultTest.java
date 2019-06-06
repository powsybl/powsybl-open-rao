package com.farao_community.farao.flowbased_computation;

import com.farao_community.farao.data.flowbased_domain.DataDomain;
import com.farao_community.farao.data.flowbased_domain.DataPreContingency;

import org.junit.Assert;
import org.junit.Test;

public class FlowBasedComputationResultTest {

    @Test
    public void run() {
        FlowBasedComputationResult result = new FlowBasedComputationResult(FlowBasedComputationResult.Status.FAILURE);
        Assert.assertTrue(result.getPtdflist().isEmpty());
        Assert.assertTrue(result.createDataPreContingency() instanceof DataPreContingency);
        Assert.assertTrue(result.createDataDomain() instanceof DataDomain);
    }

}
