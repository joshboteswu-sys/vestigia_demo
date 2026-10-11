- Photo upload is still not optimal, if not working its either Postman Issue or image too large
- Pls untick the photo for testing to continue

- In login request, under script, paste
const m = pm.response.text().match(/"token"\s*:\s*"([^"]+)"/);
if (m) { pm.collectionVariables.set('token', m[1]); }
