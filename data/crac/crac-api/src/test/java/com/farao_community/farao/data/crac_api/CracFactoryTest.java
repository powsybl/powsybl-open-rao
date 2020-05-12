package com.farao_community.farao.data.crac_api;

import com.farao_community.farao.commons.FaraoException;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class CracFactoryTest {

    @Test
    public void testGetFactory() {
        CracFactory cf1 = CracFactory.find("MockCracFactory1");
        assertNotNull(cf1);
        assertEquals(cf1.getClass(), MockCracFactory1.class);
        CracFactory cf2 = CracFactory.find("MockCracFactory2");
        assertNotNull(cf2);
        assertEquals(cf2.getClass(), MockCracFactory2.class);
    }

    @Test(expected = FaraoException.class)
    public void mustThrowIfImplemNotFound() {
        CracFactory.find("SimpleCracFactory");
    }

    @Test(expected = FaraoException.class)
    public void mustThrowIfNameNullAndMultipleImplem() {
        CracFactory.find(null);
    }

    @Test
    public void testDefaultConfig() {
        CracFactory cf = CracFactory.findDefault();
        assertNotNull(cf);
        assertEquals(cf.getClass(), MockCracFactory1.class);
    }

    // TO DO : test different default configs : empty (should throw),
    // with MockCracFactory2, with wrong implem name (should throw)
    // (this is not yet possible with PowSyBl's TestPlatformConfigProvider)
}
