import { createCanvas, loadImage } from "@napi-rs/canvas";
import Redis from "ioredis";
import { mkdir, writeFile } from "node:fs/promises";
import { dirname } from "node:path";

const GRID = 5;
const CELL_WIDTH = 260;
const CELL_HEIGHT = CELL_WIDTH;
const GAP = 12;
const PADDING = 32;
const TITLE_FONT_SIZE = 24;
const BODY_FONT_SIZE = 24;

const ZERO_UUID = "00000000-0000-0000-0000-000000000000";

const squareNames = [
  "Consejero",
  "ABABAB",
  "Explosion",
  "###@###",
  "True Faith",
  "Hall of Famer",
  "Under Pressure",
  "W.W.C.D.",
  "Take Backsies",
  "Streaking",
  "Dynamic Duo",
  "ax^2+bx+c",
  "C.B.T.",
  "Effigy",
  "Hot Potato",
  "True Contributor",
  "Cheerleading",
  "Invisible Suffering",
  "Mogged",
  "Streaking",
  "To Whom?",
  "That Worked?",
  "Dying for Pie",
  "Let me down gently",
  "Crowd goes wild",
];

function indexToId(index: number): string {
  const col = String.fromCharCode("A".charCodeAt(0) + Math.floor(index / GRID));
  const row = (index % GRID) + 1;
  return `${col}${row}`;
}

function shorten(value: string, maxLen: number): string {
  if (value.length <= maxLen) return value;
  return `${value.slice(0, maxLen - 1)}…`;
}

function fitToken(ctx: any, token: string, maxWidth: number): string[] {
  if (ctx.measureText(token).width <= maxWidth) return [token];
  const out: string[] = [];
  let current = "";
  for (const ch of token) {
    const next = `${current}${ch}`;
    if (current && ctx.measureText(next).width > maxWidth) {
      out.push(current);
      current = ch;
    } else {
      current = next;
    }
  }
  if (current) out.push(current);
  return out;
}

function wrappedLines(ctx: any, text: string, maxWidth: number, maxLines: number): string[] {
  const tokens = text
    .split(" ")
    .flatMap((token) => fitToken(ctx, token, maxWidth));

  const lines: string[] = [];
  let current = "";
  for (const token of tokens) {
    const candidate = current ? `${current} ${token}` : token;
    if (ctx.measureText(candidate).width <= maxWidth) {
      current = candidate;
      continue;
    }
    if (current) lines.push(current);
    current = token;
    if (lines.length >= maxLines) break;
  }
  if (current && lines.length < maxLines) lines.push(current);

  return lines;
}

function drawWrappedText(
  ctx: any,
  text: string,
  x: number,
  y: number,
  maxWidth: number,
  lineHeight: number,
  maxLines: number,
): number {
  const lines = wrappedLines(ctx, text, maxWidth, maxLines);

  let lineY = y;
  for (let i = 0; i < lines.length; i++) {
    ctx.fillText(lines[i], x, lineY);
    lineY += lineHeight;
  }
  return lineY;
}

function hangmanMask(value: string): string {
  return value.replaceAll(/[^ ]/g, "?");
}

type SquareState = {
  index: number;
  id: string;
  name: string;
  unlocked: boolean;
  nameRevealed: boolean;
  completions: number;
  discovered: string | null;
};

async function resolveName(uuid: string, cache: Map<string, string>): Promise<string> {
  if (uuid === ZERO_UUID) return "Several players";
  const cached = cache.get(uuid);
  if (cached) return cached;
  try {
    const clean = uuid.replaceAll("-", "");
    const response = await fetch(`https://sessionserver.mojang.com/session/minecraft/profile/${clean}`);
    if (response.ok) {
      const json = (await response.json()) as { name?: string };
      if (json.name) {
        cache.set(uuid, json.name);
        return json.name;
      }
    }
  } catch {
    // ignore
  }
  const fallback = uuid.slice(0, 8);
  cache.set(uuid, fallback);
  return fallback;
}

async function resolveFace(uuid: string, cache: Map<string, Awaited<ReturnType<typeof loadImage>> | null>) {
  if (uuid === ZERO_UUID) return null;
  if (cache.has(uuid)) return cache.get(uuid) ?? null;
  try {
    const response = await fetch(`https://nmsr.nickac.dev/face/${uuid}`);
    if (!response.ok) {
      cache.set(uuid, null);
      return null;
    }
    const bytes = Buffer.from(await response.arrayBuffer());
    const image = await loadImage(bytes);
    cache.set(uuid, image);
    return image;
  } catch {
    cache.set(uuid, null);
    return null;
  }
}

async function main() {
  const redisUrl = process.argv[2] ?? "redis://localhost:6379";
  const outputPath = process.argv[3] ?? "bingo-board.png";

  const redis = new Redis(redisUrl, { lazyConnect: true, maxRetriesPerRequest: 1 });
  const now = Date.now();
  const states: SquareState[] = [];

  try {
    await redis.connect();

    for (let index = 0; index < squareNames.length; index++) {
      const id = indexToId(index);
      const keyPrefix = `bingo:${id}`;
      const [unlockedByRaw, nameByRaw, discoveredRaw, completionsRaw] = await Promise.all([
        redis.get(`${keyPrefix}-unlocked`),
        redis.get(`${keyPrefix}-name`),
        redis.get(`${keyPrefix}-discovered`),
        redis.llen(`${keyPrefix}-completions`),
      ]);

      const unlockedBy = Number(unlockedByRaw || 0);
      const nameBy = Number(nameByRaw || 0);
      const completions = Number(completionsRaw || 0);

      states.push({
        index,
        id,
        name: squareNames[index],
        unlocked: unlockedBy !== 0 && now >= unlockedBy,
        nameRevealed: (nameBy !== 0 && now >= nameBy) || completions >= 5,
        completions,
        discovered: discoveredRaw,
      });
    }
  } finally {
    redis.disconnect();
  }

  const width = PADDING * 2 + GRID * CELL_WIDTH + (GRID - 1) * GAP;
  const height = PADDING * 2 + GRID * CELL_HEIGHT + (GRID - 1) * GAP;
  const canvas = createCanvas(width, height);
  const ctx = canvas.getContext("2d");

  ctx.fillStyle = "#0f1218";
  ctx.fillRect(0, 0, width, height);

  const nameCache = new Map<string, string>();
  const faceCache = new Map<string, Awaited<ReturnType<typeof loadImage>> | null>();

  for (const state of states) {
    const col = state.index % GRID;
    const row = Math.floor(state.index / GRID);
    const x = PADDING + col * (CELL_WIDTH + GAP);
    const y = PADDING + row * (CELL_HEIGHT + GAP);

    ctx.fillStyle = !state.unlocked ? "#20262f" : state.discovered ? "#1e3340" : "#242e22";
    ctx.fillRect(x, y, CELL_WIDTH, CELL_HEIGHT);
    ctx.strokeStyle = state.discovered ? "#5aa8d8" : "#495466";
    ctx.lineWidth = 2;
    ctx.strokeRect(x, y, CELL_WIDTH, CELL_HEIGHT);

    const safeName = state.nameRevealed ? state.name : hangmanMask(state.name);

    let lineY = y + 30;
    ctx.font = `700 ${TITLE_FONT_SIZE}px Maple Mono, monospace`;
    ctx.fillStyle = "#e7effa";
    lineY = drawWrappedText(ctx, `${state.id} ${safeName}`, x + 10, lineY, CELL_WIDTH - 20, 24, 3);

    ctx.font = `${BODY_FONT_SIZE}px Maple Mono, monospace`;
    ctx.fillStyle = "#bcc5d3";
    if (state.completions > 0) {
      lineY += 4;
      lineY = drawWrappedText(ctx, `Completions: ${state.completions}`, x + 10, lineY, CELL_WIDTH - 20, 20, 2);
    }

    if (state.discovered) {
      const discoveredName = await resolveName(state.discovered, nameCache);
      const faceX = x + 10;
      const faceY = y + CELL_HEIGHT - 58;
      let nameX = x + 10;
      if (state.discovered !== ZERO_UUID) {
        const face = await resolveFace(state.discovered, faceCache);
        if (face) {
          ctx.drawImage(face, faceX, faceY, 48, 48);
          nameX = faceX + 58;
        }
      }

      ctx.font = `${BODY_FONT_SIZE}px Maple Mono, monospace`;
      ctx.fillStyle = "#bcc5d3";
      const maxWidth = CELL_WIDTH - (nameX - x) - 10;
      const lines = wrappedLines(ctx, shorten(discoveredName, 28), maxWidth, 2);
      let bottomY = y + CELL_HEIGHT - 10;
      for (let i = lines.length - 1; i >= 0; i--) {
        ctx.fillText(lines[i], nameX, bottomY);
        bottomY -= 18;
      }
    }
  }

  await mkdir(dirname(outputPath), { recursive: true });
  const png = await canvas.encode("png");
  await writeFile(outputPath, png);
  console.log(`Rendered bingo board to ${outputPath}`);
}

await main();
