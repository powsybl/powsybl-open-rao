package com.farao_community.farao.rao_commons.linear_optimisation.fillers;

import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.farao_community.farao.rao_commons.result_api.FlowResult;
import com.farao_community.farao.rao_commons.result_api.RangeActionResult;
import com.farao_community.farao.rao_commons.result_api.SensitivityResult;
import com.google.ortools.linearsolver.MPConstraint;
import com.powsybl.iidm.network.Network;

import java.util.Optional;
import java.util.Set;

public class DiscretePstGroupFiller implements ProblemFiller {

    private final Set<PstRangeAction> pstRangeActions;
    private final Network network;

    public DiscretePstGroupFiller(Network network, Set<PstRangeAction> pstRangeActions) {
        this.pstRangeActions = pstRangeActions;
        this.network = network;
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult) {
        pstRangeActions.forEach(rangeAction -> buildRangeActionGroupConstraint(linearProblem, rangeAction));
    }

    @Override
    public void update(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionResult rangeActionResult) {
        // nothing to do
        // todo: update second member of constraints
    }

    private void buildRangeActionGroupConstraint(LinearProblem linearProblem, PstRangeAction pstRangeAction) {
        Optional<String> optGroupId = pstRangeAction.getGroupId();
        if (optGroupId.isPresent()) {
            String groupId = optGroupId.get();
            // For the first time the group ID is encountered a common variable for the tap has to be created
            if (linearProblem.getRangeActionGroupTapVariable(groupId) == null) {
                linearProblem.addRangeActionGroupTapVariable(-LinearProblem.infinity(), LinearProblem.infinity(), groupId);
            }
            addRangeActionGroupConstraint(linearProblem, pstRangeAction, groupId);
        }
    }

    private void addRangeActionGroupConstraint(LinearProblem linearProblem, PstRangeAction pstRangeAction, String groupId) {
        double currentTap = pstRangeAction.getCurrentTapPosition(network);
        MPConstraint groupSetPointConstraint = linearProblem.addPstRangeActionGroupTapConstraint(currentTap, currentTap, pstRangeAction);
        groupSetPointConstraint.setCoefficient(linearProblem.getPstTapVariationVariable(pstRangeAction, LinearProblem.VariationExtension.UPWARD), 1);
        groupSetPointConstraint.setCoefficient(linearProblem.getPstTapVariationVariable(pstRangeAction, LinearProblem.VariationExtension.DOWNWARD), -1);
        groupSetPointConstraint.setCoefficient(linearProblem.getRangeActionGroupTapVariable(groupId), 1);
    }
}
