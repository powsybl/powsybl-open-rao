/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_file.xlsx.model;

import com.farao_community.farao.data.crac_file.xlsx.ExcelReader;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit Tests for the ContingencyElement reader class
 *
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
public class MonitoredBranchXlsxReaderTest {

    private static final String BRANCH_CBCO = "Branch_CBCO";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void shouldExtractMonitoredBranchFromFile20170215XlsxCracFrV01V23() {
        //Given, When and Action
        List<MonitoredBranchXlsx> monitoredBranches = ExcelReader.of(MonitoredBranchXlsx.class)
                .from(MonitoredBranchXlsxReaderTest.class.getResourceAsStream("/20170215_xlsx_crac_fr_v01_v2.3.xlsx"))
                .skipHeaderRow(true)
                .sheet(BRANCH_CBCO)
                .list();

        //Asserts
        assertNotNull(monitoredBranches);
        assertFalse(monitoredBranches.isEmpty());
        assertEquals(8, monitoredBranches.size());

        MonitoredBranchXlsx monitoredBranch1 = monitoredBranches.get(0);
        assertEquals("France-Germany 1", monitoredBranch1.getUniqueCbcoName());
        assertEquals(ElementDescriptionMode.ORDER_CODE, monitoredBranch1.getDescriptionMode());
        assertEquals(Activation.YES, monitoredBranch1.getActivation());
        assertEquals(Tso.N_RTE, monitoredBranch1.getTso());
        assertEquals("FFR2AA1", monitoredBranch1.getUctNodeFrom());
        assertEquals("DDE5AA1", monitoredBranch1.getUctNodeTo());
        assertEquals(null, monitoredBranch1.getUniqueCOName());
        assertEquals(AbsoluteRelativeConstraint.ABS, monitoredBranch1.getAbsoluteRelativeConstraint());
        assertEquals(1f, monitoredBranch1.getPenaltyCostsForviolations(), 0);

        MonitoredBranchXlsx monitoredBranch8 = monitoredBranches.get(7);
        assertEquals("Belgium-France 2", monitoredBranch8.getUniqueCbcoName());
        assertEquals(ElementDescriptionMode.ORDER_CODE, monitoredBranch1.getDescriptionMode());
        assertEquals(Activation.YES, monitoredBranch8.getActivation());
        assertEquals(Tso.N_RTE, monitoredBranch1.getTso());
        assertEquals("BBE3AA1", monitoredBranch8.getUctNodeFrom());
        assertEquals("FFR4AA1", monitoredBranch8.getUctNodeTo());
        assertEquals(null, monitoredBranch8.getUniqueCOName());
        assertEquals(AbsoluteRelativeConstraint.ABS, monitoredBranch8.getAbsoluteRelativeConstraint());
        assertEquals(1f, monitoredBranch8.getPenaltyCostsForviolations(), 0);
    }

    @Test
    public void shouldExtractMonitoredBranchFromFile20170215XlsxCracFrV02V23() {
        //Given, When and Action
        List<MonitoredBranchXlsx> monitoredBranches = ExcelReader.of(MonitoredBranchXlsx.class)
                .from(MonitoredBranchXlsxReaderTest.class.getResourceAsStream("/20170215_xlsx_crac_fr_v02_v2.3.xlsx"))
                .skipHeaderRow(true)
                .sheet(BRANCH_CBCO)
                .list();

        //Asserts
        assertNotNull(monitoredBranches);
        assertFalse(monitoredBranches.isEmpty());
        assertEquals(8, monitoredBranches.size());
    }

    @Test
    public void shouldExtractMonitoredBranchFromFile20170215XlsxCracFrV03V23() {
        //Given, When and Action
        List<MonitoredBranchXlsx> monitoredBranches = ExcelReader.of(MonitoredBranchXlsx.class)
                .from(MonitoredBranchXlsxReaderTest.class.getResourceAsStream("/20170215_xlsx_crac_fr_v03_v2.3.xlsx"))
                .skipHeaderRow(true)
                .sheet(BRANCH_CBCO)
                .list();

        //Asserts
        assertNotNull(monitoredBranches);
        assertFalse(monitoredBranches.isEmpty());
        assertEquals(8, monitoredBranches.size());
    }

    @Test
    public void shouldExtractMonitoredBranchFromFile20170215XlsxCracFrV04V23() {
        //Given, When and Action
        List<MonitoredBranchXlsx> monitoredBranches = ExcelReader.of(MonitoredBranchXlsx.class)
                .from(MonitoredBranchXlsxReaderTest.class.getResourceAsStream("/20170215_xlsx_crac_fr_v04_v2.3.xlsx"))
                .skipHeaderRow(true)
                .sheet(BRANCH_CBCO)
                .list();

        //Asserts
        assertNotNull(monitoredBranches);
        assertFalse(monitoredBranches.isEmpty());
        assertEquals(8, monitoredBranches.size());
    }

    @Test
    public void shouldExtractMonitoredBranchFromFile20170215XlsxCracFrV05V23() {
        //Given, When and Action
        List<MonitoredBranchXlsx> monitoredBranches = ExcelReader.of(MonitoredBranchXlsx.class)
                .from(MonitoredBranchXlsxReaderTest.class.getResourceAsStream("/20170215_xlsx_crac_fr_v05_v2.3.xlsx"))
                .skipHeaderRow(true)
                .sheet(BRANCH_CBCO)
                .list();

        //Asserts
        assertNotNull(monitoredBranches);
        assertFalse(monitoredBranches.isEmpty());
        assertEquals(8, monitoredBranches.size());
    }

    @Test
    public void shouldExtractMonitoredBranchFromFile20170215XlsxCracFrV06V23() {
        //Given, When and Action
        List<MonitoredBranchXlsx> monitoredBranches = ExcelReader.of(MonitoredBranchXlsx.class)
                .from(MonitoredBranchXlsxReaderTest.class.getResourceAsStream("/20170215_xlsx_crac_fr_v06_v2.3.xlsx"))
                .skipHeaderRow(true)
                .sheet(BRANCH_CBCO)
                .list();

        //Asserts
        assertNotNull(monitoredBranches);
        assertFalse(monitoredBranches.isEmpty());
        assertEquals(32, monitoredBranches.size());
    }

    @Test
    public void shouldExtractMonitoredBranchFromFile20170215XlsxCracFrV07V23() {
        //Given, When and Action
        List<MonitoredBranchXlsx> monitoredBranches = ExcelReader.of(MonitoredBranchXlsx.class)
                .from(MonitoredBranchXlsxReaderTest.class.getResourceAsStream("/20170215_xlsx_crac_fr_v07_v2.3.xlsx"))
                .skipHeaderRow(true)
                .sheet(BRANCH_CBCO)
                .list();

        //Asserts
        assertNotNull(monitoredBranches);
        assertFalse(monitoredBranches.isEmpty());
        assertEquals(32, monitoredBranches.size());
    }

    @Test
    public void shouldExtractMonitoredBranchFromFile20170215XlsxCracFrV08V23() {
        //Given, When and Action
        List<MonitoredBranchXlsx> monitoredBranches = ExcelReader.of(MonitoredBranchXlsx.class)
                .from(MonitoredBranchXlsxReaderTest.class.getResourceAsStream("/20170215_xlsx_crac_fr_v08_v2.3.xlsx"))
                .skipHeaderRow(true)
                .sheet(BRANCH_CBCO)
                .list();

        //Asserts
        assertNotNull(monitoredBranches);
        assertFalse(monitoredBranches.isEmpty());
        assertEquals(32, monitoredBranches.size());
    }
}
