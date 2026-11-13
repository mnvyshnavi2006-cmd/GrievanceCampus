(() => {
    const maxFileSize = 5 * 1024 * 1024;
    const allowedTypes = new Set(['image/jpeg', 'image/png', 'image/webp']);
    let previewUrl = null;

    document.addEventListener('change', (event) => {
        const input = event.target;
        if (!(input instanceof HTMLInputElement)
                || input.type !== 'file'
                || !input.hasAttribute('data-preview')) {
            return;
        }

        const image = document.querySelector(input.getAttribute('data-preview'));
        if (!(image instanceof HTMLImageElement)) {
            console.error('Grievance photo preview target must be an image element.');
            return;
        }

        const file = input.files && input.files.length ? input.files[0] : null;
        if (!file) {
            image.removeAttribute('src');
            image.hidden = true;
            if (previewUrl) URL.revokeObjectURL(previewUrl);
            previewUrl = null;
            return;
        }

        if (file.size > maxFileSize) {
            input.value = '';
            alert('Choose an image smaller than 5 MB.');
            return;
        }

        if (!allowedTypes.has(file.type)) {
            input.value = '';
            alert('Choose a JPG, PNG, or WebP image.');
            return;
        }

        if (previewUrl) URL.revokeObjectURL(previewUrl);
        previewUrl = URL.createObjectURL(file);
        image.src = previewUrl;
        image.hidden = false;
    });
})();
