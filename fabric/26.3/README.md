# Cinematic Interactions — Fabric 26.3

Отдельный порт текущей NeoForge-сборки **0.18.1** на Minecraft **26.3**. Старые порты 1.21–1.21.11 собираются
соседним проектом и не используют эти исходники.

Перенесены редакторы всех станций, режим разработчика, три локальных профиля каждой станции, выбор профилей
в настройках и индикаторы приготовления еды у костра. Анимация кровати не входит в мод.

**Статус:** порт 0.18.0 проверен в игре, с Iris/Complementary и на отдельном сервере;
исправление Punchy в 0.18.1 проверено в обычном и шейдерном клиентах, а также без Punchy.
Результаты и границы проверки — в [TESTING.md](TESTING.md).

## Установка

Готовый файл: [Cinematic Interaction-Fabric-26.3-0.18.1.jar](../../release/fabric/26.3/Cinematic%20Interaction-Fabric-26.3-0.18.1.jar).
Нужны Minecraft **26.3**, Java **25**, Fabric Loader **0.19.5+** и Fabric API **0.163.0+26.3**.
Один и тот же JAR устанавливается в `mods` клиента и сервера.
Для доступа к настройкам через список модов установите Mod Menu; Cloth Config добавляет экран с вкладками,
а при его отсутствии используется встроенный экран.

Профили из NeoForge можно скопировать в `config`: `cinematic-table-layouts.json`,
`cinematic-furnace-layouts.json`, `cinematic-surface-layouts.json`, `cinematic-spatial-layouts.json`.
Общие настройки хранятся в `cinematic-client.json`, автоматического импорта TOML нет.

## Сборка

Нужен **JDK 25**. Используйте wrapper из этой папки: Gradle 9.6.0, Fabric Loom 1.17.21.

```powershell
$env:JAVA_HOME = 'путь к JDK 25'
.\gradlew.bat build
```

Игровой JAR: `build/libs/Cinematic Interaction-Fabric-26.3-0.18.1.jar`.
Зависимости: Fabric Loader **0.19.5+**, Fabric API **0.163.0+26.3**. Один и тот же JAR нужен на клиенте и сервере.
Cloth Config и Mod Menu необязательны; без Cloth Config доступен встроенный экран настроек.

## Проверка

```powershell
.\gradlew.bat runShowcase -PwithSettings
.\gradlew.bat runShowcase -PwithSettings -Pfrom=a -Ponly=true
.\gradlew.bat runShowcase -PwithSettings -PwithShaders -Pfrom=a -PeditorBatch=true
.\gradlew.bat runServer
```

Showcase создаёт отдельный мир в `run/showcase` или `run/showcase-iris`. Снимки сохраняются в `screenshots`,
проверки и замеры плавности — в журнал. Для шейдерного прогона нужен включённый пакет в
`run/showcase-iris/shaderpacks` и `config/iris.properties`. Iris 1.11.7 и Sodium 0.9.3-alpha.1 подключаются только
при `-PwithShaders`; библиотеки Iris извлекаются автоматически для запуска из среды разработки.

Не пересобирайте проект, пока его игровой клиент запущен.

## Особенности переноса

- Игра использует Java 25 и открытые имена классов, поэтому слой mappings не нужен.
- Геометрия сцены записывается во время extraction и передаётся в обычную очередь features Minecraft.
  Прозрачные предметы сохраняют индивидуальную альфу, оттенки материалов и свечение.
- Камера подключена перед расчётом её матриц; время сцены обновляется после игровых тиков и до обновления камеры.
- Интерфейсы используют `GuiGraphicsExtractor`; кнопки SDL преобразуются в прежние логические номера только
  на границе ввода. Серверные номера кнопок и слотов не меняются.
- Страницы книги рисуются в собственные текстуры через отдельную очередь features и render pass.
- Координаты редакторов, предметов, рук, подсказок рецепта и кликов используют общую геометрию из NeoForge.
- JSON-профили имеют те же имена и форматы, что и NeoForge. Общие настройки Fabric хранятся в
  `config/cinematic-client.json`; TOML автоматически не импортируется.

Текстуры, переводы и модель молотка берутся из `../../src/main/resources`. Определение модели предмета молотка
использует совместимый ресурс из `../src/1.21.5/resources`. Исходники Minecraft и Fabric API для проверки API
распакованы локально в `refs/` и не входят в JAR.
