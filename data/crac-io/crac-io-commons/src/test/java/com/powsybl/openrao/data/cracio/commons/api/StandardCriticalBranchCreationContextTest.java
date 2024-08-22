/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.commons.api;

import org.apache.commons.lang3.NotImplementedException;
import com.powsybl.openrao.data.cracio.commons.api.stdcreationcontext.NativeBranch;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.ejml.UtilEjml.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class StandardCriticalBranchCreationContextTest {
    private final NativeBranch nativeBranch = new NativeBranch("from", "to", "suffix");
    private final Map<String, String> createdCnecsIds = Map.of("preventive", "preventiveCnec", "curative", "curativeCnec");

    @Test
    void testInitWithComprehensiveData() {
        StandardCriticalBranchCreationContext context = new StandardCriticalBranchCreationContext("criticalBranchId", nativeBranch, false, "contingency", createdCnecsIds, false, ImportStatus.IMPORTED, "Details.");
        assertTrue(context.isImported());
        assertEquals("criticalBranchId", context.getNativeObjectId());
        assertEquals("criticalBranchId", context.getNativeObjectName());
        assertEquals("Several objects may have been created. Please use getCreatedCnecsIds() instead.", assertThrows(NotImplementedException.class, context::getCreatedObjectId).getMessage());
        assertEquals(Set.of("preventiveCnec", "curativeCnec"), context.getCreatedObjectsIds());
        assertFalse(context.isAltered());
        assertEquals(ImportStatus.IMPORTED, context.getImportStatus());
        assertEquals("Details.", context.getImportStatusDetail());
        assertEquals(nativeBranch, context.getNativeBranch());
        assertFalse(context.isBaseCase());
        assertEquals(Optional.of("contingency"), context.getContingencyId());
        assertFalse(context.isDirectionInvertedInNetwork());
    }

    @Test
    void testInitWithComprehensiveDataBaseCase() {
        StandardCriticalBranchCreationContext context = new StandardCriticalBranchCreationContext("criticalBranchId", nativeBranch, true, "contingency", createdCnecsIds, false, ImportStatus.IMPORTED, "Details.");
        assertTrue(context.isImported());
        assertEquals("criticalBranchId", context.getNativeObjectId());
        assertEquals("criticalBranchId", context.getNativeObjectName());
        assertEquals("Several objects may have been created. Please use getCreatedCnecsIds() instead.", assertThrows(NotImplementedException.class, context::getCreatedObjectId).getMessage());
        assertEquals(Set.of("preventiveCnec", "curativeCnec"), context.getCreatedObjectsIds());
        assertFalse(context.isAltered());
        assertEquals(ImportStatus.IMPORTED, context.getImportStatus());
        assertEquals("Details.", context.getImportStatusDetail());
        assertEquals(nativeBranch, context.getNativeBranch());
        assertTrue(context.isBaseCase());
        assertTrue(context.getContingencyId().isEmpty());
        assertFalse(context.isDirectionInvertedInNetwork());
    }

    @Test
    void testInitWithoutContingency() {
        StandardCriticalBranchCreationContext context = new StandardCriticalBranchCreationContext("criticalBranchId", nativeBranch, false, null, createdCnecsIds, false, ImportStatus.IMPORTED, "Details.");
        assertTrue(context.isImported());
        assertEquals("criticalBranchId", context.getNativeObjectId());
        assertEquals("criticalBranchId", context.getNativeObjectName());
        assertEquals("Several objects may have been created. Please use getCreatedCnecsIds() instead.", assertThrows(NotImplementedException.class, context::getCreatedObjectId).getMessage());
        assertEquals(Set.of("preventiveCnec", "curativeCnec"), context.getCreatedObjectsIds());
        assertFalse(context.isAltered());
        assertEquals(ImportStatus.IMPORTED, context.getImportStatus());
        assertEquals("Details.", context.getImportStatusDetail());
        assertEquals(nativeBranch, context.getNativeBranch());
        assertFalse(context.isBaseCase());
        assertTrue(context.getContingencyId().isEmpty());
        assertFalse(context.isDirectionInvertedInNetwork());
    }
}
