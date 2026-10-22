// Vaadin überträgt das Original begrenzt; dieser Baustein liefert nur die Ausschnittkoordinaten.
class AccountAvatarEditor extends HTMLElement {
  static get observedAttributes() {
    return ['data-mode', 'data-error-type', 'data-error-size', 'data-error-image', 'data-error-dimensions'];
  }

  connectedCallback() {
    this._observer = new MutationObserver(() => this._initialize());
    this._observer.observe(this, { childList: true, subtree: true });
    this._initialize();
  }

  disconnectedCallback() {
    this._observer?.disconnect();
    this._events?.abort();
    this._resize?.disconnect();
    this.clearImage(false);
    this._ready = false;
  }

  attributeChangedCallback() {
    if (!this._ready) return;
    this._draw();
    this._renderError();
  }

  _initialize() {
    if (this._ready) return;
    const find = name => this.querySelector(`[data-avatar="${name}"]`);
    const names = ['upload', 'filename', 'error', 'frame', 'crop', 'preview', 'zoom', 'percent', 'reset', 'cancel'];
    const elements = Object.fromEntries(names.map(name => [name, find(name)]));
    if (Object.values(elements).some(element => !element)) return;
    this._elements = elements;
    this._ready = true;
    this._epoch = (this._epoch ?? 0) + 1;
    this._zoom = 1;
    this._x = this._y = 0;
    this._events = new AbortController();
    const on = (element, type, handler) => element.addEventListener(type, handler, { signal: this._events.signal });
    on(elements.upload, 'upload-request', event => {
      this._uploading = true;
      this._activeUpload = event.detail.xhr;
      event.detail.file.avatarDraft = this.getAttribute('data-draft-id');
      event.detail.xhr.setRequestHeader('X-Avatar-Draft', event.detail.file.avatarDraft ?? '');
    });
    on(elements.upload, 'upload-success', event => {
      if (event.detail.file.avatarDraft !== this.getAttribute('data-draft-id')) return;
      this._uploading = false;
      this._activeUpload = null;
      const id = event.detail.xhr.getResponseHeader('X-Avatar-Image');
      if (id) this._select(event.detail.file, id);
      queueMicrotask(() => { elements.upload.files = []; });
    });
    on(elements.upload, 'upload-error', () => {
      this._uploading = false;
      this._activeUpload = null;
      this._showError('image');
      queueMicrotask(() => { elements.upload.files = []; });
    });
    on(elements.upload, 'upload-abort', () => { this._uploading = false; this._activeUpload = null; });
    on(elements.upload, 'file-reject', event => {
      this._elements.error.textContent = event.detail.error;
      elements.upload.files = [];
    });
    on(elements.zoom, 'input', () => {
      this._zoom = Number(elements.zoom.value);
      this._clamp();
      this._draw();
    });
    on(elements.reset, 'click', () => this._resetCrop());
    on(elements.cancel, 'click', () => this.cancelSelection());
    on(elements.crop, 'pointerdown', event => {
      if (!this._image || event.button !== 0) return;
      event.preventDefault();
      this._drag = { id: event.pointerId, x: event.clientX, y: event.clientY, left: this._x, top: this._y };
      elements.crop.setPointerCapture(event.pointerId);
    });
    on(elements.crop, 'pointermove', event => {
      if (!this._drag || this._drag.id !== event.pointerId) return;
      const side = elements.frame.clientWidth;
      if (!side) return;
      this._x = this._drag.left + (event.clientX - this._drag.x) / side;
      this._y = this._drag.top + (event.clientY - this._drag.y) / side;
      this._clamp();
      this._draw();
    });
    for (const type of ['pointerup', 'pointercancel', 'lostpointercapture']) {
      on(elements.crop, type, () => { this._drag = null; });
    }
    on(elements.crop, 'keydown', event => {
      if (!this._image) return;
      const step = event.shiftKey ? 0.1 : 0.025;
      if (event.key === 'ArrowLeft') this._x -= step;
      else if (event.key === 'ArrowRight') this._x += step;
      else if (event.key === 'ArrowUp') this._y -= step;
      else if (event.key === 'ArrowDown') this._y += step;
      else if (event.key === 'Home') { event.preventDefault(); this._resetCrop(); return; }
      else return;
      event.preventDefault();
      this._clamp();
      this._draw();
    });
    this._resize = new ResizeObserver(() => this._draw());
    this._resize.observe(elements.frame);
    this._resize.observe(elements.preview.parentElement);
    this._draw();
    this._renderError();
    this._observer.disconnect();
    if (this._savedSrc) this.loadSaved(this._savedSrc);
  }

  async _select(file, imageId) {
    this._loading = true;
    const epoch = ++this._epoch;
    if (this._pendingUrl) URL.revokeObjectURL(this._pendingUrl);
    this._pendingUrl = null;
    this._showError(null);
    if (file.size > 5 * 1024 * 1024) { this._loading = false; this._showError('size'); return; }
    try {
      const bytes = new Uint8Array(await file.slice(0, 12).arrayBuffer());
      if (epoch !== this._epoch || !this._ready) return;
      const png = [137, 80, 78, 71, 13, 10, 26, 10].every((byte, index) => bytes[index] === byte);
      const jpeg = bytes[0] === 255 && bytes[1] === 216 && bytes[2] === 255;
      if (!png && !jpeg) { this._loading = false; this._showError('type'); return; }
      const url = URL.createObjectURL(file);
      this._pendingUrl = url;
      const image = new Image();
      image.onload = () => {
        if (epoch !== this._epoch || !this._ready) { URL.revokeObjectURL(url); return; }
        this._pendingUrl = null;
        const width = image.naturalWidth, height = image.naturalHeight;
        if (!width || !height || width > 8192 || height > 8192 || width * height > 20000000) {
          URL.revokeObjectURL(url);
          this._loading = false;
          this._showError('dimensions');
          return;
        }
        if (this._url) URL.revokeObjectURL(this._url);
        this._url = url;
        this._image = image;
        this._loading = false;
        this._local = true;
        this._imageId = imageId;
        this.setAttribute('data-local-image', 'true');
        this._elements.filename.textContent = file.name;
        this.setAttribute('data-has-image', 'true');
        this.setAttribute('data-mode', 'CUSTOM');
        this._resetCrop();
        this.dispatchEvent(new CustomEvent('avatar-file-selected'));
      };
      image.onerror = () => {
        URL.revokeObjectURL(url);
        if (epoch !== this._epoch || !this._ready) return;
        this._pendingUrl = null;
        this._loading = false;
        this._showError('image');
      };
      image.src = url;
    } catch {
      if (epoch === this._epoch) { this._loading = false; this._showError('image'); }
    }
  }

  _resetCrop() {
    this._zoom = 1;
    this._x = this._y = 0;
    this._drag = null;
    this._draw();
  }

  _clamp() {
    if (!this._image) return;
    const side = Math.min(this._image.naturalWidth, this._image.naturalHeight);
    const maxX = (this._image.naturalWidth / side * this._zoom - 1) / 2;
    const maxY = (this._image.naturalHeight / side * this._zoom - 1) / 2;
    this._x = Math.max(-maxX, Math.min(maxX, this._x));
    this._y = Math.max(-maxY, Math.min(maxY, this._y));
  }

  _draw() {
    if (!this._ready) return;
    const { crop, preview, zoom, percent, reset } = this._elements;
    zoom.disabled = reset.disabled = !this._image || !this._local;
    zoom.value = String(this._zoom ?? 1);
    percent.textContent = `${Math.round((this._zoom ?? 1) * 100)} %`;
    for (const canvas of [crop, preview]) {
      const side = Math.round(canvas.parentElement.clientWidth * Math.min(window.devicePixelRatio || 1, 2));
      if (!side) continue;
      canvas.width = canvas.height = side;
      const context = canvas.getContext('2d');
      if (!context || !this._image) continue;
      const short = Math.min(this._image.naturalWidth, this._image.naturalHeight);
      const width = this._image.naturalWidth / short * this._zoom;
      const height = this._image.naturalHeight / short * this._zoom;
      context.drawImage(this._image, ((1 - width) / 2 + this._x) * side, ((1 - height) / 2 + this._y) * side, width * side, height * side);
    }
  }

  _showError(code) {
    this._errorCode = code;
    this._renderError();
  }

  _renderError() {
    if (!this._ready) return;
    this._elements.error.textContent = this._errorCode ? this.getAttribute(`data-error-${this._errorCode}`) ?? '' : '';
  }

  loadSaved(src) {
    this._savedSrc = src;
    if (!this._ready || !src) return;
    const epoch = ++this._epoch;
    this._loading = true;
    const image = new Image();
    image.onload = () => {
      if (epoch !== this._epoch || !this._ready) return;
      this._image = image; this._local = false; this._imageId = null; this._loading = false;
      this.setAttribute('data-has-image', 'true');
      this.setAttribute('data-local-image', 'false');
      this._resetCrop();
    };
    image.onerror = () => { if (epoch === this._epoch) { this._loading = false; this._showError('image'); } };
    image.src = src;
  }

  getCrop() {
    return { local: !!this._local, imageId: this._imageId ?? null, zoom: this._zoom ?? 1,
      x: this._x ?? 0, y: this._y ?? 0, loading: !!this._loading || !!this._uploading };
  }

  clearImage(notify = true) {
    this._epoch = (this._epoch ?? 0) + 1;
    this._activeUpload?.abort();
    this._activeUpload = null;
    this._savedSrc = null;
    this._uploading = this._loading = this._local = false;
    this._imageId = null;
    this.setAttribute('data-local-image', 'false');
    if (this._url) URL.revokeObjectURL(this._url);
    if (this._pendingUrl) URL.revokeObjectURL(this._pendingUrl);
    this._url = this._pendingUrl = this._image = null;
    this.setAttribute('data-has-image', 'false');
    // Den vom Server gesetzten Radiomodus erhalten: ein verzögerter Reset darf ihn nicht überschreiben.
    if (this._elements) {
      this._elements.filename.textContent = '';
      this._elements.upload.files = [];
    }
    this._showError(null);
    this._resetCrop();
    if (notify && this.isConnected) this.dispatchEvent(new CustomEvent('avatar-file-cleared'));
  }

  cancelSelection() {
    this.clearImage(false);
    // Nur die neue Auswahl verwerfen; der Dialog stellt ein gespeichertes Bild wieder her.
    if (this.isConnected) this.dispatchEvent(new CustomEvent('avatar-file-cancelled'));
  }

  resetDraft() {
    this.clearImage(false);
  }
}

if (!customElements.get('account-avatar-editor')) {
  customElements.define('account-avatar-editor', AccountAvatarEditor);
}
