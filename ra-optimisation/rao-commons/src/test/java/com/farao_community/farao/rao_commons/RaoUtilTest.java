/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.powsybl.glsk.commons.ZonalData;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.glsk.ucte.UcteGlskDocument;
import com.farao_community.farao.rao_api.RaoInput;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;

import static com.farao_community.farao.rao_api.parameters.RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE;
import static com.farao_community.farao.rao_api.parameters.RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */

public class RaoUtilTest {
    private static final double DOUBLE_TOLERANCE = 0.1;
    private RaoParameters raoParameters;
    private RaoInput raoInput;
    private Network network;
    private Crac crac;
    private String variantId;

    @Before
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
        crac = CommonCracCreation.create();
        variantId = network.getVariantManager().getWorkingVariantId();
        raoInput = RaoInput.buildWithPreventiveState(network, crac)
                .withNetworkVariantId(variantId)
                .build();
        raoParameters = new RaoParameters();
    }

    private void addGlskProvider() {
        ZonalData<LinearGlsk> glskProvider = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/GlskCountry.xml"))
                .getZonalGlsks(network);
        raoInput = RaoInput.buildWithPreventiveState(network, crac)
                .withNetworkVariantId(variantId)
                .withGlskProvider(glskProvider)
                .build();
    }

    @Test(expected = FaraoException.class)
    public void testExceptionForGlskOnRelativeMargin() {
        raoParameters.setRelativeMarginPtdfBoundariesFromString(new ArrayList<>(Arrays.asList("{FR}-{ES}", "{ES}-{PT}")));
        raoParameters.setObjectiveFunction(MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        RaoUtil.checkParameters(raoParameters, raoInput);
    }

    @Test(expected = FaraoException.class)
    public void testExceptionForNoPtdfParametersOnRelativeMargin() {
        addGlskProvider();
        raoParameters.setObjectiveFunction(MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        RaoUtil.checkParameters(raoParameters, raoInput);
    }

    @Test(expected = FaraoException.class)
    public void testExceptionForNullBoundariesOnRelativeMargin() {
        addGlskProvider();
        raoParameters.setObjectiveFunction(MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        RaoUtil.checkParameters(raoParameters, raoInput);
    }

    @Test(expected = FaraoException.class)
    public void testExceptionForEmptyBoundariesOnRelativeMargin() {
        addGlskProvider();
        raoParameters.setRelativeMarginPtdfBoundariesFromString(new ArrayList<>());
        raoParameters.setObjectiveFunction(MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT);
        RaoUtil.checkParameters(raoParameters, raoInput);
    }

    @Test(expected = FaraoException.class)
    public void testAmpereWithDc() {
        raoParameters.setObjectiveFunction(MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        raoParameters.getDefaultSensitivityAnalysisParameters().getLoadFlowParameters().setDc(true);
        RaoUtil.checkParameters(raoParameters, raoInput);
    }

    @Test
    public void testGetBranchFlowUnitMultiplier() {
        FlowCnec cnec = Mockito.mock(FlowCnec.class);
        Mockito.when(cnec.getNominalVoltage(Side.LEFT)).thenReturn(400.);
        Mockito.when(cnec.getNominalVoltage(Side.RIGHT)).thenReturn(200.);

        assertEquals(1., RaoUtil.getFlowUnitMultiplier(cnec, Side.LEFT, Unit.MEGAWATT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1., RaoUtil.getFlowUnitMultiplier(cnec, Side.RIGHT, Unit.MEGAWATT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1., RaoUtil.getFlowUnitMultiplier(cnec, Side.LEFT, Unit.AMPERE, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(1., RaoUtil.getFlowUnitMultiplier(cnec, Side.RIGHT, Unit.AMPERE, Unit.AMPERE), DOUBLE_TOLERANCE);

        assertEquals(1000 / 400. / Math.sqrt(3), RaoUtil.getFlowUnitMultiplier(cnec, Side.LEFT, Unit.MEGAWATT, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(400 * Math.sqrt(3) / 1000., RaoUtil.getFlowUnitMultiplier(cnec, Side.LEFT, Unit.AMPERE, Unit.MEGAWATT), DOUBLE_TOLERANCE);

        assertEquals(1000 / 200. / Math.sqrt(3), RaoUtil.getFlowUnitMultiplier(cnec, Side.RIGHT, Unit.MEGAWATT, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(200 * Math.sqrt(3) / 1000., RaoUtil.getFlowUnitMultiplier(cnec, Side.RIGHT, Unit.AMPERE, Unit.MEGAWATT), DOUBLE_TOLERANCE);

        assertThrows(FaraoException.class, () -> RaoUtil.getFlowUnitMultiplier(cnec, Side.LEFT, Unit.MEGAWATT, Unit.PERCENT_IMAX));
        assertThrows(FaraoException.class, () -> RaoUtil.getFlowUnitMultiplier(cnec, Side.LEFT, Unit.KILOVOLT, Unit.MEGAWATT));
        assertThrows(FaraoException.class, () -> RaoUtil.getFlowUnitMultiplier(cnec, Side.RIGHT, Unit.AMPERE, Unit.TAP));
        assertThrows(FaraoException.class, () -> RaoUtil.getFlowUnitMultiplier(cnec, Side.RIGHT, Unit.DEGREE, Unit.AMPERE));
    }
}
