package com.farao_community.farao.rao_commons.linear_optimisation.fillers;

import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;

public class MnecFiller implements ProblemFiller {
    @Override
    public void fill(RaoData raoData, LinearProblem linearProblem) {
        // build variables
        buildMarginViolationVariable(raoData, linearProblem);

        // build constraints
        buildMnecMarginConstraints(raoData, linearProblem);

        // complete objective
        //fillObjectiveWithMinMargin(linearProblem);
        //fillObjectiveWithRangeActionPenaltyCost(raoData, linearProblem);
    }

    private void buildMarginViolationVariable(RaoData raoData, LinearProblem linearProblem) {
        raoData.getCrac().getCnecs().stream().filter(Cnec::isMonitored).forEach(cnec ->
            linearProblem.addMnecViolationVariable(0, linearProblem.infinity(), cnec)
        );
    }

    private void buildMnecMarginConstraints(RaoData raoData, LinearProblem linearProblem) {
        // TO DO
    }

    @Override
    public void update(RaoData raoData, LinearProblem linearProblem) {
        // TO DO
    }
}
