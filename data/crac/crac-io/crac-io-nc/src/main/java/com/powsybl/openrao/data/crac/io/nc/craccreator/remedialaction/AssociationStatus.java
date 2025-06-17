/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
<<<<<<<< HEAD:data/crac/crac-io/crac-io-nc/src/main/java/com/powsybl/openrao/data/crac/io/nc/objects/Association.java
package com.powsybl.openrao.data.crac.io.nc.objects;
========
package com.powsybl.openrao.data.crac.io.nc.craccreator.remedialaction;
>>>>>>>> main:data/crac/crac-io/crac-io-nc/src/main/java/com/powsybl/openrao/data/crac/io/nc/craccreator/remedialaction/AssociationStatus.java

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public record AssociationStatus(boolean isValid, String statusDetails) {
}
