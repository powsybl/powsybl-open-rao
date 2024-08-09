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
import com.powsybl.openrao.data.swecneexporter.xsd.RemedialActionSeries;
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
        Mockito.when(raoResult.getComputationStatus()).thenReturn(ComputationStatus.DEFAULT);
        Instant preventiveInstant = getMockInstant(true, false, false);
        Instant autoInstant = getMockInstant(false, true, false);
        Instant curativeInstant = getMockInstant(false, false, true);
        Mockito.when(crac.getInstant(PREVENTIVE_INSTANT_ID)).thenReturn(preventiveInstant);
        Mockito.when(crac.getInstant(AUTO_INSTANT_ID)).thenReturn(autoInstant);
        Mockito.when(crac.getInstant(CURATIVE_INSTANT_ID)).thenReturn(curativeInstant);
        Mockito.when(crac.getPreventiveInstant()).thenReturn(preventiveInstant);
        Mockito.when(crac.getInstant(InstantKind.AUTO)).thenReturn(autoInstant);
        Mockito.when(crac.getInstant(InstantKind.CURATIVE)).thenReturn(curativeInstant);
    }

    private static Instant getMockInstant(boolean isPreventive, boolean isAuto, boolean isCurative) {
        Instant instant = Mockito.mock(Instant.class);
        Mockito.when(instant.isPreventive()).thenReturn(isPreventive);
        Mockito.when(instant.isAuto()).thenReturn(isAuto);
        Mockito.when(instant.isCurative()).thenReturn(isCurative);
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
        Mockito.when(rascc.getNativeObjectId()).thenReturn(nativeId);
        Mockito.when(rascc.getCreatedObjectsIds()).thenReturn(createdIds);
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
            throw new OpenRaoException("unrecognized remedial action");
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
