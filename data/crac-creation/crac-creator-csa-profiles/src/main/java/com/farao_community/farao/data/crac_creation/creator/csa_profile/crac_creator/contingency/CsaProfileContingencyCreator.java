/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.contingency;

import com.farao_community.farao.data.crac_api.ContingencyAdder;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileConstants;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreationContext;
import com.powsybl.iidm.network.Network;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
public class CsaProfileContingencyCreator {

    private final Crac crac;

    private final Network network;

    private final PropertyBags contingenciesPropertybags;

    private Set<CsaProfileContingencyCreationContext> csaProfileContingencyCreationContexts;
    private CsaProfileCracCreationContext cracCreationContext;

    public CsaProfileContingencyCreator(Crac crac, Network network, PropertyBags contingenciesPropertybags, CsaProfileCracCreationContext cracCreationContext) {
        this.crac = crac;
        this.network = network;
        this.contingenciesPropertybags = contingenciesPropertybags;
        this.cracCreationContext = cracCreationContext;
    }

    public void createAndAddContingencies() {
        this.csaProfileContingencyCreationContexts = new HashSet<>();

        for (PropertyBag contingencyPropertyBag : contingenciesPropertybags) {
            String contingencyId = contingencyPropertyBag.get(CsaProfileConstants.REQUEST_CONTINGENCIES_RDFID);
            String contingencyName = contingencyPropertyBag.get(CsaProfileConstants.REQUEST_CONTINGENCIES_NAME).concat(contingencyPropertyBag.get(CsaProfileConstants.REQUEST_CONTINGENCIES_EQUIPMENT_OPERATOR));
            String equipmentId = contingencyPropertyBag.get(CsaProfileConstants.REQUEST_CONTINGENCIES_EQUIPMENT);
            Boolean mustStudy = Boolean.parseBoolean(contingencyPropertyBag.get(CsaProfileConstants.REQUEST_CONTINGENCIES_MUST_STUDY));
            String contingentStatus = contingencyPropertyBag.get(CsaProfileConstants.REQUEST_CONTINGENCIES_CONTINGENT_STATUS);

            if (!CsaProfileConstants.IMPORTED_CONTINGENT_STATUS.equals(contingentStatus)) {
                csaProfileContingencyCreationContexts.add(CsaProfileContingencyCreationContext.notImported(contingencyId, contingencyName, ImportStatus.INCONSISTENCY_IN_DATA, "incorrect contingent status"));
                return;
            }

            if (!mustStudy) {
                csaProfileContingencyCreationContexts.add(CsaProfileContingencyCreationContext.notImported(contingencyId, contingencyName, ImportStatus.INCONSISTENCY_IN_DATA, "contingency.mustStudy is false"));
                return;
            }

            ContingencyAdder contingencyAdder = crac.newContingency()
                    .withId(contingencyId)
                    .withName(contingencyName);
            csaProfileContingencyCreationContexts.add(CsaProfileContingencyCreationContext.imported(contingencyId, contingencyName, "contingency imported correctly", false));

        }
        this.cracCreationContext.setContingencyCreationContexts(csaProfileContingencyCreationContexts);
    }
}
