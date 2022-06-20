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
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.remedial_action.PstRangeActionSeriesCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.remedial_action.RemedialActionSeriesCreationContext;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.swe_cne_exporter.xsd.*;
import org.apache.commons.lang3.NotImplementedException;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static com.farao_community.farao.data.cne_exporter_commons.CneConstants.*;

/**
 * Generates RemedialActionSeries for SWE CNE format
 *
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class SweRemedialActionSeriesCreator {
    private final CneHelper cneHelper;
    private final CimCracCreationContext cracCreationContext;

    public SweRemedialActionSeriesCreator(CneHelper cneHelper, CimCracCreationContext cracCreationContext) {
        this.cneHelper = cneHelper;
        this.cracCreationContext = cracCreationContext;
    }

    public List<RemedialActionSeries> generateRaSeries(Contingency contingency) {
        List<RemedialActionSeries> remedialActionSeriesList = new ArrayList<>();
        CimCracCreationContext context = cracCreationContext;
        Crac crac = cneHelper.getCrac();
        if (Objects.isNull(contingency)) {
            //PREVENTIVE
            context.getRemedialActionSeriesCreationContexts().stream().sorted(Comparator.comparing(RemedialActionSeriesCreationContext::getNativeId)).forEach(
                raSeriesCreationContext -> {
                    RemedialActionSeries raSeries = generateRaSeries(crac.getPreventiveState(), raSeriesCreationContext, false);
                    if (Objects.nonNull(raSeries)) {
                        remedialActionSeriesList.add(raSeries);
                    }
                }
            );
        } else {
            //CURATIVE && AUTO
            context.getRemedialActionSeriesCreationContexts().stream().sorted(Comparator.comparing(RemedialActionSeriesCreationContext::getNativeId)).forEach(
                raSeriesCreationContext -> {
                    RemedialActionSeries raSeries = generateRaSeries(crac.getState(contingency, Instant.AUTO), raSeriesCreationContext, false);
                    if (Objects.nonNull(raSeries)) {
                        remedialActionSeriesList.add(raSeries);
                    }
                }
            );
            context.getRemedialActionSeriesCreationContexts().stream().sorted(Comparator.comparing(RemedialActionSeriesCreationContext::getNativeId)).forEach(
                raSeriesCreationContext -> {
                    RemedialActionSeries raSeries = generateRaSeries(crac.getState(contingency, Instant.CURATIVE), raSeriesCreationContext, false);
                    if (Objects.nonNull(raSeries)) {
                        remedialActionSeriesList.add(raSeries);
                    }
                }
            );
        }
        return remedialActionSeriesList;
    }

    public List<RemedialActionSeries> generateRaSeriesReference(Contingency contingency) {
        List<RemedialActionSeries> remedialActionSeriesList = new ArrayList<>();
        Crac crac = cneHelper.getCrac();
        if (Objects.isNull(contingency)) {
            //PREVENTIVE
            cracCreationContext.getRemedialActionSeriesCreationContexts().stream().sorted(Comparator.comparing(RemedialActionSeriesCreationContext::getNativeId)).forEach(
                raSeriesCreationContext -> {
                    RemedialActionSeries raSeries = generateRaSeries(crac.getPreventiveState(), raSeriesCreationContext, true);
                    if (Objects.nonNull(raSeries)) {
                        remedialActionSeriesList.add(raSeries);
                    }
                }
            );
        } else {
            //for the B57, in a contingency case, we want all remedial actions with an effect on the cnecs, so PREVENTIVE, CURATIVE && AUTO
            cracCreationContext.getRemedialActionSeriesCreationContexts().stream().sorted(Comparator.comparing(RemedialActionSeriesCreationContext::getNativeId)).forEach(
                raSeriesCreationContext -> {
                    RemedialActionSeries raSeries = generateRaSeries(crac.getPreventiveState(), raSeriesCreationContext, true);
                    if (Objects.nonNull(raSeries)) {
                        remedialActionSeriesList.add(raSeries);
                    }
                }
            );
            cracCreationContext.getRemedialActionSeriesCreationContexts().stream().sorted(Comparator.comparing(RemedialActionSeriesCreationContext::getNativeId)).forEach(
                raSeriesCreationContext -> {
                    RemedialActionSeries raSeries = generateRaSeries(crac.getState(contingency, Instant.AUTO), raSeriesCreationContext, true);
                    if (Objects.nonNull(raSeries)) {
                        remedialActionSeriesList.add(raSeries);
                    }
                }
            );
            cracCreationContext.getRemedialActionSeriesCreationContexts().stream().sorted(Comparator.comparing(RemedialActionSeriesCreationContext::getNativeId)).forEach(
                raSeriesCreationContext -> {
                    RemedialActionSeries raSeries = generateRaSeries(crac.getState(contingency, Instant.CURATIVE), raSeriesCreationContext, true);
                    if (Objects.nonNull(raSeries)) {
                        remedialActionSeriesList.add(raSeries);
                    }
                }
            );
        }
        return remedialActionSeriesList;
    }

    private RemedialActionSeries generateRaSeries(State state, RemedialActionSeriesCreationContext context, boolean onlyReference) {
        RaoResult raoResult = cneHelper.getRaoResult();
        Crac crac = cneHelper.getCrac();
        List<RemedialAction<?>> usedRas = context.getCreatedIds().stream().sorted()
            .map(crac::getRemedialAction)
            .filter(ra -> raoResult.isActivatedDuringState(state, ra)).collect(Collectors.toList());
        for (RemedialAction<?> usedRa : usedRas) {
            if (usedRa instanceof NetworkAction) {
                return generateNetworkRaSeries((NetworkAction) usedRa, state);
            } else if (usedRa instanceof PstRangeAction) {
                return generatePstRaSeries((PstRangeAction) usedRa, state, context, onlyReference);
            } else if (usedRa instanceof HvdcRangeAction) {
                // In case of an HVDC, the native crac has one series per direction, we select the one that corresponds to the sign of the setpoint
                if (context.isInverted() == (raoResult.getOptimizedSetPointOnState(state, (HvdcRangeAction) usedRa) < 0)) {
                    return generateHvdcRaSeries((HvdcRangeAction) usedRa, state, context);
                }
            } else {
                throw new NotImplementedException(String.format("Range action of type %s not supported yet.", usedRa.getClass().getName()));
            }
        }
        return null;
    }

    private RemedialActionSeries generateNetworkRaSeries(NetworkAction networkAction, State state) {
        RemedialActionSeries remedialActionSeries = new RemedialActionSeries();
        remedialActionSeries.setMRID(networkAction.getId());
        remedialActionSeries.setName(networkAction.getName());
        remedialActionSeries.setApplicationModeMarketObjectStatusStatus(getApplicationModeMarketObjectStatusStatus(state));
        return remedialActionSeries;
    }

    private RemedialActionSeries generatePstRaSeries(PstRangeAction rangeAction, State state, RemedialActionSeriesCreationContext context, boolean onlyReference) {
        RemedialActionSeries remedialActionSeries = new RemedialActionSeries();
        remedialActionSeries.setMRID(rangeAction.getId() + "@" + cneHelper.getRaoResult().getOptimizedTapOnState(state, rangeAction) + "@");
        remedialActionSeries.setName(rangeAction.getName());
        remedialActionSeries.setApplicationModeMarketObjectStatusStatus(getApplicationModeMarketObjectStatusStatus(state));
        if (!onlyReference) {
            //we only want to write the registeredResource for B56s
            remedialActionSeries.getRegisteredResource().add(generateRegisteredResource(rangeAction, state, context));
        }
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

    private RemedialActionRegisteredResource generateRegisteredResource(PstRangeAction pstRangeAction, State state, RemedialActionSeriesCreationContext context) {
        if (!(context instanceof PstRangeActionSeriesCreationContext)) {
            throw new FaraoException("Expected a PstRangeActionSeriesCreationContext");
        }
        PstRangeActionSeriesCreationContext pstContext = (PstRangeActionSeriesCreationContext) context;

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
