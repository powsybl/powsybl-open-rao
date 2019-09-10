/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.closed_optimisation_rao;

import com.farao_community.farao.data.crac_file.Contingency;
import com.farao_community.farao.data.crac_file.PstElement;
import com.farao_community.farao.data.crac_file.RedispatchRemedialActionElement;
import com.farao_community.farao.data.crac_file.RemedialAction;

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

    private static final String CONTINGENCY_SEPERATOR = "@";
    private static final String GENERATOR_SEPARATOR = "@";
    private static final String POSTFIX_SEPERATOR = "#";
    private static final String PRECONTINGENCY = "precontingency";

    private static final String GENERATION_VALUE_POSTFIX = "generation_value";
    private static final String GENERATION_CONSTRAINT_POSTFIX = "generation_equation";
    private static final String REDISPATCH_VALUE_POSTFIX = "redispatch_value";
    private static final String REDISPATCH_ACTIVATION_POSTFIX = "redispatch_activation";
    private static final String ESTIMATED_FLOW_POSTFIX = "estimated_flow";
    private static final String ESTIMATED_FLOW_EQUATION_POSTFIX = "estimated_flow_equation";
    private static final String REDISPATCH_COST_POSTFIX = "redispatch_cost";
    private static final String SHIFT_VALUE_POSTFIX = "shift_value";

    public static final String PST_SENSITIVITIES_DATA_NAME = "pst_branch_sensitivities";
    public static final String TOTAL_REDISPATCH_COST = "total_redispatch_cost";
    public static final String GEN_SENSITIVITIES_DATA_NAME = "generators_branch_sensitivities";
    public static final String REFERENCE_FLOWS_DATA_NAME = "reference_flows";

    /**
     * Get standard name of redispatch value variables
     */
    public static String nameRedispatchValueVariable(Optional<Contingency> contingency, RemedialAction ra) {
        RedispatchRemedialActionElement rrae = Objects.requireNonNull(getRedispatchElement(ra));
        if (contingency.isPresent()) {
            return contingency.get().getId() + CONTINGENCY_SEPERATOR + ra.getId() + GENERATOR_SEPARATOR + rrae.getId() + POSTFIX_SEPERATOR + REDISPATCH_VALUE_POSTFIX;
        } else {
            return PRECONTINGENCY + CONTINGENCY_SEPERATOR + ra.getId() + GENERATOR_SEPARATOR + rrae.getId() + POSTFIX_SEPERATOR + REDISPATCH_VALUE_POSTFIX;
        }
    }

    /**
     * Get standard name of redispatch activation variables
     */
    public static String nameRedispatchActivationVariable(Optional<Contingency> contingency, RemedialAction ra) {
        RedispatchRemedialActionElement rrae = Objects.requireNonNull(getRedispatchElement(ra));
        if (contingency.isPresent()) {
            return contingency.get().getId() + CONTINGENCY_SEPERATOR + ra.getId() + GENERATOR_SEPARATOR + rrae.getId() + POSTFIX_SEPERATOR + REDISPATCH_ACTIVATION_POSTFIX;
        } else {
            return PRECONTINGENCY + CONTINGENCY_SEPERATOR + ra.getId() + GENERATOR_SEPARATOR + rrae.getId() + POSTFIX_SEPERATOR + REDISPATCH_ACTIVATION_POSTFIX;
        }
    }

    /**
     * Get standard name of redispatch cost variables
     */
    public static String nameRedispatchCostVariable(Optional<Contingency> contingency, RemedialAction ra) {
        RedispatchRemedialActionElement rrae = Objects.requireNonNull(getRedispatchElement(ra));
        if (contingency.isPresent()) {
            return contingency.get().getId() + CONTINGENCY_SEPERATOR + ra.getId() + GENERATOR_SEPARATOR + rrae.getId() + POSTFIX_SEPERATOR + REDISPATCH_COST_POSTFIX;
        } else {
            return PRECONTINGENCY + CONTINGENCY_SEPERATOR + ra.getId() + GENERATOR_SEPARATOR + rrae.getId() + POSTFIX_SEPERATOR + REDISPATCH_COST_POSTFIX;
        }
    }

    /**
     * Get standard name of PST's shift value variables
     */
    public static String nameShiftValueVariable(Optional<Contingency> contingency, PstElement remedialAction) {
        if (contingency.isPresent()) {
            return contingency.get().getId() + CONTINGENCY_SEPERATOR + remedialAction.getId() + POSTFIX_SEPERATOR + SHIFT_VALUE_POSTFIX;
        } else {
            return PRECONTINGENCY + CONTINGENCY_SEPERATOR + remedialAction.getId() + POSTFIX_SEPERATOR + SHIFT_VALUE_POSTFIX;
        }
    }

    /**
     * Get standard name of estimated flow variable
     */
    public static String nameEstimatedFlowVariable(String branchId) {
        return branchId + POSTFIX_SEPERATOR + ESTIMATED_FLOW_POSTFIX;
    }

    /**
     * Get standard name of flow definition constraint
     */
    public static String nameEstimatedFlowConstraint(String branchId) {
        return branchId + POSTFIX_SEPERATOR + ESTIMATED_FLOW_EQUATION_POSTFIX;
    }
}
