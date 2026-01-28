/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.iidm.network.*;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.commons.logs.RaoBusinessLogs;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracFactory;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.crac.api.networkaction.ActionType;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.api.OptimizationStepsExecuted;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoPstRegulationParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoTopoOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SecondPreventiveRaoParameters;
import com.powsybl.openrao.searchtreerao.result.impl.FailedRaoResultImpl;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class CastorFullOptimizationTest {
    private static final double DOUBLE_TOLERANCE = 1e-5;

    private Crac crac;
    private Network network;
    private RaoInput raoInput;

    public void setup(String networkFile, String cracFile) throws IOException {
        network = Network.read(networkFile, getClass().getResourceAsStream("/network/" + networkFile));
        crac = Crac.read(cracFile, getClass().getResourceAsStream("/crac/" + cracFile), network);
        raoInput = RaoInput.build(network, crac).build();
    }

    @Test
    void smallRaoWithDivergingInitialSensi() throws IOException {
        // Small RAO with diverging initial sensi
        // Cannot optimize range actions in unit tests (needs OR-Tools installed)
        setup("small-network-2P.uct", "small-crac-2P.json");
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_oneIteration_v2.json"));

        // Run RAO
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertInstanceOf(FailedRaoResultImpl.class, raoResult);
    }

    @Test
    void smallRaoWithout2P() throws IOException {
        // Small RAO without second preventive optimization and only topological actions
        // Cannot optimize range actions in unit tests (needs OR-Tools installed)
        setup("small-network-2P.uct", "small-crac-2P.json");
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json"));

        // Run RAO
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertEquals(371.88, raoResult.getFunctionalCost(null), 1.);
        assertEquals(493.56, raoResult.getFunctionalCost(crac.getPreventiveInstant()), 1.);
        assertEquals(256.78, raoResult.getFunctionalCost(crac.getLastInstant()), 1.);
        assertEquals(Set.of(crac.getNetworkAction("close_de3_de4"), crac.getNetworkAction("close_fr1_fr5")), raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState()));
        assertEquals(Set.of(crac.getNetworkAction("open_fr1_fr3")), raoResult.getActivatedNetworkActionsDuringState(crac.getState(crac.getContingency("co1_fr2_fr3_1"), crac.getLastInstant())));
        assertEquals(OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY, raoResult.getExecutionDetails());
    }

    @Test
    void smallRaoWith2P() throws IOException {
        // Same RAO as before but activating 2P => results should be better
        setup("small-network-2P.uct", "small-crac-2P.json");
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json"));
        OpenRaoSearchTreeParameters searchTreeParameters = raoParameters.getExtension(OpenRaoSearchTreeParameters.class);

        // Activate 2P
        searchTreeParameters.getSecondPreventiveRaoParameters().setExecutionCondition(SecondPreventiveRaoParameters.ExecutionCondition.POSSIBLE_CURATIVE_IMPROVEMENT);

        // Run RAO
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertEquals(371.88, raoResult.getFunctionalCost(null), 1.);
        assertEquals(694.30, raoResult.getFunctionalCost(crac.getPreventiveInstant()), 1.);
        assertEquals(-555.91, raoResult.getFunctionalCost(crac.getLastInstant()), 1.);
        assertEquals(Set.of(crac.getNetworkAction("open_fr1_fr2")), raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState()));
        assertEquals(Set.of(crac.getNetworkAction("open_fr1_fr3")), raoResult.getActivatedNetworkActionsDuringState(crac.getState(crac.getContingency("co1_fr2_fr3_1"), crac.getLastInstant())));
        assertEquals(OptimizationStepsExecuted.SECOND_PREVENTIVE_IMPROVED_FIRST, raoResult.getExecutionDetails());
    }

    @Test
    void smallRaoWithGlobal2P() throws IOException {
        // Same RAO as before but activating Global 2P => results should be the same (there are no range actions)
        setup("small-network-2P.uct", "small-crac-2P.json");
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json"));
        OpenRaoSearchTreeParameters searchTreeParameters = raoParameters.getExtension(OpenRaoSearchTreeParameters.class);

        // Activate global 2P
        searchTreeParameters.getSecondPreventiveRaoParameters().setExecutionCondition(SecondPreventiveRaoParameters.ExecutionCondition.POSSIBLE_CURATIVE_IMPROVEMENT);

        // Run RAO
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertEquals(371.88, raoResult.getFunctionalCost(null), 1.);
        assertEquals(694.30, raoResult.getFunctionalCost(crac.getPreventiveInstant()), 1.);
        assertEquals(-555.91, raoResult.getFunctionalCost(crac.getLastInstant()), 1.);
        assertEquals(Set.of(crac.getNetworkAction("open_fr1_fr2")), raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState()));
        assertEquals(Set.of(crac.getNetworkAction("open_fr1_fr3")), raoResult.getActivatedNetworkActionsDuringState(crac.getState(crac.getContingency("co1_fr2_fr3_1"), crac.getLastInstant())));
        assertEquals(OptimizationStepsExecuted.SECOND_PREVENTIVE_IMPROVED_FIRST, raoResult.getExecutionDetails());
    }

    @Test
    void testOptimizationStepsExecutedAndLogsWhenFallbackOnFirstPrev() throws IOException {
        // Catch future logs
        Logger logger = (Logger) LoggerFactory.getLogger(RaoBusinessLogs.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        // Set up RAO and run
        setup("small-network-2P.uct", "small-crac-2P_cost_increase.json");
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json"));
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();

        // Test Optimization steps executed
        assertEquals(OptimizationStepsExecuted.FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION, raoResult.getExecutionDetails());

        // Test final log after RAO fallbacks
        listAppender.stop();
        List<ILoggingEvent> logsList = listAppender.list;
        assert logsList.getLast().toString().equals("[INFO] Cost before RAO = 371.88 (functional: 371.88, virtual: 0.0), cost after RAO = 371.88 (functional: 371.88, virtual: 0.0)");
    }

    @Test
    void testThreeCurativeInstantsWithSecondCurativeHavingNoCnecAndNoRa() {
        network = Network.read("small-network-2P.uct", getClass().getResourceAsStream("/network/small-network-2P.uct"));
        crac = CracFactory.findDefault().create("crac");

        crac.newInstant("preventive", InstantKind.PREVENTIVE)
            .newInstant("outage", InstantKind.OUTAGE)
            .newInstant("auto", InstantKind.AUTO)
            .newInstant("curative1", InstantKind.CURATIVE)
            .newInstant("curative2", InstantKind.CURATIVE)
            .newInstant("curative3", InstantKind.CURATIVE);

        Contingency co = crac.newContingency().withId("co1").withContingencyElement("FFR2AA1  FFR3AA1  1", ContingencyElementType.LINE).add();

        crac.newFlowCnec().withId("c1-prev").withInstant("preventive").withNetworkElement("FFR1AA1  FFR4AA1  1").withNominalVoltage(400.)
            .newThreshold().withSide(TwoSides.ONE).withMax(2000.).withUnit(Unit.AMPERE).add()
            .withOptimized().add();
        crac.newFlowCnec().withId("c1-out").withInstant("auto").withContingency("co1").withNetworkElement("FFR1AA1  FFR4AA1  1").withNominalVoltage(400.)
            .newThreshold().withSide(TwoSides.ONE).withMax(2500.).withUnit(Unit.AMPERE).add()
            .withOptimized().add();
        crac.newFlowCnec().withId("c1-cur1").withInstant("curative1").withContingency("co1").withNetworkElement("FFR1AA1  FFR4AA1  1").withNominalVoltage(400.)
            .newThreshold().withSide(TwoSides.ONE).withMax(2400.).withUnit(Unit.AMPERE).add()
            .withOptimized().add();
        crac.newFlowCnec().withId("c1-cur3").withInstant("curative3").withContingency("co1").withNetworkElement("FFR1AA1  FFR4AA1  1").withNominalVoltage(400.)
            .newThreshold().withSide(TwoSides.ONE).withMax(1700.).withUnit(Unit.AMPERE).add()
            .withOptimized().add();

        NetworkAction pstPrev = crac.newNetworkAction().withId("pst_fr@10-prev")
            .newPhaseTapChangerTapPositionAction().withNetworkElement("FFR2AA1  FFR4AA1  1").withTapPosition(10).add()
            .newOnInstantUsageRule().withInstant("preventive").add()
            .add();
        NetworkAction naCur1 = crac.newNetworkAction().withId("open_fr1_fr3-cur1")
            .newTerminalsConnectionAction().withActionType(ActionType.OPEN).withNetworkElement("FFR1AA1  FFR3AA1  1").add()
            .newOnInstantUsageRule().withInstant("curative1").add()
            .add();
        NetworkAction pstCur = crac.newNetworkAction().withId("pst_fr@-16-cur3")
            .newPhaseTapChangerTapPositionAction().withNetworkElement("FFR2AA1  FFR4AA1  1").withTapPosition(-16).add()
            .newOnInstantUsageRule().withInstant("curative3").add()
            .add();

        raoInput = RaoInput.build(network, crac).build();
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json"));
        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN);

        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();

        assertEquals(Set.of(pstPrev), raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState()));
        assertEquals(Set.of(naCur1), raoResult.getActivatedNetworkActionsDuringState(crac.getState(co, crac.getInstant("curative1"))));
        assertEquals(Set.of(pstCur), raoResult.getActivatedNetworkActionsDuringState(crac.getState(co, crac.getInstant("curative3"))));

        FlowCnec cnec;
        cnec = crac.getFlowCnec("c1-prev");
        assertEquals(2228.9, raoResult.getFlow(null, cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(1979.7, raoResult.getFlow(crac.getInstant(InstantKind.PREVENTIVE), cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(-228.9, raoResult.getMargin(null, cnec, Unit.AMPERE), 1.);
        assertEquals(20.3, raoResult.getMargin(crac.getInstant(InstantKind.PREVENTIVE), cnec, Unit.AMPERE), 1.);

        cnec = crac.getFlowCnec("c1-out");
        assertEquals(2371.88, raoResult.getFlow(null, cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(2129.22, raoResult.getFlow(crac.getInstant(InstantKind.PREVENTIVE), cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(128.12, raoResult.getMargin(null, cnec, Unit.AMPERE), 1.);
        assertEquals(370.78, raoResult.getMargin(crac.getInstant(InstantKind.PREVENTIVE), cnec, Unit.AMPERE), 1.);

        cnec = crac.getFlowCnec("c1-cur1");
        assertEquals(2371.88, raoResult.getFlow(null, cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(2129.22, raoResult.getFlow(crac.getInstant(InstantKind.PREVENTIVE), cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(1549.18, raoResult.getFlow(crac.getInstant("curative1"), cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(28.12, raoResult.getMargin(null, cnec, Unit.AMPERE), 1.);
        assertEquals(270.78, raoResult.getMargin(crac.getInstant(InstantKind.PREVENTIVE), cnec, Unit.AMPERE), 1.);
        assertEquals(850.82, raoResult.getMargin(crac.getInstant("curative1"), cnec, Unit.AMPERE), 1.);

        cnec = crac.getFlowCnec("c1-cur3");
        assertEquals(2371.88, raoResult.getFlow(null, cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(2129.22, raoResult.getFlow(crac.getInstant(InstantKind.PREVENTIVE), cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(1549.18, raoResult.getFlow(crac.getInstant("curative1"), cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(432.73, raoResult.getFlow(crac.getInstant("curative3"), cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(-671.88, raoResult.getMargin(null, cnec, Unit.AMPERE), 1.);
        assertEquals(-429.22, raoResult.getMargin(crac.getInstant(InstantKind.PREVENTIVE), cnec, Unit.AMPERE), 1.);
        assertEquals(150.82, raoResult.getMargin(crac.getInstant("curative1"), cnec, Unit.AMPERE), 1.);
        assertEquals(1267.27, raoResult.getMargin(crac.getInstant("curative3"), cnec, Unit.AMPERE), 1.);

        assertEquals(671.88, raoResult.getFunctionalCost(null), 1.);
        assertEquals(429.22, raoResult.getFunctionalCost(crac.getInstant(InstantKind.PREVENTIVE)), 1.);
        assertEquals(-20.30, raoResult.getFunctionalCost(crac.getInstant("curative1")), 1.);
        assertEquals(-20.30, raoResult.getFunctionalCost(crac.getInstant("curative3")), 1.);
    }

    @Test
    void testThreeCurativeInstants() {
        network = Network.read("small-network-2P.uct", getClass().getResourceAsStream("/network/small-network-2P.uct"));
        crac = CracFactory.findDefault().create("crac");

        crac.newInstant("preventive", InstantKind.PREVENTIVE)
            .newInstant("outage", InstantKind.OUTAGE)
            .newInstant("auto", InstantKind.AUTO)
            .newInstant("curative1", InstantKind.CURATIVE)
            .newInstant("curative2", InstantKind.CURATIVE)
            .newInstant("curative3", InstantKind.CURATIVE);

        Contingency co = crac.newContingency().withId("co1").withContingencyElement("FFR2AA1  FFR3AA1  1", ContingencyElementType.LINE).add();

        crac.newFlowCnec().withId("c1-prev").withInstant("preventive").withNetworkElement("FFR1AA1  FFR4AA1  1").withNominalVoltage(400.)
            .newThreshold().withSide(TwoSides.ONE).withMax(2000.).withUnit(Unit.AMPERE).add()
            .withOptimized().add();
        crac.newFlowCnec().withId("c1-out").withInstant("auto").withContingency("co1").withNetworkElement("FFR1AA1  FFR4AA1  1").withNominalVoltage(400.)
            .newThreshold().withSide(TwoSides.ONE).withMax(2500.).withUnit(Unit.AMPERE).add()
            .withOptimized().add();
        crac.newFlowCnec().withId("c1-cur1").withInstant("curative1").withContingency("co1").withNetworkElement("FFR1AA1  FFR4AA1  1").withNominalVoltage(400.)
            .newThreshold().withSide(TwoSides.ONE).withMax(2400.).withUnit(Unit.AMPERE).add()
            .withOptimized().add();
        crac.newFlowCnec().withId("c1-cur2").withInstant("curative2").withContingency("co1").withNetworkElement("FFR1AA1  FFR4AA1  1").withNominalVoltage(400.)
            .newThreshold().withSide(TwoSides.ONE).withMax(2300.).withUnit(Unit.AMPERE).add()
            .withOptimized().add();
        crac.newFlowCnec().withId("c1-cur3").withInstant("curative3").withContingency("co1").withNetworkElement("FFR1AA1  FFR4AA1  1").withNominalVoltage(400.)
            .newThreshold().withSide(TwoSides.ONE).withMax(1700.).withUnit(Unit.AMPERE).add()
            .withOptimized().add();

        NetworkAction pstPrev = crac.newNetworkAction().withId("pst_fr@10-prev")
            .newPhaseTapChangerTapPositionAction().withNetworkElement("FFR2AA1  FFR4AA1  1").withTapPosition(10).add()
            .newOnInstantUsageRule().withInstant("preventive").add()
            .add();
        NetworkAction naCur1 = crac.newNetworkAction().withId("open_fr1_fr3-cur1")
            .newTerminalsConnectionAction().withActionType(ActionType.OPEN).withNetworkElement("FFR1AA1  FFR3AA1  1").add()
            .newOnInstantUsageRule().withInstant("curative1").add()
            .add();
        NetworkAction pstCur2 = crac.newNetworkAction().withId("pst_fr@-3-cur2")
            .newPhaseTapChangerTapPositionAction().withNetworkElement("FFR2AA1  FFR4AA1  1").withTapPosition(-3).add()
            .newOnInstantUsageRule().withInstant("curative2").add()
            .add();
        NetworkAction pstCur = crac.newNetworkAction().withId("pst_fr@-16-cur3")
            .newPhaseTapChangerTapPositionAction().withNetworkElement("FFR2AA1  FFR4AA1  1").withTapPosition(-16).add()
            .newOnInstantUsageRule().withInstant("curative3").add()
            .add();

        raoInput = RaoInput.build(network, crac).build();
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json"));
        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN);

        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();

        assertEquals(Set.of(pstPrev), raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState()));
        assertEquals(Set.of(naCur1), raoResult.getActivatedNetworkActionsDuringState(crac.getState(co, crac.getInstant("curative1"))));
        assertEquals(Set.of(pstCur2), raoResult.getActivatedNetworkActionsDuringState(crac.getState(co, crac.getInstant("curative2"))));
        assertEquals(Set.of(pstCur), raoResult.getActivatedNetworkActionsDuringState(crac.getState(co, crac.getInstant("curative3"))));

        FlowCnec cnec;
        cnec = crac.getFlowCnec("c1-prev");
        assertEquals(2228.9, raoResult.getFlow(null, cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(1979.7, raoResult.getFlow(crac.getInstant(InstantKind.PREVENTIVE), cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(-228.9, raoResult.getMargin(null, cnec, Unit.AMPERE), 1.);
        assertEquals(20.3, raoResult.getMargin(crac.getInstant(InstantKind.PREVENTIVE), cnec, Unit.AMPERE), 1.);

        cnec = crac.getFlowCnec("c1-out");
        assertEquals(2371.88, raoResult.getFlow(null, cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(2129.22, raoResult.getFlow(crac.getInstant(InstantKind.PREVENTIVE), cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(128.12, raoResult.getMargin(null, cnec, Unit.AMPERE), 1.);
        assertEquals(370.78, raoResult.getMargin(crac.getInstant(InstantKind.PREVENTIVE), cnec, Unit.AMPERE), 1.);

        cnec = crac.getFlowCnec("c1-cur1");
        assertEquals(2371.88, raoResult.getFlow(null, cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(2129.22, raoResult.getFlow(crac.getInstant(InstantKind.PREVENTIVE), cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(1549.18, raoResult.getFlow(crac.getInstant("curative1"), cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(28.12, raoResult.getMargin(null, cnec, Unit.AMPERE), 1.);
        assertEquals(270.78, raoResult.getMargin(crac.getInstant(InstantKind.PREVENTIVE), cnec, Unit.AMPERE), 1.);
        assertEquals(850.82, raoResult.getMargin(crac.getInstant("curative1"), cnec, Unit.AMPERE), 1.);

        cnec = crac.getFlowCnec("c1-cur2");
        assertEquals(2371.88, raoResult.getFlow(null, cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(2129.22, raoResult.getFlow(crac.getInstant(InstantKind.PREVENTIVE), cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(1549.18, raoResult.getFlow(crac.getInstant("curative1"), cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(990.32, raoResult.getFlow(crac.getInstant("curative2"), cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(-71.88, raoResult.getMargin(null, cnec, Unit.AMPERE), 1.);
        assertEquals(170.78, raoResult.getMargin(crac.getInstant(InstantKind.PREVENTIVE), cnec, Unit.AMPERE), 1.);
        assertEquals(750.82, raoResult.getMargin(crac.getInstant("curative1"), cnec, Unit.AMPERE), 1.);
        assertEquals(1309.68, raoResult.getMargin(crac.getInstant("curative2"), cnec, Unit.AMPERE), 1.);

        cnec = crac.getFlowCnec("c1-cur3");
        assertEquals(2371.88, raoResult.getFlow(null, cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(2129.22, raoResult.getFlow(crac.getInstant(InstantKind.PREVENTIVE), cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(1549.18, raoResult.getFlow(crac.getInstant("curative1"), cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(990.32, raoResult.getFlow(crac.getInstant("curative2"), cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(432.73, raoResult.getFlow(crac.getInstant("curative3"), cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(-671.88, raoResult.getMargin(null, cnec, Unit.AMPERE), 1.);
        assertEquals(-429.22, raoResult.getMargin(crac.getInstant(InstantKind.PREVENTIVE), cnec, Unit.AMPERE), 1.);
        assertEquals(150.82, raoResult.getMargin(crac.getInstant("curative1"), cnec, Unit.AMPERE), 1.);
        assertEquals(709.68, raoResult.getMargin(crac.getInstant("curative2"), cnec, Unit.AMPERE), 1.);
        assertEquals(1267.27, raoResult.getMargin(crac.getInstant("curative3"), cnec, Unit.AMPERE), 1.);

        assertEquals(671.88, raoResult.getFunctionalCost(null), 1.);
        assertEquals(429.22, raoResult.getFunctionalCost(crac.getInstant(InstantKind.PREVENTIVE)), 1.);
        assertEquals(-20.30, raoResult.getFunctionalCost(crac.getInstant("curative1")), 1.);
        assertEquals(-20.30, raoResult.getFunctionalCost(crac.getInstant("curative2")), 1.);
        assertEquals(-20.30, raoResult.getFunctionalCost(crac.getInstant("curative3")), 1.);
    }

    @Test
    void threeCurativeInstantsWithCumulativeMaximumNumberOfApplicableRemedialActions() throws IOException {
        setup("12Nodes_4ParallelLines.uct", "small-crac-ra-limits-per-instant.json");
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_DC.json"));

        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();

        // SearchTree stop criterion is MIN_OBJECTIVE so all 3 remedial actions should be applied during the first curative instant
        // Yet, the number of RAs that can be applied is restricted to 1 (resp. 2) in total for curative1 (resp. curative2)
        assertEquals(1, raoResult.getActivatedNetworkActionsDuringState(crac.getState("contingency", crac.getInstant("curative1"))).size());
        assertEquals(1, raoResult.getActivatedNetworkActionsDuringState(crac.getState("contingency", crac.getInstant("curative2"))).size());
    }

    @Test
    void threeCurativeInstantsWithCumulativeMaximumNumberOfTsos() throws IOException {
        setup("12Nodes_4ParallelLines.uct", "small-crac-ra-limits-per-instant-3-tsos.json");
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_DC.json"));

        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();

        // SearchTree stop criterion is MIN_OBJECTIVE so all 3 remedial actions should be applied during the first curative instant
        // Yet, the number of RAs that can be applied is restricted to 2 (resp. 1) in total for curative1 (resp. curative2)
        assertEquals(2, raoResult.getActivatedNetworkActionsDuringState(crac.getState("contingency", crac.getInstant("curative1"))).size());
        assertEquals(0, raoResult.getActivatedNetworkActionsDuringState(crac.getState("contingency", crac.getInstant("curative2"))).size());
        assertEquals(1, raoResult.getActivatedNetworkActionsDuringState(crac.getState("contingency", crac.getInstant("curative3"))).size());
    }

    @Test
    void curativeOptimizationShouldNotBeDoneIfPreventiveUnsecure() throws IOException {
        setup("small-network-2P.uct", "small-crac-to-check-curative-optimization-if-preventive-unsecure.json");
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json"));

        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.SECURE_FLOW);
        raoParameters.getObjectiveFunctionParameters().setEnforceCurativeSecurity(false);

        // Run RAO
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertEquals(Collections.emptySet(), raoResult.getActivatedNetworkActionsDuringState(crac.getState("Contingency FFR2AA1  FFR3AA1  1", crac.getLastInstant())));
    }

    @Test
    void curativeOptimizationShouldBeDoneIfPreventiveSecure() throws IOException {
        setup("small-network-2P.uct", "small-crac-to-check-curative-optimization-if-preventive-secure.json");
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json"));

        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.SECURE_FLOW);
        raoParameters.getObjectiveFunctionParameters().setEnforceCurativeSecurity(false);

        // Run RAO
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertEquals(Set.of(crac.getNetworkAction("Open FFR1AA1  FFR4AA1  1")), raoResult.getActivatedNetworkActionsDuringState(crac.getState("Contingency FFR2AA1  FFR3AA1  1", crac.getLastInstant())));
    }

    @Test
    void curativeOptimizationShouldBeDoneIfPreventiveMinMarginNegative() throws IOException {
        setup("small-network-2P.uct", "small-crac-to-check-curative-optimization-if-preventive-secure.json");
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json"));

        raoParameters.getObjectiveFunctionParameters().setEnforceCurativeSecurity(false);

        // Run RAO
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertEquals(Set.of(crac.getNetworkAction("Open FFR1AA1  FFR4AA1  1")), raoResult.getActivatedNetworkActionsDuringState(crac.getState("Contingency FFR2AA1  FFR3AA1  1", crac.getLastInstant())));
    }

    @Test
    void curativeOptimizationShouldBeDoneIfPreventiveUnsecureAndAssociatedParameterSet() throws IOException {
        setup("small-network-2P.uct", "small-crac-to-check-curative-optimization-if-preventive-secure.json");
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json"));

        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.SECURE_FLOW);
        raoParameters.getObjectiveFunctionParameters().setEnforceCurativeSecurity(true);

        // Run RAO
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertEquals(Set.of(crac.getNetworkAction("Open FFR1AA1  FFR4AA1  1")), raoResult.getActivatedNetworkActionsDuringState(crac.getState("Contingency FFR2AA1  FFR3AA1  1", crac.getLastInstant())));
    }

    @Test
    void curativeOptimizationShouldBeDoneIfPreventiveSecureAndAssociatedParameterSet() throws IOException {
        setup("small-network-2P.uct", "small-crac-to-check-curative-optimization-if-preventive-secure.json");
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json"));

        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.SECURE_FLOW);
        raoParameters.getObjectiveFunctionParameters().setEnforceCurativeSecurity(true);

        // Run RAO
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertEquals(Set.of(crac.getNetworkAction("Open FFR1AA1  FFR4AA1  1")), raoResult.getActivatedNetworkActionsDuringState(crac.getState("Contingency FFR2AA1  FFR3AA1  1", crac.getLastInstant())));
    }

    @Test
    void curativeOptimizationShouldBeDoneIfPreventiveMinMarginNegativeAndAssociatedParameterSet() throws IOException {
        setup("small-network-2P.uct", "small-crac-to-check-curative-optimization-if-preventive-secure.json");
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json"));

        raoParameters.getObjectiveFunctionParameters().setEnforceCurativeSecurity(true);

        // Run RAO
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertEquals(Set.of(crac.getNetworkAction("Open FFR1AA1  FFR4AA1  1")), raoResult.getActivatedNetworkActionsDuringState(crac.getState("Contingency FFR2AA1  FFR3AA1  1", crac.getLastInstant())));
    }

    @Test
    void curativeStopCriterionReachedSkipsPerimeterBuilding() throws IOException {
        setup("small-network-2P.uct", "small-crac-purely-virtual-curative.json");
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_secure.json"));

        raoParameters.getObjectiveFunctionParameters().setEnforceCurativeSecurity(true);

        // Run RAO, if not skipping, then tap to -15, since skipping, it stays at preventive optimization value (-12)
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertEquals(-12, raoResult.getOptimizedTapOnState(crac.getState("N-1 NL1-NL3", crac.getLastInstant()), crac.getPstRangeAction("CRA_PST_BE")));
    }

    @Test
    void catchDuringDataInitialization() throws IOException {
        setup("small-network-2P.uct", "small-crac-2P.json");
        RaoResult raoResult = new CastorFullOptimization(raoInput, null, null).run().join();
        assertInstanceOf(FailedRaoResultImpl.class, raoResult);
        assertEquals("RAO failed during data initialization : Cannot invoke \"com.powsybl.openrao.raoapi.parameters.RaoParameters.getObjectiveFunctionParameters()\" because \"raoParameters\" is null", raoResult.getExecutionDetails());
    }

    @Test
    void catchDuringInitialSensitivity() throws IOException {
        setup("small-network-2P.uct", "small-crac-2P.json");
        RaoParameters raoParameters = new RaoParameters();
        OpenRaoSearchTreeParameters searchTreeParameters = Mockito.spy(new OpenRaoSearchTreeParameters());
        raoParameters.addExtension(OpenRaoSearchTreeParameters.class, searchTreeParameters);
        LoadFlowAndSensitivityParameters loadFlowAndSensitivityParameters = Mockito.spy(new LoadFlowAndSensitivityParameters());
        when(searchTreeParameters.getLoadFlowAndSensitivityParameters()).thenReturn(loadFlowAndSensitivityParameters);
        when(loadFlowAndSensitivityParameters.getSensitivityProvider()).thenThrow(new OpenRaoException("Testing exception handling"));

        try (MockedStatic<JsonRaoParameters> jsonRaoParametersMock = mockStatic(JsonRaoParameters.class)) {
            jsonRaoParametersMock.when(() -> JsonRaoParameters.read(Mockito.any(InputStream.class))).thenReturn(raoParameters);

            RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
            assertInstanceOf(FailedRaoResultImpl.class, raoResult);
            assertEquals("RAO failed during initial sensitivity analysis : Testing exception handling", raoResult.getExecutionDetails());
        }
    }

    @Test
    void catchDuringFirstPreventive() throws IOException {
        setup("small-network-2P.uct", "small-crac-2P.json");
        RaoParameters raoParameters = Mockito.spy(JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json")));
        when(raoParameters.getTopoOptimizationParameters()).thenThrow(new OpenRaoException("Testing exception handling"));

        try (MockedStatic<JsonRaoParameters> jsonRaoParametersMock = mockStatic(JsonRaoParameters.class)) {
            jsonRaoParametersMock.when(() -> JsonRaoParameters.read(Mockito.any(InputStream.class))).thenReturn(raoParameters);

            RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
            assertEquals("RAO failed during first preventive : Testing exception handling", raoResult.getExecutionDetails());
        }
    }

    @Test
    void catchDuringContingencyScenarios() throws IOException {
        setup("small-network-2P.uct", "small-crac-2P.json");
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json"));

        SearchTreeRaoTopoOptimizationParameters topoOptimizationParameters = Mockito.spy(raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getTopoOptimizationParameters());
        when(topoOptimizationParameters.getMaxCurativeSearchTreeDepth()).thenThrow(new OpenRaoException("Testing exception handling"));
        raoParameters.getExtension(OpenRaoSearchTreeParameters.class).setTopoOptimizationParameters(topoOptimizationParameters);

        try (MockedStatic<JsonRaoParameters> jsonRaoParametersMock = mockStatic(JsonRaoParameters.class)) {
            jsonRaoParametersMock.when(() -> JsonRaoParameters.read(Mockito.any(InputStream.class))).thenReturn(raoParameters);

            RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
            assertEquals("RAO failed during contingency scenarios : Testing exception handling", raoResult.getExecutionDetails());
        }
    }

    @Test
    void catchDuringSecondPreventive() throws IOException {
        setup("small-network-2P.uct", "small-crac-2P.json");
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json"));
        OpenRaoSearchTreeParameters searchTreeParameters = raoParameters.getExtension(OpenRaoSearchTreeParameters.class);
        OpenRaoSearchTreeParameters searchTreeParametersSpied = Mockito.spy(searchTreeParameters);
        raoParameters.removeExtension(OpenRaoSearchTreeParameters.class);
        raoParameters.addExtension(OpenRaoSearchTreeParameters.class, searchTreeParametersSpied);
        when(searchTreeParametersSpied.getSecondPreventiveRaoParameters()).thenThrow(new OpenRaoException("Testing exception handling"));

        try (MockedStatic<JsonRaoParameters> jsonRaoParametersMock = mockStatic(JsonRaoParameters.class)) {
            jsonRaoParametersMock.when(() -> JsonRaoParameters.read(Mockito.any(InputStream.class))).thenReturn(raoParameters);

            RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
            assertEquals("RAO failed during second preventive optimization : Testing exception handling", raoResult.getExecutionDetails());
        }
    }

    // Costly optimization tests

    @Test
    void costlyPreventiveRaoNetworkActionsOnly() throws IOException {
        network = Network.read("2Nodes4ParallelLines.uct", getClass().getResourceAsStream("/network/2Nodes4ParallelLines.uct"));
        crac = Crac.read("small-crac-costly-preventive-only.json", getClass().getResourceAsStream("/crac/small-crac-costly-preventive-only.json"), network);
        RaoInput raoInput = RaoInput.build(network, crac).build();
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_dc_minObjective.json"));

        // Run RAO
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertEquals(Set.of("min-margin-violation-evaluator", "sensitivity-failure-cost"), raoResult.getVirtualCostNames());

        assertEquals(Set.of(crac.getNetworkAction("closeBeFr4")), raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState()));

        assertEquals(10.0, raoResult.getCost(crac.getInstant("preventive")), DOUBLE_TOLERANCE);
        assertEquals(10.0, raoResult.getFunctionalCost(crac.getInstant("preventive")), DOUBLE_TOLERANCE);
        assertEquals(0.0, raoResult.getVirtualCost(crac.getInstant("preventive")), DOUBLE_TOLERANCE);
    }

    @Test
    void testRaoWithEmptyCrac() throws IOException {
        setup("4Nodes.uct", "empty-crac.json");
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json"));

        // Run RAO
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertNotNull(raoResult);
        // When no cnec is present, a default value of -1e9 is returned
        assertEquals(-1e9, raoResult.getCost(null));
    }

    @Test
    void checkWithHvdc() throws IOException {
        // same test as US 15.17.8
        setup("TestCase16NodesWithHvdc_AC_emulation.xiidm", "jsonCrac_ep15us12-5case8.json");
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_AC.json"));
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertEquals(-432.82, raoResult.getCost(crac.getInstant("curative")), 1e-2);
        assertEquals(1, raoResult.getActivatedRangeActionsDuringState(crac.getState("co1_be1_fr5", crac.getInstant(InstantKind.CURATIVE))).size());
        assertEquals("CRA_HVDC", raoResult.getActivatedRangeActionsDuringState(crac.getState("co1_be1_fr5", crac.getInstant(InstantKind.CURATIVE))).iterator().next().getId());
    }

    @Test
    void testPstRegulationAtTheEndOfRao() throws IOException {
        network = Network.read("2Nodes3ParallelLinesPST.uct", getClass().getResourceAsStream("/network/2Nodes3ParallelLinesPST.uct"));
        crac = Crac.read("crac-regulation-1-PST.json", getClass().getResourceAsStream("/crac/crac-regulation-1-PST.json"), network);
        RaoInput raoInput = RaoInput.build(network, crac).build();
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_minMargin_ac.json"));

        Instant curativeInstant = crac.getInstant(InstantKind.CURATIVE);
        State curativeState = crac.getState("Contingency BE1 FR1 3", curativeInstant);

        PstRangeAction pstRangeAction = crac.getPstRangeAction("pstBeFr2");
        FlowCnec curativeCnecOnLine = crac.getFlowCnec("cnecBeFr1Curative");
        FlowCnec curativeCnecOnPst = crac.getFlowCnec("cnecBeFr2Curative");

        // first run without regulation: min margin is maximized by setting PST on tap -2 even though PSt is overloaded
        // but not seen by the RAO because it has no associated FlowCNEC
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertEquals(690.23, raoResult.getCost(crac.getLastInstant()), 1e-2);
        assertEquals(-2, raoResult.getOptimizedTapOnState(curativeState, pstRangeAction));
        assertEquals(-676.38, raoResult.getMargin(curativeInstant, curativeCnecOnLine, Unit.AMPERE), 1e-2);
        assertEquals(-690.23, raoResult.getMargin(curativeInstant, curativeCnecOnPst, Unit.AMPERE), 1e-2);

        // second run with regulation: regulation shifts PST's tap to position 7 to remove the overload but worsens min margin
        SearchTreeRaoPstRegulationParameters pstRegulationParameters = new SearchTreeRaoPstRegulationParameters();
        pstRegulationParameters.setPstsToRegulate(Map.of("BBE1AA1  FFR1AA1  2", "BBE1AA1  FFR1AA1  2"));
        raoParameters.getExtension(OpenRaoSearchTreeParameters.class).setPstRegulationParameters(pstRegulationParameters);

        network = Network.read("2Nodes3ParallelLinesPST.uct", getClass().getResourceAsStream("/network/2Nodes3ParallelLinesPST.uct"));
        raoInput = RaoInput.build(network, crac).build(); // reload RAO inputs to avoid issues on existing variants

        RaoResult raoResultWithRegulation = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertEquals(1382.77, raoResultWithRegulation.getCost(crac.getLastInstant()), 1e-2);
        assertEquals(7, raoResultWithRegulation.getOptimizedTapOnState(curativeState, pstRangeAction));
        assertEquals(-1382.77, raoResultWithRegulation.getMargin(curativeInstant, curativeCnecOnLine, Unit.AMPERE), 1e-2);
        assertEquals(15.49, raoResultWithRegulation.getMargin(curativeInstant, curativeCnecOnPst, Unit.AMPERE), 1e-2);
    }
}
