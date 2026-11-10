import '@fontsource-variable/lexend';
import '@fontsource-variable/fraunces';
import { BrowserRouter } from 'react-router-dom';
import { ProvedorDeAutenticacao } from '../features/auth/AuthContext.jsx';
import AppRoutes from './AppRoutes.jsx';
import '../shared/styles/base.css';

export default function App() {
  return (
    <BrowserRouter>
      <ProvedorDeAutenticacao>
        <AppRoutes />
      </ProvedorDeAutenticacao>
    </BrowserRouter>
  );
}
