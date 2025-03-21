/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
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

        this.operatorsNotSharingCras = findOperatorsNotSharingCras(crac);
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
     * <p>
     * If AUTO or CURATIVE state does not exist, it will not be optimized.
     */
    private void processAutoAndCurativeInstants(Contingency contingency, Crac crac) {
        ContingencyScenario.ContingencyScenarioBuilder contingencyScenarioBuilder = ContingencyScenario.create().withContingency(contingency);
        Pair<Boolean, Boolean> autoInstantHasCnecsAndRemedialActions = processAutoInstant(contingency, crac, contingencyScenarioBuilder);
        Perimeter defaultPerimeter = getDefaultPerimeter(contingency, crac, autoInstantHasCnecsAndRemedialActions.getRight());
        boolean scenarioHasCurativeStates = false;
        if (defaultPerimeter != null) {
            scenarioHasCurativeStates = processCurativeInstants(contingency, crac, contingencyScenarioBuilder, defaultPerimeter, autoInstantHasCnecsAndRemedialActions.getLeft());
        }
        if (Boolean.TRUE.equals(autoInstantHasCnecsAndRemedialActions.getLeft()) && Boolean.TRUE.equals(autoInstantHasCnecsAndRemedialActions.getRight()) || scenarioHasCurativeStates) {
            contingencyScenarios.add(contingencyScenarioBuilder.build());
        }
    }

    /**
     * Returns the default perimeter to which all curative CNECs that have no associated CRAs must be added
     * @param contingency: the scenario's contingency
     * @param crac: the input CRAC
     * @param automatonRemedialActionsExist: whether auto remedial actions were added to the CRAC or not
     * @return
     * <ul>
     *     <li>preventivePerimeter if no ARAs exist</li>
     *     <li>a perimeter with an optimisation state corresponding to the first curative instant</li>
     *     <li>null if no curative instant is defined</li>
     * </ul>
     */
    private Perimeter getDefaultPerimeter(Contingency contingency, Crac crac, boolean automatonRemedialActionsExist) {
        if (!automatonRemedialActionsExist) {
            return preventivePerimeter;
        }
        return crac.getStates(contingency)
            .stream()
            .filter(state -> state.getInstant().isCurative())
            .filter(state -> anyCnec(crac, state))
            .sorted()
            .findFirst()
            .map(state -> new Perimeter(state, new HashSet<>()))
            .orElse(null);
    }

    private Pair<Boolean, Boolean> processAutoInstant(Contingency contingency, Crac crac, ContingencyScenario.ContingencyScenarioBuilder contingencyScenarioBuilder) {
        State automatonState = crac.hasAutoInstant() ? crac.getState(contingency.getId(), crac.getInstant(InstantKind.AUTO)) : null;
        Pair<Boolean, Boolean> autoInstantHasCnecsAndRemedialActions = stateHasCnecsAndRemedialActions(crac, automatonState);
        boolean autoCnecsExist = autoInstantHasCnecsAndRemedialActions.getLeft();
        boolean autoRemedialActionsExist = autoInstantHasCnecsAndRemedialActions.getRight();
        if (autoCnecsExist && !autoRemedialActionsExist) {
            // the auto CNECs must be added to the preventive perimeter because no ARAs can affect them
            preventivePerimeter.addOtherState(automatonState);
        } else if (autoRemedialActionsExist) {
            contingencyScenarioBuilder.withAutomatonState(automatonState);
        }
        return autoInstantHasCnecsAndRemedialActions;
    }

    private Pair<Boolean, Boolean> stateHasCnecsAndRemedialActions(Crac crac, State state) {
        return state != null ? Pair.of(anyCnec(crac, state), anyAvailableRemedialAction(crac, state)) : Pair.of(false, false);
    }

    /**
     * Process the CURATIVE instants.
     * <p>
     * For each curative instant with CNECs, the nearest previous curative instant with CRAs is used as the optimisation
     * instant for these CNECs. If no such instant exists, the CNECs are added to the default perimeter.
     * <p>
     * If an instant has CRAs but not CNECs and occurs before instants with CNECs, the CRAs can affect them so a
     * curative perimeter must be built for this instant as well.
     * <p>
     * The method returns whether curative perimeters were added to the contingency scenario or not.
     **/
    private boolean processCurativeInstants(Contingency contingency, Crac crac, ContingencyScenario.ContingencyScenarioBuilder contingencyScenarioBuilder, Perimeter defaultPerimeter, boolean automatonCnecsExist) {
        Set<Instant> instantsWithCnecs = crac.getInstants(InstantKind.CURATIVE).stream().filter(instant -> anyCnec(crac, crac.getState(contingency, instant))).collect(Collectors.toSet());
        if (!automatonCnecsExist && instantsWithCnecs.isEmpty()) {
            //OpenRaoLoggerProvider.BUSINESS_WARNS.warn("Contingency {} has an automaton or a curative remedial action but no CNECs associated.", contingency.getId());
            return false;
        }

        // retrieve the nearest curative instant with CRAs for each curative instant with CNECs
        Map<Instant, Instant> associatedOptimizationInstant = new HashMap<>();
        instantsWithCnecs.forEach(instant -> associatedOptimizationInstant.put(instant, getLastCurativeInstantWithCraBeforeGivenInstant(contingency, crac, instant).orElse(null)));

        // create a perimeter for each instant with CRAs associated to an instant with CNECs
        Map<Instant, Perimeter> curativePerimeters = new HashMap<>();
        associatedOptimizationInstant.values().stream().distinct().filter(Objects::nonNull)
            .forEach(instant -> curativePerimeters.put(instant, new Perimeter(crac.getState(contingency, instant), new HashSet<>())));

        // add the CNECs of the curative instants to the different perimeters:
        // - if the associated instant is null, the CNECs are added to the default perimeter
        // - otherwise, they are added to the perimeter corresponding to their associated optimization instant
        instantsWithCnecs.forEach(cnecInstant -> curativePerimeters.getOrDefault(associatedOptimizationInstant.get(cnecInstant), defaultPerimeter).addOtherState(crac.getState(contingency, cnecInstant)));

        // add the curative perimeters to the contingency scenario builder
        if (!defaultPerimeter.equals(preventivePerimeter) && associatedOptimizationInstant.containsValue(null)) {
            // add the default perimeter to the curative perimeters if it is curative and contains CNECs of several instants
            curativePerimeters.put(defaultPerimeter.getRaOptimisationState().getInstant(), defaultPerimeter);
        }
        curativePerimeters.values().forEach(contingencyScenarioBuilder::withCurativePerimeter);

        return !curativePerimeters.isEmpty();
    }

    /**
     * Get the nearest previous curative instant with CRAs for a given curative instant.
     * @param contingency: the contingency of the scenario
     * @param crac: the CRAC data
     * @param instant: the curative instant
     * @return nearest previous curative instant with CRAs (Optional.empty() is none)
     */
    private Optional<Instant> getLastCurativeInstantWithCraBeforeGivenInstant(Contingency contingency, Crac crac, Instant instant) {
        return crac.getInstants(InstantKind.CURATIVE)
            .stream()
            .filter(otherInstant -> !otherInstant.comesAfter(instant))
            .filter(otherInstant -> Objects.nonNull(crac.getState(contingency, otherInstant)))
            .filter(otherInstant -> anyAvailableRemedialAction(crac, crac.getState(contingency, otherInstant)))
            .max(Instant::compareTo);
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
        return !crac.getFlowCnecs(state).isEmpty();
    }

    private static boolean anyAvailableRemedialAction(Crac crac, State state) {
        return !crac.getPotentiallyAvailableNetworkActions(state).isEmpty() ||
            !crac.getPotentiallyAvailableRangeActions(state).isEmpty();
    }

    static Set<String> findOperatorsNotSharingCras(Crac crac) {
        Set<String> tsos = crac.getFlowCnecs().stream().map(Cnec::getOperator).collect(Collectors.toSet());
        tsos.addAll(crac.getRemedialActions().stream().map(RemedialAction::getOperator).collect(Collectors.toSet()));
        // <!> If a CNEC's operator is not null, filter it out of the list of operators not sharing CRAs
        return tsos.stream().filter(tso -> Objects.nonNull(tso) && !tsoHasCra(tso, crac)).collect(Collectors.toSet());
    }

    static boolean tsoHasCra(String tso, Crac crac) {
        Set<State> optimizedCurativeStates = crac.getCurativeStates();
        return optimizedCurativeStates.stream().anyMatch(state ->
            crac.getPotentiallyAvailableNetworkActions(state).stream().map(RemedialAction::getOperator).anyMatch(tso::equals) ||
                crac.getPotentiallyAvailableRangeActions(state).stream().map(RemedialAction::getOperator).anyMatch(tso::equals)
        );
    }
}
