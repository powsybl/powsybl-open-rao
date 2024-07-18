package com.powsybl.openrao.monitoring.results;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.data.cracapi.RemedialAction;
import com.powsybl.openrao.data.cracapi.State;

import java.util.*;
import java.util.stream.Collectors;

import static com.powsybl.openrao.monitoring.results.MonitoringResult.Status.*;

public class MonitoringResult {

    private PhysicalParameter physicalParameter;
    private Set<? extends CnecResult> cnecResults;
    private Map<State, Set<RemedialAction>> appliedRas;
    private Status status;

    public MonitoringResult(PhysicalParameter physicalParameter, Set<? extends CnecResult> cnecResults, Map<State, Set<RemedialAction>> appliedRas, Status status) {
        this.physicalParameter = physicalParameter;
        this.cnecResults = cnecResults;
        this.appliedRas = appliedRas;
        this.status = status;
    }

    public PhysicalParameter getPhysicalParameter() {
        return physicalParameter;
    }

    public Set<? extends CnecResult> getCnecResults() {
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

    public Status getStatus() {
        return status;
    }

    public List<String> printConstraints() {
        if (status.equals(FAILURE)) {
            return List.of(physicalParameter + " monitoring failed due to a load flow divergence or an inconsistency in the crac.");
        }
        List<String> constraints = new ArrayList<>();
        if (physicalParameter.equals(PhysicalParameter.ANGLE)) {
            cnecResults.stream().filter(AngleCnecResult.class::isInstance)
                .map(AngleCnecResult.class::cast)
                .filter(AngleCnecResult::thresholdOvershoot)
                .sorted(Comparator.comparing(AngleCnecResult::getId))
                .forEach(angleCnecResult -> constraints.add(angleCnecResult.print()));
        } else {
            cnecResults.stream().filter(VoltageCnecResult.class::isInstance)
                .map(VoltageCnecResult.class::cast)
                .filter(VoltageCnecResult::thresholdOvershoot)
                .sorted(Comparator.comparing(VoltageCnecResult::getId))
                .forEach(voltageCnecResult -> constraints.add(voltageCnecResult.print()));
        }
        if (constraints.isEmpty()) {
            return List.of(String.format("All %s Cnecs are secure.", physicalParameter));
        } else {
            constraints.add(0, String.format("Some %s Cnecs are not secure:", physicalParameter));
        }
        return constraints;
    }


    public enum Status {
        SECURE,
        HIGH_CONSTRAINT,
        LOW_CONSTRAINT,
        HIGH_AND_LOW_CONSTRAINTS,
        FAILURE,
    }

    public void combine(MonitoringResult monitoringResult) {
        Set<CnecResult> thisCnecResults = new HashSet<>(this.getCnecResults());
        Set<? extends CnecResult> otherCnecResults = monitoringResult.getCnecResults();

        if (monitoringResult.getPhysicalParameter().equals(PhysicalParameter.ANGLE)) {
            thisCnecResults.addAll(otherCnecResults.stream()
                .filter(AngleCnecResult.class::isInstance)
                .map(AngleCnecResult.class::cast)
                .collect(Collectors.toSet()));
        } else {
            thisCnecResults.addAll(otherCnecResults.stream()
                .filter(VoltageCnecResult.class::isInstance)
                .map(VoltageCnecResult.class::cast)
                .collect(Collectors.toSet()));
        }
        this.cnecResults = thisCnecResults;

        Map<State, Set<RemedialAction>> thisAppliedRas = new HashMap<>(this.getAppliedRas());
        Map<State, Set<RemedialAction>> otherAppliedRas = monitoringResult.getAppliedRas();
        thisAppliedRas.putAll(otherAppliedRas);
        this.appliedRas = thisAppliedRas;

        this.status = combineStatuses(this.status, monitoringResult.getStatus());
    }

    public static Status combineStatuses(Status... status) {
        boolean atLeastOneFailed = Arrays.asList(status).contains(FAILURE);
        if (atLeastOneFailed) {
            return FAILURE;
        }

        boolean atLeastOneHigh = Arrays.asList(status).contains(HIGH_CONSTRAINT);
        boolean atLeastOneLow = Arrays.asList(status).contains(LOW_CONSTRAINT);
        boolean atLeastOneHighAndLow = Arrays.asList(status).contains(HIGH_AND_LOW_CONSTRAINTS) || atLeastOneHigh && atLeastOneLow;

        if (atLeastOneHighAndLow) {
            return HIGH_AND_LOW_CONSTRAINTS;
        }
        if (atLeastOneHigh) {
            return HIGH_CONSTRAINT;
        }
        if (atLeastOneLow) {
            return LOW_CONSTRAINT;
        }
        return SECURE;
    }
}
