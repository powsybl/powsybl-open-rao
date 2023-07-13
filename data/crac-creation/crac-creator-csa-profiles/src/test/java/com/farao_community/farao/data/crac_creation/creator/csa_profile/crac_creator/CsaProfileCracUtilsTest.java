/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator;

import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CsaProfileCracUtilsTest {

    @Test
    public void testGetLinkedPropertyBags() {
        List<String> listPropDest = Arrays.asList("destProperty1", "destProperty2", "destProperty3", "destProperty4");
        List<String> listPropSource = Arrays.asList("sourceProperty1", "sourceProperty2", "sourceProperty3", "sourceProperty4");
        PropertyBag destPb = new PropertyBag(listPropDest, false);
        destPb.put("destProperty1", "destValue1");
        destPb.put("destProperty2", "destValue2");
        destPb.put("destProperty3", "http://blablabla.eu/#_destValue3");
        destPb.put("destProperty4", "destValue4");

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

        PropertyBags result = CsaProfileCracUtils.getLinkedPropertyBags(sourcesPb, destPb, "sourceProperty2", "destProperty3");
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("sourceValue11", result.get(0).get("sourceProperty1"));
    }
}
