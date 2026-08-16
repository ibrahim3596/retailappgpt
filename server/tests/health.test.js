const test = require("node:test");
const assert = require("node:assert/strict");
const http = require("node:http");

process.env.JWT_SECRET = "test-only-jwt-secret-that-is-long-enough";

const { app } = require("../dist/index.js");

test("GET /health returns a healthy server payload", async (t) => {
  const server = http.createServer(app);

  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
  t.after(() => server.close());

  const address = server.address();
  assert.ok(address && typeof address === "object");

  const response = await fetch(`http://127.0.0.1:${address.port}/health`);
  assert.equal(response.status, 200);

  const body = await response.json();
  assert.equal(body.status, "OK");
  assert.equal(body.version, "1.0.0");
  assert.equal(typeof body.timestamp, "string");
});
