/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.craccreator;

import com.powsybl.openrao.data.crac.io.nc.NcCrac;
import com.powsybl.openrao.data.crac.io.nc.craccreator.constants.NcVersion;
import com.powsybl.openrao.data.crac.io.nc.objects.Contingency;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Version detection and read-contract gate of the NC CRAC importer (issue #2057).
 * <p>
 * The ReliCapGrid fixtures come verbatim from the published test package of
 * https://github.com/entsoe/relicapgrid (tag {@code cgmes-3.0_ncp-2.5_tc-2.0}): the
 * {@code NetworkCode/cimxml} files of the nine test systems (and the common / reference data)
 * for the NCP profiles, the corresponding {@code Grid/cimxml} and {@code boundaryData} CGMES 3.0
 * network models (1D, 2D views, SSH revisions and border files) for each system, and
 * {@code NetworkCode/cimxml/Galia_CO.xml} for the contingency profile.
 * The {@code Galia_SIS.xml} file is the only addition: it comes from the CORE ROSC mock
 * package (the ReliCapGrid repository publishes no SIS for Galia).
 * <p>
 * The mock fixture ({@code ReliCapGridMock_NCP25_AllSystems.zip}) mirrors the file list of the
 * production delivery (a wrapper zip containing the {@code CGM_package_hvdc.zip} and
 * {@code NCP_mock_hvdc.zip} sub-zips, flattened here as the NC importer receives the content):
 * the curated CGMES 3.0 subset (SSH revision 2 and a few 2D views, EQ models, border files,
 * common data) and the reduced NCP mock profiles (AE, CO, RA, SIS/SHS, OR).
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class NcCracV25Test {

    @Test
    void ncp25VocabularyPackageIsDetectedAndImported() {
        assertEquals(NcVersion.V25, importZip("/profiles/ncp25/NCP25.zip").getVersion());
    }

    @Test
    void ncp24PackageIsStillDetectedAsV24() {
        assertEquals(NcVersion.V24, importZip("/profiles/contingencies/Contingencies.zip").getVersion());
    }

    @Test
    void ncp25ConformsToPackageIsDetectedAndImported() {
        NcCrac ncCrac = importZip("/profiles/ncp25/ContingenciesV25.zip");
        assertEquals(NcVersion.V25, ncCrac.getVersion());
        Set<String> mrids = ncCrac.getContingencies().stream().map(Contingency::mrid).collect(Collectors.toSet());
        assertTrue(mrids.contains("contingency-1"));
        assertTrue(mrids.contains("contingency-2"));
        assertEquals(12, ncCrac.getContingencies().size());
    }

    @Test
    void relicapgridNcp25ContingencyProfileIsImported() {
        NcCrac ncCrac = importZip("/profiles/ncp25/Galia_CO.zip");
        assertGaliaContingencies(ncCrac);
        // NCP 2.5 relaxes the cardinality of normalMustStudy: absent means the contingency is studied
        assertTrue(ncCrac.getContingencies().stream().allMatch(Contingency::normalMustStudy));
    }

    @Test
    void relicapgridNcp25PackageWithCgmesModelIsImported() {
        assertGaliaContingencies(importZip("/profiles/ncp25/ReliCapGridGalia_NCP25_CGMES30.zip"));
    }

    @Test
    void relicapgridFullNcp25PackageOfAllSystemsIsImported() {
        // the complete release: the NCP profiles of the nine systems together with their
        // corresponding CGMES 3.0 network models (1D and 2D views)
        NcCrac ncCrac = importZip("/profiles/ncp25/ReliCapGrid_NCP25_AllSystems.zip");
        assertEquals(NcVersion.V25, ncCrac.getVersion());
        // all the contingencies of the nine ReliCapGrid systems (CO profiles, incl. the Svedala-Belgovia
        // cross-system profile) are imported. The Belgovia versioned datasets reference the same
        // contingency resources as Belgovia_CO, so they do not add contingency instances.
        assertEquals(325, ncCrac.getContingencies().size());
        Set<String> mrids = ncCrac.getContingencies().stream().map(Contingency::mrid).collect(Collectors.toSet());
        // one contingency per system
        assertTrue(mrids.contains("bd7bb012-f7b9-45e0-9e15-4e2aa3592829"), "Galia contingency missing");
        assertTrue(mrids.contains("5d587c7e-9ced-416a-ad17-6ef9b241a998"), "Svedala contingency missing");
        assertTrue(mrids.contains("ee6365ea-2277-422a-a70b-182280debf27"), "Svedala-Belgovia contingency missing");
        assertTrue(mrids.contains("ce19dd34-429e-4b72-8813-7615cc57b4a4"), "Espheim contingency missing");
        assertTrue(mrids.contains("37997e71-cb7d-4a8c-baa6-2a1594956da9"), "Belgovia contingency missing");
        assertTrue(mrids.contains("fb3a0672-5519-4a19-a10e-3989cc00af7c"), "Britheim contingency missing");
        assertTrue(mrids.contains("66627b36-aecd-4141-ab99-4bd8a74d18a8"), "DC-Espheim-Svedala contingency missing");
    }

    @Test
    void relicapgridMockProductionPackageIsImported() {
        // the production mock delivery: the curated CGMES 3.0 file list (SSH revision 2 and a few
        // 2D views, EQ models, border files, common data) together with the reduced NCP mock
        // profiles (AE, CO, RA, SIS/SHS, OR of four systems)
        NcCrac ncCrac = importZip("/profiles/ncp25/ReliCapGridMock_NCP25_AllSystems.zip");
        assertEquals(NcVersion.V25, ncCrac.getVersion());
        assertEquals(16, ncCrac.getContingencies().size());
        Set<String> mrids = ncCrac.getContingencies().stream().map(Contingency::mrid).collect(Collectors.toSet());
        assertTrue(mrids.contains("bd7bb012-f7b9-45e0-9e15-4e2aa3592829"), "Galia contingency missing");
        assertTrue(mrids.contains("5d587c7e-9ced-416a-ad17-6ef9b241a998"), "Svedala contingency missing");
        assertTrue(mrids.contains("ce19dd34-429e-4b72-8813-7615cc57b4a4"), "Espheim contingency missing");
        assertTrue(mrids.contains("37997e71-cb7d-4a8c-baa6-2a1594956da9"), "Belgovia contingency missing");
    }

    private static NcCrac importZip(String path) {
        try (InputStream in = NcCracV25Test.class.getResourceAsStream(path)) {
            return new NcCracImporter().importNativeCrac(in);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static void assertGaliaContingencies(NcCrac ncCrac) {
        assertEquals(NcVersion.V25, ncCrac.getVersion());
        Set<String> names = ncCrac.getContingencies().stream().map(Contingency::name).collect(Collectors.toSet());
        assertTrue(names.contains("CO_HVDC"), "expected the CO_HVDC contingency of the ReliCapGrid profile, got " + names);
        assertTrue(names.contains("CO3"), "expected the CO3 contingency of the ReliCapGrid profile, got " + names);
        assertEquals(12, ncCrac.getContingencies().size());
    }
}
