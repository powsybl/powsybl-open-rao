/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.rao_commons.result_api.OptimizationResult;
import com.google.common.hash.Hashing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
class RangeActionFilter {
    static final Logger LOGGER = LoggerFactory.getLogger(RangeActionFilter.class);

    private final Leaf leaf;
    private Set<RangeAction> rangeActionsToOptimize;
    private final State optimizedState;
    private final TreeParameters treeParameters;
    private final Map<RangeAction, Double> prePerimeterSetPoints;
    private final Set<RangeAction> leastPriorityRangeActions;

    public RangeActionFilter(Leaf leaf, Set<RangeAction> availableRangeActions, State optimizedState, TreeParameters treeParameters, Map<RangeAction, Double> prePerimeterSetPoints, boolean deprioritizeIgnoredRangeActions) {
        this.leaf = leaf;
        this.rangeActionsToOptimize = new HashSet<>(availableRangeActions);
        this.optimizedState = optimizedState;
        this.treeParameters = treeParameters;
        this.prePerimeterSetPoints = prePerimeterSetPoints;
        if (deprioritizeIgnoredRangeActions) {
            // define a lesser priority on range actions that have been ignored in previous optim
            this.leastPriorityRangeActions = availableRangeActions.stream().filter(ra -> leaf.getRangeActions().contains(ra) && leaf.getOptimizedSetPoint(ra) == prePerimeterSetPoints.get(ra)).collect(Collectors.toSet());
        } else {
            this.leastPriorityRangeActions = new HashSet<>();
        }
    }

    public Set<RangeAction> getRangeActionsToOptimize() {
        return rangeActionsToOptimize;
    }

    public void filterUnavailableRangeActions() {
        rangeActionsToOptimize.removeIf(ra -> !isRangeActionUsed(ra, leaf) && !SearchTree.isRemedialActionAvailable(ra, optimizedState, leaf));
    }

    public void filterPstPerTso() {
        Map<String, Integer> maxPstPerTso = recomputeMaxPstPerTso(leaf);
        if (maxPstPerTso.isEmpty()) {
            return;
        }
        // Filter the psts from Tso present in the map depending on their sensitivity
        maxPstPerTso.forEach((tso, maxPst) -> {
            Set<RangeAction> pstsForTso = rangeActionsToOptimize.stream()
                    .filter(rangeAction -> (rangeAction instanceof PstRangeAction) && rangeAction.getOperator().equals(tso))
                    .collect(Collectors.toSet());
            if (pstsForTso.size() > maxPst) {
                Set<RangeAction> rangeActionsToRemove = computeRangeActionsToExclude(pstsForTso, maxPst);
                if (!rangeActionsToRemove.isEmpty()) {
                    LOGGER.info("{} range actions have been filtered out in order to respect the maximum allowed number of pst for tso {}", rangeActionsToRemove.size(), tso);
                    rangeActionsToOptimize.removeAll(rangeActionsToRemove);
                }
            }
        });
    }

    /**
     * Create an updated version of maxPstPerTso map so as to deduce the number of applied network actions from the
     * total number of ra per tso and compare this value to max number of pst per Tso set in the treeParameters.
     */
    private Map<String, Integer> recomputeMaxPstPerTso(Leaf leaf) {

        Map<String, Integer> maxPstPerTso = new HashMap<>(treeParameters.getMaxPstPerTso());
        treeParameters.getMaxRaPerTso().forEach((tso, raLimit) -> {
            int appliedNetworkActionsForTso = (int) leaf.getActivatedNetworkActions().stream().filter(networkAction -> networkAction.getOperator().equals(tso)).count();
            int appliedNonPstRangeActionsForTso = (int) rangeActionsToOptimize.stream().filter(ra -> ra.getOperator().equals(tso) && !(ra instanceof PstRangeAction) && isRangeActionUsed(ra, leaf)).count();
            int pstLimit = raLimit - appliedNetworkActionsForTso - appliedNonPstRangeActionsForTso;
            maxPstPerTso.put(tso, Math.min(pstLimit, maxPstPerTso.getOrDefault(tso, Integer.MAX_VALUE)));
        });
        return maxPstPerTso;
    }

    public void filterTsos() {
        int maxTso = treeParameters.getMaxTso();
        if (maxTso == Integer.MAX_VALUE) {
            return;
        }
        Set<RangeAction> appliedRangeActions = rangeActionsToOptimize.stream().filter(rangeAction -> isRangeActionUsed(rangeAction, leaf)).collect(Collectors.toSet());
        Set<String> activatedTsos = leaf.getActivatedNetworkActions().stream().map(RemedialAction::getOperator).collect(Collectors.toSet());
        activatedTsos.addAll(appliedRangeActions.stream().map(RemedialAction::getOperator).collect(Collectors.toSet()));

        Set<String> tsosToKeep = sortTsosToKeepBySensitivityAndGroupId(activatedTsos, maxTso);

        Set<RangeAction> rangeActionsToRemove = rangeActionsToOptimize.stream().filter(rangeAction -> !tsosToKeep.contains(rangeAction.getOperator())).collect(Collectors.toSet());
        if (!rangeActionsToRemove.isEmpty()) {
            LOGGER.info("{} range actions have been filtered out in order to respect the maximum allowed number of tsos", rangeActionsToRemove.size());
            rangeActionsToOptimize.removeAll(rangeActionsToRemove);
        }
    }

    Set<String> sortTsosToKeepBySensitivityAndGroupId(Set<String> activatedTsos, int maxTso) {
        List<RangeAction> rangeActionsSortedBySensitivity = rangeActionsToOptimize.stream()
                .sorted((ra1, ra2) -> -comparePotentialGain(ra1, ra2, leaf.getMostLimitingElements(1).get(0), leaf))
                .collect(Collectors.toList());

        Set<String> tsosToKeep = new HashSet<>(activatedTsos);

        // Look for aligned PSTs : PSTs sharing a groupId must be all kept, or all filtered out.
        Set<String> groupIdHasBeenExplored = new HashSet<>();
        for (RangeAction ra : rangeActionsSortedBySensitivity) {
            // If ra potentially has aligned PSTs
            Optional<String> raGroupId = ra.getGroupId();
            if (raGroupId.isPresent()) {
                // ra has already been explored.
                if (groupIdHasBeenExplored.contains(raGroupId.get())) {
                    continue;
                }

                Set<String> tsosToKeepIfAlignedPstAreKept = new HashSet<>(tsosToKeep);

                Set<RangeAction> raWithSameGroupId =
                        rangeActionsSortedBySensitivity.stream().filter(rangeAction -> {
                            Optional<String> groupId = rangeAction.getGroupId();
                            return groupId.isPresent() && groupId.get().equals(raGroupId.get());
                        }).collect(Collectors.toSet());

                tsosToKeepIfAlignedPstAreKept.addAll(raWithSameGroupId.stream().map(RemedialAction::getOperator).collect(Collectors.toSet()));
                groupIdHasBeenExplored.add(raGroupId.get());

                // remove aligned pst from range actions to optimize
                if (tsosToKeepIfAlignedPstAreKept.size() > maxTso) {
                    rangeActionsToOptimize.removeAll(raWithSameGroupId);
                    LOGGER.info("{} range actions have been filtered out in order to respect the maximum allowed number of tsos on aligned PSTs.", raWithSameGroupId.size());
                } else {
                    tsosToKeep = tsosToKeepIfAlignedPstAreKept;
                }
            } else {
                if (tsosToKeep.size() >= maxTso) {
                    continue;
                }
                tsosToKeep.add(ra.getOperator());
            }
        }
        return tsosToKeep;
    }

    public void filterMaxRas() {
        if (treeParameters.getMaxRa() == Integer.MAX_VALUE) {
            return;
        }
        int numberOfNetworkActionsAlreadyApplied = leaf.getActivatedNetworkActions().size();
        Set<RangeAction> rangeActionsToRemove = computeRangeActionsToExclude(rangeActionsToOptimize, treeParameters.getMaxRa() - numberOfNetworkActionsAlreadyApplied);
        if (!rangeActionsToRemove.isEmpty()) {
            LOGGER.info("{} range actions have been filtered out in order to respect the maximum allowed number of remedial actions", rangeActionsToRemove.size());
            rangeActionsToOptimize.removeAll(rangeActionsToRemove);
        }
    }

    private Set<RangeAction> computeRangeActionsToExclude(Set<RangeAction> rangeActions, int numberOfRangeActionsToKeep) {
        if (numberOfRangeActionsToKeep < 0) {
            throw new InvalidParameterException("Trying to keep a negative number of remedial actions");
        }
        int updatedNumberOfRangeActionsToKeep = numberOfRangeActionsToKeep;
        Set<RangeAction> rangeActionsToExclude = new HashSet<>(rangeActions);
        // If in previous depth some RangeActions were activated, consider them optimizable and decrement the allowed number of PSTs
        // We have to do this because at the end of every depth, we apply optimal RangeActions for the next depth
        removeAppliedRangeActions(rangeActionsToExclude);
        updatedNumberOfRangeActionsToKeep -= rangeActions.size() - rangeActionsToExclude.size();
        // Then keep the range actions with the biggest impact
        removeRangeActionsWithBiggestImpact(rangeActionsToExclude, updatedNumberOfRangeActionsToKeep);
        return rangeActionsToExclude;
    }

    /**
     * Removes from a set of RangeActions the ones that were previously used
     *
     * @param rangeActions the set of RangeActions to filter
     */
    private void removeAppliedRangeActions(Set<RangeAction> rangeActions) {
        Set<RangeAction> appliedRangeActions = rangeActions.stream().filter(rangeAction -> isRangeActionUsed(rangeAction, leaf)).collect(Collectors.toSet());
        rangeActions.removeAll(appliedRangeActions);
    }

    /**
     * Removes from a set of RangeActions the ones with the biggest priority or sensitivity on the most limiting element
     *
     * @param rangeActions   the set of RangeActions to filter
     * @param numberToRemove the number of RangeActions to remove from the set
     */
    private void removeRangeActionsWithBiggestImpact(Set<RangeAction> rangeActions, int numberToRemove) {
        if (numberToRemove <= 0) {
            // Nothing to do
            return;
        } else if (numberToRemove >= rangeActions.size()) {
            rangeActions.clear();
            return;
        }
        List<RangeAction> rangeActionsSortedBySensitivity = rangeActions.stream()
                .sorted((ra1, ra2) -> comparePrioritiesAndSensitivities(ra1, ra2, leaf.getMostLimitingElements(1).get(0), leaf))
                .collect(Collectors.toList());

        Set<String> groupIdHasBeenExplored = new HashSet<>();
        int numberToRemoveLeft = numberToRemove;
        // Look for aligned PSTs
        for (RangeAction ra : rangeActionsSortedBySensitivity) {
            if (numberToRemoveLeft == 0) {
                return;
            }
            // If ra potentially has aligned PSTs
            Optional<String> raGroupId = ra.getGroupId();
            if (raGroupId.isPresent()) {
                // ra has already been explored.
                if (groupIdHasBeenExplored.contains(raGroupId.get())) {
                    continue;
                }
                Set<RangeAction> raWithSameGroupId =
                        rangeActionsSortedBySensitivity.stream().filter(rangeAction -> {
                            Optional<String> groupId = rangeAction.getGroupId();
                            return groupId.isPresent() && groupId.get().equals(raGroupId.get());
                        }).collect(Collectors.toSet());

                groupIdHasBeenExplored.add(raGroupId.get());
                // Remove all ra with same groupId, or none
                if (raWithSameGroupId.size() <= numberToRemoveLeft) {
                    rangeActions.removeAll(raWithSameGroupId);
                    numberToRemoveLeft -= raWithSameGroupId.size();
                }
            } else {
                rangeActions.remove(ra);
                numberToRemoveLeft -= 1;
            }
        }
    }

    /**
     * First compares priority then sensi
     * If a range action has more priority (depending on the contents of leastPriorityRangeActions) than the other, then
     * it will be considered greater.
     * If both RAs have the same priority, then absolute sensitivities will be compared.
     */
    private int comparePrioritiesAndSensitivities(RangeAction ra1, RangeAction ra2, FlowCnec cnec, OptimizationResult optimizationResult) {
        if (!leastPriorityRangeActions.contains(ra1) && leastPriorityRangeActions.contains(ra2)) {
            return -1;
        } else if (leastPriorityRangeActions.contains(ra1) && !leastPriorityRangeActions.contains(ra2)) {
            return 1;
        } else {
            return -comparePotentialGain(ra1, ra2, cnec, optimizationResult);
        }
    }

    private int comparePotentialGain(RangeAction ra1, RangeAction ra2, FlowCnec cnec, OptimizationResult optimizationResult) {
        Double gain1 = computePotentialGain(ra1, cnec, optimizationResult);
        Double gain2 = computePotentialGain(ra2, cnec, optimizationResult);
        int comparison = gain1.compareTo(gain2);
        return comparison != 0 ? comparison : orderRangeActionsRandomly(ra1, ra2);
    }

    private Double computePotentialGain(RangeAction rangeAction, FlowCnec cnec, OptimizationResult optimizationResult) {
        double directMargin = cnec.getUpperBound(Side.LEFT, Unit.MEGAWATT).orElse(Double.POSITIVE_INFINITY) - optimizationResult.getFlow(cnec, Unit.MEGAWATT);
        double oppositeMargin = optimizationResult.getFlow(cnec, Unit.MEGAWATT) - cnec.getLowerBound(Side.LEFT, Unit.MEGAWATT).orElse(Double.NEGATIVE_INFINITY);
        double sensi = optimizationResult.getSensitivityValue(cnec, rangeAction, Unit.MEGAWATT);
        double currentSetPoint = optimizationResult.getOptimizedSetPoint(rangeAction);
        if (directMargin < oppositeMargin) {
            if (sensi > 0) {
                return sensi * (currentSetPoint - rangeAction.getMinAdmissibleSetpoint(prePerimeterSetPoints.get(rangeAction)));
            } else {
                return sensi * (currentSetPoint - rangeAction.getMaxAdmissibleSetpoint(prePerimeterSetPoints.get(rangeAction)));
            }
        } else {
            if (sensi > 0) {
                return sensi * (rangeAction.getMaxAdmissibleSetpoint(prePerimeterSetPoints.get(rangeAction)) - currentSetPoint);
            } else {
                return sensi * (rangeAction.getMinAdmissibleSetpoint(prePerimeterSetPoints.get(rangeAction)) - currentSetPoint);
            }

        }

    }

    private int orderRangeActionsRandomly(RangeAction ra1, RangeAction ra2) {
        return Hashing.crc32().hashString(ra1.getId(), StandardCharsets.UTF_8).hashCode() - Hashing.crc32().hashString(ra2.getId(), StandardCharsets.UTF_8).hashCode();
    }

    boolean isRangeActionUsed(RangeAction rangeAction, Leaf leaf) {
        return leaf.getRangeActions().contains(rangeAction) && Math.abs(leaf.getOptimizedSetPoint(rangeAction) - prePerimeterSetPoints.get(rangeAction)) >= 1e-6;
    }
}
