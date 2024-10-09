/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.commons.api;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.ejml.UtilEjml.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class StandardElementaryCreationContextTest {
    @Test
    void testInitWithComprehensiveData() {
        StandardElementaryCreationContext context = new StandardElementaryCreationContext("nativeId", "nativeName", "createdId", ImportStatus.IMPORTED, "", false);
        assertTrue(context.isImported());
        assertEquals("nativeId", context.getNativeObjectId());
        assertEquals("nativeName", context.getNativeObjectName());
        assertEquals("createdId", context.getCreatedObjectId());
        assertEquals(Set.of("createdId"), context.getCreatedObjectsIds());
        assertFalse(context.isAltered());
        assertEquals(ImportStatus.IMPORTED, context.getImportStatus());
        assertEquals("", context.getImportStatusDetail());
    }

    @Test
    void testInitWithoutNativeName() {
        StandardElementaryCreationContext context = new StandardElementaryCreationContext("nativeId", null, "createdId", ImportStatus.IMPORTED, "Details.", true);
        assertTrue(context.isImported());
        assertEquals("nativeId", context.getNativeObjectId());
        assertEquals("nativeId", context.getNativeObjectName());
        assertEquals("createdId", context.getCreatedObjectId());
        assertEquals(Set.of("createdId"), context.getCreatedObjectsIds());
        assertTrue(context.isAltered());
        assertEquals(ImportStatus.IMPORTED, context.getImportStatus());
        assertEquals("Details.", context.getImportStatusDetail());
    }

    @Test
    void testImported() {
        StandardElementaryCreationContext context = StandardElementaryCreationContext.imported("nativeId", "nativeName", "createdId", true, "Details.");
        assertTrue(context.isImported());
        assertEquals("nativeId", context.getNativeObjectId());
        assertEquals("nativeName", context.getNativeObjectName());
        assertEquals("createdId", context.getCreatedObjectId());
        assertEquals(Set.of("createdId"), context.getCreatedObjectsIds());
        assertTrue(context.isAltered());
        assertEquals(ImportStatus.IMPORTED, context.getImportStatus());
        assertEquals("Details.", context.getImportStatusDetail());
    }

    @Test
    void testNotImported() {
        StandardElementaryCreationContext context = StandardElementaryCreationContext.notImported("nativeId", "nativeName", ImportStatus.NOT_FOR_RAO, "Details.");
        assertFalse(context.isImported());
        assertEquals("nativeId", context.getNativeObjectId());
        assertEquals("nativeName", context.getNativeObjectName());
        assertNull(context.getCreatedObjectId());
        assertTrue(context.getCreatedObjectsIds().isEmpty());
        assertFalse(context.isAltered());
        assertEquals(ImportStatus.NOT_FOR_RAO, context.getImportStatus());
        assertEquals("Details.", context.getImportStatusDetail());
    }
}
