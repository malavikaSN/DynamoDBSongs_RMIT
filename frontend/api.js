const API_BASE_URL = 'http://107.21.77.154';

const api = {
  async post(path, body) {
    const headers = { 'Content-Type': 'application/json' };
    const token = localStorage.getItem('token');

    if (token) {
      headers['Authorization'] = 'Bearer ' + token;
    }

    const res = await fetch(API_BASE_URL + path, {
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

    if (token) {
      headers['Authorization'] = 'Bearer ' + token;
    }

    const res = await fetch(API_BASE_URL + path, {
      method: 'GET',
      headers
    });

    if (res.headers.get('content-type')?.includes('application/json')) {
      return await res.json();
    }

    return { success: res.ok, status: res.status };
  },

  async delete(path, body) {
    const headers = { 'Content-Type': 'application/json' };
    const token = localStorage.getItem('token');

    if (token) {
      headers['Authorization'] = 'Bearer ' + token;
    }

    const res = await fetch(API_BASE_URL + path, {
      method: 'DELETE',
      headers,
      body: JSON.stringify(body)
    });

    if (res.headers.get('content-type')?.includes('application/json')) {
      return await res.json();
    }

    return { success: res.ok, status: res.status };
  },

  async presignUpload(bucket, key) {
    return await this.post('/api/presign', { bucket, key });
  },

  async uploadToS3(url, file) {
    const res = await fetch(url, {
      method: 'PUT',
      body: file
    });

    return { success: res.ok, status: res.status };
  },

  async querySongs(query) {
    return await this.post('/api/songs/query', query);
  }
};

window.api = api;