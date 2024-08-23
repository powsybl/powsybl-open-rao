/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.cim.craccreator;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.cnec.AngleCnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.range.RangeType;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeActionAdder;
import com.powsybl.openrao.data.cracio.commons.api.ImportStatus;
import com.powsybl.openrao.data.cracapi.parameters.RangeActionGroup;
import com.powsybl.openrao.data.cracio.commons.OpenRaoImportException;
import com.powsybl.openrao.data.cracio.cim.parameters.CimCracCreationParameters;
import com.powsybl.openrao.data.cracio.cim.xsd.RemedialActionRegisteredResource;
import com.powsybl.openrao.data.cracio.commons.PstHelper;
import com.powsybl.openrao.data.cracio.commons.iidm.IidmPstHelper;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class PstRangeActionCreator {
    private final Crac crac;
    private final Network network;
    private final String createdRemedialActionId;
    private final String createdRemedialActionName;
    private final String applicationModeMarketObjectStatus;
    private RemedialActionSeriesCreationContext pstRangeActionCreationContext;
    private final RemedialActionRegisteredResource pstRegisteredResource;
    private final List<Contingency> contingencies;
    private final List<String> invalidContingencies;
    private final Set<FlowCnec> flowCnecs;
    private final AngleCnec angleCnec;
    private PstRangeActionAdder pstRangeActionAdder;
    private final Country sharedDomain;

    public PstRangeActionCreator(Crac crac, Network network, String createdRemedialActionId, String createdRemedialActionName, String applicationModeMarketObjectStatus, RemedialActionRegisteredResource pstRegisteredResource, List<Contingency> contingencies, List<String> invalidContingencies, Set<FlowCnec> flowCnecs, AngleCnec angleCnec, Country sharedDomain) {
        this.crac = crac;
        this.network = network;
        this.createdRemedialActionId = createdRemedialActionId;
        this.createdRemedialActionName = createdRemedialActionName;
        this.applicationModeMarketObjectStatus = applicationModeMarketObjectStatus;
        this.pstRegisteredResource = pstRegisteredResource;
        this.contingencies = contingencies;
        this.invalidContingencies = invalidContingencies;
        this.flowCnecs = flowCnecs;
        this.angleCnec = angleCnec;
        this.sharedDomain = sharedDomain;
    }

    public void createPstRangeActionAdder(CimCracCreationParameters cimCracCreationParameters) {
        // --- Market Object status: define RangeType
        String marketObjectStatusStatus = pstRegisteredResource.getMarketObjectStatusStatus();
        if (Objects.isNull(marketObjectStatusStatus)) {
            this.pstRangeActionCreationContext = PstRangeActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCOMPLETE_DATA, "Missing marketObjectStatus");
            return;
        }
        try {
            RangeType rangeType = defineRangeType(marketObjectStatusStatus);

            // Add remedial action
            // --- For now, do not set operator.
            // --- For now, do not set group id.
            String networkElementId = pstRegisteredResource.getMRID().getValue();

            IidmPstHelper pstHelper = new IidmPstHelper(networkElementId, network);
            if (!pstHelper.isValid()) {
                this.pstRangeActionCreationContext = PstRangeActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, String.format("%s", pstHelper.getInvalidReason()));
                return;
            }

            this.pstRangeActionAdder = crac.newPstRangeAction()
                .withId(createdRemedialActionId)
                .withName(createdRemedialActionName)
                .withOperator(CimConstants.readOperator(createdRemedialActionId))
                .withNetworkElement(pstHelper.getIdInNetwork())
                .withInitialTap(pstHelper.getInitialTap())
                .withTapToAngleConversionMap(pstHelper.getTapToAngleConversionMap());

            // ---- add groupId if present
            if (cimCracCreationParameters != null && cimCracCreationParameters.getRangeActionGroups() != null) {
                List<String> raGroups = cimCracCreationParameters.getRangeActionGroups().stream()
                    .filter(rangeActionGroup -> rangeActionGroup.getRangeActionsIds().contains(createdRemedialActionId))
                    .map(RangeActionGroup::toString)
                    .toList();
                if (raGroups.size() > 1) {
                    this.pstRangeActionCreationContext = RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("Multiple (%s) groups defined for range action %s", raGroups.size(), createdRemedialActionId));
                    return;
                }
                if (!raGroups.isEmpty()) {
                    pstRangeActionAdder.withGroupId(raGroups.get(0));
                }
            }

            // -- add speed if present
            if (cimCracCreationParameters != null && cimCracCreationParameters.getRangeActionSpeedSet() != null) {
                cimCracCreationParameters.getRangeActionSpeedSet().stream()
                    .filter(rangeActionSpeed -> rangeActionSpeed.getRangeActionId().equals(createdRemedialActionId))
                    .forEach(rangeActionSpeed -> pstRangeActionAdder.withSpeed(rangeActionSpeed.getSpeed()));
            }

            // --- Resource capacity
            defineTapRange(pstRangeActionAdder, pstHelper, rangeType);
            RemedialActionSeriesCreator.addUsageRules(crac, applicationModeMarketObjectStatus, pstRangeActionAdder, contingencies, invalidContingencies, flowCnecs, angleCnec, sharedDomain);

            this.pstRangeActionCreationContext = RemedialActionSeriesCreator.importPstRaWithContingencies(createdRemedialActionId, pstRegisteredResource.getMRID().getValue(), pstRegisteredResource.getName(), invalidContingencies);
        } catch (OpenRaoImportException e) {
            this.pstRangeActionCreationContext = RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, e.getImportStatus(), e.getMessage());
        }
    }

    private RangeType defineRangeType(String marketObjectStatusStatus) {
        if (marketObjectStatusStatus.equals(CimConstants.MarketObjectStatus.ABSOLUTE.getStatus())) {
            return RangeType.ABSOLUTE;
        } else if (marketObjectStatusStatus.equals(CimConstants.MarketObjectStatus.RELATIVE_TO_INITIAL_NETWORK.getStatus())) {
            return RangeType.RELATIVE_TO_INITIAL_NETWORK;
        } else if (marketObjectStatusStatus.equals(CimConstants.MarketObjectStatus.RELATIVE_TO_PREVIOUS_INSTANT1.getStatus()) || marketObjectStatusStatus.equals(CimConstants.MarketObjectStatus.RELATIVE_TO_PREVIOUS_INSTANT2.getStatus())) {
            return RangeType.RELATIVE_TO_PREVIOUS_INSTANT;
        } else if (marketObjectStatusStatus.equals(CimConstants.MarketObjectStatus.OPEN.getStatus()) || marketObjectStatusStatus.equals(CimConstants.MarketObjectStatus.CLOSE.getStatus())) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong marketObjectStatusStatus: %s, PST can no longer be opened/closed (deprecated)", marketObjectStatusStatus));
        } else {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong marketObjectStatusStatus: %s", marketObjectStatusStatus));
        }
    }

    private void defineTapRange(PstRangeActionAdder pstRangeActionAdder, IidmPstHelper pstHelper, RangeType rangeType) {
        String unitSymbol = pstRegisteredResource.getResourceCapacityUnitSymbol();
        BigDecimal resourceCapacityMinimumCapacity = pstRegisteredResource.getResourceCapacityMinimumCapacity();
        BigDecimal resourceCapacityMaximumCapacity = pstRegisteredResource.getResourceCapacityMaximumCapacity();
        // Min and Max defined
        if (Objects.nonNull(resourceCapacityMinimumCapacity) && Objects.nonNull(resourceCapacityMaximumCapacity)) {
            int minCapacity = resourceCapacityMinimumCapacity.intValue();
            int maxCapacity = resourceCapacityMaximumCapacity.intValue();
            RemedialActionSeriesCreator.checkPstUnit(unitSymbol);
            addTapRangeWithMinAndMaxTap(pstRangeActionAdder, pstHelper, minCapacity, maxCapacity, rangeType);
        } else if (Objects.nonNull(resourceCapacityMaximumCapacity)) {
            int maxCapacity = resourceCapacityMaximumCapacity.intValue();
            RemedialActionSeriesCreator.checkPstUnit(unitSymbol);
            addTapRangeWithMaxTap(pstRangeActionAdder, pstHelper, maxCapacity, rangeType);
        } else if (Objects.nonNull(resourceCapacityMinimumCapacity)) {
            int minCapacity = resourceCapacityMinimumCapacity.intValue();
            RemedialActionSeriesCreator.checkPstUnit(unitSymbol);
            addTapRangeWithMinTap(pstRangeActionAdder, pstHelper, minCapacity, rangeType);
        }
    }

    private void addTapRangeWithMinTap(PstRangeActionAdder pstRangeActionAdder, IidmPstHelper pstHelper, int minCapacity, RangeType rangeType) {
        int minTap = minCapacity;
        if (rangeType.equals(RangeType.ABSOLUTE)) {
            minTap = pstHelper.normalizeTap(minTap, PstHelper.TapConvention.STARTS_AT_ONE);
        }
        pstRangeActionAdder.newTapRange()
            .withMinTap(minTap)
            .withRangeType(rangeType)
            .add();
    }

    private void addTapRangeWithMaxTap(PstRangeActionAdder pstRangeActionAdder, IidmPstHelper pstHelper, int maxCapacity, RangeType rangeType) {
        int maxTap = maxCapacity;
        if (rangeType.equals(RangeType.ABSOLUTE)) {
            maxTap = pstHelper.normalizeTap(maxTap, PstHelper.TapConvention.STARTS_AT_ONE);
        }
        pstRangeActionAdder.newTapRange()
            .withMaxTap(maxTap)
            .withRangeType(rangeType)
            .add();
    }

    private void addTapRangeWithMinAndMaxTap(PstRangeActionAdder pstRangeActionAdder, IidmPstHelper pstHelper, int minCapacity, int maxCapacity, RangeType rangeType) {
        int minTap = minCapacity;
        int maxTap = maxCapacity;
        if (rangeType.equals(RangeType.ABSOLUTE)) {
            minTap = pstHelper.normalizeTap(minTap, PstHelper.TapConvention.STARTS_AT_ONE);
            maxTap = pstHelper.normalizeTap(maxTap, PstHelper.TapConvention.STARTS_AT_ONE);
        }
        pstRangeActionAdder.newTapRange()
            .withMinTap(minTap)
            .withMaxTap(maxTap)
            .withRangeType(rangeType)
            .add();
    }

    public void addPstRangeAction() {
        if (this.pstRangeActionCreationContext.isImported() && this.pstRangeActionAdder != null) {
            try {
                this.pstRangeActionAdder.add();
            } catch (OpenRaoException e) {
                this.pstRangeActionCreationContext = RemedialActionSeriesCreationContext.notImported(this.pstRangeActionCreationContext.getNativeObjectId(), ImportStatus.INCONSISTENCY_IN_DATA, e.getMessage());
            }
        }
    }

    public RemedialActionSeriesCreationContext getPstRangeActionCreationContext() {
        return pstRangeActionCreationContext;
    }

    public PstRangeActionAdder getPstRangeActionAdder() {
        return pstRangeActionAdder;
    }
}
