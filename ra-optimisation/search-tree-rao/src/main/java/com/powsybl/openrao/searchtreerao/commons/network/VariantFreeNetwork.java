package com.powsybl.openrao.searchtreerao.commons.network;

import com.powsybl.action.Action;
import com.powsybl.contingency.ContingencyContext;
import com.powsybl.contingency.strategy.OperatorStrategy;
import com.powsybl.contingency.strategy.condition.TrueCondition;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.searchtreerao.commons.SensitivityComputer;
import com.powsybl.sensitivity.SensitivityAnalysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

public class VariantFreeNetwork extends AbstractBufferedActionsNetworkVariant {

    public VariantFreeNetwork(Network network) {
        super(network);
    }

    @Override
    public void removeWorkingVariants() {
        // nothing to do
    }
//
//    public static OperatorStrategy fromNetworkAction(List<NetworkAction> networkActions, State state, Map<String, Action> actionsById) {
//
//        for (var elementaryAction : action.getElementaryActions()) {
//            if (!actionsById.containsKey(elementaryAction.getId())) {
//                actionsById.put(elementaryAction.getId(), elementaryAction);
//            }
//        }
//        ContingencyContext contingencyContext = state.isPreventive() ? ContingencyContext.none()
//                : ContingencyContext.specificContingency(state.getContingency().orElseThrow().getId());
//        TECHNICAL_LOGS.info("Creating operator strategy for network action '{}'", action.getId());
//        return new OperatorStrategy(action.getId(),
//                contingencyContext,
//                new TrueCondition(),
//                action.getElementaryActions().stream().map(Action::getId).toList());
//    }

    @Override
    public void compute(SensitivityComputer sensitivityComputer) {
        checkWorkingVariantIsSet();
//        for (AppliedRangeAction appliedRangeAction : workingVariant.appliedRangeActions()) {
//            throw new OpenRaoException(String.format("Range action %s not supported", appliedRangeAction.rangeAction().getId()));
//        }
//        List<Action> actions = workingVariant.networkActions().stream()
//                .flatMap(networkAction -> networkAction.getElementaryActions().stream())
//                .toList();
//        List<OperatorStrategy> operatorStrategies = new ArrayList<>();
//        ContingencyContext contingencyContext = ContingencyContext.none();
//        for (NetworkAction networkAction : workingVariant.networkActions()) {
//            operatorStrategies.add(new OperatorStrategy(networkAction.getId(),
//                contingencyContext,
//                new TrueCondition(),
//                networkAction.getElementaryActions().stream().map(Action::getId).toList()));
//        }
//        sensitivityComputer.compute(network);
    }
}
