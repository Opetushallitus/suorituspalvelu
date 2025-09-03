import {
  type EBOppiaine,
  type IBOppiaine,
  type LukionOppiaine,
  type LukioSuoritus,
  type YOKoe,
} from '@/types/ui-types';
import { SuoritusInfoPaper } from './SuoritusInfoPaper';
import { ophColors } from '@opetushallitus/oph-design-system';
import { SuorituksenPerustiedotIndicator } from './SuorituksenPerustiedotIndicator';
import { useTranslations } from '@/hooks/useTranslations';
import { LabeledInfoItem } from './LabeledInfoItem';
import { styled } from '@/lib/theme';
import { TableBody, TableCell, TableHead, TableRow } from '@mui/material';
import { StripedTable } from './StripedTable';
import { isEmpty } from 'remeda';
import { pointToComma, formatFinnishDate } from '@/lib/common';

const UnorderedList = styled('ul')(({ theme }) => ({
  margin: 0,
  paddingLeft: theme.spacing(2),
}));

const LukionOppiaineetList = ({
  oppiaineet,
}: {
  oppiaineet: Array<LukionOppiaine>;
}) => {
  const { t, translateKielistetty } = useTranslations();
  return (
    <LabeledInfoItem
      label={t('oppija.oppiaineet')}
      value={
        <UnorderedList>
          {oppiaineet.map((oppiaine) => (
            <li key={oppiaine.tunniste}>
              {translateKielistetty(oppiaine.nimi)}
            </li>
          ))}
        </UnorderedList>
      }
    />
  );
};

const IndentedCell = styled(TableCell)(({ theme }) => ({
  textIndent: theme.spacing(1),
}));

const EBOppiaineetTable = ({
  oppiaineet,
}: {
  oppiaineet: Array<EBOppiaine>;
}) => {
  const { t, translateKielistetty } = useTranslations();

  return (
    <StripedTable stripeGroup="body">
      <TableHead>
        <TableRow>
          <TableCell>{t('oppija.oppiaine')}</TableCell>
          <TableCell>{t('oppija.suorituskieli')}</TableCell>
          <TableCell>
            {t('oppija.laajuus-yksikolla', {
              unit: t('oppija.lyhenne-vuosiviikkotunti'),
            })}
          </TableCell>
          <TableCell>{t('oppija.arvosana')}</TableCell>
        </TableRow>
      </TableHead>
      {oppiaineet.map((row) => (
        <TableBody key={row.tunniste}>
          <TableRow>
            <TableCell>{translateKielistetty(row.nimi)}</TableCell>
            <TableCell>{row.suorituskieli}</TableCell>
            <TableCell>{row.laajuus}</TableCell>
            <TableCell />
          </TableRow>
          <TableRow>
            <IndentedCell colSpan={3}>{t('oppija.eb-written')}</IndentedCell>
            <TableCell>{pointToComma(row.written?.arvosana)}</TableCell>
          </TableRow>
          <TableRow>
            <IndentedCell colSpan={3}>{t('oppija.eb-oral')}</IndentedCell>
            <TableCell>{pointToComma(row.oral?.arvosana)}</TableCell>
          </TableRow>
          <TableRow>
            <IndentedCell colSpan={3}>{t('oppija.eb-final')}</IndentedCell>
            <TableCell>{pointToComma(row.final?.arvosana)}</TableCell>
          </TableRow>
        </TableBody>
      ))}
    </StripedTable>
  );
};

const IBOppiaineetTable = ({
  oppiaineet,
}: {
  oppiaineet: Array<IBOppiaine>;
}) => {
  const { t, translateKielistetty } = useTranslations();

  return (
    <StripedTable stripeGroup="body">
      <TableHead>
        <TableRow>
          <TableCell>{t('oppija.oppiaine')}</TableCell>
          <TableCell>
            {t('oppija.laajuus-yksikolla', {
              unit: t('oppija.lyhenne-vuosiviikkotunti'),
            })}
          </TableCell>
          <TableCell>{t('oppija.predicted-grade')}</TableCell>
          <TableCell>{t('oppija.arvosana')}</TableCell>
        </TableRow>
      </TableHead>
      {oppiaineet.map((oppiaine) => (
        <TableBody key={oppiaine.tunniste}>
          <TableRow>
            <TableCell colSpan={4}>
              {translateKielistetty(oppiaine.nimi)}
            </TableCell>
          </TableRow>
          {oppiaine.suoritukset.map((suoritus) => (
            <TableRow key={suoritus.tunniste}>
              <IndentedCell>{translateKielistetty(suoritus.nimi)}</IndentedCell>
              <TableCell>{suoritus.laajuus}</TableCell>
              <TableCell>{suoritus.predictedGrade}</TableCell>
              <TableCell>{suoritus.arvosana}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      ))}
    </StripedTable>
  );
};

function DiaVastaavuusTodistusOppiaineet({
  suoritus,
}: {
  suoritus: LukioSuoritus;
}) {
  const { t, translateKielistetty } = useTranslations();

  return (
    'kieletKirjallisuusTaide' in suoritus && (
      <StripedTable stripeGroup="body">
        <TableHead>
          <TableRow>
            <TableCell>{t('oppija.oppiaine')}</TableCell>
            <TableCell>
              {t('oppija.laajuus-yksikolla', {
                unit: t('oppija.lyhenne-vuosiviikkotunti'),
              })}
            </TableCell>
            <TableCell>{t('oppija.keskiarvo')}</TableCell>
          </TableRow>
        </TableHead>
        {!isEmpty(suoritus.kieletKirjallisuusTaide) && (
          <TableBody>
            <TableRow>
              <TableCell colSpan={3}>
                {t('oppija.kielet-kirjallisuus-taide')}
              </TableCell>
            </TableRow>
            {suoritus.kieletKirjallisuusTaide.map((oppiaine) => (
              <TableRow key={oppiaine.tunniste}>
                <IndentedCell>
                  {translateKielistetty(oppiaine.nimi)}
                </IndentedCell>
                <TableCell>{oppiaine.laajuus}</TableCell>
                <TableCell>{pointToComma(oppiaine.keskiarvo)}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        )}
        {!isEmpty(suoritus.matematiikkaLuonnontieteet) && (
          <TableBody>
            <TableRow>
              <TableCell colSpan={3}>
                {t('oppija.matematiikka-ja-luonnontieteet')}
              </TableCell>
            </TableRow>
            {suoritus.matematiikkaLuonnontieteet.map((oppiaine) => (
              <TableRow key={oppiaine.tunniste}>
                <IndentedCell>
                  {translateKielistetty(oppiaine.nimi)}
                </IndentedCell>
                <TableCell>{oppiaine.laajuus}</TableCell>
                <TableCell>{pointToComma(oppiaine.keskiarvo)}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        )}
      </StripedTable>
    )
  );
}

function LukionOppiaineet({ suoritus }: { suoritus: LukioSuoritus }) {
  if ('oppiaineet' in suoritus) {
    switch (suoritus.koulutustyyppi) {
      case 'eb':
        return <EBOppiaineetTable oppiaineet={suoritus.oppiaineet} />;
      case 'ib':
        return <IBOppiaineetTable oppiaineet={suoritus.oppiaineet} />;
      default:
        return <LukionOppiaineetList oppiaineet={suoritus.oppiaineet} />;
    }
  } else if ('kieletKirjallisuusTaide' in suoritus) {
    return <DiaVastaavuusTodistusOppiaineet suoritus={suoritus} />;
  }
  return null;
}

const YoKokeetTable = ({ yoKokeet }: { yoKokeet: Array<YOKoe> }) => {
  const { t } = useTranslations();

  return (
    <StripedTable>
      <TableHead>
        <TableRow>
          <TableCell>{t('oppija.yo-kokeen-aine')}</TableCell>
          <TableCell>{t('oppija.yo-kokeen-taso')}</TableCell>
          <TableCell>{t('oppija.arvosana')}</TableCell>
          <TableCell>{t('oppija.yo-kokeen-yhteispistemaara')}</TableCell>
          <TableCell>{t('oppija.yo-kokeen-tutkintokerta')}</TableCell>
        </TableRow>
      </TableHead>
      <TableBody>
        {yoKokeet.map((row) => (
          <TableRow key={row.aine}>
            <TableCell>{row.aine}</TableCell>
            <TableCell>{row.taso}</TableCell>
            <TableCell>{row.arvosana}</TableCell>
            <TableCell>{row.yhteispistemaara.toString()}</TableCell>
            <TableCell>{formatFinnishDate(row.tutkintokerta)}</TableCell>
          </TableRow>
        ))}
      </TableBody>
    </StripedTable>
  );
};

export const LukioSuoritusPaper = ({
  suoritus,
}: {
  suoritus: LukioSuoritus;
}) => {
  return (
    <SuoritusInfoPaper suoritus={suoritus} topColor={ophColors.blue2}>
      <SuorituksenPerustiedotIndicator perustiedot={suoritus} />
      <LukionOppiaineet suoritus={suoritus} />
      {'yoKokeet' in suoritus && <YoKokeetTable yoKokeet={suoritus.yoKokeet} />}
    </SuoritusInfoPaper>
  );
};
