package com.farao_community.farao.data.crac_io_json;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.CracFactory;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.TapConvention;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.range_action.RangeType;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.Assert.assertEquals;

public class JsonExportTest {

    @Test
    public void test() {

        Crac crac = CracFactory.findDefault().create("cracId", "cracName");

        crac.newContingency()
                .withId("CO_00001")
                .withName("[CO] contingency on branch A-B")
                .withNetworkElement("networkElement1Id", "branch A-B")
                .add();

        crac.newContingency()
                .withId("CO_00002")
                .withNetworkElement("networkElement2Id", "name2")
                .withNetworkElement("networkElement3Id")
                .add();

        crac.newFlowCnec()
                .withId("cnec-1-id")
                .withName("cnec-1-name")
                //.withNetworkElement("networkElement1Id") // chiant, on doit mettre le name sinon il est considéré différent de celui qui existe déjà
                .withNetworkElement("networkElement1Id", "branch A-B")
                .withOperator("FR")
                .withInstant(Instant.CURATIVE)
                .withContingency("CO_00002")
                .withOptimized(true)
                .withMonitored(false)
                .withReliabilityMargin(10.0)
                .newThreshold().withMax(1000.).withRule(BranchThresholdRule.ON_RIGHT_SIDE).withUnit(Unit.AMPERE).add()
                .newThreshold().withMin(-500.).withRule(BranchThresholdRule.ON_LEFT_SIDE).withUnit(Unit.AMPERE).add()
                .add();

        crac.newFlowCnec()
                .withId("cnec-2-id")
                .withName("cnec-1-name")
                //.withNetworkElement("networkElement1Id") // chiant, on doit mettre le name sinon il est considéré différent de celui qui existe déjà
                .withNetworkElement("networkElement1Id", "branch A-B")
                .withOperator("FR")
                .withInstant(Instant.CURATIVE)
                .withContingency("CO_00002")
                .withOptimized(true)
                .withMonitored(false)
                .withReliabilityMargin(10.0)
                .newThreshold().withMax(1000.).withRule(BranchThresholdRule.ON_RIGHT_SIDE).withUnit(Unit.AMPERE).add()
                .newThreshold().withMin(-500.).withRule(BranchThresholdRule.ON_LEFT_SIDE).withUnit(Unit.AMPERE).add()
                .add();

        crac.newPstRangeAction()
                .withId("id1")
                .withOperator("BE")
                .withNetworkElement("pst_network_element")
                .withGroupId("groupId1")
                .newPstRange().withMinTap(-10).withMaxTap(10).withTapConvention(TapConvention.CENTERED_ON_ZERO).withRangeType(RangeType.ABSOLUTE).add()
                .newPstRange().withMaxTap(20).withTapConvention(TapConvention.STARTS_AT_ONE).withRangeType(RangeType.RELATIVE_TO_INITIAL_NETWORK).add()
                .newFreeToUseUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
                .newOnStateUsageRule().withInstant(Instant.CURATIVE).withUsageMethod(UsageMethod.FORCED).withContingency("CO_00001").add()
                .add();

        crac.newNetworkAction()
                .withId("networkActionId")
                .withName("networkActionName")
                .withOperator("operator")
                .newTopologicalAction().withNetworkElement("branchNetworkElementId").withActionType(ActionType.OPEN).add()
                .newTopologicalAction().withNetworkElement("branchNetworkElement2Id").withActionType(ActionType.CLOSE).add()
                .newPstSetPoint().withNetworkElement("pstpst").withSetpoint(0).withTapConvention(TapConvention.CENTERED_ON_ZERO).add()
                .newInjectionSetPoint().withNetworkElement("injection").withSetpoint(15.).add()
                .add();

        crac.newNetworkAction()
                .withId("networkActionId2")
                .withName("networkActionName")
                .withOperator("operator")
                .newTopologicalAction().withNetworkElement("branchNetworkElement3Id", "na").withActionType(ActionType.OPEN).add()
                .add();

        OutputStream os = new ByteArrayOutputStream();
        new JsonExport().exportCrac(crac, os);

        InputStream is = new ByteArrayInputStream(os.toString().getBytes());
        Crac cracImported = new JsonImport().importCrac(is, null);

        OutputStream os2 = new ByteArrayOutputStream();
        new JsonExport().exportCrac(cracImported, os2);

        assertEquals(os.toString(), os2.toString());
    }

}
