/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.rao_api.parameters.MaxMinRelativeMarginParameters;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import static org.junit.Assert.assertEquals;

import java.util.*;
import java.util.Optional;
import java.util.Set;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class MaxMinRelativeMarginParametersTest {
    private static final double DOUBLE_TOLERANCE = 0.1;

    private FlowCnec cnecA;
    private FlowCnec cnecB;
    private FlowCnec cnecC;
    private FlowCnec cnecD;
    private MaxMinRelativeMarginParameters maxMinRelativeMarginParameters = new MaxMinRelativeMarginParameters(0.01, 0.01, 0.01, 0.01);

    @Before
    public void setUp() {
        // Set up
        cnecA = Mockito.mock(FlowCnec.class);
        cnecB = Mockito.mock(FlowCnec.class);
        cnecC = Mockito.mock(FlowCnec.class);
        cnecD = Mockito.mock(FlowCnec.class);
        Mockito.when(cnecA.isOptimized()).thenReturn(true);
        Mockito.when(cnecB.isOptimized()).thenReturn(true);
        Mockito.when(cnecC.isOptimized()).thenReturn(true);
        Mockito.when(cnecD.isOptimized()).thenReturn(false);
        Mockito.when(cnecA.getUpperBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.of(1000.));
        Mockito.when(cnecA.getLowerBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.empty());
        Mockito.when(cnecB.getUpperBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.empty());
        Mockito.when(cnecB.getLowerBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.of(-1500.));
        Mockito.when(cnecC.getUpperBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.empty());
        Mockito.when(cnecC.getLowerBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.empty());
        Mockito.when(cnecD.getUpperBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.of(-16000.));
        Mockito.when(cnecD.getLowerBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.of(-16000.));
    }

    @Test
    public void testGetLargestCnecThreshold() {
        assertEquals(1000., maxMinRelativeMarginParameters.getLargestCnecThreshold(Set.of(cnecA)), DOUBLE_TOLERANCE);
        assertEquals(1500., maxMinRelativeMarginParameters.getLargestCnecThreshold(Set.of(cnecB)), DOUBLE_TOLERANCE);
        assertEquals(1500., maxMinRelativeMarginParameters.getLargestCnecThreshold(Set.of(cnecA, cnecB)), DOUBLE_TOLERANCE);
        assertEquals(1500., maxMinRelativeMarginParameters.getLargestCnecThreshold(Set.of(cnecA, cnecB, cnecC)), DOUBLE_TOLERANCE);
        assertEquals(1000., maxMinRelativeMarginParameters.getLargestCnecThreshold(Set.of(cnecA, cnecC)), DOUBLE_TOLERANCE);
        assertEquals(1500., maxMinRelativeMarginParameters.getLargestCnecThreshold(Set.of(cnecA, cnecB, cnecD)), DOUBLE_TOLERANCE);
    }
}
