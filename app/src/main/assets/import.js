(async () => {
  const state = window.__kejian = { state: 'running', progress: '获取教学周' };
  const base = { clientOSType: 'H5-android', deviceModel: 'Android', clientVerNum: '102', source: '157', osType: 'H5', deviceId: window.__kejianDevice };
  async function request(rid, extra) {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 6000);
    try {
      const response = await fetch('/api_proxy/cplus/index.php?rid=' + rid, {
        method: 'POST', credentials: 'same-origin', signal: controller.signal,
        headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
        body: new URLSearchParams({ ...base, ...extra })
      });
      if (!response.ok) throw new Error('服务器暂不可用');
      return await response.json();
    } finally { clearTimeout(timer); }
  }
  async function readProfile() {
    let profile = null;
    try {
      const user = await request('queryUser', {});
      if (user.code !== '0x000000') return null;
      const role = user.roleInfo || {};
      profile = { nickname: role.NICKNAME || (user.moreUserInfo || {}).xm || '', avatar: '' };
      const raw = String(role.HEADIMAGE || '').trim();
      if (!raw) return { ...profile, avatarIssue: 'no_photo' };
      const directory = new URL((user.data || {}).photoUrl || '/', location.origin);
      const absolute = raw.startsWith('//') || /^[a-z][a-z0-9+.-]*:/i.test(raw);
      const url = new URL(absolute ? raw : raw.replace(/^\/+/, ''), directory.href.replace(/\/?$/, '/'));
      if (url.protocol !== 'https:') return profile;
      const controller = new AbortController();
      const timer = setTimeout(() => controller.abort(), 6000);
      let blob;
      try {
        const image = await fetch(url.href, { credentials: 'same-origin', signal: controller.signal });
        if (!image.ok) return { ...profile, avatarIssue: 'http' };
        blob = await image.blob();
      } finally { clearTimeout(timer); }
      if (blob.type === 'text/html') return { ...profile, avatarIssue: 'html' };
      if (!blob.size || blob.size > 1000000 ||
          (blob.type && !blob.type.startsWith('image/') && blob.type !== 'application/octet-stream')) {
        return { ...profile, avatarIssue: 'format' };
      }
      profile.avatar = await new Promise(resolve => {
        const reader = new FileReader();
        const timeout = setTimeout(() => resolve(''), 2000);
        reader.onload = () => { clearTimeout(timeout); resolve(String(reader.result).split(',')[1] || ''); };
        reader.onerror = () => { clearTimeout(timeout); resolve(''); };
        reader.readAsDataURL(blob);
      });
    } catch (ignored) {}
    return profile;
  }
  const wait = () => new Promise(resolve => setTimeout(resolve, 200));
  try {
    if (window.__kejianAvatarOnly) {
      state.progress = '正在同步账号头像';
      window.__kejian = { state: 'done', data: { profile: await readProfile() } };
      return;
    }
    const meta = await request('getNowTermWeek', {});
    if (meta.code !== '0x000000' || !meta.data || !Array.isArray(meta.data.weeks) || !meta.data.weeks.length) throw new Error('请确认登录成功后重试');
    if (meta.data.weeks.length > 60) throw new Error('教学周数量异常');
    const weeks = [];
    let networkFailures = 0;
    for (const week of meta.data.weeks) {
      state.progress = '正在获取第' + week.value + '周';
      const row = { number: Number(week.value), start: week.ks, end: week.js, state: 'unknown', courses: [], confirmable: false };
      if (networkFailures < 3) {
        let result;
        for (let attempt = 0; attempt < 2; attempt++) {
          try { result = await request('getCurriculumScheduleByWeek', { week: week.value }); break; }
          catch (error) { row.reason = 'network'; }
        }
        if (result) {
          networkFailures = 0;
          if (result.code === '0x000000') {
            if (!Array.isArray(result.data)) throw new Error('课程返回格式异常，原课表已保留');
            row.state = 'ok';
            // Only timetable fields are retained; exclude student identifiers.
            row.courses = result.data.map(x => Object.fromEntries(['KBID','XNXQDM','KCM','SKLS','JSMC','SKXQ','KSJC','JSJC'].map(k => [k, x[k]])));
          } else if (result.code === 'CampusController_class_0x00000600000') {
            row.reason = 'business'; row.confirmable = true;
          } else { throw new Error('登录可能失效，或来源暂不可用，请重新登录'); }
        } else { networkFailures++; }
      } else { row.reason = 'network'; }
      weeks.push(row);
      await wait();
    }
    const profile = window.__kejianSkipProfile ? undefined : await readProfile();
    window.__kejian = { state: 'done', data: { weeks, updated: Date.now(), profile, termEnd: weeks[weeks.length - 1].end, termLabel: weeks[0].start + " 至 " + weeks[weeks.length - 1].end } };
  } catch (error) {
    window.__kejian = { state: 'error', message: String(error.message || error) };
  }
})();
