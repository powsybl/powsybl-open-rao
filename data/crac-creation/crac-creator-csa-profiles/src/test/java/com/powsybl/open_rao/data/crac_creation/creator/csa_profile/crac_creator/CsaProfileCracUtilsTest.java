/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.data.crac_creation.creator.csa_profile.crac_creator;

import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CsaProfileCracUtilsTest {

    @Test
    void testGetLinkedPropertyBags() {
        List<String> listPropDest = Arrays.asList("destProperty1", "destProperty2", "destProperty3", "destProperty4");
        List<String> listPropSource = Arrays.asList("sourceProperty1", "sourceProperty2", "sourceProperty3", "sourceProperty4");
        PropertyBag destPb = new PropertyBag(listPropDest, false);
        destPb.put("destProperty1", "destValue1");
        destPb.put("destProperty2", "destValue2");
        destPb.put("destProperty3", "http://blablabla.eu/#_destValue3");
        destPb.put("destProperty4", "destValue4");
        PropertyBags destsPb = new PropertyBags();
        destsPb.add(destPb);

        PropertyBags sourcesPb = new PropertyBags();
        PropertyBag sourcePb1 = new PropertyBag(listPropSource, false);
        sourcePb1.put("sourceProperty1", "sourceValue11");
        sourcePb1.put("sourceProperty2", "_destValue3");
        sourcePb1.put("sourceProperty3", "sourceValue13");
        sourcePb1.put("sourceProperty4", "sourceValue14");

        PropertyBag sourcePb2 = new PropertyBag(listPropSource, false);
        sourcePb2.put("sourceProperty1", "sourceValue21");
        sourcePb2.put("sourceProperty3", "sourceValue23");
        sourcePb2.put("sourceProperty4", "sourceValue24");

        PropertyBag sourcePb3 = new PropertyBag(listPropSource, false);
        sourcePb3.put("sourceProperty1", "sourceValue31");
        sourcePb3.put("sourceProperty2", "sourceValue32");
        sourcePb3.put("sourceProperty3", "sourceValue33");
        sourcePb3.put("sourceProperty4", "sourceValue34");

        sourcesPb.addAll(Arrays.asList(sourcePb1, sourcePb2, sourcePb3));

        Map<String, Set<PropertyBag>> map = CsaProfileCracUtils.getMappedPropertyBagsSet(sourcesPb, "sourceProperty2");
        Set<PropertyBag> result = map.get(destPb.getId("destProperty3"));
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("sourceValue11", result.iterator().next().get("sourceProperty1"));

    }

    @Test
    void testDurationConversion() {
        assertEquals(90063, CsaProfileCracUtils.convertDurationToSeconds("P1DT1H1M3S"));
        assertEquals(10920, CsaProfileCracUtils.convertDurationToSeconds("PT3H2M0S"));
        assertEquals(315, CsaProfileCracUtils.convertDurationToSeconds("P0DT5M15S"));
        assertEquals(172800, CsaProfileCracUtils.convertDurationToSeconds("P2DT0H0S"));
        assertEquals(3600, CsaProfileCracUtils.convertDurationToSeconds("P0DT1H0M"));
        assertEquals(62, CsaProfileCracUtils.convertDurationToSeconds("PT1M2S"));
        assertEquals(2, CsaProfileCracUtils.convertDurationToSeconds("PT0H2S"));
        assertEquals(2, CsaProfileCracUtils.convertDurationToSeconds("P0DT2S"));
        assertEquals(240, CsaProfileCracUtils.convertDurationToSeconds("PT0H4M"));
        assertEquals(60, CsaProfileCracUtils.convertDurationToSeconds("P0DT1M"));
        assertEquals(3600, CsaProfileCracUtils.convertDurationToSeconds("P0DT1H"));
        assertEquals(5, CsaProfileCracUtils.convertDurationToSeconds("PT5S"));
        assertEquals(60, CsaProfileCracUtils.convertDurationToSeconds("PT1M"));
        assertEquals(0, CsaProfileCracUtils.convertDurationToSeconds("PT0H"));
        assertEquals(86400, CsaProfileCracUtils.convertDurationToSeconds("P1D"));
        assertThrows(RuntimeException.class, () -> CsaProfileCracUtils.convertDurationToSeconds("P1R"));
    }
}
