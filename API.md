# API specification (minimal)

POST /api/register
- Request: { email, user_name, password }
- Response: 201 { success: true }

POST /api/login
- Request: { email, password }
- Response: 200 { success: true, token }

POST /api/presign
- Request: { bucket, key, expiresMinutes }
- Response: 200 { success: true, url }

GET /api/songs
- Response: 200 { songs: [ ... ] }

POST /api/songs
- Request: { artist, songKey, title, album, year, image_url }
- Response: 200 { success: true }

Notes:
- JWT is returned by /api/login. Current implementation places a demo secret in code; move this to env for production.
- S3 presigned uploads require proper bucket CORS as documented in README.
