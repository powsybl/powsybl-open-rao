/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.*;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;

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
    private final BasecaseScenario basecaseScenario;
    private final Set<ContingencyScenario> contingencyScenarios = new HashSet<>();

    public StateTree(Crac crac) {
        basecaseScenario = new BasecaseScenario(crac.getPreventiveState(), null);

        for (Contingency contingency : crac.getContingencies()) {
            processOutageInstant(contingency, crac);
            processAutoAndCurativeInstants(contingency, crac);
        }

        Set<List<State>> optimizedCurativeStates = contingencyScenarios.stream().map(ContingencyScenario::getCurativeStates).collect(Collectors.toSet());
        this.operatorsNotSharingCras = findOperatorsNotSharingCras(crac, optimizedCurativeStates);
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
                basecaseScenario.addOtherState(outageState);
            }
        }
    }

    /**
     * Process AUTO and CURATIVE states for a given contingency.
     * If the state has RAs in AUTO but not in CURATIVE, the case is not supported by Open RAO.
     * If the state has AUTO and CURATIVE RAs, both states will be treated in a dedicated scenario.
     * If the AUTO has no RA but the CURATIVE has RAs, the AUTO will be optimized in basecase RAO and the CURATIVE in a dedicated scenario.
     * If neither AUTO nor CURATIVE states have RAs, they will be optimized in basecase RAO.
     * <p>
     * If AUTO or CURATIVE state does not exist, it will not be optimized.
     */
    private void processAutoAndCurativeInstants(Contingency contingency, Crac crac) {
        State automatonState = crac.hasAutoInstant() ? crac.getState(contingency.getId(), crac.getInstant(InstantKind.AUTO)) : null;
        List<State> curativeStates = crac.getCurativeInstants().stream().map(instant -> crac.getState(contingency.getId(), instant)).toList().stream().filter(Objects::nonNull).toList();
        boolean autoRasExist = automatonState != null && anyAvailableRemedialAction(crac, automatonState);
        boolean curativeRasExist = !curativeStates.stream().filter(curativeState -> anyAvailableRemedialAction(crac, curativeState)).toList().isEmpty();

        if (autoRasExist && !curativeRasExist) {
            throw new OpenRaoException(String.format("Automaton state %s has RAs, but none of the curative states for contingency '%s' do. This is not supported.", automatonState, contingency.getId()));
        } else if (autoRasExist) {
            contingencyScenarios.add(new ContingencyScenario(automatonState, curativeStates));
        } else if (curativeRasExist) {
            if (automatonState != null) {
                basecaseScenario.addOtherState(automatonState);
            }
            contingencyScenarios.add(new ContingencyScenario(curativeStates));
        } else {
            if (automatonState != null) {
                basecaseScenario.addOtherState(automatonState);
            }
            if (!curativeStates.isEmpty()) {
                curativeStates.forEach(basecaseScenario::addOtherState);
            }
        }
    }

    public BasecaseScenario getBasecaseScenario() {
        return basecaseScenario;
    }

    public Set<ContingencyScenario> getContingencyScenarios() {
        return contingencyScenarios;
    }

    public Set<String> getOperatorsNotSharingCras() {
        return operatorsNotSharingCras;
    }

    private static boolean anyAvailableRemedialAction(Crac crac, State state) {
        return !crac.getPotentiallyAvailableNetworkActions(state).isEmpty() ||
                !crac.getPotentiallyAvailableRangeActions(state).isEmpty();
    }

    static Set<String> findOperatorsNotSharingCras(Crac crac, Set<List<State>> optimizedCurativeStates) {
        Set<String> tsos = crac.getFlowCnecs().stream().map(Cnec::getOperator).collect(Collectors.toSet());
        tsos.addAll(crac.getRemedialActions().stream().map(RemedialAction::getOperator).collect(Collectors.toSet()));
        // <!> If a CNEC's operator is null, filter it out of the list of operators not sharing CRAs
        return tsos.stream().filter(tso -> !Objects.isNull(tso) && !tsoHasCra(tso, crac, optimizedCurativeStates)).collect(Collectors.toSet());
    }

    static boolean tsoHasCra(String tso, Crac crac, Set<List<State>> optimizedCurativeStates) {
        return optimizedCurativeStates.stream().anyMatch(states ->
                states.stream().anyMatch(state ->
                        crac.getPotentiallyAvailableNetworkActions(state).stream().map(RemedialAction::getOperator).anyMatch(tso::equals) ||
                                crac.getPotentiallyAvailableRangeActions(state).stream().map(RemedialAction::getOperator).anyMatch(tso::equals))
        );
    }
}
