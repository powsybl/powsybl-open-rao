/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api;

import com.powsybl.action.Action;
import com.powsybl.action.GeneratorActionBuilder;
import com.powsybl.action.PhaseTapChangerTapPositionActionBuilder;
import com.powsybl.action.SwitchActionBuilder;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.ActionType;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.networkaction.SwitchPair;
import com.powsybl.openrao.data.crac.api.usagerule.OnContingencyStateAdderToRemedialAction;
import com.powsybl.openrao.data.crac.api.usagerule.UsageRule;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class NetworkActionUtils {

    private NetworkActionUtils() {
    }

    public static class SwitchPairImplTest implements SwitchPair {

        private final String switchToOpen;
        private final String switchToClose;

        public SwitchPairImplTest(String switchToOpen, String switchToClose) {
            this.switchToOpen = switchToOpen;
            this.switchToClose = switchToClose;
        }

        @Override
        public String getSwitchToOpenId() {
            return switchToOpen;
        }

        @Override
        public String getSwitchToCloseId() {
            return switchToClose;
        }

        @Override
        public String getType() {
            return null;
        }

        @Override
        public String getId() {
            return null;
        }
    }

    public static class NetworkActionImplTest implements NetworkAction {

        private final Set<Action> elementaryActions;

        public NetworkActionImplTest(Set<Action> elementaryActions) {
            this.elementaryActions = new HashSet<>(elementaryActions);
        }

        public NetworkActionImplTest(Action elementaryAction) {
            this.elementaryActions = Collections.singleton(elementaryAction);
        }

        @Override
        public String getId() {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public String getOperator() {
            return null;
        }

        @Override
        public Set<UsageRule> getUsageRules() {
            return null;
        }

        @Override
        public Optional<Integer> getSpeed() {
            return Optional.empty();
        }

        @Override
        public Set<FlowCnec> getFlowCnecsConstrainingUsageRules(Set<FlowCnec> perimeterCnecs, Network network, State optimizedState) {
            return null;
        }

        @Override
        public Set<FlowCnec> getFlowCnecsConstrainingForOneUsageRule(UsageRule usageRule, Set<FlowCnec> perimeterCnecs, Network network) {
            return null;
        }

        @Override
        public Set<String> getNetworkElementsIds() {
            return null;
        }

        @Override
        public OnContingencyStateAdderToRemedialAction<NetworkAction> newOnStateUsageRule() {
            return null;
        }

        @Override
        public Optional<Double> getActivationCost() {
            return Optional.empty();
        }

        @Override
        public boolean hasImpactOnNetwork(Network network) {
            return false;
        }

        @Override
        public boolean apply(Network network) {
            return false;
        }

        @Override
        public Set<Action> getElementaryActions() {
            return elementaryActions;
        }

        @Override
        public boolean canBeApplied(Network network) {
            return false;
        }

        @Override
        public void addUsageRule(UsageRule usageRule) {
            // not used
        }

        @Override
        public <E extends Extension<NetworkAction>> void addExtension(Class<? super E> aClass, E e) {
            //not used
        }

        @Override
        public <E extends Extension<NetworkAction>> E getExtension(Class<? super E> aClass) {
            return null;
        }

        @Override
        public <E extends Extension<NetworkAction>> E getExtensionByName(String s) {
            return null;
        }

        @Override
        public <E extends Extension<NetworkAction>> boolean removeExtension(Class<E> aClass) {
            return false;
        }

        @Override
        public <E extends Extension<NetworkAction>> Collection<E> getExtensions() {
            return null;
        }

    }

    public static NetworkAction createSwitchAction(String networkElementId, ActionType actionType) {
        return new NetworkActionImplTest(new SwitchActionBuilder().withId("id").withNetworkElementId(networkElementId).withOpen(actionType == ActionType.OPEN).build());
    }

    public static NetworkAction createSwitchPair(String switchToOpenId, String switchToCloseId) {
        return new NetworkActionImplTest(new SwitchPairImplTest(switchToOpenId, switchToCloseId));
    }

    public static NetworkAction createPhaseTapChangerTapPositionAction(String pstId, int setpoint) {
        return new NetworkActionImplTest(new PhaseTapChangerTapPositionActionBuilder().withId("id").withNetworkElementId(pstId).withTapPosition(setpoint).withRelativeValue(false).build());
    }

    public static NetworkAction createGeneratorActivePowerAction(String networkElementId, double setpoint) {
        return new NetworkActionImplTest(new GeneratorActionBuilder().withId("id").withGeneratorId(networkElementId).withActivePowerValue(setpoint).withActivePowerRelativeValue(false).build());
    }

    public static NetworkAction createGeneratorTargetVAction(String networkElementId, double setpoint) {
        return new NetworkActionImplTest(new GeneratorActionBuilder().withId("id").withGeneratorId(networkElementId).withTargetV(setpoint).build());
    }
}
