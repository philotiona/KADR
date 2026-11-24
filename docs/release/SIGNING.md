# Подпись KADR

## Ключ
| Параметр | Значение |
|---|---|
| Файл | `kadr-upload.jks` (PKCS12) |
| Alias | `kadr-upload` |
| Алгоритм | RSA 4096, SHA384withRSA |
| Срок | 10 000 дней (до 2054 г.) |
| SHA-256 сертификата | `9D:3C:82:D0:42:B4:75:3D:1E:B3:13:7D:FA:C4:0C:45:7E:35:23:B8:4C:F3:6D:28:3B:B7:BC:86:42:BD:37:82` |

Создание:
```bash
keytool -genkeypair -v -keystore kadr-upload.jks -storetype PKCS12 \
  -alias kadr-upload -keyalg RSA -keysize 4096 -validity 10000
```

## Upload key и App signing key (Play App Signing)
| | Upload key | App signing key |
|---|---|---|
| Где хранится | У разработчика / в CI Secrets | В Google (Cloud KMS), разработчику недоступен |
| Чем подписывается | AAB перед загрузкой в Play Console | APK, которые Google Play выдаёт пользователям |
| Назначение | Доказать Google, что загрузку сделал владелец | Подпись, которую видит Android на устройстве; обновление принимается, только если подпись совпадает |
| Потеря | Не критично: владелец аккаунта запрашивает сброс upload key в Play Console, Google регистрирует новый сертификат (обычно 1–2 дня) | При Play App Signing невозможна (ключ у Google). Без Play App Signing — **нельзя выпустить обновление**: нужен новый applicationId, пользователи переустанавливают приложение, данные и отзывы не переносятся |
| Компрометация | Сбросить upload key, отозвать доступы CI | Ротация ключа (APK Signature Scheme v3, key rotation) |

Для каналов без Play App Signing (GitHub Releases, RuStore, прямая установка) `kadr-upload.jks` фактически является ключом подписи приложения: Android примет обновление только с той же подписью. Поэтому резервная копия ключа обязательна.

## Хранение
| Где | Что | Защита |
|---|---|---|
| ПК разработчика | `%USERPROFILE%\.kadr-signing\kadr-upload.jks` — вне папки проекта | Учётная запись Windows, BitLocker |
| ПК разработчика | `keystore.properties` в корне проекта | В `.gitignore`, в Git не попадает |
| Резервная копия | Зашифрованный архив (7-Zip AES-256) в облаке + офлайн-флешка | Пароль хранится в менеджере паролей отдельно от архива |
| CI (GitHub Actions) | `KADR_KEYSTORE_BASE64`, `KADR_KEYSTORE_PASSWORD`, `KADR_KEY_ALIAS`, `KADR_KEY_PASSWORD` | GitHub Encrypted Secrets, файл ключа создаётся во временной папке runner и удаляется |

## Настройка в Gradle
`app/build.gradle` читает параметры из `keystore.properties` (ПК) или переменных окружения (CI).
Если ключа нет, release собирается неподписанным, и сборка не падает у других разработчиков.
