/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.castor.algorithm;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class StateTree {

    private Set<String> operatorsNotSharingCras;
    private BasecaseScenario basecaseScenario;
    private Set<ContingencyScenario> contingencyScenarios = new HashSet<>();

    public StateTree(Crac crac) {
        basecaseScenario = new BasecaseScenario(crac.getPreventiveState(), null);

        for (Contingency contingency : crac.getContingencies()) {
            processOutageInstant(contingency, crac);
            processAutoAndCurativeInstants(contingency, crac);
        }

        Set<State> optimizedCurativeStates = contingencyScenarios.stream().map(ContingencyScenario::getCurativeState).collect(Collectors.toSet());
        this.operatorsNotSharingCras = findOperatorsNotSharingCras(crac, optimizedCurativeStates);
    }

    /**
     * Process OUTAGE state for a given contingency.
     * If the state has RAs, the case is not supported by FARAO.
     * Else, the state is optimized in basecase RAO.
     */
    private void processOutageInstant(Contingency contingency, Crac crac) {
        State outageState = crac.getState(contingency.getId(), Instant.OUTAGE);
        if (outageState != null) {
            if (anyAvailableRemedialAction(crac, outageState)) {
                throw new FaraoException(String.format("Outage state %s has available RAs. This is not supported.", outageState));
            } else {
                basecaseScenario.addOtherState(outageState);
            }
        }
    }

    /**
     * Process AUTO and CURATIVE states for a given contingency.
     * If the state has RAs in AUTO but not in CURATIVE, the case is not supported by FARAO.
     * If the state has AUTO and CURATIVE RAs, both states will be treated in a dedicated scenario.
     * If the AUTO has no RA but the CURATIVE has RAs, the AUTO will be optimized in basecase RAO and the CURATIVE in a dedicated scenario.
     * If neither AUTO nor CURATIVE states have RAs, they will be optimized in basecase RAO.
     *
     * If AUTO or CURATIVE state does not exist, it will not be optimized.
     */
    private void processAutoAndCurativeInstants(Contingency contingency, Crac crac) {
        State automatonState = crac.getState(contingency.getId(), Instant.AUTO);
        State curativeState = crac.getState(contingency.getId(), Instant.CURATIVE);
        boolean autoRasExist = (automatonState != null) && anyAvailableRemedialAction(crac, automatonState);
        boolean curativeRasExist = (curativeState != null) && anyAvailableRemedialAction(crac, curativeState);

        if (autoRasExist && !curativeRasExist) {
            throw new FaraoException(String.format("Automaton state %s has RAs, but curative state %s doesn't. This is not supported.", automatonState, curativeState));
        } else if (autoRasExist) {
            contingencyScenarios.add(new ContingencyScenario(automatonState, curativeState));
        } else if (curativeRasExist) {
            if (automatonState != null) {
                basecaseScenario.addOtherState(automatonState);
            }
            contingencyScenarios.add(new ContingencyScenario(null, curativeState));
        } else {
            if (automatonState != null) {
                basecaseScenario.addOtherState(automatonState);
            }
            if (curativeState != null) {
                basecaseScenario.addOtherState(curativeState);
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
        return !crac.getNetworkActions(state, UsageMethod.AVAILABLE, UsageMethod.TO_BE_EVALUATED, UsageMethod.FORCED).isEmpty() ||
                !crac.getRangeActions(state, UsageMethod.AVAILABLE, UsageMethod.TO_BE_EVALUATED, UsageMethod.FORCED).isEmpty();
    }

    static Set<String> findOperatorsNotSharingCras(Crac crac, Set<State> optimizedCurativeStates) {
        Set<String> tsos = crac.getFlowCnecs().stream().map(Cnec::getOperator).collect(Collectors.toSet());
        tsos.addAll(crac.getRemedialActions().stream().map(RemedialAction::getOperator).collect(Collectors.toSet()));
        // <!> If a CNEC's operator is null, filter it out of the list of operators not sharing CRAs
        return tsos.stream().filter(tso -> !Objects.isNull(tso) && !tsoHasCra(tso, crac, optimizedCurativeStates)).collect(Collectors.toSet());
    }

    static boolean tsoHasCra(String tso, Crac crac, Set<State> optimizedCurativeStates) {
        return optimizedCurativeStates.stream().anyMatch(state ->
           crac.getNetworkActions(state, UsageMethod.AVAILABLE, UsageMethod.TO_BE_EVALUATED, UsageMethod.FORCED).stream().map(RemedialAction::getOperator).anyMatch(raTso -> raTso.equals(tso)) ||
                crac.getRangeActions(state, UsageMethod.AVAILABLE, UsageMethod.TO_BE_EVALUATED, UsageMethod.FORCED).stream().map(RemedialAction::getOperator).anyMatch(raTso -> raTso.equals(tso))
        );
    }
}
