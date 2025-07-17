'use client';
import { useTranslate } from '@tolgee/react';
import Head from 'next/head';

const HomePage = () => {
  const { t } = useTranslate();
  return (
    <div>
      <Head>
        <title>{t('suorituspalvelu')}</title>
      </Head>
    </div>
  );
};

export default HomePage;
