/**
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
public class ContingencyElementReaderTest {

    private static final String SHEET_NAME = "Branch_CO";

    @Rule
    public ExpectedException thrown = ExpectedException.none();


    @Test
    public void should_extract_contingencyElement_from_file_20170215_xlsx_crac_fr_v01_v2_3() {
        //Given, When and Action
        List<ContingencyElementXlsx> contingencyElements = ExcelReader.of(ContingencyElementXlsx.class)
                .from(ContingencyElementReaderTest.class.getResourceAsStream("/20170215_xlsx_crac_fr_v01_v2.3.xlsx"))
                .skipHeaderRow(true)
                .sheet(SHEET_NAME)
                .list();

        //Asserts
        assertNotNull(contingencyElements);
        assertTrue(contingencyElements.isEmpty());
        assertEquals(0, contingencyElements.size());
    }

    @Test
    public void should_extract_contingencyElement_from_file_20170215_xlsx_crac_fr_v02_v2_3() {
        //Given, When and Action
        List<ContingencyElementXlsx> contingencyElements = ExcelReader.of(ContingencyElementXlsx.class)
                .from(ContingencyElementReaderTest.class.getResourceAsStream("/20170215_xlsx_crac_fr_v02_v2.3.xlsx"))
                .skipHeaderRow(true)
                .sheet(SHEET_NAME)
                .list();

        //Asserts
        assertNotNull(contingencyElements);
        assertTrue(contingencyElements.isEmpty());
        assertEquals(0, contingencyElements.size());
    }

    @Test
    public void should_extract_contingencyElement_from_file_20170215_xlsx_crac_fr_v03_v2_3() {
        //Given, When and Action
        List<ContingencyElementXlsx> contingencyElements = ExcelReader.of(ContingencyElementXlsx.class)
                .from(ContingencyElementReaderTest.class.getResourceAsStream("/20170215_xlsx_crac_fr_v03_v2.3.xlsx"))
                .skipHeaderRow(true)
                .sheet(SHEET_NAME)
                .list();

        //Asserts
        assertNotNull(contingencyElements);
        assertTrue(contingencyElements.isEmpty());
        assertEquals(0, contingencyElements.size());
    }


    @Test
    public void should_extract_contingencyElement_from_file_20170215_xlsx_crac_fr_v04_v2_3() {
        //Given, When and Action
        List<ContingencyElementXlsx> contingencyElements = ExcelReader.of(ContingencyElementXlsx.class)
                .from(ContingencyElementReaderTest.class.getResourceAsStream("/20170215_xlsx_crac_fr_v04_v2.3.xlsx"))
                .skipHeaderRow(true)
                .sheet(SHEET_NAME)
                .list();

        //Asserts
        assertNotNull(contingencyElements);
        assertTrue(contingencyElements.isEmpty());
        assertEquals(0, contingencyElements.size());
    }


    @Test
    public void should_extract_contingencyElement_from_file_20170215_xlsx_crac_fr_v05_v2_3() {
        //Given, When and Action
        List<ContingencyElementXlsx> contingencyElements = ExcelReader.of(ContingencyElementXlsx.class)
                .from(ContingencyElementReaderTest.class.getResourceAsStream("/20170215_xlsx_crac_fr_v05_v2.3.xlsx"))
                .skipHeaderRow(true)
                .sheet(SHEET_NAME)
                .list();

        //Asserts
        assertNotNull(contingencyElements);
        assertTrue(contingencyElements.isEmpty());
        assertEquals(0, contingencyElements.size());
    }


    @Test
    public void should_extract_contingencyElement_from_file_file_20170215_xlsx_crac_fr_v06_v2_3() {
        //Given, When and Action
        List<ContingencyElementXlsx> contingencyElements = ExcelReader.of(ContingencyElementXlsx.class)
                .from(ContingencyElementReaderTest.class.getResourceAsStream("/20170215_xlsx_crac_fr_v06_v2.3.xlsx"))
                .skipHeaderRow(true)
                .sheet(SHEET_NAME)
                .list();
        //Asserts
        assertNotNull(contingencyElements);
        assertFalse(contingencyElements.isEmpty());
        assertEquals(3, contingencyElements.size());
    }

    @Test
    public void should_extract_contingencyElement_from_file_20170215_xlsx_crac_fr_v07_v2_3() {
        // Given, When and Action
        List<ContingencyElementXlsx> contingencyElements = ExcelReader.of(ContingencyElementXlsx.class)
                .from(ContingencyElementReaderTest.class.getResourceAsStream("/20170215_xlsx_crac_fr_v07_v2.3.xlsx"))
                .skipHeaderRow(true)
                .sheet(SHEET_NAME)
                .list();
        //Asserts
        assertNotNull(contingencyElements);
        assertFalse(contingencyElements.isEmpty());
        assertEquals(3, contingencyElements.size());
    }


    @Test
    public void should_throw_exception_when_extract_contingencyElement_from_file_20170215_xlsx_crac_fr_v08_v2_3() {
        ////Given, When and Action
        List<ContingencyElementXlsx> contingencyElements = ExcelReader.of(ContingencyElementXlsx.class)
                .from(ContingencyElementReaderTest.class.getResourceAsStream("/20170215_xlsx_crac_fr_v08_v2.3.xlsx"))
                .skipHeaderRow(true)
                .sheet(SHEET_NAME)
                .list();
        //Asserts
        assertNotNull(contingencyElements);
        assertFalse(contingencyElements.isEmpty());
        assertEquals(3, contingencyElements.size());
    }


}
