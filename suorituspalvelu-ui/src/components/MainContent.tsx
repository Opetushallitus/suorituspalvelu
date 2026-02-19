import { Box } from '@mui/material';
import { QuerySuspenseBoundary } from './QuerySuspenseBoundary';

export const MainContent = ({ children }: { children: React.ReactNode }) => (
  <Box component="main" sx={{ flexGrow: 1, width: '100%' }}>
    <QuerySuspenseBoundary>{children}</QuerySuspenseBoundary>
  </Box>
);
