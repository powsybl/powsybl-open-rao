package com.farao_community.farao.flowbased_computation;

import com.farao_community.farao.commons.chronology.DataChronology;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;

public class GlskValuesProviderTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlskValuesProviderTest.class);

    private Network testNetwork;
    private Instant instant;

    @Test
    public void run() throws ParserConfigurationException, SAXException, IOException {
        testNetwork = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        instant = Instant.parse("2018-08-29T21:00:00Z");
        GlskValuesProvider glskValuesProvider = new GlskValuesProvider();
        Map<String, DataChronology<LinearGlsk> > map = glskValuesProvider.getDataChronologyLinearGlskMap(testNetwork,
                getClass().getResource("/GlskB43ParticipationFactorIIDM.xml").getPath());
        Assert.assertFalse(map.isEmpty());

        LinearGlsk linearGlsk = glskValuesProvider.getCountryLinearGlsk(testNetwork,
                getClass().getResource("/GlskB43ParticipationFactorIIDM.xml").getPath(), instant, "10YFR-RTE------C");
        Assert.assertFalse(linearGlsk.getGLSKs().isEmpty());

        Map<String, LinearGlsk> linearGlskMap = glskValuesProvider.getLinearGlskMap(testNetwork, getClass().getResource("/GlskB43ParticipationFactorIIDM.xml").getPath(), instant);
        Assert.assertFalse(linearGlskMap.isEmpty());
    }
}
