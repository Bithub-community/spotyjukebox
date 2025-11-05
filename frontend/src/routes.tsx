import React, { useContext } from 'react';
import { BrowserRouter as Router, Route, Routes, Navigate } from 'react-router-dom';
import { AuthProvider, AuthContext } from '@/context/AuthProvider';
import LoginPage from '@/pages/LoginPage';
import PostLoginPage from '@/pages/PostLoginPage';
import UserInfoPage from './pages/UserInfoPage';
import PlaylistPage from './pages/PlayListPage';
import PlaylistDetailPage from './pages/PlaylistDetailPage';
import SpotifyCallbackPage from './pages/SpotifyCallbackPage';
import SelectDevicePage from './pages/SelectDevicePage';
import ConfirmPage from './pages/ConfirmPage';
import SuccessPage from './pages/SuccessPage';
import MainPage from './pages/MainPage';
import ClientPage from './pages/ClientPage';
import ClientSessionPage from './pages/ClientSessionPage';

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
          <Route path="/callback" element={<SpotifyCallbackPage />} />
          <Route 
          path='/userinfo'
          element={
            <ProtectedRoute>
              <UserInfoPage></UserInfoPage>
            </ProtectedRoute>
          }
          />
          <Route 
          path='/playlists'
          element={
            <ProtectedRoute>
              <PlaylistPage></PlaylistPage>
            </ProtectedRoute>
          }
          />
          <Route 
          path='/playlist/:id'
          element={
            <ProtectedRoute>
              <PlaylistDetailPage></PlaylistDetailPage>
            </ProtectedRoute>
          }
          />
          <Route
            path="/main"
            element={
              <ProtectedRoute>
                <PostLoginPage />
              </ProtectedRoute>
            }
          />
          
          <Route path="/devices" element={
            <ProtectedRoute>
              <SelectDevicePage />
            </ProtectedRoute>
            } 
          />

        <Route path="/confirm" element={
          <ProtectedRoute>
            <ConfirmPage />
          </ProtectedRoute>
          } 
          />
          <Route path="/success" element={<SuccessPage />} /> 

          <Route path="/" element={<MainPage />} />
          <Route path="/client" element={<ClientPage />} />  // QR ile gelenler için
          <Route path="/client/session" element={<ClientSessionPage />} />

        </Routes>

      </Router>
    </AuthProvider>

    
  )
};