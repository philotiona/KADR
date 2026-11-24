# Чек-лист готовности к релизу KADR 1.0.0

Дата аудита: 24.11.2026. Отметка: ✅ выполнено, ⚠️ выполнено с ограничением, ❌ не выполнено.

## 1. Функциональность
| Проверка | Статус | Доказательство |
|---|---|---|
| Вход/выход, сессия 30 дней | ✅ | Ручная проверка release-сборки, `KadrEspressoTest` |
| Афиша, поиск, фильтры, даты | ✅ | Ручная проверка release после R8 |
| Бронирование мест, промокоды, атомарная транзакция | ✅ | `BookingDatabaseTest`, `CinemaPriceTest` |
| Электронный билет, QR, PNG, ICS | ✅ | `KadrEspressoTest`, ручная проверка |
| Администрирование (фильмы, залы, сеансы, касса) | ✅ | Ручная проверка |
| Работа без сети | ✅ | Данные в локальной SQLite |

## 2. Известные ошибки (Known issues)
| № | Описание | Влияние | План |
|---|---|---|---|
| KI-1 | «О приложении» показывает жёстко заданную «Версия 1.0» и «практическая работа №5» | Низкое | Исправить в 1.1.0 (брать `BuildConfig.VERSION_NAME`) |
| KI-2 | Lint из AGP 8.7.3 не запускается на JDK 25 (`IllegalArgumentException: 25.0.3`) | Только сборка на ПК | Локально `-x lintVital*`, полный lint в CI на JDK 17 |

## 3. Устройства и версии ОС
| Конфигурация | Android / API | Экран | Результат |
|---|---|---|---|
| Medium Phone (эмулятор) | 17 / API 37 | 1080×2400, 420 dpi | ✅ debug, staging, release |
| GitHub Actions emulator (`lab7.yml`) | 15 / API 35 | 1080×1920 | ✅ инструментальные тесты |
| minSdk | 7.0 / API 24 | — | ⚠️ проверено компиляцией и lint (NewApi), реального устройства нет |

## 4. Разрешения (permissions)
| Сборка | Разрешения | Обоснование |
|---|---|---|
| release 1.0.0 | **нет** | Все данные локальные, файлы сохраняются через системный выбор файла (SAF) |
| debug | INTERNET, READ/WRITE_EXTERNAL_STORAGE, POST_NOTIFICATIONS | Только отладка: лабораторный HTTP, LeakCanary. В release не попадают |

Проверка: `aapt dump permissions` (см. отчёт).

## 5. Сторонние библиотеки и лицензии
| Библиотека | Где | Лицензия |
|---|---|---|
| — (нет runtime-зависимостей) | release | — |
| OkHttp 4.12.0, Room 2.6.1, LeakCanary 2.14, Espresso idling 3.7.0 | debug | Apache 2.0 |
| JUnit 4.13.2 | тесты | EPL 1.0 |
| Mockito 5.18, Robolectric 4.15.1, MockWebServer | тесты | MIT / MIT / Apache 2.0 |
| org.json 20240303 | тесты | Public Domain |
| Checkstyle 10.20.1 | инструмент сборки | LGPL 2.1 (не входит в APK) |

Все лицензии совместимы с распространением приложения; в release не встраивается ни одна сторонняя библиотека.

## 6. Секреты
| Проверка | Статус |
|---|---|
| Нет API-ключей/паролей в исходниках (`git grep` по шаблонам) | ✅ |
| `local.properties`, `keystore.properties`, `*.jks`, `.env`, `google-services.json` в `.gitignore` | ✅ |
| Пароли подписи в CI только через GitHub Secrets | ✅ |
| Checkstyle-правило «Possible hard-coded secret» в CI | ✅ |

## 7. Отладочные логи
| Проверка | Статус |
|---|---|
| `Log.d/i` в main-коде обёрнуты `if (BuildConfig.DEBUG)` | ✅ |
| R8 `-assumenosideeffects` удаляет `Log.v/d/i` в release | ✅ |
| Строки отладочных логов отсутствуют в `classes.dex` release | ✅ |
| Кнопка «Лабораторная работа №7» только в debug | ✅ |
| `android:allowBackup="false"`, release не `debuggable` | ✅ |
