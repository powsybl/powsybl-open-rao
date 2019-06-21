package com.farao_community.farao.flowbased_computation;

import com.farao_community.farao.data.crac_file.CracFile;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.time.Instant;

public class FlowBasedComputationFactoryImplTest {

    private FlowBasedComputationFactoryImpl flowBasedComputationFactoryMock;
    private Network network;
    private CracFile cracFile;
    private FlowBasedGlskValuesProvider flowBasedGlskValuesProvider;
    private ComputationManager computationManager;

    @Before
    public void setup() {
        flowBasedComputationFactoryMock = Mockito.mock(FlowBasedComputationFactoryImpl.class);
        network = Mockito.mock(Network.class);
        cracFile = Mockito.mock(CracFile.class);
        flowBasedGlskValuesProvider = Mockito.mock(FlowBasedGlskValuesProvider.class);
        computationManager = Mockito.mock(ComputationManager.class);
    }

    @Test
    public void runTest() {
        Instant instant = Instant.parse("2018-08-28T22:00:00Z");
        ComputationManager computationManager = Mockito.mock(ComputationManager.class);
        flowBasedComputationFactoryMock.create(network, cracFile, flowBasedGlskValuesProvider, instant, computationManager, 0);
    }

    @Test
    public void runTestBis() {
        flowBasedComputationFactoryMock.create(network, cracFile, computationManager, 0);
    }

}
