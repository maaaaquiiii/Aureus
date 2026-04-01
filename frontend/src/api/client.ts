import axios from "axios";

const client = axios.create({
    baseURL: "http://localhost:8080/api/v1",
    headers: {
        "Content-Type": "application/json",
    },
});

client.interceptors.request.use((config) => {
    const stored = localStorage.getItem("auth");
    if (stored) {
        const { token } = JSON.parse(stored);
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

export default client;