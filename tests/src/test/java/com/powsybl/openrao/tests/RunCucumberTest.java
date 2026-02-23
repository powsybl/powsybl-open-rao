/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.tests;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.core.options.Constants.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
@Suite
@IncludeEngines("cucumber")
@SelectPackages("com.powsybl.openrao.tests")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.powsybl.openrao.tests")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "not @flaky and not @fast-rao and not @dont-run") // For SearchTreeRao testing
//@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "not @flaky and not @multi-curative and not @search-tree-rao") // For FastRao testing
public class RunCucumberTest {
}
