package com.powsybl.open_rao.data.crac_creation.creator.csa_profile.crac_creator;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.powsybl.open_rao.commons.logs.RaoBusinessWarns;
import com.powsybl.open_rao.data.crac_api.Crac;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static com.powsybl.open_rao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreationTestUtil.getCsaCracCreationContext;
import static com.powsybl.open_rao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreationTestUtil.getLogs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsaProfileCracFilteringTest {

    @Test
    void testCracCreatorFiltersOutBadTimeStamps() {
        ListAppender<ILoggingEvent> listAppender = getLogs(RaoBusinessWarns.class);
        List<ILoggingEvent> logsList = listAppender.list;

        CsaProfileCracCreationContext context = getCsaCracCreationContext("/CSA_82_FilterBadTS.zip");
        Crac crac = context.getCrac();

        // checks files filtering
        assertEquals(7, logsList.size());
        logsList.sort(Comparator.comparing(ILoggingEvent::getMessage));
        assert logsList.get(0).getFormattedMessage().contains("CSA_RA_bad_end_date_remove.xml");
        assert logsList.get(1).getFormattedMessage().contains("CSA_RA_bad_start_date_remove.xml");
        assert logsList.get(2).getFormattedMessage().contains("CSA_RA_missing_end_date_remove.xml");
        assert logsList.get(3).getFormattedMessage().contains("CSA_RA_missing_start_date_remove.xml");
        assert logsList.get(4).getFormattedMessage().contains("CSA_RA_not_yet_valid_remove.xml");
        assert logsList.get(5).getFormattedMessage().contains("CSA_RA_outdated_remove.xml");
        assert logsList.get(6).getFormattedMessage().contains("CSA_RA_start_date_after_end_date_remove.xml");

        // checks crac content
        assertTrue(crac.getRemedialActions().isEmpty());
        assertEquals(1, crac.getContingencies().size());
        assertEquals("RTE_co1_fr2_fr3_1", crac.getContingencies().stream().iterator().next().getName());
    }
}
