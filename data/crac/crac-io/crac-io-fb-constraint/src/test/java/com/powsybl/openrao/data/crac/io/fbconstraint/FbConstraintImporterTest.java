package com.powsybl.openrao.data.crac.io.fbconstraint;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class FbConstraintImporterTest extends TestBase {

    @Test
    void testExists() {
        final FbConstraintImporter importer = new FbConstraintImporter();
        Assertions.assertThat(importer.exists(getResourceAsReader("/merged_cb/complex_variants.xml"))).isTrue(); // v17
        Assertions.assertThat(importer.exists(getResourceAsReader("/merged_cb/thresholds_test.xml"))).isTrue(); // v18
        Assertions.assertThat(importer.exists(getResourceAsReader("/merged_cb/MNEC_test.xml"))).isTrue(); // v23
        Assertions.assertThat(importer.exists(getResourceAsReader("/merged_cb/with_xsd_v11.xml"))).isFalse(); // v11
    }
}
