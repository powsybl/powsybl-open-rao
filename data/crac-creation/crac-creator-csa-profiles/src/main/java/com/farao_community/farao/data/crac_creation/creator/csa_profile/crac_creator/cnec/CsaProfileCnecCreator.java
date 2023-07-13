/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.cnec;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileConstants;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracUtils;
import com.powsybl.iidm.network.Network;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
public class CsaProfileCnecCreator {
    private final Crac crac;
    private final Network network;
    private final PropertyBags assessedElementsPropertyBags;
    private final PropertyBags assessedElementsWithContingenciesPropertyBags;
    private final PropertyBags currentLimitsPropertyBags;
    private Set<CsaProfileCnecCreationContext> csaProfileCnecCreationContexts;
    private CsaProfileCracCreationContext cracCreationContext;

    public CsaProfileCnecCreator(Crac crac, Network network, PropertyBags assessedElementsPropertyBags, PropertyBags assessedElementsWithContingenciesPropertyBags, PropertyBags currentLimitsPropertyBags, CsaProfileCracCreationContext cracCreationContext) {
        this.crac = crac;
        this.network = network;
        this.assessedElementsPropertyBags = assessedElementsPropertyBags;
        this.assessedElementsWithContingenciesPropertyBags = assessedElementsWithContingenciesPropertyBags;
        this.currentLimitsPropertyBags = currentLimitsPropertyBags;
        this.cracCreationContext = cracCreationContext;
        this.createAndAddCnecs();
    }

    private void createAndAddCnecs() {
        this.csaProfileCnecCreationContexts = new HashSet<>();

        for (PropertyBag assessedElementPropertyBag : assessedElementsPropertyBags) {
            this.addCnec(assessedElementPropertyBag);
        }
    }

    private void addCnec(PropertyBag assessedElementPropertyBag) {
        String keyword = assessedElementPropertyBag.get(CsaProfileConstants.REQUEST_HEADER_KEYWORD);
        String startTime = assessedElementPropertyBag.get(CsaProfileConstants.REQUEST_HEADER_START_DATE);
        String endTime = assessedElementPropertyBag.get(CsaProfileConstants.REQUEST_HEADER_END_DATE);

        if (!CsaProfileConstants.CONTINGENCY_FILE_KEYWORD.equals(keyword)) {
            return;
        }

        if (!CsaProfileCracUtils.isValidInterval(cracCreationContext.getTimeStamp(), startTime, endTime)) {
            return;
        }
    }
}
