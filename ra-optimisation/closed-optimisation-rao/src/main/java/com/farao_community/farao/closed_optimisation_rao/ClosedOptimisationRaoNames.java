/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.closed_optimisation_rao;

import com.farao_community.farao.data.crac_file.*;

import java.util.Objects;
import java.util.Optional;

import static com.farao_community.farao.closed_optimisation_rao.ClosedOptimisationRaoUtil.getRedispatchElement;

/**
 *  Utility class designed to build parameters, variables and constraints names
 *  of the optimisation RAO problem.
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public final class ClosedOptimisationRaoNames {

    private ClosedOptimisationRaoNames() {
        throw new AssertionError("Utility class should not have constructor");
    }

    private static final String CONTINGENCY_SEPARATOR = "@";
    private static final String GENERATOR_SEPARATOR = "@";
    private static final String POSTFIX_SEPARATOR = "#";
    private static final String PRECONTINGENCY = "precontingency";

    private static final String REDISPATCH_VALUE_POSTFIX = "redispatch_value";
    private static final String REDISPATCH_ACTIVATION_POSTFIX = "redispatch_activation";
    private static final String ESTIMATED_FLOW_POSTFIX = "estimated_flow";
    private static final String ESTIMATED_FLOW_EQUATION_POSTFIX = "estimated_flow_equation";
    private static final String REDISPATCH_COST_POSTFIX = "redispatch_cost";
    private static final String SHIFT_VALUE_POSTFIX = "shift_value";
    private static final String OVERLOAD_VARIABLE = "overload";
    private static final String POSITIVE_MAXFLOW_CONSTRAINT = "ub_flow_constraint";
    private static final String NEGATIVE_MAXFLOW_CONSTRAINT = "lb_flow_constraint";

    public static final String PST_SENSITIVITIES_DATA_NAME = "pst_branch_sensitivities";
    public static final String TOTAL_REDISPATCH_COST = "total_redispatch_cost";
    public static final String GEN_SENSITIVITIES_DATA_NAME = "generators_branch_sensitivities";
    public static final String OPTIMISATION_CONSTANTS_DATA_NAME = "constants";
    public static final String REFERENCE_FLOWS_DATA_NAME = "reference_flows";
    public static final String OVERLOAD_PENALTY_COST_NAME = "overload_penalty_cost";
    public static final String RD_SENSITIVITY_SIGNIFICANCE_THRESHOLD_NAME = "rd_sensitivity_threshold";
    public static final String PST_SENSITIVITY_SIGNIFICANCE_THRESHOLD_NAME = "pst_sensitivity_threshold";
    public static final String NUMBER_OF_PARALLEL_THREADS_NAME = "number_of_parallel_threads";

    /**
     * Get standard name of redispatch value variables
     */
    public static String nameRedispatchValueVariable(Optional<Contingency> contingency, RemedialAction ra) {
        RedispatchRemedialActionElement rrae = Objects.requireNonNull(getRedispatchElement(ra));
        if (contingency.isPresent()) {
            return contingency.get().getId() + CONTINGENCY_SEPARATOR + ra.getId() + GENERATOR_SEPARATOR + rrae.getId() + POSTFIX_SEPARATOR + REDISPATCH_VALUE_POSTFIX;
        } else {
            return PRECONTINGENCY + CONTINGENCY_SEPARATOR + ra.getId() + GENERATOR_SEPARATOR + rrae.getId() + POSTFIX_SEPARATOR + REDISPATCH_VALUE_POSTFIX;
        }
    }

    /**
     * Get standard name of redispatch activation variables
     */
    public static String nameRedispatchActivationVariable(Optional<Contingency> contingency, RemedialAction ra) {
        RedispatchRemedialActionElement rrae = Objects.requireNonNull(getRedispatchElement(ra));
        if (contingency.isPresent()) {
            return contingency.get().getId() + CONTINGENCY_SEPARATOR + ra.getId() + GENERATOR_SEPARATOR + rrae.getId() + POSTFIX_SEPARATOR + REDISPATCH_ACTIVATION_POSTFIX;
        } else {
            return PRECONTINGENCY + CONTINGENCY_SEPARATOR + ra.getId() + GENERATOR_SEPARATOR + rrae.getId() + POSTFIX_SEPARATOR + REDISPATCH_ACTIVATION_POSTFIX;
        }
    }

    /**
     * Get standard name of redispatch cost variables
     */
    public static String nameRedispatchCostVariable(Optional<Contingency> contingency, RemedialAction ra) {
        RedispatchRemedialActionElement rrae = Objects.requireNonNull(getRedispatchElement(ra));
        if (contingency.isPresent()) {
            return contingency.get().getId() + CONTINGENCY_SEPARATOR + ra.getId() + GENERATOR_SEPARATOR + rrae.getId() + POSTFIX_SEPARATOR + REDISPATCH_COST_POSTFIX;
        } else {
            return PRECONTINGENCY + CONTINGENCY_SEPARATOR + ra.getId() + GENERATOR_SEPARATOR + rrae.getId() + POSTFIX_SEPARATOR + REDISPATCH_COST_POSTFIX;
        }
    }

    /**
     * Get standard name of PST's shift value variables
     */
    public static String nameShiftValueVariable(Optional<Contingency> contingency, PstElement remedialAction) {
        if (contingency.isPresent()) {
            return contingency.get().getId() + CONTINGENCY_SEPARATOR + remedialAction.getId() + POSTFIX_SEPARATOR + SHIFT_VALUE_POSTFIX;
        } else {
            return PRECONTINGENCY + CONTINGENCY_SEPARATOR + remedialAction.getId() + POSTFIX_SEPARATOR + SHIFT_VALUE_POSTFIX;
        }
    }

    /**
     * Get standard name of estimated flow variable
     */
    public static String nameEstimatedFlowVariable(MonitoredBranch branch) {
        return branch.getId() + POSTFIX_SEPARATOR + ESTIMATED_FLOW_POSTFIX;
    }

    /**
     * Get standard name of flow definition constraint
     */
    public static String nameEstimatedFlowConstraint(MonitoredBranch branch) {
        return branch.getId() + POSTFIX_SEPARATOR + ESTIMATED_FLOW_EQUATION_POSTFIX;
    }

    /**
     * Get standard name of overload variable
     */
    public static String nameOverloadVariable(MonitoredBranch branch) {
        return branch.getId() + POSTFIX_SEPARATOR + OVERLOAD_VARIABLE;
    }

    /**
     * Get standard name of maximum positive flow constraint
     */
    public static String namePositiveMaximumFlowConstraint(MonitoredBranch branch) {
        return branch.getId() + POSTFIX_SEPARATOR + POSITIVE_MAXFLOW_CONSTRAINT;
    }

    /**
     * Get standard name of maximum negative flow constraint
     */
    public static String nameNegativeMaximumFlowConstraint(MonitoredBranch branch) {
        return branch.getId() + POSTFIX_SEPARATOR + NEGATIVE_MAXFLOW_CONSTRAINT;
    }
}
