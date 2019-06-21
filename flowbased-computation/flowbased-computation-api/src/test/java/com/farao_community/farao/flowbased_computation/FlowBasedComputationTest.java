package com.farao_community.farao.flowbased_computation;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class FlowBasedComputationTest {

    private FlowBasedComputation flowBasedComputationMock;

    @Before
    public void setup() {
        flowBasedComputationMock = Mockito.mock(FlowBasedComputation.class);
    }

    @Test
    public void runTest() {
        FlowBasedComputationParameters parameters = Mockito.mock(FlowBasedComputationParameters.class);
        String workingStateId = "1";
        flowBasedComputationMock.run(workingStateId, parameters);
    }
}
