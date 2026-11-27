/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.craccreator;

import com.powsybl.openrao.data.crac.io.nc.craccreator.constants.NcVersion;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class NcVersionDetectorTest {

    @Test
    void packageWithoutAnySignalIsDetectedAsV24() {
        assertEquals(NcVersion.V24, NcVersionDetector.detectVersion(Set.of(), Set.of(), Set.of()));
    }

    @Test
    void packageWithOnlyV24ConformsToIsDetectedAsV24() {
        assertEquals(NcVersion.V24, NcVersionDetector.detectVersion(Set.of(), Set.of(), Set.of("CO.xml")));
    }

    @Test
    void packageWithNcp25VocabularyNamespacesIsDetectedAsV25() {
        assertEquals(NcVersion.V25, NcVersionDetector.detectVersion(Set.of("CO-v2.5.rdf"), Set.of(), Set.of()));
    }

    @Test
    void packageWithOnlyNcp25ConformsToIsDetectedAsV25() {
        assertEquals(NcVersion.V25, NcVersionDetector.detectVersion(Set.of(), Set.of("CO.xml"), Set.of()));
    }

    @Test
    void packageWithConflictingSignalsIsDetectedAsV25() {
        assertEquals(NcVersion.V25, NcVersionDetector.detectVersion(Set.of(), Set.of("CO.xml"), Set.of("CO.xml")));
    }
}
