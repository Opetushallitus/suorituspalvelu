import { Alert, Snackbar } from '@mui/material';
import type { AlertColor } from '@mui/material';
import {
  createContext,
  use,
  useCallback,
  useState,
  type ReactNode,
} from 'react';

export type NotificationType = AlertColor;

export type NotificationOptions = {
  message: string;
  type?: NotificationType;
  duration?: number;
};

type NotificationContextType = {
  showNotification: (options: NotificationOptions) => void;
};

const NotificationContext = createContext<NotificationContextType | null>(null);

type NotificationState = {
  open: boolean;
  message: string;
  type: NotificationType;
  duration: number;
};

const DEFAULT_DURATION = 6000;

export function NotificationProvider({ children }: { children: ReactNode }) {
  const [notification, setNotification] = useState<NotificationState>({
    open: false,
    message: '',
    type: 'info',
    duration: DEFAULT_DURATION,
  });

  const showNotification = useCallback(
    ({
      message,
      type = 'info',
      duration = DEFAULT_DURATION,
    }: NotificationOptions) => {
      setNotification({
        open: true,
        message,
        type,
        duration,
      });
    },
    [],
  );

  const handleClose = useCallback(
    (_event?: React.SyntheticEvent | Event, reason?: string) => {
      if (reason === 'clickaway') {
        return;
      }
      setNotification((prev) => ({ ...prev, open: false }));
    },
    [],
  );

  return (
    <NotificationContext value={{ showNotification }}>
      {children}
      <Snackbar
        open={notification.open}
        autoHideDuration={notification.duration}
        onClose={handleClose}
        anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
      >
        <Alert
          onClose={handleClose}
          severity={notification.type}
          variant="filled"
          sx={{ width: '100%' }}
        >
          {notification.message}
        </Alert>
      </Snackbar>
    </NotificationContext>
  );
}

export function useNotifications() {
  const context = use(NotificationContext);
  if (!context) {
    throw new Error(
      'useNotifications must be used within a NotificationProvider',
    );
  }
  return context;
}
