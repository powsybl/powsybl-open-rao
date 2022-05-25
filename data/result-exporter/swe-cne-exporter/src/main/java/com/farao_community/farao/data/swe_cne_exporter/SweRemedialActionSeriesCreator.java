/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.swe_cne_exporter;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.cne_exporter_commons.CneHelper;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.remedial_action.PstRangeActionSeriesCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.remedial_action.RemedialActionSeriesCreationContext;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.swe_cne_exporter.xsd.*;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;

import java.math.BigDecimal;
import java.util.*;

import static com.farao_community.farao.data.cne_exporter_commons.CneConstants.*;

/**
 * Generates RemedialActionSeries for SWE CNE format
 *
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class SweRemedialActionSeriesCreator {
    private final CneHelper cneHelper;

    public SweRemedialActionSeriesCreator(CneHelper cneHelper) {
        this.cneHelper = cneHelper;
    }

    public List<RemedialActionSeries> generateRaSeries(Contingency contingency) {
        List<RemedialActionSeries> remedialActionSeriesList = new ArrayList<>();
        CimCracCreationContext context = cneHelper.getCimCracCreationContext();
        Crac crac = cneHelper.getCrac();
        if (Objects.isNull(contingency)) {
            //PREVENTIVE
            context.getRemedialActionSeriesCreationContexts().forEach(
                raSeriesCreationContext -> remedialActionSeriesList.add(generateRaSeries(crac.getPreventiveState(), raSeriesCreationContext))
            );
        } else {
            //CURATIVE && AUTO
            context.getRemedialActionSeriesCreationContexts().forEach(
                raSeriesCreationContext -> remedialActionSeriesList.addAll(generateRaSeries(crac.getState(contingency, Instant.AUTO), raSeriesCreationContext))
            );
            context.getRemedialActionSeriesCreationContexts().forEach(
                raSeriesCreationContext -> remedialActionSeriesList.addAll(generateRaSeries(crac.getState(contingency, Instant.AUTO), raSeriesCreationContext))
            );
        }
        return remedialActionSeriesList;
    }

    private RemedialActionSeries generateRaSeries(State state, RemedialActionSeriesCreationContext context) {
        RaoResult raoResult = cneHelper.getRaoResult();
        Crac crac = cneHelper.getCrac();
        RemedialAction<?> usedRa = context.getCreatedIds().stream().map(crac::getRangeAction)
            .filter(ra -> raoResult.isActivatedDuringState(state, ra)).findFirst().orElse(null);
        if (Objects.nonNull(usedRa)) {
            if (usedRa instanceof NetworkAction) {
                return generateNetworkRaSeries((NetworkAction) usedRa, state);
            } else if (usedRa instanceof PstRangeAction) {
                return generatePstRaSeries((PstRangeAction) usedRa, state, context);
            } else if (usedRa instanceof HvdcRangeAction) {
                // In case of an HVDC, the native crac has one series per direction, we select the one that corresponds to the sign of the setpoint
                if (context.isInverted() == (raoResult.getOptimizedSetPointOnState(state, (HvdcRangeAction) usedRa) < 0)) {
                    return generateHvdcRaSeries((HvdcRangeAction) usedRa, state, context);
                } else {
                    return null;
                }
            } else {
                throw new NotImplementedException(String.format("Range action of type %s not supported yet.", usedRa.getClass().getName()));
            }
        }
    }

    public List<RemedialActionSeries> generateRaSeriesReference(Contingency contingency) {
        if (Objects.isNull(contingency)) {
            //PREVENTIVE
        } else {
            //AUTO and CURATIVE
        }
        return new ArrayList<>();
    }

    private RemedialActionSeries generateNetworkRaSeries(NetworkAction networkAction, State state) {
        RemedialActionSeries remedialActionSeries = new RemedialActionSeries();
        remedialActionSeries.setMRID(networkAction.getId());
        remedialActionSeries.setName(networkAction.getName());
        remedialActionSeries.setApplicationModeMarketObjectStatusStatus(getApplicationModeMarketObjectStatusStatus(state));
        return remedialActionSeries;
    }

    private RemedialActionSeries generatePstRaSeries(PstRangeAction rangeAction, State state, RemedialActionSeriesCreationContext context) {
        RemedialActionSeries remedialActionSeries = new RemedialActionSeries();
        remedialActionSeries.setMRID(rangeAction.getId() + "@" + cneHelper.getRaoResult().getOptimizedTapOnState(state, rangeAction) + "@");
        remedialActionSeries.setName(rangeAction.getName());
        remedialActionSeries.setApplicationModeMarketObjectStatusStatus(getApplicationModeMarketObjectStatusStatus(state));
        remedialActionSeries.getRegisteredResource().add(generateRegisteredResource(rangeAction, state));
        return remedialActionSeries;
    }

    private RemedialActionSeries generateHvdcRaSeries(HvdcRangeAction rangeAction, State state, RemedialActionSeriesCreationContext context) {
        RemedialActionSeries remedialActionSeries = new RemedialActionSeries();
        double absoluteSetpoint = Math.abs(cneHelper.getRaoResult().getOptimizedSetPointOnState(state, rangeAction));
        String nativeId = context.getNativeId();
        remedialActionSeries.setMRID(nativeId + "@" + absoluteSetpoint + "@");
        remedialActionSeries.setName(nativeId + "@" + absoluteSetpoint + "@");
        remedialActionSeries.setApplicationModeMarketObjectStatusStatus(getApplicationModeMarketObjectStatusStatus(state));
        return remedialActionSeries;
    }

    private String getApplicationModeMarketObjectStatusStatus(State state) {
        if (state.isPreventive()) {
            return PREVENTIVE_MARKET_OBJECT_STATUS;
        } else if (state.getInstant().equals(Instant.AUTO)) {
            return AUTO_MARKET_OBJECT_STATUS;
        } else if (state.getInstant().equals(Instant.CURATIVE)) {
            return CURATIVE_MARKET_OBJECT_STATUS;
        } else {
            throw new FaraoException(String.format("Unexpected instant for remedial action application : %s", state.getInstant().toString()));
        }
    }

    private RemedialActionRegisteredResource generateRegisteredResource(PstRangeAction pstRangeAction, State state) {
        PstRangeActionSeriesCreationContext pstContext = (PstRangeActionSeriesCreationContext) cneHelper
            .getCimCracCreationContext().getRemedialActionSeriesCreationContexts().stream()
            .filter(context -> context.getCreatedIds().contains(pstRangeAction.getId()))
            .findFirst().orElseThrow(() -> new FaraoException(String.format("Unable to find PST %s in crac creation context", pstRangeAction.getId())));

        RemedialActionRegisteredResource registeredResource = new RemedialActionRegisteredResource();
        registeredResource.setMRID(SweCneUtil.createResourceIDString(A02_CODING_SCHEME, pstContext.getNetworkElementNativeMrid()));
        registeredResource.setName(pstContext.getNetworkElementNativeName());
        registeredResource.setPSRTypePsrType(PST_RANGE_PSR_TYPE);
        registeredResource.setMarketObjectStatusStatus(ABSOLUTE_MARKET_OBJECT_STATUS);
        registeredResource.setResourceCapacityDefaultCapacity(new BigDecimal(cneHelper.getRaoResult().getOptimizedTapOnState(state, pstRangeAction)));
        registeredResource.setResourceCapacityUnitSymbol(WITHOUT_UNIT_SYMBOL);

        return registeredResource;
    }

}
