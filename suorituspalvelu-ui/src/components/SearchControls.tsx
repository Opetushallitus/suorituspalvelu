import { DEFAULT_BORDER } from '@/common';
import { useSearchQueryParamsState } from '@/hooks/useSearchOppijat';
import { useTranslate } from '@tolgee/react';

export function SearchControls() {
  const { t } = useTranslate();

  const { oppijaSearchTerm, setOppijaSearchTerm } = useSearchQueryParamsState();

  return (
    <div
      style={{ width: '100%', height: '50px', borderBottom: DEFAULT_BORDER }}
    >
      <label>{t('hae-henkiloa')}: </label>
      <input
        value={oppijaSearchTerm ?? ''}
        type="text"
        onChange={(e) => {
          setOppijaSearchTerm(e.target.value);
        }}
      />
    </div>
  );
}
