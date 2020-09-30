package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_loopflow_extension.CnecLoopFlowExtension;
import com.farao_community.farao.data.crac_result_extensions.CnecResult;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoopFlowViolationCostEvaluator implements CostEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoopFlowViolationCostEvaluator.class);

    private static final double MAX_LOOP_FLOW_VIOLATION_COST = 1000000.0;
    private double violationCost;

    LoopFlowViolationCostEvaluator(double violationCost) {
        if (Math.abs(violationCost)>0) {
            this.violationCost = violationCost;
        } else {
            this.violationCost = MAX_LOOP_FLOW_VIOLATION_COST;
        }
    }

    @Override
    public double getCost(RaoData raoData) {
        double cost = raoData.getCnecs().stream()
            .filter(cnec -> !cnec.getState().getContingency().isPresent()) // preventive state
            .filter(cnec -> cnec.getExtension(CnecLoopFlowExtension.class) != null) // with loop-flow extension
            .mapToDouble(cnec -> getLoopFlowExcess(raoData, cnec) * violationCost)
            .sum();

        if (cost > 0) {
            LOGGER.info("Some loopflow constraints are not respected.");
        }

        return cost;
    }

    @Override
    public Unit getUnit() {
        return Unit.MEGAWATT;
    }

    private double getLoopFlowExcess(RaoData raoData, Cnec cnec) {
        CnecResult cnecResult = cnec.getExtension(CnecResultExtension.class).getVariant(raoData.getWorkingVariantId());
        return Math.max(0, Math.abs(cnecResult.getLoopflowInMW()) - Math.abs(cnecResult.getLoopflowThresholdInMW()));
    }
}
