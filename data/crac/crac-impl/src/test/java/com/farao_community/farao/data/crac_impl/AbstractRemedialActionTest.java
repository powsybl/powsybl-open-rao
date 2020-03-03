package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.UsageRule;
import org.mockito.Mockito;

import java.util.ArrayList;


/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
abstract public class AbstractRemedialActionTest {

    public ArrayList<UsageRule> createUsageRules() {
        UsageRule mockedUsageRule = Mockito.mock(UsageRule.class);
        ArrayList<UsageRule> usageRules = new ArrayList<>();
        usageRules.add(mockedUsageRule);
        return usageRules;
    }

}
