/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.swecneexporter;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.*;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracio.cim.craccreator.CimCracCreationContext;
import com.powsybl.openrao.data.cracio.cim.craccreator.PstRangeActionSeriesCreationContext;
import com.powsybl.openrao.data.cracio.cim.craccreator.RemedialActionSeriesCreationContext;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.data.swecneexporter.xsd.RemedialActionRegisteredResource;
import com.powsybl.openrao.data.swecneexporter.xsd.RemedialActionSeries;
import org.apache.commons.lang3.NotImplementedException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static com.powsybl.openrao.data.cneexportercommons.CneConstants.*;

/**
 * Generates RemedialActionSeries for SWE CNE format
 *
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class SweRemedialActionSeriesCreator {
    private final SweCneHelper sweCneHelper;
    private final CimCracCreationContext cracCreationContext;

    public SweRemedialActionSeriesCreator(SweCneHelper sweCneHelper, CimCracCreationContext cracCreationContext) {
        this.sweCneHelper = sweCneHelper;
        this.cracCreationContext = cracCreationContext;
    }

    public List<RemedialActionSeries> generateRaSeries(Contingency contingency) {
        List<RemedialActionSeries> remedialActionSeriesList = new ArrayList<>();
        Crac crac = sweCneHelper.getCrac();
        List<RemedialActionSeriesCreationContext> sortedRas = cracCreationContext.getRemedialActionSeriesCreationContexts().stream()
            .filter(RemedialActionSeriesCreationContext::isImported)
            .sorted(Comparator.comparing(RemedialActionSeriesCreationContext::getNativeObjectId))
            .toList();
        if (contingency == null) {
            //PREVENTIVE
            sortedRas.forEach(
                raSeriesCreationContext -> {
                    RemedialActionSeries raSeries = generateRaSeries(crac.getPreventiveState(), raSeriesCreationContext, false);
                    if (Objects.nonNull(raSeries)) {
                        remedialActionSeriesList.add(raSeries);
                    }
                }
            );
        } else {
            //CURATIVE && AUTO
            sortedRas.forEach(
                raSeriesCreationContext -> {
                    RemedialActionSeries raSeries = generateRaSeries(crac.getState(contingency, crac.getInstant(InstantKind.AUTO)), raSeriesCreationContext, false);
                    if (Objects.nonNull(raSeries)) {
                        remedialActionSeriesList.add(raSeries);
                    }
                }
            );
            sortedRas.forEach(
                raSeriesCreationContext -> {
                    RemedialActionSeries raSeries = generateRaSeries(crac.getState(contingency, crac.getInstant(InstantKind.CURATIVE)), raSeriesCreationContext, false);
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
        Crac crac = sweCneHelper.getCrac();
        List<RemedialActionSeriesCreationContext> sortedRas = cracCreationContext.getRemedialActionSeriesCreationContexts().stream()
            .filter(RemedialActionSeriesCreationContext::isImported)
            .sorted(Comparator.comparing(RemedialActionSeriesCreationContext::getNativeObjectId))
            .toList();
        if (contingency == null) {
            //PREVENTIVE
            sortedRas.forEach(
                raSeriesCreationContext -> {
                    RemedialActionSeries raSeries = generateRaSeries(crac.getPreventiveState(), raSeriesCreationContext, true);
                    if (Objects.nonNull(raSeries)) {
                        remedialActionSeriesList.add(raSeries);
                    }
                }
            );
        } else {
            //for the B57, in a contingency case, we want all remedial actions with an effect on the cnecs, so PREVENTIVE, CURATIVE && AUTO
            sortedRas.forEach(
                raSeriesCreationContext -> {
                    RemedialActionSeries raSeries = generateRaSeries(crac.getPreventiveState(), raSeriesCreationContext, true);
                    if (Objects.nonNull(raSeries)) {
                        remedialActionSeriesList.add(raSeries);
                    }
                }
            );
            sortedRas.forEach(
                raSeriesCreationContext -> {
                    RemedialActionSeries raSeries = generateRaSeries(crac.getState(contingency, crac.getInstant(InstantKind.AUTO)), raSeriesCreationContext, true);
                    if (Objects.nonNull(raSeries)) {
                        remedialActionSeriesList.add(raSeries);
                    }
                }
            );
            sortedRas.forEach(
                raSeriesCreationContext -> {
                    RemedialActionSeries raSeries = generateRaSeries(crac.getState(contingency, crac.getInstant(InstantKind.CURATIVE)), raSeriesCreationContext, true);
                    if (Objects.nonNull(raSeries)) {
                        remedialActionSeriesList.add(raSeries);
                    }
                }
            );
        }
        return remedialActionSeriesList;
    }

    private RemedialActionSeries generateRaSeries(State state, RemedialActionSeriesCreationContext context, boolean onlyReference) {
        RaoResult raoResult = sweCneHelper.getRaoResult();
        Crac crac = sweCneHelper.getCrac();
        List<RemedialAction<?>> usedRas = new ArrayList<>();
        if (raoResult == null) {
            return null;
        }
        context.getCreatedObjectsIds().stream().sorted()
                .map(crac::getRemedialAction)
                .filter(ra -> raoResult.isActivatedDuringState(state, ra)).forEach(usedRas::add);
        if (!raoResult.getComputationStatus().equals(ComputationStatus.FAILURE)) {
            context.getCreatedObjectsIds().stream().sorted()
                .map(crac::getRemedialAction).filter(ra ->
                    raoResult.getActivatedRangeActionsDuringState(state).stream().anyMatch(cra -> cra.getId().equals(ra.getId())) ||
                        raoResult.getActivatedNetworkActionsDuringState(state).stream().anyMatch(cra -> cra.getId().equals(ra.getId()))
                ).forEach(usedRas::add);
        }
        for (RemedialAction<?> usedRa : usedRas) {
            if (usedRa instanceof NetworkAction networkAction) {
                return generateNetworkRaSeries(networkAction, state);
            } else if (usedRa instanceof PstRangeAction pstRangeAction) {
                return generatePstRaSeries(pstRangeAction, state, context, onlyReference);
            } else if (usedRa instanceof HvdcRangeAction hvdcRangeAction) {
                // In case of an HVDC, the native crac has one series per direction, we select the one that corresponds to the sign of the setpoint
                if (context.isInverted() == (raoResult.getOptimizedSetPointOnState(state, hvdcRangeAction) < 0)) {
                    return generateHvdcRaSeries(hvdcRangeAction, state, context);
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
        remedialActionSeries.setMRID(rangeAction.getId() + "@" + sweCneHelper.getRaoResult().getOptimizedTapOnState(state, rangeAction) + "@");
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
        double absoluteSetpoint = Math.abs(sweCneHelper.getRaoResult().getOptimizedSetPointOnState(state, rangeAction));
        String nativeId = context.getNativeObjectId();
        remedialActionSeries.setMRID(nativeId + "@" + absoluteSetpoint + "@");
        remedialActionSeries.setName(nativeId + "@" + absoluteSetpoint + "@");
        remedialActionSeries.setApplicationModeMarketObjectStatusStatus(getApplicationModeMarketObjectStatusStatus(state));
        return remedialActionSeries;
    }

    private String getApplicationModeMarketObjectStatusStatus(State state) {
        if (state.isPreventive()) {
            return PREVENTIVE_MARKET_OBJECT_STATUS;
        } else if (state.getInstant().isAuto()) {
            return AUTO_MARKET_OBJECT_STATUS;
        } else if (state.getInstant().isCurative()) {
            return CURATIVE_MARKET_OBJECT_STATUS;
        } else {
            throw new OpenRaoException(String.format("Unexpected instant for remedial action application : %s", state.getInstant().toString()));
        }
    }

    private RemedialActionRegisteredResource generateRegisteredResource(PstRangeAction pstRangeAction, State state, RemedialActionSeriesCreationContext context) {
        if (!(context instanceof PstRangeActionSeriesCreationContext pstContext)) {
            throw new OpenRaoException("Expected a PstRangeActionSeriesCreationContext");
        }

        RemedialActionRegisteredResource registeredResource = new RemedialActionRegisteredResource();
        registeredResource.setMRID(SweCneUtil.createResourceIDString(A02_CODING_SCHEME, pstContext.getNetworkElementNativeMrid()));
        registeredResource.setName(pstContext.getNetworkElementNativeName());
        registeredResource.setPSRTypePsrType(PST_RANGE_PSR_TYPE);
        registeredResource.setMarketObjectStatusStatus(ABSOLUTE_MARKET_OBJECT_STATUS);
        registeredResource.setResourceCapacityDefaultCapacity(new BigDecimal(sweCneHelper.getRaoResult().getOptimizedTapOnState(state, pstRangeAction)));
        registeredResource.setResourceCapacityUnitSymbol(WITHOUT_UNIT_SYMBOL);

        return registeredResource;
    }

}
