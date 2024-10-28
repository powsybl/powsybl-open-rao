package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TotalCostEvaluator implements CostEvaluator {
    private final Set<FlowCnec> flowCnecs;
    private final Unit unit;
    private final MarginEvaluator marginEvaluator;
    private final NetworkActionsCostEvaluator networkActionsCostEvaluator;
    private final RangeActionsCostEvaluator rangeActionsCostEvaluator;

    public TotalCostEvaluator(Set<FlowCnec> flowCnecs, Unit unit, MarginEvaluator marginEvaluator, NetworkActionsCostEvaluator networkActionsCostEvaluator, RangeActionsCostEvaluator rangeActionsCostEvaluator) {
        this.flowCnecs = flowCnecs;
        this.unit = unit;
        this.marginEvaluator = marginEvaluator;
        this.networkActionsCostEvaluator = networkActionsCostEvaluator;
        this.rangeActionsCostEvaluator = rangeActionsCostEvaluator;
    }

    @Override
    public String getName() {
        return "total-cost-evaluator";
    }

    @Override
    public Pair<Double, List<FlowCnec>> computeCostAndLimitingElements(FlowResult flowResult, Set<String> contingenciesToExclude) {
        double totalCost = networkActionsCostEvaluator.getTotalCost() + rangeActionsCostEvaluator.getTotalCost();
        List<FlowCnec> limitingElements = getCostlyElements(flowResult, contingenciesToExclude);
        return Pair.of(totalCost, limitingElements);
    }

    private List<FlowCnec> getCostlyElements(FlowResult flowResult, Set<String> contingenciesToExclude) {
        Map<FlowCnec, Double> margins = new HashMap<>();

        flowCnecs.stream()
            .filter(cnec -> cnec.getState().getContingency().isEmpty() || !contingenciesToExclude.contains(cnec.getState().getContingency().get().getId()))
            .filter(Cnec::isOptimized)
            .forEach(flowCnec -> margins.put(flowCnec, marginEvaluator.getMargin(flowResult, flowCnec, unit)));

        return margins.keySet().stream()
            .filter(Cnec::isOptimized)
            .sorted(Comparator.comparing(margins::get))
            .toList();
    }

    @Override
    public Unit getUnit() {
        return unit;
    }

    @Override
    public Set<FlowCnec> getFlowCnecs() {
        return flowCnecs;
    }
}
