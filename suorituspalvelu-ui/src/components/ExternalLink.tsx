import { OphLink } from '@opetushallitus/oph-design-system';

export const ExternalLink = ({
  href,
  children,
}: {
  href: string;
  children: React.ReactNode;
}) => {
  return (
    <OphLink
      href={href}
      component="a"
      iconVisible={true}
      target="_blank"
      rel="noopener noreferrer"
    >
      {children}
    </OphLink>
  );
};
