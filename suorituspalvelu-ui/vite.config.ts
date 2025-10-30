import { reactRouter } from '@react-router/dev/vite';
import tsConfigPaths from 'vite-tsconfig-paths';
import { defineConfig } from 'vite';
import { vitePluginOptimizeNamedImports } from './vitePluginOptimizeNamedImports';

export default defineConfig({
  plugins: [
    reactRouter(),
    tsConfigPaths(),
    vitePluginOptimizeNamedImports(['@mui/icons-material']),
  ],
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
