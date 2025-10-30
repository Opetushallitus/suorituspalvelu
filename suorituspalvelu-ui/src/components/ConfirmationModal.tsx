import { OphButton } from '@opetushallitus/oph-design-system';
import { OphModal } from './OphModal';
import { useTranslations } from '@/hooks/useTranslations';
import React from 'react';

export type ConfirmationModalProps = {
  title: string;
  open: boolean;
  content?: React.ReactNode;
  onConfirm: () => void;
  onCancel?: () => void;
  confirmLabel?: string;
  cancelLabel?: string;
  maxWidth?: 'sm' | 'md' | false;
};

type ConfirmationModalContextValue = {
  showConfirmation: (props: Omit<ConfirmationModalProps, 'open'>) => void;
  hideConfirmation: () => void;
};

export const ConfirmationModalContext =
  React.createContext<ConfirmationModalContextValue | null>(null);

export const ConfirmationModalProvider = ({
  children,
}: {
  children: React.ReactNode;
}) => {
  const [modalProps, setModalProps] =
    React.useState<ConfirmationModalProps | null>(null);

  const contextValue: ConfirmationModalContextValue = React.useMemo(
    () => ({
      showConfirmation: (props) =>
        setModalProps({
          ...props,
          onConfirm: () => {
            props.onConfirm();
            setModalProps(null);
          },
          onCancel: () => {
            props.onCancel?.();
            setModalProps(null);
          },
          open: true,
        }),
      hideConfirmation: () => setModalProps(null),
    }),
    [setModalProps],
  );

  return (
    <ConfirmationModalContext value={contextValue}>
      {modalProps && <ConfirmationModal {...modalProps} />}
      {children}
    </ConfirmationModalContext>
  );
};

export const ConfirmationModal = ({
  title,
  open,
  content,
  onConfirm,
  onCancel,
  confirmLabel,
  cancelLabel,
  maxWidth = 'sm',
}: ConfirmationModalProps) => {
  const { t } = useTranslations();
  return (
    <OphModal
      open={open}
      onClose={onCancel}
      title={title}
      maxWidth={maxWidth}
      actions={
        <>
          <OphButton variant="outlined" onClick={onCancel}>
            {cancelLabel ?? t('ei')}
          </OphButton>
          <OphButton variant="contained" onClick={onConfirm}>
            {confirmLabel ?? t('kylla')}
          </OphButton>
        </>
      }
    >
      {content}
    </OphModal>
  );
};

export const useGlobalConfirmationModal = () => {
  const context = React.use(ConfirmationModalContext);
  if (!context) {
    throw new Error(
      'useGlobalConfirmationModal must be used within a ConfirmationModalProvider',
    );
  }
  return context;
};
