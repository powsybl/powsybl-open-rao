/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.RemedialAction;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.triggercondition.TriggerCondition;
import com.powsybl.openrao.data.cracapi.usagerule.*;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Business object of a group of elementary remedial actions (range or network action).
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public abstract class AbstractRemedialAction<I extends RemedialAction<I>> extends AbstractIdentifiable<I> implements RemedialAction<I> {
    protected String operator;
    protected Set<UsageRule> usageRules;
    protected Set<TriggerCondition> triggerConditions;
    protected Integer speed;
    private boolean computedUsageMethods = false;
    private Map<State, UsageMethod> usageMethodPerState;
    private Map<Instant, UsageMethod> usageMethodPerInstant;

    protected AbstractRemedialAction(String id, String name, String operator, Set<UsageRule> usageRules, Integer speed, Set<TriggerCondition> triggerConditions) {
        super(id, name);
        this.operator = operator;
        this.usageRules = usageRules;
        this.triggerConditions = triggerConditions;
        this.speed = speed;
    }

    @Override
    public String getOperator() {
        return operator;
    }

    @Override
    public final Set<UsageRule> getUsageRules() {
        return usageRules;
    }

    @Override
    public Set<TriggerCondition> getTriggerConditions() {
        return triggerConditions;
    }

    @Override
    public Optional<Integer> getSpeed() {
        return Optional.ofNullable(speed);
    }

    @Override
    public UsageMethod getUsageMethod(State state) {
        if (!computedUsageMethods) {
            computeUsageMethodPerStateAndInstant();
            computedUsageMethods = true;
        }
        if (usageMethodPerState.getOrDefault(state, UsageMethod.UNDEFINED).equals(usageMethodPerInstant.getOrDefault(state.getInstant(), UsageMethod.UNDEFINED))) {
            return usageMethodPerInstant.getOrDefault(state.getInstant(), UsageMethod.UNDEFINED);
        }
        return UsageMethod.getStrongestUsageMethod(Set.of(
            usageMethodPerState.getOrDefault(state, UsageMethod.UNDEFINED),
            usageMethodPerInstant.getOrDefault(state.getInstant(), UsageMethod.UNDEFINED)));
    }

    private void computeUsageMethodPerStateAndInstant() {
        usageMethodPerState = new HashMap<>();
        usageMethodPerInstant = new HashMap<>();

        for (UsageRule usageRule : usageRules) {
            if (usageRule.getInstant().isPreventive()) {
                updateMapWithValue(usageMethodPerInstant, usageRule.getInstant(), usageRule.getUsageMethod());
            } else if (usageRule instanceof OnConstraint<?> oc) {
                State state = oc.getCnec().getState();
                if (usageRule.getInstant().equals(state.getInstant())) {
                    updateMapWithValue(usageMethodPerState, state, usageRule.getUsageMethod());
                }
            } else if (usageRule instanceof OnContingencyState ocs) {
                State state = ocs.getState();
                updateMapWithValue(usageMethodPerState, state, usageRule.getUsageMethod());
            } else if (usageRule instanceof OnFlowConstraintInCountry || usageRule instanceof OnInstant) {
                updateMapWithValue(usageMethodPerInstant, usageRule.getInstant(), usageRule.getUsageMethod());
            } else {
                throw new OpenRaoException(String.format("Usage rule of type %s is not implemented yet.", usageRule.getClass().getName()));
            }
        }
    }

    private void updateMapWithValue(Map<Instant, UsageMethod> map, Instant key, UsageMethod value) {
        if (!map.containsKey(key)) {
            map.put(key, value);
        } else if (!value.equals(map.get(key))) {
            map.put(key, UsageMethod.getStrongestUsageMethod(Set.of(map.get(key), value)));
        }
    }

    private void updateMapWithValue(Map<State, UsageMethod> map, State key, UsageMethod value) {
        if (!map.containsKey(key)) {
            map.put(key, value);
        } else if (!value.equals(map.get(key))) {
            map.put(key, UsageMethod.getStrongestUsageMethod(Set.of(map.get(key), value)));
        }
    }

    /**
     * Retrieves cnecs associated to the remedial action's OnFlowConstraint and OnFlowConstraintInCountry usage rules.
     */
    // TODO: move this method to RaoUtil
    public Set<FlowCnec> getFlowCnecsConstrainingUsageRules(Set<FlowCnec> perimeterCnecs, Network network, State optimizedState) {
        Set<FlowCnec> toBeConsideredCnecs = new HashSet<>();
        Set<UsageRule> usageRulesOnFlowConstraint = new HashSet<>();
        usageRulesOnFlowConstraint.addAll(getUsageRules(OnConstraint.class, optimizedState).stream().filter(onConstraint -> onConstraint.getCnec() instanceof FlowCnec).toList());
        usageRulesOnFlowConstraint.addAll(getUsageRules(OnFlowConstraintInCountry.class, optimizedState));
        usageRulesOnFlowConstraint.forEach(usageRule -> toBeConsideredCnecs.addAll(getFlowCnecsConstrainingForOneUsageRule(usageRule, perimeterCnecs, network)));
        return toBeConsideredCnecs;
    }

    // TODO: move this method to RaoUtil
    public Set<FlowCnec> getFlowCnecsConstrainingForOneUsageRule(UsageRule usageRule, Set<FlowCnec> perimeterCnecs, Network network) {
        if (usageRule instanceof OnConstraint<?> onConstraint && onConstraint.getCnec() instanceof FlowCnec flowCnec) {
            return Set.of(flowCnec);
        } else if (usageRule instanceof OnFlowConstraintInCountry onFlowConstraintInCountry) {
            return perimeterCnecs.stream()
                .filter(cnec -> !cnec.getState().getInstant().comesBefore(usageRule.getInstant()))
                .filter(cnec -> onFlowConstraintInCountry.getContingency().isEmpty() || onFlowConstraintInCountry.getContingency().equals(cnec.getState().getContingency()))
                .filter(cnec -> isCnecInCountry(cnec, onFlowConstraintInCountry.getCountry(), network)).collect(Collectors.toSet());
        } else {
            throw new OpenRaoException(String.format("This method should only be used for Ofc Usage rules not for this type of UsageRule: %s", usageRule.getClass().getName()));
        }
    }

    private <T extends UsageRule> List<T> getUsageRules(Class<T> usageRuleClass, State state) {
        return getUsageRules().stream().filter(usageRuleClass::isInstance).map(usageRuleClass::cast)
            .filter(ofc -> state.getInstant().isAuto() ?
                ofc.getUsageMethod(state).equals(UsageMethod.FORCED) :
                ofc.getUsageMethod(state).equals(UsageMethod.AVAILABLE) || ofc.getUsageMethod(state).equals(UsageMethod.FORCED))
            .toList();
    }

    private static boolean isCnecInCountry(Cnec<?> cnec, Country country, Network network) {
        return cnec.getLocation(network).stream()
            .filter(Optional::isPresent)
            .map(Optional::get)
            .anyMatch(cnecCountry -> cnecCountry.equals(country));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractRemedialAction<?> remedialAction = (AbstractRemedialAction<?>) o;
        return super.equals(remedialAction)
            && new HashSet<>(usageRules).equals(new HashSet<>(remedialAction.getUsageRules()))
            && (operator != null && operator.equals(remedialAction.operator) || operator == null && remedialAction.operator == null);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
