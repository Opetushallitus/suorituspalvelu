import { Box, InputAdornment } from '@mui/material';
import {
  ophColors,
  OphInput,
  OphFormFieldWrapper,
} from '@opetushallitus/oph-design-system';
import ReactDatePicker, { registerLocale } from 'react-datepicker';
import { type ReactNode, useRef } from 'react';
import {
  CalendarTodayOutlined,
  KeyboardArrowRight,
  KeyboardArrowLeft,
} from '@mui/icons-material';
import { capitalize } from 'remeda';
import { styled } from '@/lib/theme';
import { useTranslations } from '@/hooks/useTranslations';
import { fi, sv, enUS } from 'date-fns/locale';

registerLocale('fi', fi);
registerLocale('sv', sv);
registerLocale('en', enUS);

const CALENDAR_CLASSNAME = 'oph-calendar';

// React Router / Vite ei suostu lataamaan react-datepickerin tyylejä suoraan CSS-tiedostosta kun ajetaan bundlattua sovellusta
const datePickerStyles = {
  '.react-datepicker__navigation-icon::before, .react-datepicker__year-read-view--down-arrow, .react-datepicker__month-read-view--down-arrow, .react-datepicker__month-year-read-view--down-arrow':
    {
      borderColor: '#ccc',
      borderStyle: 'solid',
      borderWidth: '3px 3px 0 0',
      content: '""',
      display: 'block',
      height: '9px',
      position: 'absolute',
      top: '6px',
      width: '9px',
    },
  '.react-datepicker__sr-only': {
    position: 'absolute',
    width: '1px',
    height: '1px',
    padding: 0,
    margin: '-1px',
    overflow: 'hidden',
    clip: 'rect(0, 0, 0, 0)',
    whiteSpace: 'nowrap',
    border: 0,
  },
  '.react-datepicker-wrapper': {
    display: 'inline-block',
    padding: 0,
    border: 0,
  },
  '.react-datepicker': {
    fontFamily: '"Open Sans", sans-serif',
    fontSize: '0.8rem',
    backgroundColor: '#fff',
    color: '#000',
    border: '1px solid #aeaeae',
    borderRadius: '0.3rem',
    display: 'inline-block',
    position: 'relative',
    lineHeight: 'initial',
  },
  '.react-datepicker--time-only .react-datepicker__time-container': {
    borderLeft: 0,
  },
  '.react-datepicker--time-only .react-datepicker__time, .react-datepicker--time-only .react-datepicker__time-box':
    {
      borderBottomLeftRadius: '0.3rem',
      borderBottomRightRadius: '0.3rem',
    },
  '.react-datepicker-popper': {
    zIndex: 1,
    lineHeight: 0,
  },
  '.react-datepicker-popper .react-datepicker__triangle': {
    stroke: '#aeaeae',
  },
  '.react-datepicker-popper[data-placement^="bottom"] .react-datepicker__triangle':
    {
      fill: '#f0f0f0',
      color: '#f0f0f0',
    },
  '.react-datepicker-popper[data-placement^="top"] .react-datepicker__triangle':
    {
      fill: '#fff',
      color: '#fff',
    },
  '.react-datepicker__header': {
    textAlign: 'center',
    backgroundColor: '#f0f0f0',
    borderTopLeftRadius: '0.3rem',
    padding: '8px 0',
    position: 'relative',
  },
  '.react-datepicker__header--time': {
    paddingBottom: '8px',
    paddingLeft: '5px',
    paddingRight: '5px',
  },
  '.react-datepicker__header--time:not(.react-datepicker__header--time--only)':
    {
      borderTopLeftRadius: 0,
    },
  '.react-datepicker__header:not(.react-datepicker__header--has-time-select)': {
    borderTopRightRadius: '0.3rem',
  },
  '.react-datepicker__year-dropdown-container--select, .react-datepicker__month-dropdown-container--select, .react-datepicker__month-year-dropdown-container--select, .react-datepicker__year-dropdown-container--scroll, .react-datepicker__month-dropdown-container--scroll, .react-datepicker__month-year-dropdown-container--scroll':
    {
      display: 'inline-block',
      margin: '0 15px',
    },
  '.react-datepicker__current-month, .react-datepicker-time__header, .react-datepicker-year-header':
    {
      marginTop: 0,
      color: '#000',
      fontWeight: 'bold',
      fontSize: '0.944rem',
    },
  'h2.react-datepicker__current-month': {
    padding: 0,
    margin: 0,
  },
  '.react-datepicker-time__header': {
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
    overflow: 'hidden',
  },
  '.react-datepicker__navigation': {
    alignItems: 'center',
    background: 'none',
    display: 'flex',
    justifyContent: 'center',
    textAlign: 'center',
    cursor: 'pointer',
    position: 'absolute',
    top: '2px',
    padding: 0,
    border: 'none',
    zIndex: 1,
    height: '32px',
    width: '32px',
    textIndent: '-999em',
    overflow: 'hidden',
  },
  '.react-datepicker__navigation--previous': {
    left: '2px',
  },
  '.react-datepicker__navigation--next': {
    right: '2px',
  },
  '.react-datepicker__navigation--next--with-time:not(.react-datepicker__navigation--next--with-today-button)':
    {
      right: '85px',
    },
  '.react-datepicker__navigation--years': {
    position: 'relative',
    top: 0,
    display: 'block',
    marginLeft: 'auto',
    marginRight: 'auto',
  },
  '.react-datepicker__navigation--years-previous': {
    top: '4px',
  },
  '.react-datepicker__navigation--years-upcoming': {
    top: '-4px',
  },
  '.react-datepicker__navigation:hover *::before': {
    borderColor: 'rgb(165.75, 165.75, 165.75)',
  },
  '.react-datepicker__navigation-icon': {
    position: 'relative',
    top: '-1px',
    fontSize: '20px',
    width: 0,
  },
  '.react-datepicker__navigation-icon--next': {
    left: '-2px',
  },
  '.react-datepicker__navigation-icon--next::before': {
    transform: 'rotate(45deg)',
    left: '-7px',
  },
  '.react-datepicker__navigation-icon--previous': {
    right: '-2px',
  },
  '.react-datepicker__navigation-icon--previous::before': {
    transform: 'rotate(225deg)',
    right: '-7px',
  },
  '.react-datepicker__month-container': {
    float: 'left',
  },
  '.react-datepicker__year': {
    margin: '0.4rem',
    textAlign: 'center',
  },
  '.react-datepicker__year-wrapper': {
    display: 'flex',
    flexWrap: 'wrap',
    maxWidth: '180px',
  },
  '.react-datepicker__year .react-datepicker__year-text': {
    display: 'inline-block',
    width: '4rem',
    margin: '2px',
  },
  '.react-datepicker__month': {
    margin: '0.4rem',
    textAlign: 'center',
  },
  '.react-datepicker__month .react-datepicker__month-text, .react-datepicker__month .react-datepicker__quarter-text':
    {
      display: 'inline-block',
      width: '4rem',
      margin: '2px',
    },
  '.react-datepicker__input-time-container': {
    clear: 'both',
    width: '100%',
    float: 'left',
    margin: '5px 0 10px 15px',
    textAlign: 'left',
  },
  '.react-datepicker__input-time-container .react-datepicker-time__caption': {
    display: 'inline-block',
  },
  '.react-datepicker__input-time-container .react-datepicker-time__input-container':
    {
      display: 'inline-block',
    },
  '.react-datepicker__input-time-container .react-datepicker-time__input-container .react-datepicker-time__input':
    {
      display: 'inline-block',
      marginLeft: '10px',
    },
  '.react-datepicker__input-time-container .react-datepicker-time__input-container .react-datepicker-time__input input':
    {
      width: 'auto',
    },
  '.react-datepicker__input-time-container .react-datepicker-time__input-container .react-datepicker-time__input input[type="time"]::-webkit-inner-spin-button, .react-datepicker__input-time-container .react-datepicker-time__input-container .react-datepicker-time__input input[type="time"]::-webkit-outer-spin-button':
    {
      WebkitAppearance: 'none',
      margin: 0,
    },
  '.react-datepicker__input-time-container .react-datepicker-time__input-container .react-datepicker-time__input input[type="time"]':
    {
      MozAppearance: 'textfield',
    },
  '.react-datepicker__input-time-container .react-datepicker-time__input-container .react-datepicker-time__delimiter':
    {
      marginLeft: '5px',
      display: 'inline-block',
    },
  '.react-datepicker__time-container': {
    float: 'right',
    borderLeft: '1px solid #aeaeae',
    width: '85px',
  },
  '.react-datepicker__time-container--with-today-button': {
    display: 'inline',
    border: '1px solid #aeaeae',
    borderRadius: '0.3rem',
    position: 'absolute',
    right: '-87px',
    top: 0,
  },
  '.react-datepicker__time-container .react-datepicker__time': {
    position: 'relative',
    background: 'white',
    borderBottomRightRadius: '0.3rem',
  },
  '.react-datepicker__time-container .react-datepicker__time .react-datepicker__time-box':
    {
      width: '85px',
      overflowX: 'hidden',
      margin: '0 auto',
      textAlign: 'center',
      borderBottomRightRadius: '0.3rem',
    },
  '.react-datepicker__time-container .react-datepicker__time .react-datepicker__time-box ul.react-datepicker__time-list':
    {
      listStyle: 'none',
      margin: 0,
      height: 'calc(195px + 1.7rem / 2)',
      overflowY: 'scroll',
      paddingRight: 0,
      paddingLeft: 0,
      width: '100%',
      boxSizing: 'content-box',
    },
  '.react-datepicker__time-container .react-datepicker__time .react-datepicker__time-box ul.react-datepicker__time-list li.react-datepicker__time-list-item':
    {
      height: '30px',
      padding: '5px 10px',
      whiteSpace: 'nowrap',
    },
  '.react-datepicker__time-container .react-datepicker__time .react-datepicker__time-box ul.react-datepicker__time-list li.react-datepicker__time-list-item:hover':
    {
      cursor: 'pointer',
      backgroundColor: '#f0f0f0',
    },
  '.react-datepicker__time-container .react-datepicker__time .react-datepicker__time-box ul.react-datepicker__time-list li.react-datepicker__time-list-item--selected':
    {
      backgroundColor: '#216ba5',
      color: 'white',
      fontWeight: 'bold',
    },
  '.react-datepicker__time-container .react-datepicker__time .react-datepicker__time-box ul.react-datepicker__time-list li.react-datepicker__time-list-item--selected:hover':
    {
      backgroundColor: '#216ba5',
    },
  '.react-datepicker__time-container .react-datepicker__time .react-datepicker__time-box ul.react-datepicker__time-list li.react-datepicker__time-list-item--disabled':
    {
      color: '#ccc',
    },
  '.react-datepicker__time-container .react-datepicker__time .react-datepicker__time-box ul.react-datepicker__time-list li.react-datepicker__time-list-item--disabled:hover':
    {
      cursor: 'default',
      backgroundColor: 'transparent',
    },
  '.react-datepicker__week-number': {
    color: '#ccc',
    display: 'inline-block',
    width: '1.7rem',
    lineHeight: '1.7rem',
    textAlign: 'center',
    margin: '0.166rem',
  },
  '.react-datepicker__week-number.react-datepicker__week-number--clickable': {
    cursor: 'pointer',
  },
  '.react-datepicker__week-number.react-datepicker__week-number--clickable:not(.react-datepicker__week-number--selected):hover':
    {
      borderRadius: '0.3rem',
      backgroundColor: '#f0f0f0',
    },
  '.react-datepicker__week-number--selected': {
    borderRadius: '0.3rem',
    backgroundColor: '#216ba5',
    color: '#fff',
  },
  '.react-datepicker__week-number--selected:hover': {
    backgroundColor: 'rgb(28.75, 93.2196969697, 143.75)',
  },
  '.react-datepicker__day-names': {
    whiteSpace: 'nowrap',
    marginBottom: '-8px',
  },
  '.react-datepicker__week': {
    whiteSpace: 'nowrap',
  },
  '.react-datepicker__day-name, .react-datepicker__day, .react-datepicker__time-name':
    {
      color: '#000',
      display: 'inline-block',
      width: '1.7rem',
      lineHeight: '1.7rem',
      textAlign: 'center',
      margin: '0.166rem',
    },
  '.react-datepicker__day, .react-datepicker__month-text, .react-datepicker__quarter-text, .react-datepicker__year-text':
    {
      cursor: 'pointer',
    },
  '.react-datepicker__day:not([aria-disabled="true"]):hover, .react-datepicker__month-text:not([aria-disabled="true"]):hover, .react-datepicker__quarter-text:not([aria-disabled="true"]):hover, .react-datepicker__year-text:not([aria-disabled="true"]):hover':
    {
      borderRadius: '0.3rem',
      backgroundColor: '#f0f0f0',
    },
  '.react-datepicker__day--today, .react-datepicker__month-text--today, .react-datepicker__quarter-text--today, .react-datepicker__year-text--today':
    {
      fontWeight: 'bold',
    },
  '.react-datepicker__day--highlighted, .react-datepicker__month-text--highlighted, .react-datepicker__quarter-text--highlighted, .react-datepicker__year-text--highlighted':
    {
      borderRadius: '0.3rem',
      backgroundColor: '#3dcc4a',
      color: '#fff',
    },
  '.react-datepicker__day--highlighted:not([aria-disabled="true"]):hover, .react-datepicker__month-text--highlighted:not([aria-disabled="true"]):hover, .react-datepicker__quarter-text--highlighted:not([aria-disabled="true"]):hover, .react-datepicker__year-text--highlighted:not([aria-disabled="true"]):hover':
    {
      backgroundColor: 'rgb(49.8551020408, 189.6448979592, 62.5632653061)',
    },
  '.react-datepicker__day--highlighted-custom-1, .react-datepicker__month-text--highlighted-custom-1, .react-datepicker__quarter-text--highlighted-custom-1, .react-datepicker__year-text--highlighted-custom-1':
    {
      color: 'magenta',
    },
  '.react-datepicker__day--highlighted-custom-2, .react-datepicker__month-text--highlighted-custom-2, .react-datepicker__quarter-text--highlighted-custom-2, .react-datepicker__year-text--highlighted-custom-2':
    {
      color: 'green',
    },
  '.react-datepicker__day--holidays, .react-datepicker__month-text--holidays, .react-datepicker__quarter-text--holidays, .react-datepicker__year-text--holidays':
    {
      position: 'relative',
      borderRadius: '0.3rem',
      backgroundColor: '#ff6803',
      color: '#fff',
    },
  '.react-datepicker__day--holidays .overlay, .react-datepicker__month-text--holidays .overlay, .react-datepicker__quarter-text--holidays .overlay, .react-datepicker__year-text--holidays .overlay':
    {
      position: 'absolute',
      bottom: '100%',
      left: '50%',
      transform: 'translateX(-50%)',
      backgroundColor: '#333',
      color: '#fff',
      padding: '4px',
      borderRadius: '4px',
      whiteSpace: 'nowrap',
      visibility: 'hidden',
      opacity: 0,
      transition: 'visibility 0s, opacity 0.3s ease-in-out',
    },
  '.react-datepicker__day--holidays:not([aria-disabled="true"]):hover, .react-datepicker__month-text--holidays:not([aria-disabled="true"]):hover, .react-datepicker__quarter-text--holidays:not([aria-disabled="true"]):hover, .react-datepicker__year-text--holidays:not([aria-disabled="true"]):hover':
    {
      backgroundColor: 'rgb(207, 82.9642857143, 0)',
    },
  '.react-datepicker__day--holidays:hover .overlay, .react-datepicker__month-text--holidays:hover .overlay, .react-datepicker__quarter-text--holidays:hover .overlay, .react-datepicker__year-text--holidays:hover .overlay':
    {
      visibility: 'visible',
      opacity: 1,
    },
  '.react-datepicker__day--selected, .react-datepicker__day--in-selecting-range, .react-datepicker__day--in-range, .react-datepicker__month-text--selected, .react-datepicker__month-text--in-selecting-range, .react-datepicker__month-text--in-range, .react-datepicker__quarter-text--selected, .react-datepicker__quarter-text--in-selecting-range, .react-datepicker__quarter-text--in-range, .react-datepicker__year-text--selected, .react-datepicker__year-text--in-selecting-range, .react-datepicker__year-text--in-range':
    {
      borderRadius: '0.3rem',
      backgroundColor: '#216ba5',
      color: '#fff',
    },
  '.react-datepicker__day--selected:not([aria-disabled="true"]):hover, .react-datepicker__day--in-selecting-range:not([aria-disabled="true"]):hover, .react-datepicker__day--in-range:not([aria-disabled="true"]):hover, .react-datepicker__month-text--selected:not([aria-disabled="true"]):hover, .react-datepicker__month-text--in-selecting-range:not([aria-disabled="true"]):hover, .react-datepicker__month-text--in-range:not([aria-disabled="true"]):hover, .react-datepicker__quarter-text--selected:not([aria-disabled="true"]):hover, .react-datepicker__quarter-text--in-selecting-range:not([aria-disabled="true"]):hover, .react-datepicker__quarter-text--in-range:not([aria-disabled="true"]):hover, .react-datepicker__year-text--selected:not([aria-disabled="true"]):hover, .react-datepicker__year-text--in-selecting-range:not([aria-disabled="true"]):hover, .react-datepicker__year-text--in-range:not([aria-disabled="true"]):hover':
    {
      backgroundColor: 'rgb(28.75, 93.2196969697, 143.75)',
    },
  '.react-datepicker__day--keyboard-selected, .react-datepicker__month-text--keyboard-selected, .react-datepicker__quarter-text--keyboard-selected, .react-datepicker__year-text--keyboard-selected':
    {
      borderRadius: '0.3rem',
      backgroundColor: 'rgb(186.25, 217.0833333333, 241.25)',
      color: 'rgb(0, 0, 0)',
    },
  '.react-datepicker__day--keyboard-selected:not([aria-disabled="true"]):hover, .react-datepicker__month-text--keyboard-selected:not([aria-disabled="true"]):hover, .react-datepicker__quarter-text--keyboard-selected:not([aria-disabled="true"]):hover, .react-datepicker__year-text--keyboard-selected:not([aria-disabled="true"]):hover':
    {
      backgroundColor: 'rgb(28.75, 93.2196969697, 143.75)',
    },
  '.react-datepicker__day--in-selecting-range:not(.react-datepicker__day--in-range, .react-datepicker__month-text--in-range, .react-datepicker__quarter-text--in-range, .react-datepicker__year-text--in-range), .react-datepicker__month-text--in-selecting-range:not(.react-datepicker__day--in-range, .react-datepicker__month-text--in-range, .react-datepicker__quarter-text--in-range, .react-datepicker__year-text--in-range), .react-datepicker__quarter-text--in-selecting-range:not(.react-datepicker__day--in-range, .react-datepicker__month-text--in-range, .react-datepicker__quarter-text--in-range, .react-datepicker__year-text--in-range), .react-datepicker__year-text--in-selecting-range:not(.react-datepicker__day--in-range, .react-datepicker__month-text--in-range, .react-datepicker__quarter-text--in-range, .react-datepicker__year-text--in-range)':
    {
      backgroundColor: 'rgba(33, 107, 165, 0.5)',
    },
  '.react-datepicker__month--selecting-range .react-datepicker__day--in-range:not(.react-datepicker__day--in-selecting-range, .react-datepicker__month-text--in-selecting-range, .react-datepicker__quarter-text--in-selecting-range, .react-datepicker__year-text--in-selecting-range), .react-datepicker__year--selecting-range .react-datepicker__day--in-range:not(.react-datepicker__day--in-selecting-range, .react-datepicker__month-text--in-selecting-range, .react-datepicker__quarter-text--in-selecting-range, .react-datepicker__year-text--in-selecting-range), .react-datepicker__month--selecting-range .react-datepicker__month-text--in-range:not(.react-datepicker__day--in-selecting-range, .react-datepicker__month-text--in-selecting-range, .react-datepicker__quarter-text--in-selecting-range, .react-datepicker__year-text--in-selecting-range), .react-datepicker__year--selecting-range .react-datepicker__month-text--in-range:not(.react-datepicker__day--in-selecting-range, .react-datepicker__month-text--in-selecting-range, .react-datepicker__quarter-text--in-selecting-range, .react-datepicker__year-text--in-selecting-range), .react-datepicker__month--selecting-range .react-datepicker__quarter-text--in-range:not(.react-datepicker__day--in-selecting-range, .react-datepicker__month-text--in-selecting-range, .react-datepicker__quarter-text--in-selecting-range, .react-datepicker__year-text--in-selecting-range), .react-datepicker__year--selecting-range .react-datepicker__quarter-text--in-range:not(.react-datepicker__day--in-selecting-range, .react-datepicker__month-text--in-selecting-range, .react-datepicker__quarter-text--in-selecting-range, .react-datepicker__year-text--in-selecting-range), .react-datepicker__month--selecting-range .react-datepicker__year-text--in-range:not(.react-datepicker__day--in-selecting-range, .react-datepicker__month-text--in-selecting-range, .react-datepicker__quarter-text--in-selecting-range, .react-datepicker__year-text--in-selecting-range), .react-datepicker__year--selecting-range .react-datepicker__year-text--in-range:not(.react-datepicker__day--in-selecting-range, .react-datepicker__month-text--in-selecting-range, .react-datepicker__quarter-text--in-selecting-range, .react-datepicker__year-text--in-selecting-range)':
    {
      backgroundColor: '#f0f0f0',
      color: '#000',
    },
  '.react-datepicker__day--disabled, .react-datepicker__month-text--disabled, .react-datepicker__quarter-text--disabled, .react-datepicker__year-text--disabled':
    {
      cursor: 'default',
      color: '#ccc',
    },
  '.react-datepicker__day--disabled .overlay, .react-datepicker__month-text--disabled .overlay, .react-datepicker__quarter-text--disabled .overlay, .react-datepicker__year-text--disabled .overlay':
    {
      position: 'absolute',
      bottom: '70%',
      left: '50%',
      transform: 'translateX(-50%)',
      backgroundColor: '#333',
      color: '#fff',
      padding: '4px',
      borderRadius: '4px',
      whiteSpace: 'nowrap',
      visibility: 'hidden',
      opacity: 0,
      transition: 'visibility 0s, opacity 0.3s ease-in-out',
    },
  '.react-datepicker__input-container': {
    position: 'relative',
    display: 'inline-block',
    width: '100%',
  },
  '.react-datepicker__input-container .react-datepicker__calendar-icon': {
    position: 'absolute',
    padding: '0.5rem',
    boxSizing: 'content-box',
  },
  '.react-datepicker__view-calendar-icon input': {
    padding: '6px 10px 5px 25px',
  },
  '.react-datepicker__year-read-view, .react-datepicker__month-read-view, .react-datepicker__month-year-read-view':
    {
      border: '1px solid transparent',
      borderRadius: '0.3rem',
      position: 'relative',
    },
  '.react-datepicker__year-read-view:hover, .react-datepicker__month-read-view:hover, .react-datepicker__month-year-read-view:hover':
    {
      cursor: 'pointer',
    },
  '.react-datepicker__year-read-view:hover .react-datepicker__year-read-view--down-arrow, .react-datepicker__year-read-view:hover .react-datepicker__month-read-view--down-arrow, .react-datepicker__month-read-view:hover .react-datepicker__year-read-view--down-arrow, .react-datepicker__month-read-view:hover .react-datepicker__month-read-view--down-arrow, .react-datepicker__month-year-read-view:hover .react-datepicker__year-read-view--down-arrow, .react-datepicker__month-year-read-view:hover .react-datepicker__month-read-view--down-arrow':
    {
      borderTopColor: 'rgb(178.5, 178.5, 178.5)',
    },
  '.react-datepicker__year-read-view--down-arrow, .react-datepicker__month-read-view--down-arrow, .react-datepicker__month-year-read-view--down-arrow':
    {
      transform: 'rotate(135deg)',
      right: '-16px',
      top: 0,
    },
  '.react-datepicker__year-dropdown, .react-datepicker__month-dropdown, .react-datepicker__month-year-dropdown':
    {
      backgroundColor: '#f0f0f0',
      position: 'absolute',
      width: '50%',
      left: '25%',
      top: '30px',
      zIndex: 1,
      textAlign: 'center',
      borderRadius: '0.3rem',
      border: '1px solid #aeaeae',
    },
  '.react-datepicker__year-dropdown:hover, .react-datepicker__month-dropdown:hover, .react-datepicker__month-year-dropdown:hover':
    {
      cursor: 'pointer',
    },
  '.react-datepicker__year-dropdown--scrollable, .react-datepicker__month-dropdown--scrollable, .react-datepicker__month-year-dropdown--scrollable':
    {
      height: '150px',
      overflowY: 'scroll',
    },
  '.react-datepicker__year-option, .react-datepicker__month-option, .react-datepicker__month-year-option':
    {
      lineHeight: '20px',
      width: '100%',
      display: 'block',
      marginLeft: 'auto',
      marginRight: 'auto',
    },
  '.react-datepicker__year-option:first-of-type, .react-datepicker__month-option:first-of-type, .react-datepicker__month-year-option:first-of-type':
    {
      borderTopLeftRadius: '0.3rem',
      borderTopRightRadius: '0.3rem',
    },
  '.react-datepicker__year-option:last-of-type, .react-datepicker__month-option:last-of-type, .react-datepicker__month-year-option:last-of-type':
    {
      WebkitUserSelect: 'none',
      MozUserSelect: 'none',
      msUserSelect: 'none',
      userSelect: 'none',
      borderBottomLeftRadius: '0.3rem',
      borderBottomRightRadius: '0.3rem',
    },
  '.react-datepicker__year-option:hover, .react-datepicker__month-option:hover, .react-datepicker__month-year-option:hover':
    {
      backgroundColor: '#ccc',
    },
  '.react-datepicker__year-option:hover .react-datepicker__navigation--years-upcoming, .react-datepicker__month-option:hover .react-datepicker__navigation--years-upcoming, .react-datepicker__month-year-option:hover .react-datepicker__navigation--years-upcoming':
    {
      borderBottomColor: 'rgb(178.5, 178.5, 178.5)',
    },
  '.react-datepicker__year-option:hover .react-datepicker__navigation--years-previous, .react-datepicker__month-option:hover .react-datepicker__navigation--years-previous, .react-datepicker__month-year-option:hover .react-datepicker__navigation--years-previous':
    {
      borderTopColor: 'rgb(178.5, 178.5, 178.5)',
    },
  '.react-datepicker__year-option--selected, .react-datepicker__month-option--selected, .react-datepicker__month-year-option--selected':
    {
      position: 'absolute',
      left: '15px',
    },
  '.react-datepicker__close-icon': {
    cursor: 'pointer',
    backgroundColor: 'transparent',
    border: 0,
    outline: 0,
    padding: '0 6px 0 0',
    position: 'absolute',
    top: 0,
    right: 0,
    height: '100%',
    display: 'table-cell',
    verticalAlign: 'middle',
  },
  '.react-datepicker__close-icon::after': {
    cursor: 'pointer',
    backgroundColor: '#216ba5',
    color: '#fff',
    borderRadius: '50%',
    height: '16px',
    width: '16px',
    padding: '2px',
    fontSize: '12px',
    lineHeight: 1,
    textAlign: 'center',
    display: 'table-cell',
    verticalAlign: 'middle',
    content: '"×"',
  },
  '.react-datepicker__close-icon--disabled': {
    cursor: 'default',
  },
  '.react-datepicker__close-icon--disabled::after': {
    cursor: 'default',
    backgroundColor: '#ccc',
  },
  '.react-datepicker__today-button': {
    background: '#f0f0f0',
    borderTop: '1px solid #aeaeae',
    cursor: 'pointer',
    textAlign: 'center',
    fontWeight: 'bold',
    padding: '5px 0',
    clear: 'left',
  },
  '.react-datepicker__portal': {
    position: 'fixed',
    width: '100vw',
    height: '100vh',
    backgroundColor: 'rgba(0, 0, 0, 0.8)',
    left: 0,
    top: 0,
    justifyContent: 'center',
    alignItems: 'center',
    display: 'flex',
    zIndex: 2147483647,
  },
  '.react-datepicker__portal .react-datepicker__day-name, .react-datepicker__portal .react-datepicker__day, .react-datepicker__portal .react-datepicker__time-name':
    {
      width: '3rem',
      lineHeight: '3rem',
    },
  '@media (max-width: 400px), (max-height: 550px)': {
    '.react-datepicker__portal .react-datepicker__day-name, .react-datepicker__portal .react-datepicker__day, .react-datepicker__portal .react-datepicker__time-name':
      {
        width: '2rem',
        lineHeight: '2rem',
      },
  },
  '.react-datepicker__portal .react-datepicker__current-month, .react-datepicker__portal .react-datepicker-time__header':
    {
      fontSize: '1.44rem',
    },
  '.react-datepicker__children-container': {
    width: '13.8rem',
    margin: '0.4rem',
    paddingRight: '0.2rem',
    paddingLeft: '0.2rem',
    height: 'auto',
  },
  '.react-datepicker__aria-live': {
    position: 'absolute',
    clipPath: 'circle(0)',
    border: 0,
    height: '1px',
    margin: '-1px',
    overflow: 'hidden',
    padding: 0,
    width: '1px',
    whiteSpace: 'nowrap',
  },
  '.react-datepicker__calendar-icon': {
    width: '1em',
    height: '1em',
    verticalAlign: '-0.125em',
  },
};

const CalendarStyles = styled(Box)(({ theme }) => ({
  ...datePickerStyles,
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
      <OphInput
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
