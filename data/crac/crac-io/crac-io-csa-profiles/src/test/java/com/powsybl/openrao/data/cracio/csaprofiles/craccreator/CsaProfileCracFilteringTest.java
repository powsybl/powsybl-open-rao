package com.powsybl.openrao.data.cracio.csaprofiles.craccreator;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.powsybl.openrao.commons.logs.RaoBusinessWarns;
import com.powsybl.openrao.data.cracapi.Crac;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil.getCsaCracCreationContext;
import static com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil.getLogs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsaProfileCracFilteringTest {

    @Test
    void testCracCreatorFiltersOutBadTimeStamps() {
        ListAppender<ILoggingEvent> listAppender = getLogs(RaoBusinessWarns.class);
        List<ILoggingEvent> logsList = listAppender.list;

        CsaProfileCracCreationContext context = getCsaCracCreationContext("/profiles/ProfilesWithIncoherentTimestamps.zip");
        Crac crac = context.getCrac();

        // check files filtering
        assertEquals(7, logsList.size());
        logsList.sort(Comparator.comparing(ILoggingEvent::getMessage));
        assertEquals("[REMOVED] The file : contexts:CSA_RA_bad_end_date_remove.xml will be ignored. Its dates are not consistent with the import date : 2023-03-29T12:00Z", logsList.get(0).getFormattedMessage());
        assertEquals("[REMOVED] The file : contexts:CSA_RA_bad_start_date_remove.xml will be ignored. Its dates are not consistent with the import date : 2023-03-29T12:00Z", logsList.get(1).getFormattedMessage());
        assertEquals("[REMOVED] The file : contexts:CSA_RA_missing_end_date_remove.xml will be ignored. Its dates are not consistent with the import date : 2023-03-29T12:00Z", logsList.get(2).getFormattedMessage());
        assertEquals("[REMOVED] The file : contexts:CSA_RA_missing_start_date_remove.xml will be ignored. Its dates are not consistent with the import date : 2023-03-29T12:00Z", logsList.get(3).getFormattedMessage());
        assertEquals("[REMOVED] The file : contexts:CSA_RA_not_yet_valid_remove.xml will be ignored. Its dates are not consistent with the import date : 2023-03-29T12:00Z", logsList.get(4).getFormattedMessage());
        assertEquals("[REMOVED] The file : contexts:CSA_RA_outdated_remove.xml will be ignored. Its dates are not consistent with the import date : 2023-03-29T12:00Z", logsList.get(5).getFormattedMessage());
        assertEquals("[REMOVED] The file : contexts:CSA_RA_start_date_after_end_date_remove.xml will be ignored. Its dates are not consistent with the import date : 2023-03-29T12:00Z", logsList.get(6).getFormattedMessage());

        // check crac content
        assertTrue(crac.getRemedialActions().isEmpty());
        assertEquals(1, crac.getContingencies().size());
        assertEquals("RTE_co1_fr2_fr3_1", crac.getContingencies().stream().iterator().next().getName().get());
    }
}
