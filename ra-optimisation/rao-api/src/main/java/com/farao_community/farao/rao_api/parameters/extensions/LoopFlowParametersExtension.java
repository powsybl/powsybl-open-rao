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

import static com.farao_community.farao.rao_api.RaoParametersConstants.*;

/**
 * Extension : loopFlow parameters for RAO
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class LoopFlowParametersExtension extends AbstractExtension<RaoParameters> {
    static final double DEFAULT_ACCEPTABLE_INCREASE = 0.0;
    static final Approximation DEFAULT_APPROXIMATION = Approximation.FIXED_PTDF;
    static final double DEFAULT_CONSTRAINT_ADJUSTMENT_COEFFICIENT = 0.0;
    static final double DEFAULT_VIOLATION_COST = 0.0;
    static final Set<Country> DEFAULT_COUNTRIES = new HashSet<>(); //Empty by default
    private double acceptableIncrease = DEFAULT_ACCEPTABLE_INCREASE;
    private Approximation approximation = DEFAULT_APPROXIMATION;

    private double constraintAdjustmentCoefficient = DEFAULT_CONSTRAINT_ADJUSTMENT_COEFFICIENT;
    private double violationCost = DEFAULT_VIOLATION_COST;
    private Set<Country> countries = DEFAULT_COUNTRIES;

    public enum Approximation {
        FIXED_PTDF, // compute PTDFs only once at beginning of RAO (best performance, worst accuracy)
        UPDATE_PTDF_WITH_TOPO, // recompute PTDFs after every topological change in the network (worst performance, better accuracy for AC, best accuracy for DC)
        UPDATE_PTDF_WITH_TOPO_AND_PST; // recompute PTDFs after every topological or PST change in the network (worst performance, best accuracy for AC)

        public boolean shouldUpdatePtdfWithTopologicalChange() {
            return !this.equals(FIXED_PTDF);
        }

        public boolean shouldUpdatePtdfWithPstChange() {
            return this.equals(UPDATE_PTDF_WITH_TOPO_AND_PST);
        }
    }

    // Getters and setters
    public double getAcceptableIncrease() {
        return acceptableIncrease;
    }

    public void setAcceptableIncrease(double acceptableIncrease) {
        this.acceptableIncrease = acceptableIncrease;
    }

    public Approximation getApproximation() {
        return approximation;
    }

    public void setApproximation(Approximation approximation) {
        this.approximation = approximation;
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

