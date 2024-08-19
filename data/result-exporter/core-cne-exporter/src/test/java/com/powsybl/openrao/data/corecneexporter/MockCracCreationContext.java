/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.corecneexporter;

import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.NetworkElement;
import com.powsybl.openrao.data.cracapi.RemedialAction;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.CracCreationReport;
import com.powsybl.openrao.data.cracio.commons.api.ElementaryCreationContext;
import com.powsybl.openrao.data.cracio.commons.api.ImportStatus;
import com.powsybl.openrao.data.cracio.commons.api.stdcreationcontext.*;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dummy class that has no real use, but allows the CRAC exporters
 * to function with cracs that were created in the code or using a json file
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class MockCracCreationContext implements UcteCracCreationContext {

    private Crac crac;
    private List<MockCnecCreationContext> mockCnecCreationContexts;
    private List<MockRemedialActionCreationContext> mockRemedialActionCreationContexts;

    public MockCracCreationContext(Crac crac) {
        this.crac = crac;

        this.mockCnecCreationContexts = new ArrayList<>();
        this.crac.getFlowCnecs().forEach(flowCnec -> addCnecCreationContext(flowCnec, crac));

        this.mockRemedialActionCreationContexts = new ArrayList<>();
        this.crac.getRangeActions().forEach(rangeAction -> mockRemedialActionCreationContexts.add(new MockRemedialActionCreationContext(rangeAction, crac)));
        this.crac.getNetworkActions().forEach(networkAction -> mockRemedialActionCreationContexts.add(new MockRemedialActionCreationContext(networkAction, crac)));
    }

    private void addCnecCreationContext(FlowCnec flowCnec, Crac crac) {
        List<MockCnecCreationContext> cnecsWithSameNe = mockCnecCreationContexts.stream().filter(creationContext ->
                creationContext.getFlowCnec().getNetworkElements().equals(flowCnec.getNetworkElements())
                && creationContext.getFlowCnec().getState().getContingency().equals(flowCnec.getState().getContingency())
        ).collect(Collectors.toList());
        if (cnecsWithSameNe.isEmpty()) {
            mockCnecCreationContexts.add(new MockCnecCreationContext(flowCnec, crac));
        } else {
            cnecsWithSameNe.get(0).addCreatedCnec(flowCnec);
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

    public class MockCnecCreationContext implements BranchCnecCreationContext {
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

    public class MockRemedialActionCreationContext implements PstRangeActionCreationContext {

        private RemedialAction remedialAction;
        boolean isImported;
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
