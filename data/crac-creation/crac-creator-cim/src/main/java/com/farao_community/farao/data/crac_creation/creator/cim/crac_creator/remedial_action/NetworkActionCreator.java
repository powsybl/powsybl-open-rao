/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.remedial_action;

import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkActionAdder;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimConstants;
import com.farao_community.farao.data.crac_creation.creator.cim.xsd.RemedialActionRegisteredResource;
import com.farao_community.farao.data.crac_creation.util.PstHelper;
import com.farao_community.farao.data.crac_creation.util.cgmes.CgmesBranchHelper;
import com.farao_community.farao.data.crac_creation.util.iidm.IidmPstHelper;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Switch;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimConstants.MEGAWATT_UNIT_SYMBOL;

/**
 * @author Godelaine de Montmorillon <godelaine.demontmorillon at rte-france.com>
 */
public class NetworkActionCreator {
    private final Crac crac;
    private final Network network;
    private final String createdRemedialActionId;
    private final String createdRemedialActionName;
    private final String applicationModeMarketObjectStatus;
    private final List<RemedialActionRegisteredResource> networkActionRegisteredResources;
    private Set<RemedialActionSeriesCreationContext> networkActionCreationContexts;
    private final List<Contingency> contingencies;
    private final List<String> invalidContingencies;
    private Set<FlowCnec> flowCnecs;

    public NetworkActionCreator(Crac crac, Network network, String createdRemedialActionId, String createdRemedialActionName, String applicationModeMarketObjectStatus, List<RemedialActionRegisteredResource> networkActionRegisteredResources, List<Contingency> contingencies, List<String> invalidContingencies, Set<FlowCnec> flowCnecs) {
        this.crac = crac;
        this.network = network;
        this.createdRemedialActionId = createdRemedialActionId;
        this.createdRemedialActionName = createdRemedialActionName;
        this.applicationModeMarketObjectStatus = applicationModeMarketObjectStatus;
        this.networkActionRegisteredResources = networkActionRegisteredResources;
        this.contingencies = contingencies;
        this.invalidContingencies = invalidContingencies;
        this.flowCnecs = flowCnecs;
    }

    public Set<RemedialActionSeriesCreationContext> addRemedialActionNetworkAction() {
        this.networkActionCreationContexts = new HashSet<>();
        NetworkActionAdder networkActionAdder = crac.newNetworkAction()
                .withId(createdRemedialActionId)
                .withName(createdRemedialActionName);

        if (!RemedialActionSeriesCreator.addUsageRules(createdRemedialActionId, applicationModeMarketObjectStatus, networkActionAdder, contingencies, invalidContingencies, flowCnecs, networkActionCreationContexts)) {
            return networkActionCreationContexts;
        }

        // Elementary actions
        for (RemedialActionRegisteredResource remedialActionRegisteredResource : networkActionRegisteredResources) {
            String psrType = remedialActionRegisteredResource.getPSRTypePsrType();
            String elementaryActionId = remedialActionRegisteredResource.getName();
            if (Objects.isNull(psrType)) {
                networkActionCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCOMPLETE_DATA, String.format("Missing psrType on elementary action %s", elementaryActionId)));
                return networkActionCreationContexts;
            }

            // ------ PST setpoint elementary action
            if (psrType.equals(CimConstants.PsrType.PST.getStatus())) {
                if (!addPstSetpointElementaryAction(createdRemedialActionId, elementaryActionId, remedialActionRegisteredResource, networkActionAdder)) {
                    return networkActionCreationContexts;
                }
                // ------ Injection elementary action
            } else if (psrType.equals(CimConstants.PsrType.GENERATION.getStatus()) || psrType.equals(CimConstants.PsrType.LOAD.getStatus())) {
                if (!addInjectionSetpointElementaryAction(createdRemedialActionId, elementaryActionId, remedialActionRegisteredResource, networkActionAdder)) {
                    return networkActionCreationContexts;
                }
                // ------ TODO : Missing check : default capacity in ohms
            } else if (psrType.equals(CimConstants.PsrType.LINE.getStatus()) && remedialActionRegisteredResource.getMarketObjectStatusStatus().equals(CimConstants.MarketObjectStatus.ABSOLUTE.getStatus())) {
                networkActionCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.NOT_YET_HANDLED_BY_FARAO, String.format("Modify line impedance as remedial action on elementary action %s", elementaryActionId)));
                return networkActionCreationContexts;
                // ------ Topological elementary action
            } else if (psrType.equals(CimConstants.PsrType.TIE_LINE.getStatus()) || psrType.equals(CimConstants.PsrType.LINE.getStatus()) || psrType.equals(CimConstants.PsrType.CIRCUIT_BREAKER.getStatus()) || psrType.equals(CimConstants.PsrType.TRANSFORMER.getStatus())) {
                if (!addTopologicalElementaryAction(createdRemedialActionId, elementaryActionId, remedialActionRegisteredResource, networkActionAdder)) {
                    return networkActionCreationContexts;
                }
            } else if (psrType.equals(CimConstants.PsrType.DEPRECATED_LINE.getStatus())) {
                networkActionCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong psrType: %s, deprecated LINE psrType on elementary action %s", psrType, elementaryActionId)));
                return networkActionCreationContexts;
            } else {
                networkActionCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong psrType: %s on elementary action %s", psrType, elementaryActionId)));
                return networkActionCreationContexts;
            }
        }

        RemedialActionSeriesCreator.importWithContingencies(createdRemedialActionId, invalidContingencies, networkActionCreationContexts);
        networkActionAdder.add();
        return networkActionCreationContexts;
    }

    private boolean addPstSetpointElementaryAction(String createdRemedialActionId, String elementaryActionId, RemedialActionRegisteredResource remedialActionRegisteredResource, NetworkActionAdder networkActionAdder) {
        if (!RemedialActionSeriesCreator.checkPstUnit(createdRemedialActionId, remedialActionRegisteredResource.getResourceCapacityUnitSymbol(), networkActionCreationContexts)) {
            return false;
        }
        // Pst helper
        String networkElementId = remedialActionRegisteredResource.getMRID().getValue();
        IidmPstHelper pstHelper = new IidmPstHelper(networkElementId, network);
        if (!pstHelper.isValid()) {
            networkActionCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, String.format("%s on elementary action %s", pstHelper.getInvalidReason(), elementaryActionId)));
            return false;
        }
        // --- Market Object status: define RangeType
        String marketObjectStatusStatus = remedialActionRegisteredResource.getMarketObjectStatusStatus();
        int setpoint;
        if (Objects.isNull(marketObjectStatusStatus)) {
            networkActionCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCOMPLETE_DATA, String.format("Missing marketObjectStatus on elementary action %s", elementaryActionId)));
            return false;
        } else if (marketObjectStatusStatus.equals(CimConstants.MarketObjectStatus.ABSOLUTE.getStatus())) {
            setpoint = pstHelper.normalizeTap(remedialActionRegisteredResource.getResourceCapacityDefaultCapacity().intValue(), PstHelper.TapConvention.STARTS_AT_ONE);
        } else if (marketObjectStatusStatus.equals(CimConstants.MarketObjectStatus.RELATIVE_TO_INITIAL_NETWORK.getStatus())) {
            setpoint = pstHelper.getInitialTap() + remedialActionRegisteredResource.getResourceCapacityDefaultCapacity().intValue();
        } else if (marketObjectStatusStatus.equals(CimConstants.MarketObjectStatus.OPEN.getStatus()) || marketObjectStatusStatus.equals(CimConstants.MarketObjectStatus.CLOSE.getStatus())) {
            networkActionCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong marketObjectStatusStatus: %s, PST can no longer be opened/closed (deprecated) on elementary action %s", marketObjectStatusStatus, elementaryActionId)));
            return false;
        } else {
            networkActionCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong marketObjectStatusStatus: %s on elementary action %s", marketObjectStatusStatus, elementaryActionId)));
            return false;
        }
        networkActionAdder.newPstSetPoint()
                .withNetworkElement(networkElementId)
                .withSetpoint(setpoint)
                .add();

        return true;
    }

    private boolean addInjectionSetpointElementaryAction(String createdRemedialActionId, String elementaryActionId, RemedialActionRegisteredResource remedialActionRegisteredResource, NetworkActionAdder networkActionAdder) {
        // Injection range actions aren't handled
        if (Objects.nonNull(remedialActionRegisteredResource.getResourceCapacityMinimumCapacity()) || Objects.nonNull(remedialActionRegisteredResource.getResourceCapacityMaximumCapacity())) {
            networkActionCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("Injection setpoint elementary action %s should not have a min or max capacity defined", elementaryActionId)));
            return false;
        }
        // Market object status
        String marketObjectStatusStatus = remedialActionRegisteredResource.getMarketObjectStatusStatus();
        int setpoint;
        if (Objects.isNull(marketObjectStatusStatus)) {
            networkActionCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCOMPLETE_DATA, String.format("Missing marketObjectStatus on injection setpoint elementary action %s", elementaryActionId)));
            return false;
        } else if (marketObjectStatusStatus.equals(CimConstants.MarketObjectStatus.ABSOLUTE.getStatus())) {
            if (Objects.isNull(remedialActionRegisteredResource.getResourceCapacityDefaultCapacity())) {
                networkActionCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCOMPLETE_DATA, String.format("Injection setpoint elementary action %s with ABSOLUTE marketObjectStatus should have a defaultCapacity", elementaryActionId)));
                return false;
            }
            if (Objects.isNull(remedialActionRegisteredResource.getResourceCapacityUnitSymbol())) {
                networkActionCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCOMPLETE_DATA, String.format("Injection setpoint elementary action %s with ABSOLUTE marketObjectStatus should have a unitSymbol", elementaryActionId)));
                return false;
            }
            if (!remedialActionRegisteredResource.getResourceCapacityUnitSymbol().equals(MEGAWATT_UNIT_SYMBOL)) {
                networkActionCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong unit symbol for injection setpoint elementary action %s with ABSOLUTE marketObjectStatus : %s", elementaryActionId, remedialActionRegisteredResource.getResourceCapacityUnitSymbol())));
                return false;
            }
            setpoint = remedialActionRegisteredResource.getResourceCapacityDefaultCapacity().intValue();
        } else if (marketObjectStatusStatus.equals(CimConstants.MarketObjectStatus.STOP.getStatus())) {
            if (Objects.nonNull(remedialActionRegisteredResource.getResourceCapacityDefaultCapacity())) {
                networkActionCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("Injection setpoint elementary action %s with STOP marketObjectStatus shouldn't have a defaultCapacity", elementaryActionId)));
                return false;
            }
            if (Objects.nonNull(remedialActionRegisteredResource.getResourceCapacityUnitSymbol())) {
                networkActionCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("Injection setpoint elementary action %s with STOP marketObjectStatus shouldn't have a unitSymbol", elementaryActionId)));
                return false;
            }
            setpoint = 0;
        } else {
            networkActionCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong marketObjectStatusStatus: %s on injection setpoint elementary action %s", marketObjectStatusStatus, elementaryActionId)));
            return false;
        }

        String networkElementId = remedialActionRegisteredResource.getMRID().getValue();
        if (Objects.isNull(network.getGenerator(networkElementId)) && Objects.isNull(network.getLoad(networkElementId))) {
            networkActionCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, String.format("%s is nor a generator nor a load on injection setpoint elementary action %s", networkElementId, elementaryActionId)));
            return false;
        }

        networkActionAdder.newInjectionSetPoint()
                .withNetworkElement(networkElementId)
                .withSetpoint(setpoint)
                .add();

        return true;
    }

    private boolean addTopologicalElementaryAction(String createdRemedialActionId, String elementaryActionId, RemedialActionRegisteredResource remedialActionRegisteredResource, NetworkActionAdder networkActionAdder) {
        if (Objects.nonNull(remedialActionRegisteredResource.getResourceCapacityMinimumCapacity()) || Objects.nonNull(remedialActionRegisteredResource.getResourceCapacityMaximumCapacity()) || Objects.nonNull(remedialActionRegisteredResource.getResourceCapacityDefaultCapacity())) {
            networkActionCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("Topological elementary action %s should not have any resource capacity defined", elementaryActionId)));
            return false;
        }
        // Market object status
        String marketObjectStatusStatus = remedialActionRegisteredResource.getMarketObjectStatusStatus();
        ActionType actionType;
        if (Objects.isNull(marketObjectStatusStatus)) {
            networkActionCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCOMPLETE_DATA, String.format("Missing marketObjectStatus on topological elementary action %s", elementaryActionId)));
            return false;
        } else if (marketObjectStatusStatus.equals(CimConstants.MarketObjectStatus.OPEN.getStatus())) {
            actionType = ActionType.OPEN;
        } else if (marketObjectStatusStatus.equals(CimConstants.MarketObjectStatus.CLOSE.getStatus())) {
            actionType = ActionType.CLOSE;
        } else {
            networkActionCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA,  String.format("Wrong marketObjectStatusStatus: %s on topological elementary action %s", marketObjectStatusStatus, elementaryActionId)));
            return false;
        }

        String networkElementId = remedialActionRegisteredResource.getMRID().getValue();
        Identifiable<?> element = network.getIdentifiable(networkElementId);
        if (Objects.isNull(element)) {
            // Check that network element is not half a tie line
            CgmesBranchHelper branchHelper = new CgmesBranchHelper(networkElementId, network);
            if (branchHelper.getBranch() == null) {
                networkActionCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, String.format("%s not in network on topological elementary action %s", networkElementId, elementaryActionId)));
                return false;
            }
            networkElementId = branchHelper.getIdInNetwork();
            element = branchHelper.getBranch();
        }
        if (!(element instanceof Branch) &&  !(element instanceof Switch)) {
            networkActionCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("%s is nor a branch nor a switch on elementary action %s", networkElementId, elementaryActionId)));
            return false;
        }

        networkActionAdder.newTopologicalAction()
                .withNetworkElement(networkElementId)
                .withActionType(actionType)
                .add();

        return true;
    }
}


