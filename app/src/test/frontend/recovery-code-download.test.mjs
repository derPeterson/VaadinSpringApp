import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { test } from 'node:test';
import vm from 'node:vm';

const source = readFileSync(new URL('../../main/resources/META-INF/resources/custom-theme/recovery-code-download.js', import.meta.url), 'utf8');

function fixture(response) {
  const attributes = new Map([['data-download-url', '/VAADIN/dynamic/resource/own-fixture/']]);
  const button = {
    getAttribute: name => attributes.get(name),
    hasAttribute: name => attributes.has(name),
    setAttribute: (name, value) => attributes.set(name, value),
    removeAttribute: name => attributes.delete(name)
  };
  const clicks = [];
  const requests = [];
  const urls = [];
  const window = { document: {
    body: { append() {} },
    createElement() { return { click() { clicks.push(this.download); }, remove() {} }; }
  } };
  vm.runInNewContext(source, {
    window,
    fetch: async (url, options) => {
      requests.push({ url, options });
      if (response instanceof Error) throw response;
      return response;
    },
    URL: { createObjectURL(blob) { urls.push(blob); return 'blob:own-pdf'; }, revokeObjectURL() {} },
    setTimeout: callback => callback()
  });
  return { button, attributes, clicks, requests, urls, download: window.cosmosRecoveryCodeDownload };
}

function pdfResponse(body = '%PDF-synthetic', contentType = 'application/pdf', status = 200) {
  return {
    ok: status >= 200 && status < 300,
    headers: new Map([['content-type', contentType]]),
    blob: async () => new Blob([body], { type: contentType })
  };
}

for (const status of [403, 500]) {
  test(`HTTP ${status} never offers an error body as a file`, async () => {
    const page = fixture(pdfResponse('Forbidden', 'text/plain', status));
    assert.equal(await page.download(page.button), false);
    assert.equal(page.clicks.length, 0);
    assert.equal(page.urls.length, 0);
    assert.equal(page.attributes.has('aria-busy'), false);
  });
}

test('a login HTML page and a fake PDF do not trigger downloads', async () => {
  for (const response of [pdfResponse('<html>Login</html>', 'text/html'), pdfResponse('Error')]) {
    const page = fixture(response);
    assert.equal(await page.download(page.button), false);
    assert.equal(page.clicks.length, 0);
    assert.equal(page.urls.length, 0);
  }
});

test('a verified PDF is downloaded with its PDF filename and no cache', async () => {
  const page = fixture(pdfResponse());
  assert.equal(await page.download(page.button), true);
  assert.deepEqual(page.clicks, ['recovery-codes.pdf']);
  assert.equal(page.requests[0].options.cache, 'no-store');
  assert.equal(page.requests[0].options.credentials, 'same-origin');
  assert.equal(page.attributes.has('aria-busy'), false);
});

test('a network failure remains on the current page without a file', async () => {
  const page = fixture(new Error('Synthetic disconnect'));
  assert.equal(await page.download(page.button), false);
  assert.equal(page.clicks.length, 0);
  assert.equal(page.attributes.has('aria-busy'), false);
});

test('the sanitized service filename from the server is preserved', async () => {
  const response = pdfResponse();
  response.headers.set('content-disposition', 'attachment; filename="My-Service-recovery-codes.pdf"');
  const page = fixture(response);
  assert.equal(await page.download(page.button), true);
  assert.deepEqual(page.clicks, ['My-Service-recovery-codes.pdf']);
});

test('unsafe filenames cannot introduce a path in the download', async () => {
  const response = pdfResponse();
  response.headers.set('content-disposition', 'attachment; filename="../../secret.pdf"');
  const page = fixture(response);
  assert.equal(await page.download(page.button), true);
  assert.deepEqual(page.clicks, ['recovery-codes.pdf']);
});

test('another click during a running download is ignored', async () => {
  const page = fixture(pdfResponse());
  page.button.setAttribute('aria-busy', 'true');
  assert.equal(await page.download(page.button), null);
  assert.equal(page.requests.length, 0);
});
