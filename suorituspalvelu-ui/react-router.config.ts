import { BASENAME } from './src/lib/common';
import type { Config } from '@react-router/dev/config';

export default {
  appDirectory: 'src/app',
  buildDirectory: 'build',
  basename: BASENAME,
  ssr: false,
} satisfies Config;
