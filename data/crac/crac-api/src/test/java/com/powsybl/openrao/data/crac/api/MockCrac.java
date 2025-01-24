/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api;

import com.powsybl.commons.extensions.Extension;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.data.crac.api.rangeaction.CounterTradeRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.CounterTradeRangeActionAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeActionAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeActionAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeActionAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnecAdder;
import com.powsybl.openrao.data.crac.api.cnec.BranchCnec;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnecAdder;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnecAdder;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkActionAdder;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class MockCrac implements Crac {
    private final String id;
    private boolean wasExportSuccessful;

    public MockCrac(String id) {
        this.id = id;
        this.wasExportSuccessful = false;
    }

    public Optional<OffsetDateTime> getTimestamp() {
        return Optional.empty();
    }

    public void setExportSuccessful() {
        wasExportSuccessful = true;
    }

    public boolean wasExportSuccessful() {
        return wasExportSuccessful;
    }

    @Override
    public ContingencyAdder newContingency() {
        return null;
    }

    @Override
    public Set<Contingency> getContingencies() {
        return null;
    }

    @Override
    public Contingency getContingency(String id) {
        return null;
    }

    @Override
    public void removeContingency(String id) {
        //not used
    }

    @Override
    public Crac newInstant(String instantId, InstantKind instantKind) {
        return null;
    }

    @Override
    public Instant getInstant(String instantId) {
        return null;
    }

    @Override
    public List<Instant> getSortedInstants() {
        return null;
    }

    @Override
    public Instant getInstant(InstantKind instantKind) {
        return null;
    }

    @Override
    public SortedSet<Instant> getInstants(InstantKind instantKind) {
        return null;
    }

    @Override
    public Instant getInstantBefore(Instant providedInstant) {
        return null;
    }

    @Override
    public Instant getPreventiveInstant() {
        return null;
    }

    @Override
    public Instant getOutageInstant() {
        return null;
    }

    @Override
    public Instant getLastInstant() {
        return null;
    }

    @Override
    public boolean hasAutoInstant() {
        return false;
    }

    @Override
    public Set<State> getStates() {
        return null;
    }

    @Override
    public State getPreventiveState() {
        return null;
    }

    @Override
    public Set<State> getCurativeStates() {
        return null;
    }

    @Override
    public SortedSet<State> getStates(Contingency contingency) {
        return null;
    }

    @Override
    public Set<State> getStates(Instant instant) {
        return null;
    }

    @Override
    public State getState(Contingency contingency, Instant instant) {
        return null;
    }

    @Override
    public FlowCnecAdder newFlowCnec() {
        return null;
    }

    @Override
    public AngleCnecAdder newAngleCnec() {
        return null;
    }

    @Override
    public VoltageCnecAdder newVoltageCnec() {
        return null;
    }

    @Override
    public Set<Cnec> getCnecs() {
        return null;
    }

    @Override
    public Set<Cnec> getCnecs(State state) {
        return null;
    }

    @Override
    public Set<Cnec> getCnecs(PhysicalParameter physicalParameter) {
        return null;
    }

    @Override
    public Set<Cnec> getCnecs(PhysicalParameter physicalParameter, State state) {
        return null;
    }

    @Override
    public Cnec getCnec(String cnecId) {
        return null;
    }

    @Override
    public Set<BranchCnec> getBranchCnecs() {
        return null;
    }

    @Override
    public Set<BranchCnec> getBranchCnecs(State state) {
        return null;
    }

    @Override
    public BranchCnec getBranchCnec(String branchCnecId) {
        return null;
    }

    @Override
    public Set<FlowCnec> getFlowCnecs() {
        return null;
    }

    @Override
    public Set<FlowCnec> getFlowCnecs(State state) {
        return null;
    }

    @Override
    public FlowCnec getFlowCnec(String flowCnecId) {
        return null;
    }

    @Override
    public Set<AngleCnec> getAngleCnecs() {
        return null;
    }

    @Override
    public Set<AngleCnec> getAngleCnecs(State state) {
        return null;
    }

    @Override
    public AngleCnec getAngleCnec(String angleCnecId) {
        return null;
    }

    @Override
    public Set<VoltageCnec> getVoltageCnecs() {
        return null;
    }

    @Override
    public Set<VoltageCnec> getVoltageCnecs(State state) {
        return null;
    }

    @Override
    public VoltageCnec getVoltageCnec(String voltageCnecId) {
        return null;
    }

    @Override
    public void removeCnec(String cnecId) {
        //not used
    }

    @Override
    public void removeFlowCnec(String flowCnecId) {
        //not used
    }

    @Override
    public void removeFlowCnecs(Set<String> flowCnecsIds) {
        //not used
    }

    @Override
    public void removeAngleCnec(String angleCnecId) {
        //not used
    }

    @Override
    public void removeAngleCnecs(Set<String> angleCnecsIds) {
        //not used
    }

    @Override
    public void removeVoltageCnec(String voltageCnecId) {
        //not used
    }

    @Override
    public void removeVoltageCnecs(Set<String> voltageCnecsIds) {
        //not used
    }

    @Override
    public Set<RemedialAction<?>> getRemedialActions() {
        return null;
    }

    @Override
    public RemedialAction<?> getRemedialAction(String remedialActionId) {
        return null;
    }

    @Override
    public void removeRemedialAction(String id) {
        //not used
    }

    @Override
    public PstRangeActionAdder newPstRangeAction() {
        return null;
    }

    @Override
    public HvdcRangeActionAdder newHvdcRangeAction() {
        return null;
    }

    @Override
    public InjectionRangeActionAdder newInjectionRangeAction() {
        return null;
    }

    @Override
    public CounterTradeRangeActionAdder newCounterTradeRangeAction() {
        return null;
    }

    @Override
    public Set<RangeAction<?>> getRangeActions() {
        return null;
    }

    @Override
    public Set<RangeAction<?>> getRangeActions(State state, UsageMethod... usageMethod) {
        return null;
    }

    @Override
    public Set<RangeAction<?>> getPotentiallyAvailableRangeActions(State state) {
        return null;
    }

    @Override
    public RangeAction<?> getRangeAction(String id) {
        return null;
    }

    @Override
    public Set<PstRangeAction> getPstRangeActions() {
        return null;
    }

    @Override
    public Set<HvdcRangeAction> getHvdcRangeActions() {
        return null;
    }

    @Override
    public Set<InjectionRangeAction> getInjectionRangeActions() {
        return null;
    }

    @Override
    public Set<CounterTradeRangeAction> getCounterTradeRangeActions() {
        return null;
    }

    @Override
    public PstRangeAction getPstRangeAction(String pstRangeActionId) {
        return null;
    }

    @Override
    public HvdcRangeAction getHvdcRangeAction(String hvdcRangeActionId) {
        return null;
    }

    @Override
    public InjectionRangeAction getInjectionRangeAction(String injectionRangeActionId) {
        return null;
    }

    @Override
    public CounterTradeRangeAction getCounterTradeRangeAction(String counterTradeRangeActionId) {
        return null;
    }

    @Override
    public void removePstRangeAction(String id) {
        //not used
    }

    @Override
    public void removeHvdcRangeAction(String id) {
        //not used
    }

    @Override
    public void removeInjectionRangeAction(String id) {
        //not used
    }

    @Override
    public NetworkActionAdder newNetworkAction() {
        return null;
    }

    @Override
    public Set<NetworkAction> getNetworkActions() {
        return null;
    }

    @Override
    public Set<NetworkAction> getNetworkActions(State state, UsageMethod... usageMethod) {
        return null;
    }

    @Override
    public Set<NetworkAction> getPotentiallyAvailableNetworkActions(State state) {
        return null;
    }

    @Override
    public NetworkAction getNetworkAction(String id) {
        return null;
    }

    @Override
    public void removeNetworkAction(String id) {
        //not used
    }

    @Override
    public Map<Instant, RaUsageLimits> getRaUsageLimitsPerInstant() {
        return null;
    }

    @Override
    public RaUsageLimits getRaUsageLimits(Instant instant) {
        return null;
    }

    @Override
    public RaUsageLimitsAdder newRaUsageLimits(String instantName) {
        return null;
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
    public <E extends Extension<Crac>> void addExtension(Class<? super E> aClass, E e) {
        //not used
    }

    @Override
    public <E extends Extension<Crac>> E getExtension(Class<? super E> aClass) {
        return null;
    }

    @Override
    public <E extends Extension<Crac>> E getExtensionByName(String s) {
        return null;
    }

    @Override
    public <E extends Extension<Crac>> boolean removeExtension(Class<E> aClass) {
        return false;
    }

    @Override
    public <E extends Extension<Crac>> Collection<E> getExtensions() {
        return null;
    }
}
