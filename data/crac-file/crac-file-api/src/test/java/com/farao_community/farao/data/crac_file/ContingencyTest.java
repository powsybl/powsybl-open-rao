/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_file;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Test of equal function in Contingency
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class ContingencyTest {

    @Test
    public void testContingencyEqual() {
        ContingencyElement contingencyElement = new ContingencyElement("elementId", "name");
        MonitoredBranch monitoredBranch = new MonitoredBranch("id", "name", "branchId", 10);
        Contingency contingency = new Contingency("id", "name", Arrays.asList(monitoredBranch), Arrays.asList(contingencyElement));
        Contingency contingencySame = new Contingency("id", "name", Arrays.asList(monitoredBranch), Arrays.asList(contingencyElement));

        assertEquals(true, contingency.equals(contingencySame));
    }

    @Test
    public void testContingencyNotEqual() {
        ContingencyElement contingencyElement = new ContingencyElement("elementId", "name");
        MonitoredBranch monitoredBranch = new MonitoredBranch("id", "name", "branchId", 10);
        Contingency contingency = new Contingency("id", "name", Arrays.asList(monitoredBranch), Arrays.asList(contingencyElement));
        Contingency contingencyOther = new Contingency("idOther", "nameOther", Arrays.asList(monitoredBranch), Arrays.asList(contingencyElement));

        assertEquals(false, contingency.equals(contingencyOther));
    }

    @Test
    public void testContingencyHashEqual() {
        ContingencyElement contingencyElement = new ContingencyElement("elementId", "name");
        MonitoredBranch monitoredBranch = new MonitoredBranch("id", "name", "branchId", 10);
        Contingency contingency = new Contingency("id", "name", Arrays.asList(monitoredBranch), Arrays.asList(contingencyElement));
        Contingency contingencySame = new Contingency("id", "name", Arrays.asList(monitoredBranch), Arrays.asList(contingencyElement));

        assertEquals(contingency.hashCode(), contingencySame.hashCode());
    }

    @Test
    public void testContingencyHashNotEqual() {
        ContingencyElement contingencyElement = new ContingencyElement("elementId", "name");
        MonitoredBranch monitoredBranch = new MonitoredBranch("id", "name", "branchId", 10);
        Contingency contingency = new Contingency("id", "name", Arrays.asList(monitoredBranch), Arrays.asList(contingencyElement));
        Contingency contingencyOther = new Contingency("idOther", "nameOther", Arrays.asList(monitoredBranch), Arrays.asList(contingencyElement));

        assertFalse(contingency.hashCode() == contingencyOther.hashCode());
    }
}
