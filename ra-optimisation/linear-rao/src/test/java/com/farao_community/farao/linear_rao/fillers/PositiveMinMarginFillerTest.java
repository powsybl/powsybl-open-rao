/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.linear_rao.fillers;

import com.farao_community.farao.data.crac_api.SynchronizationException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class PositiveMinMarginFillerTest extends FillerTest {

    private PositiveMinMarginFiller positiveMinMarginFiller;

    @Before
    public void setUp() {
        init();
        coreProblemFiller = new CoreProblemFiller();
        positiveMinMarginFiller = new PositiveMinMarginFiller();
    }

    @Test
    public void fillWithCnec() throws SynchronizationException {
        initCnec();
        coreProblemFiller.fill(linearRaoProblem, linearRaoData);
        positiveMinMarginFiller.fill(linearRaoProblem, linearRaoData);

        assertEquals(-Double.MAX_VALUE, linearRaoProblem.getSolver().lookupVariableOrNull("pos-min-margin").lb(), 0.1);
        assertEquals(Double.MAX_VALUE, linearRaoProblem.getSolver().lookupVariableOrNull("pos-min-margin").ub(), 0.1);
        assertEquals(100, linearRaoProblem.getSolver().lookupConstraintOrNull("pos-min-margin-cnec1-id-max").ub(), 0.1);
        assertEquals(-1, linearRaoProblem.getSolver().lookupConstraintOrNull("pos-min-margin-cnec1-id-min").ub(), 0.1);
        assertEquals(100, linearRaoProblem.getSolver().lookupConstraintOrNull("pos-min-margin-cnec2-id-max").ub(), 0.1);
        assertEquals(-1, linearRaoProblem.getSolver().lookupConstraintOrNull("pos-min-margin-cnec2-id-min").ub(), 0.1);
    }

    @Test
    public void fillRaAndCnec() throws SynchronizationException {
        initBoth();
        coreProblemFiller.fill(linearRaoProblem, linearRaoData);
        positiveMinMarginFiller.fill(linearRaoProblem, linearRaoData);

        assertEquals(-Double.MAX_VALUE, linearRaoProblem.getSolver().lookupVariableOrNull("pos-min-margin").lb(), 0.1);
        assertEquals(Double.MAX_VALUE, linearRaoProblem.getSolver().lookupVariableOrNull("pos-min-margin").ub(), 0.1);
        assertEquals(100, linearRaoProblem.getSolver().lookupConstraintOrNull("pos-min-margin-cnec1-id-max").ub(), 0.1);
        assertEquals(-1, linearRaoProblem.getSolver().lookupConstraintOrNull("pos-min-margin-cnec1-id-min").ub(), 0.1);
        assertEquals(100, linearRaoProblem.getSolver().lookupConstraintOrNull("pos-min-margin-cnec2-id-max").ub(), 0.1);
        assertEquals(-1, linearRaoProblem.getSolver().lookupConstraintOrNull("pos-min-margin-cnec2-id-min").ub(), 0.1);
    }
}
