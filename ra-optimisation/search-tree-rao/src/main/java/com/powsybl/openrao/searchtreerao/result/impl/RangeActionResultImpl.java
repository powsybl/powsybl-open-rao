/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Identifiable;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionResult;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * @author Baptiste Seguinot {@literal <joris.mancini at rte-france.com>}
 */
public class RangeActionResultImpl implements RangeActionResult {
    private final Map<RangeAction<?>, ElementaryResult> elementaryResultMap = new HashMap<>();
    boolean shouldRecomputeSetpoints = true;
    private Map<String, Double> setpointPerNetworkElementsIds;
    private static final double EPSILON_FOR_ACTIVATION = 1e-6;

    private static class ElementaryResult {
         double refSetpoint;
        private double optimizedSetpoint;

        ElementaryResult(double refSetpoint) {
            this.refSetpoint = refSetpoint;
            this.optimizedSetpoint = refSetpoint;
        }

        ElementaryResult(double refSetpoint, double optimizedSetpoint) {
            this.refSetpoint = refSetpoint;
            this.optimizedSetpoint = optimizedSetpoint;
        }

        private void activate(double setpoint) {
            this.optimizedSetpoint = setpoint;
        }

        private void preActivate(double setpoint) {
            this.refSetpoint = setpoint;
        }

        private boolean isActivated() {
            return Math.abs(optimizedSetpoint - refSetpoint) > EPSILON_FOR_ACTIVATION;
        }

        private double getSetpoint() {
            return optimizedSetpoint;
        }

    }

    public RangeActionResultImpl(RangeActionResult rangeActionResult) {
        rangeActionResult.getRangeActions().forEach(ra ->
            elementaryResultMap.put(ra, new ElementaryResult(rangeActionResult.getOptimizedSetpoint(ra))));
    }

    private RangeActionResultImpl(Map<RangeAction<?>, ElementaryResult> elementaryResultMap) {
        this.elementaryResultMap.putAll(elementaryResultMap);
    }

    public static RangeActionResultImpl buildWithSetpointsFromNetwork(Network network, Set<RangeAction<?>> rangeActions) {
        Map<RangeAction<?>, ElementaryResult> elementaryResultMapFromNetwork = new HashMap<>();
        rangeActions.forEach(rangeAction -> elementaryResultMapFromNetwork.put(rangeAction, new ElementaryResult(rangeAction.getCurrentSetpoint(network))));
        return new RangeActionResultImpl(elementaryResultMapFromNetwork);
    }

    public static RangeActionResultImpl buildFromPreviousResult(RangeActionResult rangeActionResult) {
        Map<RangeAction<?>, ElementaryResult> elementaryResultMapFromResult = new HashMap<>();
        rangeActionResult.getOptimizedSetpoints().forEach((ra, setpoint) -> elementaryResultMapFromResult.put(ra, new ElementaryResult(setpoint)));
        return new RangeActionResultImpl(elementaryResultMapFromResult);
    }

    public static RangeActionResultImpl buildFromPreviousResult(RangeActionResult rangeActionResult, Set<RangeAction<?>> rangeActions) {
        Map<RangeAction<?>, ElementaryResult> elementaryResultMapFromResult = new HashMap<>();
        rangeActions.forEach(ra -> elementaryResultMapFromResult.put(ra, new ElementaryResult(rangeActionResult.getOptimizedSetpoint(ra))));
        return new RangeActionResultImpl(elementaryResultMapFromResult);
    }

    public void activate(RangeAction<?> rangeAction, double setpoint) {
        elementaryResultMap.get(rangeAction).activate(setpoint);
    }

    public void preActivate(RangeAction<?> rangeAction, double setpoint) {
        String networkElementIds = concatenateNetworkElementsIds(rangeAction);
        elementaryResultMap.keySet().stream()
            .filter(otherRangeAction -> concatenateNetworkElementsIds(otherRangeAction).equals(networkElementIds))
            .forEach(otherRangeAction -> elementaryResultMap.get(otherRangeAction).preActivate(setpoint));
    }

    private synchronized void computeSetpointsPerNetworkElementIds() {
        if (!shouldRecomputeSetpoints) {
            return;
        }
        setpointPerNetworkElementsIds = new HashMap<>();
        elementaryResultMap.forEach((rangeAction, elementaryResult) -> {
            String networkElementsIds = concatenateNetworkElementsIds(rangeAction);
            setpointPerNetworkElementsIds.computeIfAbsent(networkElementsIds, raId -> elementaryResult.getSetpoint());
        });
        shouldRecomputeSetpoints = false;
    }

    private String concatenateNetworkElementsIds(RangeAction<?> rangeAction) {
        return rangeAction.getNetworkElements().stream().map(Identifiable::getId).sorted().collect(Collectors.joining("+"));
    }

    @Override
    public Set<RangeAction<?>> getRangeActions() {
        return elementaryResultMap.keySet();
    }

    @Override
    public Set<RangeAction<?>> getActivatedRangeActions() {
        return elementaryResultMap.entrySet().stream()
            .filter(e -> e.getValue().isActivated())
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }

    @Override
    public double getOptimizedSetpoint(RangeAction<?> rangeAction) {
        computeSetpointsPerNetworkElementIds();
        String networkElementsIds = concatenateNetworkElementsIds(rangeAction);
        // if at least one elementary result is on the correct network elements, find the right state to get the setpoint
        // else return the reference setpoint
        if (setpointPerNetworkElementsIds.containsKey(networkElementsIds)) {
            return setpointPerNetworkElementsIds.get(networkElementsIds);
        } else {
            throw new OpenRaoException(format("range action %s is not present in the result", rangeAction.getName()));
        }
    }

    @Override
    public Map<RangeAction<?>, Double> getOptimizedSetpoints() {
        computeSetpointsPerNetworkElementIds();
        Map<RangeAction<?>, Double> optimizedSetpoints = new HashMap<>();
        elementaryResultMap.keySet().forEach(ra -> optimizedSetpoints.put(ra, getOptimizedSetpoint(ra)));
        return optimizedSetpoints;
    }

    @Override
    public int getOptimizedTap(PstRangeAction pstRangeAction) {
        computeSetpointsPerNetworkElementIds();
        return pstRangeAction.convertAngleToTap(getOptimizedSetpoint(pstRangeAction));
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTaps() {
        computeSetpointsPerNetworkElementIds();
        Map<PstRangeAction, Integer> optimizedTaps = new HashMap<>();
        elementaryResultMap.keySet().stream()
            .filter(ra -> ra instanceof PstRangeAction)
            .map(ra -> (PstRangeAction) ra)
            .forEach(pstRa -> optimizedTaps.put(pstRa, getOptimizedTap(pstRa)));
        return optimizedTaps;
    }
}
