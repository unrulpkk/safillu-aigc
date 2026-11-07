const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

function fixture() {
  const cloud = fs.readFileSync(path.join(__dirname, '../dist/studio-cloud.js'), 'utf8');
  const tasks = fs.readFileSync(path.join(__dirname, '../dist/studio-tasks.js'), 'utf8');
  const uploadStart = cloud.indexOf('async function cloudUpload(');
  const upload = cloud.slice(uploadStart, cloud.indexOf("\ndocument.addEventListener('change'", uploadStart));
  const generateStart = tasks.indexOf('generate=async function()');
  const generate = tasks.slice(generateStart, tasks.indexOf('actions.generate=generate;', generateStart) + 'actions.generate=generate;'.length);
  let resolveUpload, rejectUpload;
  const waiting = new Promise((resolve, reject) => { resolveUpload = resolve; rejectUpload = reject; });
  const calls = [], draft = { mode: 'image', prompt: 'Edit my reference', refs: [], attachments: [] };
  const context = vm.createContext({
    cloudUploadsPending: 0, cloudEpoch: 0, taskSubmitting: false, taskRequest: null,
    state: { current: '1' }, actions: {}, referenceSlot: 'reference', structuredClone,
    FormData: class { append() {} }, $: () => ({}), syncComposer() {}, render() {}, save() {}, toast() {},
    cloudRequire: () => true, cloudFlush: async () => {}, cloudLoad: async () => {},
    record: id => ({ id }), cloudAddReference: r => { calls.push('reference-ready'); draft.refs.push({ assetId: r.id }); },
    draft: () => draft, normalizeDraft: x => x, isReady: () => true, liveCapability: () => ({ id: '870013' }),
    uid: () => 'request-1', refreshTasks: async () => {}, cloudFail: error => { throw error; },
    cloudRequest: async (url, options) => {
      if (url.endsWith('/assets/upload')) { calls.push('upload-start'); return waiting; }
      if (url.endsWith('/tasks/quote')) { calls.push('quote'); return {}; }
      if (url.endsWith('/tasks')) { calls.push({ request: JSON.parse(options.body) }); return { id: '1', reused: false }; }
      throw Error('Unexpected request');
    }
  });
  vm.runInContext(upload + '\n' + generate, context);
  return { context, calls, draft, resolveUpload, rejectUpload };
}

test('generation waits for the upload and receives the uploaded asset ID', async () => {
  const f = fixture();
  const uploading = f.context.cloudUpload([{ size: 100, name: 'reference.png' }], 'reference');
  await new Promise(setImmediate);
  assert.equal(f.context.cloudUploadsPending, 1);
  await f.context.generate();
  assert.deepEqual(f.calls, ['upload-start']);
  f.resolveUpload('11');
  await uploading;
  assert.equal(f.context.cloudUploadsPending, 0);
  await f.context.generate();
  assert.equal(f.calls[1], 'reference-ready');
  assert.equal(f.calls[2], 'quote');
  assert.equal(f.calls[3].request.draft.refs[0].assetId, '11');
});

test('a failed upload releases the UI gate without submitting generation', async () => {
  const f = fixture();
  const uploading = f.context.cloudUpload([{ size: 100, name: 'reference.png' }], 'reference');
  await new Promise(setImmediate);
  f.rejectUpload(Error('Upload failed'));
  await assert.rejects(uploading, /Upload failed/);
  assert.equal(f.context.cloudUploadsPending, 0);
  assert.deepEqual(f.calls, ['upload-start']);
});

test('an upload completing after an account switch never attaches to the new account', async () => {
  const f = fixture();
  const uploading = f.context.cloudUpload([{ size: 100, name: 'reference.png' }], 'reference');
  await new Promise(setImmediate);
  f.context.cloudEpoch++;
  f.resolveUpload('old-account-asset');
  await uploading;
  assert.equal(f.context.cloudUploadsPending, 0);
  assert.equal(f.draft.refs.length, 0);
  assert.deepEqual(f.calls, ['upload-start']);
});
