/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class SearchTreeRaoResultTest {

    @Test
    public void test() {
        SearchTreeRaoResult searchTreeRaoResult1 = new SearchTreeRaoResult(SearchTreeRaoResult.ComputationStatus.SECURE, SearchTreeRaoResult.StopCriterion.OPTIMIZATION_FINISHED);
        SearchTreeRaoResult searchTreeRaoResult2 = new SearchTreeRaoResult(SearchTreeRaoResult.ComputationStatus.UNSECURE, SearchTreeRaoResult.StopCriterion.NO_COMPUTATION);
        SearchTreeRaoResult searchTreeRaoResult3 = new SearchTreeRaoResult(SearchTreeRaoResult.ComputationStatus.ERROR, SearchTreeRaoResult.StopCriterion.DIVERGENCE);
        SearchTreeRaoResult searchTreeRaoResult4 = new SearchTreeRaoResult(SearchTreeRaoResult.ComputationStatus.SECURE, SearchTreeRaoResult.StopCriterion.TIME_OUT);
        SearchTreeRaoResult searchTreeRaoResult5 = new SearchTreeRaoResult(SearchTreeRaoResult.ComputationStatus.SECURE, SearchTreeRaoResult.StopCriterion.OPTIMIZATION_TIME_OUT);

        assertNotNull(searchTreeRaoResult1.getStopCriterion());
        assertNotNull(searchTreeRaoResult2.getComputationStatus());
    }

}
