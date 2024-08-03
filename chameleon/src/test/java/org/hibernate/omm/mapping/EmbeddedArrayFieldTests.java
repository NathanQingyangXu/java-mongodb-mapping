package org.hibernate.omm.mapping;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.bson.Document;
import org.hibernate.omm.AbstractMongodbIntegrationTests;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Nathan Xu
 */
class EmbeddedArrayFieldTests extends AbstractMongodbIntegrationTests {

    final Integer id = 22433;

    @Test
    @DisplayName("when collection field was persisted, it should be saved as embedded array in MongoDB")
    void test_persist_into_mongo_embedded_array_field() {
        var insertedMovie = getSessionFactory().fromTransaction(session -> {
            var movie = new Movie();
            movie.id = id;
            movie.title = "Gone with Wind";
            movie.tags = List.of("romantic", "classic");
            session.persist(movie);
            return movie;
        });

        var nativeQuery = "{ find: \"movies\", filter: { _id: { $eq: " + id + " } }, projection: { _id: 0, tags: 1 } }";
        Document result = getMongoDatabase().runCommand(Document.parse(nativeQuery));

        assertThat(result.getDouble("ok")).isEqualTo(1.0);

        @SuppressWarnings("unchecked")
        List<Document> firstBatch = result.getEmbedded(List.of("cursor", "firstBatch"), List.class);

        assertThat(firstBatch.size()).isEqualTo(1L);
        Document doc = firstBatch.get(0);

        assertThat(doc).usingRecursiveComparison().isEqualTo(new Document("tags", insertedMovie.tags));
    }

    @Test
    @DisplayName("when collection field was persisted, it should be fetched as expected")
    void test_load_from_mongo_embedded_array_field() {
        var insertedMovie = getSessionFactory().fromTransaction(session -> {
            var movie = new Movie();
            movie.id = id;
            movie.title = "Gone with Wind";
            movie.tags = List.of("romantic", "classic");
            session.persist(movie);
            return movie;
        });
        getSessionFactory().inTransaction(session -> {
            var loadedMovie = new Movie();
            session.load(loadedMovie, id);
            assertThat(loadedMovie).usingRecursiveComparison().isEqualTo(insertedMovie);
        });
    }

    @Override
    public List<Class<?>> getAnnotatedClasses() {
        return List.of(Movie.class);
    }

    @Entity(name = "Movie")
    @Table(name = "movies")
    static class Movie {
        @Id
        Integer id;

        String title;
        List<String> tags;

    }
}
