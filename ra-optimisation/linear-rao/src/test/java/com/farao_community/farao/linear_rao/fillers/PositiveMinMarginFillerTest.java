/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.linear_rao.fillers;

import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.linear_rao.mocks.CnecMock;
import com.farao_community.farao.linear_rao.mocks.RangeActionMock;
import com.farao_community.farao.linear_rao.mocks.TwoWindingsTransformerMock;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.when;

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
    public void fillWithRangeAction() {
        final String rangeActionId = "range-action-id";
        final String networkElementId = "network-element-id";
        final int minTap = -10;
        final int maxTap = 16;
        final int currentTap = 5;
        final double referenceFlow1 = 500.;
        final double referenceFlow2 = 300.;
        final double cnec1toRangeSensitivity = 0.2;
        final double cnec2toRangeSensitivity = 0.5;
        Cnec cnec1 = new CnecMock("cnec1-id", 0, 800);
        Cnec cnec2 = new CnecMock("cnec2-id", 0, 800);
        when(linearRaoData.getReferenceFlow(cnec1)).thenReturn(referenceFlow1);
        when(linearRaoData.getReferenceFlow(cnec2)).thenReturn(referenceFlow2);

        cnecs.add(cnec1);
        cnecs.add(cnec2);
        RangeAction rangeAction = new RangeActionMock(rangeActionId, networkElementId, minTap, maxTap);
        when(linearRaoData.getSensitivity(cnec1, rangeAction)).thenReturn(cnec1toRangeSensitivity);
        when(linearRaoData.getSensitivity(cnec2, rangeAction)).thenReturn(cnec2toRangeSensitivity);
        rangeActions.add(rangeAction);
        TwoWindingsTransformer twoWindingsTransformer = new TwoWindingsTransformerMock(minTap, maxTap, currentTap);
        when(network.getIdentifiable(networkElementId)).thenReturn((Identifiable) twoWindingsTransformer);

        coreProblemFiller.fill(linearRaoProblem, linearRaoData);
        positiveMinMarginFiller.fill(linearRaoProblem, linearRaoData);
    }
}
