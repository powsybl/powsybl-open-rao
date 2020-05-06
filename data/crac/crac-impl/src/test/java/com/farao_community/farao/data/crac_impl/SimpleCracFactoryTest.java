package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.CracFactory;
import com.farao_community.farao.data.crac_api.Crac;
import org.junit.Test;
import static org.junit.Assert.*;

public class SimpleCracFactoryTest {
    final String factoryName = "SimpleCracFactory";

    @Test
    public void testDependencyInjection() {
        assertEquals(factoryName, new SimpleCracFactory().getName());
        CracFactory factory = CracFactory.find(factoryName);
        assertNotNull(factory);
        assertEquals(factory.getClass(), SimpleCracFactory.class);
    }

    @Test
    public void testCreateSimpleCrac()
    {
        String id = "idForTest";
        String name = "testName";
        Crac crac = new SimpleCracFactory().create(id, name);
        assertEquals(crac.getClass(), SimpleCrac.class);
        assertEquals(crac.);
    }
}
