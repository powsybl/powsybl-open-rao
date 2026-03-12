/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.usagerule.OnConstraint;
import com.powsybl.openrao.data.crac.api.usagerule.OnContingencyStateAdderToRemedialAction;
import com.powsybl.openrao.data.crac.api.usagerule.OnFlowConstraintInCountry;
import com.powsybl.openrao.data.crac.api.usagerule.UsageRule;

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
    protected Set<UsageRule> usageRules;
    protected Integer speed;
    protected Double activationCost;

    protected AbstractRemedialAction(String id, String name, String operator, Set<UsageRule> usageRules, Integer speed, Double activationCost) {
        super(id, name);
        this.operator = operator;
        this.usageRules = usageRules;
        this.speed = speed;
        this.activationCost = activationCost;
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
    public Optional<Double> getActivationCost() {
        return Optional.ofNullable(activationCost);
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
        return getUsageRules().stream()
            .filter(usageRuleClass::isInstance)
            .map(usageRuleClass::cast)
            .filter(usageRule -> usageRule.isDefinedForState(state))
            .toList();
    }

    private static boolean isCnecInCountry(Cnec<?> cnec, Country country, Network network) {
        return cnec.getLocation(network).stream().anyMatch(cnecCountry -> cnecCountry.equals(country));
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
