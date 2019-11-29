/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.flow_decomposition_results;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class FlowDecompositionResultsTest {

    @Test
    public void testBuildingAndGetting() {
        String branchId = "Branch ID";
        FlowDecompositionResults results = new FlowDecompositionResults();
        assertEquals(0, results.getPerBranchResults().size());

        PerBranchResult perBranchResult = PerBranchResult.builder().build();
        results.addPerBranchResult(branchId, perBranchResult);
        assertTrue(results.hasPerBranchResult(branchId));
        assertEquals(1, results.getPerBranchResults().size());
        assertEquals(perBranchResult, results.getPerBranchResult(branchId));
    }
}
