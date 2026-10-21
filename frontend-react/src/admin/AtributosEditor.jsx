// Pares clave/valor opcionales del producto (ej. talla = M). filas es
// [{ id, clave, valor }]; onChange recibe la lista actualizada.
export default function AtributosEditor({ filas, onChange }) {
  function actualizarFila(id, campo, valor) {
    onChange(filas.map((f) => (f.id === id ? { ...f, [campo]: valor } : f)));
  }

  function quitarFila(id) {
    onChange(filas.filter((f) => f.id !== id));
  }

  function agregarFila() {
    onChange([...filas, { id: crypto.randomUUID(), clave: '', valor: '' }]);
  }

  return (
    <div className="field">
      <label>Atributos</label>
      <span className="hint">Pares clave/valor opcionales, por ejemplo talla = M.</span>
      <div className="atributos-filas">
        {filas.map((f) => (
          <div className="atributo-fila" key={f.id}>
            <input
              type="text"
              placeholder="clave"
              value={f.clave}
              onChange={(e) => actualizarFila(f.id, 'clave', e.target.value)}
            />
            <input
              type="text"
              placeholder="valor"
              value={f.valor}
              onChange={(e) => actualizarFila(f.id, 'valor', e.target.value)}
            />
            <button
              type="button"
              className="btn ghost small"
              onClick={() => quitarFila(f.id)}
              aria-label="Quitar atributo"
            >
              ×
            </button>
          </div>
        ))}
      </div>
      <button type="button" className="btn secondary small" onClick={agregarFila}>Agregar atributo</button>
    </div>
  );
}

export function atributosAFilas(atributos = {}) {
  return Object.entries(atributos).map(([clave, valor]) => ({ id: crypto.randomUUID(), clave, valor }));
}

export function filasAAtributos(filas) {
  const atributos = {};
  filas.forEach(({ clave, valor }) => {
    const k = clave.trim();
    if (k) atributos[k] = valor;
  });
  return atributos;
}
