/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_loopflow_extension;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_io_api.CracExporters;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class JsonCnecLoopFlowExtensionImportExportTest {

    @Test
    public void roundTripTest() {
        // Crac
        SimpleCrac simpleCrac = new SimpleCrac("cracId", "cracName", Collections.emptySet());

        simpleCrac.newBranchCnec()
            .setId("cnec1")
            .newNetworkElement().setId("ne1").add()
            .setInstant(Instant.PREVENTIVE)
            .newThreshold().setRule(BranchThresholdRule.ON_LEFT_SIDE).setUnit(Unit.AMPERE).setMin(-500.).add()
            .add();
        simpleCrac.getBranchCnec("cnec1").addExtension(CnecLoopFlowExtension.class, new CnecLoopFlowExtension(100, Unit.AMPERE));

        simpleCrac.newBranchCnec()
            .setId("cnec2")
            .newNetworkElement().setId("ne2").add()
            .setInstant(Instant.PREVENTIVE)
            .newThreshold().setRule(BranchThresholdRule.ON_LEFT_SIDE).setUnit(Unit.PERCENT_IMAX).setMin(-0.3).add()
            .add();
        simpleCrac.getBranchCnec("cnec2").addExtension(CnecLoopFlowExtension.class, new CnecLoopFlowExtension(30, Unit.PERCENT_IMAX));

        simpleCrac.newBranchCnec()
            .setId("cnec3")
            .newNetworkElement().setId("ne3").add()
            .setInstant(Instant.PREVENTIVE)
            .newThreshold().setRule(BranchThresholdRule.ON_LEFT_SIDE).setUnit(Unit.MEGAWATT).setMin(-700.).setMax(700.).add()
            .add();

        // export Crac
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CracExporters.exportCrac(simpleCrac, "Json", outputStream);

        // import Crac
        Crac importedCrac;
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray())) {
            importedCrac = CracImporters.importCrac("unknown.json", inputStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // test
        assertNotNull(simpleCrac.getBranchCnec("cnec1").getExtension(CnecLoopFlowExtension.class));
        assertEquals(100, simpleCrac.getBranchCnec("cnec1").getExtension(CnecLoopFlowExtension.class).getInputThreshold(), 0.1);
        assertEquals(Unit.AMPERE, simpleCrac.getBranchCnec("cnec1").getExtension(CnecLoopFlowExtension.class).getInputThresholdUnit());

        assertNotNull(simpleCrac.getBranchCnec("cnec2").getExtension(CnecLoopFlowExtension.class));
        assertEquals(30, simpleCrac.getBranchCnec("cnec2").getExtension(CnecLoopFlowExtension.class).getInputThreshold(), 0.1);
        assertEquals(Unit.PERCENT_IMAX, simpleCrac.getBranchCnec("cnec2").getExtension(CnecLoopFlowExtension.class).getInputThresholdUnit());

        assertNull(simpleCrac.getBranchCnec("cnec3").getExtension(CnecLoopFlowExtension.class));
    }
}
