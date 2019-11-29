/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_file;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test of equal function in ContingencyElement
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class ContingencyElementTest {

    @Test
    public void testContingencyElementEqual() {
        ContingencyElement contingencyElement = new ContingencyElement("elementId", "name");
        ContingencyElement contingencyElementSame = new ContingencyElement("elementId", "name");

        assertEquals(true, contingencyElement.equals(contingencyElementSame));
    }

    @Test
    public void testContingencyElementNotEqual() {
        ContingencyElement contingencyElement = new ContingencyElement("elementId", "name");
        ContingencyElement contingencyElementOther = new ContingencyElement("elementIdOther", "nameOther");

        assertEquals(false, contingencyElement.equals(contingencyElementOther));
    }

    @Test
    public void testContingencyElementHashEqual() {
        ContingencyElement contingencyElement = new ContingencyElement("elementId", "name");
        ContingencyElement contingencyElementSame = new ContingencyElement("elementId", "name");

        assertEquals(contingencyElement.hashCode(), contingencyElementSame.hashCode());
    }

    @Test
    public void testContingencyElementHashNotEqual() {
        ContingencyElement contingencyElement = new ContingencyElement("elementId", "name");
        ContingencyElement contingencyElementOther = new ContingencyElement("elementIdOther", "nameOther");

        assertFalse(contingencyElement.hashCode() == contingencyElementOther.hashCode());
    }
}
