import { FullSpinner } from '@/components/FullSpinner';
import { useNavigation } from 'react-router';

export const NavigationSpinner = ({
  children,
}: {
  children: React.ReactNode;
}) => {
  const navigation = useNavigation();
  const isNavigating = Boolean(navigation.location);

  return isNavigating ? <FullSpinner /> : children;
};
