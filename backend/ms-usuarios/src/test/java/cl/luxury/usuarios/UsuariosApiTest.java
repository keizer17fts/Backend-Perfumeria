package cl.luxury.usuarios;

import cl.luxury.usuarios.model.Usuario;
import cl.luxury.usuarios.repository.UsuarioRepository;
import cl.luxury.usuarios.util.RunUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UsuariosApiTest {

    // RUN válidos (módulo 11): 190110222, 152345674, 123456785, 111111111
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder encoder;

    @BeforeEach
    void limpiar() {
        usuarios.deleteAll();
    }

    private Usuario guardar(String run, String correo, String clave, String tipo) {
        Usuario u = new Usuario();
        u.setRun(run);
        u.setNombre("Nombre");
        u.setApellidos("Apellido");
        u.setCorreo(correo);
        u.setPasswordHash(encoder.encode(clave));
        u.setTipo(tipo);
        return usuarios.save(u);
    }

    private String cuerpo(String run, String correo, String password) {
        String clave = password == null ? "" : ",\"password\":\"" + password + "\"";
        return "{\"run\":\"" + run + "\",\"nombre\":\"Ana\",\"apellidos\":\"Pérez\",\"correo\":\"" + correo + "\""
                + clave + ",\"tipo\":\"Cliente\"}";
    }

    private String login(String correo, String password) {
        return "{\"correo\":\"" + correo + "\",\"password\":\"" + password + "\"}";
    }

    // ------------------------------------------------------------ RUN

    @Test
    void elRunValidoSePuedeVerificarConElModulo11() {
        assertTrue(RunUtil.esValido("190110222"));
        assertTrue(RunUtil.esValido("20.987.654-k"));
        assertFalse(RunUtil.esValido("190110221"));
        assertFalse(RunUtil.esValido("abc"));
    }

    // ------------------------------------------------------------ crear

    @Test
    void crearNoDevuelveLaPasswordYGuardaUnHashBcrypt() throws Exception {
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("123456785", "ana@gmail.com", "clave1")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("123456785"))
                .andExpect(jsonPath("$.run").value("123456785"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        String hash = usuarios.findById("123456785").orElseThrow().getPasswordHash();
        assertTrue(hash.startsWith("$2"));
        assertFalse(hash.contains("clave1"));
    }

    @Test
    void crearNormalizaElRunConPuntosYGuion() throws Exception {
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("12.345.678-5", "ana@gmail.com", "clave1")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("123456785"));
    }

    @Test
    void crearConRunInvalidoResponde400() throws Exception {
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("190110221", "ana@gmail.com", "clave1")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearConCorreoDeDominioNoPermitidoResponde400() throws Exception {
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("123456785", "ana@hotmail.com", "clave1")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearConPasswordCortaResponde400() throws Exception {
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("123456785", "ana@gmail.com", "abc")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearSinPasswordResponde400() throws Exception {
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("123456785", "ana@gmail.com", null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearConCorreoRepetidoResponde409() throws Exception {
        guardar("111111111", "ana@gmail.com", "clave1", "Cliente");
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("123456785", "ANA@gmail.com", "clave1")))
                .andExpect(status().isConflict());
    }

    @Test
    void crearConRunRepetidoResponde409() throws Exception {
        guardar("123456785", "otra@gmail.com", "clave1", "Cliente");
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("123456785", "ana@gmail.com", "clave1")))
                .andExpect(status().isConflict());
    }

    // ------------------------------------------------------------ consultar

    @Test
    void listarPuedeFiltrarPorCorreoYPorRun() throws Exception {
        guardar("123456785", "ana@gmail.com", "clave1", "Cliente");
        guardar("111111111", "luis@gmail.com", "clave1", "Cliente");

        mvc.perform(get("/api/usuarios").param("correo", "ANA@gmail.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].run").value("123456785"))
                .andExpect(jsonPath("$[0].password").doesNotExist());

        mvc.perform(get("/api/usuarios").param("run", "11.111.111-1"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].correo").value("luis@gmail.com"));

        mvc.perform(get("/api/usuarios").param("run", "999999999"))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void usuarioInexistenteResponde404() throws Exception {
        mvc.perform(get("/api/usuarios/123456785"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ------------------------------------------------------------ login

    @Test
    void loginCorrectoDevuelveElUsuarioSinPassword() throws Exception {
        guardar("190110222", "admin@duoc.cl", "admin123", "Administrador");
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("  ADMIN@duoc.cl ", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Administrador"))
                .andExpect(jsonPath("$.id").value("190110222"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void loginConCorreoInexistenteResponde401IndicandoElCampoCorreo() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("nadie@gmail.com", "clave1")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.campo").value("correo"));
    }

    @Test
    void loginConPasswordIncorrectaResponde401IndicandoElCampoPassword() throws Exception {
        guardar("190110222", "admin@duoc.cl", "admin123", "Administrador");
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("admin@duoc.cl", "otra")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.campo").value("password"));
    }

    // ------------------------------------------------------------ editar

    @Test
    void putSinPasswordConservaLaContrasenaActual() throws Exception {
        guardar("111111111", "luis@gmail.com", "clave1", "Cliente");
        mvc.perform(put("/api/usuarios/111111111").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("111111111", "luis@gmail.com", null).replace("Ana", "Luis")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Luis"));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("luis@gmail.com", "clave1")))
                .andExpect(status().isOk());
    }

    @Test
    void putNoPermiteCambiarElRun() throws Exception {
        guardar("111111111", "luis@gmail.com", "clave1", "Cliente");
        mvc.perform(put("/api/usuarios/111111111").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("123456785", "luis@gmail.com", null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patchCambiaSoloLoEnviadoYPermiteCambiarLaPassword() throws Exception {
        guardar("111111111", "luis@gmail.com", "clave1", "Cliente");
        mvc.perform(patch("/api/usuarios/111111111").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Luisito\",\"password\":\"nueva9\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Luisito"))
                .andExpect(jsonPath("$.apellidos").value("Apellido"));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("luis@gmail.com", "nueva9")))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("luis@gmail.com", "clave1")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void patchIgnoraUnIntentoDeCambiarElRun() throws Exception {
        guardar("111111111", "luis@gmail.com", "clave1", "Cliente");
        mvc.perform(patch("/api/usuarios/111111111").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"run\":\"123456785\",\"nombre\":\"Otro\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("111111111"));
    }

    // ------------------------------------------------------------ eliminar

    @Test
    void eliminarUnClienteResponde204YLuegoYaNoExiste() throws Exception {
        guardar("190110222", "admin@duoc.cl", "admin123", "Administrador");
        guardar("111111111", "luis@gmail.com", "clave1", "Cliente");

        mvc.perform(delete("/api/usuarios/111111111")).andExpect(status().isNoContent());
        mvc.perform(get("/api/usuarios/111111111")).andExpect(status().isNotFound());
    }

    @Test
    void noSePuedeEliminarAlUnicoAdministrador() throws Exception {
        guardar("190110222", "admin@duoc.cl", "admin123", "Administrador");
        mvc.perform(delete("/api/usuarios/190110222")).andExpect(status().isConflict());
    }
}
