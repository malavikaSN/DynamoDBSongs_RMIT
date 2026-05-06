This frontend is a simple static prototype for the DynamoDBSongs project.

Files:
- index.html  - Main page showing songs (placeholder)
- login.html  - Login form that calls the backend API
- register.html - Registration form
- api.js      - Small wrapper for API calls (login/register, presigned S3 upload)
- styles.css  - Basic styles

How to use:
- Serve these files from any static webserver (e.g. `npx http-server frontend`)
- The API client (`api.js`) assumes backend endpoints under `/api/*`:
  - POST /api/login   { email, password }
  - POST /api/register { email, user_name, password }
  - POST /api/presign  { bucket, key } -> { url }

Notes:
- This is a starting point; you will need to implement the backend endpoints to accept these requests and interact with DynamoDB/S3.
