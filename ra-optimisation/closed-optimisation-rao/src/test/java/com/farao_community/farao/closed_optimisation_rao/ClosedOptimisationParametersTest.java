/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.closed_optimisation_rao;

import com.farao_community.farao.ra_optimisation.RaoComputationParameters;
import com.powsybl.commons.config.PlatformConfig;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.Assert.assertTrue;


/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class ClosedOptimisationParametersTest {

    private RaoComputationParameters parameters;
    private ClosedOptimisationRaoParameters parametersExtension;

    @Before
    public void setUp() {
        PlatformConfig config = Mockito.mock(PlatformConfig.class);
        parameters = RaoComputationParameters.load(config);
        parametersExtension = parameters.getExtension(ClosedOptimisationRaoParameters.class);
    }

    @Test
    public void getOptimisationConstantsTest() {
        Map<String, Double> constants = ConfigurationUtil.getOptimisationConstants(parametersExtension);
        assertTrue(constants.containsKey(ClosedOptimisationRaoNames.OVERLOAD_PENALTY_COST_NAME));
    }
}