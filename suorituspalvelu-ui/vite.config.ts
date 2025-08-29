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
  // Jos buildataan tuotantoa varten, assetit täytyy noutaa /suorituspalvelu-polun alta.
  // dev-serverillä assetit on löytyy juuresta
  ...(process.env.BUILD === 'true'
    ? {
        build: {
          assetsDir: 'suorituspalvelu/assets',
        },
      }
    : {}),
  server: {
    port: 3000,
    host: 'localhost',
  },
});
