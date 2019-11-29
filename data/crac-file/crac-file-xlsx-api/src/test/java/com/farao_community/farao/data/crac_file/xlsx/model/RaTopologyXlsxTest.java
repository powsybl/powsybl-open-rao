/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
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

public class RaTopologyXlsxTest {

    private static final String RA_TOPOLOGY = "RA_Topology";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void shouldExtractTopologyRasFromFile20170215XlsxCracFrV09V23() {
        //Given, When and Action
        List<RaTopologyXlsx> raTopology = ExcelReader.of(RaTopologyXlsx.class)
                .from(RaTopologyXlsxTest.class.getResourceAsStream("/20170215_xlsx_crac_fr_v09_v2.3.xlsx"))
                .skipHeaderRow(true)
                .sheet(RA_TOPOLOGY)
                .list();

        //Asserts
        assertNotNull(raTopology);
        assertFalse(raTopology.isEmpty());
        assertEquals(2, raTopology.size());

        RaTopologyXlsx raTopology1 = raTopology.get(0);
        assertEquals("Topology RA 1", raTopology1.getName());
        assertEquals(Tso.N_RTE, raTopology1.getTso());
        assertEquals(Activation.YES, raTopology1.getActivation());
        assertEquals(150, raTopology1.getPenaltyCost(), 1e-3f);
        assertEquals(2, raTopology1.getMaxSwitchingPosition());
        assertEquals(Activation.YES, raTopology1.getPreventive());
        assertEquals(Activation.NO, raTopology1.getCurative());
        assertEquals(Activation.NO, raTopology1.getSpsMode());
        assertEquals(SharingDefinition.SRA, raTopology1.getSharingDefinition());
        assertEquals("France-Germany 1", raTopology1.getConnectedCbco());
        assertEquals(ElementDescriptionMode.ORDER_CODE, raTopology1.getElementDescriptionMode1());
        assertEquals("FFR2AA1", raTopology1.getUctNodeFrom1());
        assertEquals("FFR3AA1", raTopology1.getUctNodeTo1());
        assertEquals("1", raTopology1.getOrderCodeElementName1());
        assertEquals(ElementDescriptionMode.ELEMENT_NAME, raTopology1.getElementDescriptionMode2());
        assertEquals("FFR2AA1", raTopology1.getUctNodeFrom2());
        assertEquals("FFR3AA1", raTopology1.getUctNodeTo2());
        assertEquals("3_2_ELEM", raTopology1.getOrderCodeElementName2());

        RaTopologyXlsx raTopology2 = raTopology.get(1);
        assertEquals("Topology RA 2", raTopology2.getName());
        assertEquals(Tso.N_RTE, raTopology2.getTso());
        assertEquals(Activation.YES, raTopology2.getActivation());
        assertEquals(0, raTopology2.getPenaltyCost(), 1e-3f);
        assertEquals(5, raTopology2.getMaxSwitchingPosition());
        assertEquals(Activation.NO, raTopology2.getPreventive());
        assertEquals(Activation.YES, raTopology2.getCurative());
        assertEquals(Activation.NO, raTopology2.getSpsMode());
        assertEquals(SharingDefinition.NSRA, raTopology2.getSharingDefinition());
        assertEquals("France-Germany 2", raTopology2.getConnectedCbco());
        assertEquals(ElementDescriptionMode.ORDER_CODE, raTopology2.getElementDescriptionMode1());
        assertEquals("FFR2AA1", raTopology2.getUctNodeFrom1());
        assertEquals("FFR3AA1", raTopology2.getUctNodeTo1());
        assertEquals("1", raTopology2.getOrderCodeElementName1());
        assertEquals(ElementDescriptionMode.ORDER_CODE, raTopology2.getElementDescriptionMode2());
        assertEquals("FFR2AA1", raTopology2.getUctNodeFrom2());
        assertEquals("FFR3AA1", raTopology2.getUctNodeTo2());
        assertEquals("2", raTopology2.getOrderCodeElementName2());
        assertEquals(ElementDescriptionMode.ORDER_CODE, raTopology2.getElementDescriptionMode3());
        assertEquals("FFR2AA1", raTopology2.getUctNodeFrom3());
        assertEquals("FFR3AA1", raTopology2.getUctNodeTo3());
        assertEquals("3", raTopology2.getOrderCodeElementName3());
        assertEquals(ElementDescriptionMode.ORDER_CODE, raTopology2.getElementDescriptionMode4());
        assertEquals("FFR2AA1", raTopology2.getUctNodeFrom4());
        assertEquals("FFR3AA1", raTopology2.getUctNodeTo4());
        assertEquals("4", raTopology2.getOrderCodeElementName4());
        assertEquals(ElementDescriptionMode.ORDER_CODE, raTopology2.getElementDescriptionMode5());
        assertEquals("FFR2AA1", raTopology2.getUctNodeFrom5());
        assertEquals("FFR3AA1", raTopology2.getUctNodeTo5());
        assertEquals("5", raTopology2.getOrderCodeElementName5());
        assertEquals(ElementDescriptionMode.ORDER_CODE, raTopology2.getElementDescriptionMode6());
        assertEquals("FFR2AA1", raTopology2.getUctNodeFrom6());
        assertEquals("FFR3AA1", raTopology2.getUctNodeTo6());
        assertEquals("6", raTopology2.getOrderCodeElementName6());
        assertEquals(ElementDescriptionMode.ORDER_CODE, raTopology2.getElementDescriptionMode7());
        assertEquals("FFR2AA1", raTopology2.getUctNodeFrom7());
        assertEquals("FFR3AA1", raTopology2.getUctNodeTo7());
        assertEquals("7", raTopology2.getOrderCodeElementName7());
        assertEquals(ElementDescriptionMode.ORDER_CODE, raTopology2.getElementDescriptionMode8());
        assertEquals("FFR2AA1", raTopology2.getUctNodeFrom8());
        assertEquals("FFR3AA1", raTopology2.getUctNodeTo8());
        assertEquals("8", raTopology2.getOrderCodeElementName8());
    }
}
