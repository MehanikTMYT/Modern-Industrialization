# MI_TRANSLATION_KEYS_QA — Аудит технических ключей локализации (26.1.2)

Дата: 2026-11-22 · Мод: Modern Industrialization (mi-stack/Modern-Industrialization) ·
MC/NeoForge 26.1.2 / 26.1.2.128 · Worktree: `.worktrees/tlk-26-fix`, ветка `fix/translation-keys-26`.

## 1. Первопричина дефекта Quantum Sword

**Симптом (Windows QA):** в тултипе квантового меча отображается
`attribute.name.generic.attack_damage: ∞` вместо «Урон при атаке».

**Причина — НЕ MI-тултип-код, а удалённые ванильные ключи языка в 26.1.2:**

- В 26.1.2 Mojang переименовал ключи имён атрибутов: `attribute.name.generic.attack_damage` →
  `attribute.name.attack_damage`, `attribute.name.generic.armor` → `attribute.name.armor` и т.д.
  Старые ключи вынесены в `assets/minecraft/lang/deprecated.json` в список `removed`.
- `net.minecraft.locale.DeprecatedTranslationsInfo.applyToMap(...)` **удаляет** ключи из
  `removed`-списка из загруженных переводов — и для дефолтного en_us
  (`Language.loadDefault`, `Language.java:40`), и для каждого клиентского языка
  (`ClientLanguage.java:57`, включая ru_ru).
- MI в `InfiniteDamageAttribute.getTooltipDescriptionId()` возвращал именно удалённый
  `attribute.name.generic.attack_damage`; NeoForge 26.1.2 строит строку атрибута как
  `Component.translatable("attribute.modifier.equals.0", "∞", Component.translatable(ЭТОТ_КЛЮЧ))`
  (`AttributeUtil.toBaseComponent` → базовый модификатор с id `mi:base_infinite_damage` = attr.getBaseId()).
  Ключа нет в активных переводах → в тултип уходит сырая техническая строка.
- Аналогично `QuantumArmorAttribute` возвращал удалённый `attribute.name.generic.armor`.

**Отклонённые гипотезы (проверены по исходникам 26.1.2):**
- `Component.literal` с ключами в MI — не найдено (grep по всему src/main).
- `getDescriptionId()` атрибутов MI — возвращает существующий `text.modern_industrialization.*`
  (есть и в en_us, и в ru_ru) — не дефект.
- Собственный форматтер `DisplayNamedAttribute.toComponent` — переопределяет
  `IAttributeExtension.toComponent`, который в 26.1.2 **вызывается** из
  `AttributeUtil` (VanillaDataComponentTooltips → addAttributeTooltips) — совместимо с актуальным API.
- `ItemAttributeModifiers` API у QuantumSword — корректен (builder + EquipmentSlotGroup.MAINHAND).
- Языковые ресурсы MI загружаются, ключ `text.modern_industrialization.AttributeInfiniteDamage`
  присутствует в en_us и ru_ru.

**Важно:** бесконечный урон не менялся (2048 ADD_VALUE + INFINITE_DAMAGE ADD_VALUE и
LivingIncomingDamageEvent → Integer.MAX_VALUE сохранены как есть).

## 2. Полный аудит MI

Методика: grep всех `Component.translatable("…")` со строковыми литералами (6 уникальных ключей —
все резолвятся; 2 динамических проверены отдельно), всех `Component.literal("…")` с key-подобными
строками (0), сверка всех ключей `MIText.*` с en_us/ru_ru (полное покрытие), сверка с
`deprecated.json` (`removed`: 34 ключа, включая все `attribute.name.generic.*`/`player.*`/`zombie.*`).

Проверенные предметы/интерфейсы:

| Область | Результат |
|---|---|
| Quantum Sword (атрибуты атаки) | **ДЕФЕКТ** — удалённый ключ `attribute.name.generic.attack_damage` → **исправлен** |
| Quantum Armor (QuantumArmorAttribute) | **ДЕФЕКТ** — удалённый ключ `attribute.name.generic.armor` → **исправлен** |
| Attack speed / armor / toughness предметов MI | OK — ванильные ключи атрибутов не хардкодятся, идут через `getDescriptionId()` ванильных Attributes (актуальные ключи) |
| Трубы (Item/FluidNetworkNode) | OK — `item.<ns>.<path>`, ключи труб есть в en_us/ru_ru |
| DimensionProcessCondition (описание условия рецепта) | **ДЕФЕКТ** — `dimension.<ns>.<path>`: ваниль в 26.1.2 вообще не имеет ключей имён измерений → сырые `dimension.minecraft.overworld` и т.п. → **исправлено**: 3 ванильных ключа добавлены в MI-переводы (en+ru) и в datagen |
| Машины / энергия / мультиблоки / Structure Viewer | OK — тексты через `MIText`, полное покрытие en_us/ru_ru |
| SEM-подсказки, ошибки, fluids/components | OK — через MIText/MITooltips, покрытие подтверждено скриптом (MIText keys: 100% в en_us) |

Иные категории (`item.*`, `block.*`, `tooltip.*`, `text.*`, `gui.*`, `menu.*`, `container.*`) —
просмотрены; переводимых компонентов с сырыми ключами, кроме перечисленных, не найдено.

## 3. Другие моды (выборочный аудит)

| Мод | Проверка | Результат |
|---|---|---|
| Mekanism (mekanism-stack/Mekanism) | `attribute.*`, literal-ключи, словарь | OK — свои ключи `dictionary.mekanism.attribute*` есть в datagen-lang; удалённых ванильных ключей нет |
| Applied Energistics 2 (work/Applied-Energistics-2) | `attribute.*`, literal-ключи | OK — хардкодов нет; атрибуты предметов идут через ванильный путь |
| MineColonies | `attribute.*`, literal-ключи | OK |
| Ender IO (EnderIO-26.1.2-dev) | `attribute.*`, literal-ключи | OK |
| Extended-Industrialization / Industrialization-Overdrive | общие атрибуты MI | Зависят от `MIRegistries.INFINITE_DAMAGE` → чинятся исправлением MI; собственных удалённых ключей нет |

Незакрытых дефектов в портированных модах не обнаружено; отдельные задачи не требуются.

Фоновый grep по всем деревьям `/root/home/forks` на `attribute.name.generic.*`/`player.*`/`zombie.*`
в java-исходниках: попадания только в (а) сам MI — исправлено здесь; (б) снапшоты
`work/fleet-sync-wave-19/21/23/24/MI` — статические копии синка, не рантайм; (в)
`character-wardrobe-donors/CustomNPC-Plus` — дерево-донор (в сборку не входит). Если
CustomNPC-Plus когда-либо портируется — учесть `CustomAttributes.java` (отдельная задача, не блокер).

## 4. Исправления

Классы / файлы (worktree `.worktrees/tlk-26-fix`, ветка `fix/translation-keys-26`):

1. `attributes/InfiniteDamageAttribute.java` — `getTooltipDescriptionId()` → `attribute.name.attack_damage`.
2. `attributes/QuantumArmorAttribute.java` — `getTooltipDescriptionId()` → `attribute.name.armor`.
3. `datagen/translation/TranslationProvider.java` — добавлены
   `dimension.minecraft.overworld/the_nether/the_end` (en).
4. `assets/modern_industrialization/lang/en_us.json` (generated) и `lang/ru_ru.json` —
   добавлены те же 3 ключа (ru: Верхний мир / Незер / Край).
5. `build.gradle` — задача `translationKeyRegressionTest` (включена в `check`).
6. `src/test/java/.../TranslationKeyRegressionTest.java` — регрессионная проверка (см. §5).

Баланс, рецепты и игровые характеристики не менялись. Чужие незакоммиченные изменения
не затронуты (работа в отдельном worktree).

## 5. Регрессионная проверка

`src/test/java/aztech/modern_industrialization/TranslationKeyRegressionTest.java` — plain-Java
(без JUnit), запуск: `./gradlew translationKeyRegressionTest`. Проверяет:
- `attribute.name.generic.attack_damage` / `.armor` отсутствуют в активной en_us-данных ванили
  (deprecated.json реально чистит язык — охрана от молчаливых изменений ванили);
- актуальные ключи `attribute.name.attack_damage` / `attribute.name.armor` присутствуют;
- `getTooltipDescriptionId()`/`getDescriptionId()` обоих MI-атрибутов резолвятся
  (ваниль — по языковым данным ванили, MIText-ключи — по `assets/modern_industrialization/lang/en_us.json`);
- ключи ванильных измерений присутствуют в MI-переводах.

Результаты (en/ru):
- `compileTestJava translationKeyRegressionTest` — **EXIT=0**, вывод:
  `TranslationKeyRegressionTest: OK (attribute + dimension tooltip ids verified against active
  vanilla en_us data and MI en_us.json)`;
- `gradlew build` (MI_VERSION=2.6.0-26.1.2-fleet02, `--no-configuration-cache`) — **BUILD SUCCESSFUL**,
  0 `error:`; регрессионный тест выполняется как часть `check`;
- en_us/ru_ru в собранном JAR содержат `dimension.minecraft.overworld/the_nether/the_end`
  («Верхний мир», «Незер», «Край»); в байткоде `InfiniteDamageAttribute` — константа
  `attribute.name.attack_damage`, удалённый `attribute.name.generic.*` отсутствует.
- Примечание: `checkLicenseClient`/`checkLicenseMain` и `javaImmaculateCheck` падают на
  **pre-existing** файлах HEAD (13 client-классов без заголовков, NeutronHistoryComponent.java) —
  воспроизводится и без правок этого фикса, в задачу не входит; при сборке исключены `-x`.
- Серверный smoke и клиентский QA выполняются владельцем на Windows PC (см. §8).

## 6. Production JAR

- Файл: `build/libs/Modern-Industrialization-2.6.0-26.1.2-fleet02.jar` (7 247 212 байт)
- SHA256: `ac74a70058d81e4077d7b8dd163bfad608a0b09f6811887be084ea70c1b56d6b`
- Sources JAR рядом (`…-fleet02-sources.jar`).

## 7. Git

- Ветка: `fix/translation-keys-26` (worktree `/root/home/forks/mi-stack/Modern-Industrialization/.worktrees/tlk-26-fix`)
- Commit: `git log --oneline -1` в worktree — «Fix removed vanilla attribute/dimension translation keys in 26.1.2 tooltips»
  (хэш не фиксируется в самом отчёте сознательно: отчёт входит в тот же коммит; на момент
  подготовки финальной версии — `e172029c3` + правка этого абзаца)
  (8 файлов: 2 атрибута, TranslationProvider, en_us/ru_ru, build.gradle, регрессионный тест, отчёт)
- Push/merge: НЕ выполнялись (по условиям задачи).

## 8. QA на PC (критерии приёмки)

Проверяется владельцем на Windows QA (инстанс QA-26.1.2):
1. Тултип Quantum Sword: `+2049 Урон при атаке`-строка скрыта (GatherSkippedAttributeTooltips),
   строка бесконечного урона: `∞ Урон при атаке` (ru) / `∞ Attack Damage` (en) — без сырых ключей.
2. Квантовая броня: строка атрибута брони — «Броня»/«Armor», значение `¼ |∞> + ¾ |0>` — без ключей.
3. Описание условия измерения в SEM/рецептах: «Overworld/Верхний мир» вместо `dimension.minecraft.overworld`.
4. Урон меча фактически бесконечный (баланс не изменён).
