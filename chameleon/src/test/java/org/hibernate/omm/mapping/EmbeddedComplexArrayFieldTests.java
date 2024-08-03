package org.hibernate.omm.mapping;

import com.mongodb.client.model.Filters;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Struct;
import org.hibernate.omm.AbstractMongodbIntegrationTests;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * @author Nathan Xu
 * @author Jeff Yemin
 */
class EmbeddedComplexArrayFieldTests extends AbstractMongodbIntegrationTests {

    @Test
    void test() {
        getSessionFactory().inTransaction(session -> {
            var tag = new Tag();
            tag.tag = "comedy";
            var movie = new Movie();
            movie.id = 1;
            movie.tags = new Tag[] { tag };
            session.persist(movie);
        });

        var doc = getMongoDatabase().getCollection("movies")
                .find(Filters.eq(1)).first();

        System.out.println(doc); // minor defect still but should be easy to solve
    }

    @Override
    public List<Class<?>> getAnnotatedClasses() {
        return List.of(Movie.class, Tag.class);
    }

    @Entity(name = "Movie")
    @Table(name = "movies")
    static class Movie {
        @Id
        Integer id;

        @Struct(name = "tag")
        Tag[] tags;
    }

    @Embeddable
    static class Tag {
        String tag;
    }
}
