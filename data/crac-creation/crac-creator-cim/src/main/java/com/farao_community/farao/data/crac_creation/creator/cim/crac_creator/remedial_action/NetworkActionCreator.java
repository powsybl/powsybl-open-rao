/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.remedial_action;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkActionAdder;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimConstants;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.FaraoImportException;
import com.farao_community.farao.data.crac_creation.creator.cim.xsd.RemedialActionRegisteredResource;
import com.farao_community.farao.data.crac_creation.util.PstHelper;
import com.farao_community.farao.data.crac_creation.util.cgmes.CgmesBranchHelper;
import com.farao_community.farao.data.crac_creation.util.iidm.IidmPstHelper;
import com.powsybl.iidm.network.*;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimConstants.MEGAWATT_UNIT_SYMBOL;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class NetworkActionCreator {
    private final Crac crac;
    private final Network network;
    private final String createdRemedialActionId;
    private final String createdRemedialActionName;
    private final String applicationModeMarketObjectStatus;
    private final List<RemedialActionRegisteredResource> networkActionRegisteredResources;
    private RemedialActionSeriesCreationContext networkActionCreationContext;
    private NetworkActionAdder networkActionAdder;
    private final List<Contingency> contingencies;
    private final List<String> invalidContingencies;
    private final Set<FlowCnec> flowCnecs;
    private final AngleCnec angleCnec;
    private final Country sharedDomain;

    public NetworkActionCreator(Crac crac, Network network, String createdRemedialActionId, String createdRemedialActionName, String applicationModeMarketObjectStatus, List<RemedialActionRegisteredResource> networkActionRegisteredResources, List<Contingency> contingencies, List<String> invalidContingencies, Set<FlowCnec> flowCnecs, AngleCnec angleCnec, Country sharedDomain) {
        this.crac = crac;
        this.network = network;
        this.createdRemedialActionId = createdRemedialActionId;
        this.createdRemedialActionName = createdRemedialActionName;
        this.applicationModeMarketObjectStatus = applicationModeMarketObjectStatus;
        this.networkActionRegisteredResources = networkActionRegisteredResources;
        this.contingencies = contingencies;
        this.invalidContingencies = invalidContingencies;
        this.flowCnecs = flowCnecs;
        this.angleCnec = angleCnec;
        this.sharedDomain = sharedDomain;
    }

    public void createNetworkActionAdder() {
        this.networkActionAdder = crac.newNetworkAction()
            .withId(createdRemedialActionId)
            .withName(createdRemedialActionName)
            .withOperator(CimConstants.readOperator(createdRemedialActionId));

        try {
            RemedialActionSeriesCreator.addUsageRules(applicationModeMarketObjectStatus, networkActionAdder, contingencies, invalidContingencies, flowCnecs, angleCnec, sharedDomain);

            // Elementary actions
            for (RemedialActionRegisteredResource remedialActionRegisteredResource : networkActionRegisteredResources) {
                String psrType = remedialActionRegisteredResource.getPSRTypePsrType();
                String elementaryActionId = remedialActionRegisteredResource.getName();
                if (Objects.isNull(psrType)) {
                    this.networkActionCreationContext = RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCOMPLETE_DATA, String.format("Missing psrType on elementary action %s", elementaryActionId));
                    return;
                }
                if (psrType.equals(CimConstants.PsrType.PST.getStatus())) {
                    // PST setpoint elementary action
                    addPstSetpointElementaryAction(elementaryActionId, remedialActionRegisteredResource, networkActionAdder);
                } else if (psrType.equals(CimConstants.PsrType.GENERATION.getStatus()) || psrType.equals(CimConstants.PsrType.LOAD.getStatus())) {
                    // Injection elementary action
                    addInjectionSetpointElementaryAction(elementaryActionId, remedialActionRegisteredResource, networkActionAdder);
                    // ------ TODO : Missing check : default capacity in ohms
                } else if (psrType.equals(CimConstants.PsrType.LINE.getStatus()) && remedialActionRegisteredResource.getMarketObjectStatusStatus().equals(CimConstants.MarketObjectStatus.ABSOLUTE.getStatus())) {
                    this.networkActionCreationContext = RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.NOT_YET_HANDLED_BY_FARAO, String.format("Modify line impedance as remedial action on elementary action %s", elementaryActionId));
                    return;
                } else if (psrType.equals(CimConstants.PsrType.TIE_LINE.getStatus()) || psrType.equals(CimConstants.PsrType.LINE.getStatus()) || psrType.equals(CimConstants.PsrType.CIRCUIT_BREAKER.getStatus()) || psrType.equals(CimConstants.PsrType.TRANSFORMER.getStatus())) {
                    // Topological elementary action
                    addTopologicalElementaryAction(elementaryActionId, remedialActionRegisteredResource, networkActionAdder);
                } else if (psrType.equals(CimConstants.PsrType.DEPRECATED_LINE.getStatus())) {
                    this.networkActionCreationContext = RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong psrType: %s, deprecated LINE psrType on elementary action %s", psrType, elementaryActionId));
                    return;
                } else {
                    this.networkActionCreationContext = RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong psrType: %s on elementary action %s", psrType, elementaryActionId));
                    return;
                }
            }
            this.networkActionCreationContext = RemedialActionSeriesCreator.importWithContingencies(createdRemedialActionId, invalidContingencies);
        } catch (FaraoImportException e) {
            this.networkActionCreationContext = RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, e.getImportStatus(), e.getMessage());
        }
    }

    private void addPstSetpointElementaryAction(String elementaryActionId, RemedialActionRegisteredResource remedialActionRegisteredResource, NetworkActionAdder networkActionAdder) {
        RemedialActionSeriesCreator.checkPstUnit(remedialActionRegisteredResource.getResourceCapacityUnitSymbol());

        // Pst helper
        String networkElementId = remedialActionRegisteredResource.getMRID().getValue();
        IidmPstHelper pstHelper = new IidmPstHelper(networkElementId, network);
        if (!pstHelper.isValid()) {
            throw new FaraoImportException(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, String.format("%s on elementary action %s", pstHelper.getInvalidReason(), elementaryActionId));
        }
        // --- Market Object status: define RangeType
        String marketObjectStatusStatus = remedialActionRegisteredResource.getMarketObjectStatusStatus();
        int setpoint;
        if (Objects.isNull(marketObjectStatusStatus)) {
            throw new FaraoImportException(ImportStatus.INCOMPLETE_DATA, String.format("Missing marketObjectStatus on elementary action %s", elementaryActionId));
        } else if (marketObjectStatusStatus.equals(CimConstants.MarketObjectStatus.ABSOLUTE.getStatus())) {
            setpoint = pstHelper.normalizeTap(remedialActionRegisteredResource.getResourceCapacityDefaultCapacity().intValue(), PstHelper.TapConvention.STARTS_AT_ONE);
        } else if (marketObjectStatusStatus.equals(CimConstants.MarketObjectStatus.RELATIVE_TO_INITIAL_NETWORK.getStatus())) {
            setpoint = pstHelper.getInitialTap() + remedialActionRegisteredResource.getResourceCapacityDefaultCapacity().intValue();
        } else if (marketObjectStatusStatus.equals(CimConstants.MarketObjectStatus.OPEN.getStatus()) || marketObjectStatusStatus.equals(CimConstants.MarketObjectStatus.CLOSE.getStatus())) {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong marketObjectStatusStatus: %s, PST can no longer be opened/closed (deprecated) on elementary action %s", marketObjectStatusStatus, elementaryActionId));
        } else {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong marketObjectStatusStatus: %s on elementary action %s", marketObjectStatusStatus, elementaryActionId));
        }
        networkActionAdder.newPstSetPoint()
            .withNetworkElement(networkElementId)
            .withSetpoint(setpoint)
            .add();
    }

    private void addInjectionSetpointElementaryAction(String elementaryActionId, RemedialActionRegisteredResource remedialActionRegisteredResource, NetworkActionAdder networkActionAdder) {
        // Injection range actions aren't handled
        if (Objects.nonNull(remedialActionRegisteredResource.getResourceCapacityMinimumCapacity()) || Objects.nonNull(remedialActionRegisteredResource.getResourceCapacityMaximumCapacity())) {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Injection setpoint elementary action %s should not have a min or max capacity defined", elementaryActionId));
        }
        // Market object status
        String marketObjectStatusStatus = remedialActionRegisteredResource.getMarketObjectStatusStatus();
        if (Objects.isNull(marketObjectStatusStatus)) {
            throw new FaraoImportException(ImportStatus.INCOMPLETE_DATA, String.format("Missing marketObjectStatus on injection setpoint elementary action %s", elementaryActionId));
        }
        int setpoint;
        if (marketObjectStatusStatus.equals(CimConstants.MarketObjectStatus.ABSOLUTE.getStatus())) {
            if (Objects.isNull(remedialActionRegisteredResource.getResourceCapacityDefaultCapacity())
                || Objects.isNull(remedialActionRegisteredResource.getResourceCapacityUnitSymbol())) {
                throw new FaraoImportException(ImportStatus.INCOMPLETE_DATA, String.format("Injection setpoint elementary action %s with ABSOLUTE marketObjectStatus should have a defaultCapacity and a unitSymbol", elementaryActionId));
            }
            if (!remedialActionRegisteredResource.getResourceCapacityUnitSymbol().equals(MEGAWATT_UNIT_SYMBOL)) {
                throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong unit symbol for injection setpoint elementary action %s with ABSOLUTE marketObjectStatus : %s", elementaryActionId, remedialActionRegisteredResource.getResourceCapacityUnitSymbol()));
            }
            setpoint = remedialActionRegisteredResource.getResourceCapacityDefaultCapacity().intValue();
        } else if (marketObjectStatusStatus.equals(CimConstants.MarketObjectStatus.STOP.getStatus())) {
            if (Objects.nonNull(remedialActionRegisteredResource.getResourceCapacityDefaultCapacity())
                || Objects.nonNull(remedialActionRegisteredResource.getResourceCapacityUnitSymbol())) {
                throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Injection setpoint elementary action %s with STOP marketObjectStatus shouldn't have a defaultCapacity nor a unitSymbol", elementaryActionId));
            }
            setpoint = 0;
        } else {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong marketObjectStatusStatus: %s on injection setpoint elementary action %s", marketObjectStatusStatus, elementaryActionId));
        }

        String networkElementId = remedialActionRegisteredResource.getMRID().getValue();
        checkGeneratorOrLoad(networkElementId);

        networkActionAdder.newInjectionSetPoint()
            .withNetworkElement(networkElementId)
            .withSetpoint(setpoint)
            .add();
    }

    private void checkGeneratorOrLoad(String networkElementId) {
        if (Objects.isNull(network.getGenerator(networkElementId)) && Objects.isNull(network.getLoad(networkElementId))) {
            throw new FaraoImportException(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, String.format("%s is neither a generator nor a load", networkElementId));
        }
    }

    private void addTopologicalElementaryAction(String elementaryActionId, RemedialActionRegisteredResource remedialActionRegisteredResource, NetworkActionAdder networkActionAdder) {
        if (Objects.nonNull(remedialActionRegisteredResource.getResourceCapacityMinimumCapacity()) || Objects.nonNull(remedialActionRegisteredResource.getResourceCapacityMaximumCapacity()) || Objects.nonNull(remedialActionRegisteredResource.getResourceCapacityDefaultCapacity())) {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Topological elementary action %s should not have any resource capacity defined", elementaryActionId));
        }
        // Market object status
        String marketObjectStatusStatus = remedialActionRegisteredResource.getMarketObjectStatusStatus();
        ActionType actionType;
        if (Objects.isNull(marketObjectStatusStatus)) {
            throw new FaraoImportException(ImportStatus.INCOMPLETE_DATA, String.format("Missing marketObjectStatus on topological elementary action %s", elementaryActionId));
        } else if (marketObjectStatusStatus.equals(CimConstants.MarketObjectStatus.OPEN.getStatus())) {
            actionType = ActionType.OPEN;
        } else if (marketObjectStatusStatus.equals(CimConstants.MarketObjectStatus.CLOSE.getStatus())) {
            actionType = ActionType.CLOSE;
        } else {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong marketObjectStatusStatus: %s on topological elementary action %s", marketObjectStatusStatus, elementaryActionId));
        }

        String networkElementId = remedialActionRegisteredResource.getMRID().getValue();
        Identifiable<?> element = network.getIdentifiable(networkElementId);
        if (Objects.isNull(element)) {
            // Check that network element is not half a tie line
            CgmesBranchHelper branchHelper = new CgmesBranchHelper(networkElementId, network);
            if (!branchHelper.isValid()) {
                throw new FaraoImportException(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, String.format("%s not in network on topological elementary action %s", networkElementId, elementaryActionId));
            }
            networkElementId = branchHelper.getIdInNetwork();
            element = branchHelper.getBranch();
        }
        if (!(element instanceof Branch) && !(element instanceof Switch)) {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("%s is nor a branch nor a switch on elementary action %s", networkElementId, elementaryActionId));
        }

        networkActionAdder.newTopologicalAction()
            .withNetworkElement(networkElementId)
            .withActionType(actionType)
            .add();
    }

    public void addNetworkAction() {
        if (this.networkActionCreationContext.isImported() && this.networkActionAdder != null) {
            try {
                this.networkActionAdder.add();
            } catch (FaraoException e) {
                this.networkActionCreationContext = RemedialActionSeriesCreationContext.notImported(this.networkActionCreationContext.getNativeId(), ImportStatus.INCONSISTENCY_IN_DATA, e.getMessage());
            }
        }
    }

    public RemedialActionSeriesCreationContext getNetworkActionCreationContext() {
        return networkActionCreationContext;
    }

    public NetworkActionAdder getNetworkActionAdder() {
        return networkActionAdder;
    }
}


