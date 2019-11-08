/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.closed_optimisation_rao;

import com.farao_community.farao.data.crac_file.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility class designed to manipulate the remedial actions of a Crac File
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public final class ClosedOptimisationRaoUtil {

    private ClosedOptimisationRaoUtil() {
        throw new AssertionError("Utility class should not have constructor");
    }

    /**
     * Build map of RedispatchingRemedialActionElements with their associated contingency
     * Required for the initialisation of all fillers which invokes redispatching remedial action
     */
    public static Map<Optional<Contingency>, List<RemedialAction>> buildRedispatchRemedialActionMap(CracFile cracFile) {
        Map<Optional<Contingency>, List<RemedialAction>> redispatchingRemedialActions = new HashMap<>();
        // add preventive redispatching remedial actions (in that case, the Hashmap key is empty)
        redispatchingRemedialActions.put(Optional.empty(), getRedispatchRemedialActions(getPreventiveRemedialActions(cracFile)));
        // add curative redispatching remedial actions
        cracFile.getContingencies().forEach(contingency -> redispatchingRemedialActions.put(Optional.of(contingency),
                getRedispatchRemedialActions(getCurativeRemedialActions(cracFile, contingency))));
        return redispatchingRemedialActions;
    }

    /**
     * Build map of PstElement with their associated contingency
     * Required for the initialisation of all fillers which invokes PST remedial action
     */
    public static Map<Optional<Contingency>, List<PstElement>> buildPstRemedialActionMap(CracFile cracFile) {
        Map<Optional<Contingency>, List<PstElement>> pstRemedialActions = new HashMap<>();
        // add preventive pst remedial actions (in that case, the Hashmap key is empty)
        pstRemedialActions.put(Optional.empty(), getPstElement(getPreventiveRemedialActions(cracFile)));
        // add curative pst remedial actions
        cracFile.getContingencies().forEach(contingency -> pstRemedialActions.put(Optional.of(contingency),
                getPstElement(getCurativeRemedialActions(cracFile, contingency))));
        return pstRemedialActions;
    }

    /**
     * Get all preventive remedial actions of a cracFile
     */
    public static Stream<RemedialAction> getPreventiveRemedialActions(CracFile cracFile) {
        return cracFile.getRemedialActions().stream()
                .filter(ClosedOptimisationRaoUtil::isRemedialActionPreventiveFreeToUse);
    }

    /**
     * Get curative remedial actions applicable on a given N-1 contingency
     */
    public static Stream<RemedialAction> getCurativeRemedialActions(CracFile cracFile, Contingency contingency) {
        return cracFile.getRemedialActions().stream()
                .filter(ra -> isRemedialActionCurativeAndApplicable(ra, contingency));
    }

    /**
     * Get redispatching remedial action from a stream of RemedialAction and convert
     * them into a list of RedispatchingRemedialActionElement
     */
    public static List<RemedialAction> getRedispatchRemedialActions(Stream<RemedialAction> raList) {
        return raList
                .filter(ClosedOptimisationRaoUtil::isRedispatchRemedialAction)
                .collect(Collectors.toList());
    }

    /**
     * Get Pst remedial action from a stream of RemedialAction and convert
     * them into a list of PstElement
     */
    public static List<PstElement> getPstElement(Stream<RemedialAction> raList) {
        return raList
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
     * Check if the remedial action is curative (Instant = CURATIVE), and :
     * - free-to-use (Usage = FREE_TO_USE), or
     * - applicable on contingency (Usage = ON_OUTAGE), for the given contingency
     */
    public static boolean isRemedialActionCurativeAndApplicable(RemedialAction remedialAction, Contingency contingency) {
        // is remedial action curative and free to use ?
        if (remedialAction.getUsageRules().stream().anyMatch(usageRule -> usageRule.getInstants().equals(UsageRule.Instant.CURATIVE)
                && usageRule.getUsage().equals(UsageRule.Usage.FREE_TO_USE))) {
            return true;
        }
        // is remedial action curative, on constraint for the given contingency ?
        if (remedialAction.getUsageRules().stream().anyMatch(usageRule -> usageRule.getInstants().equals(UsageRule.Instant.CURATIVE)
                && usageRule.getUsage().equals(UsageRule.Usage.ON_OUTAGE) && usageRule.getContingenciesID().contains(contingency.getId()))) {
            return true;
        }
        return false;
    }

    /**
     * Get redispatchRemedialActionElement from remedialAction
     * Or, return null if :
     * - the remedialAction includes several RemedialActionElement
     * - the remedialActionElement is not a redispatchRemedialActionElement
     */
    public static RedispatchRemedialActionElement getRedispatchElement(RemedialAction remedialAction) {
        List<RemedialActionElement> raeList = remedialAction.getRemedialActionElements();
        if (raeList.size() != 1) {
            return null;
        }
        if (!(raeList.get(0) instanceof RedispatchRemedialActionElement)) {
            return null;
        }
        return (RedispatchRemedialActionElement) raeList.get(0);
    }

    /**
     * Get all monitored branch (pre and post contingency) from a CracFile
     */
    public static List<MonitoredBranch> getAllMonitoredBranches(CracFile cracFile) {
        List<MonitoredBranch> allMonitoredBranches = new ArrayList<>();
        // add pre-contingency monitored branches
        allMonitoredBranches.addAll(cracFile.getPreContingency().getMonitoredBranches());
        // add post-contingency monitored branches
        allMonitoredBranches.addAll(cracFile.getContingencies().stream()
                .flatMap(contingency -> contingency.getMonitoredBranches().stream())
                .collect(Collectors.toList()));
        return allMonitoredBranches;
    }

    /**
     * Test the significance of a given sensitivity
     */
    public static boolean isSignificant(double sensitivity, double threshold) {
        return Math.abs(sensitivity) > threshold;
    }

}
