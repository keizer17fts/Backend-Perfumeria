-- Amplía estados preservando órdenes y datos existentes.
ALTER TABLE ordenes DROP CONSTRAINT ck_ordenes_estado;
ALTER TABLE ordenes ADD CONSTRAINT ck_ordenes_estado
  CHECK (estado IN ('Pendiente', 'Pagado', 'Pago fallido', 'En preparación', 'Enviado', 'Entregado'));
