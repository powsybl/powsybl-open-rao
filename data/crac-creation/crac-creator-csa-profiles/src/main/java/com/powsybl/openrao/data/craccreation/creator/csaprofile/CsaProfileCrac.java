/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craccreation.creator.csaprofile;

import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracUtils;
import com.powsybl.openrao.data.nativecracapi.NativeCrac;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;
import com.powsybl.triplestore.api.QueryCatalog;
import com.powsybl.triplestore.api.TripleStore;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
public class CsaProfileCrac implements NativeCrac {

    private final TripleStore tripleStoreCsaProfileCrac;

    private final QueryCatalog queryCatalogCsaProfileCrac;

    private final Map<String, Set<String>> keywordMap;

    public CsaProfileCrac(TripleStore tripleStoreCsaProfileCrac, Map<String, Set<String>> keywordMap) {
        this.tripleStoreCsaProfileCrac = tripleStoreCsaProfileCrac;
        this.queryCatalogCsaProfileCrac = new QueryCatalog(CsaProfileConstants.SPARQL_FILE_CSA_PROFILE);
        this.keywordMap = keywordMap;
    }

    @Override
    public String getFormat() {
        return "CsaProfileCrac";
    }

    public void clearContext(String context) {
        tripleStoreCsaProfileCrac.clear(context);
    }

    public void clearKeywordMap(String context) {
        for (Map.Entry<String, Set<String>> entry : keywordMap.entrySet()) {
            String keyword = entry.getKey();
            Set<String> contextNames = entry.getValue();
            if (contextNames.contains(context)) {
                contextNames.remove(context);
                keywordMap.put(keyword, contextNames);
                break;
            }
        }
    }

    private Set<String> getContextNamesToRequest(String keyword) {
        if (keywordMap.containsKey(keyword)) {
            return keywordMap.get(keyword);
        }
        return Collections.emptySet();
    }

    public Map<String, PropertyBags> getHeaders() {
        Map<String, PropertyBags> returnMap = new HashMap<>();
        tripleStoreCsaProfileCrac.contextNames().forEach(context -> returnMap.put(context, queryTripleStore(CsaProfileConstants.REQUEST_HEADER, Set.of(context))));
        return returnMap;
    }

    public PropertyBags getPropertyBags(String csaProfileConstant, String keyword) {
        Set<String> namesToRequest = getContextNamesToRequest(keyword);
        if (namesToRequest.isEmpty()) {
            return new PropertyBags();
        }
        return this.queryTripleStore(csaProfileConstant, namesToRequest);
    }

    public PropertyBags getPropertyBags(List<String> csaProfileConstants, String keyword) {
        Set<String> namesToRequest = getContextNamesToRequest(keyword);
        if (namesToRequest.isEmpty()) {
            return new PropertyBags();
        }
        return this.queryTripleStore(csaProfileConstants, namesToRequest);
    }

    public PropertyBags getContingencies() {
        return getPropertyBags(Arrays.asList(CsaProfileConstants.REQUEST_ORDINARY_CONTINGENCY, CsaProfileConstants.REQUEST_EXCEPTIONAL_CONTINGENCY, CsaProfileConstants.REQUEST_OUT_OF_RANGE_CONTINGENCY), CsaProfileConstants.CsaProfileKeywords.CONTINGENCY.toString());
    }

    public PropertyBags getContingencyEquipments() {
        return getPropertyBags(CsaProfileConstants.REQUEST_CONTINGENCY_EQUIPMENT, CsaProfileConstants.CsaProfileKeywords.CONTINGENCY.toString());
    }

    public PropertyBags getAssessedElements() {
        return getPropertyBags(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT, CsaProfileConstants.CsaProfileKeywords.ASSESSED_ELEMENT.toString());
    }

    public PropertyBags getAssessedElementsWithContingencies() {
        return getPropertyBags(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_WITH_CONTINGENCY, CsaProfileConstants.CsaProfileKeywords.ASSESSED_ELEMENT.toString());
    }

    public PropertyBags getAssessedElementsWithRemedialAction() {
        return getPropertyBags(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_WITH_REMEDIAL_ACTION, CsaProfileConstants.CsaProfileKeywords.ASSESSED_ELEMENT.toString());
    }

    public PropertyBags getCurrentLimits() {
        return getPropertyBags(CsaProfileConstants.REQUEST_CURRENT_LIMIT, CsaProfileConstants.CGMES);
    }

    public PropertyBags getVoltageLimits() {
        return getPropertyBags(CsaProfileConstants.REQUEST_VOLTAGE_LIMIT, CsaProfileConstants.CGMES);
    }

    public PropertyBags getAngleLimits() {
        return getPropertyBags(CsaProfileConstants.REQUEST_ANGLE_LIMIT, CsaProfileConstants.CsaProfileKeywords.EQUIPMENT_RELIABILITY.toString());
    }

    public PropertyBags getRemedialActions() {
        return getPropertyBags(CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION, CsaProfileConstants.CsaProfileKeywords.REMEDIAL_ACTION.toString());
    }

    public PropertyBags getTopologyAction() {
        return getPropertyBags(CsaProfileConstants.TOPOLOGY_ACTION, CsaProfileConstants.CsaProfileKeywords.REMEDIAL_ACTION.toString());
    }

    public PropertyBags getRotatingMachineAction() {
        return getPropertyBags(CsaProfileConstants.ROTATING_MACHINE_ACTION, CsaProfileConstants.CsaProfileKeywords.REMEDIAL_ACTION.toString());
    }

    public PropertyBags getShuntCompensatorModifications() {
        return getPropertyBags(CsaProfileConstants.SHUNT_COMPENSATOR_MODIFICATION, CsaProfileConstants.CsaProfileKeywords.REMEDIAL_ACTION.toString());
    }

    public PropertyBags getTapPositionAction() {
        return getPropertyBags(CsaProfileConstants.TAP_POSITION_ACTION, CsaProfileConstants.CsaProfileKeywords.REMEDIAL_ACTION.toString());
    }

    public PropertyBags getStaticPropertyRanges() {
        return getPropertyBags(CsaProfileConstants.STATIC_PROPERTY_RANGE, CsaProfileConstants.CsaProfileKeywords.REMEDIAL_ACTION.toString());
    }

    public PropertyBags getContingencyWithRemedialAction() {
        return getPropertyBags(CsaProfileConstants.REQUEST_CONTINGENCY_WITH_REMEDIAL_ACTION, CsaProfileConstants.CsaProfileKeywords.REMEDIAL_ACTION.toString());
    }

    public PropertyBags getStage() {
        return getPropertyBags(CsaProfileConstants.STAGE, CsaProfileConstants.CsaProfileKeywords.REMEDIAL_ACTION.toString());
    }

    public PropertyBags getGridStateAlterationCollection() {
        return getPropertyBags(CsaProfileConstants.GRID_STATE_ALTERATION_COLLECTION, CsaProfileConstants.CsaProfileKeywords.REMEDIAL_ACTION.toString());
    }

    public PropertyBags getRemedialActionScheme() {
        return getPropertyBags(CsaProfileConstants.REMEDIAL_ACTION_SCHEME, CsaProfileConstants.CsaProfileKeywords.REMEDIAL_ACTION.toString());
    }

    public PropertyBags getSchemeRemedialActions() {
        return getPropertyBags(CsaProfileConstants.REQUEST_SCHEME_REMEDIAL_ACTION, CsaProfileConstants.CsaProfileKeywords.REMEDIAL_ACTION.toString());
    }

    public Map<String, String> getOverridingCracData(OffsetDateTime importTimestamp) {
        Map<String, String> overridingData = new HashMap<>();
        for (CsaProfileConstants.OverridingObjectsFields overridingObject : CsaProfileConstants.OverridingObjectsFields.values()) {
            addDataFromTripleStoreToMap(overridingData, overridingObject.getRequestName(), overridingObject.getObjectName(), overridingObject.getOverridedFieldName(), overridingObject.getHeaderType(), importTimestamp);
        }
        return overridingData;
    }

    private void addDataFromTripleStoreToMap(Map<String, String> dataMap, String queryName, String queryObjectName, String queryFieldName, CsaProfileConstants.HeaderType headerType, OffsetDateTime importTimestamp) {
        PropertyBags propertyBagsResult = queryTripleStore(queryName, tripleStoreCsaProfileCrac.contextNames());
        for (PropertyBag propertyBag : propertyBagsResult) {
            if (CsaProfileConstants.HeaderType.START_END_DATE.equals(headerType)) {
                if (CsaProfileCracUtils.checkProfileKeyword(propertyBag, CsaProfileConstants.CsaProfileKeywords.SSI) && CsaProfileCracUtils.checkProfileValidityInterval(propertyBag, importTimestamp)) {
                    String id = propertyBag.getId(queryObjectName);
                    String overridedValue = propertyBag.get(queryFieldName);
                    dataMap.put(id, overridedValue);
                }
            } else {
                if (CsaProfileCracUtils.checkProfileKeyword(propertyBag, CsaProfileConstants.CsaProfileKeywords.SSH)) {
                    OffsetDateTime scenarioTime = OffsetDateTime.parse(propertyBag.get(CsaProfileConstants.SCENARIO_TIME));
                    if (importTimestamp.isEqual(scenarioTime)) {
                        String id = propertyBag.getId(queryObjectName);
                        String overridedValue = propertyBag.get(queryFieldName);
                        dataMap.put(id, overridedValue);
                    }
                }
            }

        }
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
     */
    private PropertyBags queryTripleStore(String queryKey, Set<String> contexts) {
        String query = queryCatalogCsaProfileCrac.get(queryKey);
        if (query == null) {
            OpenRaoLoggerProvider.TECHNICAL_LOGS.warn("Query [{}] not found in catalog", queryKey);
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
