/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.commons;

import com.google.auto.service.AutoService;
import com.powsybl.tools.Version;
import com.powsybl.tools.AbstractVersion;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@AutoService(Version.class)
public class OpenRaoVersion extends AbstractVersion {

    public OpenRaoVersion() {
        super("open-rao", "${project.version}", "${buildNumber}", "${scmBranch}", Long.parseLong("${timestamp}"));
    }
}
