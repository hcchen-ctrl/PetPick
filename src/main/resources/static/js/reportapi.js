export async function listNeedAdoptions() {
  const r = await fetch('/api/petreport/adoptions/need');
  if (!r.ok) throw new Error('Failed to load need list');
  return r.json();
}

export async function listDoneAdoptions() {
  const r = await fetch('/api/petreport/adoptions/done');
  if (!r.ok) throw new Error('Failed to load done list');
  return r.json();
}

export async function listReportsByAdoptionId(id) {
  const r = await fetch(`/api/petreport/adoptions/${id}/reports`);
  if (!r.ok) throw new Error('Failed to load reports');
  return r.json();
}

export async function searchAll(q) {
  q = (q || '').trim();
  if (q.length < 2) return [];
  const r = await fetch(`/api/petreport/search?q=${encodeURIComponent(q)}`);
  if (!r.ok) throw new Error('Search failed');
  return r.json();
}

export async function deleteReport(id) {
  const r = await fetch(`/api/petreport/reports/${id}`, { method: 'DELETE' });
  if (!r.ok && r.status !== 204) throw new Error('Delete failed');
  return true;
}

export async function createReport(adoptionId, payload) {
  const r = await fetch(`/api/petreport/adoptions/${adoptionId}/reports`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json; charset=utf-8' },
    body: JSON.stringify(payload),
  });
  if (!r.ok && r.status !== 204 && r.status !== 201) {
    throw new Error('Submit failed');
  }
  return null;
}

export async function getAdoption(adoptionId) {
  const r = await fetch(`/api/petreport/adoptions/${adoptionId}`);
  if (!r.ok) throw new Error('Load adoption failed');
  if (r.status === 204) return null;
  return r.json();
}




