const debounce = (fn, ms = 250) => {
  let t;
  return (...args) => { clearTimeout(t); t = setTimeout(()=>fn(...args), ms); };
};

function initSearch() {
  const input = document.querySelector('input[name="q"]');
  const results = document.getElementById('ajax-results');
  let suggBox = document.getElementById('suggestions');
  if (!suggBox) {
    suggBox = document.createElement('div');
    suggBox.className = 'suggest-list';
    input.parentNode.appendChild(suggBox);
  }

  input.addEventListener('input', debounce(async (e)=>{
    const q = e.target.value.trim();
    suggBox.innerHTML = '';
    suggBox.style.display = 'none';
    if (q.length < 2) return;
    try {
      const res = await fetch('/api/suggest?q=' + encodeURIComponent(q));
      const items = await res.json();
      if (!items || items.length===0) { suggBox.style.display='none'; return; }
      suggBox.style.display = 'block';
      items.forEach(it => {
        const li = document.createElement('li');
        li.className = 'list-group-item list-group-item-action';
        li.textContent = it;
        li.addEventListener('click', ()=>{ input.value = it; suggBox.innerHTML = ''; const f = input.closest('form'); if(f) f.submit(); });
        suggBox.appendChild(li);
      });
    } catch(e){ /* ignore */ }
  }, 200));
  // Note: form submission is handled by the browser to perform a server-side search.
}

document.addEventListener('DOMContentLoaded', initSearch);
