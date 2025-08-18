/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring.results;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.monitoring.SecurityStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public abstract class AbstractMonitoringResult<I extends Cnec<?>> implements MonitoringResult<I> {
    protected final PhysicalParameter physicalParameter;
    protected Set<CnecResult<I>> cnecResults;
    protected Map<State, Set<RemedialAction<?>>> appliedRas;
    protected SecurityStatus status;

    protected AbstractMonitoringResult(PhysicalParameter physicalParameter, Set<CnecResult<I>> cnecResults, Map<State, Set<RemedialAction<?>>> appliedRas, SecurityStatus status) {
        this.physicalParameter = physicalParameter;
        this.cnecResults = cnecResults;
        this.appliedRas = appliedRas;
        this.status = status;
    }

    @Override
    public Set<CnecResult<I>> getCnecResults() {
        return cnecResults;
    }

    @Override
    public Map<State, Set<RemedialAction<?>>> getAppliedRas() {
        return appliedRas;
    }

    @Override
    public Set<RemedialAction<?>> getAppliedRas(State state) {
        return appliedRas.getOrDefault(state, Collections.emptySet());
    }

    @Override
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

    @Override
    public SecurityStatus getStatus() {
        return status;
    }

    @Override
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

    @Override
    public synchronized void combine(MonitoringResult<I> monitoringResult) {
        Set<CnecResult<I>> thisCnecResults = new HashSet<>(this.getCnecResults());
        Set<CnecResult<I>> otherCnecResults = monitoringResult.getCnecResults();
        thisCnecResults.addAll(otherCnecResults);
        this.cnecResults = thisCnecResults;

        Map<State, Set<RemedialAction<?>>> thisAppliedRas = new HashMap<>(this.getAppliedRas());
        Map<State, Set<RemedialAction<?>>> otherAppliedRas = monitoringResult.getAppliedRas();
        thisAppliedRas.putAll(otherAppliedRas);
        this.appliedRas = thisAppliedRas;

        this.status = MonitoringResult.combineStatuses(this.status, monitoringResult.getStatus());
    }

    @Override
    public void setStatusToFailure() {
        this.status = SecurityStatus.FAILURE;
    }
}
