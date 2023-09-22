/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.usage_rule.*;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Business object of a group of elementary remedial actions (range or network action).
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public abstract class AbstractRemedialAction<I extends RemedialAction<I>> extends AbstractIdentifiable<I> implements RemedialAction<I> {
    protected String operator;
    protected List<UsageRule> usageRules;
    protected Integer speed = null;

    protected AbstractRemedialAction(String id, String name, String operator, List<UsageRule> usageRules, Integer speed) {
        super(id, name);
        this.operator = operator;
        this.usageRules = usageRules;
        this.speed = speed;
    }

    protected AbstractRemedialAction(String id, String name, String operator, List<UsageRule> usageRules) {
        super(id, name);
        this.operator = operator;
        this.usageRules = usageRules;
    }

    void addUsageRule(UsageRule usageRule) {
        this.usageRules.add(usageRule);
    }

    @Override
    public OnContingencyStateAdderToRemedialAction<I> newOnStateUsageRule() {
        return new OnStateAdderToRemedialActionImpl(this);
    }

    @Override
    public String getOperator() {
        return operator;
    }

    @Override
    public final List<UsageRule> getUsageRules() {
        return usageRules;
    }

    @Override
    public Optional<Integer> getSpeed() {
        return Optional.ofNullable(speed);
    }

    @Override
    public UsageMethod getUsageMethod(State state) {
        Set<UsageMethod> usageMethods = usageRules.stream()
            .map(usageRule -> usageRule.getUsageMethod(state))
            .collect(Collectors.toSet());

        if (usageMethods.contains(UsageMethod.UNAVAILABLE)) {
            return UsageMethod.UNAVAILABLE;
        } else if (usageMethods.contains(UsageMethod.AVAILABLE)) {
            return UsageMethod.AVAILABLE;
        } else if (usageMethods.contains(UsageMethod.TO_BE_EVALUATED)) {
            return UsageMethod.TO_BE_EVALUATED;
        } else if (usageMethods.contains(UsageMethod.FORCED)) {
            return UsageMethod.FORCED;
        } else {
            return UsageMethod.UNAVAILABLE;
        }
    }

    /**
     * Evaluates if the remedial action is available depending on its UsageMethod.
     * If TO_BE_EVALUATED condition has not been evaluated, default behavior is false
     */
    @Override
    public boolean isRemedialActionAvailable(State state) {
        return isRemedialActionAvailable(state, false);
    }

    /**
     * Evaluates if the remedial action is available depending on its UsageMethod.
     * When UsageMethod is TO_BE_EVALUATED, condition has to have been evaluated previously
     */
    @Override
    public boolean isRemedialActionAvailable(State state, boolean evaluatedCondition) {
        switch (getUsageMethod(state)) {
            case AVAILABLE:
                return true;
            case TO_BE_EVALUATED:
                return evaluatedCondition;
            default:
                return false;
        }
    }

    /**
     * Retrieves cnecs associated to the remedial action's OnFlowConstraint and OnFlowConstraintInCountry usage rules.
     */
    public Set<FlowCnec> getFlowCnecsConstrainingUsageRules(Set<FlowCnec> perimeterCnecs, Network network, State optimizedState) {
        Set<FlowCnec> toBeConsideredCnecs = new HashSet<>();
        // OnFlowConstraint
        List<OnFlowConstraint> onFlowConstraintUsageRules = getUsageRules().stream().filter(OnFlowConstraint.class::isInstance).map(OnFlowConstraint.class::cast)
                .filter(ofc -> ofc.getUsageMethod(optimizedState).equals(UsageMethod.TO_BE_EVALUATED)).collect(Collectors.toList());
        onFlowConstraintUsageRules.forEach(onFlowConstraint -> toBeConsideredCnecs.add(onFlowConstraint.getFlowCnec()));

        // OnFlowConstraintInCountry
        List<OnFlowConstraintInCountry> onFlowConstraintInCountryUsageRules = getUsageRules().stream().filter(OnFlowConstraintInCountry.class::isInstance).map(OnFlowConstraintInCountry.class::cast)
                .filter(ofc -> ofc.getUsageMethod(optimizedState).equals(UsageMethod.TO_BE_EVALUATED)).collect(Collectors.toList());
        onFlowConstraintInCountryUsageRules.forEach(onFlowConstraintInCountry -> {
            // TODO : is this change OK?
            toBeConsideredCnecs.addAll(perimeterCnecs.stream()
                    .filter(cnec -> onFlowConstraintInCountry.getInstant().isPreventive() || !cnec.getState().getInstant().comesBefore(onFlowConstraintInCountry.getInstant()))
                    .filter(cnec -> isCnecInCountry(cnec, onFlowConstraintInCountry.getCountry(), network))
                    .collect(Collectors.toSet()));
        });
        return toBeConsideredCnecs;
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
                && ((operator != null && operator.equals(remedialAction.operator)) || (operator == null && remedialAction.operator == null));
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
