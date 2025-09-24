import { useTranslations } from '@/hooks/useTranslations';
import { OphButton } from '@opetushallitus/oph-design-system';
import React from 'react';
import { Link } from 'react-router';

const NotFoundPage = () => {
  const { t } = useTranslations();
  return (
    <div style={{ textAlign: 'center', marginTop: '10vh' }}>
      <h1>{t('404.sivua-ei-loytynyt')}</h1>
      <p>{t('404.sivua-ei-loytynyt-teksti')}</p>
      <OphButton to="/" component={Link} variant="contained">
        {t('404.mene-etusivulle')}
      </OphButton>
    </div>
  );
};

export default NotFoundPage;
