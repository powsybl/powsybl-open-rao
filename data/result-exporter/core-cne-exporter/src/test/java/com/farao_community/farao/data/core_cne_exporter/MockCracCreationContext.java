/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.core_cne_exporter;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_creation.creator.api.CracCreationReport;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.api.std_creation_context.*;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Dummy class that has no real use, but allows the CRAC exporters
 * to function with cracs that were created in the code or using a json file
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class MockCracCreationContext implements StandardCracCreationContext {

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
                creationContext.getFlowCnec().getNetworkElement().equals(flowCnec.getNetworkElement())
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
    public List<? extends RemedialActionCreationContext> getRemedialActionCreationContexts() {
        return mockRemedialActionCreationContexts;
    }

    @Override
    public RemedialActionCreationContext getRemedialActionCreationContext(String s) {
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
        Map<Instant, String> createdCnecsIds;

        MockCnecCreationContext(FlowCnec flowCnec, Crac crac) {
            this.flowCnec = flowCnec;
            this.isBaseCase = flowCnec.getState().getContingency().isEmpty();
            this.nativeBranch = getNativeBranch(flowCnec);
            this.isImported = crac.getFlowCnec(flowCnec.getId()) != null;
            this.createdCnecsIds = buildCreatedCnecsIds(flowCnec, crac);
        }

        private Map<Instant, String> buildCreatedCnecsIds(FlowCnec flowCnec, Crac crac) {
            Map<Instant, String> map = new HashMap<>();
            map.put(flowCnec.getState().getInstant(), flowCnec.getId());
            if (!isBaseCase) {
                Stream.of(Instant.values())
                    .filter(instant -> instant != Instant.PREVENTIVE)
                    .filter(instant -> instant != flowCnec.getState().getInstant())
                    .forEach(instant -> {
                        FlowCnec otherBranchCnec = crac.getFlowCnecs(crac.getState(flowCnec.getState().getContingency().get().getId(), instant)).stream()
                            .filter(flowCnec1 -> flowCnec1.getNetworkElement().equals(flowCnec.getNetworkElement()))
                            .findFirst()
                            .orElse(flowCnec);
                        map.put(instant, otherBranchCnec.getId());
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
        public String getNativeId() {
            return flowCnec.getId();
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
        public Map<Instant, String> getCreatedCnecsIds() {
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
            createdCnecsIds.put(flowCnec.getState().getInstant(), flowCnec.getId());
        }
    }

    public class MockRemedialActionCreationContext implements PstRangeActionCreationContext {

        private RemedialAction remedialAction;
        boolean isImported;
        boolean isInverted;
        String nativeNetworkElementId;

        public MockRemedialActionCreationContext(RemedialAction remedialAction, Crac crac) {
            this.remedialAction = remedialAction;
            this.isImported = (crac.getRangeAction(remedialAction.getId()) != null) || (crac.getNetworkAction(remedialAction.getId()) != null);
            this.isInverted = false;
            this.nativeNetworkElementId = ((NetworkElement) remedialAction.getNetworkElements().iterator().next()).getId();
        }

        @Override
        public String getNativeId() {
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
        public String getCreatedRAId() {
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
