package com.farao_community.farao.flowbased_computation;

import com.farao_community.farao.data.crac_file.CracFile;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import org.junit.Test;
import org.mockito.Mockito;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class FlowBasedComputationTest {

    @Test(expected = NullPointerException.class)
    public void run() {
        FlowBasedComputationFactory factory = Mockito.mock(FlowBasedComputationFactory.class);

        FlowBasedComputation flowBasedComputation = factory
                .create(any(Network.class),
                        any(CracFile.class),
                        any(FlowBasedGlskValuesProvider.class),
                        any(Instant.class),
                        any(ComputationManager.class),
                        any(int.class));

        when(flowBasedComputation.run(anyString(), any(FlowBasedComputationParameters.class)));
    }
}
