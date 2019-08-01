package com.farao_community.farao.closed_optimisation_rao;

import com.farao_community.farao.data.crac_file.Contingency;
import com.farao_community.farao.data.crac_file.PstElement;
import com.farao_community.farao.data.crac_file.RedispatchRemedialActionElement;

import java.util.Optional;

public final class ClosedOptimisationRaoNames {

    private ClosedOptimisationRaoNames() {
        throw new AssertionError("Utility class should not have constructor");
    }

    private static final String CONTIGENCY_SEPERATOR = "@";
    private static final String POSTFIX_SEPERATOR = "#";
    private static final String PRECONTINGENCY = "precontingency";

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
    public static String nameRedispatchValueVariable(Optional<Contingency> contingency, RedispatchRemedialActionElement remedialAction) {
        if(contingency.isPresent()) {
            return contingency.get().getId() + CONTIGENCY_SEPERATOR + remedialAction.getId() + POSTFIX_SEPERATOR + REDISPATCH_VALUE_POSTFIX;
        }
       else{return PRECONTINGENCY + CONTIGENCY_SEPERATOR + remedialAction.getId() + POSTFIX_SEPERATOR + REDISPATCH_VALUE_POSTFIX;}
    }

    /**
     * Get standard name of redispatch activation variables
     */
    public static String nameRedispatchActivationVariable(Optional<Contingency> contingency, RedispatchRemedialActionElement remedialAction) {
        if(contingency.isPresent()) {
            return contingency.get().getId() + CONTIGENCY_SEPERATOR + remedialAction.getId() + POSTFIX_SEPERATOR + REDISPATCH_ACTIVATION_POSTFIX;
        }
        else{return PRECONTINGENCY + CONTIGENCY_SEPERATOR + remedialAction.getId() + POSTFIX_SEPERATOR + REDISPATCH_ACTIVATION_POSTFIX;}
    }

    /**
     * Get standard name of redispatch cost variables
     */
    public static String nameRedispatchCostVariable(Optional<Contingency> contingency, RedispatchRemedialActionElement remedialAction) {
        if(contingency.isPresent()) {
            return contingency.get().getId() + CONTIGENCY_SEPERATOR + remedialAction.getId() + POSTFIX_SEPERATOR + REDISPATCH_COST_POSTFIX;
        }
        else{return PRECONTINGENCY + CONTIGENCY_SEPERATOR + remedialAction.getId() + POSTFIX_SEPERATOR + REDISPATCH_COST_POSTFIX;}
    }

    /**
     * Get standard name of PST's shift value variables
     */
    public static String nameShiftValueVariable(Optional<Contingency> contingency, PstElement remedialAction) {
        if(contingency.isPresent()) {
            return contingency.get().getId() + CONTIGENCY_SEPERATOR + remedialAction.getId() + POSTFIX_SEPERATOR + SHIFT_VALUE_POSTFIX;
        }
        else{return PRECONTINGENCY + CONTIGENCY_SEPERATOR + remedialAction.getId() + POSTFIX_SEPERATOR + SHIFT_VALUE_POSTFIX;}
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
