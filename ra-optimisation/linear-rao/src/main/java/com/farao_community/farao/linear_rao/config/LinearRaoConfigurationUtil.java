/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.linear_rao.config;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_loopflow_extension.CracLoopFlowExtension;
import com.farao_community.farao.rao_api.RaoParameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Utility class designed to check availability of all plugins listed in the
 * RAO parameters, and to interpret them in different contexts of the linear
 * RAO.
 *
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public final class LinearRaoConfigurationUtil {

    private LinearRaoConfigurationUtil() {
        throw new AssertionError("Utility class should not be instanciated");
    }

    /**
     * Validates RAO parameters compatibility with platform available plugins.
     * Return a list of errors, that is empty if the configuration is correct.
     *
     * @param raoParameters RAO parameters
     * @return a list of configuration issues
     */
    public static List<String> checkLinearRaoConfiguration(RaoParameters raoParameters, Crac crac) {
        List<String> errors = new ArrayList<>();

        // Check that correct extension is provided
        // Return directly if the extension is not provided
        try {
            getLinearRaoParameters(raoParameters);
        } catch (FaraoException e) {
            errors.add(e.getMessage());
            return errors;
        }

        // loopflows extension vs. loopflow parameters
        if (raoParameters.isRaoWithLoopFlowLimitation() && Objects.isNull(crac.getExtension(CracLoopFlowExtension.class))) {
            errors.add("Loop flow parameters are inconsistent with CRAC loopflow extension");
        }

        // loopflow violation cost should not be negative
        if (raoParameters.getLoopflowViolationCost() < 0.0) {
            errors.add("Loopflow violation cost should not be negative");
        }

        /*
         todo : check that the objective-function is compatible with the sensi mode. If the objective
          function is "MAX_MARGIN_IN_AMPERE" and the sensi is in DC mode, throw an exception
          (it is not possible to check this for now as the PowSyBl API does not allow yet to retrieve
          the AC/DC information of the sensi).
         */

        return errors;
    }

    /**
     * Get LinearRaoParameters from a RaoParameters
     * Throws a FaraoException if it does not exists
     *
     * @param raoParameters RAO parameters
     * @return the linear RAO parameters extension
     */
    public static LinearRaoParameters getLinearRaoParameters(RaoParameters raoParameters) {
        LinearRaoParameters linearRaoParameters = raoParameters.getExtension(LinearRaoParameters.class);
        if (Objects.isNull(linearRaoParameters)) {
            throw new FaraoException("Linear Rao parameters not available");
        }
        return linearRaoParameters;
    }
}
