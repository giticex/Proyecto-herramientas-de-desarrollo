import assert from 'node:assert/strict';
import { test } from 'node:test';
import { coincidenciaMarcaYPrecio, errorRangoPrecio, opcionesDeMarca } from '../src/tienda/filtrosCatalogo.js';

const productos = [
  { categoria: 'Audio', precio: '50.00', atributos: { Marca: ' Sony ', Color: 'Negro' } },
  { categoria: 'Audio', precio: 150, atributos: { marca: 'sony' } },
  { categoria: 'Monitores', precio: 200, atributos: { MARCA: 'LG' } },
  { categoria: 'Audio', precio: 25, atributos: {} },
];

test('lista marcas de la categoría sin duplicar variantes de capitalización', () => {
  const audio = productos.filter((producto) => producto.categoria === 'Audio');
  assert.deepEqual(opcionesDeMarca(audio), [{ valor: 'sony', etiqueta: 'Sony' }]);
  assert.equal(opcionesDeMarca(productos).length, 2);
});

test('combina marca con ambos límites inclusivos y admite límites abiertos', () => {
  assert.deepEqual(
    productos.filter(coincidenciaMarcaYPrecio({ marca: 'sony', desde: '50', hasta: '50' })),
    [productos[0]],
  );
  assert.deepEqual(
    productos.filter(coincidenciaMarcaYPrecio({ marca: '', desde: '', hasta: '50' })),
    [productos[0], productos[3]],
  );
  assert.deepEqual(
    productos.filter(coincidenciaMarcaYPrecio({ marca: '', desde: '150', hasta: '' })),
    [productos[1], productos[2]],
  );
});

test('informa rangos negativos o invertidos y permite limpiar los filtros', () => {
  assert.ok(errorRangoPrecio('-1', '150'));
  assert.ok(errorRangoPrecio('200', '50'));
  assert.deepEqual(
    productos.filter(coincidenciaMarcaYPrecio({ marca: '', desde: '200', hasta: '50' })),
    [],
  );
  assert.deepEqual(
    productos.filter(coincidenciaMarcaYPrecio({ marca: '', desde: '', hasta: '' })),
    productos,
  );
});
