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
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.crac.api.networkaction.ActionType;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.api.OptimizationStepsExecuted;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.MultithreadingParameters;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.SecondPreventiveRaoParameters;
import com.powsybl.openrao.searchtreerao.result.impl.FailedRaoResultImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.powsybl.openrao.data.raoresult.api.OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class CastorFullOptimizationTest {
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
        assertEquals(FIRST_PREVENTIVE_ONLY, raoResult.getExecutionDetails());
    }

    @Test
    void smallRaoWith2P() throws IOException {
        // Same RAO as before but activating 2P => results should be better
        setup("small-network-2P.uct", "small-crac-2P.json");
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json"));

        // Activate 2P
        raoParameters.getSecondPreventiveRaoParameters().setExecutionCondition(SecondPreventiveRaoParameters.ExecutionCondition.POSSIBLE_CURATIVE_IMPROVEMENT);

        // Run RAO
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertEquals(371.88, raoResult.getFunctionalCost(null), 1.);
        assertEquals(674.6, raoResult.getFunctionalCost(crac.getPreventiveInstant()), 1.);
        assertEquals(-555.91, raoResult.getFunctionalCost(crac.getLastInstant()), 1.);
        assertEquals(Set.of(crac.getNetworkAction("close_de3_de4"), crac.getNetworkAction("open_fr1_fr2")), raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState()));
        assertEquals(Set.of(crac.getNetworkAction("open_fr1_fr3")), raoResult.getActivatedNetworkActionsDuringState(crac.getState(crac.getContingency("co1_fr2_fr3_1"), crac.getLastInstant())));
        assertEquals(OptimizationStepsExecuted.SECOND_PREVENTIVE_IMPROVED_FIRST, raoResult.getExecutionDetails());
    }

    @Test
    void smallRaoWithGlobal2P() throws IOException {
        // Same RAO as before but activating Global 2P => results should be the same (there are no range actions)
        setup("small-network-2P.uct", "small-crac-2P.json");
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json"));

        // Activate global 2P
        raoParameters.getSecondPreventiveRaoParameters().setExecutionCondition(SecondPreventiveRaoParameters.ExecutionCondition.POSSIBLE_CURATIVE_IMPROVEMENT);
        raoParameters.getSecondPreventiveRaoParameters().setReOptimizeCurativeRangeActions(true);

        // Run RAO
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertEquals(371.88, raoResult.getFunctionalCost(null), 1.);
        assertEquals(674.6, raoResult.getFunctionalCost(crac.getPreventiveInstant()), 1.);
        assertEquals(-555.91, raoResult.getFunctionalCost(crac.getLastInstant()), 1.);
        assertEquals(Set.of(crac.getNetworkAction("close_de3_de4"), crac.getNetworkAction("open_fr1_fr2")), raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState()));
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
        raoParameters.getObjectiveFunctionParameters().setForbidCostIncrease(true);
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();

        // Test Optimization steps executed
        assertEquals(OptimizationStepsExecuted.FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION, raoResult.getExecutionDetails());

        // Test final log after RAO fallbacks
        listAppender.stop();
        List<ILoggingEvent> logsList = listAppender.list;
        assert logsList.get(logsList.size() - 1).toString().equals("[INFO] Cost before RAO = 371.88 (functional: 371.88, virtual: 0.0), cost after RAO = 371.88 (functional: 371.88, virtual: 0.0)");
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
            .newOnInstantUsageRule().withInstant("preventive").withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();
        NetworkAction naCur1 = crac.newNetworkAction().withId("open_fr1_fr3-cur1")
            .newTerminalsConnectionAction().withActionType(ActionType.OPEN).withNetworkElement("FFR1AA1  FFR3AA1  1").add()
            .newOnInstantUsageRule().withInstant("curative1").withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();
        NetworkAction pstCur = crac.newNetworkAction().withId("pst_fr@-16-cur3")
            .newPhaseTapChangerTapPositionAction().withNetworkElement("FFR2AA1  FFR4AA1  1").withTapPosition(-16).add()
            .newOnInstantUsageRule().withInstant("curative3").withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        raoInput = RaoInput.build(network, crac).build();
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json"));
        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN_IN_AMPERE);

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
            .newOnInstantUsageRule().withInstant("preventive").withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();
        NetworkAction naCur1 = crac.newNetworkAction().withId("open_fr1_fr3-cur1")
            .newTerminalsConnectionAction().withActionType(ActionType.OPEN).withNetworkElement("FFR1AA1  FFR3AA1  1").add()
            .newOnInstantUsageRule().withInstant("curative1").withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();
        NetworkAction pstCur2 = crac.newNetworkAction().withId("pst_fr@-3-cur2")
            .newPhaseTapChangerTapPositionAction().withNetworkElement("FFR2AA1  FFR4AA1  1").withTapPosition(-3).add()
            .newOnInstantUsageRule().withInstant("curative2").withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();
        NetworkAction pstCur = crac.newNetworkAction().withId("pst_fr@-16-cur3")
            .newPhaseTapChangerTapPositionAction().withNetworkElement("FFR2AA1  FFR4AA1  1").withTapPosition(-16).add()
            .newOnInstantUsageRule().withInstant("curative3").withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        raoInput = RaoInput.build(network, crac).build();
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json"));
        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN_IN_AMPERE);

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
    void optimizationWithAutoSearchTree() throws IOException {
        setup("12Nodes_2_twin_lines.uct", "small-crac-available-aras.json");
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_DC.json"));

        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();

        // One FORCED topological ARA is simulated
        // Two AVAILABLE topological ARA are present in the CRAC but one is enough to secure the network
        // One FORCED PST ARA will not be used because the network is already secure after the search tree

        State automatonState = crac.getState("Contingency DE2 NL3 1", crac.getInstant("auto"));
        Set<RangeAction<?>> appliedPstAras = raoResult.getActivatedRangeActionsDuringState(automatonState);

        assertEquals(Set.of("ARA_CLOSE_DE2_NL3_2", "ARA_CLOSE_NL2_BE3_2"), raoResult.getActivatedNetworkActionsDuringState(automatonState).stream().map(NetworkAction::getId).collect(Collectors.toSet()));
        assertTrue(appliedPstAras.isEmpty());

        assertEquals(-382.0, raoResult.getFlow(crac.getInstant("preventive"), crac.getFlowCnec("NNL2AA1  BBE3AA1  1 - preventive"), TwoSides.ONE, Unit.MEGAWATT), 1.);
        assertEquals(-1000.0, raoResult.getFlow(crac.getInstant("outage"), crac.getFlowCnec("NNL2AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - outage"), TwoSides.ONE, Unit.MEGAWATT), 1.);
        assertEquals(-207.0, raoResult.getFlow(crac.getInstant("auto"), crac.getFlowCnec("NNL2AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - auto"), TwoSides.ONE, Unit.MEGAWATT), 1.);
        assertEquals(-207.0, raoResult.getFlow(crac.getInstant("curative"), crac.getFlowCnec("NNL2AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative"), TwoSides.ONE, Unit.MEGAWATT), 1.);
    }

    @Test
    void optimizationWithAutoSearchTreeAndAutoPsts() throws IOException {
        setup("12Nodes_2_twin_lines.uct", "small-crac-available-aras-low-limits-thresholds.json");
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_DC.json"));

        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();

        State automatonState = crac.getState("Contingency DE2 NL3 1", crac.getInstant("auto"));
        List<NetworkAction> appliedNetworkAras = raoResult.getActivatedNetworkActionsDuringState(automatonState).stream().sorted(Comparator.comparing(NetworkAction::getId)).toList();
        Set<RangeAction<?>> appliedPstAras = raoResult.getActivatedRangeActionsDuringState(automatonState);

        assertEquals(3, appliedNetworkAras.size());
        assertEquals("ARA_CLOSE_DE2_NL3_2", appliedNetworkAras.get(0).getId());
        assertEquals("ARA_CLOSE_NL2_BE3_2", appliedNetworkAras.get(1).getId());
        assertEquals("ARA_INJECTION_SETPOINT_800MW", appliedNetworkAras.get(2).getId());
        assertEquals(1, appliedPstAras.size());
        assertEquals("ARA_PST_BE", appliedPstAras.iterator().next().getId());

        assertEquals(-382.0, raoResult.getFlow(crac.getInstant("preventive"), crac.getFlowCnec("NNL2AA1  BBE3AA1  1 - preventive"), TwoSides.ONE, Unit.MEGAWATT), 1.);
        assertEquals(-1000.0, raoResult.getFlow(crac.getInstant("outage"), crac.getFlowCnec("NNL2AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - outage"), TwoSides.ONE, Unit.MEGAWATT), 1.);
        assertEquals(-131.0, raoResult.getFlow(crac.getInstant("auto"), crac.getFlowCnec("NNL2AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - auto"), TwoSides.ONE, Unit.MEGAWATT), 1.);
        assertEquals(-131.0, raoResult.getFlow(crac.getInstant("curative"), crac.getFlowCnec("NNL2AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative"), TwoSides.ONE, Unit.MEGAWATT), 1.);
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

        raoParameters.getObjectiveFunctionParameters().setPreventiveStopCriterion(ObjectiveFunctionParameters.PreventiveStopCriterion.SECURE);
        raoParameters.getObjectiveFunctionParameters().setOptimizeCurativeIfPreventiveUnsecure(false);

        // Run RAO
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertEquals(Collections.emptySet(), raoResult.getActivatedNetworkActionsDuringState(crac.getState("Contingency FFR2AA1  FFR3AA1  1", crac.getLastInstant())));
    }

    @Test
    void curativeOptimizationShouldBeDoneIfPreventiveSecure() throws IOException {
        setup("small-network-2P.uct", "small-crac-to-check-curative-optimization-if-preventive-secure.json");
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json"));

        raoParameters.getObjectiveFunctionParameters().setPreventiveStopCriterion(ObjectiveFunctionParameters.PreventiveStopCriterion.SECURE);
        raoParameters.getObjectiveFunctionParameters().setOptimizeCurativeIfPreventiveUnsecure(false);

        // Run RAO
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertEquals(Set.of(crac.getNetworkAction("Open FFR1AA1  FFR4AA1  1")), raoResult.getActivatedNetworkActionsDuringState(crac.getState("Contingency FFR2AA1  FFR3AA1  1", crac.getLastInstant())));
    }

    @Test
    void curativeOptimizationShouldBeDoneIfPreventiveMinMarginNegative() throws IOException {
        setup("small-network-2P.uct", "small-crac-to-check-curative-optimization-if-preventive-secure.json");
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json"));

        raoParameters.getObjectiveFunctionParameters().setOptimizeCurativeIfPreventiveUnsecure(false);

        // Run RAO
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertEquals(Set.of(crac.getNetworkAction("Open FFR1AA1  FFR4AA1  1")), raoResult.getActivatedNetworkActionsDuringState(crac.getState("Contingency FFR2AA1  FFR3AA1  1", crac.getLastInstant())));
    }

    @Test
    void curativeOptimizationShouldBeDoneIfPreventiveUnsecureAndAssociatedParameterSet() throws IOException {
        setup("small-network-2P.uct", "small-crac-to-check-curative-optimization-if-preventive-secure.json");
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json"));

        raoParameters.getObjectiveFunctionParameters().setPreventiveStopCriterion(ObjectiveFunctionParameters.PreventiveStopCriterion.SECURE);
        raoParameters.getObjectiveFunctionParameters().setOptimizeCurativeIfPreventiveUnsecure(true);

        // Run RAO
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertEquals(Set.of(crac.getNetworkAction("Open FFR1AA1  FFR4AA1  1")), raoResult.getActivatedNetworkActionsDuringState(crac.getState("Contingency FFR2AA1  FFR3AA1  1", crac.getLastInstant())));
    }

    @Test
    void curativeOptimizationShouldBeDoneIfPreventiveSecureAndAssociatedParameterSet() throws IOException {
        setup("small-network-2P.uct", "small-crac-to-check-curative-optimization-if-preventive-secure.json");
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json"));

        raoParameters.getObjectiveFunctionParameters().setPreventiveStopCriterion(ObjectiveFunctionParameters.PreventiveStopCriterion.SECURE);
        raoParameters.getObjectiveFunctionParameters().setOptimizeCurativeIfPreventiveUnsecure(true);

        // Run RAO
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertEquals(Set.of(crac.getNetworkAction("Open FFR1AA1  FFR4AA1  1")), raoResult.getActivatedNetworkActionsDuringState(crac.getState("Contingency FFR2AA1  FFR3AA1  1", crac.getLastInstant())));
    }

    @Test
    void curativeOptimizationShouldBeDoneIfPreventiveMinMarginNegativeAndAssociatedParameterSet() throws IOException {
        setup("small-network-2P.uct", "small-crac-to-check-curative-optimization-if-preventive-secure.json");
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json"));

        raoParameters.getObjectiveFunctionParameters().setOptimizeCurativeIfPreventiveUnsecure(true);

        // Run RAO
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertEquals(Set.of(crac.getNetworkAction("Open FFR1AA1  FFR4AA1  1")), raoResult.getActivatedNetworkActionsDuringState(crac.getState("Contingency FFR2AA1  FFR3AA1  1", crac.getLastInstant())));
    }

    @Test
    void curativeStopCriterionReachedSkipsPerimeterBuilding() throws IOException {
        setup("small-network-2P.uct", "small-crac-purely-virtual-curative.json");
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_secure.json"));

        raoParameters.getObjectiveFunctionParameters().setOptimizeCurativeIfPreventiveUnsecure(true);

        // Run RAO, if not skipping, then tap to -15, since skipping, it stays at preventive optimization value (-12)
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertEquals(-12, raoResult.getOptimizedTapOnState(crac.getState("N-1 NL1-NL3", crac.getLastInstant()), crac.getPstRangeAction("CRA_PST_BE")));
    }

    @Test
    void catchDuringDataInitialization() throws IOException {
        setup("small-network-2P.uct", "small-crac-2P.json");
        RaoParameters raoParameters = null;
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertInstanceOf(FailedRaoResultImpl.class, raoResult);
        assertEquals("RAO failed during data initialization : Cannot invoke \"com.powsybl.openrao.raoapi.parameters.RaoParameters.getObjectiveFunctionParameters()\" because \"raoParameters\" is null", raoResult.getExecutionDetails());
    }

    @Test
    void catchDuringInitialSensitivity() throws IOException {
        setup("small-network-2P.uct", "small-crac-2P.json");
        RaoParameters raoParameters = Mockito.spy(new RaoParameters());
        when(raoParameters.getLoadFlowAndSensitivityParameters()).thenThrow(new OpenRaoException("Testing exception handling"));
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertInstanceOf(FailedRaoResultImpl.class, raoResult);
        assertEquals("RAO failed during initial sensitivity analysis : Testing exception handling", raoResult.getExecutionDetails());
    }

    @Test
    void catchDuringFirstPreventive() throws IOException {
        setup("small-network-2P.uct", "small-crac-2P.json");
        RaoParameters raoParameters = Mockito.spy(JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json")));
        when(raoParameters.getTopoOptimizationParameters()).thenThrow(new OpenRaoException("Testing exception handling"));

        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertEquals("RAO failed during first preventive : Testing exception handling", raoResult.getExecutionDetails());
    }

    @Test
    void catchDuringContingencyScenarios() throws IOException {
        setup("small-network-2P.uct", "small-crac-2P.json");
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json"));
        MultithreadingParameters multithreadingParameters = Mockito.spy(raoParameters.getMultithreadingParameters());
        when(multithreadingParameters.getContingencyScenariosInParallel()).thenThrow(new OpenRaoException("Testing exception handling"));
        raoParameters.setMultithreadingParameters(multithreadingParameters);

        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertEquals("RAO failed during contingency scenarios : Testing exception handling", raoResult.getExecutionDetails());
    }

    @Test
    void catchDuringSecondPreventive() throws IOException {
        setup("small-network-2P.uct", "small-crac-2P.json");
        RaoParameters raoParameters = Mockito.spy(JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json")));
        when(raoParameters.getSecondPreventiveRaoParameters()).thenThrow(new OpenRaoException("Testing exception handling"));

        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertEquals("RAO failed during second preventive optimization : Testing exception handling", raoResult.getExecutionDetails());
    }
}
