import { reactRouter } from '@react-router/dev/vite';
import { defineConfig } from 'vite';
import { vitePluginOptimizeNamedImports } from './vitePluginOptimizeNamedImports.ts';

export default defineConfig({
  plugins: [
    reactRouter(),
    vitePluginOptimizeNamedImports(['@mui/icons-material']),
  ],
  resolve: {
    tsconfigPaths: true,
  },
  optimizeDeps: {
    entries: ['@tolgee/format-icu'],
  },
  build: {
    // Jotta toimii myös Spring Bootissa, assetit täytyy noutaa /suorituspalvelu-polun alta eikä juuresta.
    assetsDir: 'suorituspalvelu/assets',
  },
  server: {
    port: 3000,
    host: 'localhost',
  },
});
