package persistence.entity;

import database.DatabaseServer;
import database.H2;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jdbc.JdbcTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import persistence.sql.Dialect;
import persistence.sql.H2Dialect;
import persistence.sql.ddl.query.CreateQueryBuilder;
import persistence.sql.ddl.query.DropQueryBuilder;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class EntityLoaderTest {

    @Entity
    private static class EntityLoaderTestEntity1 {
        @Id
        private Long id;

        private Integer age;

        public EntityLoaderTestEntity1() {
        }

        public EntityLoaderTestEntity1(Long id, Integer age) {
            this.id = id;
            this.age = age;
        }
    }

    @Entity
    private static class EntityLoaderTestEntity2 {
        @Id
        private Long id;

        @Column(name = "nick_name", length = 60, nullable = false)
        private String name;

        public EntityLoaderTestEntity2() {
        }

        public EntityLoaderTestEntity2(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private static DatabaseServer server;

    @BeforeEach
    void setUp() throws SQLException {
        server = new H2();
        server.start();

        Dialect dialect = new H2Dialect();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(server.getConnection());

        String createTestEntity1TableQuery = new CreateQueryBuilder(dialect).build(EntityLoaderTestEntity1.class);
        String createTestEntity2TableQuery = new CreateQueryBuilder(dialect).build(EntityLoaderTestEntity2.class);

        jdbcTemplate.execute(createTestEntity1TableQuery);
        jdbcTemplate.execute(createTestEntity2TableQuery);
    }

    @AfterEach
    void tearDown() throws SQLException {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(server.getConnection());

        String dropTestEntity1TableQuery = new DropQueryBuilder().build(EntityLoaderTestEntity1.class);
        String dropTestEntity2TableQuery = new DropQueryBuilder().build(EntityLoaderTestEntity2.class);

        jdbcTemplate.execute(dropTestEntity1TableQuery);
        jdbcTemplate.execute(dropTestEntity2TableQuery);
        server.stop();
    }

    @Test
    @DisplayName("Entity Loader는 EntityLoaderTestEntity1 클래스 타입으로 값을 읽어 반환 후 영속성 컨텍스트에 값을 추가한다.")
    void loadEntity() throws Exception {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(server.getConnection());
        PersistenceContext persistenceContext = new PersistenceContextImpl();
        EntityLoader entityLoader = new EntityLoader(jdbcTemplate, persistenceContext);

        EntityLoaderTestEntity1 entity1 = new EntityLoaderTestEntity1(1L, 30);
        EntityLoaderTestEntity1 entity2 = new EntityLoaderTestEntity1(2L, 40);

        EntityPersister entityPersister = new EntityPersister(EntityLoaderTestEntity1.class, jdbcTemplate);
        entityPersister.insert(entity1);
        entityPersister.insert(entity2);

        EntityKey entityKey1 = new EntityKey(1L, EntityLoaderTestEntity1.class);
        EntityKey entityKey2 = new EntityKey(2L, EntityLoaderTestEntity1.class);

        EntityLoaderTestEntity1 loadedEntity1 = entityLoader.loadEntity(EntityLoaderTestEntity1.class, entityKey1);
        EntityLoaderTestEntity1 loadedEntity2 = entityLoader.loadEntity(EntityLoaderTestEntity1.class, entityKey2);


        assertAll(
                () -> assertThat(loadedEntity1.id).isEqualTo(1L),
                () -> assertThat(loadedEntity1.age).isEqualTo(30),
                () -> assertThat(loadedEntity2.id).isEqualTo(2L),
                () -> assertThat(loadedEntity2.age).isEqualTo(40),

                () -> assertThat(persistenceContext.getEntity(entityKey1)).isEqualTo(loadedEntity1),
                () -> assertThat(persistenceContext.getEntity(entityKey2)).isEqualTo(loadedEntity2)
        );
    }

    @Test
    @DisplayName("이미 영속성 컨텍스트에 존재하는 엔티티를 로드할 때, 영속성 컨텍스트에 존재하는 엔티티를 반환한다.")
    void loadEntityFromPersistenceContext() throws Exception {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(server.getConnection());
        PersistenceContext persistenceContext = new PersistenceContextImpl();
        EntityLoader entityLoader = new EntityLoader(jdbcTemplate, persistenceContext);

        EntityLoaderTestEntity1 entity1 = new EntityLoaderTestEntity1(1L, 30);
        EntityLoaderTestEntity1 entity2 = new EntityLoaderTestEntity1(2L, 40);

        EntityKey entityKey1 = new EntityKey(1L, EntityLoaderTestEntity1.class);
        EntityKey entityKey2 = new EntityKey(2L, EntityLoaderTestEntity1.class);

        persistenceContext.addEntity(entityKey1, entity1);
        persistenceContext.addEntity(entityKey2, entity2);

        EntityLoaderTestEntity1 loadedEntity1 = entityLoader.loadEntity(EntityLoaderTestEntity1.class, entityKey1);
        EntityLoaderTestEntity1 loadedEntity2 = entityLoader.loadEntity(EntityLoaderTestEntity1.class, entityKey2);

        assertAll(
                () -> assertThat(loadedEntity1.id).isEqualTo(1L),
                () -> assertThat(loadedEntity1.age).isEqualTo(30),
                () -> assertThat(loadedEntity2.id).isEqualTo(2L),
                () -> assertThat(loadedEntity2.age).isEqualTo(40),

                () -> assertThat(persistenceContext.getEntity(entityKey1)).isEqualTo(loadedEntity1),
                () -> assertThat(persistenceContext.getEntity(entityKey2)).isEqualTo(loadedEntity2)
        );
    }

    @Test
    @DisplayName("Entity Loader는 EntityLoaderTestEntity2 클래스 타입으로 값을 읽어 반환 후 영속성 컨텍스트에 값을 추가한다.")
    void loadEntityOtherClassType() throws SQLException {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(server.getConnection());
        PersistenceContext persistenceContext = new PersistenceContextImpl();
        EntityLoader entityLoader = new EntityLoader(jdbcTemplate, persistenceContext);

        EntityLoaderTestEntity2 entity1 = new EntityLoaderTestEntity2(1L, "John");
        EntityLoaderTestEntity2 entity2 = new EntityLoaderTestEntity2(2L, "Jane");

        EntityPersister entityPersister = new EntityPersister(EntityLoaderTestEntity2.class, jdbcTemplate);
        entityPersister.insert(entity1);
        entityPersister.insert(entity2);

        EntityKey entityKey1 = new EntityKey(1L, EntityLoaderTestEntity2.class);
        EntityKey entityKey2 = new EntityKey(2L, EntityLoaderTestEntity2.class);

        EntityLoaderTestEntity2 loadedEntity1 = entityLoader.loadEntity(EntityLoaderTestEntity2.class, entityKey1);
        EntityLoaderTestEntity2 loadedEntity2 = entityLoader.loadEntity(EntityLoaderTestEntity2.class, entityKey2);


        assertAll(
                () -> assertThat(loadedEntity1.id).isEqualTo(1L),
                () -> assertThat(loadedEntity1.name).isEqualTo("John"),
                () -> assertThat(loadedEntity2.id).isEqualTo(2L),
                () -> assertThat(loadedEntity2.name).isEqualTo("Jane"),

                () -> assertThat(persistenceContext.getEntity(entityKey1)).isEqualTo(loadedEntity1),
                () -> assertThat(persistenceContext.getEntity(entityKey2)).isEqualTo(loadedEntity2)
        );
    }
}