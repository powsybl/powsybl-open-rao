/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.remedial_action;

import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.range.RangeType;
import com.farao_community.farao.data.crac_api.range_action.PstRangeActionAdder;
import com.farao_community.farao.data.crac_creation.creator.api.CracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.RangeActionGroup;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimConstants;
import com.farao_community.farao.data.crac_creation.creator.cim.parameters.CimCracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.cim.parameters.RangeActionSpeed;
import com.farao_community.farao.data.crac_creation.creator.cim.xsd.RemedialActionRegisteredResource;
import com.farao_community.farao.data.crac_creation.util.PstHelper;
import com.farao_community.farao.data.crac_creation.util.iidm.IidmPstHelper;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.tuple.Pair;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @author Godelaine de Montmorillon <godelaine.demontmorillon at rte-france.com>
 */
public class PstRangeActionCreator {
    private final Crac crac;
    private final Network network;
    private final String createdRemedialActionId;
    private final String createdRemedialActionName;
    private final String applicationModeMarketObjectStatus;
    private Set<RemedialActionSeriesCreationContext> pstRangeActionCreationContexts;
    private final RemedialActionRegisteredResource pstRegisteredResource;
    private final List<Contingency> contingencies;
    private final List<String> invalidContingencies;

    public PstRangeActionCreator(Crac crac, Network network, String createdRemedialActionId, String createdRemedialActionName, String applicationModeMarketObjectStatus, RemedialActionRegisteredResource pstRegisteredResource, List<Contingency> contingencies, List<String> invalidContingencies) {
        this.crac = crac;
        this.network = network;
        this.createdRemedialActionId = createdRemedialActionId;
        this.createdRemedialActionName = createdRemedialActionName;
        this.applicationModeMarketObjectStatus = applicationModeMarketObjectStatus;
        this.pstRegisteredResource = pstRegisteredResource;
        this.contingencies = contingencies;
        this.invalidContingencies = invalidContingencies;
    }

    public Set<RemedialActionSeriesCreationContext> addPstRangeAction(CimCracCreationParameters cimCracCreationParameters, CracCreationContext cracCreationContext) {
        this.pstRangeActionCreationContexts = new HashSet<>();
        // --- Market Object status: define RangeType
        String marketObjectStatusStatus = pstRegisteredResource.getMarketObjectStatusStatus();
        if (Objects.isNull(marketObjectStatusStatus)) {
            pstRangeActionCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCOMPLETE_DATA, "Missing marketObjectStatus"));
            return pstRangeActionCreationContexts;
        }
        Pair<Boolean, RangeType> rangeTypeStatus = defineRangeType(marketObjectStatusStatus);
        if (!rangeTypeStatus.getLeft()) {
            return pstRangeActionCreationContexts;
        }
        RangeType rangeType = rangeTypeStatus.getRight();

        // Add remedial action
        // --- For now, do not set operator.
        // --- For now, do not set group id.
        String networkElementId = pstRegisteredResource.getMRID().getValue();

        IidmPstHelper pstHelper = new IidmPstHelper(networkElementId, network);
        if (!pstHelper.isValid()) {
            pstRangeActionCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, String.format("%s", pstHelper.getInvalidReason())));
            return pstRangeActionCreationContexts;
        }

        PstRangeActionAdder pstRangeActionAdder = crac.newPstRangeAction()
                .withId(createdRemedialActionId)
                .withName(createdRemedialActionName)
                .withNetworkElement(pstHelper.getIdInNetwork())
                .withInitialTap(pstHelper.getInitialTap())
                .withTapToAngleConversionMap(pstHelper.getTapToAngleConversionMap());

        // ---- add groupId if present
        if (cimCracCreationParameters != null && cimCracCreationParameters.getRangeActionGroups() != null) {
            String groupId = null;
            for (RangeActionGroup rangeActionGroup : cimCracCreationParameters.getRangeActionGroups()) {
                for (String raGroupId : rangeActionGroup.getRangeActionsIds()) {
                    if (Objects.isNull(raGroupId)) {
                        cracCreationContext.getCreationReport().warn(String.format("RangeActionGroup %s contains a range action group containing a null value.", rangeActionGroup));
                        continue;
                    }
                    if (raGroupId.equals(createdRemedialActionId)) {
                        if (groupId != null) {
                            cracCreationContext.getCreationReport().warn(String.format("GroupId already defined to %s for PST %s, group %s is ignored (only in PST %s).", groupId, createdRemedialActionId, rangeActionGroup, createdRemedialActionId));
                        } else {
                            groupId = rangeActionGroup.toString();
                            pstRangeActionAdder.withGroupId(groupId);
                        }
                    }
                }
            }
        }
        // -- add speed if present
        if (cimCracCreationParameters != null && cimCracCreationParameters.getRangeActionSpeedSet() != null) {
            for (RangeActionSpeed rangeActionSpeed : cimCracCreationParameters.getRangeActionSpeedSet()) {
                if (rangeActionSpeed.getRangeActionId().equals(createdRemedialActionId)) {
                    pstRangeActionAdder.withSpeed(rangeActionSpeed.getSpeed());
                }
            }
        }

        // --- Resource capacity
        if (!defineTapRange(pstRangeActionAdder, pstHelper, rangeType)) {
            return pstRangeActionCreationContexts;
        }

        if (!RemedialActionSeriesCreator.addUsageRules(createdRemedialActionId, applicationModeMarketObjectStatus, pstRangeActionAdder, contingencies, invalidContingencies, pstRangeActionCreationContexts)) {
            return pstRangeActionCreationContexts;
        }

        RemedialActionSeriesCreator.importWithContingencies(createdRemedialActionId, invalidContingencies, pstRangeActionCreationContexts);
        pstRangeActionAdder.add();
        return pstRangeActionCreationContexts;
    }

    private Pair<Boolean, RangeType> defineRangeType(String marketObjectStatusStatus) {
        if (marketObjectStatusStatus.equals(CimConstants.MarketObjectStatus.ABSOLUTE.getStatus())) {
            return Pair.of(true, RangeType.ABSOLUTE);
        } else if (marketObjectStatusStatus.equals(CimConstants.MarketObjectStatus.RELATIVE_TO_INITIAL_NETWORK.getStatus())) {
            return Pair.of(true, RangeType.RELATIVE_TO_INITIAL_NETWORK);
        } else if (marketObjectStatusStatus.equals(CimConstants.MarketObjectStatus.RELATIVE_TO_PREVIOUS_INSTANT1.getStatus()) || marketObjectStatusStatus.equals(CimConstants.MarketObjectStatus.RELATIVE_TO_PREVIOUS_INSTANT2.getStatus())) {
            return Pair.of(true, RangeType.RELATIVE_TO_PREVIOUS_INSTANT);
        } else if (marketObjectStatusStatus.equals(CimConstants.MarketObjectStatus.OPEN.getStatus()) || marketObjectStatusStatus.equals(CimConstants.MarketObjectStatus.CLOSE.getStatus())) {
            pstRangeActionCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong marketObjectStatusStatus: %s, PST can no longer be opened/closed (deprecated)", marketObjectStatusStatus)));
            return Pair.of(false, RangeType.ABSOLUTE);
        } else {
            pstRangeActionCreationContexts.add(RemedialActionSeriesCreationContext.notImported(createdRemedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong marketObjectStatusStatus: %s", marketObjectStatusStatus)));
            return Pair.of(false, RangeType.ABSOLUTE);
        }
    }

    private boolean defineTapRange(PstRangeActionAdder pstRangeActionAdder, IidmPstHelper pstHelper, RangeType rangeType) {
        String unitSymbol = pstRegisteredResource.getResourceCapacityUnitSymbol();
        BigDecimal resourceCapacityMinimumCapacity = pstRegisteredResource.getResourceCapacityMinimumCapacity();
        BigDecimal resourceCapacityMaximumCapacity = pstRegisteredResource.getResourceCapacityMaximumCapacity();
        // Min and Max defined
        if (Objects.nonNull(resourceCapacityMinimumCapacity) && Objects.nonNull(resourceCapacityMaximumCapacity)) {
            int minCapacity = resourceCapacityMinimumCapacity.intValue();
            int maxCapacity = resourceCapacityMaximumCapacity.intValue();
            if (!RemedialActionSeriesCreator.checkPstUnit(createdRemedialActionId, unitSymbol, pstRangeActionCreationContexts)) {
                return false;
            }
            addTapRangeWithMinAndMaxTap(pstRangeActionAdder, pstHelper, minCapacity, maxCapacity, rangeType);
        } else if (Objects.nonNull(resourceCapacityMaximumCapacity)) {
            int maxCapacity = resourceCapacityMaximumCapacity.intValue();
            if (!RemedialActionSeriesCreator.checkPstUnit(createdRemedialActionId, unitSymbol, pstRangeActionCreationContexts)) {
                return false;
            }
            addTapRangeWithMaxTap(pstRangeActionAdder, pstHelper, maxCapacity, rangeType);
        } else if (Objects.nonNull(resourceCapacityMinimumCapacity)) {
            int minCapacity = resourceCapacityMinimumCapacity.intValue();
            if (!RemedialActionSeriesCreator.checkPstUnit(createdRemedialActionId, unitSymbol, pstRangeActionCreationContexts)) {
                return false;
            }
            addTapRangeWithMinTap(pstRangeActionAdder, pstHelper, minCapacity, rangeType);
        }
        return true;
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
}
