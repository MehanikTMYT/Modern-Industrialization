/*
 * MIT License
 *
 * Copyright (c) 2020 Azercoco & Technici4n
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package aztech.modern_industrialization;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import net.minecraft.locale.DeprecatedTranslationsInfo;
import net.minecraft.locale.Language;

/**
 * Regression test for the 26.1.2 attribute tooltip defect (QA 2026-11-22):
 * {@code attribute.name.generic.attack_damage} was removed from the active language
 * data by {@link DeprecatedTranslationsInfo} (deprecated.json "removed" list), so MI
 * attributes using the legacy "attribute.name.generic.*" keys rendered raw technical
 * keys ("attribute.name.generic.attack_damage: ∞") in item tooltips.
 *
 * Plain-Java entry point, run with {@code ./gradlew translationKeyRegressionTest}
 * (no JUnit required, safe to run headless without a bootstrapped registry).
 */
public final class TranslationKeyRegressionTest {
    private static final Gson GSON = new Gson();

    private TranslationKeyRegressionTest() {}

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("REGRESSION: " + message);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> loadMiLang() {
        try (Reader reader = new InputStreamReader(
                Objects.requireNonNull(
                        TranslationKeyRegressionTest.class.getResourceAsStream("/assets/modern_industrialization/lang/en_us.json"),
                        "MI en_us.json not on classpath"),
                StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, Map.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws InstantiationException {
        // 1. The vanilla (en_us) language must not contain the removed legacy keys...
        Map<String, String> vanilla = Language.getInstance().getLanguageData();
        check(!vanilla.containsKey("attribute.name.generic.attack_damage"),
                "deprecated key attribute.name.generic.attack_damage is active in vanilla language data");
        check(!vanilla.containsKey("attribute.name.generic.armor"),
                "deprecated key attribute.name.generic.armor is active in vanilla language data");

        // ...and must contain the current 26.1.2 keys we rely on.
        check(vanilla.containsKey("attribute.name.attack_damage"), "missing active key attribute.name.attack_damage");
        check(vanilla.containsKey("attribute.name.armor"), "missing active key attribute.name.armor");

        // 2. The MI attributes must not carry removed legacy keys. Attribute instances
        //    cannot be created headless (Attribute.<clinit> requires a bootstrapped FML
        //    registry environment), so the compiled class bytecodes are verified for the
        //    expected/denied string constants instead.
        checkClassLiteral("InfiniteDamageAttribute", "attribute.name.attack_damage", "attribute.name.generic.attack_damage");
        checkClassLiteral("QuantumArmorAttribute", "attribute.name.armor", "attribute.name.generic.armor");

        // 3. Vanilla dimension names used by DimensionProcessCondition tooltips must be
        //    provided by MI (the vanilla "dimension.<ns>.<path>" keys no longer exist).
        Map<String, String> mi = loadMiLang();
        for (String dim : new String[] {
                "dimension.minecraft.overworld", "dimension.minecraft.the_nether", "dimension.minecraft.the_end",
        }) {
            check(mi.containsKey(dim), "MI lang is missing vanilla dimension name: " + dim);
        }

        // 4. The deprecated.json data actually strips the legacy keys (guards against
        //    future vanilla changes silently re-activating them).
        DeprecatedTranslationsInfo deprecated = DeprecatedTranslationsInfo.loadFromDefaultResource();
        check(deprecated.removed().contains("attribute.name.generic.attack_damage"),
                "expected attribute.name.generic.attack_damage in deprecated.json removed list");

        System.out.println("TranslationKeyRegressionTest: OK (attribute + dimension tooltip ids "
                + "verified against active vanilla en_us data and MI en_us.json)");
    }

    /**
     * Verifies the compiled {@code attributes/<cls>.class} contains the expected tooltip id
     * constant and does not contain the removed legacy key.
     */
    private static void checkClassLiteral(String simpleName, String expected, String denied) {
        String path = "/aztech/modern_industrialization/attributes/" + simpleName + ".class";
        byte[] bytes;
        try (var in = Objects.requireNonNull(
                TranslationKeyRegressionTest.class.getResourceAsStream(path), "class not on classpath: " + path)) {
            bytes = in.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String constants = new String(bytes, StandardCharsets.ISO_8859_1);
        check(constants.contains(expected),
                simpleName + " does not use expected tooltip id constant: " + expected);
        check(!constants.contains(denied),
                simpleName + " still contains removed legacy key constant: " + denied);
    }
}
