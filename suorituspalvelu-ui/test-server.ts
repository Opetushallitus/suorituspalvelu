import path from 'path';
import http from 'http';
import express from 'express';
import { fileURLToPath } from 'url';
import { BASENAME } from './src/lib/common';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const buildPath = path.join(__dirname, 'build', 'client');

const app = express();

app.use(
  BASENAME + '/assets',
  express.static(path.join(buildPath, 'suorituspalvelu', 'assets')),
);

app.get(['/', BASENAME, `${BASENAME}/*splat`], function (_req, res) {
  res.sendFile(path.join(buildPath, 'index.html'));
});

http.createServer(app).listen(3000);
