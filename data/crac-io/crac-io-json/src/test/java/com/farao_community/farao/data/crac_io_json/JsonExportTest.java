package com.farao_community.farao.data.crac_io_json;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.CracFactory;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

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
            .withNetworkElement("networkElement2Id")
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
                .newThreshold().withMin(-100.).withMax(1000.).withRule(BranchThresholdRule.ON_RIGHT_SIDE).withUnit(Unit.AMPERE).add()
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
                .newThreshold().withMin(-100.).withMax(1000.).withRule(BranchThresholdRule.ON_RIGHT_SIDE).withUnit(Unit.AMPERE).add()
                .newThreshold().withMin(-500.).withRule(BranchThresholdRule.ON_LEFT_SIDE).withUnit(Unit.AMPERE).add()
                .add();

        OutputStream os = new ByteArrayOutputStream();
        new JsonExport().exportCrac(crac, os);

        InputStream is = new ByteArrayInputStream(os.toString().getBytes());
        Crac cracImported = new JsonImport().importCrac(is, null);

        System.out.println("coucou");

    }

}
