import { FullSpinner } from './FullSpinner';
import { OphModal, type OphModalProps } from './OphModal';

export const SpinnerModal = (props: OphModalProps) => {
  return (
    <OphModal maxWidth="md" titleAlign="center" {...props}>
      <FullSpinner />
    </OphModal>
  );
};
