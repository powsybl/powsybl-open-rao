/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.cim.craccreator;

import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.*;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.cnec.AngleCnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkActionAdder;
import com.powsybl.openrao.data.cracio.commons.api.ImportStatus;
import com.powsybl.openrao.data.cracio.commons.OpenRaoImportException;
import com.powsybl.openrao.data.cracio.cim.xsd.RemedialActionRegisteredResource;
import com.powsybl.openrao.data.cracio.commons.PstHelper;
import com.powsybl.openrao.data.cracio.commons.cgmes.CgmesBranchHelper;
import com.powsybl.openrao.data.cracio.commons.iidm.IidmPstHelper;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.powsybl.openrao.data.cracio.cim.craccreator.CimConstants.MEGAWATT_UNIT_SYMBOL;

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
            RemedialActionSeriesCreator.addUsageRules(crac, applicationModeMarketObjectStatus, networkActionAdder, contingencies, invalidContingencies, flowCnecs, angleCnec, sharedDomain);

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
                } else if (psrType.equals(CimConstants.PsrType.GENERATION.getStatus())) {
                    addGeneratorElementaryAction(elementaryActionId, remedialActionRegisteredResource, networkActionAdder);
                } else if (psrType.equals(CimConstants.PsrType.LOAD.getStatus())) {
                    addLoadElementaryAction(elementaryActionId, remedialActionRegisteredResource, networkActionAdder);
                } else if (psrType.equals(CimConstants.PsrType.LINE.getStatus()) && remedialActionRegisteredResource.getMarketObjectStatusStatus().equals(CimConstants.MarketObjectStatus.ABSOLUTE.getStatus())) {
                    this.networkActionCreationContext = RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.NOT_YET_HANDLED_BY_OPEN_RAO, String.format("Modify line impedance as remedial action on elementary action %s", elementaryActionId));
                    return;
                } else if (psrType.equals(CimConstants.PsrType.TIE_LINE.getStatus()) || psrType.equals(CimConstants.PsrType.LINE.getStatus()) || psrType.equals(CimConstants.PsrType.TRANSFORMER.getStatus())) {
                    addTerminalsConnectionElementaryAction(elementaryActionId, remedialActionRegisteredResource, networkActionAdder);
                } else if (psrType.equals(CimConstants.PsrType.CIRCUIT_BREAKER.getStatus())) {
                    addSwitchElementaryAction(elementaryActionId, remedialActionRegisteredResource, networkActionAdder);
                } else if (psrType.equals(CimConstants.PsrType.DEPRECATED_LINE.getStatus())) {
                    this.networkActionCreationContext = RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong psrType: %s, deprecated LINE psrType on elementary action %s", psrType, elementaryActionId));
                    return;
                } else {
                    this.networkActionCreationContext = RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong psrType: %s on elementary action %s", psrType, elementaryActionId));
                    return;
                }
            }
            this.networkActionCreationContext = RemedialActionSeriesCreator.importWithContingencies(createdRemedialActionId, invalidContingencies);
        } catch (OpenRaoImportException e) {
            this.networkActionCreationContext = RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, e.getImportStatus(), e.getMessage());
        }
    }

    private void addPstSetpointElementaryAction(String elementaryActionId, RemedialActionRegisteredResource remedialActionRegisteredResource, NetworkActionAdder networkActionAdder) {
        RemedialActionSeriesCreator.checkPstUnit(remedialActionRegisteredResource.getResourceCapacityUnitSymbol());

        // Pst helper
        String networkElementId = remedialActionRegisteredResource.getMRID().getValue();
        IidmPstHelper pstHelper = new IidmPstHelper(networkElementId, network);
        if (!pstHelper.isValid()) {
            throw new OpenRaoImportException(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, String.format("%s on elementary action %s", pstHelper.getInvalidReason(), elementaryActionId));
        }
        // --- Market Object status: define RangeType
        String marketObjectStatusStatus = remedialActionRegisteredResource.getMarketObjectStatusStatus();
        int setpoint;
        if (Objects.isNull(marketObjectStatusStatus)) {
            throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA, String.format("Missing marketObjectStatus on elementary action %s", elementaryActionId));
        } else if (marketObjectStatusStatus.equals(CimConstants.MarketObjectStatus.ABSOLUTE.getStatus())) {
            setpoint = pstHelper.normalizeTap(remedialActionRegisteredResource.getResourceCapacityDefaultCapacity().intValue(), PstHelper.TapConvention.STARTS_AT_ONE);
        } else if (marketObjectStatusStatus.equals(CimConstants.MarketObjectStatus.RELATIVE_TO_INITIAL_NETWORK.getStatus())) {
            setpoint = pstHelper.getInitialTap() + remedialActionRegisteredResource.getResourceCapacityDefaultCapacity().intValue();
        } else if (marketObjectStatusStatus.equals(CimConstants.MarketObjectStatus.OPEN.getStatus()) || marketObjectStatusStatus.equals(CimConstants.MarketObjectStatus.CLOSE.getStatus())) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong marketObjectStatusStatus: %s, PST can no longer be opened/closed (deprecated) on elementary action %s", marketObjectStatusStatus, elementaryActionId));
        } else {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong marketObjectStatusStatus: %s on elementary action %s", marketObjectStatusStatus, elementaryActionId));
        }
        networkActionAdder.newPhaseTapChangerTapPositionAction()
            .withNetworkElement(networkElementId)
            .withTapPosition((pstHelper.getLowTapPosition() + pstHelper.getHighTapPosition()) / 2 + setpoint)
            .add();
    }

    private int checkAndComputeSetpointForInjectionElementaryAction(String elementaryActionId, RemedialActionRegisteredResource remedialActionRegisteredResource) {
        // Injection range actions aren't handled
        if (Objects.nonNull(remedialActionRegisteredResource.getResourceCapacityMinimumCapacity()) || Objects.nonNull(remedialActionRegisteredResource.getResourceCapacityMaximumCapacity())) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Injection setpoint elementary action %s should not have a min or max capacity defined", elementaryActionId));
        }
        // Market object status
        String marketObjectStatusStatus = remedialActionRegisteredResource.getMarketObjectStatusStatus();
        if (Objects.isNull(marketObjectStatusStatus)) {
            throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA, String.format("Missing marketObjectStatus on injection setpoint elementary action %s", elementaryActionId));
        }
        if (marketObjectStatusStatus.equals(CimConstants.MarketObjectStatus.ABSOLUTE.getStatus())) {
            if (Objects.isNull(remedialActionRegisteredResource.getResourceCapacityDefaultCapacity())
                || Objects.isNull(remedialActionRegisteredResource.getResourceCapacityUnitSymbol())) {
                throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA, String.format("Injection setpoint elementary action %s with ABSOLUTE marketObjectStatus should have a defaultCapacity and a unitSymbol", elementaryActionId));
            }
            if (!remedialActionRegisteredResource.getResourceCapacityUnitSymbol().equals(MEGAWATT_UNIT_SYMBOL)) {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong unit symbol for injection setpoint elementary action %s with ABSOLUTE marketObjectStatus : %s", elementaryActionId, remedialActionRegisteredResource.getResourceCapacityUnitSymbol()));
            }
            return remedialActionRegisteredResource.getResourceCapacityDefaultCapacity().intValue();
        } else if (marketObjectStatusStatus.equals(CimConstants.MarketObjectStatus.STOP.getStatus())) {
            if (Objects.nonNull(remedialActionRegisteredResource.getResourceCapacityDefaultCapacity())
                || Objects.nonNull(remedialActionRegisteredResource.getResourceCapacityUnitSymbol())) {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Injection setpoint elementary action %s with STOP marketObjectStatus shouldn't have a defaultCapacity nor a unitSymbol", elementaryActionId));
            }
            return 0;
        } else {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong marketObjectStatusStatus: %s on injection setpoint elementary action %s", marketObjectStatusStatus, elementaryActionId));
        }
    }

    private Identifiable<?> checkAndComputeNetworkElementForInjectionElementaryAction(RemedialActionRegisteredResource remedialActionRegisteredResource) {
        String networkElementId = remedialActionRegisteredResource.getMRID().getValue();
        Identifiable<?> networkElement = network.getIdentifiable(networkElementId);
        if (Objects.isNull(networkElement)) {
            throw new OpenRaoImportException(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, String.format("%s not found in network", networkElementId));
        }
        return networkElement;
    }

    private void addGeneratorElementaryAction(String elementaryActionId, RemedialActionRegisteredResource remedialActionRegisteredResource, NetworkActionAdder networkActionAdder) {
        int setpoint = checkAndComputeSetpointForInjectionElementaryAction(elementaryActionId, remedialActionRegisteredResource);
        Identifiable<?> networkElement = checkAndComputeNetworkElementForInjectionElementaryAction(remedialActionRegisteredResource);
        if (networkElement.getType() != IdentifiableType.GENERATOR) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("%s is not a generator but a %s while psrType is %s", networkElement.getId(), networkElement.getType(), CimConstants.PsrType.GENERATION.getStatus()));
        }
        networkActionAdder.newGeneratorAction()
            .withNetworkElement(networkElement.getId())
            .withActivePowerValue(setpoint)
            .add();
    }

    private void addLoadElementaryAction(String elementaryActionId, RemedialActionRegisteredResource remedialActionRegisteredResource, NetworkActionAdder networkActionAdder) {
        int setpoint = checkAndComputeSetpointForInjectionElementaryAction(elementaryActionId, remedialActionRegisteredResource);
        Identifiable<?> networkElement = checkAndComputeNetworkElementForInjectionElementaryAction(remedialActionRegisteredResource);
        if (networkElement.getType() != IdentifiableType.LOAD) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("%s is not a load but a %s while psrType is %s", networkElement.getId(), networkElement.getType(), CimConstants.PsrType.LOAD.getStatus()));
        }
        networkActionAdder.newLoadAction()
            .withNetworkElement(networkElement.getId())
            .withActivePowerValue(setpoint)
            .add();
    }

    private ActionType checkAndComputeActionTypeForTopologicalElementaryAction(String elementaryActionId, RemedialActionRegisteredResource remedialActionRegisteredResource) {
        if (Objects.nonNull(remedialActionRegisteredResource.getResourceCapacityMinimumCapacity()) || Objects.nonNull(remedialActionRegisteredResource.getResourceCapacityMaximumCapacity()) || Objects.nonNull(remedialActionRegisteredResource.getResourceCapacityDefaultCapacity())) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Topological elementary action %s should not have any resource capacity defined", elementaryActionId));
        }
        // Market object status
        String marketObjectStatusStatus = remedialActionRegisteredResource.getMarketObjectStatusStatus();
        if (Objects.isNull(marketObjectStatusStatus)) {
            throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA, String.format("Missing marketObjectStatus on topological elementary action %s", elementaryActionId));
        } else if (marketObjectStatusStatus.equals(CimConstants.MarketObjectStatus.OPEN.getStatus())) {
            return ActionType.OPEN;
        } else if (marketObjectStatusStatus.equals(CimConstants.MarketObjectStatus.CLOSE.getStatus())) {
            return ActionType.CLOSE;
        } else {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong marketObjectStatusStatus: %s on topological elementary action %s", marketObjectStatusStatus, elementaryActionId));
        }
    }

    private void addTerminalsConnectionElementaryAction(String elementaryActionId, RemedialActionRegisteredResource remedialActionRegisteredResource, NetworkActionAdder networkActionAdder) {
        ActionType actionType = checkAndComputeActionTypeForTopologicalElementaryAction(elementaryActionId, remedialActionRegisteredResource);
        String networkElementId = remedialActionRegisteredResource.getMRID().getValue();
        Identifiable<?> element = network.getIdentifiable(networkElementId);
        if (Objects.isNull(element)) {
            // Check that network element is not half a tie line
            CgmesBranchHelper branchHelper = new CgmesBranchHelper(networkElementId, network);
            if (!branchHelper.isValid()) {
                throw new OpenRaoImportException(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, String.format("%s not in network on topological elementary action %s", networkElementId, elementaryActionId));
            }
            networkElementId = branchHelper.getIdInNetwork();
            element = branchHelper.getBranch();
        }
        if (element instanceof Branch) {
            networkActionAdder.newTerminalsConnectionAction()
                .withNetworkElement(networkElementId)
                .withActionType(actionType)
                .add();
        } else {
            throw new OpenRaoImportException(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, String.format("%s is not branch but a %s", networkElementId, element.getType()));
        }
    }

    private void addSwitchElementaryAction(String elementaryActionId, RemedialActionRegisteredResource remedialActionRegisteredResource, NetworkActionAdder networkActionAdder) {
        ActionType actionType = checkAndComputeActionTypeForTopologicalElementaryAction(elementaryActionId, remedialActionRegisteredResource);
        String networkElementId = remedialActionRegisteredResource.getMRID().getValue();
        Identifiable<?> element = network.getIdentifiable(networkElementId);
        if (Objects.isNull(element)) {
            throw new OpenRaoImportException(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, String.format("%s not found in network %s", networkElementId, network.getId()));
        }
        if (element.getType() == IdentifiableType.SWITCH) {
            networkActionAdder.newSwitchAction()
                .withNetworkElement(networkElementId)
                .withActionType(actionType)
                .add();
        } else {
            throw new OpenRaoImportException(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, String.format("%s is not a switch but a %s", networkElementId, element.getType()));
        }
    }

    public void addNetworkAction() {
        if (this.networkActionCreationContext.isImported() && this.networkActionAdder != null) {
            try {
                this.networkActionAdder.add();
            } catch (OpenRaoException e) {
                this.networkActionCreationContext = RemedialActionSeriesCreationContext.notImported(this.networkActionCreationContext.getNativeObjectId(), ImportStatus.INCONSISTENCY_IN_DATA, e.getMessage());
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


