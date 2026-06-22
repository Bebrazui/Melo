# Melo admin function

Привилегированные действия разработчика: выдача галочки (`isVerified`), статуса
разработчика (`isDeveloper`), удаление аккаунтов.

## Деплой
1. Создай функцию в Appwrite (Node 18+), entrypoint `src/main.js`, build `npm install`.
2. Залей содержимое этой папки (или `admin.tar.gz`: `src/main.js` + `package.json` в корне).
3. Settings → Execute Access: **Users** (любой залогиненный может вызвать; права проверяются внутри).
4. Variables (env):
   - `APPWRITE_API_KEY` — API-ключ с правами `databases.write`, `users.read`, `users.write`.
   - `DEVELOPER_IDS` — список userId разработчиков через запятую (твой ID — он показан в «Аккаунте» приложения).
   - `APPWRITE_FUNCTION_API_ENDPOINT` и `APPWRITE_FUNCTION_PROJECT_ID` Appwrite задаёт сам.
5. Скопируй Function ID и впиши в `AdminRepository.FUNCTION_ID` в приложении.

## Bootstrap первого разработчика
После деплоя и установки `DEVELOPER_IDS`: в приложении (Аккаунт → «Статус») вызовется
`whoami`, который пометит твой профиль `isDeveloper = true`. После этого появится
шестерёнка и панель разработчика на чужих профилях.

## Действия (body JSON)
- `{ "action": "whoami" }` → `{ ok, isDeveloper }`
- `{ "action": "setVerified", "target": "<userId>", "value": true }`
- `{ "action": "setDeveloper", "target": "<userId>", "value": true }`
- `{ "action": "deleteUser", "target": "<userId>" }`
