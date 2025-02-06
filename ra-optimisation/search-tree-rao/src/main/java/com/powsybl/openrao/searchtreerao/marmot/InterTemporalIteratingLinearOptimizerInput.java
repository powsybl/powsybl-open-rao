package com.powsybl.openrao.searchtreerao.marmot;

import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.data.intertemporalconstraint.PowerGradient;
import com.powsybl.openrao.searchtreerao.linearoptimisation.inputs.IteratingLinearOptimizerInput;

import java.util.Set;

public record InterTemporalIteratingLinearOptimizerInput(TemporalData<IteratingLinearOptimizerInput> iteratingLinearOptimizerInputs, Set<PowerGradient> powerGradients) {
    // TOOD: add builder?
}
