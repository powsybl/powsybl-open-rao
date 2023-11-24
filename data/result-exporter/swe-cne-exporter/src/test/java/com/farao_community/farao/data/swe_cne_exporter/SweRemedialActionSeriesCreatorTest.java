/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.swe_cne_exporter;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.remedial_action.PstRangeActionSeriesCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.remedial_action.RemedialActionSeriesCreationContext;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.swe_cne_exporter.xsd.RemedialActionSeries;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
class SweRemedialActionSeriesCreatorTest {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";

    private SweCneHelper cneHelper;
    private Crac crac;
    private RaoResult raoResult;
    private CimCracCreationContext cracCreationContext;

    @BeforeEach
    public void setup() {
        this.crac = mock(Crac.class);
        this.raoResult = mock(RaoResult.class);
        this.cracCreationContext = mock(CimCracCreationContext.class);
        this.cneHelper = mock(SweCneHelper.class);

        when(cneHelper.getCrac()).thenReturn(crac);
        when(cneHelper.getRaoResult()).thenReturn(raoResult);
        Instant preventiveInstant = getMockInstant(true, false, false);
        Instant autoInstant = getMockInstant(false, true, false);
        Instant curativeInstant = getMockInstant(false, false, true);
        when(crac.getInstant(PREVENTIVE_INSTANT_ID)).thenReturn(preventiveInstant);
        when(crac.getInstant(AUTO_INSTANT_ID)).thenReturn(autoInstant);
        when(crac.getInstant(CURATIVE_INSTANT_ID)).thenReturn(curativeInstant);
        when(crac.getInstant(InstantKind.PREVENTIVE)).thenReturn(preventiveInstant);
        when(crac.getInstant(InstantKind.AUTO)).thenReturn(autoInstant);
        when(crac.getInstant(InstantKind.CURATIVE)).thenReturn(curativeInstant);
    }

    private static Instant getMockInstant(boolean isPreventive, boolean isAuto, boolean isCurative) {
        Instant instant = mock(Instant.class);
        when(instant.isPreventive()).thenReturn(isPreventive);
        when(instant.isAuto()).thenReturn(isAuto);
        when(instant.isCurative()).thenReturn(isCurative);
        return instant;
    }

    @Test
    void generateRemedialActionSeriesTest() {
        Set<RemedialActionSeriesCreationContext> rasccList = new HashSet<>();
        rasccList.add(createRascc("networkActionNativeId", true, Set.of("networkActionCreatedId"), false, "", "", false));
        rasccList.add(createRascc("networkAction_shouldNotBeExported", false, Set.of("na_missing"), false, "", "", false));
        rasccList.add(createRascc("pstNativeId", true, Set.of("pstCreatedId"), true, "pstId", "pstName", false));
        rasccList.add(createRascc("pst_shouldNotBeExported", false, Set.of("pst_missing"), true, "pstId", "pstName", false));
        rasccList.add(createRascc("hvdcFrEs", true, Set.of("hvdcFrEs + hvdcEsFr - 1", "hvdcFrEs + hvdcEsFr - 2"), false, "", "", false));
        rasccList.add(createRascc("hvdcEsFr", true, Set.of("hvdcFrEs + hvdcEsFr - 1", "hvdcFrEs + hvdcEsFr - 2"), false, "", "", true));
        rasccList.add(createRascc("hvdcPtEs", true, Set.of("hvdcPtEs + hvdcEsPt - 1", "hvdcPtEs + hvdcEsPt - 2"), false, "", "", false));
        rasccList.add(createRascc("hvdcEsPt", true, Set.of("hvdcPtEs + hvdcEsPt - 1", "hvdcPtEs + hvdcEsPt - 2"), false, "", "", true));
        when(cracCreationContext.getRemedialActionSeriesCreationContexts()).thenReturn(rasccList);

        addRemedialActionToCrac("networkActionCreatedId", "networkActionName", NetworkAction.class);
        addRemedialActionToCrac("na_missing", "networkActionName", NetworkAction.class);
        addRemedialActionToCrac("pstCreatedId", "pstRangeActionName", PstRangeAction.class);
        addRemedialActionToCrac("pst_missing", "pstRangeActionName", PstRangeAction.class);
        addRemedialActionToCrac("hvdcFrEs + hvdcEsFr - 1", "hvdcFrEs1", HvdcRangeAction.class);
        addRemedialActionToCrac("hvdcFrEs + hvdcEsFr - 2", "hvdcFrEs2", HvdcRangeAction.class);
        addRemedialActionToCrac("hvdcPtEs + hvdcEsPt - 1", "hvdcPtEs1", HvdcRangeAction.class);
        addRemedialActionToCrac("hvdcPtEs + hvdcEsPt - 2", "hvdcPtEs2", HvdcRangeAction.class);

        State preventiveState = addStateToCrac(crac.getInstant(PREVENTIVE_INSTANT_ID), null);
        Contingency contingency = mock(Contingency.class);
        when(contingency.getId()).thenReturn("contingency");
        State autoState = addStateToCrac(crac.getInstant(AUTO_INSTANT_ID), contingency);
        State curativeState = addStateToCrac(crac.getInstant(CURATIVE_INSTANT_ID), contingency);

        addNetworkActionToRaoResult(preventiveState, "networkActionCreatedId");
        addNetworkActionToRaoResult(preventiveState, "na_missing");
        addPstRangeActionToRaoResult(preventiveState, "pstCreatedId", 0.6, 2);
        addPstRangeActionToRaoResult(autoState, "pstCreatedId", 0.9, 3);
        addPstRangeActionToRaoResult(curativeState, "pstCreatedId", 0.3, 1);
        addPstRangeActionToRaoResult(curativeState, "pst_missing", 0.3, 1);
        addHvdcRangeActionToRaoResult(autoState, "hvdcFrEs + hvdcEsFr - 1", 600.0);
        addHvdcRangeActionToRaoResult(autoState, "hvdcFrEs + hvdcEsFr - 2", 600.0);
        addHvdcRangeActionToRaoResult(autoState, "hvdcPtEs + hvdcEsPt - 1", -400.0);
        addHvdcRangeActionToRaoResult(autoState, "hvdcPtEs + hvdcEsPt - 2", -400.0);

        SweRemedialActionSeriesCreator raSeriesCreator = new SweRemedialActionSeriesCreator(cneHelper, cracCreationContext);

        List<RemedialActionSeries> basecaseSeries = raSeriesCreator.generateRaSeries(null);
        List<RemedialActionSeries> contingencySeries = raSeriesCreator.generateRaSeries(contingency);

        List<RemedialActionSeries> basecaseReferences = raSeriesCreator.generateRaSeriesReference(null);
        List<RemedialActionSeries> contingencyReferences = raSeriesCreator.generateRaSeriesReference(contingency);

        assertEquals(2, basecaseSeries.size());
        assertEquals(4, contingencySeries.size());
        assertEquals(2, basecaseReferences.size());
        assertEquals(6, contingencyReferences.size());

    }

    private RemedialActionSeriesCreationContext createRascc(String nativeId, boolean isImported, Set<String> createdIds, boolean isPst, String pstElementMrid, String pstElementName, boolean isInverted) {
        RemedialActionSeriesCreationContext rascc;
        if (isPst) {
            rascc = mock(PstRangeActionSeriesCreationContext.class);
            when(((PstRangeActionSeriesCreationContext) rascc).getNetworkElementNativeMrid()).thenReturn(pstElementMrid);
            when(((PstRangeActionSeriesCreationContext) rascc).getNetworkElementNativeName()).thenReturn(pstElementName);
        } else {
            rascc = mock(RemedialActionSeriesCreationContext.class);
        }
        when(rascc.getNativeId()).thenReturn(nativeId);
        when(rascc.getCreatedIds()).thenReturn(createdIds);
        when(rascc.isInverted()).thenReturn(isInverted);
        when(rascc.isImported()).thenReturn(isImported);
        return rascc;
    }

    private State addStateToCrac(Instant instant, Contingency contingency) {
        State state = mock(State.class);
        when(state.getInstant()).thenReturn(instant);
        when(state.getContingency()).thenReturn(Objects.isNull(contingency) ? Optional.empty() : Optional.of(contingency));
        when(crac.getState(contingency, instant)).thenReturn(state);
        if (instant.isPreventive()) {
            when(state.isPreventive()).thenReturn(true);
            when(crac.getPreventiveState()).thenReturn(state);
        } else {
            when(state.isPreventive()).thenReturn(false);
        }
        return state;
    }

    private RemedialAction<?> addRemedialActionToCrac(String raId, String raName, Class clazz) {
        RemedialAction remedialAction;
        if (clazz.equals(NetworkAction.class)) {
            remedialAction = mock(NetworkAction.class);
        } else if (clazz.equals(PstRangeAction.class)) {
            remedialAction = mock(PstRangeAction.class);
        } else if (clazz.equals(HvdcRangeAction.class)) {
            remedialAction = mock(HvdcRangeAction.class);
        } else {
            throw new FaraoException("unrecognized remedial action");
        }
        when(remedialAction.getId()).thenReturn(raId);
        when(remedialAction.getName()).thenReturn(raName);
        when(crac.getRemedialAction(raId)).thenReturn(remedialAction);
        return remedialAction;
    }

    private void addNetworkActionToRaoResult(State state, String remedialActionId) {
        RemedialAction<?> remedialAction = crac.getRemedialAction(remedialActionId);
        when(raoResult.isActivatedDuringState(state, remedialAction)).thenReturn(true);
    }

    private void addPstRangeActionToRaoResult(State state, String remedialActionId, double setpoint, int tap) {
        RemedialAction<?> remedialAction = crac.getRemedialAction(remedialActionId);
        when(raoResult.isActivatedDuringState(state, remedialAction)).thenReturn(true);
        when(raoResult.getOptimizedSetPointOnState(state, (PstRangeAction) remedialAction)).thenReturn(setpoint);
        when(raoResult.getOptimizedTapOnState(state, (PstRangeAction) remedialAction)).thenReturn(tap);
    }

    private void addHvdcRangeActionToRaoResult(State state, String remedialActionId, double setpoint) {
        RemedialAction<?> remedialAction = crac.getRemedialAction(remedialActionId);
        when(raoResult.isActivatedDuringState(state, remedialAction)).thenReturn(true);
        when(raoResult.getOptimizedSetPointOnState(state, (HvdcRangeAction) remedialAction)).thenReturn(setpoint);
    }
}
