import assert from 'node:assert/strict';
import { test } from 'node:test';
import { coincidenciaBusquedaCatalogo } from '../src/tienda/busquedaCatalogo.js';

const productos = [
  { nombre: 'Teclado', categoria: 'Periféricos', sku: '00123' },
  { nombre: 'Funda 00123', categoria: 'Accesorios', sku: '001234' },
  { nombre: 'Mouse', categoria: 'Periféricos', sku: 'MOUSE-2' },
  { nombre: 'Pantalla', categoria: 'Monitores' },
];

const buscar = (texto) => productos.filter(coincidenciaBusquedaCatalogo(productos, texto));

test('el SKU exacto tiene prioridad sobre nombres y códigos parecidos', () => {
  assert.deepEqual(buscar(' 00123 '), [productos[0]]);
  assert.deepEqual(buscar('mouse-2'), [productos[2]]);
  assert.deepEqual(buscar('001'), [productos[1]]);
});

test('conserva la búsqueda por nombre y categoría y admite productos sin SKU', () => {
  assert.deepEqual(buscar('pantalla'), [productos[3]]);
  assert.deepEqual(buscar('periféricos'), [productos[0], productos[2]]);
  assert.deepEqual(buscar(' '), productos);
  assert.deepEqual(buscar('código inexistente'), []);
});
