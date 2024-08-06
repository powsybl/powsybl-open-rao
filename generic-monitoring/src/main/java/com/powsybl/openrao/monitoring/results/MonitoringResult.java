package com.powsybl.openrao.monitoring.results;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.data.cracapi.RemedialAction;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.Cnec.CnecSecurityStatus;

import java.util.*;
import java.util.stream.Collectors;

public class MonitoringResult {

    private PhysicalParameter physicalParameter;
    private Set<CnecResult> cnecResults;
    private Map<State, Set<RemedialAction>> appliedRas;
    private CnecSecurityStatus status;

    public MonitoringResult(PhysicalParameter physicalParameter, Set<CnecResult> cnecResults, Map<State, Set<RemedialAction>> appliedRas, CnecSecurityStatus status) {
        this.physicalParameter = physicalParameter;
        this.cnecResults = cnecResults;
        this.appliedRas = appliedRas;
        this.status = status;
    }

    public PhysicalParameter getPhysicalParameter() {
        return physicalParameter;
    }

    public Set<CnecResult> getCnecResults() {
        return cnecResults;
    }

    public Map<State, Set<RemedialAction>> getAppliedRas() {
        return appliedRas;
    }

    public Set<RemedialAction> getAppliedRas(State state) {
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

    public CnecSecurityStatus getStatus() {
        return status;
    }

    public List<String> printConstraints() {
        if (status.equals(CnecSecurityStatus.FAILURE)) {
            return List.of(physicalParameter + " monitoring failed due to a load flow divergence or an inconsistency in the crac.");
        }
        List<String> constraints = new ArrayList<>();
        cnecResults.stream()
            .filter(CnecResult::thresholdOvershoot)
            .sorted(Comparator.comparing(CnecResult::getId))
            .forEach(angleCnecResult -> constraints.add(angleCnecResult.print()));

        if (constraints.isEmpty()) {
            return List.of(String.format("All %s Cnecs are secure.", physicalParameter));
        } else {
            constraints.add(0, String.format("Some %s Cnecs are not secure:", physicalParameter));
        }
        return constraints;
    }

    public void combine(MonitoringResult monitoringResult) {
        Set<CnecResult> thisCnecResults = new HashSet<>(this.getCnecResults());
        Set<CnecResult> otherCnecResults = monitoringResult.getCnecResults();
        thisCnecResults.addAll(otherCnecResults);
        this.cnecResults = thisCnecResults;

        Map<State, Set<RemedialAction>> thisAppliedRas = new HashMap<>(this.getAppliedRas());
        Map<State, Set<RemedialAction>> otherAppliedRas = monitoringResult.getAppliedRas();
        thisAppliedRas.putAll(otherAppliedRas);
        this.appliedRas = thisAppliedRas;

        this.status = combineStatuses(this.status, monitoringResult.getStatus());
    }

    public static CnecSecurityStatus combineStatuses(CnecSecurityStatus... status) {
        boolean atLeastOneFailed = Arrays.asList(status).contains(CnecSecurityStatus.FAILURE);
        if (atLeastOneFailed) {
            return CnecSecurityStatus.FAILURE;
        }

        boolean atLeastOneHigh = Arrays.asList(status).contains(CnecSecurityStatus.HIGH_CONSTRAINT);
        boolean atLeastOneLow = Arrays.asList(status).contains(CnecSecurityStatus.LOW_CONSTRAINT);
        boolean atLeastOneHighAndLow = Arrays.asList(status).contains(CnecSecurityStatus.HIGH_AND_LOW_CONSTRAINTS) || atLeastOneHigh && atLeastOneLow;

        if (atLeastOneHighAndLow) {
            return CnecSecurityStatus.HIGH_AND_LOW_CONSTRAINTS;
        }
        if (atLeastOneHigh) {
            return CnecSecurityStatus.HIGH_CONSTRAINT;
        }
        if (atLeastOneLow) {
            return CnecSecurityStatus.LOW_CONSTRAINT;
        }
        return CnecSecurityStatus.SECURE;
    }
}
