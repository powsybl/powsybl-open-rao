/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.usage_rule.*;
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
    protected Integer speed;

    protected AbstractRemedialAction(String id, String name, String operator, Set<UsageRule> usageRules, Integer speed) {
        super(id, name);
        this.operator = operator;
        this.usageRules = usageRules;
        this.speed = speed;
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
    public final Set<UsageRule> getUsageRules() {
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
        return UsageMethod.getStrongestUsageMethod(usageMethods);
    }

    /**
     * Retrieves cnecs associated to the remedial action's OnFlowConstraint and OnFlowConstraintInCountry usage rules.
     */

    public Set<FlowCnec> getFlowCnecsConstrainingUsageRules(Set<FlowCnec> perimeterCnecs, Network network, State optimizedState) {
        Set<FlowCnec> toBeConsideredCnecs = new HashSet<>();
        Set<UsageRule> usageRulesOnFlowConstraint = new HashSet<>();
        usageRulesOnFlowConstraint.addAll(getUsageRules(OnFlowConstraint.class, optimizedState));
        usageRulesOnFlowConstraint.addAll(getUsageRules(OnFlowConstraintInCountry.class, optimizedState));
        usageRulesOnFlowConstraint.forEach(usageRule -> toBeConsideredCnecs.addAll(getFlowCnecsConstrainingForOneUsageRule(usageRule, perimeterCnecs, network)));
        return toBeConsideredCnecs;
    }

    public Set<FlowCnec> getFlowCnecsConstrainingForOneUsageRule(UsageRule usageRule, Set<FlowCnec> perimeterCnecs, Network network) {
        if (usageRule instanceof OnFlowConstraint) {
            return Set.of(((OnFlowConstraint) usageRule).getFlowCnec());
        } else if (usageRule instanceof OnFlowConstraintInCountry) {
            Map<Instant, Set<Instant>> allowedCnecInstantPerRaInstant = Map.of(
                Instant.PREVENTIVE, Set.of(Instant.PREVENTIVE, Instant.OUTAGE, Instant.CURATIVE),
                Instant.AUTO, Set.of(Instant.AUTO),
                Instant.CURATIVE, Set.of(Instant.CURATIVE)
            );
            return perimeterCnecs.stream()
                .filter(cnec -> allowedCnecInstantPerRaInstant.get(usageRule.getInstant()).contains(cnec.getState().getInstant()))
                .filter(cnec -> isCnecInCountry(cnec, ((OnFlowConstraintInCountry) usageRule).getCountry(), network)).collect(Collectors.toSet());
        } else {
            throw new FaraoException(String.format("This method should only be used for Ofc Usage rules not for this type of UsageRule: %s", usageRule.getClass().getName()));
        }
    }

    private <T extends UsageRule> List<T> getUsageRules(Class<T> usageRuleClass, State state) {
        return getUsageRules().stream().filter(usageRuleClass::isInstance).map(usageRuleClass::cast)
            .filter(ofc -> state.getInstant().equals(Instant.AUTO) ?
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
                && ((operator != null && operator.equals(remedialAction.operator)) || (operator == null && remedialAction.operator == null));
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
