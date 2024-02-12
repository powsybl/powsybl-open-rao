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
import org.apache.commons.lang3.tuple.Pair;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
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

    private void processAutoAndCurativeInstants(Contingency contingency, Crac crac) {
        ContingencyScenario.ContingencyScenarioBuilder contingencyScenarioBuilder = ContingencyScenario.create().withContingency(contingency);
        State automatonState = processAutoInstant(contingency, crac, contingencyScenarioBuilder);
        processCurativeInstants(contingency, crac, contingencyScenarioBuilder, automatonState);
    }

    private State processAutoInstant(Contingency contingency, Crac crac, ContingencyScenario.ContingencyScenarioBuilder contingencyScenarioBuilder) {
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
        return automatonState;
    }

    private Pair<Boolean, Boolean> stateHasCnecsAndRemedialActions(Crac crac, State state) {
        return Objects.nonNull(state) ? Pair.of(anyCnec(crac, state), anyAvailableRemedialAction(crac, state)) : Pair.of(false, false);
    }

    private void processCurativeInstants(Contingency contingency, Crac crac, ContingencyScenario.ContingencyScenarioBuilder contingencyScenarioBuilder, State automatonState) {
        Pair<SortedSet<Instant>, SortedSet<Instant>> classifiedIntants = classifyCurativeInstantsOnCnecsAndRemedialActionExistence(contingency, crac);
        SortedSet<Instant> instantsWithCnecs = classifiedIntants.getLeft();
        SortedSet<Instant> instantsWithRemedialActions = classifiedIntants.getRight();

        Pair<Boolean, Boolean> autoInstantHasCnecsAndRemedialActions = stateHasCnecsAndRemedialActions(crac, automatonState);
        boolean autoCnecsExist = autoInstantHasCnecsAndRemedialActions.getLeft();
        boolean autoRemedialActionsExist = autoInstantHasCnecsAndRemedialActions.getRight();

        if (!autoCnecsExist && instantsWithCnecs.isEmpty()) {
            OpenRaoLoggerProvider.BUSINESS_WARNS.warn("Contingency {} has an automaton or a curative remedial action but no CNECs associated.", contingency.getId());
            return;
        }

        boolean scenarioHasCurativeStates;
        Instant firstCurativeInstantWithRemedialActions = instantsWithRemedialActions.isEmpty() ? null : instantsWithRemedialActions.stream().toList().get(0);
        Instant lastCurativeInstantWithRemedialActions = instantsWithRemedialActions.isEmpty() ? null : instantsWithRemedialActions.stream().toList().get(instantsWithRemedialActions.size() - 1);

        // get all the states the instants of which occur before the first curative instant with CRAs:
        // - if ARAs are defined in the CRAC, create a curative perimeter using the first of these states (chronologically)
        // - otherwise, add them all to the preventive perimeter
        scenarioHasCurativeStates = addStatesToPreventiveOrFirstCurativePerimeter(contingency, crac, contingencyScenarioBuilder, instantsWithCnecs, autoRemedialActionsExist, firstCurativeInstantWithRemedialActions);

        if (!instantsWithCnecs.isEmpty()) {
            scenarioHasCurativeStates = addCurativePerimeters(contingency, crac, contingencyScenarioBuilder, instantsWithCnecs, instantsWithRemedialActions, firstCurativeInstantWithRemedialActions, lastCurativeInstantWithRemedialActions) || scenarioHasCurativeStates;
        }

        // Only create
        if (autoRemedialActionsExist || scenarioHasCurativeStates) {
            contingencyScenarios.add(contingencyScenarioBuilder.build());
        }
    }

    private Pair<SortedSet<Instant>, SortedSet<Instant>> classifyCurativeInstantsOnCnecsAndRemedialActionExistence(Contingency contingency, Crac crac) {
        SortedSet<Instant> instantsWithCnecs = new TreeSet<>();
        SortedSet<Instant> instantsWithRemedialActions = new TreeSet<>();
        for (Instant curativeInstant : crac.getInstants(InstantKind.CURATIVE)) {
            State curativeState = crac.getState(contingency, curativeInstant);
            Pair<Boolean, Boolean> curativeStateHasCnecsAndRemedialActions = stateHasCnecsAndRemedialActions(crac, curativeState);
            if (Boolean.TRUE.equals(curativeStateHasCnecsAndRemedialActions.getLeft())) {
                instantsWithCnecs.add(curativeInstant);
            }
            if (Boolean.TRUE.equals(curativeStateHasCnecsAndRemedialActions.getRight())) {
                instantsWithRemedialActions.add(curativeInstant);
            }
        }
        return Pair.of(instantsWithCnecs, instantsWithRemedialActions);
    }

    private boolean addCurativePerimeters(Contingency contingency, Crac crac, ContingencyScenario.ContingencyScenarioBuilder contingencyScenarioBuilder, SortedSet<Instant> instantsWithCnecs, SortedSet<Instant> instantsWithRemedialActions, Instant firstCurativeInstantWithRemedialActions, Instant lastCurativeInstantWithRemedialActions) {
        Instant previousInstantWithRemedialActions = null;
        boolean scenarioHasCurativeStates = false;
        for (Instant currentInstantWithRemedialActions : instantsWithRemedialActions) {
            if (!currentInstantWithRemedialActions.equals(firstCurativeInstantWithRemedialActions)) {
                // create a curative perimeter using the previous instant with remedial actions as the optimization instant
                // all the instants between the previous and the current instants with CNECs but no CRAs are added to the perimeter
                addCurativePerimeter(contingency, crac, contingencyScenarioBuilder, instantsWithCnecs, previousInstantWithRemedialActions, currentInstantWithRemedialActions);
                scenarioHasCurativeStates = true;
            }
            previousInstantWithRemedialActions = currentInstantWithRemedialActions;
            if (currentInstantWithRemedialActions.equals(lastCurativeInstantWithRemedialActions)) {
                // gather all the remaining states in a final curative perimeter
                scenarioHasCurativeStates = addLastCurativePerimeter(contingency, crac, contingencyScenarioBuilder, instantsWithCnecs, scenarioHasCurativeStates, currentInstantWithRemedialActions) || scenarioHasCurativeStates;
            }
        }
        return scenarioHasCurativeStates;
    }

    private boolean addStatesToPreventiveOrFirstCurativePerimeter(Contingency contingency, Crac crac, ContingencyScenario.ContingencyScenarioBuilder contingencyScenarioBuilder, SortedSet<Instant> instantsWithCnecs, boolean autoRemedialActionsExist, Instant firstCurativeInstantWithRemedialActions) {
        Set<State> statesToAddToPreventiveOrFirstCurativePerimeter = getAllStatesBetweenTwoCurativeInstants(instantsWithCnecs, null, firstCurativeInstantWithRemedialActions, crac, contingency);
        if (autoRemedialActionsExist && !statesToAddToPreventiveOrFirstCurativePerimeter.isEmpty()) {
            State firstCurativeState = statesToAddToPreventiveOrFirstCurativePerimeter.
                stream()
                .sorted(Comparator.comparingInt(state -> state.getInstant().getOrder()))
                .toList()
                .get(0);
            statesToAddToPreventiveOrFirstCurativePerimeter.remove(firstCurativeState);
            contingencyScenarioBuilder.withCurativePerimeter(new Perimeter(firstCurativeState, statesToAddToPreventiveOrFirstCurativePerimeter));
            return true;
        } else {
            statesToAddToPreventiveOrFirstCurativePerimeter.forEach(preventivePerimeter::addOtherState);
            return false;
        }
    }

    private void addCurativePerimeter(Contingency contingency, Crac crac, ContingencyScenario.ContingencyScenarioBuilder contingencyScenarioBuilder, SortedSet<Instant> instantsWithCnecs, Instant previousInstantWithRemedialActions, Instant currentInstantWithRemedialActions) {
        contingencyScenarioBuilder.withCurativePerimeter(new Perimeter(crac.getState(contingency, previousInstantWithRemedialActions), getAllStatesBetweenTwoCurativeInstants(instantsWithCnecs, previousInstantWithRemedialActions, currentInstantWithRemedialActions, crac, contingency)));
    }

    private boolean addLastCurativePerimeter(Contingency contingency, Crac crac, ContingencyScenario.ContingencyScenarioBuilder contingencyScenarioBuilder, SortedSet<Instant> instantsWithCnecs, boolean scenarioHasCurativeStates, Instant currentInstantWithRemedialActions) {
        Set<State> lastCurativeCnecStates = getAllStatesBetweenTwoCurativeInstants(instantsWithCnecs, currentInstantWithRemedialActions, null, crac, contingency);
        if (instantsWithCnecs.contains(currentInstantWithRemedialActions) || !lastCurativeCnecStates.isEmpty()) {
            contingencyScenarioBuilder.withCurativePerimeter(new Perimeter(crac.getState(contingency, currentInstantWithRemedialActions), lastCurativeCnecStates));
            return true;
        }
        return false;
    }

    Set<State> getAllStatesBetweenTwoCurativeInstants(SortedSet<Instant> instantsSet, Instant afterInstant, Instant beforeInstant, Crac crac, Contingency contingency) {
        return instantsSet.stream()
            .filter(instant -> Objects.isNull(afterInstant) || instant.comesAfter(afterInstant))
            .filter(instant -> Objects.isNull(beforeInstant) || instant.comesBefore(beforeInstant))
            .map(instant -> crac.getState(contingency, instant))
            .collect(Collectors.toSet());
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
    private void processAutoAndCurativeInstants2(Contingency contingency, Crac crac) {
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
                OpenRaoLoggerProvider.BUSINESS_WARNS.warn("Contingency {} has an automaton or a curative remedial action but no CNECs associated.", contingency.getId());
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
        boolean curativeCnecsEncountered = false;

        // if curative state has CNECs but no CRAs, add it to the last optimisation state before it with CRAs
        for (State curativeState : curativeStates) {
            boolean curativeCnecsPresent = anyCnec(crac, curativeState);
            curativeCnecsEncountered = curativeCnecsEncountered || curativeCnecsPresent;
            if (anyAvailableRemedialAction(crac, curativeState) && curativeCnecsEncountered) {
                contingencyScenarioBuilder.withCurativePerimeter(new Perimeter(curativeState, new HashSet<>(curativeStatesWithCnecsButNoCras)));
                curativeStatesWithCnecsButNoCras.clear();
                curativeCnecsEncountered = false;
                contingencyScenarioUsed = true;
            } else if (curativeCnecsPresent) {
                curativeStatesWithCnecsButNoCras.add(curativeState);
            }
        }

        // run automaton perimeter if auto ARAs exist and auto or curative CNECs exist
        if (autoRasExist) {
            contingencyScenarioBuilder.withAutomatonState(automatonState);
            contingencyScenarioUsed = true;
        }

        // add curative CNECs to preventive if no ARAs and no CRAs affect them
        if (!autoRasExist && !curativeStatesWithCnecsButNoCras.isEmpty()) {
            curativeStatesWithCnecsButNoCras.forEach(preventivePerimeter::addOtherState);
            curativeStatesWithCnecsButNoCras.clear();
        }

        // run curative perimeter if curative CNECs exist and either CRA exist or auto state was added to the scenario
        if (!curativeStatesWithCnecsButNoCras.isEmpty()) {
            State firstCurativeState = curativeStates.get(curativeStates.size() - 1);
            curativeStatesWithCnecsButNoCras.remove(firstCurativeState);
            contingencyScenarioBuilder.withCurativePerimeter(new Perimeter(firstCurativeState, new HashSet<>(curativeStatesWithCnecsButNoCras)));
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
