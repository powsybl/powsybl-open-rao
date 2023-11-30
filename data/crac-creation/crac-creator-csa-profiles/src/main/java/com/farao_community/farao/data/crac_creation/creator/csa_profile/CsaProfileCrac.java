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

import java.util.*;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
public class CsaProfileCrac implements NativeCrac {

    private final TripleStore tripleStoreCsaProfileCrac;

    private final QueryCatalog queryCatalogCsaProfileCrac;

    private Map<String, List<String>> keywordMap;

    public CsaProfileCrac(TripleStore tripleStoreCsaProfileCrac) {
        this.tripleStoreCsaProfileCrac = tripleStoreCsaProfileCrac;
        this.queryCatalogCsaProfileCrac = new QueryCatalog(CsaProfileConstants.SPARQL_FILE_CSA_PROFILE);
        this.keywordMap = new HashMap<>();
    }

    @Override
    public String getFormat() {
        return "CsaProfileCrac";
    }

    public PropertyBags getContingencies() {
        return queryTripleStore(Arrays.asList(CsaProfileConstants.REQUEST_ORDINARY_CONTINGENCY, CsaProfileConstants.REQUEST_EXCEPTIONAL_CONTINGENCY, CsaProfileConstants.REQUEST_OUT_OF_RANGE_CONTINGENCY), tripleStoreCsaProfileCrac.contextNames());
    }

    public Map<String, PropertyBags> getHeaders() {
        Map<String, PropertyBags> returnMap = new HashMap<>();
        tripleStoreCsaProfileCrac.contextNames().forEach(context -> returnMap.put(context, queryTripleStore(CsaProfileConstants.REQUEST_HEADER, context)));
        return returnMap;
    }

    public void clearContext(String context) {
        tripleStoreCsaProfileCrac.clear(context);
    }

    public void fillKeywordMap(Map<String, List<String>> keywordMap) {
        this.keywordMap = keywordMap;
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

    public PropertyBags getAssessedElementsWithRemedialAction() {
        return this.queryTripleStore(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_WITH_REMEDIAL_ACTION);
    }

    public PropertyBags getCurrentLimits() {
        return this.queryTripleStore(CsaProfileConstants.REQUEST_CURRENT_LIMIT);
    }

    public PropertyBags getVoltageLimits() {
        return this.queryTripleStore(CsaProfileConstants.REQUEST_VOLTAGE_LIMIT);
    }

    public PropertyBags getAngleLimits() {
        return this.queryTripleStore(CsaProfileConstants.REQUEST_ANGLE_LIMIT, tripleStoreCsaProfileCrac.contextNames());
    }

    public PropertyBags getRemedialActions() {
        return this.queryTripleStore(CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION, tripleStoreCsaProfileCrac.contextNames());
    }

    public PropertyBags getTopologyAction() {
        return this.queryTripleStore(CsaProfileConstants.TOPOLOGY_ACTION);
    }

    public PropertyBags getRotatingMachineAction() {
        return this.queryTripleStore(CsaProfileConstants.ROTATING_MACHINE_ACTION);
    }

    public PropertyBags getShuntCompensatorModifications() {
        return this.queryTripleStore(CsaProfileConstants.SHUNT_COMPENSATOR_MODIFICATION);
    }

    public PropertyBags getTapPositionAction() {
        return this.queryTripleStore(CsaProfileConstants.TAP_POSITION_ACTION, tripleStoreCsaProfileCrac.contextNames());
    }

    public PropertyBags getStaticPropertyRanges() {
        return this.queryTripleStore(CsaProfileConstants.STATIC_PROPERTY_RANGE);
    }

    public PropertyBags getContingencyWithRemedialAction() {
        return this.queryTripleStore(CsaProfileConstants.CONTINGENCY_WITH_REMEDIAL_ACTION);
    }

    public PropertyBags getShuntCompensatorModificationAuto() {
        return this.queryTripleStore(CsaProfileConstants.SHUNT_COMPENSATOR_MODIFICATION_AUTO);
    }

    public PropertyBags getRotatingMachineActionAuto() {
        return this.queryTripleStore(CsaProfileConstants.ROTATING_MACHINE_ACTION_AUTO);
    }

    public PropertyBags getTopologyActionAuto() {
        return this.queryTripleStore(CsaProfileConstants.TOPOLOGY_ACTION_AUTO);
    }

    public PropertyBags getTapPositionActionAuto() {
        return this.queryTripleStore(CsaProfileConstants.TAP_POSITION_ACTION_AUTO);
    }

    public PropertyBags getStage() {
        return this.queryTripleStore(CsaProfileConstants.STAGE);
    }

    public PropertyBags getGridStateAlterationCollection() {
        return this.queryTripleStore(CsaProfileConstants.GRID_STATE_ALTERATION_COLLECTION);
    }

    public PropertyBags getRemedialActionScheme() {
        return this.queryTripleStore(CsaProfileConstants.REMEDIAL_ACTION_SCHEME, tripleStoreCsaProfileCrac.contextNames());
    }

    public PropertyBags getSchemeRemedialActions() {
        return this.queryTripleStore(CsaProfileConstants.SCHEME_REMEDIAL_ACTION);
    }

    public PropertyBags getRemedialActionsSchedule() {
        return this.queryTripleStore(CsaProfileConstants.REMEDIAL_ACTION_SCHEDULE, tripleStoreCsaProfileCrac.contextNames());
    }

    private PropertyBags queryTripleStore(String queryKey) {
        return this.queryTripleStore(queryKey, new HashSet<>());
    }

    private PropertyBags queryTripleStore(List<String> queryKeys, Set<String> contexts) {
        PropertyBags mergedPropertyBags = new PropertyBags();
        for (String queryKey : queryKeys) {
            mergedPropertyBags.addAll(queryTripleStore(queryKey, contexts));
        }
        return mergedPropertyBags;
    }

    /**
     * execute query on a specific context
     *
     * @param queryKey : query name in the sparql file
     * @param context : context where the query will be executed
     * */
    private PropertyBags queryTripleStore(String queryKey, String context) {
        String query = queryCatalogCsaProfileCrac.get(queryKey);
        if (query == null) {
            FaraoLoggerProvider.TECHNICAL_LOGS.warn("Query [{}] not found in catalog", queryKey);
            return new PropertyBags();
        }
        return tripleStoreCsaProfileCrac.query(String.format(query, context));
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

