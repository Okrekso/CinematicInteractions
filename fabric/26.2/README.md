# Cinematic Interactions — Fabric 26.2

Порт текущей сборки **0.18.1** на Minecraft **26.2**. Основа — Fabric 26.3 со всеми функциями
NeoForge: сцены станций, редакторы, режим разработчика, профили, справочник рецептов,
индикаторы готовки костров, быстрый возврат и переходы между соседними станциями.
Исправление конфликта рук и камеры Punchy включено. Анимации кровати и Cataclysm в моде нет.

## Установка

Нужны Minecraft **26.2**, Java **25**, Fabric Loader **0.19.5+** и Fabric API **0.161.0+26.2**.
Один и тот же JAR и Fabric API нужны на клиенте и сервере. Устанавливайте только одну версию Cinematic Interactions.
Готовая сборка: [Cinematic Interaction-Fabric-26.2-0.18.1.jar](../../release/fabric/26.2/Cinematic%20Interaction-Fabric-26.2-0.18.1.jar).
Контрольная сумма находится рядом в `SHA256SUMS.txt`.

Mod Menu добавляет кнопку настроек; Cloth Config — экран с вкладками. Оба дополнения необязательны.
Без Cloth Config используется встроенный экран. Настройки находятся в `config/cinematic-client.json`.
Локальные профили совместимы с NeoForge и Fabric 26.3:

- `cinematic-table-layouts.json`;
- `cinematic-furnace-layouts.json`;
- `cinematic-surface-layouts.json`;
- `cinematic-spatial-layouts.json`.

## Сборка и проверка

Используйте wrapper из этой папки и JDK 25: Gradle 9.6.0, Loom 1.17.21.

```powershell
$env:JAVA_HOME = 'путь к JDK 25'
.\gradlew.bat build
.\gradlew.bat runShowcase -PwithSettings -PwithPunchy
.\gradlew.bat runShowcase -PwithSettings -PwithShaders -PwithPunchy
.\gradlew.bat runShowcase -Pfrom=z
.\gradlew.bat runServer
```

`-PwithPunchy` подключает только тестовый Punchy 2.8f для 26.2. `-PwithShaders` добавляет
Iris 1.11.4 и Sodium 0.9.2; встроенные библиотеки Iris извлекаются автоматически.
Cloth Config 26.2.155 и Mod Menu 20.0.3 подключаются к игре только с `-PwithSettings`.
`-Pfrom=<акт>` начинает проверку с выбранной станции, `-Ponly=true` завершает её после этого акта,
`-PeditorBatch=true` пропускает повторяющиеся проверки высоты, `-PtraceMixins` экспортирует преобразованные классы.

Каждый запуск создаёт новый тестовый мир. Обычный и шейдерный клиенты используют разные папки
`run/showcase` и `run/showcase-iris`; снимки находятся в `screenshots` этих папок.
Не пересобирайте проект и не запускайте его сервер, пока игровой клиент работает.

## Отличия API от 26.3

- В 26.2 используется Blaze3D и GLFW; координаты слотов и внутренние номера действий сохраняются.
- `ItemInHandRenderer.submitHandsWithItems` обёрнут целиком через MixinExtras `WrapMethod`.
  Отменяются и ранние обработчики Punchy. `CameraAnimationState` не влияет на камеру сцены;
  вне сцены Punchy работает как обычно. Обязательной зависимости от него нет.
- Геометрия записывается в очередь features 26.2. Альфа предметов, tint и отдельный слой блеска сохраняются.
- Свечение наследует shader ванильных глаз и использует аддитивное смешивание; растворяемые предметы
  сохраняют shader исходного материала и его привязку Iris.
- Страницы книги рисуются в собственные текстуры; выходные цели, проекция и туман восстанавливаются после отрисовки.
- Тайминги сцен, пути камеры, руки, серверные операции и форматы пакетов сохранены.

Текстуры, переводы и модель молотка общие с основным проектом. Исходники игры в `refs/`,
тестовые миры и сторонние моды не включаются в JAR. Результаты проверки — в [TESTING.md](TESTING.md).

Документация API сверена с [официальными заметками Fabric для 26.2](https://fabricmc.net/2026/06/15/262.html)
и локально извлечёнными классами игры. Для проверки используется [Punchy 2.8f для 26.2](https://modrinth.com/mod/punchy-fpa/version/O6KuUYUk).
