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
import com.farao_community.farao.data.crac_impl.CracImpl;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.swe_cne_exporter.xsd.RemedialActionSeries;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
class SweRemedialActionSeriesCreatorTest {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";

    private SweCneHelper cneHelper;
    private Crac crac;
    private RaoResult raoResult;
    private CimCracCreationContext cracCreationContext;

    @BeforeEach
    public void setup() {
        this.crac = Mockito.mock(Crac.class);
        this.raoResult = Mockito.mock(RaoResult.class);
        this.cracCreationContext = Mockito.mock(CimCracCreationContext.class);
        this.cneHelper = Mockito.mock(SweCneHelper.class);

        Mockito.when(cneHelper.getCrac()).thenReturn(crac);
        Mockito.when(cneHelper.getRaoResult()).thenReturn(raoResult);
        Crac cracForInstants = new CracImpl("test-cracForInstants")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT_ID, InstantKind.OUTAGE)
            .newInstant(AUTO_INSTANT_ID, InstantKind.AUTO)
            .newInstant(CURATIVE_INSTANT_ID, InstantKind.CURATIVE);
        Instant preventiveInstant = cracForInstants.getInstant(PREVENTIVE_INSTANT_ID);
        Instant outageInstant = cracForInstants.getInstant(OUTAGE_INSTANT_ID);
        Instant autoInstant = cracForInstants.getInstant(AUTO_INSTANT_ID);
        Instant curativeInstant = cracForInstants.getInstant(CURATIVE_INSTANT_ID);
        Mockito.when(crac.getInstant(PREVENTIVE_INSTANT_ID)).thenReturn(preventiveInstant);
        Mockito.when(crac.getInstant(OUTAGE_INSTANT_ID)).thenReturn(outageInstant);
        Mockito.when(crac.getInstant(AUTO_INSTANT_ID)).thenReturn(autoInstant);
        Mockito.when(crac.getInstant(CURATIVE_INSTANT_ID)).thenReturn(curativeInstant);
        Mockito.when(crac.getInstant(InstantKind.PREVENTIVE)).thenReturn(preventiveInstant);
        Mockito.when(crac.getInstant(InstantKind.OUTAGE)).thenReturn(outageInstant);
        Mockito.when(crac.getInstant(InstantKind.AUTO)).thenReturn(autoInstant);
        Mockito.when(crac.getInstant(InstantKind.CURATIVE)).thenReturn(curativeInstant);
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
        Mockito.when(cracCreationContext.getRemedialActionSeriesCreationContexts()).thenReturn(rasccList);

        addRemedialActionToCrac("networkActionCreatedId", "networkActionName", NetworkAction.class);
        addRemedialActionToCrac("na_missing", "networkActionName", NetworkAction.class);
        addRemedialActionToCrac("pstCreatedId", "pstRangeActionName", PstRangeAction.class);
        addRemedialActionToCrac("pst_missing", "pstRangeActionName", PstRangeAction.class);
        addRemedialActionToCrac("hvdcFrEs + hvdcEsFr - 1", "hvdcFrEs1", HvdcRangeAction.class);
        addRemedialActionToCrac("hvdcFrEs + hvdcEsFr - 2", "hvdcFrEs2", HvdcRangeAction.class);
        addRemedialActionToCrac("hvdcPtEs + hvdcEsPt - 1", "hvdcPtEs1", HvdcRangeAction.class);
        addRemedialActionToCrac("hvdcPtEs + hvdcEsPt - 2", "hvdcPtEs2", HvdcRangeAction.class);

        State preventiveState = addStateToCrac(crac.getInstant(PREVENTIVE_INSTANT_ID), null);
        Contingency contingency = Mockito.mock(Contingency.class);
        Mockito.when(contingency.getId()).thenReturn("contingency");
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
            rascc = Mockito.mock(PstRangeActionSeriesCreationContext.class);
            Mockito.when(((PstRangeActionSeriesCreationContext) rascc).getNetworkElementNativeMrid()).thenReturn(pstElementMrid);
            Mockito.when(((PstRangeActionSeriesCreationContext) rascc).getNetworkElementNativeName()).thenReturn(pstElementName);
        } else {
            rascc = Mockito.mock(RemedialActionSeriesCreationContext.class);
        }
        Mockito.when(rascc.getNativeId()).thenReturn(nativeId);
        Mockito.when(rascc.getCreatedIds()).thenReturn(createdIds);
        Mockito.when(rascc.isInverted()).thenReturn(isInverted);
        Mockito.when(rascc.isImported()).thenReturn(isImported);
        return rascc;
    }

    private State addStateToCrac(Instant instant, Contingency contingency) {
        State state = Mockito.mock(State.class);
        Mockito.when(state.getInstant()).thenReturn(instant);
        Mockito.when(state.getContingency()).thenReturn(Objects.isNull(contingency) ? Optional.empty() : Optional.of(contingency));
        Mockito.when(crac.getState(contingency, instant)).thenReturn(state);
        if (instant.isPreventive()) {
            Mockito.when(state.isPreventive()).thenReturn(true);
            Mockito.when(crac.getPreventiveState()).thenReturn(state);
        } else {
            Mockito.when(state.isPreventive()).thenReturn(false);
        }
        return state;
    }

    private RemedialAction<?> addRemedialActionToCrac(String raId, String raName, Class clazz) {
        RemedialAction remedialAction;
        if (clazz.equals(NetworkAction.class)) {
            remedialAction = Mockito.mock(NetworkAction.class);
        } else if (clazz.equals(PstRangeAction.class)) {
            remedialAction = Mockito.mock(PstRangeAction.class);
        } else if (clazz.equals(HvdcRangeAction.class)) {
            remedialAction = Mockito.mock(HvdcRangeAction.class);
        } else {
            throw new FaraoException("unrecognized remedial action");
        }
        Mockito.when(remedialAction.getId()).thenReturn(raId);
        Mockito.when(remedialAction.getName()).thenReturn(raName);
        Mockito.when(crac.getRemedialAction(raId)).thenReturn(remedialAction);
        return remedialAction;
    }

    private void addNetworkActionToRaoResult(State state, String remedialActionId) {
        RemedialAction<?> remedialAction = crac.getRemedialAction(remedialActionId);
        Mockito.when(raoResult.isActivatedDuringState(state, remedialAction)).thenReturn(true);
    }

    private void addPstRangeActionToRaoResult(State state, String remedialActionId, double setpoint, int tap) {
        RemedialAction<?> remedialAction = crac.getRemedialAction(remedialActionId);
        Mockito.when(raoResult.isActivatedDuringState(state, remedialAction)).thenReturn(true);
        Mockito.when(raoResult.getOptimizedSetPointOnState(state, (PstRangeAction) remedialAction)).thenReturn(setpoint);
        Mockito.when(raoResult.getOptimizedTapOnState(state, (PstRangeAction) remedialAction)).thenReturn(tap);
    }

    private void addHvdcRangeActionToRaoResult(State state, String remedialActionId, double setpoint) {
        RemedialAction<?> remedialAction = crac.getRemedialAction(remedialActionId);
        Mockito.when(raoResult.isActivatedDuringState(state, remedialAction)).thenReturn(true);
        Mockito.when(raoResult.getOptimizedSetPointOnState(state, (HvdcRangeAction) remedialAction)).thenReturn(setpoint);
    }
}
