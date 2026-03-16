/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.cne.core;

import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracCreationReport;
import com.powsybl.openrao.data.crac.api.NetworkElement;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.crac.io.commons.api.ElementaryCreationContext;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import com.powsybl.openrao.data.crac.io.commons.api.StandardElementaryCreationContext;
import com.powsybl.openrao.data.crac.io.commons.api.stdcreationcontext.BranchCnecCreationContext;
import com.powsybl.openrao.data.crac.io.commons.api.stdcreationcontext.NativeBranch;
import com.powsybl.openrao.data.crac.io.commons.api.stdcreationcontext.PstRangeActionCreationContext;
import com.powsybl.openrao.data.crac.io.commons.api.stdcreationcontext.UcteCracCreationContext;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Dummy class that has no real use, but allows the CRAC exporters
 * to function with cracs that were created in the code or using a json file
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class MockCracCreationContext implements UcteCracCreationContext {

    private final Crac crac;
    private final List<MockCnecCreationContext> mockCnecCreationContexts;
    private final List<ElementaryCreationContext> mockRemedialActionCreationContexts;

    public MockCracCreationContext(Crac crac) {
        this.crac = crac;

        this.mockCnecCreationContexts = new ArrayList<>();
        this.crac.getFlowCnecs().forEach(flowCnec -> addCnecCreationContext(flowCnec, crac));

        this.mockRemedialActionCreationContexts = new ArrayList<>();
        this.crac.getRangeActions().stream()
            .filter(rangeAction -> !(rangeAction instanceof InjectionRangeAction))
            .forEach(rangeAction -> mockRemedialActionCreationContexts.add(new MockRemedialActionCreationContext(rangeAction, crac)));
        this.crac.getInjectionRangeActions().stream()
            .filter(ira -> ira.getId().contains(" + ") && ira.getName().contains(" + "))
            .forEach(injectionRangeAction -> createRemedialActionCreationContext(crac, injectionRangeAction));
        this.crac.getNetworkActions().forEach(networkAction -> mockRemedialActionCreationContexts.add(new MockRemedialActionCreationContext(networkAction, crac)));
    }

    private void createRemedialActionCreationContext(final Crac crac, final InjectionRangeAction injectionRangeAction) {
        final String[] splitId = injectionRangeAction.getId().split(" \\+ ");
        final String[] splitName = injectionRangeAction.getName().split(" \\+ ");
        final ImportStatus importStatus = crac.getRangeAction(injectionRangeAction.getId()) != null || crac.getNetworkAction(injectionRangeAction.getId()) != null
            ? ImportStatus.IMPORTED
            : ImportStatus.OTHER;
        mockRemedialActionCreationContexts.add(new StandardElementaryCreationContext(
            splitId[0], splitName[0], splitId[0], importStatus, "", false
        ));
        mockRemedialActionCreationContexts.add(new StandardElementaryCreationContext(
            splitId[1], splitName[1], splitId[1], importStatus, "", false
        ));
    }

    private void addCnecCreationContext(FlowCnec flowCnec, Crac crac) {
        List<MockCnecCreationContext> cnecsWithSameNe = mockCnecCreationContexts.stream().filter(creationContext ->
                creationContext.getFlowCnec().getNetworkElements().equals(flowCnec.getNetworkElements())
                && creationContext.getFlowCnec().getState().getContingency().equals(flowCnec.getState().getContingency())
        ).toList();
        if (cnecsWithSameNe.isEmpty()) {
            mockCnecCreationContexts.add(new MockCnecCreationContext(flowCnec, crac));
        } else {
            cnecsWithSameNe.getFirst().addCreatedCnec(flowCnec);
        }
    }

    @Override
    public List<? extends BranchCnecCreationContext> getBranchCnecCreationContexts() {
        return mockCnecCreationContexts;
    }

    @Override
    public BranchCnecCreationContext getBranchCnecCreationContext(String s) {
        return null;
    }

    @Override
    public List<? extends ElementaryCreationContext> getRemedialActionCreationContexts() {
        return mockRemedialActionCreationContexts;
    }

    @Override
    public ElementaryCreationContext getRemedialActionCreationContext(String s) {
        return null;
    }

    @Override
    public OffsetDateTime getTimeStamp() {
        return OffsetDateTime.now();
    }

    @Override
    public String getNetworkName() {
        return null;
    }

    @Override
    public boolean isCreationSuccessful() {
        return true;
    }

    @Override
    public Crac getCrac() {
        return crac;
    }

    @Override
    public CracCreationReport getCreationReport() {
        return null;
    }

    public static class MockCnecCreationContext implements BranchCnecCreationContext {
        FlowCnec flowCnec;
        boolean isBaseCase;
        boolean isImported;
        NativeBranch nativeBranch;
        Map<String, String> createdCnecsIds;

        MockCnecCreationContext(FlowCnec flowCnec, Crac crac) {
            this.flowCnec = flowCnec;
            this.isBaseCase = flowCnec.getState().getContingency().isEmpty();
            this.nativeBranch = getNativeBranch(flowCnec);
            this.isImported = crac.getFlowCnec(flowCnec.getId()) != null;
            this.createdCnecsIds = buildCreatedCnecsIds(flowCnec, crac);
        }

        private Map<String, String> buildCreatedCnecsIds(FlowCnec flowCnec, Crac crac) {
            Map<String, String> map = new HashMap<>();
            map.put(flowCnec.getState().getInstant().getId(), flowCnec.getId());
            if (!isBaseCase) {
                crac.getSortedInstants().stream()
                    .filter(instant -> !instant.isPreventive())
                    .filter(instant -> instant != flowCnec.getState().getInstant())
                    .forEach(instant -> {
                        FlowCnec otherBranchCnec = crac.getFlowCnecs(crac.getState(flowCnec.getState().getContingency().get().getId(), instant)).stream()
                            .filter(flowCnec1 -> flowCnec1.getNetworkElements().equals(flowCnec.getNetworkElements()))
                            .findFirst()
                            .orElse(flowCnec);
                        map.put(instant.getId(), otherBranchCnec.getId());
                    });
            }
            return map;
        }

        private NativeBranch getNativeBranch(FlowCnec flowCnec) {
            String[] split = flowCnec.getNetworkElement().getId().split("[ ]+");
            String from = split[0];
            String to = split[1];
            String suffix = split[2];
            return new NativeBranch(from, to, suffix);
        }

        @Override
        public String getNativeObjectId() {
            return flowCnec.getId();
        }

        @Override
        public String getNativeObjectName() {
            return flowCnec.getId();
        }

        @Override
        public String getCreatedObjectId() {
            return null;
        }

        @Override
        public NativeBranch getNativeBranch() {
            return nativeBranch;
        }

        @Override
        public boolean isBaseCase() {
            return isBaseCase;
        }

        @Override
        public Optional<String> getContingencyId() {
            if (flowCnec.getState().getContingency().isPresent()) {
                return Optional.of(flowCnec.getState().getContingency().get().getId());
            } else {
                return Optional.empty();
            }
        }

        @Override
        public boolean isImported() {
            return isImported;
        }

        @Override
        public boolean isAltered() {
            return false;
        }

        @Override
        public ImportStatus getImportStatus() {
            return isImported ? ImportStatus.IMPORTED : ImportStatus.OTHER;
        }

        @Override
        public String getImportStatusDetail() {
            return "";
        }

        @Override
        public Map<String, String> getCreatedCnecsIds() {
            return createdCnecsIds;
        }

        @Override
        public boolean isDirectionInvertedInNetwork() {
            return false;
        }

        FlowCnec getFlowCnec() {
            return flowCnec;
        }

        void addCreatedCnec(FlowCnec flowCnec) {
            createdCnecsIds.put(flowCnec.getState().getInstant().getId(), flowCnec.getId());
        }
    }

    public static class MockRemedialActionCreationContext implements PstRangeActionCreationContext {
        private final RemedialAction remedialAction;
        private final boolean isImported;
        boolean isInverted;
        String nativeNetworkElementId;

        public MockRemedialActionCreationContext(RemedialAction remedialAction, Crac crac) {
            this.remedialAction = remedialAction;
            this.isImported = crac.getRangeAction(remedialAction.getId()) != null || crac.getNetworkAction(remedialAction.getId()) != null;
            this.isInverted = false;
            this.nativeNetworkElementId = ((NetworkElement) remedialAction.getNetworkElements().iterator().next()).getId();
        }

        @Override
        public String getNativeObjectId() {
            return remedialAction.getId();
        }

        @Override
        public String getNativeObjectName() {
            return remedialAction.getId();
        }

        @Override
        public boolean isImported() {
            return isImported;
        }

        @Override
        public boolean isAltered() {
            return false;
        }

        @Override
        public ImportStatus getImportStatus() {
            return isImported ? ImportStatus.IMPORTED : ImportStatus.OTHER;
        }

        @Override
        public String getImportStatusDetail() {
            return "";
        }

        @Override
        public String getCreatedObjectId() {
            return remedialAction.getId();
        }

        @Override
        public boolean isInverted() {
            return isInverted;
        }

        @Override
        public String getNativeNetworkElementId() {
            return nativeNetworkElementId;
        }

        public void setInverted(boolean inverted) {
            isInverted = inverted;
        }

        public void setNativeNetworkElementId(String nativeNetworkElementId) {
            this.nativeNetworkElementId = nativeNetworkElementId;
        }
    }
}
