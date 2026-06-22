import { Client, Users, Databases } from 'node-appwrite';

const DB = '6a38fc430015b7804515';
const COL = 'profiles';

/**
 * Привилегированные действия разработчика.
 * Авторизация — по списку DEVELOPER_IDS (env), а не по флагу в профиле.
 * Действия: whoami, setVerified, setDeveloper, deleteUser.
 */
export default async ({ req, res, log, error }) => {
  const client = new Client()
    .setEndpoint(process.env.APPWRITE_FUNCTION_API_ENDPOINT)
    .setProject(process.env.APPWRITE_FUNCTION_PROJECT_ID)
    .setKey(process.env.APPWRITE_API_KEY);

  const users = new Users(client);
  const db = new Databases(client);

  const devIds = (process.env.DEVELOPER_IDS || '')
    .split(',').map((s) => s.trim()).filter(Boolean);
  const caller = req.headers['x-appwrite-user-id'] || '';
  const isDev = devIds.includes(caller);

  let body = {};
  try { body = req.bodyJson ?? JSON.parse(req.body || '{}'); } catch (e) { /* ignore */ }
  const action = body.action;

  try {
    if (action === 'whoami') {
      if (isDev) {
        try { await db.updateDocument(DB, COL, caller, { isDeveloper: true }); } catch (e) { /* профиль ещё не создан */ }
      }
      return res.json({ ok: true, isDeveloper: isDev });
    }

    if (!isDev) return res.json({ ok: false, error: 'Недостаточно прав' });

    if (action === 'setVerified') {
      await db.updateDocument(DB, COL, body.target, { isVerified: !!body.value });
      return res.json({ ok: true });
    }
    if (action === 'setDeveloper') {
      await db.updateDocument(DB, COL, body.target, { isDeveloper: !!body.value });
      return res.json({ ok: true });
    }
    if (action === 'deleteUser') {
      try { await db.deleteDocument(DB, COL, body.target); } catch (e) { /* профиля может не быть */ }
      await users.delete(body.target);
      return res.json({ ok: true });
    }

    return res.json({ ok: false, error: 'Неизвестное действие' });
  } catch (e) {
    error(e.message);
    return res.json({ ok: false, error: e.message });
  }
};
