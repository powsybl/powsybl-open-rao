/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.monitoring.results;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.monitoring.api.SecurityStatus;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class MonitoringResult {

    private final PhysicalParameter physicalParameter;
    private Set<CnecResult<?>> cnecResults;
    private Map<State, Set<RemedialAction<?>>> appliedRas;
    private SecurityStatus status;

    public MonitoringResult(PhysicalParameter physicalParameter, Set<CnecResult<?>> cnecResults, Map<State, Set<RemedialAction<?>>> appliedRas, SecurityStatus status) {
        this.physicalParameter = physicalParameter;
        this.cnecResults = cnecResults;
        this.appliedRas = appliedRas;
        this.status = status;
    }

    public PhysicalParameter getPhysicalParameter() {
        return physicalParameter;
    }

    public Set<CnecResult<?>> getCnecResults() {
        return cnecResults;
    }

    public Map<State, Set<RemedialAction<?>>> getAppliedRas() {
        return appliedRas;
    }

    public Set<RemedialAction<?>> getAppliedRas(State state) {
        return appliedRas.getOrDefault(state, Collections.emptySet());
    }

    public Set<String> getAppliedRas(String stateId) {
        Set<State> states = appliedRas.keySet().stream().filter(s -> s.getId().equals(stateId)).collect(Collectors.toSet());
        if (states.isEmpty()) {
            return Collections.emptySet();
        } else if (states.size() > 1) {
            throw new OpenRaoException(String.format("%s states share the same id : %s.", states.size(), stateId));
        } else {
            return appliedRas.get(states.iterator().next()).stream().map(RemedialAction::getId).collect(Collectors.toSet());
        }
    }

    public SecurityStatus getStatus() {
        return status;
    }

    public List<String> printConstraints() {
        if (status.equals(SecurityStatus.FAILURE)) {
            return List.of(physicalParameter + " monitoring failed due to a load flow divergence or an inconsistency in the crac or in the parameters.");
        }
        List<String> constraints = new ArrayList<>();
        cnecResults.stream()
            .filter(cr -> cr.getMargin() < 0)
            .sorted(Comparator.comparing(CnecResult::getId))
            .forEach(cnecResult -> constraints.add(cnecResult.print()));

        if (constraints.isEmpty()) {
            return List.of(String.format("All %s Cnecs are secure.", physicalParameter));
        } else {
            constraints.add(0, String.format("Some %s Cnecs are not secure:", physicalParameter));
        }
        return constraints;
    }

    // Add synchronized in the signature to make the function blocking
    // Necessary because in the function runMonitoring this function is called in parallel threads -> can cause overwriting conflict.
    public synchronized void combine(MonitoringResult monitoringResult) {
        Set<CnecResult<?>> thisCnecResults = new HashSet<>(this.getCnecResults());
        Set<CnecResult<?>> otherCnecResults = monitoringResult.getCnecResults();
        thisCnecResults.addAll(otherCnecResults);
        this.cnecResults = thisCnecResults;

        Map<State, Set<RemedialAction<?>>> thisAppliedRas = new HashMap<>(this.getAppliedRas());
        Map<State, Set<RemedialAction<?>>> otherAppliedRas = monitoringResult.getAppliedRas();
        thisAppliedRas.putAll(otherAppliedRas);
        this.appliedRas = thisAppliedRas;

        this.status = combineStatuses(this.status, monitoringResult.getStatus());
    }

    public static SecurityStatus combineStatuses(SecurityStatus... statuses) {
        boolean atLeastOneFailed = Arrays.asList(statuses).contains(SecurityStatus.FAILURE);
        if (atLeastOneFailed) {
            return SecurityStatus.FAILURE;
        }

        boolean atLeastOneHigh = Arrays.asList(statuses).contains(SecurityStatus.HIGH_CONSTRAINT);
        boolean atLeastOneLow = Arrays.asList(statuses).contains(SecurityStatus.LOW_CONSTRAINT);
        boolean atLeastOneHighAndLow = Arrays.asList(statuses).contains(SecurityStatus.HIGH_AND_LOW_CONSTRAINTS) || atLeastOneHigh && atLeastOneLow;

        if (atLeastOneHighAndLow) {
            return SecurityStatus.HIGH_AND_LOW_CONSTRAINTS;
        }
        if (atLeastOneHigh) {
            return SecurityStatus.HIGH_CONSTRAINT;
        }
        if (atLeastOneLow) {
            return SecurityStatus.LOW_CONSTRAINT;
        }
        return SecurityStatus.SECURE;
    }

    public void setStatusToFailure() {
        this.status = SecurityStatus.FAILURE;
    }

}
