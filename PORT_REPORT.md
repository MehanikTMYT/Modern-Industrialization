# PORT_REPORT — MI Structure Viewer → 26.1.2

Дата: 2026-11-16 · Ветка: `port/26.1.2` · Дерево: `/root/home/forks/worktrees/mi-structure-viewer-26.1.2` (NL)

## Контекст

- Оригинал (KuDikan, miviewer-1.1.5, JEI, ARR) — исходники не найдены; реализация полностью
  собственная (см. README), лицензия MIT, код/ресурсы оригинала не использовались.
- Прежний каркас этапа-1 жил на выведенном из строя хосте FI и до NL не доехал:
  в `/root/home/forks/worktrees/` NL и в `mi-stack/Modern-Industrialization` его нет
  (проверено `git worktree list` + поиск по дереву). Этап-2 выполнен с нуля поверх
  свежего апстрима MI `port/26.1.2` (`27e5ae771`, fetch 2026-11-16).

## Реализация (этап 2)

1. **Извлечение форм** — прямое, без рефлексии: `ReiMachineRecipes.multiblockShapes`
   (public static List, record `MultiblockShape(Identifier, ShapeTemplate, String)`).
   Конвертация в plain-MC снапшот (`StructureIndex`), ошибки по форме изолируются.
2. **EMI-категория + рецепты**: `StructureViewerEmiPlugin` (@EmiEntrypoint) →
   категория `mi_structure_viewer:multiblock`, по рецепту на форму; дублирования с
   собственной EMI-интеграцией MI (ViewerPluginEmi — категории машинных рецептов) нет.
3. **Инфо-карточка**: имя машины (фоллбек-перевод), габариты X×Y×Z, casing, вариант
   (EBF — катушка), сводный список блоков с количеством, перечень типов люков.
4. **Послойный просмотр**: сетка 18px по X/Z, кнопки ←/→ («Layer i / n», yOffset),
   ячейки люков цветом + тултипы; 3D отложен (см. «Осталось»).
5. **Пустые/битые формы**: пропускаются (`isEmpty` + try/catch на форму и на позицию);
   недоступный item → серая ячейка + тултип с id блока; отсутствующий перевод →
   `translatableWithFallback` (претифай id). Повторная регистрация: плагин пересобирает
   снапшот на каждом EMI-reindex (в т.ч. после `/reload`).
6. **Языки**: `en_us.json`, `ru_ru.json` (14 ключей + категория).

## Сборка

- `./gradlew compileJava` — **BUILD SUCCESSFUL**, 0 ошибок (Java 25, ModDevGradle 2.0.143, NeoForge 26.1.2.126).
- `./gradlew build` — **BUILD SUCCESSFUL**; артефакт `build/libs/mi_structure_viewer-26.1.2-1.0.0-fleet.1.jar`
  (+sources), mods.toml в формате neoforge26 (loaderVersion `[2,)`), deps: neoforge / modern_industrialization / emi.

## Ожидаемое количество форм/рецептов

- В исходниках MI — **28 точек** `registerMultiblockShape(...)`; одна из них (EBF,
  `ElectricBlastFurnaceBlockEntity`) в цикле по тирам катушек → базово 2 тира
  (cupronickel, kanthal; +KubeJS-тиры при их наличии).
- Расчёт: **≈29–30 форм → столько же EMI-рецептов** (по одному на форму, вариант EBF
  = отдельная форма). Точное рантайм-число фиксирует QA-агент (см. ниже).

## Ограничения

- Runtime-подтверждение (клиент EMI, `/reload`, реальные счётчики форм и рецептов,
  скриншоты) — за отдельным QA-агентом; здесь только compile + jar, серверного smoke
  этого этапа НЕ заявляю.
- Слой мутабелен в recipe-инстансе (паттерн EMI PageManager): при двух открытых
  дисплеях одной формы слои крутятся синхронно — осознанный компромисс этапа 1.
- Категория-иконка — item первой зарегистрированной машины; для пустого реестра
  категория вообще не регистрируется.
- `alternative` EBF выводится как перевод имени блока катушки; если блок не найден —
  сырой id серым.
- EMI подключён `compileOnly(api)` + `localRuntimeOnly`; для run-клиента QA jar EMI
  кладётся в run/mods (как в sv3-smoke прошлой сессии).

## Осталось

1. Клиентский QA: скриншоты категорий/рецептов, `/reload`, RU-локаль, навигация слоями.
2. 3D-просмотр (второй этап оригинала) — по желанию заказчика.
3. Опционально: кликабельная подсветка блоков (Set Block Preview) из оригинала.
4. Занести итог в roadmap/STATUS — отдельным агентом/командой fleet-sync (STATUS.md руками не трогаю).
