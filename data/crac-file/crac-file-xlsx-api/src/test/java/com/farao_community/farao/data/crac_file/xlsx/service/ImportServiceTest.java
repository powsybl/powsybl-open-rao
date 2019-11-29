/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
package com.farao_community.farao.data.crac_file.xlsx.service;

import com.farao_community.farao.data.crac_file.Contingency;
import com.farao_community.farao.data.crac_file.ContingencyElement;
import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.RemedialAction;
import com.farao_community.farao.data.crac_file.xlsx.model.TimesSeries;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Unit Tests for the ImportService class
 */
public class ImportServiceTest {
    private static final double EPSILON = 1e-5;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private ImportService importService;

    @Before
    public void setUp() {
        importService = new ImportService();
    }

    /**
     * Input containing only preventive monitored branches. All the information is provided, and only using node/node/order code description.
     * @throws Exception
     */
    @Test
    public void shouldImportXlsxCracFileFromFile20170215XlsxCracFrV01V23() throws Exception {
        //Given, When and Action
        CracFile cracFile = importService.importContacts(ImportServiceTest.class.getResourceAsStream("/20170215_xlsx_crac_fr_v01_v2.3.xlsx"), TimesSeries.TIME_0530, "/20170215_xlsx_crac_fr_v01_v2.3.xlsx");
        //Asserts
        assertEquals("France-Germany 1", cracFile.getPreContingency().getMonitoredBranches().get(0).getName());
        assertEquals(2130., cracFile.getPreContingency().getMonitoredBranches().get(3).getFmax(), EPSILON);
    }

    /**
     * Input containing only preventive monitored branches. TS 14:30 lacks some fmax values, the associated monitored branches should not be imported.
     * @throws Exception
     */
    @Test
    public void shouldImportXlsxCracFileFromFile20170215XlsxCracFrV02V23() throws Exception {
        //Given, When and Action
        CracFile cracFile = importService.importContacts(ImportServiceTest.class.getResourceAsStream("/20170215_xlsx_crac_fr_v02_v2.3.xlsx"), TimesSeries.TIME_1430, "/20170215_xlsx_crac_fr_v02_v2.3.xlsx");
        //Asserts
        assertEquals("France-Germany 1", cracFile.getPreContingency().getMonitoredBranches().get(0).getName());
        assertEquals(2000, cracFile.getPreContingency().getMonitoredBranches().get(2).getFmax(), EPSILON);
    }

    /**
     * Input containing only preventive monitored branches. A monitored branch is not activated and should not be imported.
     * @throws Exception
     */
    @Test
    public void shouldImportXlsxCracFileFromFile20170215XlsxCracFrV03V23() throws Exception {
        CracFile cracFile = importService.importContacts(ImportServiceTest.class.getResourceAsStream("/20170215_xlsx_crac_fr_v03_v2.3.xlsx"), TimesSeries.TIME_1130, "/20170215_xlsx_crac_fr_v03_v2.3.xlsx");
        //Asserts
        assertEquals("Belgium-France 2", cracFile.getPreContingency().getMonitoredBranches().get(6).getName());
        assertEquals(2000, cracFile.getPreContingency().getMonitoredBranches().get(1).getFmax(), EPSILON);
    }

    /**
     * Input containing only preventive monitored branches. A monitored branch has relative limit and not absolute. Not treated yet, should not be imported.
     * @throws Exception
     */
    @Test
    public void shouldImportXlsxCracFileFromFile20170215XlsxCracFrV04V23() throws Exception {
        CracFile cracFile = importService.importContacts(ImportServiceTest.class.getResourceAsStream("/20170215_xlsx_crac_fr_v04_v2.3.xlsx"), TimesSeries.TIME_0930, "/20170215_xlsx_crac_fr_v04_v2.3.xlsx");
        //Asserts
        assertEquals("Germany-Netherlands 2", cracFile.getPreContingency().getMonitoredBranches().get(3).getName());
        assertEquals(1550, cracFile.getPreContingency().getMonitoredBranches().get(1).getFmax(), EPSILON);
    }

    /**
     * Input containing only preventive monitored branches. One monitored branch is referenced using element name description
     * @throws Exception
     */
    @Test
    public void shouldImportXlsxCracFileFromFile20170215XlsxCracFrV05V23() throws Exception {
        //Given, When and Action
        CracFile cracFile = importService.importContacts(ImportServiceTest.class.getResourceAsStream("/20170215_xlsx_crac_fr_v05_v2.3.xlsx"), TimesSeries.TIME_1830, "/20170215_xlsx_crac_fr_v05_v2.3.xlsx");
    }

    /**
     * Input containing preventive and curative monitored branches. All the information is provided, and only using node/node/order code description.
     * @throws Exception
     */
    @Test
    public void shouldImportXlsxCracFileFromFile20170215XlsxCracFrV06V23() throws Exception {
        //Given, When and Action
        CracFile cracFile = importService.importContacts(ImportServiceTest.class.getResourceAsStream("/20170215_xlsx_crac_fr_v06_v2.3.xlsx"), TimesSeries.TIME_1830, "/20170215_xlsx_crac_fr_v06_v2.3.xlsx");
        List<ContingencyElement> ce = cracFile.getContingencies().get(1).getContingencyElements();
        assertEquals("FR1-FR2 / FR5-FR1 double trip", ce.get(1).getName());
    }

    /**
     * Input containing preventive and curative monitored branches. Some lacking fmax values, monitored branch not activated and relative limits should not be
     * @throws Exception
     */
    @Test
    public void shouldImportXlsxCracFileFromFile20170215XlsxCracFrV07V23() throws Exception {
        CracFile cracFile = importService.importContacts(ImportServiceTest.class.getResourceAsStream("/20170215_xlsx_crac_fr_v07_v2.3.xlsx"), TimesSeries.TIME_1430, "/20170215_xlsx_crac_fr_v07_v2.3.xlsx");
/*        List<MonitoredBranch> monitoredBranches = cracFile.getContingencies().get(0).getMonitoredBranches();

        // test activation no
        assertEquals(monitoredBranches.get(1).getName(),"Germany-Netherlands 1 / FR1-FR2 trip");

        monitoredBranches = cracFile.getContingencies().get(1).getMonitoredBranches();
        // test fmax not here
        assertEquals(monitoredBranches.get(3).getName(),"Netherlands-Belgium 2 / FR1-FR2 / FR5-FR1 double trip");

        monitoredBranches = cracFile.getContingencies().get(2).getMonitoredBranches();
        // test of  Absolute / Relative value
        assertEquals(monitoredBranches.get(2).getName(),"Germany-Netherlands 2 / FR2-DE5 / FR3-DE4 / DE5-DE1 triple trip");
  */
    }

    /**
     *  Input containing preventive and curative monitored branches. A contingency is not activated and should not be imported.
     * @throws Exception
     */
    @Test
    public void shouldImportXlsxCracFileFromFile20170215XlsxCracFrV08V23() throws Exception {
        CracFile cracFile = importService.importContacts(ImportServiceTest.class.getResourceAsStream("/20170215_xlsx_crac_fr_v08_v2.3.xlsx"), TimesSeries.TIME_1830, "/20170215_xlsx_crac_fr_v08_v2.3.xlsx");
        Contingency contingency = cracFile.getContingencies().get(1);

        assertEquals("FR2-DE5 / FR3-DE4 / DE5-DE1 triple trip", contingency.getName());
    }

    /**
     *  Input containing topological remedial actions.
     * @throws Exception
     */
    @Test
    public void shouldImportXlsxCracFileFromFile20170215XlsxCracFrV09V23() throws Exception {
        CracFile cracFile = importService.importContacts(ImportServiceTest.class.getResourceAsStream("/20170215_xlsx_crac_fr_v09_v2.3.xlsx"), TimesSeries.TIME_1830, "/20170215_xlsx_crac_fr_v09_v2.3.xlsx");
        RemedialAction remedialAction = cracFile.getRemedialActions().get(0);

        assertEquals("Topology RA 1", remedialAction.getName());
    }

    @Test
    public void shouldImportCorrectIdFromXlsxCracFile() throws Exception {
        CracFile cracFile = importService.importContacts(ImportServiceTest.class.getResourceAsStream("/test_crac_simple.xlsx"), TimesSeries.TIME_1830, "/test_crac_simple.xlsx");
        assertEquals(3, cracFile.getRemedialActions().size());
        assertEquals(2, cracFile.getPreContingency().getMonitoredBranches().size());

        assertEquals("FFR1AA1  BBE1AA1  1", cracFile.getPreContingency().getMonitoredBranches().get(0).getBranchId());
        assertEquals("FFR1AA1  BBE2AA1  1", cracFile.getPreContingency().getMonitoredBranches().get(1).getBranchId());
        assertEquals("FFR1AA1 _generator", cracFile.getRemedialActions().get(0).getRemedialActionElements().get(0).getId());
        assertEquals("pstExample", cracFile.getRemedialActions().get(2).getId());
        assertEquals("pstExample", cracFile.getRemedialActions().get(2).getName());
        assertEquals("BBE1AA1  BBE2AA1  1", cracFile.getRemedialActions().get(2).getRemedialActionElements().get(0).getId());
    }
}
