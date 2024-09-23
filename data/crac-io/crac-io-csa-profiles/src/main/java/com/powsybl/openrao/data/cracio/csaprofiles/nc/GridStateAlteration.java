package com.powsybl.openrao.data.cracio.csaprofiles.nc;

public interface GridStateAlteration extends NCObject {
    String propertyReference();

    Boolean normalEnabled();

    String gridStateAlterationRemedialAction();

    String gridStateAlterationCollection();
}
