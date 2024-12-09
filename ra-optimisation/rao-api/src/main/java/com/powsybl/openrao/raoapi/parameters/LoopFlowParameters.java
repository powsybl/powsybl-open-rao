/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.raoapi.parameters;

import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.iidm.network.Country;

import java.util.*;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.*;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.COUNTRIES;

/**
 * Extension : loopFlow parameters for RAO
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class LoopFlowParameters {
    static final double DEFAULT_ACCEPTABLE_INCREASE = 0.0;
    static final Set<Country> DEFAULT_COUNTRIES = new HashSet<>(); //Empty by default
    private double acceptableIncrease = DEFAULT_ACCEPTABLE_INCREASE;
    private Set<Country> countries = DEFAULT_COUNTRIES;

    // Getters and setters
    public double getAcceptableIncrease() {
        return acceptableIncrease;
    }

    public void setAcceptableIncrease(double acceptableIncrease) {
        this.acceptableIncrease = acceptableIncrease;
    }

    public Set<Country> getCountries() {
        return countries;
    }

    public void setCountries(Set<Country> countries) {
        this.countries = countries;
    }

    public void setCountries(List<String> countryStrings) {
        this.countries = ParametersUtil.convertToCountrySet(countryStrings);
    }

    public static Optional<LoopFlowParameters> load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        return platformConfig.getOptionalModuleConfig(LOOP_FLOW_PARAMETERS_SECTION)
            .map(config -> {
                LoopFlowParameters parameters = new LoopFlowParameters();
                parameters.setAcceptableIncrease(config.getDoubleProperty(ACCEPTABLE_INCREASE, LoopFlowParameters.DEFAULT_ACCEPTABLE_INCREASE));
                parameters.setCountries(ParametersUtil.convertToCountrySet(config.getStringListProperty(COUNTRIES, new ArrayList<>())));
                return parameters;
            });
    }
}

