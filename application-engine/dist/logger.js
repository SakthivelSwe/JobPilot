"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.logger = void 0;
const winston_1 = __importDefault(require("winston"));
const fs_1 = __importDefault(require("fs"));
const path_1 = __importDefault(require("path"));
const dir = process.env.LOG_DIR || './logs';
if (!fs_1.default.existsSync(dir))
    fs_1.default.mkdirSync(dir, { recursive: true });
exports.logger = winston_1.default.createLogger({
    level: 'info',
    format: winston_1.default.format.combine(winston_1.default.format.timestamp(), winston_1.default.format.printf(({ timestamp, level, message, ...meta }) => `${timestamp} ${level.toUpperCase().padEnd(5)} ${message}` +
        (Object.keys(meta).length ? ` ${JSON.stringify(meta)}` : ''))),
    transports: [
        new winston_1.default.transports.Console(),
        new winston_1.default.transports.File({ filename: path_1.default.join(dir, 'engine.log') }),
    ],
});
//# sourceMappingURL=logger.js.map