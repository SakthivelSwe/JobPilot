import winston from 'winston';
import fs from 'fs';
import path from 'path';

const dir = process.env.LOG_DIR || './logs';
if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });

export const logger = winston.createLogger({
  level: 'info',
  format: winston.format.combine(
    winston.format.timestamp(),
    winston.format.printf(
      ({ timestamp, level, message, ...meta }) =>
        `${timestamp} ${level.toUpperCase().padEnd(5)} ${message}` +
        (Object.keys(meta).length ? ` ${JSON.stringify(meta)}` : ''),
    ),
  ),
  transports: [
    new winston.transports.Console(),
    new winston.transports.File({ filename: path.join(dir, 'engine.log') }),
  ],
});

