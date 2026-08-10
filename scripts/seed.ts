/**
 * Spring LMS demo seed runner (Bun, zero npm deps).
 *
 * Reads scripts/seed.sql, swaps in a freshly generated bcrypt hash for the
 * seed password, then pipes the SQL into the dockerized MySQL via `docker exec`.
 *
 * Usage:
 *   bun scripts/seed.ts
 *
 * Env overrides:
 *   DB_PASSWORD      (default: supersecretpassword)
 *   DB_NAME          (default: lmsdb)
 *   MYSQL_CONTAINER  (default: mysql)
 *   SEED_PASSWORD    (default: Passw0rd!)
 */
import { readFileSync } from "node:fs";
import { join } from "node:path";

const dbPassword = process.env.DB_PASSWORD ?? "supersecretpassword";
const dbName = process.env.DB_NAME ?? "lmsdb";
const container = process.env.MYSQL_CONTAINER ?? "mysql";
const seedPassword = process.env.SEED_PASSWORD ?? "Passw0rd!";

const seedSql = join(import.meta.dir, "seed.sql");
let sql = readFileSync(seedSql, "utf8");

const hash = Bun.password.hashSync(seedPassword, { algorithm: "bcrypt", cost: 12 });

// bcrypt hashes are `$2a$|$2b$|$2y$` + cost + 53-char salt+digest.
const BCryptPattern = /\$2[aby]\$\d+\$[A-Za-z0-9./]{53}/;
const matchCount = sql.match(BCryptPattern)?.length ?? 0;
if (matchCount === 0) {
  console.error(`[seed] ERROR: no bcrypt hash found in ${seedSql} to replace — aborting.`);
  process.exit(1);
}
sql = sql.replace(BCryptPattern, hash);
console.log(`[seed] hash replaced (${matchCount} occurrence(s)), password="${seedPassword}"`);

const proc = Bun.spawn(
  ["docker", "exec", "-i", container, "mysql", "-u", "root", `-p${dbPassword}`, dbName],
  { stdin: "pipe", stdout: "pipe", stderr: "pipe" },
);

const writer = proc.stdin;
writer.write(sql);
await writer.end();

const [stdout, stderr] = await Promise.all([
  new Response(proc.stdout).text(),
  new Response(proc.stderr).text(),
]);
const exitCode = await proc.exited;

if (stdout) console.log(stdout.trimEnd());
if (stderr) console.error(stderr.trimEnd());

if (exitCode === 0) {
  console.log("[seed] done.");
} else {
  console.error(`[seed] ERROR: mysql exited with code ${exitCode}`);
  process.exit(exitCode);
}
