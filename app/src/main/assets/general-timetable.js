(function () {
  const records = [];
  const errors = [];
  const groups = [];
  function collect(doc, depth) {
    groups.push(...doc.querySelectorAll('#kbgrid .timetable_con, #kbtable .kbcontent'));
    if (depth < 3) for (const frame of doc.querySelectorAll('iframe,frame')) {
      if (frame.contentDocument) collect(frame.contentDocument, depth + 1);
    }
  }
  collect(document, 0);
  for (const group of groups) {
    const text = group.innerText.trim();
    if (!text || text === ' ') continue;
    const cell = group.closest('td');
    const coordinates = (group.id || cell?.id || '').match(/^([1-7])-([1-9]\d?)(?:-\d+)?$/);
    const cells = cell?.parentElement?.cells;
    const gridDay = group.closest('#kbtable') && cells?.length === 8
      && [...cells].every(item => item.colSpan === 1) ? cell.cellIndex : 0;
    const day = Number(coordinates?.[1] || cell?.dataset.weekday || gridDay);
    const slots = [...text.matchAll(/(?:第[ \t]*)?\[?(\d{1,2})(?:[ \t]*[-－至][ \t]*(\d{1,2}))?\]?[ \t]*节/g)];
    const weeks = [...text.matchAll(/([\d,，、\-－至 \t]+)[ \t]*(?:\(([单双])\)|（([单双])）)?[ \t]*周(?:[ \t]*\(([单双])周?\)|[ \t]*（([单双])周?）)?/g)];
    const title = group.querySelector('.title')?.innerText.trim() || text.split('\n')[0].trim();
    if (!(day >= 1 && day <= 7) || slots.length !== 1 || weeks.length !== 1 || !title
      || group.querySelector('hr') || /-{8,}/.test(text)) {
      errors.push(title.slice(0, 80));
      continue;
    }
    const findField = (label) => {
      const inline = text.match(new RegExp(label + '[:：]\\s*([^\\n]+)'));
      if (inline) return inline[1].trim();
      return [...group.querySelectorAll('[title]')].find(el => el.title === label)?.innerText.trim() || '';
    };
    records.push({title, day, first: Number(slots[0][1]), last: Number(slots[0][2] || slots[0][1]),
      weeks: weeks[0][1].trim(), parity: weeks[0].slice(2).find(Boolean) || '',
      room: (findField('上课地点') || findField('教室')).replace(/\[\d+(?:[-－至]\d+)?\]节$/, '').trim(),
      teacher: findField('教师') || findField('老师')});
  }
  return {records, errors};
})()
