const api = {
  async post(path, body) {
    const headers = { 'Content-Type': 'application/json' };
    const token = localStorage.getItem('token');
    if (token) headers['Authorization'] = 'Bearer ' + token;

    const res = await fetch(path, {
      method: 'POST',
      headers,
      body: JSON.stringify(body)
    });
    if (res.headers.get('content-type')?.includes('application/json')) {
      return await res.json();
    }
    return { success: res.ok, status: res.status };
  },

  async get(path) {
    const headers = {};
    const token = localStorage.getItem('token');
    if (token) headers['Authorization'] = 'Bearer ' + token;
    const res = await fetch(path, { method: 'GET', headers });
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

  ,

  // Query songs via backend if available, otherwise fetch all and filter client-side
  async querySongs(query) {
    // try backend query endpoint
    try {
      const resp = await this.post('/api/songs/query', query);
      if (resp && resp.songs) return resp;
    } catch (e) {
      // ignore and fallback
    }

    // fallback: fetch all and filter locally
    const all = await this.get('/api/songs');
    const songs = (all.songs || []).filter(s => {
      if (query.type === 'title-artist') {
        return (!query.title || (s.title||'').toLowerCase().includes(query.title.toLowerCase()))
            && (!query.artist || (s.artist||'').toLowerCase().includes(query.artist.toLowerCase()));
      }
      if (query.type === 'album-artist') {
        return (!query.album || (s.album||'').toLowerCase().includes(query.album.toLowerCase()))
            && (!query.artist || (s.artist||'').toLowerCase().includes(query.artist.toLowerCase()));
      }
      if (query.type === 'artist-year') {
        return (!query.artist || (s.artist||'').toLowerCase().includes(query.artist.toLowerCase()))
            && (!query.year || (s.year||'').toString().includes(query.year.toString()));
      }
      return false;
    });
    return { songs };
  }
};

// Expose api to global scope for simple pages
window.api = api;
