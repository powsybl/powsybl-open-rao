package com.farao_community.farao.data.crac_io_json;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.CracFactory;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
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

        OutputStream os = new ByteArrayOutputStream();
        new JsonExport().exportCrac(crac, os);

        System.out.println("coucou");

    }

}
