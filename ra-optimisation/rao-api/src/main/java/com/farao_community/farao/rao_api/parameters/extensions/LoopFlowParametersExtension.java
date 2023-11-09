/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api.parameters.extensions;

import com.farao_community.farao.rao_api.parameters.ParametersUtil;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.iidm.network.Country;

import java.util.*;

import static com.farao_community.farao.rao_api.RaoParametersCommons.*;

/**
 * Extension : loopFlow parameters for RAO
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class LoopFlowParametersExtension extends AbstractExtension<RaoParameters> {
    static final double DEFAULT_ACCEPTABLE_INCREASE = 0.0;
    static final PtdfApproximation DEFAULT_PTDF_APPROXIMATION = PtdfApproximation.FIXED_PTDF;
    static final double DEFAULT_CONSTRAINT_ADJUSTMENT_COEFFICIENT = 0.0;
    static final double DEFAULT_VIOLATION_COST = 0.0;
    static final Set<Country> DEFAULT_COUNTRIES = new HashSet<>(); //Empty by default
    private double acceptableIncrease = DEFAULT_ACCEPTABLE_INCREASE;
    private PtdfApproximation ptdfApproximation = DEFAULT_PTDF_APPROXIMATION;

    private double constraintAdjustmentCoefficient = DEFAULT_CONSTRAINT_ADJUSTMENT_COEFFICIENT;
    private double violationCost = DEFAULT_VIOLATION_COST;
    private Set<Country> countries = DEFAULT_COUNTRIES;

    // Getters and setters
    public double getAcceptableIncrease() {
        return acceptableIncrease;
    }

    public void setAcceptableIncrease(double acceptableIncrease) {
        this.acceptableIncrease = acceptableIncrease;
    }

    public PtdfApproximation getPtdfApproximation() {
        return ptdfApproximation;
    }

    public void setPtdfApproximation(PtdfApproximation ptdfApproximation) {
        this.ptdfApproximation = ptdfApproximation;
    }

    public double getConstraintAdjustmentCoefficient() {
        return constraintAdjustmentCoefficient;
    }

    public void setConstraintAdjustmentCoefficient(double constraintAdjustmentCoefficient) {
        this.constraintAdjustmentCoefficient = constraintAdjustmentCoefficient;
    }

    public double getViolationCost() {
        return violationCost;
    }

    public void setViolationCost(double violationCost) {
        this.violationCost = violationCost;
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

    @Override
    public String getName() {
        return LOOP_FLOW_PARAMETERS;
    }
}

