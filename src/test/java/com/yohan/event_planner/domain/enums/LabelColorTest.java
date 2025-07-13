package com.yohan.event_planner.domain.enums;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LabelColorTest {

    @Nested
    class EnumValuesTests {

        @Test
        void shouldContainExpectedColors() {
            // Assert all expected color values exist
            LabelColor[] expectedColors = {
                LabelColor.RED, LabelColor.ORANGE, LabelColor.YELLOW,
                LabelColor.GREEN, LabelColor.TEAL, LabelColor.BLUE,
                LabelColor.PURPLE, LabelColor.PINK, LabelColor.GRAY
            };

            assertThat(LabelColor.values()).containsExactly(expectedColors);
        }

        @Test
        void shouldHaveCorrectColorCount() {
            assertThat(LabelColor.values()).hasSize(9);
        }

        @Test
        void shouldProvideValueOfMethod() {
            assertThat(LabelColor.valueOf("RED")).isEqualTo(LabelColor.RED);
            assertThat(LabelColor.valueOf("BLUE")).isEqualTo(LabelColor.BLUE);
            assertThat(LabelColor.valueOf("GRAY")).isEqualTo(LabelColor.GRAY);
        }
    }

    @Nested
    class HexColorTests {

        @Test
        void shouldProvideValidBaseHexColors() {
            // Test a few key colors to ensure hex values are valid
            assertThat(LabelColor.RED.getBaseHex()).isEqualTo("#FF4D4F");
            assertThat(LabelColor.BLUE.getBaseHex()).isEqualTo("#1890FF");
            assertThat(LabelColor.GREEN.getBaseHex()).isEqualTo("#52C41A");
            assertThat(LabelColor.GRAY.getBaseHex()).isEqualTo("#8C8C8C");
        }

        @Test
        void shouldProvideValidPastelHexColors() {
            assertThat(LabelColor.RED.getPastelHex()).isEqualTo("#FFD6D7");
            assertThat(LabelColor.BLUE.getPastelHex()).isEqualTo("#A3D3FF");
            assertThat(LabelColor.GREEN.getPastelHex()).isEqualTo("#C7EFCF");
            assertThat(LabelColor.GRAY.getPastelHex()).isEqualTo("#D9D9D9");
        }

        @Test
        void shouldProvideValidMetallicHexColors() {
            assertThat(LabelColor.RED.getMetallicHex()).isEqualTo("#D72631");
            assertThat(LabelColor.BLUE.getMetallicHex()).isEqualTo("#0050B3");
            assertThat(LabelColor.GREEN.getMetallicHex()).isEqualTo("#237804");
            assertThat(LabelColor.GRAY.getMetallicHex()).isEqualTo("#595959");
        }

        @Test
        void shouldHaveValidHexFormat() {
            // Test all colors have valid hex format (#RRGGBB)
            for (LabelColor color : LabelColor.values()) {
                assertThat(color.getBaseHex())
                    .matches("#[0-9A-F]{6}")
                    .withFailMessage("Base hex for %s should be valid", color);
                
                assertThat(color.getPastelHex())
                    .matches("#[0-9A-F]{6}")
                    .withFailMessage("Pastel hex for %s should be valid", color);
                
                assertThat(color.getMetallicHex())
                    .matches("#[0-9A-F]{6}")
                    .withFailMessage("Metallic hex for %s should be valid", color);
            }
        }

        @Test
        void shouldHaveUniqueBaseColors() {
            // Ensure no two colors have the same base hex value
            LabelColor[] colors = LabelColor.values();
            for (int i = 0; i < colors.length; i++) {
                for (int j = i + 1; j < colors.length; j++) {
                    assertThat(colors[i].getBaseHex())
                        .isNotEqualTo(colors[j].getBaseHex())
                        .withFailMessage("Colors %s and %s should have different base hex values",
                            colors[i], colors[j]);
                }
            }
        }

        @Test
        void shouldHaveUniquePastelColors() {
            // Ensure no two colors have the same pastel hex value
            LabelColor[] colors = LabelColor.values();
            for (int i = 0; i < colors.length; i++) {
                for (int j = i + 1; j < colors.length; j++) {
                    assertThat(colors[i].getPastelHex())
                        .isNotEqualTo(colors[j].getPastelHex())
                        .withFailMessage("Colors %s and %s should have different pastel hex values",
                            colors[i], colors[j]);
                }
            }
        }

        @Test
        void shouldHaveUniqueMetallicColors() {
            // Ensure no two colors have the same metallic hex value
            LabelColor[] colors = LabelColor.values();
            for (int i = 0; i < colors.length; i++) {
                for (int j = i + 1; j < colors.length; j++) {
                    assertThat(colors[i].getMetallicHex())
                        .isNotEqualTo(colors[j].getMetallicHex())
                        .withFailMessage("Colors %s and %s should have different metallic hex values",
                            colors[i], colors[j]);
                }
            }
        }
    }

    @Nested
    class VariantDifferenceTests {

        @Test
        void shouldHaveDifferentVariantsPerColor() {
            // Each color should have three different hex values
            for (LabelColor color : LabelColor.values()) {
                String base = color.getBaseHex();
                String pastel = color.getPastelHex();
                String metallic = color.getMetallicHex();

                assertThat(base).isNotEqualTo(pastel)
                    .withFailMessage("Base and pastel should be different for %s", color);
                
                assertThat(base).isNotEqualTo(metallic)
                    .withFailMessage("Base and metallic should be different for %s", color);
                
                assertThat(pastel).isNotEqualTo(metallic)
                    .withFailMessage("Pastel and metallic should be different for %s", color);
            }
        }
    }

    @Nested
    class UsabilityTests {

        @Test
        void shouldProvideReadableNames() {
            // Ensure enum names are appropriate for user display
            for (LabelColor color : LabelColor.values()) {
                assertThat(color.name())
                    .matches("[A-Z]+")
                    .withFailMessage("Color name should be uppercase: %s", color);
                
                assertThat(color.name().length())
                    .isGreaterThan(2)
                    .withFailMessage("Color name should be meaningful: %s", color);
            }
        }

        @Test
        void shouldBeSerializableAsString() {
            // Test that enum can be converted to/from string (for JSON/JPA)
            for (LabelColor color : LabelColor.values()) {
                String name = color.name();
                LabelColor restored = LabelColor.valueOf(name);
                assertThat(restored).isEqualTo(color);
            }
        }

        @Test
        void shouldProvideConsistentToString() {
            // Ensure toString behaves consistently
            for (LabelColor color : LabelColor.values()) {
                assertThat(color.toString()).isEqualTo(color.name());
            }
        }
    }

    @Nested
    class AccessibilityTests {

        @Test
        void shouldNotUseExtremelyLightColors() {
            // Ensure base colors are not too light for accessibility
            for (LabelColor color : LabelColor.values()) {
                String hex = color.getBaseHex().substring(1); // Remove #
                
                // Parse RGB values
                int r = Integer.parseInt(hex.substring(0, 2), 16);
                int g = Integer.parseInt(hex.substring(2, 4), 16);
                int b = Integer.parseInt(hex.substring(4, 6), 16);
                
                // Simple brightness check (perceived brightness)
                double brightness = 0.299 * r + 0.587 * g + 0.114 * b;
                
                // Base colors should not be too bright (too close to white)
                assertThat(brightness)
                    .isLessThan(240)
                    .withFailMessage("Base color %s might be too bright for accessibility: %s (brightness: %.1f)",
                        color, color.getBaseHex(), brightness);
            }
        }

        @Test
        void shouldNotUseExtremelyDarkColors() {
            // Ensure metallic colors are not too dark
            for (LabelColor color : LabelColor.values()) {
                String hex = color.getMetallicHex().substring(1); // Remove #
                
                // Parse RGB values
                int r = Integer.parseInt(hex.substring(0, 2), 16);
                int g = Integer.parseInt(hex.substring(2, 4), 16);
                int b = Integer.parseInt(hex.substring(4, 6), 16);
                
                // Simple brightness check
                double brightness = 0.299 * r + 0.587 * g + 0.114 * b;
                
                // Metallic colors should not be too dark (too close to black)
                assertThat(brightness)
                    .isGreaterThan(15)
                    .withFailMessage("Metallic color %s might be too dark: %s (brightness: %.1f)",
                        color, color.getMetallicHex(), brightness);
            }
        }
    }
}