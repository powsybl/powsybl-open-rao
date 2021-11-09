/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.powsybl.commons.extensions.AbstractExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class SearchTreeRaoParameters extends AbstractExtension<RaoParameters> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchTreeRaoParameters.class);

    public enum PreventiveRaoStopCriterion {
        MIN_OBJECTIVE,
        SECURE
    }

    public enum CurativeRaoStopCriterion {
        MIN_OBJECTIVE, // only stop after minimizing objective
        SECURE, //stop when objective is strictly negative
        PREVENTIVE_OBJECTIVE, // stop when preventive objective is reached, or bested by curativeRaoMinObjImprovement
        PREVENTIVE_OBJECTIVE_AND_SECURE // stop when preventive objective is reached or bested by curativeRaoMinObjImprovement, and the situation is secure
        // TODO : we can add WORST_OBJECTIVE and WORST_OBJECTIVE_AND_SECURE if we want to use the worst curative perimeter objective as a stop criterion for other curative perimeters too
    }

    public enum SecondPreventiveRaoCondition {
        DISABLED, // do not run 2nd preventive RAO
        POSSIBLE_CURATIVE_IMPROVEMENT, // run 2nd preventive RAO if curative results can be improved, taking into account the curative RAO stop criterion
        COST_INCREASE // run 2nd preventive RAO if curative results can be improved + only if the overall cost has increased during RAO (ie if preventive RAO degraded a curative CNEC's margin or created a curative virtual cost)
    }

    static final int DEFAULT_MAXIMUM_SEARCH_DEPTH = Integer.MAX_VALUE;
    static final double DEFAULT_NETWORK_ACTION_MINIMUM_IMPACT_THRESHOLD = 0;
    static final int DEFAULT_PREVENTIVE_LEAVES_IN_PARALLEL = 1;
    static final int DEFAULT_CURATIVE_LEAVES_IN_PARALLEL = 1;
    static final boolean DEFAULT_SKIP_NETWORK_ACTIONS_FAR_FROM_MOST_LIMITING_ELEMENT = false;
    static final int DEFAULT_MAX_NUMBER_OF_BOUNDARIES_FOR_SKIPPING_NETWORK_ACTIONS = 2;
    static final PreventiveRaoStopCriterion DEFAULT_PREVENTIVE_RAO_STOP_CRITERION = PreventiveRaoStopCriterion.SECURE;
    static final CurativeRaoStopCriterion DEFAULT_CURATIVE_RAO_STOP_CRITERION = CurativeRaoStopCriterion.MIN_OBJECTIVE;
    static final double DEFAULT_CURATIVE_RAO_MIN_OBJ_IMPROVEMENT = 0;
    static final int DEFAULT_MAX_CURATIVE_RA = Integer.MAX_VALUE;
    static final int DEFAULT_MAX_CURATIVE_TSO = Integer.MAX_VALUE;
    static final Map<String, Integer> DEFAULT_MAX_CURATIVE_TOPO_PER_TSO = new HashMap<>();
    static final Map<String, Integer> DEFAULT_MAX_CURATIVE_PST_PER_TSO = new HashMap<>();
    static final Map<String, Integer> DEFAULT_MAX_CURATIVE_RA_PER_TSO = new HashMap<>();
    static final boolean DEFAULT_CURATIVE_RAO_OPTIMIZE_OPERATORS_NOT_SHARING_CRAS = true;
    static final SecondPreventiveRaoCondition DEFAULT_WITH_SECOND_PREVENTIVE_OPTIMIZATION = SecondPreventiveRaoCondition.DISABLED;
    static final List<List<String>> DEFAULT_NETWORK_ACTION_ID_COMBINATIONS = new ArrayList<>();

    private int maximumSearchDepth = DEFAULT_MAXIMUM_SEARCH_DEPTH;
    private double relativeNetworkActionMinimumImpactThreshold = DEFAULT_NETWORK_ACTION_MINIMUM_IMPACT_THRESHOLD;
    private double absoluteNetworkActionMinimumImpactThreshold = DEFAULT_NETWORK_ACTION_MINIMUM_IMPACT_THRESHOLD;
    private int preventiveLeavesInParallel = DEFAULT_PREVENTIVE_LEAVES_IN_PARALLEL;
    private int curativeLeavesInParallel = DEFAULT_CURATIVE_LEAVES_IN_PARALLEL;
    private boolean skipNetworkActionsFarFromMostLimitingElement = DEFAULT_SKIP_NETWORK_ACTIONS_FAR_FROM_MOST_LIMITING_ELEMENT;
    private int maxNumberOfBoundariesForSkippingNetworkActions = DEFAULT_MAX_NUMBER_OF_BOUNDARIES_FOR_SKIPPING_NETWORK_ACTIONS;
    private PreventiveRaoStopCriterion preventiveRaoStopCriterion = DEFAULT_PREVENTIVE_RAO_STOP_CRITERION;
    private CurativeRaoStopCriterion curativeRaoStopCriterion = DEFAULT_CURATIVE_RAO_STOP_CRITERION;
    private double curativeRaoMinObjImprovement = DEFAULT_CURATIVE_RAO_MIN_OBJ_IMPROVEMENT; // used for CurativeRaoStopCriterion.PREVENTIVE_OBJECTIVE and CurativeRaoStopCriterion.PREVENTIVE_OBJECTIVE_AND_SECURE
    private int maxCurativeRa = DEFAULT_MAX_CURATIVE_RA;
    private int maxCurativeTso = DEFAULT_MAX_CURATIVE_TSO;
    private Map<String, Integer> maxCurativeTopoPerTso = DEFAULT_MAX_CURATIVE_TOPO_PER_TSO;
    private Map<String, Integer> maxCurativePstPerTso = DEFAULT_MAX_CURATIVE_PST_PER_TSO;
    private Map<String, Integer> maxCurativeRaPerTso = DEFAULT_MAX_CURATIVE_RA_PER_TSO;
    private boolean curativeRaoOptimizeOperatorsNotSharingCras = DEFAULT_CURATIVE_RAO_OPTIMIZE_OPERATORS_NOT_SHARING_CRAS;
    private SecondPreventiveRaoCondition secondPreventiveOptimizationCondition = DEFAULT_WITH_SECOND_PREVENTIVE_OPTIMIZATION;
    private List<List<String>> networkActionIdCombinations = DEFAULT_NETWORK_ACTION_ID_COMBINATIONS;
    private List<NetworkActionCombination> networkActionCombinations = null;

    @Override
    public String getName() {
        return "SearchTreeRaoParameters";
    }

    public PreventiveRaoStopCriterion getPreventiveRaoStopCriterion() {
        return preventiveRaoStopCriterion;
    }

    public void setPreventiveRaoStopCriterion(PreventiveRaoStopCriterion preventiveRaoStopCriterion) {
        this.preventiveRaoStopCriterion = preventiveRaoStopCriterion;
    }

    public int getMaximumSearchDepth() {
        return maximumSearchDepth;
    }

    public void setMaximumSearchDepth(int maximumSearchDepth) {
        this.maximumSearchDepth = maximumSearchDepth;
    }

    public double getRelativeNetworkActionMinimumImpactThreshold() {
        return relativeNetworkActionMinimumImpactThreshold;
    }

    public void setRelativeNetworkActionMinimumImpactThreshold(double relativeNetworkActionMinimumImpactThreshold) {
        if (relativeNetworkActionMinimumImpactThreshold < 0) {
            LOGGER.warn("The value {} provided for relative network action minimum impact threshold is smaller than 0. It will be set to 0.", relativeNetworkActionMinimumImpactThreshold);
            this.relativeNetworkActionMinimumImpactThreshold = 0;
        } else if (relativeNetworkActionMinimumImpactThreshold > 1) {
            LOGGER.warn("The value {} provided for relative network action minimum impact threshold is greater than 1. It will be set to 1.", relativeNetworkActionMinimumImpactThreshold);
            this.relativeNetworkActionMinimumImpactThreshold = 1;
        } else {
            this.relativeNetworkActionMinimumImpactThreshold = relativeNetworkActionMinimumImpactThreshold;
        }
    }

    public double getAbsoluteNetworkActionMinimumImpactThreshold() {
        return absoluteNetworkActionMinimumImpactThreshold;
    }

    public void setAbsoluteNetworkActionMinimumImpactThreshold(double absoluteNetworkActionMinimumImpactThreshold) {
        this.absoluteNetworkActionMinimumImpactThreshold = absoluteNetworkActionMinimumImpactThreshold;
    }

    public int getPreventiveLeavesInParallel() {
        return preventiveLeavesInParallel;
    }

    public void setPreventiveLeavesInParallel(int preventiveLeavesInParallel) {
        this.preventiveLeavesInParallel = preventiveLeavesInParallel;
    }

    public int getCurativeLeavesInParallel() {
        return curativeLeavesInParallel;
    }

    public void setCurativeLeavesInParallel(int curativeLeavesInParallel) {
        this.curativeLeavesInParallel = curativeLeavesInParallel;
    }

    public boolean getSkipNetworkActionsFarFromMostLimitingElement() {
        return skipNetworkActionsFarFromMostLimitingElement;
    }

    public void setSkipNetworkActionsFarFromMostLimitingElement(boolean skipNetworkActionsFarFromMostLimitingElement) {
        this.skipNetworkActionsFarFromMostLimitingElement = skipNetworkActionsFarFromMostLimitingElement;
    }

    public int getMaxNumberOfBoundariesForSkippingNetworkActions() {
        return maxNumberOfBoundariesForSkippingNetworkActions;
    }

    public void setMaxNumberOfBoundariesForSkippingNetworkActions(int maxNumberOfBoundariesForSkippingNetworkActions) {
        if (maxNumberOfBoundariesForSkippingNetworkActions < 0) {
            LOGGER.warn("The value {} provided for max number of boundaries for skipping network actions is smaller than 0. It will be set to 0.", maxNumberOfBoundariesForSkippingNetworkActions);
            this.maxNumberOfBoundariesForSkippingNetworkActions = 0;
        } else {
            this.maxNumberOfBoundariesForSkippingNetworkActions = maxNumberOfBoundariesForSkippingNetworkActions;
        }
    }

    public CurativeRaoStopCriterion getCurativeRaoStopCriterion() {
        return curativeRaoStopCriterion;
    }

    public void setCurativeRaoStopCriterion(CurativeRaoStopCriterion curativeRaoStopCriterion) {
        this.curativeRaoStopCriterion = curativeRaoStopCriterion;
    }

    public double getCurativeRaoMinObjImprovement() {
        return curativeRaoMinObjImprovement;
    }

    public void setCurativeRaoMinObjImprovement(double curativeRaoMinObjImprovement) {
        if (curativeRaoMinObjImprovement < 0) {
            LOGGER.warn("The value {} provided for curative RAO minimum objective improvement is smaller than 0. It will be set to + {}", curativeRaoMinObjImprovement, -curativeRaoMinObjImprovement);
        }
        this.curativeRaoMinObjImprovement = Math.abs(curativeRaoMinObjImprovement);
    }

    public int getMaxCurativeRa() {
        return maxCurativeRa;
    }

    public void setMaxCurativeRa(int maxCurativeRa) {
        if (maxCurativeRa < 0) {
            LOGGER.warn("The value {} provided for max number of curative RAs is smaller than 0. It will be set to 0 instead.", maxCurativeRa);
            this.maxCurativeRa = 0;
        } else {
            this.maxCurativeRa = maxCurativeRa;
        }
    }

    public int getMaxCurativeTso() {
        return maxCurativeTso;
    }

    public void setMaxCurativeTso(int maxCurativeTso) {
        if (maxCurativeTso < 0) {
            LOGGER.warn("The value {} provided for max number of curative TSOs is smaller than 0. It will be set to 0 instead.", maxCurativeTso);
            this.maxCurativeTso = 0;
        } else {
            this.maxCurativeTso = maxCurativeTso;
        }
    }

    public Map<String, Integer> getMaxCurativeTopoPerTso() {
        return maxCurativeTopoPerTso;
    }

    public void setMaxCurativeTopoPerTso(Map<String, Integer> maxCurativeTopoPerTso) {
        if (Objects.isNull(maxCurativeTopoPerTso)) {
            this.maxCurativeTopoPerTso = new HashMap<>();
        } else {
            this.maxCurativeTopoPerTso = maxCurativeTopoPerTso;
        }
    }

    public Map<String, Integer> getMaxCurativePstPerTso() {
        return maxCurativePstPerTso;
    }

    public void setMaxCurativePstPerTso(Map<String, Integer> maxCurativePstPerTso) {
        if (Objects.isNull(maxCurativePstPerTso)) {
            this.maxCurativePstPerTso = new HashMap<>();
        } else {
            this.maxCurativePstPerTso = maxCurativePstPerTso;
        }
    }

    public Map<String, Integer> getMaxCurativeRaPerTso() {
        return maxCurativeRaPerTso;
    }

    public void setMaxCurativeRaPerTso(Map<String, Integer> maxCurativeRaPerTso) {
        if (Objects.isNull(maxCurativeRaPerTso)) {
            this.maxCurativeRaPerTso = new HashMap<>();
        } else {
            this.maxCurativeRaPerTso = maxCurativeRaPerTso;
        }
    }

    public boolean getCurativeRaoOptimizeOperatorsNotSharingCras() {
        return curativeRaoOptimizeOperatorsNotSharingCras;
    }

    public void setCurativeRaoOptimizeOperatorsNotSharingCras(boolean curativeRaoOptimizeOperatorsNotSharingCras) {
        this.curativeRaoOptimizeOperatorsNotSharingCras = curativeRaoOptimizeOperatorsNotSharingCras;
    }

    public SecondPreventiveRaoCondition getSecondPreventiveOptimizationCondition() {
        return secondPreventiveOptimizationCondition;
    }

    public void setSecondPreventiveOptimizationCondition(SecondPreventiveRaoCondition secondPreventiveOptimizationCondition) {
        this.secondPreventiveOptimizationCondition = secondPreventiveOptimizationCondition;
    }

    public List<List<String>> getNetworkActionIdCombinations() {
        return networkActionIdCombinations;
    }

    public void setNetworkActionIdCombinations(List<List<String>> networkActionCombinations) {
        this.networkActionIdCombinations = networkActionCombinations;
    }

    public List<NetworkActionCombination> getNetworkActionCombinations(Crac crac) {
        if (networkActionCombinations == null) {
            networkActionCombinations = new ArrayList<>();
            networkActionIdCombinations.forEach(networkActionIds -> {
                Optional<NetworkActionCombination> optNaCombination = getNetworkActionCombinationFromIds(networkActionIds, crac);
                optNaCombination.ifPresent(networkActionCombination -> networkActionCombinations.add(networkActionCombination));
            });
        }
        return networkActionCombinations;
    }

    private Optional<NetworkActionCombination> getNetworkActionCombinationFromIds(List<String> networkActionIds, Crac crac) {

        if (networkActionIds.size() < 2) {
            LOGGER.warn("A network-action-combination should at least contains 2 NetworkAction ids");
            return Optional.empty();
        }

        Set<NetworkAction> networkActions = new HashSet<>();
        for (String naId : networkActionIds) {
            NetworkAction na = crac.getNetworkAction(naId);
            if (na == null) {
                LOGGER.warn("Unknown network action id in network-action-combinations parameter: {}", naId);
                return Optional.empty();
            }
            networkActions.add(na);
        }

        return Optional.of(new NetworkActionCombination(networkActions));
    }

}
