import { AuthContext } from "@/context/AuthProvider";
import { useContext, useState, useEffect } from "react";

type UserInfo = {
  id: number;
  userId: string;
  mail: string;
  displayName: string;
  accessToken: string;
  refreshToken: string;
  createdAt: string;
  updatedAt: string;
  scopes: string[];
};

export default function UserInfoPage() {

  const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

  const { user } = useContext(AuthContext);
  const [email, setEmail] = useState("");
  const [data, setData] = useState<UserInfo | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  // 🔹 Automatically get user’s Spotify email if access token is available
  useEffect(() => {
    if (user?.access_token) {
      fetch("https://api.spotify.com/v1/me", {
        headers: { Authorization: `Bearer ${user.access_token}` },
      })
        .then((res) => res.json())
        .then((profile) => setEmail(profile.email))
        .catch((err) => console.error("Failed to load Spotify profile", err));
    }
  }, [user]);

  // 🔹 Fetch user data from backend using email
  const fetchUser = async () => {
    if (!email.trim()) {
      setError("Please enter an email address");
      return;
    }

    setLoading(true);
    setError(null);
    setData(null);

    try {
      const url = `${API_BASE_URL}/user/get-by-email/${encodeURIComponent(email)}`;
      console.log("📡 Request URL:", url);

      const res = await fetch(url);
      console.log("📥 Response status:", res.status);

      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message || "Request failed");
      }

      const json = (await res.json()) as UserInfo;
      console.log("✅ User data received:", json);
      setData(json);
    } catch (e: any) {
      console.error("❌ Fetch error:", e);
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-50 to-indigo-50 py-10 px-4">
      <div className="max-w-2xl mx-auto bg-white rounded-2xl shadow-xl p-8">
        {/* Header */}
        <h1 className="text-3xl font-extrabold bg-gradient-to-r from-indigo-600 to-purple-600 bg-clip-text text-transparent mb-6 flex items-center gap-2">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            className="h-7 w-7 text-indigo-500"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M5 13l4 4L19 7"
            />
          </svg>
          User Info
        </h1>

        {/* Email Input and Fetch Button */}
        <div className="flex gap-2 mb-6">
          <input
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="flex-1 border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
            placeholder="Enter user email"
          />
          <button
            onClick={fetchUser}
            disabled={loading}
            className="px-5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-medium rounded-lg shadow-sm transition disabled:opacity-50"
          >
            {loading ? "Loading..." : "Fetch"}
          </button>
        </div>

        {/* Error Message */}
        {error && (
          <div className="mb-4 rounded-lg bg-red-50 border border-red-300 text-red-700 px-4 py-3">
            ⚠️ {error}
          </div>
        )}

        {/* User Info Display */}
        {data && (
          <div className="space-y-4">
            <dl className="divide-y divide-gray-200">
              <div className="py-2 flex justify-between">
                <dt className="font-semibold text-gray-600">UserId</dt>
                <dd className="text-gray-800">{data.userId}</dd>
              </div>
              <div className="py-2 flex justify-between">
                <dt className="font-semibold text-gray-600">Scopes</dt>
                <dd className="text-gray-800">
                  {data.scopes?.join(", ") || "—"}
                </dd>
              </div>
              <div className="py-2 flex justify-between">
                <dt className="font-semibold text-gray-600">Created</dt>
                <dd className="text-gray-800">{data.createdAt}</dd>
              </div>
              <div className="py-2 flex justify-between">
                <dt className="font-semibold text-gray-600">Updated</dt>
                <dd className="text-gray-800">{data.updatedAt}</dd>
              </div>
            </dl>

            {/* Expandable Raw JSON */}
            <details className="mt-4">
              <summary className="cursor-pointer text-indigo-600 font-medium">
                Show Raw JSON
              </summary>
              <pre className="bg-gray-100 mt-2 p-4 rounded-lg text-sm text-gray-700 overflow-x-auto">
                {JSON.stringify(data, null, 2)}
              </pre>
            </details>
          </div>
        )}
      </div>
    </div>
  );
}
