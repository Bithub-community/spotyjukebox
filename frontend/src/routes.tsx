import React, { useContext, useEffect } from 'react';
import { BrowserRouter as Router, Route, Routes, Navigate } from 'react-router-dom';
import { AuthProvider, AuthContext } from '@/context/AuthProvider';
import { userManager } from '@/config/oidc.config';
import LoginPage from '@/pages/LoginPage';
import PostLoginPage from '@/pages/PostLoginPage';

const CallbackPage: React.FC = () => {

  const handleAuthCallback = async () => {
    const params = new URLSearchParams(window.location.search);
    const accessToken = params.get('access_token');
    
    if (accessToken) {
      // Store the access token securely (e.g., in memory or secure storage)
      localStorage.setItem('spotify_access_token', accessToken);
    
    }
  };

  useEffect(() => {
    userManager.signinRedirectCallback().then(() => {
      handleAuthCallback();
      window.location.href = '/main';
    });
  }, []);

  return <div>Loading...</div>;
};

const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { user } = useContext(AuthContext);
  if (!user) {
    return <Navigate to="/login" />;
  }
  return <>{children}</>;
};

export const RoutesProvider: React.FC = () => {
  return (
    <AuthProvider>
      <Router>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/callback" element={<CallbackPage />} />
          <Route
            path="/main"
            element={
              <ProtectedRoute>
                <PostLoginPage />
              </ProtectedRoute>
            }
          />
          <Route path="/" element={<Navigate to="/login" />} />
        </Routes>
      </Router>
    </AuthProvider>
  )
};