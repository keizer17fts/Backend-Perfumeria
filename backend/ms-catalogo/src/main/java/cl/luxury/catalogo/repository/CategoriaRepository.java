package cl.luxury.catalogo.repository;

import cl.luxury.catalogo.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, String> {
}
