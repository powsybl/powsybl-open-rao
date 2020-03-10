/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.config;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.rao_api.Rao;
import com.farao_community.farao.rao_api.RaoParameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Utility class designed to check availability of all plugins listed in the
 * RAO parameters, and to interpret them in different contexts of the search
 * tree RAO.
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public final class SearchTreeConfigurationUtil {

    private SearchTreeConfigurationUtil() {
        throw new AssertionError("Utility class should not be instanciated");
    }

    /**
     * Validates RAO parameters compatibility with platform available plugins.
     * Return a list of errors, that is empty if the configuration is correct.
     *
     * @param raoParameters RAO parameters
     * @return a list of configuration issues
     */
    public static List<String> checkSearchTreeRaoConfiguration(RaoParameters raoParameters) {
        List<String> errors = new ArrayList<>();

        // Check that correct extension is provided
        // Return directly if the extension is not provided
        SearchTreeRaoParameters searchTreeRaoParameters;
        try {
            searchTreeRaoParameters = getSearchTreeParameters(raoParameters);
        } catch (FaraoException e) {
            errors.add(e.getMessage());
            return errors;
        }

        // check that RangeActionRao exists
        try {
            Rao.find(searchTreeRaoParameters.getRangeActionRao());
        } catch (FaraoException e) {
            errors.add(String.format("Range action Rao '%s' not found", searchTreeRaoParameters.getRangeActionRao()));
        }

        // check that RangeActionRao is not the Search Tree Rao
        if (searchTreeRaoParameters.getRangeActionRao().equals("SearchTreeRao")) {
            errors.add("Range action Rao cannot be the 'SearchTreeRao'");
        }

        return errors;
    }

    /**
     * Get SearchTreeRaoParameters from a RaoParameters
     * Throws a FaraoException if it does not exists
     */
    public static SearchTreeRaoParameters getSearchTreeParameters(RaoParameters raoParameters) {
        SearchTreeRaoParameters searchTreeRaoParameters = raoParameters.getExtension(SearchTreeRaoParameters.class);
        if (Objects.isNull(searchTreeRaoParameters)) {
            throw new FaraoException("Search Tree Rao parameters not available");
        }
        return searchTreeRaoParameters;
    }
}
