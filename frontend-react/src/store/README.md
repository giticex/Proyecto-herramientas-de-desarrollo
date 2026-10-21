# Store (`src/store`)

Un único hook de zustand, `useStore`, con la marca de la tienda y sus productos.
Cada acción intenta el backend real (`src/api`) y, si falla o el endpoint
todavía no existe, cae al mock de `localStorage` (mismo comportamiento que el
`Store` viejo de `frontend/js/store.js`).

**Estado:** `tienda`, `productos`, `umbral`.

**Acciones:** `getTienda`, `saveTienda`, `listProductos`, `createProducto`,
`updateProducto`, `deleteProducto`, `getUmbral`, `setUmbral`.

**Forma de los datos** (no cambiarla):
- Marca: `{ nombre, rubro, slug, colorPrimario, colorSecundario, logoUrl }`
- Producto: `{ idProducto, nombre, stock, precio, categoria, atributos }` —
  `idProducto` es número y `atributos` es un objeto libre opcional (ej.
  `{ talla: "M", color: "negro" }`).

## Ejemplo de uso

```jsx
import { useEffect } from 'react';
import { useStore } from '../store/store';

function ListaProductos() {
  const { productos, listProductos } = useStore();
  useEffect(() => { listProductos(); }, [listProductos]);
  return <ul>{productos.map((p) => <li key={p.idProducto}>{p.nombre}</li>)}</ul>;
}
```
