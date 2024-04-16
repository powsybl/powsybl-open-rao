package com.powsybl.openrao.searchtreerao.commons.optimizationperimeters;

import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.*;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.cracimpl.utils.NetworkImportsUtil;
import com.powsybl.openrao.data.cracloopflowextension.LoopFlowThresholdAdder;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

abstract class AbstractOptimizationPerimeterTest {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";

    protected Network network;
    protected Crac crac;
    protected State pState;
    protected State oState1;
    protected State oState2;
    protected State cState1;
    protected State cState2;
    protected FlowCnec pCnec;
    protected FlowCnec oCnec1;
    protected FlowCnec oCnec2;
    protected FlowCnec cCnec1;
    protected FlowCnec cCnec2;
    protected RangeAction<?> pRA;
    protected RangeAction<?> cRA;
    protected RemedialAction<?> pNA;
    protected RemedialAction<?> cNA;
    protected RaoParameters raoParameters;
    protected PrePerimeterResult prePerimeterResult;

    @BeforeEach
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
        raoParameters = new RaoParameters();

        crac = CracFactory.findDefault().create("cracId")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT_ID, InstantKind.OUTAGE)
            .newInstant(AUTO_INSTANT_ID, InstantKind.AUTO)
            .newInstant(CURATIVE_INSTANT_ID, InstantKind.CURATIVE);
        Instant outageInstant = crac.getInstant(OUTAGE_INSTANT_ID);
        Instant curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);
        crac.newContingency().withId("outage-1").withContingencyElement("FFR1AA1  FFR3AA1  1", ContingencyElementType.LINE).add();
        crac.newContingency().withId("outage-2").withContingencyElement("FFR2AA1  DDE3AA1  1", ContingencyElementType.LINE).add();

        // one preventive CNEC
        pCnec = crac.newFlowCnec()
            .withId("cnec-prev")
            .withNetworkElement("BBE2AA1  FFR3AA1  1")
            .withInstant(PREVENTIVE_INSTANT_ID)
            .withOptimized(true)
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(500.).withMin(-500.).withSide(Side.LEFT).add()
            .add();

        // one outage CNEC for each CO
        oCnec1 = crac.newFlowCnec()
            .withId("cnec-co-outage-1")
            .withNetworkElement("FFR2AA1  FFR3AA1  1")
            .withInstant(OUTAGE_INSTANT_ID)
            .withContingency("outage-1")
            .withMonitored(true)
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(500.).withMin(-500.).withSide(Side.LEFT).add()
            .add();
        oCnec1.newExtension(LoopFlowThresholdAdder.class).withUnit(Unit.MEGAWATT).withValue(100.).add();

        oCnec2 = crac.newFlowCnec()
            .withId("cnec-co-outage-2")
            .withNetworkElement("FFR2AA1  FFR3AA1  1")
            .withInstant(OUTAGE_INSTANT_ID)
            .withContingency("outage-2")
            .withOptimized(true).withMonitored(true)
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(500.).withMin(-500.).withSide(Side.LEFT).add()
            .add();

        cCnec1 = crac.newFlowCnec()
            .withId("cnec-co-curative-1")
            .withNetworkElement("BBE2AA1  FFR3AA1  1")
            .withInstant(CURATIVE_INSTANT_ID)
            .withContingency("outage-1")
            .withMonitored(true)
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(500.).withMin(-500.).withSide(Side.LEFT).add()
            .add();

        cCnec2 = crac.newFlowCnec()
            .withId("cnec-co-curative-2")
            .withNetworkElement("BBE2AA1  FFR3AA1  1")
            .withInstant(CURATIVE_INSTANT_ID)
            .withContingency("outage-2")
            .withOptimized(true)
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(500.).withMin(-500.).withSide(Side.LEFT).add()
            .add();
        cCnec2.newExtension(LoopFlowThresholdAdder.class).withUnit(Unit.MEGAWATT).withValue(100.).add();

        // one preventive range action and one curative
        pRA = crac.newInjectionRangeAction().withId("preventive-ra")
            .withNetworkElementAndKey(1, "BBE2AA1 _generator")
            .newRange().withMin(-1000).withMax(1000).add()
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        cRA = crac.newInjectionRangeAction().withId("curative-ra")
            .withNetworkElementAndKey(1, "BBE2AA1 _generator")
            .newRange().withMin(-1000).withMax(1000).add()
            .newOnContingencyStateUsageRule().withInstant(CURATIVE_INSTANT_ID).withContingency("outage-1").withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        // one preventive network action and one curative
        pNA = crac.newNetworkAction().withId("preventive-na")
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .newTerminalsConnectionAction().withActionType(ActionType.OPEN).withNetworkElement("BBE2AA1  FFR3AA1  1").add()
            .add();

        cNA = crac.newNetworkAction().withId("curative-na")
            .withName("complexNetworkActionName")
            .newTerminalsConnectionAction().withActionType(ActionType.OPEN).withNetworkElement("BBE2AA1  FFR3AA1  1").add()
            .newOnContingencyStateUsageRule().withInstant(CURATIVE_INSTANT_ID).withContingency("outage-1").withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        pState = crac.getPreventiveState();
        oState1 = crac.getState("outage-1", outageInstant);
        oState2 = crac.getState("outage-2", outageInstant);
        cState1 = crac.getState("outage-1", curativeInstant);
        cState2 = crac.getState("outage-2", curativeInstant);

        prePerimeterResult = Mockito.mock(PrePerimeterResult.class);
    }
}
