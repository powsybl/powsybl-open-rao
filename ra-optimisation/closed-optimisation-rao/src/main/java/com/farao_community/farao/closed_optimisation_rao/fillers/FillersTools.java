package com.farao_community.farao.closed_optimisation_rao.fillers;

import com.farao_community.farao.data.crac_file.RedispatchRemedialActionElement;
import com.farao_community.farao.data.crac_file.RemedialAction;
import com.farao_community.farao.data.crac_file.UsageRule;

public final class FillersTools {

    private FillersTools() {
        throw new AssertionError("Utility class should not have constructor");
    }

    private static final String REDISPATCH_VALUE_N_POSTFIX = "_redispatch_value";
    private static final String REDISPATCH_VALUE_CURATIVE_POSTFIX = "_redispatch_value";
    private static final String ESTIMATED_FLOW_EQUATION_POSTFIX = "_estimated_flow_equation";
    public static final String GEN_SENSITIVITIES_DATA_NAME = "generators_branch_sensitivities";
    private static final String BLANK_CHARACTER = "_";

    /**
     * Check if the remedial action is a Redispatch remedial action (i.e. with only
     * one remedial action element and redispatch)
     */
    public static boolean isRedispatchRemedialAction(RemedialAction remedialAction) {
        return remedialAction.getRemedialActionElements().size() == 1 &&
                remedialAction.getRemedialActionElements().get(0) instanceof RedispatchRemedialActionElement;
    }

    /**
     * Check if the remedial action is preventive (Instant = N) and free-to-use (Usage = FREE_TO_USE)
     */
    public static boolean isRemedialActionPreventiveFreeToUse(RemedialAction remedialAction) {
        return remedialAction.getUsageRules().stream().anyMatch(usageRule -> usageRule.getInstants().equals(UsageRule.Instant.N)
                && usageRule.getUsage().equals(UsageRule.Usage.FREE_TO_USE));
    }

    /**
     * Check if the remedial action is preventive (Instant = CURATIVE) and free-to-use (Usage = FREE_TO_USE)
     */
    public static boolean isRemedialActionCurativeFreeToUse(RemedialAction remedialAction) {
        return remedialAction.getUsageRules().stream().anyMatch(usageRule -> usageRule.getInstants().equals(UsageRule.Instant.CURATIVE)
                && usageRule.getUsage().equals(UsageRule.Usage.FREE_TO_USE));
    }

    /**
     * Get standard name of redispatch value variable for preventive remedial actions
     */
    public static String nameRedispatchValueVariableN(String remedialActionId)
    {
        return remedialActionId + REDISPATCH_VALUE_N_POSTFIX;
    }

    /**
     * Get standard name of flow definition constraint
     */
    public static String nameEstimatedFlowConstraint(String branchId)
    {
        return branchId + ESTIMATED_FLOW_EQUATION_POSTFIX;
    }

    /**
     * Get standard name of redispatch value variable for curative remedial actions
     */
    public static String nameRedispatchValueVariableCurative(String contingencyId, String remedialActionId)
    {
        return contingencyId + BLANK_CHARACTER + remedialActionId + REDISPATCH_VALUE_CURATIVE_POSTFIX;
    }
}
