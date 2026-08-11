import { Navigate, Route, Routes } from 'react-router-dom'

import Layout from './components/Layout.jsx'
import ProtectedRoute from './components/ProtectedRoute.jsx'
import DashboardPage from './pages/DashboardPage.jsx'
import FuelFormPage from './pages/FuelFormPage.jsx'
import LandingPage from './pages/LandingPage.jsx'
import LoginPage from './pages/LoginPage.jsx'
import MaintenanceFormPage from './pages/MaintenanceFormPage.jsx'
import NotFoundPage from './pages/NotFoundPage.jsx'
import ProfilePage from './pages/ProfilePage.jsx'
import RegisterPage from './pages/RegisterPage.jsx'
import ReminderFormPage from './pages/ReminderFormPage.jsx'
import VehicleDetailPage from './pages/VehicleDetailPage.jsx'
import VehicleFormPage from './pages/VehicleFormPage.jsx'
import VehiclesPage from './pages/VehiclesPage.jsx'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      <Route element={<ProtectedRoute />}>
        <Route path="/app" element={<Layout />}>
          <Route index element={<DashboardPage />} />
          <Route path="vehicles" element={<VehiclesPage />} />
          <Route path="vehicles/new" element={<VehicleFormPage />} />
          <Route path="vehicles/:vehicleId" element={<VehicleDetailPage />} />
          <Route path="vehicles/:vehicleId/edit" element={<VehicleFormPage />} />
          <Route path="vehicles/:vehicleId/maintenance/new" element={<MaintenanceFormPage />} />
          <Route path="maintenance/:recordId/edit" element={<MaintenanceFormPage />} />
          <Route path="vehicles/:vehicleId/fuel/new" element={<FuelFormPage />} />
          <Route path="fuel/:entryId/edit" element={<FuelFormPage />} />
          <Route path="vehicles/:vehicleId/reminders/new" element={<ReminderFormPage />} />
          <Route path="reminders/:reminderId/edit" element={<ReminderFormPage />} />
          <Route path="profile" element={<ProfilePage />} />
        </Route>
      </Route>

      <Route path="/app/*" element={<Navigate to="/app" replace />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
