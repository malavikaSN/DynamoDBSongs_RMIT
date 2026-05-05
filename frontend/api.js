const api = {
  async post(path, body) {
    const res = await fetch(path, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    });
    if (res.headers.get('content-type')?.includes('application/json')) {
      return await res.json();
    }
    return { success: res.ok, status: res.status };
  },

  async get(path) {
    const res = await fetch(path, { method: 'GET' });
    if (res.headers.get('content-type')?.includes('application/json')) {
      return await res.json();
    }
    return { success: res.ok, status: res.status };
  },

  // Request a presigned upload URL from the backend
  async presignUpload(bucket, key) {
    return await this.post('/api/presign', { bucket, key });
  },

  // Upload a file directly to S3 using presigned URL
  async uploadToS3(url, file) {
    const res = await fetch(url, { method: 'PUT', body: file });
    return { success: res.ok, status: res.status };
  }
};

// Expose api to global scope for simple pages
window.api = api;
