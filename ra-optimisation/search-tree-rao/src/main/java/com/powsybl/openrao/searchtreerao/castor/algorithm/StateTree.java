/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.cracapi.*;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class StateTree {

    private final Set<String> operatorsNotSharingCras;
    private final Perimeter preventivePerimeter;
    private final Set<ContingencyScenario> contingencyScenarios = new HashSet<>();

    public StateTree(Crac crac) {
        preventivePerimeter = new Perimeter(crac.getPreventiveState(), null);

        for (Contingency contingency : crac.getContingencies()) {
            processOutageInstant(contingency, crac);
            processAutoAndCurativeInstants(contingency, crac);
        }

        this.operatorsNotSharingCras = findOperatorsNotSharingCras(crac, crac.getCurativeStates());
    }

    /**
     * Process OUTAGE state for a given contingency.
     * If the state has RAs, the case is not supported by Open RAO.
     * Else, the state is optimized in basecase RAO.
     */
    private void processOutageInstant(Contingency contingency, Crac crac) {
        State outageState = crac.getState(contingency.getId(), crac.getOutageInstant());
        if (outageState != null) {
            if (anyAvailableRemedialAction(crac, outageState)) {
                throw new OpenRaoException(String.format("Outage state %s has available RAs. This is not supported.", outageState));
            } else {
                preventivePerimeter.addOtherState(outageState);
            }
        }
    }

    /**
     * Process AUTO and CURATIVE states for a given contingency.
     * If the state has RAs in AUTO but not in CURATIVE, the case is not supported by Open RAO.
     * If the state has AUTO and CURATIVE RAs, both states will be treated in a dedicated scenario.
     * If the AUTO has no RA but the CURATIVE has RAs, the AUTO will be optimized in basecase RAO and the CURATIVE in a dedicated scenario.
     * If neither AUTO nor CURATIVE states have RAs, they will be optimized in basecase RAO.
     *
     * If AUTO or CURATIVE state does not exist, it will not be optimized.
     */
    private void processAutoAndCurativeInstants(Contingency contingency, Crac crac) {
        State automatonState = crac.hasAutoInstant() ? crac.getState(contingency.getId(), crac.getInstant(InstantKind.AUTO)) : null;
        List<State> curativeStates = crac.getStates(contingency)
            .stream()
            .filter(state -> state.getInstant().isCurative())
            // Invert order for more efficient processing
            .sorted(Comparator.comparingInt(state -> -state.getInstant().getOrder()))
            .toList();
        boolean autoRasExist = automatonState != null && anyAvailableRemedialAction(crac, automatonState);
        boolean autoCnecsExist = automatonState != null && anyCnec(crac, automatonState);
        boolean curativeCnecsExist = !curativeStates.isEmpty() && curativeStates.stream().anyMatch(curativeState -> anyCnec(crac, curativeState));

        if (!autoCnecsExist && !curativeCnecsExist) {
            // do not create scenarios with no CNECs even if RAs exist
            if (Objects.nonNull(automatonState) || !curativeStates.isEmpty()) {
                OpenRaoLoggerProvider.BUSINESS_WARNS.warn("Contingency {} has an automaton or a curative state but no CNECs associated.", contingency.getId());
            }
            return;
        }
        // add automaton CNECs to preventive if no ARAs affect them
        if (autoCnecsExist && !autoRasExist) {
            preventivePerimeter.addOtherState(automatonState);
        }

        ContingencyScenario.ContingencyScenarioBuilder contingencyScenarioBuilder = ContingencyScenario.create().withContingency(contingency);
        boolean contingencyScenarioUsed = false;
        Set<State> curativeStatesWithCnecsButNoCras = new HashSet<>();

        // if curative state has CNECs but no CRAs, add it to the closest optimisation state with CRAs
        for (State curativeState : curativeStates) {
            if (anyAvailableRemedialAction(crac, curativeState)) {
                if (anyCnec(crac, curativeState) || !curativeStatesWithCnecsButNoCras.isEmpty()) {
                    contingencyScenarioBuilder.withCurativePerimeter(new Perimeter(curativeState, curativeStatesWithCnecsButNoCras));
                    curativeStatesWithCnecsButNoCras.clear();
                    contingencyScenarioUsed = true;
                }
            } else if (anyCnec(crac, curativeState)) {
                curativeStatesWithCnecsButNoCras.add(curativeState);
            }
        }

        // add curative CNECs to preventive if no ARAs and no CRAs affect them
        if (!(autoCnecsExist && autoRasExist) && !curativeStatesWithCnecsButNoCras.isEmpty()) {
            curativeStatesWithCnecsButNoCras.forEach(preventivePerimeter::addOtherState);
            curativeStatesWithCnecsButNoCras.clear();
        }

        // run automaton perimeter if auto RAs and CNECs exist
        if (autoCnecsExist && autoRasExist) {
            contingencyScenarioBuilder.withAutomatonState(automatonState);
            contingencyScenarioUsed = true;
        }

        // run curative perimeter if curative CNECs exist and either CRA exist or auto state was added to the scenario
        if (!curativeStatesWithCnecsButNoCras.isEmpty()) {
            State firstCurativeState = curativeStates.get(curativeStates.size() - 1);
            curativeStatesWithCnecsButNoCras.remove(firstCurativeState);
            contingencyScenarioBuilder.withCurativePerimeter(new Perimeter(firstCurativeState, curativeStatesWithCnecsButNoCras));
            // no need to set contingencyScenarioUsed to true because it was already set to true for the automaton perimeter
        }

        // if no auto and no curative perimeter, do not add scenario
        if (contingencyScenarioUsed) {
            contingencyScenarios.add(contingencyScenarioBuilder.build());
        }
    }

    public Perimeter getBasecaseScenario() {
        return preventivePerimeter;
    }

    public Set<ContingencyScenario> getContingencyScenarios() {
        return contingencyScenarios;
    }

    public Set<String> getOperatorsNotSharingCras() {
        return operatorsNotSharingCras;
    }

    private boolean anyCnec(Crac crac, State state) {
        return !crac.getCnecs(state).isEmpty();
    }

    private static boolean anyAvailableRemedialAction(Crac crac, State state) {
        return !crac.getPotentiallyAvailableNetworkActions(state).isEmpty() ||
                !crac.getPotentiallyAvailableRangeActions(state).isEmpty();
    }

    static Set<String> findOperatorsNotSharingCras(Crac crac, Set<State> optimizedCurativeStates) {
        Set<String> tsos = crac.getFlowCnecs().stream().map(Cnec::getOperator).collect(Collectors.toSet());
        tsos.addAll(crac.getRemedialActions().stream().map(RemedialAction::getOperator).collect(Collectors.toSet()));
        // <!> If a CNEC's operator is not null, filter it out of the list of operators not sharing CRAs
        return tsos.stream().filter(tso -> Objects.nonNull(tso) && !tsoHasCra(tso, crac, optimizedCurativeStates)).collect(Collectors.toSet());
    }

    static boolean tsoHasCra(String tso, Crac crac, Set<State> optimizedCurativeStates) {
        return optimizedCurativeStates.stream().anyMatch(state ->
           crac.getPotentiallyAvailableNetworkActions(state).stream().map(RemedialAction::getOperator).anyMatch(tso::equals) ||
                crac.getPotentiallyAvailableRangeActions(state).stream().map(RemedialAction::getOperator).anyMatch(tso::equals)
        );
    }
}
