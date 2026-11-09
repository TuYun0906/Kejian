const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const script = fs.readFileSync(path.join(__dirname, '../app/src/main/assets/general-timetable.js'), 'utf8');

function read(text, { id = '', grid = false, fields = [] } = {}) {
  const cell = { id, dataset: {}, cellIndex: 5, parentElement: {
    cells: Array.from({ length: 8 }, () => ({ colSpan: 1 }))
  } };
  const group = { innerText: text, closest: selector => selector === 'td' ? cell : grid,
    querySelector: () => null, querySelectorAll: () => fields };
  const document = { querySelectorAll: selector => selector === 'iframe,frame' ? [] : [group] };
  return JSON.parse(JSON.stringify(vm.runInNewContext(script, { document })));
}

test('reads weekday, inclusive section range and odd weeks from a timetable block', () => {
  const result = read('高等数学\n第1-2节\n1-16周(单)\n教室：明理101', { id: '3-1' });
  assert.deepEqual(result, { records: [{ title: '高等数学', day: 3, first: 1,
    last: 2, weeks: '1-16', parity: '单', room: '明理101', teacher: '' }], errors: [] });
});

test('reads bracketed sections and Friday from an eight-column timetable', () => {
  const result = read('英语\n1-16周\n教学楼101[3-4]节', { grid: true, fields: [
    { title: '老师', innerText: '张老师' }, { title: '教室', innerText: '教学楼101[3-4]节' }
  ] });
  assert.deepEqual(result.records[0], { title: '英语', day: 5, first: 3,
    last: 4, weeks: '1-16', parity: '', room: '教学楼101', teacher: '张老师' });
});

test('reports incomplete or combined blocks instead of silently dropping them', () => {
  assert.deepEqual(read('高等数学\n第1-2节', { id: '3-1' }),
    { records: [], errors: ['高等数学'] });
  assert.deepEqual(read('英语\n1-16周\n第3-4节\n--------\n实验', { id: '3-1' }),
    { records: [], errors: ['英语'] });
});
