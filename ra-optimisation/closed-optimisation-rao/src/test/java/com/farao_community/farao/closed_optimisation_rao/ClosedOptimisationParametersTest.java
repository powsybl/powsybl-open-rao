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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
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
    public void copySetAndGetClosedOptimisationParametersTest() {
        List<String> listProcessors = new ArrayList<>();
        List<String> listFillers = new ArrayList<>();

        listProcessors.add("processor1");
        listProcessors.add("processor2");
        listFillers.add("filler");

        ClosedOptimisationRaoParameters copiedParameters = new ClosedOptimisationRaoParameters(parametersExtension);

        copiedParameters.setOverloadPenaltyCost(3000.0);
        copiedParameters.setMaxTimeInSeconds(3600);
        copiedParameters.setSolverType("ANOTHER_SOLVER");
        copiedParameters.setRelativeMipGap(0.001);
        copiedParameters.setRdSensitivityThreshold(0.05);
        copiedParameters.setPstSensitivityThreshold(1);
        copiedParameters.setNumberOfParallelThreads(8);
        copiedParameters.addAllPreProcessors(listProcessors);
        copiedParameters.addAllFillers(listFillers);
        copiedParameters.addAllPostProcessors(listProcessors);

        double  tol = 1e-3;
        assertEquals(3000.0, copiedParameters.getOverloadPenaltyCost(), tol);
        assertEquals(3600, copiedParameters.getMaxTimeInSeconds(), tol);
        assertEquals("ANOTHER_SOLVER", copiedParameters.getSolverType());
        assertEquals(0.001, copiedParameters.getRelativeMipGap(), tol);
        assertEquals(0.05, copiedParameters.getRdSensitivityThreshold(), tol);
        assertEquals(1, copiedParameters.getPstSensitivityThreshold(), tol);
        assertEquals(8, copiedParameters.getNumberOfParallelThreads(), tol);
        assertEquals(parametersExtension.getPreProcessorsList().size() + 2, copiedParameters.getPreProcessorsList().size(), tol);
        assertEquals(parametersExtension.getFillersList().size() + 1, copiedParameters.getFillersList().size(), tol);
        assertEquals(parametersExtension.getPostProcessorsList().size() + 2, copiedParameters.getPostProcessorsList().size(), tol);
    }

    @Test
    public void getOptimisationConstantsTest() {
        Map<String, Object> constants = ConfigurationUtil.getOptimisationConstants(parametersExtension);
        assertTrue(constants.containsKey(ClosedOptimisationRaoNames.OVERLOAD_PENALTY_COST_NAME));
    }
}
