package org.hibernate.omm.mapping;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.omm.AbstractMongodbIntegrationTests;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * @author Nathan Xu
 * @author Jeff Yemin
 */
class EmbeddedComplexArrayFieldTests extends AbstractMongodbIntegrationTests {

    @Test
    @Disabled("Hibernate v6,6 will fix the issue; waiting for its release to finish the test")
    void test() {
        getSessionFactory().inTransaction(session -> {
            var tag = new TagsByAuthor();
            tag.author = "Nathan Xu";
            tag.tags = List.of("comedy", "drama");
            var movie = new Movie();
            movie.id = 1;
            movie.title = "Forrest Gump";
            movie.tagsByAuthor = List.of(tag);
            session.persist(movie);
        });
    }

    @Override
    public List<Class<?>> getAnnotatedClasses() {
        return List.of(Movie.class, TagsByAuthor.class);
    }

    @Entity(name = "Movie")
    @Table(name = "movies")
    static class Movie {
        @Id
        Integer id;
        String title;
        List<TagsByAuthor> tagsByAuthor;
    }

    @Embeddable
    static class TagsByAuthor {
        String author;
        List<String> tags;
    }
}
