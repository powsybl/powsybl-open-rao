/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_loopflow_extension;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_impl.CracImpl;
import com.farao_community.farao.data.crac_io_api.CracExporters;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import static junit.framework.TestCase.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class JsonLoopFlowThresholdImplImportExportTest {

    @Test
    public void roundTripTest() {
        Crac crac = new CracImpl("cracId");

        crac.newFlowCnec()
                .withId("cnec1")
                .withNetworkElement("ne1")
                .withInstant(Instant.PREVENTIVE)
                .newThreshold().withRule(BranchThresholdRule.ON_LEFT_SIDE).withUnit(Unit.AMPERE).withMin(-500.).add()
                .add()
                .newExtension(LoopFlowThresholdAdder.class).withValue(100).withUnit(Unit.AMPERE).add();

        crac.newFlowCnec()
                .withId("cnec2")
                .withNetworkElement("ne2")
                .withInstant(Instant.PREVENTIVE)
                .newThreshold().withRule(BranchThresholdRule.ON_LEFT_SIDE).withUnit(Unit.PERCENT_IMAX).withMin(-0.3).add()
                .add()
                .newExtension(LoopFlowThresholdAdder.class).withValue(.3).withUnit(Unit.PERCENT_IMAX).add();

        crac.newFlowCnec()
                .withId("cnec3")
                .withNetworkElement("ne3")
                .withInstant(Instant.PREVENTIVE)
                .newThreshold().withRule(BranchThresholdRule.ON_LEFT_SIDE).withUnit(Unit.MEGAWATT).withMin(-700.).withMax(700.).add()
                .add();

        // export Crac
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CracExporters.exportCrac(crac, "Json", outputStream);

        // import Crac
        Crac importedCrac;
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray())) {
            importedCrac = CracImporters.importCrac("whatever.json", inputStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // test
        assertNotNull(importedCrac.getFlowCnec("cnec1").getExtension(LoopFlowThreshold.class));
        assertEquals(100, importedCrac.getFlowCnec("cnec1").getExtension(LoopFlowThreshold.class).getValue(), 0.1);
        assertEquals(Unit.AMPERE, importedCrac.getFlowCnec("cnec1").getExtension(LoopFlowThreshold.class).getUnit());

        assertNotNull(importedCrac.getFlowCnec("cnec2").getExtension(LoopFlowThreshold.class));
        assertEquals(.3, importedCrac.getFlowCnec("cnec2").getExtension(LoopFlowThreshold.class).getValue(), 0.1);
        assertEquals(Unit.PERCENT_IMAX, importedCrac.getFlowCnec("cnec2").getExtension(LoopFlowThreshold.class).getUnit());

        assertNull(importedCrac.getFlowCnec("cnec3").getExtension(LoopFlowThreshold.class));
    }
}
