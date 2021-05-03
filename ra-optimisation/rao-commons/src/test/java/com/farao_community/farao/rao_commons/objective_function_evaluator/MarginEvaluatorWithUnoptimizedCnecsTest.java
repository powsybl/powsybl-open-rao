/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.objective_function_evaluator;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.rao_api.results.BranchResult;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class MarginEvaluatorWithUnoptimizedCnecsTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    private final BranchCnec branchCnec = Mockito.mock(BranchCnec.class);
    private final BranchResult currentBranchResult = Mockito.mock(BranchResult.class);
    private final BranchResult prePerimeterBranchResult = Mockito.mock(BranchResult.class);
    private final MarginEvaluatorWithUnoptimizedCnecs marginEvaluatorWithUnoptimizedCnecs =
            new MarginEvaluatorWithUnoptimizedCnecs(
                    BranchResult::getMargin,
                    Set.of("FR"),
                    prePerimeterBranchResult
            );

    @Test
    public void getMarginInMegawattOnOptimizedCnec() {
        when(branchCnec.getOperator()).thenReturn("NL");
        when(currentBranchResult.getMargin(branchCnec, Unit.MEGAWATT)).thenReturn(200.);

        double margin = marginEvaluatorWithUnoptimizedCnecs.getMargin(currentBranchResult, branchCnec, Unit.MEGAWATT);
        assertEquals(200., margin, DOUBLE_TOLERANCE);
    }

    @Test
    public void getMarginInAmpereOnOptimizedCnec() {
        when(branchCnec.getOperator()).thenReturn("NL");
        when(currentBranchResult.getMargin(branchCnec, Unit.AMPERE)).thenReturn(50.);

        double margin = marginEvaluatorWithUnoptimizedCnecs.getMargin(currentBranchResult, branchCnec, Unit.AMPERE);
        assertEquals(50., margin, DOUBLE_TOLERANCE);
    }

    @Test
    public void getMarginInMegawattOnConstrainedUnoptimizedCnec() {
        when(branchCnec.getOperator()).thenReturn("FR");
        when(currentBranchResult.getMargin(branchCnec, Unit.MEGAWATT)).thenReturn(200.);
        when(prePerimeterBranchResult.getMargin(branchCnec, Unit.MEGAWATT)).thenReturn(400.);

        double margin = marginEvaluatorWithUnoptimizedCnecs.getMargin(currentBranchResult, branchCnec, Unit.MEGAWATT);
        assertEquals(200., margin, DOUBLE_TOLERANCE);
    }

    @Test
    public void getMarginInMegawattOnUnconstrainedUnoptimizedCnec() {
        when(branchCnec.getOperator()).thenReturn("FR");
        when(currentBranchResult.getMargin(branchCnec, Unit.MEGAWATT)).thenReturn(200.);
        when(prePerimeterBranchResult.getMargin(branchCnec, Unit.MEGAWATT)).thenReturn(100.);

        double margin = marginEvaluatorWithUnoptimizedCnecs.getMargin(currentBranchResult, branchCnec, Unit.MEGAWATT);
        assertEquals(Double.MAX_VALUE, margin, DOUBLE_TOLERANCE);
    }
}
