document.addEventListener('click', event => { const link=event.target.closest('[data-confirm]'); if(link && !window.confirm(link.dataset.confirm)) event.preventDefault(); });
