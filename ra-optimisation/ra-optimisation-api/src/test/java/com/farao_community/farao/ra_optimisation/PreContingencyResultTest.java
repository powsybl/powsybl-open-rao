/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.ra_optimisation;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class PreContingencyResultTest {

    private String remedialActionResultId = "id";
    private PreContingencyResult preContingencyResult;
    private RemedialActionResult remedialActionResult;

    @Test
    public void getRemedialActionResultById() {
        assertEquals(remedialActionResult, preContingencyResult.getRemedialActionResultById(remedialActionResultId));
    }

    @Before
    public void setUp() {
        ArrayList<RemedialActionResult> remedialActionResults = new ArrayList<>();
        remedialActionResult = Mockito.mock(RemedialActionResult.class);
        Mockito.when(remedialActionResult.getId()).thenReturn(remedialActionResultId);
        remedialActionResults.add(remedialActionResult);

        ArrayList<MonitoredBranchResult> monitoredBranchResults = new ArrayList<>();

        preContingencyResult = new PreContingencyResult(monitoredBranchResults, remedialActionResults);
    }
}
