# Лабораторная работа №7 · KADR

Запустите конфигурацию **KADR** на **Medium Phone** в Android Studio. Кнопка **Лабораторная работа №7** есть внизу экрана входа и в разделе **Кабинет**. В отладочной сборке добавлены отдельные демонстрации сбоев, а обычный экран бронирования сохраняет корректную формулу и данные.

## 1–4 · Отладка

1. Откройте **Logcat** и вставьте `package:com.example.kaspikids tag:KadrLab level:DEBUG`. Нажмите **Показать Logcat D / I / W / E**. На втором снимке смените уровень на `level:WARN`. При реальном бронировании логируются число выбранных мест, отклонение промокода и сохранение брони.
2. Поставьте breakpoint на строке `int total =` в `LabActivity.priceProbe()`. Нажмите **Расчёт с ошибкой**. В Watches добавьте `price`, `count`, `discount`. Проверьте обе формулы в Evaluate Expression. Для условной точки задайте `injectFault && discount == 15`. Для logpoint снимите флаг **Suspend** и включите логирование. Шагните в вызов `CinemaRules.total` на исправленном пути, затем выполните Step Over и Step Out. Снимите результаты 5 750 ₸ и 4 250 ₸.
3. Добавьте Java exception breakpoints для трёх классов, названных на экране. Нажимайте по одной кнопке сбоя за запуск. Сохраняйте исключение и первый кадр `LabActivity` из стека. Для проверки исправлений используйте **Проверить null и пустой список** и **Обновить объявление в фоне**.
4. Для Layout Inspector нажмите **Вёрстка · показать наложение**, выберите метку в иерархии, затем нажмите **Вёрстка · убрать наложение**. Для Database Inspector откройте `kadr_cinema.db` после бронирования и сравните `bookings.id` с `booking_seats.booking_id`. Для Network Inspector оставьте локальный сервер включённым и нажмите **Обновить объявление в фоне**. Сохраните статус HTTP, длительность и ответ.

Локальный сервер для HTTP запускается в отдельном терминале PowerShell из корня проекта. Команда `adb reverse` подключает порт Android к порту ПК. Приложение запрашивает демонстрационный JSON без учётных данных.

```powershell
.\tools\start-lab7-network.ps1 -Serial emulator-5554
```

## 5–12 · Тесты и покрытие

Выполняйте из корня проекта с JDK 17. Сначала запустите Medium Phone. Параметр `-PorchestrateLab` устанавливает отдельный тестовый пакет `com.example.kaspikids.labtest` и очищает его данные между тестами.

```powershell
.\gradlew.bat :app:lintDebug :app:createDebugUnitTestCoverageReport -PlabCoverage
.\gradlew.bat :app:connectedDebugAndroidTest -PorchestrateLab
.\gradlew.bat :app:createDebugAndroidTestCoverageReport -PlabCoverage -PisolateLab
```

Для отчёта JaCoCo используйте отдельный прогон `-PisolateLab`. Он сохраняет тестовый идентификатор приложения и даёт раннеру записать общий файл покрытия до очистки данных.

Robolectric загружает отдельный образ Android для JVM. Запустите его отдельно, чтобы обычный прогон JUnit не ждал эту загрузку.

```powershell
.\gradlew.bat :app:testDebugUnitTest -ProbolectricLab --tests '*PreferencesRobolectricTest'
```

| № | Доказательство |
|---|---|
| 5 | `CinemaPriceTest` и `CinemaValidationTest` в HTML-отчёте `app/build/reports/tests/testDebugUnitTest/index.html`. |
| 6 | `AnnouncementServiceTest` с подстановкой ответа API и проверками Mockito `verify`. |
| 7 | Длительность `PreferencesRobolectricTest` и `PreferencesDeviceTest` в результатах. Время запуска Gradle и эмулятора записывайте отдельно. |
| 8 | Тесты SQLite в памяти, Room DAO, миграции Room 1→2 и ответы MockWebServer в отчётах JVM и Android. |
| 9–10 | `KadrEspressoTest`, `LoginPage`, `TicketPage` и повторный запуск с `-PorchestrateLab`. Ресурс ожидания учитывает открытие БД и фоновые операции до обновления интерфейса. |
| 11 | Таблица ручных тестов и журнал Monkey на отдельном тестовом пакете. |
| 12 | `CinemaRules` в HTML-отчёте JaCoCo из `app/build/reports/coverage/`, отдельно для JVM и Android. В отчёте укажите, какая метрика использована. |

Для матрицы используйте Medium Phone и устройство с другой версией Android и размером экрана. Для каждого сценария запишите шаг, ожидаемый и фактический результаты.

| № | Шаг | Ожидаемый результат |
|---|---|---|
| 1 | Войти как зритель | Открывается афиша. |
| 2 | Ввести неверный пароль | Появляется ошибка входа. |
| 3 | Выбрать два места по 2 500 ₸ и ввести STUDENT | Итог 4 250 ₸. |
| 4 | Создать и отменить бронь | Билет появляется, после отмены места освобождаются. |
| 5 | Повернуть экран с выбранными местами | Экран доступен, выбор сохраняется. |
| 6 | Свернуть и вернуться через список приложений | Приложение продолжает работу. |
| 7 | Завершить процесс и открыть KADR | Сохранённые сеанс входа и билет доступны. |
| 8 | Отключить сеть и создать бронь | Локальное бронирование доступно. |
| 9 | Включить системную тёмную тему | Текст и элементы управления читаемы. |
| 10 | Изменить язык системы | Приложение запускается, элементы доступны. |

Для Monkey используйте отдельную сборку без учебных кнопок сбоя.

```powershell
.\gradlew.bat :app:installBenchmark
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell monkey -p com.example.kaspikids.benchmark -s 7 --throttle 150 300
```

## 13–15 · Профилирование, облако и CI

- Включите **StrictMode** в меню лабораторной работы и фильтруйте `tag:StrictMode` в Logcat. Запишите системную трассу при бронировании. В новой версии запись брони выполняется на рабочем потоке и отмечается секцией `KADR book seats`. Сравнивайте с базовой сборкой на одном устройстве и для одного сценария.
- Для утечки нажмите **Удержать Activity и закрыть экран**, затем сверните KADR. Откройте уведомление LeakCanary и сохраните цепочку удержания. Откройте лабораторный экран, нажмите **Освободить удержанную Activity** и повторите проверку без удержания. Укажите, что утечка внесена для демонстрации. Снимите системную трассу, heap dump, сетевой запрос и доступные данные Power Profiler на поддерживаемом устройстве.
- Macrobenchmark запускается командой `.\gradlew.bat :benchmark:connectedBenchmarkAndroidTest` на физическом Android. Результаты и трассы сохраняются в `benchmark/build/outputs/connected_android_test_additional_output/`. Тестовый пакет `com.example.kaspikids.benchmark` использует отдельную БД.
- Для Firebase Test Lab соберите пару APK командой `.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest -PisolateLab`. Загрузите файлы из `app/build/outputs/apk/debug/` и `app/build/outputs/apk/androidTest/debug/` как **Instrumentation test**. Выберите несколько устройств и сохраните результат матрицы, видео и логи.
- Конфигурация GitHub Actions находится в `.github/workflows/lab7.yml`. После добавления проекта в репозиторий откройте pull request. В отчёт приложите ссылку на запуск, страницу артефактов и таблицу дефектов с приоритетом, статусом и фактическими измерениями.
