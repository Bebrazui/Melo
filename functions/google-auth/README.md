# Melo — Google Auth Function

Серверная функция Appwrite: меняет Google `idToken` на сессию Appwrite (Custom Token),
без браузера. Нужна, потому что OAuth-домены Appwrite заблокированы по IP в РФ.

## Что сделать в Appwrite Console

### 1. Создать функцию
Functions → **Create function** → Manual / "Create from scratch":
- Runtime: **Node 18** (или новее).
- Name: `google-auth`.
- Запиши её **Function ID** (понадобится в приложении).

### 2. Переменные окружения (Settings → Variables)
- `GOOGLE_CLIENT_ID` = твой **Web** OAuth Client ID из Google Cloud (тот же, что в провайдере Google).
- `APPWRITE_API_KEY` = серверный API-ключ со скоупами **users.read, users.write**
  (Project → API Keys → Create; можно тот же, что для setup, если ещё жив, но лучше отдельный).
- `APPWRITE_FUNCTION_API_ENDPOINT` = `https://fra.cloud.appwrite.io/v1` (обычно подставляется само).

### 3. Права запуска
Settings → **Execute Access** → добавь **Any** (или Users), чтобы приложение могло вызвать.

### 4. Деплой кода
Вариант А — **Appwrite CLI** (проще всего):
```bash
npm i -g appwrite-cli
appwrite login
appwrite push function   # выбери google-auth, путь functions/google-auth
```
Вариант Б — вручную: запакуй содержимое `functions/google-auth/` (чтобы `src/main.js` и `package.json` были в корне архива) в tar.gz и загрузи в Deployments, entrypoint `src/main.js`, build command `npm install`.

### 5. Дай мне
- **Function ID**
- **Web Client ID** (Google) — для приложения (Credential Manager).

После этого я впишу их в код и соберу — Google-вход заработает без браузера и без VPN.
