package com.farao_community.farao.data.rao_result_json;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.data.rao_result_impl.*;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class RaoResultExporterTest {

    @Test
    public void testExport() throws FileNotFoundException {

        Crac crac = CommonCracCreation.createWithCurativePstRange();

        NetworkAction na = crac.newNetworkAction().withId("na-id")
            .newTopologicalAction().withNetworkElement("any").withActionType(ActionType.OPEN).add()
            .newFreeToUseUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
            .newFreeToUseUsageRule().withInstant(Instant.CURATIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        RaoResultImpl raoResult = new RaoResultImpl();

        FlowCnecResult flowCnecResult = raoResult.getAndCreateIfAbsentFlowCnecResult(crac.getFlowCnec("cnec1basecase"));

        flowCnecResult.getAndCreateIfAbsentResultForOptimizationState(OptimizationState.INITIAL);
        ElementaryFlowCnecResult elementaryFlowCnecResult = flowCnecResult.getResult(OptimizationState.INITIAL);

        elementaryFlowCnecResult.setFlow(100., Unit.MEGAWATT);
        elementaryFlowCnecResult.setMargin(101., Unit.MEGAWATT);
        elementaryFlowCnecResult.setRelativeMargin(102., Unit.MEGAWATT);
        elementaryFlowCnecResult.setLoopFlow(103., Unit.MEGAWATT);
        elementaryFlowCnecResult.setCommercialFlow(104., Unit.MEGAWATT);

        elementaryFlowCnecResult.setFlow(110., Unit.AMPERE);
        elementaryFlowCnecResult.setMargin(111., Unit.AMPERE);
        elementaryFlowCnecResult.setRelativeMargin(112., Unit.AMPERE);
        elementaryFlowCnecResult.setLoopFlow(113., Unit.AMPERE);
        elementaryFlowCnecResult.setCommercialFlow(114., Unit.AMPERE);

        elementaryFlowCnecResult.setPtdfZonalSum(0.1);

        flowCnecResult.getAndCreateIfAbsentResultForOptimizationState(OptimizationState.AFTER_CRA);
        elementaryFlowCnecResult = flowCnecResult.getResult(OptimizationState.AFTER_CRA);

        elementaryFlowCnecResult.setFlow(200., Unit.MEGAWATT);
        elementaryFlowCnecResult.setMargin(201., Unit.MEGAWATT);
        elementaryFlowCnecResult.setRelativeMargin(202., Unit.MEGAWATT);
        elementaryFlowCnecResult.setLoopFlow(203., Unit.MEGAWATT);

        elementaryFlowCnecResult.setFlow(210., Unit.AMPERE);
        elementaryFlowCnecResult.setMargin(211., Unit.AMPERE);
        elementaryFlowCnecResult.setRelativeMargin(212., Unit.AMPERE);
        elementaryFlowCnecResult.setLoopFlow(213., Unit.AMPERE);

        elementaryFlowCnecResult.setPtdfZonalSum(0.1);

        raoResult.getAndCreateIfAbsentNetworkActionResult(na).addActivationForState(crac.getState("Contingency FR1 FR3", Instant.CURATIVE));
        raoResult.getAndCreateIfAbsentNetworkActionResult(na).addActivationForState(crac.getState("Contingency FR1 FR2", Instant.CURATIVE));

        PstRangeActionResult pstRangeActionResult = raoResult.getAndCreateIfAbsentPstRangeActionResult(crac.getPstRangeAction("pst"));
        pstRangeActionResult.setInitialTap(3);
        pstRangeActionResult.setInitialSetPoint(2.3);
        pstRangeActionResult.addActivationForState(crac.getPreventiveState(), -7, -3.2);

        CostResult costResult = raoResult.getAndCreateIfAbsentCostResult(OptimizationState.INITIAL);
        costResult.setFunctionalCost(100.);
        costResult.setVirtualCost("loopFlow", 0.);
        costResult.setVirtualCost("MNEC", 0.);

        costResult = raoResult.getAndCreateIfAbsentCostResult(OptimizationState.AFTER_CRA);
        costResult.setFunctionalCost(-50.);
        costResult.setVirtualCost("loopFlow", 10.);
        costResult.setVirtualCost("MNEC", 2.);

        raoResult.setComputationStatus(ComputationStatus.DEFAULT);

        new RaoResultExporter().export(raoResult, crac, new FileOutputStream(new File("/tmp/raoResult.json")));

        //RaoResult importedRaoResult = new RaoResultImporter().importRaoResult(new FileInputStream(new File("/tmp/raoResult.json")), crac);

        System.out.println("coucou");
    }

}
