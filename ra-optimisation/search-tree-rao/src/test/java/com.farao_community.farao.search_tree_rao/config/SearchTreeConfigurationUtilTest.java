/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.config;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.rao_api.RaoParameters;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class SearchTreeConfigurationUtilTest {

    private RaoParameters raoParameters = new RaoParameters();

    @Test
    public void getSearchTreeParametersOk() {
        raoParameters.addExtension(SearchTreeRaoParameters.class, new SearchTreeRaoParameters());
        assertNotNull(SearchTreeConfigurationUtil.getSearchTreeParameters(raoParameters));
    }

    @Test
    public void getSearchTreeParametersError() {
        try {
            SearchTreeConfigurationUtil.getSearchTreeParameters(raoParameters);
            fail();
        } catch (FaraoException e) {
            // should throw
        }
    }

    @Test
    public void checkSearchTreeRaoConfigurationOk() {
        raoParameters.addExtension(SearchTreeRaoParameters.class, new SearchTreeRaoParameters());
        raoParameters.getExtension(SearchTreeRaoParameters.class).setRangeActionRao("Linear Range Action Rao Mock");
        assertTrue(SearchTreeConfigurationUtil.checkSearchTreeRaoConfiguration(raoParameters).isEmpty());
    }

    @Test
    public void checkSearchTreeRaoConfigurationNoSearchTreeParameters() {
        List<String> errors = SearchTreeConfigurationUtil.checkSearchTreeRaoConfiguration(raoParameters);
        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).contains("Search Tree Rao parameters"));
    }

    @Test
    public void checkSearchTreeRaoConfigurationWithRangeActionRaoNotFound() {
        raoParameters.addExtension(SearchTreeRaoParameters.class, new SearchTreeRaoParameters());
        raoParameters.getExtension(SearchTreeRaoParameters.class).setRangeActionRao("Unknown Range Action Rao");
        List<String> errors = SearchTreeConfigurationUtil.checkSearchTreeRaoConfiguration(raoParameters);
        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).contains("not found"));
    }

    @Test
    public void checkSearchTreeRaoConfigurationWithForbiddenRangeActionRao() {
        raoParameters.addExtension(SearchTreeRaoParameters.class, new SearchTreeRaoParameters());
        raoParameters.getExtension(SearchTreeRaoParameters.class).setRangeActionRao("SearchTreeRao");
        List<String> errors = SearchTreeConfigurationUtil.checkSearchTreeRaoConfiguration(raoParameters);
        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).contains("cannot be the 'SearchTreeRao'"));
    }

}
