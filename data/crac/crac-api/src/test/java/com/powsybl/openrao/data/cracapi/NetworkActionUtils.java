/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracapi;

import com.powsybl.commons.extensions.Extension;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.networkaction.ElementaryAction;
import com.powsybl.openrao.data.cracapi.networkaction.InjectionSetpoint;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.networkaction.PstSetpoint;
import com.powsybl.openrao.data.cracapi.networkaction.SwitchPair;
import com.powsybl.openrao.data.cracapi.networkaction.TopologicalAction;
import com.powsybl.openrao.data.cracapi.usagerule.OnContingencyStateAdderToRemedialAction;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.cracapi.usagerule.UsageRule;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class NetworkActionUtils {

    private NetworkActionUtils() {
    }

    public static class TopologicalActionImplTest implements TopologicalAction {

        private final NetworkElement networkElement;
        private final ActionType actionType;

        public TopologicalActionImplTest(NetworkElement networkElement, ActionType actionType) {
            this.networkElement = networkElement;
            this.actionType = actionType;
        }

        @Override
        public boolean hasImpactOnNetwork(Network network) {
            return false;
        }

        @Override
        public boolean canBeApplied(Network network) {
            return false;
        }

        @Override
        public void apply(Network network) {

        }

        @Override
        public Set<NetworkElement> getNetworkElements() {
            return null;
        }

        @Override
        public ActionType getActionType() {
            return actionType;
        }

        @Override
        public NetworkElement getNetworkElement() {
            return networkElement;
        }
    }

    public static class InjectionSetpointImplTest implements InjectionSetpoint {

        private final NetworkElement networkElement;
        private final double setpoint;
        private final Unit unit;

        public InjectionSetpointImplTest(NetworkElement networkElement, double setpoint, Unit unit) {
            this.networkElement = networkElement;
            this.setpoint = setpoint;
            this.unit = unit;
        }

        @Override
        public boolean hasImpactOnNetwork(Network network) {
            return false;
        }

        @Override
        public boolean canBeApplied(Network network) {
            return false;
        }

        @Override
        public void apply(Network network) {

        }

        @Override
        public Set<NetworkElement> getNetworkElements() {
            return null;
        }

        @Override
        public double getSetpoint() {
            return setpoint;
        }

        @Override
        public NetworkElement getNetworkElement() {
            return networkElement;
        }

        @Override
        public Unit getUnit() {
            return unit;
        }
    }

    public static class SwitchPairImplTest implements SwitchPair {

        private final NetworkElement switchToOpen;
        private final NetworkElement switchToClose;

        public SwitchPairImplTest(NetworkElement switchToOpen, NetworkElement switchToClose) {
            this.switchToOpen = switchToOpen;
            this.switchToClose = switchToClose;
        }

        @Override
        public boolean hasImpactOnNetwork(Network network) {
            return false;
        }

        @Override
        public boolean canBeApplied(Network network) {
            return false;
        }

        @Override
        public void apply(Network network) {

        }

        @Override
        public Set<NetworkElement> getNetworkElements() {
            return null;
        }

        @Override
        public NetworkElement getSwitchToOpen() {
            return switchToOpen;
        }

        @Override
        public NetworkElement getSwitchToClose() {
            return switchToClose;
        }
    }

    public static class PstSetpointImplTest implements PstSetpoint {

        private final NetworkElement networkElement;
        private final int setpoint;

        public PstSetpointImplTest(NetworkElement networkElement, int setpoint) {
            this.networkElement = networkElement;
            this.setpoint = setpoint;
        }

        @Override
        public boolean hasImpactOnNetwork(Network network) {
            return false;
        }

        @Override
        public boolean canBeApplied(Network network) {
            return false;
        }

        @Override
        public void apply(Network network) {

        }

        @Override
        public Set<NetworkElement> getNetworkElements() {
            return null;
        }

        @Override
        public int getSetpoint() {
            return setpoint;
        }

        @Override
        public NetworkElement getNetworkElement() {
            return networkElement;
        }
    }

    public static class NetworkActionImplTest implements NetworkAction {

        private final Set<ElementaryAction> elementaryActions;

        public NetworkActionImplTest(Set<ElementaryAction> elementaryActions) {
            this.elementaryActions = new HashSet<>(elementaryActions);
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
        public UsageMethod getUsageMethod(State state) {
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
        public Set<NetworkElement> getNetworkElements() {
            return null;
        }

        @Override
        public OnContingencyStateAdderToRemedialAction<NetworkAction> newOnStateUsageRule() {
            return null;
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
        public Set<ElementaryAction> getElementaryActions() {
            return elementaryActions;
        }

        @Override
        public <E extends Extension<NetworkAction>> void addExtension(Class<? super E> aClass, E e) {

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

    public static class NetworkElementImplTest implements NetworkElement {

        private final String id;

        public NetworkElementImplTest(String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public Set<Optional<Country>> getLocation(Network network) {
            return null;
        }

        @Override
        public <E extends Extension<NetworkElement>> void addExtension(Class<? super E> aClass, E e) {

        }

        @Override
        public <E extends Extension<NetworkElement>> E getExtension(Class<? super E> aClass) {
            return null;
        }

        @Override
        public <E extends Extension<NetworkElement>> E getExtensionByName(String s) {
            return null;
        }

        @Override
        public <E extends Extension<NetworkElement>> boolean removeExtension(Class<E> aClass) {
            return false;
        }

        @Override
        public <E extends Extension<NetworkElement>> Collection<E> getExtensions() {
            return null;
        }

        @Override
        public boolean equals(Object object) {
            if (object instanceof NetworkElement networkElement) {
                return getId().equals(networkElement.getId());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }

    public static NetworkElement createNetworkElement(String networkElementId) {
        return new NetworkElementImplTest(networkElementId);
    }

    public static TopologicalAction createTopologyAction(NetworkElement networkElement, ActionType actionType) {
        return new TopologicalActionImplTest(networkElement, actionType);
    }

    public static SwitchPair createSwitchPair(NetworkElement switchToOpen, NetworkElement switchToClose) {
        return new SwitchPairImplTest(switchToOpen, switchToClose);
    }

    public static PstSetpoint createPstSetpoint(NetworkElement pst, int setpoint) {
        return new PstSetpointImplTest(pst, setpoint);
    }

    public static InjectionSetpoint createInjectionSetpoint(NetworkElement networkElement, double setpoint, Unit unit) {
        return new InjectionSetpointImplTest(networkElement, setpoint, unit);
    }
}
