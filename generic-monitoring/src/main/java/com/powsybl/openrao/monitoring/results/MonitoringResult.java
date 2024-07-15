package com.powsybl.openrao.monitoring.results;

import com.powsybl.openrao.data.cracapi.RemedialAction;
import com.powsybl.openrao.data.cracapi.State;

import java.util.*;

public class MonitoringResult {
    private final Set<? extends CnecResult> cnecResults;
    private final Map<State, Set<RemedialAction>> appliedRas;
    private final Status status;

    public MonitoringResult(Set<? extends CnecResult> cnecResults, Map<State, Set<RemedialAction>> appliedRas, Status status) {
        this.cnecResults = cnecResults;
        this.appliedRas = appliedRas;
        this.status = status;
    }

    public Set<? extends CnecResult> getCnecResults() {
        return cnecResults;
    }

    public Map<State, Set<RemedialAction>> getAppliedRas() {
        return appliedRas;
    }

    public Status getStatus() {
        return status;
    }

    public enum Status {

        SECURE,
        HIGH_CONSTRAINT,
        LOW_CONSTRAINT,
        HIGH_AND_LOW_CONSTRAINTS,
        FAILURE;
    }


}
