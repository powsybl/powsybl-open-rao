package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.UsageRule;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;

import static org.junit.Assert.*;

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

    @Test
    public void setOperator() {
    }

    @Test
    public void getOperator() {
    }

    @Test
    public void setUsageRules() {
    }

    @Test
    public void getUsageRules() {
    }

    @Test
    public void addUsageRule() {
    }

    @Test
    public void getUsageMethod() {
    }

    @Test
    public void testEquals() {
    }

    @Test
    public void testHashCode() {
    }
}
