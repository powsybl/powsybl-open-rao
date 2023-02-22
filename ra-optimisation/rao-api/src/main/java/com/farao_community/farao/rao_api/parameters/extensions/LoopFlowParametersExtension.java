/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_api.parameters.extensions;

import com.farao_community.farao.rao_api.parameters.ParametersUtil;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.google.auto.service.AutoService;
import com.powsybl.commons.config.PlatformConfig;
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
    private double acceptableIncrease;
    private Approximation approximation;

    private double constraintAdjustmentCoefficient;
    private double violationCost;
    private Set<Country> countries;

    public static final double DEFAULT_ACCEPTABLE_INCREASE = 0.0;
    public static final Approximation DEFAULT_APPROXIMATION = Approximation.FIXED_PTDF;
    public static final double DEFAULT_CONSTRAINT_ADJUSTMENT_COEFFICIENT = 0.0;
    public static final double DEFAULT_VIOLATION_COST = 0.0;
    private static final Set<Country> DEFAULT_COUNTRIES = new HashSet<>(); //Empty by default

    public LoopFlowParametersExtension(double acceptableIncrease, Approximation approximation, double constraintAdjustmentCoefficient, double violationCost, Set<Country> countries) {
        this.acceptableIncrease = acceptableIncrease;
        this.approximation = approximation;
        this.constraintAdjustmentCoefficient = constraintAdjustmentCoefficient;
        this.violationCost = violationCost;
        this.countries = countries;
    }

    public static LoopFlowParametersExtension loadDefault() {
        return new LoopFlowParametersExtension(DEFAULT_ACCEPTABLE_INCREASE, DEFAULT_APPROXIMATION, DEFAULT_CONSTRAINT_ADJUSTMENT_COEFFICIENT,
                DEFAULT_VIOLATION_COST, DEFAULT_COUNTRIES);
    }

    public enum Approximation {
        FIXED_PTDF, // compute PTDFs only once at beginning of RAO (best performance, worst accuracy)
        UPDATE_PTDF_WITH_TOPO, // recompute PTDFs after every topological change in the network (worst performance, better accuracy for AC, best accuracy for DC)
        UPDATE_PTDF_WITH_TOPO_AND_PST; // recompute PTDFs after every topological or PST change in the network (worst performance, best accuracy for AC)

        // TODO : are these methods called ?
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
        return LOOP_FLOW_PARAMETERS_EXTENSION_NAME;
    }

    @AutoService(RaoParameters.ConfigLoader.class)
    public class LoopFlowParametersConfigLoader implements RaoParameters.ConfigLoader<LoopFlowParametersExtension> {

        @Override
        public LoopFlowParametersExtension load(PlatformConfig platformConfig) {
            Objects.requireNonNull(platformConfig);
            LoopFlowParametersExtension parameters = loadDefault();
            platformConfig.getOptionalModuleConfig(LOOP_FLOW_PARAMETERS)
                    .ifPresent(config -> {
                        parameters.setAcceptableIncrease(config.getDoubleProperty(ACCEPTABLE_INCREASE, DEFAULT_ACCEPTABLE_INCREASE));
                        parameters.setApproximation(config.getEnumProperty(APPROXIMATION, Approximation.class,
                                DEFAULT_APPROXIMATION));
                        parameters.setConstraintAdjustmentCoefficient(config.getDoubleProperty(CONSTRAINT_ADJUSTMENT_COEFFICIENT, DEFAULT_CONSTRAINT_ADJUSTMENT_COEFFICIENT));
                        parameters.setViolationCost(config.getDoubleProperty(VIOLATION_COST, DEFAULT_VIOLATION_COST));
                        parameters.setCountries(ParametersUtil.convertToCountrySet(config.getStringListProperty(COUNTRIES, new ArrayList<>())));
                    });
            return parameters;
        }

        @Override
        public String getExtensionName() {
            return LOOP_FLOW_PARAMETERS_EXTENSION_NAME;
        }

        @Override
        public String getCategoryName() {
            return "rao-parameters";
        }

        @Override
        public Class<? super LoopFlowParametersExtension> getExtensionClass() {
            return LoopFlowParametersExtension.class;
        }
    }
}
