import { Client, Users, Query, ID } from 'node-appwrite';
import { OAuth2Client } from 'google-auth-library';

// Принимает Google idToken от приложения, проверяет его, находит/создаёт
// пользователя Appwrite и выдаёт Custom Token (userId + secret) для входа.
// Приложение затем делает account.createSession(userId, secret) — без браузера.
export default async ({ req, res, log, error }) => {
  let body = {};
  try { body = JSON.parse(req.body || req.bodyRaw || '{}'); } catch (_) {}
  const idToken = body.idToken;
  if (!idToken) return res.json({ ok: false, error: 'no idToken' }, 400);

  // 1) Проверяем Google idToken (audience = наш Web Client ID).
  let payload;
  try {
    const google = new OAuth2Client();
    const ticket = await google.verifyIdToken({
      idToken,
      audience: process.env.GOOGLE_CLIENT_ID,
    });
    payload = ticket.getPayload();
  } catch (e) {
    error('verify failed: ' + e.message);
    return res.json({ ok: false, error: 'invalid token' }, 401);
  }
  const email = (payload.email || '').toLowerCase();
  const name = payload.name || email;
  if (!email) return res.json({ ok: false, error: 'no email' }, 400);

  // 2) Appwrite server client.
  const client = new Client()
    .setEndpoint(process.env.APPWRITE_FUNCTION_API_ENDPOINT || 'https://fra.cloud.appwrite.io/v1')
    .setProject(process.env.APPWRITE_FUNCTION_PROJECT_ID)
    .setKey(process.env.APPWRITE_API_KEY);
  const users = new Users(client);

  // 3) Находим пользователя по email или создаём.
  let user;
  try {
    const found = await users.list([Query.equal('email', email)]);
    user = found.total > 0 ? found.users[0] : await users.create(ID.unique(), email, undefined, undefined, name);
  } catch (e) {
    error('user lookup/create failed: ' + e.message);
    return res.json({ ok: false, error: 'user error' }, 500);
  }

  // 4) Custom Token → секрет для входа в приложении.
  try {
    const token = await users.createToken(user.$id);
    return res.json({ ok: true, userId: user.$id, secret: token.secret });
  } catch (e) {
    error('createToken failed: ' + e.message);
    return res.json({ ok: false, error: 'token error' }, 500);
  }
};
