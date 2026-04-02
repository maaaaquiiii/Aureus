import axios from "axios";

const client = axios.create({
    baseURL: "http://localhost:8080/api/v1",
    headers: {
        "Content-Type": "application/json",
    },
});

let authToken: string | null = null;

export function setAuthToken(token: string | null) {
    authToken = token;
}

client.interceptors.request.use((config) => {
    if (authToken) {
        config.headers.Authorization = `Bearer ${authToken}`;
    }
    return config;
});

export default client;