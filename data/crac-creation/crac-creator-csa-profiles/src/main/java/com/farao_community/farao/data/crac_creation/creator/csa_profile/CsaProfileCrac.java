/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.csa_profile;

import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileConstants;
import com.farao_community.farao.data.native_crac_api.NativeCrac;
import com.powsybl.triplestore.api.PropertyBags;
import com.powsybl.triplestore.api.QueryCatalog;
import com.powsybl.triplestore.api.TripleStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
public class CsaProfileCrac implements NativeCrac {

    private final TripleStore tripleStoreCsaProfileCrac;

    private final QueryCatalog queryCatalogCsaProfileCrac;

    private static final Logger LOGGER = LoggerFactory.getLogger(CsaProfileCrac.class);

    public CsaProfileCrac(TripleStore tripleStoreCsaProfileCrac) {
        this.tripleStoreCsaProfileCrac = tripleStoreCsaProfileCrac;
        this.queryCatalogCsaProfileCrac = new QueryCatalog(CsaProfileConstants.SPARQL_FILE_CSA_PROFILE);
    }

    @Override
    public String getFormat() {
        return "CsaProfileCrac";
    }

    private PropertyBags queryTripleStore(String queryKey) {
        String query = queryCatalogCsaProfileCrac.get(queryKey);
        if (query == null) {
            LOGGER.warn("Query [{}] not found in catalog", queryKey);
            return new PropertyBags();
        }
        return tripleStoreCsaProfileCrac.query(query);
    }

    public PropertyBags getContingencies() {
        return this.queryTripleStore(CsaProfileConstants.REQUEST_CONTINGENCIES);
    }
}
