# WealthFlow Finance Tracker - Cloud Sync Gateway

A robust, production-ready, secure, zero-knowledge REST API and data synchronization gateway built with Node.js, Express, and SQLite.

This server acts as the cloud backup registry and multi-wallet data sync companion for the WealthFlow Android client app.

---

## Key Production Features

1. **Zero-Knowledge Architecture**: The server receives database states that are already encrypted client-side with AES-256. It indexes payloads using a hashed client passcode. The server operators can never read the client’s financial records.
2. **REST API Data Sync**: Direct synchronization of accounts and transaction ledgers to easily enable cross-device compatibility.
3. **Robust Local Storage**: SQLite is utilized out-of-the-box for low overhead, single-instance deployments, but can be scaled to PostgreSQL pools seamlessly.
4. **CORS Protected**: Configured for cross-origin compliance on Android mobile and desktop clients.

---

## 🚀 Local Quickstart Guide

Ensure you have [Node.js (v18+)](https://nodejs.org/) installed.

### 1. Install Dependencies
```bash
cd backend
npm install
```

### 2. Configure Environment Variables
Create a `.env` file in the root of the `/backend` directory:
```env
PORT=5000
NODE_ENV=production
```

### 3. Run the Server
*   **Production Launch:**
    ```bash
    npm start
    ```
*   **Development / Live-Reload Mode:**
    ```bash
    npm run dev
    ```

Check that it's working:
*   Open `http://localhost:5000/api/status` in your browser. You should see a JSON health payload indicating a healthy connection.

---

## 🐳 Docker Deployment

To build and run this containerized microservice:

### 1. Build the Docker Image
```bash
docker build -t wealthflow-sync-gateway .
```

### 2. Spin up the Container
```bash
docker run -d -p 5000:5000 --name wealthflow-backend -v $(pwd)/db_data:/backend wealthflow-sync-gateway
```

---

## ☁️ Cloud Deployment Instructions

### Option A: Deployed to [Render](https://render.com) (Recommended)
1. Commit the `backend` subdirectory to your GitHub repository.
2. Go to **Render Dashboard** and select **New Web Service**.
3. Link your GitHub repository.
4. Set the following settings:
    *   **Root Directory:** `backend`
    *   **Runtime:** `Node`
    *   **Build Command:** `npm install`
    *   **Start Command:** `npm start`
5. Click **Deploy Web Service**. You will receive an HTTPS endpoint (e.g. `https://wealthflow-api.onrender.com`).
6. Point your WealthFlow Android client to this URL under **Cloud Server Portal**!

### Option B: Deployed to [Railway](https://railway.app)
1. Select **New Project** -> **Deploy from GitHub repo**.
2. Select your repository.
3. Railway automatically detects the project. Add standard Environment Variables: `PORT=5000`.
4. Click deploy.
