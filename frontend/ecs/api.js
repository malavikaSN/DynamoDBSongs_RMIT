// Base URL of backend API (EC2 public IP)
const API_BASE_URL = 'http://34.239.138.8';

const api = {

  // Send POST request (used for register, login, subscribe)
  async post(path, body) {
    const headers = { 'Content-Type': 'application/json' };
    const token = localStorage.getItem('token');

    // Add auth token if exists
    if (token) {
      headers['Authorization'] = 'Bearer ' + token;
    }

    const res = await fetch(API_BASE_URL + path, {
      method: 'POST',
      headers,
      body: JSON.stringify(body)
    });

    // If response is JSON, return parsed JSON
    if (res.headers.get('content-type')?.includes('application/json')) {
      return await res.json();
    }

    // Otherwise return basic status
    return { success: res.ok, status: res.status };
  },

  // Send GET request (used for query songs)
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

  // Send DELETE request (used for removing subscription)
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

  // Request presigned URL for uploading image to S3
  async presignUpload(bucket, key) {
    return await this.post('/api/presign', { bucket, key });
  },

  // Upload file directly to S3 using presigned URL
  async uploadToS3(url, file) {
    const res = await fetch(url, {
      method: 'PUT',
      body: file
    });

    return { success: res.ok, status: res.status };
  },

  // Query songs using GET (RESTful requirement)
  async querySongs(query) {
    const params = new URLSearchParams(query).toString();
    return await this.get('/api/songs?' + params);
  }
};

// Expose api globally to HTML
window.api = api;