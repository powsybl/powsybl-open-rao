/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craccreation.creator.csaprofile;

import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.NcPropertyBagsConverter;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.CurrentLimit;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.GridStateAlterationRemedialAction;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.SchemeRemedialAction;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.VoltageLimit;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracUtils;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.AssessedElement;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.AssessedElementWithContingency;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.AssessedElementWithRemedialAction;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.Contingency;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.ContingencyEquipment;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.ContingencyWithRemedialAction;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.GridStateAlterationCollection;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.RemedialActionDependency;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.RemedialActionGroup;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.RemedialActionScheme;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.RotatingMachineAction;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.ShuntCompensatorModification;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.Stage;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.StaticPropertyRange;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.TapPositionAction;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.TopologyAction;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.VoltageAngleLimit;
import com.powsybl.openrao.data.nativecracapi.NativeCrac;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;
import com.powsybl.triplestore.api.QueryCatalog;
import com.powsybl.triplestore.api.TripleStore;

import java.time.OffsetDateTime;
import java.util.*;

import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracUtils.isValidInterval;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
public class CsaProfileCrac implements NativeCrac {

    private final TripleStore tripleStoreCsaProfileCrac;

    private final QueryCatalog queryCatalogCsaProfileCrac;

    private final Map<String, Set<String>> keywordMap;
    private Map<String, String> overridingData;

    public CsaProfileCrac(TripleStore tripleStoreCsaProfileCrac, Map<String, Set<String>> keywordMap) {
        this.tripleStoreCsaProfileCrac = tripleStoreCsaProfileCrac;
        this.queryCatalogCsaProfileCrac = new QueryCatalog(CsaProfileConstants.SPARQL_FILE_CSA_PROFILE);
        this.keywordMap = keywordMap;
        this.overridingData = new HashMap<>();
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

    private Set<String> getContextNamesToRequest(CsaProfileConstants.CsaProfileKeyword keyword) {
        return keywordMap.getOrDefault(keyword.toString(), Collections.emptySet());
    }

    public Map<String, PropertyBags> getHeaders() {
        Map<String, PropertyBags> returnMap = new HashMap<>();
        tripleStoreCsaProfileCrac.contextNames().forEach(context -> returnMap.put(context, queryTripleStore(CsaProfileConstants.REQUEST_HEADER, Set.of(context))));
        return returnMap;
    }

    public PropertyBags getPropertyBags(CsaProfileConstants.CsaProfileKeyword keyword, String... queries) {
        Set<String> namesToRequest = getContextNamesToRequest(keyword);
        if (namesToRequest.isEmpty()) {
            return new PropertyBags();
        }
        return this.queryTripleStore(List.of(queries), namesToRequest);
    }

    public PropertyBags getPropertyBags(CsaProfileConstants.CsaProfileKeyword keyword, CsaProfileConstants.OverridingObjectsFields withOverride, String... queries) {
        return withOverride == null ? getPropertyBags(keyword, queries) : CsaProfileCracUtils.overrideData(getPropertyBags(keyword, queries), overridingData, withOverride);
    }

    public Set<Contingency> getContingencies() {
        return new NcPropertyBagsConverter<>(Contingency::fromPropertyBag).convert(getPropertyBags(CsaProfileConstants.CsaProfileKeyword.CONTINGENCY, CsaProfileConstants.OverridingObjectsFields.CONTINGENCY, CsaProfileConstants.REQUEST_ORDINARY_CONTINGENCY, CsaProfileConstants.REQUEST_EXCEPTIONAL_CONTINGENCY, CsaProfileConstants.REQUEST_OUT_OF_RANGE_CONTINGENCY));
    }

    public Set<ContingencyEquipment> getContingencyEquipments() {
        return new NcPropertyBagsConverter<>(ContingencyEquipment::fromPropertyBag).convert(getPropertyBags(CsaProfileConstants.CsaProfileKeyword.CONTINGENCY, CsaProfileConstants.REQUEST_CONTINGENCY_EQUIPMENT));
    }

    public Set<AssessedElement> getAssessedElements() {
        return new NcPropertyBagsConverter<>(AssessedElement::fromPropertyBag).convert(getPropertyBags(CsaProfileConstants.CsaProfileKeyword.ASSESSED_ELEMENT, CsaProfileConstants.OverridingObjectsFields.ASSESSED_ELEMENT, CsaProfileConstants.REQUEST_ASSESSED_ELEMENT));
    }

    public Set<AssessedElementWithContingency> getAssessedElementWithContingencies() {
        return new NcPropertyBagsConverter<>(AssessedElementWithContingency::fromPropertyBag).convert(getPropertyBags(CsaProfileConstants.CsaProfileKeyword.ASSESSED_ELEMENT, CsaProfileConstants.OverridingObjectsFields.ASSESSED_ELEMENT_WITH_CONTINGENCY, CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_WITH_CONTINGENCY));
    }

    public Set<AssessedElementWithRemedialAction> getAssessedElementWithRemedialActions() {
        return new NcPropertyBagsConverter<>(AssessedElementWithRemedialAction::fromPropertyBag).convert(getPropertyBags(CsaProfileConstants.CsaProfileKeyword.ASSESSED_ELEMENT, CsaProfileConstants.OverridingObjectsFields.ASSESSED_ELEMENT_WITH_REMEDIAL_ACTION, CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_WITH_REMEDIAL_ACTION));
    }

    public Set<CurrentLimit> getCurrentLimits() {
        return new NcPropertyBagsConverter<>(CurrentLimit::fromPropertyBag).convert(getPropertyBags(CsaProfileConstants.CsaProfileKeyword.CGMES, CsaProfileConstants.OverridingObjectsFields.CURRENT_LIMIT, CsaProfileConstants.REQUEST_CURRENT_LIMIT));
    }

    public Set<VoltageLimit> getVoltageLimits() {
        return new NcPropertyBagsConverter<>(VoltageLimit::fromPropertyBag).convert(getPropertyBags(CsaProfileConstants.CsaProfileKeyword.CGMES, CsaProfileConstants.OverridingObjectsFields.VOLTAGE_LIMIT, CsaProfileConstants.REQUEST_VOLTAGE_LIMIT));
    }

    public Set<VoltageAngleLimit> getVoltageAngleLimits() {
        return new NcPropertyBagsConverter<>(VoltageAngleLimit::fromPropertyBag).convert(getPropertyBags(CsaProfileConstants.CsaProfileKeyword.EQUIPMENT_RELIABILITY, CsaProfileConstants.OverridingObjectsFields.VOLTAGE_ANGLE_LIMIT, CsaProfileConstants.REQUEST_VOLTAGE_ANGLE_LIMIT));
    }

    public Set<GridStateAlterationRemedialAction> getGridStateAlterationRemedialActions() {
        return new NcPropertyBagsConverter<>(GridStateAlterationRemedialAction::fromPropertyBag).convert(getPropertyBags(CsaProfileConstants.CsaProfileKeyword.REMEDIAL_ACTION, CsaProfileConstants.OverridingObjectsFields.GRID_STATE_ALTERATION_REMEDIAL_ACTION, CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION));
    }

    public Set<TopologyAction> getTopologyActions() {
        return new NcPropertyBagsConverter<>(TopologyAction::fromPropertyBag).convert(getPropertyBags(CsaProfileConstants.CsaProfileKeyword.REMEDIAL_ACTION, CsaProfileConstants.OverridingObjectsFields.TOPOLOGY_ACTION, CsaProfileConstants.TOPOLOGY_ACTION));
    }

    public Set<RotatingMachineAction> getRotatingMachineActions() {
        return new NcPropertyBagsConverter<>(RotatingMachineAction::fromPropertyBag).convert(getPropertyBags(CsaProfileConstants.CsaProfileKeyword.REMEDIAL_ACTION, CsaProfileConstants.OverridingObjectsFields.ROTATING_MACHINE_ACTION, CsaProfileConstants.ROTATING_MACHINE_ACTION));
    }

    public Set<ShuntCompensatorModification> getShuntCompensatorModifications() {
        return new NcPropertyBagsConverter<>(ShuntCompensatorModification::fromPropertyBag).convert(getPropertyBags(CsaProfileConstants.CsaProfileKeyword.REMEDIAL_ACTION, CsaProfileConstants.OverridingObjectsFields.SHUNT_COMPENSATOR_MODIFICATION, CsaProfileConstants.SHUNT_COMPENSATOR_MODIFICATION));
    }

    public Set<TapPositionAction> getTapPositionActions() {
        return new NcPropertyBagsConverter<>(TapPositionAction::fromPropertyBag).convert(getPropertyBags(CsaProfileConstants.CsaProfileKeyword.REMEDIAL_ACTION, CsaProfileConstants.OverridingObjectsFields.TAP_POSITION_ACTION, CsaProfileConstants.TAP_POSITION_ACTION));
    }

    public Set<StaticPropertyRange> getStaticPropertyRanges() {
        return new NcPropertyBagsConverter<>(StaticPropertyRange::fromPropertyBag).convert(getPropertyBags(CsaProfileConstants.CsaProfileKeyword.REMEDIAL_ACTION, CsaProfileConstants.OverridingObjectsFields.STATIC_PROPERTY_RANGE, CsaProfileConstants.STATIC_PROPERTY_RANGE));
    }

    public Set<ContingencyWithRemedialAction> getContingencyWithRemedialActions() {
        return new NcPropertyBagsConverter<>(ContingencyWithRemedialAction::fromPropertyBag).convert(getPropertyBags(CsaProfileConstants.CsaProfileKeyword.REMEDIAL_ACTION, CsaProfileConstants.OverridingObjectsFields.CONTINGENCY_WITH_REMEDIAL_ACTION, CsaProfileConstants.REQUEST_CONTINGENCY_WITH_REMEDIAL_ACTION));
    }

    public Set<Stage> getStages() {
        return new NcPropertyBagsConverter<>(Stage::fromPropertyBag).convert(getPropertyBags(CsaProfileConstants.CsaProfileKeyword.REMEDIAL_ACTION, CsaProfileConstants.STAGE));
    }

    public Set<GridStateAlterationCollection> getGridStateAlterationCollections() {
        return new NcPropertyBagsConverter<>(GridStateAlterationCollection::fromPropertyBag).convert(getPropertyBags(CsaProfileConstants.CsaProfileKeyword.REMEDIAL_ACTION, CsaProfileConstants.GRID_STATE_ALTERATION_COLLECTION));
    }

    public Set<RemedialActionScheme> getRemedialActionSchemes() {
        return new NcPropertyBagsConverter<>(RemedialActionScheme::fromPropertyBag).convert(getPropertyBags(CsaProfileConstants.CsaProfileKeyword.REMEDIAL_ACTION, CsaProfileConstants.OverridingObjectsFields.REMEDIAL_ACTION_SCHEME, CsaProfileConstants.REMEDIAL_ACTION_SCHEME));
    }

    public Set<SchemeRemedialAction> getSchemeRemedialActions() {
        return new NcPropertyBagsConverter<>(SchemeRemedialAction::fromPropertyBag).convert(getPropertyBags(CsaProfileConstants.CsaProfileKeyword.REMEDIAL_ACTION, CsaProfileConstants.OverridingObjectsFields.SCHEME_REMEDIAL_ACTION, CsaProfileConstants.REQUEST_SCHEME_REMEDIAL_ACTION));
    }

    public Set<RemedialActionGroup> getRemedialActionGroups() {
        return new NcPropertyBagsConverter<>(RemedialActionGroup::fromPropertyBag).convert(getPropertyBags(CsaProfileConstants.CsaProfileKeyword.REMEDIAL_ACTION, CsaProfileConstants.REQUEST_REMEDIAL_ACTION_GROUP));

    }

    public Set<RemedialActionDependency> getRemedialActionDependencies() {
        return new NcPropertyBagsConverter<>(RemedialActionDependency::fromPropertyBag).convert(getPropertyBags(CsaProfileConstants.CsaProfileKeyword.REMEDIAL_ACTION, CsaProfileConstants.OverridingObjectsFields.SCHEME_REMEDIAL_ACTION_DEPENDENCY, CsaProfileConstants.REQUEST_REMEDIAL_ACTION_DEPENDENCY));
    }

    private void setOverridingData(OffsetDateTime importTimestamp) {
        overridingData = new HashMap<>();
        for (CsaProfileConstants.OverridingObjectsFields overridingObject : CsaProfileConstants.OverridingObjectsFields.values()) {
            addDataFromTripleStoreToMap(overridingData, overridingObject.getRequestName(), overridingObject.getObjectName(), overridingObject.getOverridedFieldName(), overridingObject.getHeaderType(), importTimestamp);
        }
    }

    private void addDataFromTripleStoreToMap(Map<String, String> dataMap, String queryName, String queryObjectName, String queryFieldName, CsaProfileConstants.HeaderType headerType, OffsetDateTime importTimestamp) {
        PropertyBags propertyBagsResult = queryTripleStore(queryName, tripleStoreCsaProfileCrac.contextNames());
        for (PropertyBag propertyBag : propertyBagsResult) {
            if (CsaProfileConstants.HeaderType.START_END_DATE.equals(headerType)) {
                if (CsaProfileCracUtils.checkProfileKeyword(propertyBag, CsaProfileConstants.CsaProfileKeyword.STEADY_STATE_INSTRUCTION) && CsaProfileCracUtils.checkProfileValidityInterval(propertyBag, importTimestamp)) {
                    String id = propertyBag.getId(queryObjectName);
                    String overridedValue = propertyBag.get(queryFieldName);
                    dataMap.put(id, overridedValue);
                }
            } else {
                if (CsaProfileCracUtils.checkProfileKeyword(propertyBag, CsaProfileConstants.CsaProfileKeyword.STEADY_STATE_HYPOTHESIS)) {
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

    public void setForTimestamp(OffsetDateTime offsetDateTime) {
        clearTimewiseIrrelevantContexts(offsetDateTime);
        setOverridingData(offsetDateTime);
    }

    private void clearTimewiseIrrelevantContexts(OffsetDateTime offsetDateTime) {
        getHeaders().forEach((contextName, properties) -> {
            if (!properties.isEmpty()) {
                PropertyBag property = properties.get(0);
                if (!checkTimeCoherence(property, offsetDateTime)) {
                    OpenRaoLoggerProvider.BUSINESS_WARNS.warn(String.format("[REMOVED] The file : %s will be ignored. Its dates are not consistent with the import date : %s", contextName, offsetDateTime));
                    clearContext(contextName);
                    clearKeywordMap(contextName);
                }
            }
        });
    }

    private static boolean checkTimeCoherence(PropertyBag header, OffsetDateTime offsetDateTime) {
        String startTime = header.getId(CsaProfileConstants.REQUEST_HEADER_START_DATE);
        String endTime = header.getId(CsaProfileConstants.REQUEST_HEADER_END_DATE);
        return isValidInterval(offsetDateTime, startTime, endTime);
    }
}
