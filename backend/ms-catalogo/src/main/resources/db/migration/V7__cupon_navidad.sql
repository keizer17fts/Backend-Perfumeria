INSERT INTO cupones (codigo, descuento, activo) SELECT 'NAVIDAD20',20,TRUE WHERE NOT EXISTS (SELECT 1 FROM cupones WHERE codigo = 'NAVIDAD20');
