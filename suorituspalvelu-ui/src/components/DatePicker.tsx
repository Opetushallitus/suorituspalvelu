import { Box, InputAdornment } from '@mui/material';
import {
  ophColors,
  OphInput,
  OphFormFieldWrapper,
} from '@opetushallitus/oph-design-system';
import ReactDatePicker, { registerLocale } from 'react-datepicker';
import 'react-datepicker/dist/react-datepicker.css';
import { type ReactNode, useRef } from 'react';
import {
  CalendarTodayOutlined,
  KeyboardArrowRight,
  KeyboardArrowLeft,
} from '@mui/icons-material';
import { capitalize } from 'remeda';
import { styled } from '../lib/theme';
import { useTranslations } from '@/hooks/useTranslations';
import { fi, sv, enIE } from 'date-fns/locale';

registerLocale('fi', fi);
registerLocale('sv', sv);
registerLocale('en', enIE);

const CALENDAR_CLASSNAME = 'oph-calendar';

const CalendarStyles = styled(Box)(({ theme }) => ({
  '.oph-calendar': {
    border: 'none',
    boxShadow: '2px 2px 8px 0px rgba(0,0,0,0.17)',
    '.react-datepicker': {
      '&__header': {
        backgroundColor: ophColors.white,
        borderBottom: 'none',
        paddingTop: theme.spacing(2),
        'h2.react-datepicker__current-month, .react-datepicker-time__header': {
          ...theme.typography.label,
        },
      },
      '&__day': {
        fontSize: 'inherit',
        width: '2rem',
        height: '2rem',
        lineHeight: '2rem',
        verticalAlign: 'bottom',
        '&:focus-visible': {
          outline: `2px solid ${ophColors.black}`,
        },
      },
      '&__day--selected, &__day--keyboard-selected': {
        backgroundColor: ophColors.blue2,
        borderRadius: 45,
        color: ophColors.white,
      },
      '&__day:hover:not(&__day--disabled)': {
        backgroundColor: ophColors.lightBlue2,
        borderRadius: 45,
      },
      '&__day-name': {
        color: ophColors.grey400,
        width: '2rem',
        marginTop: theme.spacing(1),
      },
    },
    '.react-datepicker__time-container': {
      borderColor: ophColors.grey200,
    },
    '.react-datepicker__time-box ul.react-datepicker__time-list li.react-datepicker__time-list-item':
      {
        height: 'unset',
        marginBottom: theme.spacing(0.3),
        '&:focus-visible': {
          outline: `2px solid ${ophColors.black}`,
        },
      },
    '.react-datepicker__time-box ul.react-datepicker__time-list li.react-datepicker__time-list-item--selected':
      {
        backgroundColor: ophColors.blue2,
        '&:hover': {
          backgroundColor: ophColors.blue2,
        },
      },
    '.react-datepicker__time-box ul.react-datepicker__time-list li.react-datepicker__time-list-item:hover':
      {
        backgroundColor: ophColors.lightBlue2,
      },
    '&__navigation': {
      marginTop: theme.spacing(0.5),
      marginLeft: theme.spacing(1.3),
      marginRight: theme.spacing(1.3),
      '&:focus-visible': {
        outline: `2px solid ${ophColors.black}`,
      },
      '&-icon': {
        border: '1px solid',
        borderColor: ophColors.grey300,
        borderRadius: 45,
        color: ophColors.grey300,
        '&:hover': {
          borderColor: ophColors.blue2,
          color: ophColors.blue2,
        },
      },
    },
  },
  '.react-datepicker__triangle': {
    display: 'none',
  },
}));

const StyledOphInput = styled(OphInput)(() => ({
  marginTop: 2,
}));

interface CalendarInputProps {
  value?: string | Date;
  onClick?: () => void;
}

export type DatePickerProps = {
  value?: Date | null;
  onChange?: (value: Date | null) => void;
  label: string;
  required?: boolean;
};

export const DatePicker = ({
  value,
  onChange,
  label,
  required = false,
}: DatePickerProps) => {
  const { t, getLanguage } = useTranslations();

  const refCustomInput = useRef<HTMLInputElement>(null);

  const CustomInput = function renderCalendarInput({
    ...props
  }: CalendarInputProps & { ref?: React.RefObject<HTMLInputElement | null> }) {
    return (
      <StyledOphInput
        placeholder={t('datepicker.placeholder')}
        {...props}
        endAdornment={
          <InputAdornment position="end">
            <CalendarTodayOutlined style={{ color: ophColors.blue2 }} />
          </InputAdornment>
        }
      />
    );
  };

  const language = getLanguage();

  return (
    <CalendarStyles>
      <OphFormFieldWrapper
        label={label}
        required={required}
        renderInput={({ labelId }) => (
          <ReactDatePicker
            ariaLabelledBy={labelId}
            selected={value}
            onChange={onChange}
            customInput={<CustomInput ref={refCustomInput} />}
            calendarClassName={CALENDAR_CLASSNAME}
            dateFormat="dd.MM.yyyy"
            strictParsing={true}
            formatWeekDay={(wd) => capitalize(wd).substring(0, 2)}
            locale={language}
            placeholderText={t('datepicker.placeholder')}
            popperContainer={({ children }: { children?: ReactNode }) => (
              <Box sx={{ position: 'fixed', zIndex: 9999, width: '100%' }}>
                {children}
              </Box>
            )}
            popperPlacement="bottom-start"
            renderCustomHeader={({
              monthDate,
              decreaseMonth,
              increaseMonth,
            }) => (
              <div>
                <button
                  type="button"
                  aria-label={t('kalenteri.edellinen-kuukausi')}
                  className={
                    'react-datepicker__navigation react-datepicker__navigation--previous oph-calendar__navigation'
                  }
                  onClick={decreaseMonth}
                >
                  <KeyboardArrowLeft className="oph-calendar__navigation-icon" />
                </button>
                <span className="react-datepicker__current-month">
                  {capitalize(
                    monthDate.toLocaleString(language, {
                      month: 'long',
                      year: 'numeric',
                    }),
                  )}
                </span>
                <button
                  type="button"
                  aria-label={t('kalenteri.seuraava-kuukausi')}
                  className={
                    'react-datepicker__navigation react-datepicker__navigation--next oph-calendar__navigation'
                  }
                  onClick={increaseMonth}
                >
                  <KeyboardArrowRight className="oph-calendar__navigation-icon" />
                </button>
              </div>
            )}
          />
        )}
      />
    </CalendarStyles>
  );
};
