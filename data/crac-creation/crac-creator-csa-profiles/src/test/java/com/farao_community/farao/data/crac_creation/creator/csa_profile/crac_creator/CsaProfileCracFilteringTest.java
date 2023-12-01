package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.farao_community.farao.commons.logs.RaoBusinessWarns;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreationTestUtil.getCsaCracCreationContext;
import static com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreationTestUtil.getLogs;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CsaProfileCracFilteringTest {

    @Test
    void testCracCreatorFiltersOutBadTimeStamps() {
        ListAppender<ILoggingEvent> listAppender = getLogs(RaoBusinessWarns.class);
        List<ILoggingEvent> logsList = listAppender.list;

        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/CSA_82_FilterBadTS.zip");
        assertEquals(5, logsList.size());
        assert logsList.get(0).getFormattedMessage().contains("CSA_RA_outdated_remove.xml");
        assert logsList.get(1).getFormattedMessage().contains("CSA_RA_bad_end_date_remove.xml");
        assert logsList.get(2).getFormattedMessage().contains("CSA_RA_bad_start_date_remove.xml");
        assert logsList.get(3).getFormattedMessage().contains("CSA_RA_start_date_after_end_date_remove.xml");
        assert logsList.get(4).getFormattedMessage().contains("CSA_RA_not_yet_valid_remove.xml");
    }
}
