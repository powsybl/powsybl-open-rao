/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.csa_profile;

import com.farao_community.farao.commons.logs.FaraoLoggerProvider;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileConstants;
import com.farao_community.farao.data.native_crac_api.NativeCrac;
import com.powsybl.triplestore.api.PropertyBags;
import com.powsybl.triplestore.api.QueryCatalog;
import com.powsybl.triplestore.api.TripleStore;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
public class CsaProfileCrac implements NativeCrac {

    private final TripleStore tripleStoreCsaProfileCrac;

    private final QueryCatalog queryCatalogCsaProfileCrac;

    public CsaProfileCrac(TripleStore tripleStoreCsaProfileCrac) {
        this.tripleStoreCsaProfileCrac = tripleStoreCsaProfileCrac;
        this.queryCatalogCsaProfileCrac = new QueryCatalog(CsaProfileConstants.SPARQL_FILE_CSA_PROFILE);
    }

    @Override
    public String getFormat() {
        return "CsaProfileCrac";
    }

    public PropertyBags getContingencies() {
        return this.queryTripleStore(Arrays.asList(CsaProfileConstants.REQUEST_ORDINARY_CONTINGENCY, CsaProfileConstants.REQUEST_EXCEPTIONAL_CONTINGENCY, CsaProfileConstants.REQUEST_OUT_OF_RANGE_CONTINGENCY), tripleStoreCsaProfileCrac.contextNames());
    }

    public PropertyBags getContingencyEquipments() {
        return this.queryTripleStore(CsaProfileConstants.REQUEST_CONTINGENCY_EQUIPMENT);
    }

    public PropertyBags getAssessedElements() {
        return this.queryTripleStore(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT, tripleStoreCsaProfileCrac.contextNames());
    }

    public PropertyBags getAssessedElementsWithContingencies() {
        return this.queryTripleStore(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_WITH_CONTINGENCY);
    }

    public PropertyBags getCurrentLimits() {
        return this.queryTripleStore(CsaProfileConstants.REQUEST_CURRENT_LIMIT);
    }

    private PropertyBags queryTripleStore(String queryKey) {
        return this.queryTripleStore(queryKey, new HashSet<>());
    }

    public PropertyBags getRemedialActions() {
        return this.queryTripleStore(CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION, tripleStoreCsaProfileCrac.contextNames());
    }

    public PropertyBags getTopologyAction() {
        return this.queryTripleStore(CsaProfileConstants.TOPOLOGY_ACTION, tripleStoreCsaProfileCrac.contextNames());
    }

    public PropertyBags getRotatingMachineAction() {
        return this.queryTripleStore(CsaProfileConstants.ROTATING_MACHINE_ACTION, tripleStoreCsaProfileCrac.contextNames());
    }

    public PropertyBags getStaticPropertyRanges() {
        return this.queryTripleStore(CsaProfileConstants.STATIC_PROPERTY_RANGE, tripleStoreCsaProfileCrac.contextNames());
    }

    public PropertyBags getContingencyWithRemedialAction() {
        return this.queryTripleStore(CsaProfileConstants.CONTINGENCY_WITH_REMEDIAL_ACTION, tripleStoreCsaProfileCrac.contextNames());
    }

    private PropertyBags queryTripleStore(List<String> queryKeys, Set<String> contexts) {
        PropertyBags mergedPropertyBags = new PropertyBags();
        for (String queryKey : queryKeys) {
            mergedPropertyBags.addAll(queryTripleStore(queryKey, contexts));
        }
        return mergedPropertyBags;
    }

    /**
     * execute query on the whole tripleStore or on each context included in the set
     *
     * @param queryKey : query name in the sparql file
     * @param contexts : list of contexts where the query will be executed (if empty, the query is executed on the whole tripleStore
     * @return
     */
    private PropertyBags queryTripleStore(String queryKey, Set<String> contexts) {
        String query = queryCatalogCsaProfileCrac.get(queryKey);
        if (query == null) {
            FaraoLoggerProvider.TECHNICAL_LOGS.warn("Query [{}] not found in catalog", queryKey);
            return new PropertyBags();
        }

        if (contexts.isEmpty()) {
            return tripleStoreCsaProfileCrac.query(query);
        }

        PropertyBags multiContextsPropertyBags = new PropertyBags();
        for (String context : contexts) {
            String contextQuery = String.format(query, context);
            multiContextsPropertyBags.addAll(tripleStoreCsaProfileCrac.query(contextQuery));
        }
        return multiContextsPropertyBags;
    }
}

