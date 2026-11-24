# Changelog

Все заметные изменения KADR фиксируются в этом файле.

Формат основан на [Keep a Changelog](https://keepachangelog.com/ru/1.1.0/),
версии следуют [Semantic Versioning](https://semver.org/lang/ru/) (см. `docs/release/VERSIONING.md`).

## [Unreleased]

## [1.2.1] - 2026-11-24
### Fixed
- Критично: сбой `ArithmeticException: divide by zero` в карточке «Ближайший сеанс» через 1–2 с после открытия «Афиши» (#3). Вместёмость зала теперь считается как `rows_count × cols_count`.

## [1.2.0] - 2026-11-24
### Added
- Карточка «Ближайший сеанс» на главном экране: обратный отсчёт и заполненность зала (флаг `nearest_show_card`).
- «Проверить обновления» показывает группу поэтапной раздачи.

## [1.1.0] - 2026-11-24
### Added
- Firebase Crashlytics (сбои, нефатальные ошибки, хлебные крошки) и Google Analytics for Firebase (вход, бронирование, экраны).
- Удалённая конфигурация `config.json`: уведомление о новой версии с поэтапной раздачей, обязательное обновление, флаги функций.
- «Что нового», «Сообщить о проблеме», «Политика конфиденциальности», «Проверить обновления» в личном кабинете.
- Диагностика в debug/staging: тестовый сбой и тестовый ANR.
### Fixed
- «О приложении» показывало жёстко заданную «Версия 1.0» и устаревший текст; теперь версия берётся из сборки (KI-1).
### Security
- Разрешение `INTERNET` используется только для HTTPS-запросов конфигурации и Firebase.

## [1.0.0] - 2026-11-24
### Added
- Варианты сборки `debug`, `staging`, `release` с отдельными applicationId, API и иконками.
- Версия, окружение и адрес API в нижней части экрана входа и личного кабинета.
- Подпись release-сборки upload-ключом; секреты читаются из `keystore.properties` или переменных окружения.
- Конвейеры GitHub Actions: CI (Checkstyle, Android Lint, unit-тесты, APK) и CD по тегу `v*`.
### Changed
- Release-сборка оптимизирована R8: минификация, обфускация, удаление неиспользуемых ресурсов.
- Отладочные `Log.d/i/v` удаляются из release-кода правилом `-assumenosideeffects`.
### Security
- `keystore.properties`, `*.jks`, `.env`, `google-services.json` исключены из Git.

## [0.1.0] - 2026-11-24
### Added
- Первая внутренняя сборка: афиша, бронирование мест, электронные билеты с QR, касса, роли администратора и зрителя, локальная SQLite.

[Unreleased]: https://github.com/philotiona/KADR/compare/v1.2.1...HEAD
[1.2.1]: https://github.com/philotiona/KADR/compare/v1.2.0...v1.2.1
[1.2.0]: https://github.com/philotiona/KADR/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/philotiona/KADR/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/philotiona/KADR/compare/v0.1.0...v1.0.0
[0.1.0]: https://github.com/philotiona/KADR/releases/tag/v0.1.0
