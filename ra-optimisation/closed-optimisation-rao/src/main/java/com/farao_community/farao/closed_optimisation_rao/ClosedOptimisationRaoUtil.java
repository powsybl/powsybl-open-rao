package com.farao_community.farao.closed_optimisation_rao;

import com.farao_community.farao.data.crac_file.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ClosedOptimisationRaoUtil {

    private ClosedOptimisationRaoUtil() {
        throw new AssertionError("Utility class should not have constructor");
    }

    public static Stream<RemedialAction> getPreventiveRemedialActions(CracFile cracFile){
        return cracFile.getRemedialActions().stream()
                .filter(ClosedOptimisationRaoUtil::isRemedialActionPreventiveFreeToUse);
    }

    public static Stream<RemedialAction> getCurativeRemedialActions(CracFile cracFile, Contingency contingency){
        return cracFile.getRemedialActions().stream()
                .filter(ra -> isRemedialActionCurativeAndApplicable(ra, contingency))
                .filter(ClosedOptimisationRaoUtil::isRedispatchRemedialAction);
    }


    public static List<RedispatchRemedialActionElement> getRedispatchRemedialActionElement(Stream<RemedialAction> RaList) {
        return RaList
                .filter(ClosedOptimisationRaoUtil::isRedispatchRemedialAction)
                .flatMap(remedialAction -> remedialAction.getRemedialActionElements().stream())
                .map(remedialActionElement -> (RedispatchRemedialActionElement) remedialActionElement)
                .collect(Collectors.toList());
    }

    public static List<PstElement> getPstElement(Stream<RemedialAction> RaList) {
        return RaList
                .filter(ClosedOptimisationRaoUtil::isPstRemedialAction)
                .flatMap(remedialAction -> remedialAction.getRemedialActionElements().stream())
                .map(remedialActionElement -> (PstElement) remedialActionElement)
                .collect(Collectors.toList());
    }
    /**
     * Check if the remedial action is a Redispatch remedial action (i.e. with only
     * one remedial action element and redispatch)
     */
    public static boolean isRedispatchRemedialAction(RemedialAction remedialAction) {
        return remedialAction.getRemedialActionElements().size() == 1 &&
                remedialAction.getRemedialActionElements().get(0) instanceof RedispatchRemedialActionElement;
    }

    /**
     * Check if the remedial action is a PST remedial action (i.e. with only
     * one remedial action element and pst)
     */
    public static boolean isPstRemedialAction(RemedialAction remedialAction) {
        return remedialAction.getRemedialActionElements().size() == 1 &&
                remedialAction.getRemedialActionElements().get(0) instanceof PstElement;
    }

    /**
     * Check if the remedial action is preventive (Instant = N) and free-to-use (Usage = FREE_TO_USE)
     */
    public static boolean isRemedialActionPreventiveFreeToUse(RemedialAction remedialAction) {
        return remedialAction.getUsageRules().stream().anyMatch(usageRule -> usageRule.getInstants().equals(UsageRule.Instant.N)
                && usageRule.getUsage().equals(UsageRule.Usage.FREE_TO_USE));
    }

    /**
     * Check if the remedial action is curative (Instant = CURATIVE) and free-to-use (Usage = FREE_TO_USE)
     */
    public static boolean isRemedialActionCurativeFreeToUse(RemedialAction remedialAction) {
        return remedialAction.getUsageRules().stream().anyMatch(usageRule -> usageRule.getInstants().equals(UsageRule.Instant.CURATIVE)
                && usageRule.getUsage().equals(UsageRule.Usage.FREE_TO_USE));
    }

    /**
     * Check if the remedial action is curative (Instant = CURATIVE), and :
     *   - free-to-use (Usage = FREE_TO_USE), or
     *   - applicable on contingency (Usage = ON_OUTAGE), for the given contingency
     */
    public static boolean isRemedialActionCurativeAndApplicable(RemedialAction remedialAction, Contingency contingency) {
        // is remedial action curative and free to use ?
        if(remedialAction.getUsageRules().stream().anyMatch(usageRule -> usageRule.getInstants().equals(UsageRule.Instant.CURATIVE)
                && usageRule.getUsage().equals(UsageRule.Usage.FREE_TO_USE))) {
            return true;
        }
        // is remedial action curative, on constraint for the given contingency ?
        if(remedialAction.getUsageRules().stream().anyMatch(usageRule -> usageRule.getInstants().equals(UsageRule.Instant.CURATIVE)
                && usageRule.getUsage().equals(UsageRule.Usage.ON_CONSTRAINT) && usageRule.getContingenciesID().contains(contingency.getId()))) {
            return true;
        }
        return false;
    }

}
