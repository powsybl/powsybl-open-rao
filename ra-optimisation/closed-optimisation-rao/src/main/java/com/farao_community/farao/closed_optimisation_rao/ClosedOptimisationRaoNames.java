package com.farao_community.farao.closed_optimisation_rao;

import com.farao_community.farao.data.crac_file.Contingency;
import com.farao_community.farao.data.crac_file.RedispatchRemedialActionElement;

import java.util.Optional;

public final class ClosedOptimisationRaoNames {

    private ClosedOptimisationRaoNames() {
        throw new AssertionError("Utility class should not have constructor");
    }

    private static final String BLANK_CHARACTER = "@";

    private static final String REDISPATCH_VALUE_N_POSTFIX = "redispatch_value_n";
    private static final String REDISPATCH_VALUE_CURATIVE_POSTFIX = "redispatch_value_curative";
    private static final String ESTIMATED_FLOW_POSTFIX = "estimated_flow";
    private static final String ESTIMATED_FLOW_EQUATION_POSTFIX = "estimated_flow_equation";
    private static final String REDISPATCH_ACTIVATION_N_POSTFIX = "redispatch_activation_n";
    private static final String REDISPATCH_ACTIVATION_CURATIVE_POSTFIX = "redispatch_activation_curative";
    private static final String REDISPATCH_COST_N_POSTFIX = "redispatch_cost_n";
    private static final String REDISPATCH_COST_CURATIVE_POSTFIX = "redispatch_cost_curative";
    private static final String SHIFT_VALUE_N_POSTFIX = "shift_value_n";
    private static final String SHIFT_VALUE_CURATIVE_POSTFIX = "shift_value_curative";

    public static final String PST_SENSITIVITIES_DATA_NAME = "pst_branch_sensitivities";
    public static final String TOTAL_REDISPATCH_COST = "total_redispatch_cost";
    public static final String GEN_SENSITIVITIES_DATA_NAME = "generators_branch_sensitivities";
    public static final String REFERENCE_FLOWS_DATA_NAME = "reference_flows";

    /**
     * Get standard name of redispatch value variable for preventive remedial actions
     */
    public static String nameRedispatchValueVariableN(String remedialActionId) {
        return remedialActionId + BLANK_CHARACTER + REDISPATCH_VALUE_N_POSTFIX;
    }

    /**
     * Get standard name of redispatch value variable for preventive remedial actions
     */
    public static String nameRedispatchValueVariable(RedispatchRemedialActionElement remedialAction, Optional<Contingency> contingency) {
        if(contingency.isPresent()) {
            return contingency.get().getId() + BLANK_CHARACTER + remedialAction.getId() + BLANK_CHARACTER + REDISPATCH_VALUE_CURATIVE_POSTFIX;
        }
       else{return remedialAction.getId() + BLANK_CHARACTER + REDISPATCH_VALUE_N_POSTFIX;}
    }
    /**
     * Get standard name of redispatch value variable for curative remedial actions
     */
    public static String nameRedispatchValueVariableCurative(String contingencyId, String remedialActionId) {
        return contingencyId + BLANK_CHARACTER + remedialActionId + BLANK_CHARACTER + REDISPATCH_VALUE_CURATIVE_POSTFIX;
    }

    /**
     * Get standard name of estimated flow variable
     */
    public static String nameEstimatedFlowVariable(String branchId) {
        return branchId + BLANK_CHARACTER + ESTIMATED_FLOW_POSTFIX;
    }

    /**
     * Get standard name of flow definition constraint
     */
    public static String nameEstimatedFlowConstraint(String branchId) {
        return branchId + BLANK_CHARACTER + ESTIMATED_FLOW_EQUATION_POSTFIX;
    }

    /**
     * Get standard name of redispatch activation variables for preventive remedial actions
     */
    public static String nameRedispatchActivationVariableN(String remedialActionId) {
        return remedialActionId + BLANK_CHARACTER + REDISPATCH_ACTIVATION_N_POSTFIX;
    }

    /**
     * Get standard name of redispatch activation variables for curative remedial actions
     */
    public static String nameRedispatchActivationVariableCurative(String contingencyId, String remedialActionId) {
        return contingencyId + BLANK_CHARACTER + remedialActionId + BLANK_CHARACTER + REDISPATCH_ACTIVATION_CURATIVE_POSTFIX;
    }

    /**
     * Get standard name of redispatch cost variables for preventive remedial actions
     */
    public static String nameRedispatchCostVariableN(String remedialActionId) {
        return remedialActionId + BLANK_CHARACTER + REDISPATCH_COST_N_POSTFIX;
    }

    /**
     * Get standard name of redispatch cost variables for curative remedial actions
     */
    public static String nameRedispatchCostVariableCurative(String contingencyId, String remedialActionId) {
        return contingencyId + BLANK_CHARACTER + remedialActionId + BLANK_CHARACTER + REDISPATCH_COST_CURATIVE_POSTFIX;
    }

    /**
     * Get standard name of PST's shift value variables for preventive remedial actions
     */
    public static String nameShiftValueVariableN(String remedialActionId) {
        return remedialActionId + BLANK_CHARACTER + SHIFT_VALUE_N_POSTFIX;
    }

    /**
     * Get standard name of PST's shift value variables for curative remedial actions
     */
    public static String nameShiftValueVariableCurative(String contingencyId, String remedialActionId) {
        return contingencyId + BLANK_CHARACTER + remedialActionId + BLANK_CHARACTER + SHIFT_VALUE_CURATIVE_POSTFIX;
    }
}
