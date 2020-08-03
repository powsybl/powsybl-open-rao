/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_loopflow_extension;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.threshold.AbsoluteFlowThreshold;
import com.farao_community.farao.data.crac_impl.threshold.RelativeFlowThreshold;
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
        SimpleCrac simpleCrac = new SimpleCrac("cracId");

        // States
        Instant initialInstant = simpleCrac.addInstant("N", 0);
        State preventiveState = simpleCrac.addState(null, initialInstant);

        // Cnecs
        simpleCrac.addNetworkElement("ne1");
        simpleCrac.addNetworkElement("ne2");
        simpleCrac.addNetworkElement("ne3");
        simpleCrac.addCnec("cnec1", "ne1", Collections.singleton(new AbsoluteFlowThreshold(Unit.AMPERE, Side.LEFT, Direction.OPPOSITE, 500)), preventiveState.getId());
        simpleCrac.addCnec("cnec2", "ne2", Collections.singleton(new RelativeFlowThreshold(Side.LEFT, Direction.OPPOSITE, 30)), preventiveState.getId());
        simpleCrac.addCnec("cnec3", "ne3", Collections.singleton(new AbsoluteFlowThreshold(Unit.MEGAWATT, Side.LEFT, Direction.BOTH, 700)), preventiveState.getId());

        // LoopFlowExtensions
        simpleCrac.getCnec("cnec1").addExtension(CnecLoopFlowExtension.class, new CnecLoopFlowExtension(100, Unit.AMPERE));
        simpleCrac.getCnec("cnec2").addExtension(CnecLoopFlowExtension.class, new CnecLoopFlowExtension(30, Unit.PERCENT_IMAX));

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
        assertNotNull(simpleCrac.getCnec("cnec1").getExtension(CnecLoopFlowExtension.class));
        assertEquals(100, simpleCrac.getCnec("cnec1").getExtension(CnecLoopFlowExtension.class).getInputThreshold(), 0.1);
        assertEquals(Unit.AMPERE, simpleCrac.getCnec("cnec1").getExtension(CnecLoopFlowExtension.class).getInputThresholdUnit());

        assertNotNull(simpleCrac.getCnec("cnec2").getExtension(CnecLoopFlowExtension.class));
        assertEquals(30, simpleCrac.getCnec("cnec2").getExtension(CnecLoopFlowExtension.class).getInputThreshold(), 0.1);
        assertEquals(Unit.PERCENT_IMAX, simpleCrac.getCnec("cnec2").getExtension(CnecLoopFlowExtension.class).getInputThresholdUnit());

        assertNull(simpleCrac.getCnec("cnec3").getExtension(CnecLoopFlowExtension.class));
    }
}
