package com.powsybl.openrao.data.crac.io.fbconstraint;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.CracCreationContext;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.crac.io.fbconstraint.parameters.FbConstraintCracCreationParameters;
import com.powsybl.openrao.virtualhubs.VirtualHubsConfiguration;
import com.powsybl.openrao.virtualhubs.xml.XmlVirtualHubsConfiguration;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

class FbConstraintImporterTest {

    @Test
    void testExistsFileIsNotXml() {
        final FbConstraintImporter importer = new FbConstraintImporter();
        Assertions.assertThat(importer.exists("complex_variants.txt", getClass().getResourceAsStream("/merged_cb/complex_variants.xml"))).isFalse();
    }

    @Test
    void testExistsFileHasInvalidFormat() {
        final FbConstraintImporter importer = new FbConstraintImporter();
        Assertions.assertThat(importer.exists("with_invalid_format.xml", getClass().getResourceAsStream("/merged_cb/with_invalid_format.xml"))).isFalse();
    }

    @Test
    void testExistsFileHasUnsupportedFormat() {
        final FbConstraintImporter importer = new FbConstraintImporter();
        Assertions.assertThat(importer.exists("with_xsd_v11.xml", getClass().getResourceAsStream("/merged_cb/with_xsd_v11.xml"))).isFalse(); // v11
    }

    @Test
    void testExistsOK() {
        final FbConstraintImporter importer = new FbConstraintImporter();
        Assertions.assertThat(importer.exists("complex_variants.xml", getClass().getResourceAsStream("/merged_cb/complex_variants.xml"))).isTrue(); // v17
        Assertions.assertThat(importer.exists("thresholds_test.xml", getClass().getResourceAsStream("/merged_cb/thresholds_test.xml"))).isTrue(); // v18
        Assertions.assertThat(importer.exists("MNEC_test.xml", getClass().getResourceAsStream("/merged_cb/MNEC_test.xml"))).isTrue(); // v23
    }

    @Test
    void testImportHvdcNoComplexVariant() {
        // Given
        final VirtualHubsConfiguration virtualHubsConfiguration = XmlVirtualHubsConfiguration.importConfiguration(getClass().getResourceAsStream("/fake-hvdc/virtualhubsconfiguration.xml"));
        final FbConstraintCracCreationParameters fbConstraintCracCreationParameters = new FbConstraintCracCreationParameters();
        fbConstraintCracCreationParameters.setTimestamp(OffsetDateTime.parse("2026-01-27T17:00Z"));
        fbConstraintCracCreationParameters.setInternalHvdcs(virtualHubsConfiguration.getInternalHvdcs());
        final CracCreationParameters cracCreationParameters = new CracCreationParameters();
        cracCreationParameters.addExtension(FbConstraintCracCreationParameters.class, fbConstraintCracCreationParameters);
        final Network network = Network.read("network_mini2.uct", getClass().getResourceAsStream("/fake-hvdc/network_mini2.uct"));

        // When
        final CracCreationContext context = new FbConstraintImporter().importData(
            getClass().getResourceAsStream("/fake-hvdc/crac_without_complex_variant.xml"),
            cracCreationParameters,
            network);

        // Then
        final Set<InjectionRangeAction> injectionRangeActions = context.getCrac().getInjectionRangeActions();
        Assertions.assertThat(injectionRangeActions).isEmpty();
        Assertions.assertThat(context.getCreationReport().getReport())
            .contains("[WARN] the flow-based constraint document does not contain any complex variant");
    }

    @Test
    void testImportHvdcNoComplexVariantForTimestamp() {
        // Given
        final VirtualHubsConfiguration virtualHubsConfiguration = XmlVirtualHubsConfiguration.importConfiguration(getClass().getResourceAsStream("/fake-hvdc/virtualhubsconfiguration.xml"));
        final FbConstraintCracCreationParameters fbConstraintCracCreationParameters = new FbConstraintCracCreationParameters();
        fbConstraintCracCreationParameters.setTimestamp(OffsetDateTime.parse("2026-01-27T00:00Z"));
        fbConstraintCracCreationParameters.setInternalHvdcs(virtualHubsConfiguration.getInternalHvdcs());
        final CracCreationParameters cracCreationParameters = new CracCreationParameters();
        cracCreationParameters.addExtension(FbConstraintCracCreationParameters.class, fbConstraintCracCreationParameters);
        final Network network = Network.read("network_mini2.uct", getClass().getResourceAsStream("/fake-hvdc/network_mini2.uct"));

        // When
        final CracCreationContext context = new FbConstraintImporter().importData(
            getClass().getResourceAsStream("/fake-hvdc/crac.xml"),
            cracCreationParameters,
            network);

        // Then
        final Set<InjectionRangeAction> injectionRangeActions = context.getCrac().getInjectionRangeActions();
        Assertions.assertThat(injectionRangeActions).isEmpty();
        Assertions.assertThat(context.getCreationReport().getReport())
            .contains("[WARN] the flow-based constraint document does not contain any complex variant for the requested timestamp");
    }

    @Test
    void testImportHvdcDisconnectedFromMainComponent() {
        // Given
        final VirtualHubsConfiguration virtualHubsConfiguration = XmlVirtualHubsConfiguration.importConfiguration(getClass().getResourceAsStream("/fake-hvdc/virtualhubsconfiguration.xml"));
        final FbConstraintCracCreationParameters fbConstraintCracCreationParameters = new FbConstraintCracCreationParameters();
        fbConstraintCracCreationParameters.setTimestamp(OffsetDateTime.parse("2026-01-27T17:00Z"));
        fbConstraintCracCreationParameters.setInternalHvdcs(virtualHubsConfiguration.getInternalHvdcs());
        final CracCreationParameters cracCreationParameters = new CracCreationParameters();
        cracCreationParameters.addExtension(FbConstraintCracCreationParameters.class, fbConstraintCracCreationParameters);
        final Network network = Network.read("network_mini2_hvdc_disconnected_from_main_connected_component.uct", getClass().getResourceAsStream("/fake-hvdc/network_mini2_hvdc_disconnected_from_main_connected_component.uct"));

        // When
        final CracCreationContext context = new FbConstraintImporter().importData(
            getClass().getResourceAsStream("/fake-hvdc/crac.xml"),
            cracCreationParameters,
            network);

        // Then
        final Set<InjectionRangeAction> injectionRangeActions = context.getCrac().getInjectionRangeActions();
        Assertions.assertThat(injectionRangeActions).isEmpty();
    }

    @Test
    void testImportHvdc() {
        // Given
        final VirtualHubsConfiguration virtualHubsConfiguration = XmlVirtualHubsConfiguration.importConfiguration(getClass().getResourceAsStream("/fake-hvdc/virtualhubsconfiguration.xml"));
        final FbConstraintCracCreationParameters fbConstraintCracCreationParameters = new FbConstraintCracCreationParameters();
        fbConstraintCracCreationParameters.setTimestamp(OffsetDateTime.parse("2026-01-27T17:00Z"));
        fbConstraintCracCreationParameters.setInternalHvdcs(virtualHubsConfiguration.getInternalHvdcs());
        final CracCreationParameters cracCreationParameters = new CracCreationParameters();
        cracCreationParameters.addExtension(FbConstraintCracCreationParameters.class, fbConstraintCracCreationParameters);
        final Network network = Network.read("network_mini2.uct", getClass().getResourceAsStream("/fake-hvdc/network_mini2.uct"));

        // When
        final CracCreationContext context = new FbConstraintImporter().importData(
            getClass().getResourceAsStream("/fake-hvdc/crac.xml"),
            cracCreationParameters,
            network);

        // Then
        final List<InjectionRangeAction> injectionRangeActions = context.getCrac().getInjectionRangeActions().stream()
            .sorted(Comparator.comparing(InjectionRangeAction::getId))
            .toList();
        Assertions.assertThat(injectionRangeActions).hasSize(2);

        final InjectionRangeAction firstRA = injectionRangeActions.get(0);
        final InjectionRangeAction secondRA = injectionRangeActions.get(1);

        SoftAssertions softAssertions = new SoftAssertions();
        softAssertions.assertThat(firstRA).hasFieldOrPropertyWithValue("id", "D7_RA_99991 + D4_RA_99991");
        softAssertions.assertThat(firstRA).hasFieldOrPropertyWithValue("name", "PRA_TEST_1A + PRA_TEST_1");
        softAssertions.assertThat(firstRA).hasFieldOrPropertyWithValue("operator", "D7 + D4");
        softAssertions.assertThat(firstRA).hasFieldOrPropertyWithValue("groupId", Optional.of("Esgaroth + Numenor"));
        softAssertions.assertThat(firstRA).hasFieldOrPropertyWithValue("initialSetpoint", 1000.0);
        softAssertions.assertThat(firstRA.getUsageRules()).hasSize(1);
        softAssertions.assertThat(firstRA.getUsageRules().stream().findFirst().get().getInstant().isPreventive()).isTrue();
        softAssertions.assertThat(firstRA.getRanges()).hasSize(1);
        softAssertions.assertThat(firstRA.getRanges().getFirst()).hasFieldOrPropertyWithValue("min", -1000.0);
        softAssertions.assertThat(firstRA.getRanges().getFirst()).hasFieldOrPropertyWithValue("max", 1000.0);
        softAssertions.assertThat(firstRA.getInjectionDistributionKeys()).hasSize(2);
        softAssertions.assertThat(
                firstRA.getInjectionDistributionKeys().entrySet().stream()
                    .collect(
                        Collectors.toMap(entry -> entry.getKey().getId(),
                            Map.Entry::getValue))
            )
            .containsExactlyInAnyOrderEntriesOf(Map.of("D4AAA11B_generator", 1.0, "D7AAA11A_generator", -1.0));
        softAssertions.assertAll();

        softAssertions = new SoftAssertions();
        softAssertions.assertThat(secondRA).hasFieldOrPropertyWithValue("id", "D7_RA_99992 + D4_RA_99992");
        softAssertions.assertThat(secondRA).hasFieldOrPropertyWithValue("name", "PRA_TEST_2A + PRA_TEST_2");
        softAssertions.assertThat(secondRA).hasFieldOrPropertyWithValue("operator", "D7 + D4");
        softAssertions.assertThat(secondRA).hasFieldOrPropertyWithValue("groupId", Optional.of("Esgaroth + Numenor"));
        softAssertions.assertThat(secondRA).hasFieldOrPropertyWithValue("initialSetpoint", 1000.0);
        softAssertions.assertThat(secondRA.getUsageRules()).hasSize(1);
        softAssertions.assertThat(secondRA.getUsageRules().stream().findFirst().get().getInstant().isPreventive()).isTrue();
        softAssertions.assertThat(secondRA.getRanges()).hasSize(1);
        softAssertions.assertThat(secondRA.getRanges().getFirst()).hasFieldOrPropertyWithValue("min", -1000.0);
        softAssertions.assertThat(secondRA.getRanges().getFirst()).hasFieldOrPropertyWithValue("max", 1000.0);
        softAssertions.assertThat(secondRA.getInjectionDistributionKeys()).hasSize(2);
        softAssertions.assertThat(
                secondRA.getInjectionDistributionKeys().entrySet().stream()
                    .collect(
                        Collectors.toMap(entry -> entry.getKey().getId(),
                            Map.Entry::getValue))
            )
            .containsExactlyInAnyOrderEntriesOf(Map.of("D4AAA21B_generator", 1.0, "D7AAA21A_generator", -1.0));
        softAssertions.assertAll();
    }
}
